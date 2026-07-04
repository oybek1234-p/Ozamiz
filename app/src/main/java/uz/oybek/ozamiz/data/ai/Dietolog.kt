package uz.oybek.ozamiz.data.ai

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.local.AiChatDatabase
import uz.oybek.ozamiz.data.ai.local.AiChatMessageEntity
import uz.oybek.ozamiz.data.ai.model.AiMealRecommendation
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import java.util.Calendar
import java.util.Date

object Dietolog {
    private val _latestMessage = MutableStateFlow<String?>(null)
    val latestMessage: StateFlow<String?> = _latestMessage.asStateFlow()

    private val _newMessagesEvent = MutableSharedFlow<String>()
    val newMessagesEvent = _newMessagesEvent.asSharedFlow()

    val isGenerating = MutableStateFlow(false)
    private var hasCheckedAppOpenedAdvice = false

    private val aiChatDao by lazy {
        AiChatDatabase.getDatabase(appContext).aiChatDao()
    }

    suspend fun onFoodLogAdded(foodLog: FoodLog? = null) {
        val user = UserRepository.user
        if (!user.valid || user.uid.isBlank() || !user.isPremiumOrTrial()) return

        if (isGenerating.value) return
        isGenerating.value = true
        try {
            val date = Date()
            val summary = DailyLogRepository.getDailySummarySync(date)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val mealTimeId = when {
                hour < 10 -> "breakfast"
                hour < 14 -> "lunch"
                else -> "dinner"
            }
            val todayStr = DateUtils.getFormattedDateString(date, "yyyy-MM-dd")

            if (foodLog != null && foodLog.calories > 100 && (hour !in 4..<21)) {
                val warnings = appContext.resources.getStringArray(R.array.late_food_warnings)
                if (warnings.isNotEmpty()) {
                    val parsedCode = warnings.random()
                    _latestMessage.value = parsedCode
                    return
                }
            }

            if (checkCalorieLimit(summary, user)) {
                return
            }

            if (checkLowProteinHighCarbFat(summary, user, mealTimeId, todayStr)) {
                return
            }

            if (checkMacroLimits(summary, user, mealTimeId, todayStr)) {
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isGenerating.value = false
        }
    }

    private suspend fun checkCalorieLimit(
        summary: DailySummary,
        user: User
    ): Boolean {
        val targetCalories =
            if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(
                user
            )
        val netCalories = summary.consumedCalories - summary.burnedCalories
        if (netCalories >= targetCalories) {
            val parsedMessage = DietologAnswers.calorieLimitExceededMessages.random()
            _latestMessage.value = parsedMessage
            return true
        }
        return false
    }

    private suspend fun checkLowProteinHighCarbFat(
        summary: DailySummary,
        user: User,
        mealTimeId: String,
        todayStr: String
    ): Boolean {
        val targetCals =
            if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(
                user
            )
        val macros = DietController.calculateMacrosByPercent(targetCals)
        val targetProtein = if (user.targetProtein > 0) user.targetProtein else macros.protein
        val targetFat = if (user.targetFat > 0) user.targetFat else macros.fat
        val targetCarbs = if (user.targetCarbs > 0) user.targetCarbs else macros.carbs

        if (summary.consumedFat > targetFat && summary.consumedCarbs > targetCarbs && summary.consumedProtein < targetProtein * 0.5) {
            val key = "${todayStr}_${mealTimeId}_protein_low"
            if (!LocalPreferencesRepository.isViolationGated(key)) {
                LocalPreferencesRepository.setViolationGated(key)
                val parsedMessage = DietologAnswers.lowProteinHighCarbFatMessages.random()
                _latestMessage.value = parsedMessage
                return true
            }
        }
        return false
    }

    private suspend fun checkMacroLimits(
        summary: DailySummary,
        user: User,
        mealTimeId: String,
        todayStr: String
    ): Boolean {
        val targetCals =
            if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(
                user
            )
        val macros = DietController.calculateMacrosByPercent(targetCals)
        val targetProtein = if (user.targetProtein > 0) user.targetProtein else macros.protein
        val targetFat = if (user.targetFat > 0) user.targetFat else macros.fat
        val targetCarbs = if (user.targetCarbs > 0) user.targetCarbs else macros.carbs

        if (summary.consumedFat > targetFat) {
            val key = "${todayStr}_${mealTimeId}_fat"
            if (!LocalPreferencesRepository.isViolationGated(key)) {
                LocalPreferencesRepository.setViolationGated(key)
                val parsedMessage = DietologAnswers.fatExceededMessages.random()
                _latestMessage.value = parsedMessage
                return true
            }
        }
        if (summary.consumedCarbs > targetCarbs) {
            val key = "${todayStr}_${mealTimeId}_carbs"
            if (!LocalPreferencesRepository.isViolationGated(key)) {
                LocalPreferencesRepository.setViolationGated(key)
                val parsedMessage = DietologAnswers.carbExceededMessages.random()
                _latestMessage.value = parsedMessage
                return true
            }
        }
        if (summary.consumedProtein > targetProtein) {
            val key = "${todayStr}_${mealTimeId}_protein"
            if (!LocalPreferencesRepository.isViolationGated(key)) {
                LocalPreferencesRepository.setViolationGated(key)
                val parsedMessage = DietologAnswers.proteinExceededMessages.random()
                _latestMessage.value = parsedMessage
                return true
            }
        }
        return false
    }

    suspend fun getMealRecommendations(): Pair<String, AiMealRecommendation>? {
        val user = UserRepository.user
        if (!user.valid || user.uid.isBlank() || !user.isPremiumOrTrial()) return null
        if (!LocalPreferencesRepository.isNewUserAdviceShown()) {
            return null
        }

        return try {
            val date = Date()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val mealTime: MealTime = when {
                hour < 10 -> MealTime.Breakfast
                hour < 14 -> MealTime.Lunch
                else -> MealTime.Dinner
            }

            val summary = DailyLogRepository.getDailySummarySync(date)
            val mealTypeStr = appContext.getString(mealTime.nameRes)
            val targetCals =
                if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(
                    user
                )

            val netCalories = summary.consumedCalories - summary.burnedCalories
            val isExceeded = netCalories >= targetCals

            val todayStr = DateUtils.getFormattedDateString(date, "yyyy-MM-dd")
            val gateKey = "${todayStr}_${mealTime.id}_exceeded_${isExceeded}"
            if (LocalPreferencesRepository.getMealRecommendationKey() == gateKey) {
                val cachedText = LocalPreferencesRepository.getMealRecommendationText()
                if (cachedText != null) {
                    val displayTitle = if (isExceeded) {
                        appContext.getString(R.string.limit_exceeded_title)
                    } else {
                        mealTypeStr
                    }
                    return Pair(displayTitle, AiMealRecommendation(cachedText, mealTime))
                }
                return null
            }

            val cal = Calendar.getInstance().apply { time = date }
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

            val recommendationText = if (isExceeded) {
                val calIndex = dayOfYear % DietologAnswers.calorieLimitExceededMessages.size
                DietologAnswers.calorieLimitExceededMessages[calIndex]
            } else {
                val recommendationsList = when (mealTime) {
                    MealTime.Breakfast -> DietologAnswers.breakfastRecommendations
                    MealTime.Lunch -> DietologAnswers.lunchRecommendations
                    MealTime.Dinner, MealTime.Snack -> DietologAnswers.dinnerRecommendations
                }
                val index = dayOfYear % recommendationsList.size
                recommendationsList[index]
            }

            val displayTitle = if (isExceeded) {
                appContext.getString(R.string.limit_exceeded_title)
            } else {
                mealTypeStr
            }

            LocalPreferencesRepository.setMealRecommendationKey(gateKey)
            LocalPreferencesRepository.setMealRecommendationText(recommendationText)
            Pair(displayTitle, AiMealRecommendation(recommendationText, mealTime))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun checkAppOpenedAdvice() {
        if (hasCheckedAppOpenedAdvice) {
            restoreLatestMessageIfNeeded()
            return
        }
        hasCheckedAppOpenedAdvice = true

        if (!LocalPreferencesRepository.isNewUserAdviceShown()) {
            val welcomeMsg = DietologAnswers.newUserWelcomingMessages.random()
            _latestMessage.value = welcomeMsg
            LocalPreferencesRepository.setNewUserAdviceShown(true)
            LocalPreferencesRepository.setLastAiEventTimestamp(System.currentTimeMillis())
            return
        }

        val lastTs = LocalPreferencesRepository.getLastAiEventTimestamp()
        val threeHoursMs = 3 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - lastTs < threeHoursMs) {
            restoreLatestMessageIfNeeded()
            return
        }
        val user = UserRepository.user
        if (!user.valid || user.uid.isBlank() || !user.isPremiumOrTrial()) {
            restoreLatestMessageIfNeeded()
            return
        }

        if (isGenerating.value) {
            restoreLatestMessageIfNeeded()
            return
        }
    }

    suspend fun restoreLatestMessageIfNeeded() {
        if (_latestMessage.value != null) return
        try {
            val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
            val messages = aiChatDao.getMessagesForDateSync(todayStr)
            val lastModelMessage = messages.lastOrNull { !it.isUser }?.text
            if (lastModelMessage != null) {
                _latestMessage.value = lastModelMessage
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addMessageToChat(messageText: String, showSetHasUnread: Boolean) {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val entity = AiChatMessageEntity(
            text = messageText,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            dateString = todayStr
        )
        aiChatDao.insertMessage(entity)
        LocalPreferencesRepository.setHasUnreadChat(showSetHasUnread)
        _newMessagesEvent.emit(messageText)
    }
}
