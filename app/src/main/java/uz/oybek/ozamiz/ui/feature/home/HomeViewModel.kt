package uz.oybek.ozamiz.ui.feature.home

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.AppConstants
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.course.LocalCourseRepository
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.logs.DailySummaryRepository
import uz.oybek.ozamiz.data.logs.ExerciseLogRepository
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.WaterLogRepository
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.ui.utils.DailyTipUtil
import java.util.Date
import kotlin.math.roundToInt

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isInitialLoading = true)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _foodJustAddedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 1)
    val foodJustAddedEvent = _foodJustAddedEvent.asSharedFlow()

    private val _showRateAppEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showRateAppEvent = _showRateAppEvent.asSharedFlow()

    private val _showPremiumEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val showPremiumEvent = _showPremiumEvent.asSharedFlow()

    private val glassVolume = AppConstants.Water.GLASS_VOLUME_ML
    private var previousWaterAmount = 0
    private var previousFoodCalories = 0
    private var previousExerciseBurnt = 0
    private var previousPoints = -1
    private var isFirstLoadCompleted = false
    private var isTipInitialized = false
    private var isFoodAdditionPending = false
    private var dataCollectionJob: Job? = null

    private var summaryJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                datesForScroller = DateUtils.generateDatesForScroller(),
                selectedDate = DateUtils.getStartOfDay(Date())
            )
        }
        initDailyTip()

        viewModelScope.launch {
            try {
                StreakRepository.updateStreak()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        UserRepository.observeCurrentUser { user ->
            if (user != null && user.valid) {
                val newPoints = user.scorePoints
                val added =
                    if (previousPoints >= 0 && newPoints != previousPoints) newPoints - previousPoints else 0
                previousPoints = newPoints

                _uiState.update {
                    it.copy(
                        streakInfo = user.streakInfo,
                        userPoints = newPoints,
                        pointsJustAdded = if (added > 0) added else 0,
                        caloriesTarget = user.targetCalories,
                        waterTarget = user.targetWaterMl,
                        isInitialLoading = false
                    )
                }

                checkUserAchievements(user)

                if (dataCollectionJob == null || !dataCollectionJob!!.isActive) {
                    startLoadingData(_uiState.value.selectedDate)
                }

                if (summaryJob == null || !summaryJob!!.isActive) {
                    val dates = _uiState.value.datesForScroller
                    if (dates.isNotEmpty()) {
                        val start = dates.first()
                        val end = dates.last()
                        summaryJob = DailySummaryRepository.observeSummaries(start, end)
                            .onEach { summaries ->
                                _uiState.update { it.copy(dailySummaries = summaries) }
                            }.launchIn(viewModelScope)
                    }
                }
            } else if (user == null || !user.valid) {
                _uiState.update { it.copy(isInitialLoading = false, dailySummaries = emptyMap()) }
                summaryJob?.cancel()
                summaryJob = null
                dataCollectionJob?.cancel()
                dataCollectionJob = null
                previousWaterAmount = 0
                previousFoodCalories = 0
                previousExerciseBurnt = 0
                isFirstLoadCompleted = false
            }
        }

        LocalCourseRepository.levelsFlow.onEach { levels ->
            _uiState.update { it.copy(courseLevels = levels) }
        }.launchIn(viewModelScope)

        LocalCourseRepository.activeLessonFlow.onEach { lesson ->
            _uiState.update { it.copy(activeLesson = lesson) }
        }.launchIn(viewModelScope)

        WeightLogRepository.getLogsFlow(user.uid).onEach { result ->
            if (result is DataWrapper.Success) {
                val logs = result.data.sortedBy { it.timestampLong }
                val showWeightProgress = if (logs.isNotEmpty()) {
                    val lastLog = logs.last()
                    val oneWeekMs = 7L * 24 * 60 * 60 * 1000
                    System.currentTimeMillis() - lastLog.timestampLong > oneWeekMs
                } else {
                    true
                }
                _uiState.update { it.copy(showWeightLogCard = showWeightProgress) }
            }
        }.launchIn(viewModelScope)
    }

    private fun checkUserAchievements(user: User) {
        val regDate = user.registerDate ?: return
        val daysPassed = (System.currentTimeMillis() - regDate.time) / (1000 * 60 * 60 * 24)
        val prefs =
            appContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

        if (daysPassed >= 1 && !user.premium && !user.paymentChecking) {
            val hasShownPremium = prefs.getBoolean("has_shown_premium_sheet", false)
            if (!hasShownPremium) {
                viewModelScope.launch {
                    _showPremiumEvent.emit(Unit)
                    prefs.edit().putBoolean("has_shown_premium_sheet", true).apply()
                }
            }
        }
    }

    private fun startLoadingData(date: Date) {
        dataCollectionJob?.cancel()
        dataCollectionJob = viewModelScope.launch {
            val timeoutJob = launch {
                delay(5000)
                if (_uiState.value.isRefreshing || _uiState.value.isInitialLoading) {
                    _uiState.update { it.copy(isRefreshing = false, isInitialLoading = false) }
                }
            }

            val summaryFlow = DailyLogRepository.observeDailySummary(date)
            val exerciseFlow = ExerciseLogRepository.getLogs(date)
            val foodFlow = FoodLogRepository.getAllLogs(date)

            combine(
                summaryFlow,
                exerciseFlow,
                foodFlow
            ) { summaryWrapper, exercises, foods ->
                Triple(summaryWrapper, exercises, foods)
            }.collect { (summaryWrapper, exercises, foods) ->
                timeoutJob.cancel()
                processStateUpdate(date, user, summaryWrapper, exercises, foods)
            }
        }
    }

    private fun processStateUpdate(
        date: Date,
        user: User?,
        summaryWrapper: DataWrapper<DailySummary>,
        exerciseLogs: List<ExerciseLog>,
        foodLogs: List<FoodLog>
    ) {
        if (user == null || !user.valid) {
            _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            return
        }
        when (summaryWrapper) {
            is DataWrapper.Success -> {
                val summary = summaryWrapper.data
                val macros = DietController.calculateMacrosByPercent(user.targetCalories)

                val currentWater = summary.consumedWaterMl
                val waterJustAddedEvent = isFirstLoadCompleted && currentWater > previousWaterAmount

                val currentFoodCalories = summary.consumedCalories
                val foodJustAddedEvent =
                    isFirstLoadCompleted && currentFoodCalories > previousFoodCalories

                val currentExerciseBurnt = summary.burnedCalories
                val exerciseJustAddedEvent =
                    isFirstLoadCompleted && currentExerciseBurnt > previousExerciseBurnt
                val lastExerciseType = if (exerciseJustAddedEvent) {
                    exerciseLogs.maxByOrNull { it.timestamp as? Long ?: 0L }?.exerciseType
                } else null


                val motivationalEvent =
                    waterJustAddedEvent || foodJustAddedEvent || exerciseJustAddedEvent
                val motivationalMessage = if (motivationalEvent) {
                    val name = user.name.ifBlank { "Foydalanuvchi" }
                    val isFemale = user.gender == 1

                    val femaleMessages = listOf(
                        "$name, o'zingizga ajratgan vaqtingiz o'zini oqlayapti, malikalardek ochilib ketyapsiz 👑",
                        "Kundan-kun chiroyli, kundan-kun nafis bo'lyapsiz, bu hali faqat boshlanishi! 🚀",
                        "$name, o'zingizni sevishni boshlaganingiz qiyofangizdan so'zsiz bilinib turibdi 💖",
                        "Bu ketishda tez orada hamma sizdan go'zallik sirini so'raydi, to'xtamang ✨",
                        "Ichki xotirjamlik va tashqi go'zallik sizda mukammal uyg'unlashmoqda 🌸",
                        "$name, qadam-baqadam o'zingizning eng zo'r versiyangizga aylanib boryapsiz 👌",
                        "Bugun ham o'zingiz uchun eng zo'r ishlarni qildingiz, malikam 👑",
                        "Ko'rinish va kayfiyat a'lo darajada! Faqat olg'a qadam bosing ✨",
                        "$name, hayotingizga kirib kelgan bu yangi ritm sizga nihoyatda yarashyapti 🎵",
                        "O'zingizga bo'lgan ishonch har qanday libosdan ham ko'proq chiroy beryapti 👗",
                        "Siz kabi o'z ustida ishlashni sevadigan ayollar doim e'tibor markazida bo'ladi 🌟",
                        "$name, bugungi sa'y-harakatlaringiz o'zingizga bo'lgan muhabbatning eng go'zal isbotidir 🌸",
                        "Yashirin jozibangiz kundan-kun yuzaga chiqib kelyapti, bu jarayon juda chiroyli 🎭",
                        "Har kuni oynaga qarab o'zingiz bilan faxrlanishingizga to'la haqqingiz bor 👌",
                        "$name, maqsadingiz sari intilishingiz sizni yanada viqorli va kuchli ko'rsatyapti 👑",
                        "Sizning holatingiz – 'Men buni eplayman va eng go'zali bo'laman' degan chin shijoatdir 💪",
                        "Kundan-kun ochilib, yanada nafis bo'lib boryapsiz, bu haqiqiy go'zallik ✨",
                        "$name, siz nafaqat o'zgaryapsiz, balki o'zingizning eng asl go'zalligingizni topyapsiz 🦋",
                        "Bugungi kuningiz ham latofat va sog'lik sari tashlangan muvaffaqiyatli qadam bo'ldi 🥇",
                        "O'zini sevgan ayol doim atrofdagilarni ham ilhomlantiradi, siz xuddi shundaysiz 💖",
                        "$name, sizdagi bu sabr va mehnat tez orada mo'jizaviy qomatga aylanadi ✨",
                        "Bu yo'lda ko'rsatayotgan mardona harakatlaringiz sizni haqiqiy malika qilmoqda 👑",
                        "O'zingizga ishongan holatda oldinga siljishingiz — eng to'g'ri tanlovingizdir 🌟",
                        "$name, sizni kuzatib, inson o'zini qanday qadrlashi kerakligini o'rganadi 🌿",
                        "Harakatdan aslo to'xtamang, natija siz kutgandan ham shirin va chiroyli bo'ladi 🍓",
                        "Kundan-kun yengillashib, chiroyingizga chiroy qo'shilayotganini his qilish naqadar maroqli 🕊️",
                        "$name, siz o'ylaganingizdan ko'proq kuchli va jozibalisuz! 💎",
                        "Go'zallik siri nima deyishsa, tinimsiz harakat deb sizni ko'rsatishlari aniq 💯",
                        "Har bir yangi kun sizga faqatgina latofat va yangi muvaffaqiyatlar olib kelsin 🌸"
                    )

                    val maleMessages = listOf(
                        "$name, temir irodangiz natijasini beryapti, qomat zo'r bo'lyapti! 💪",
                        "Kundan-kun kelishgan va baquvvat bo'lib boryapsiz! 🚀",
                        "$name, o'z ustida ishlaydigan erkak doim hurmatga loyiq 👊",
                        "Bu intizom bilan tez orada haqiqiy atlet qomatiga ega bo'lasiz 🔥",
                        "Mashg'ulotlar o'z so'zini aytyapti, formangiz ancha tiklandi 🏋️‍♂️",
                        "$name, haqiqiy chempionlarga xos xarakter ko'rsatyapsiz 🏆",
                        "Qat'iyat va mehnat — sizdagi yigitlik g'ururining belgisi ⚡",
                        "$name, yelkalar kengayib, qomat chiroyli shakl olyapti, qoyil! 🦍",
                        "O'z maqsadiga intilgan erkakning viqori boshqacha bo'ladi 🦅",
                        "Bugungi mehnatingiz sizni ertaga yanada baquvvat qiladi 🧱",
                        "$name, kundan-kun o'zingizning eng zo'r versiyangizga aylanyapsiz 💯",
                        "Chekinmaslik — bu sizning eng kuchli qurolingiz ⚔️",
                        "Irodangiz po'latdek ekan, natija shunga yarasha bo'lyapti 🛡️",
                        "$name, sport formasi sizga juda yarashyapti, gap yo'q! 👕",
                        "O'zini yenggan inson har qanday to'siqni yengadi, barakalla! 🏔️",
                        "$name, bu ketishda atrofdagilarga zo'r o'rnak bo'lyapsiz 🌟",
                        "Maqsad sari dadil qadam, ko'rinishingiz ancha o'zgargan! 🚶‍♂️",
                        "Har bir kuningiz iroda va kuch sinovi, siz buni eplayapsiz ⚙️",
                        "$name, qadam-baqadam ideal qomat sari borayapsiz, to'xtamang 🎯",
                        "Bu shijoatni ko'rib qoyil qolmaslikning iloji yo'q 👏",
                        "$name, sog'lom rejim sizning kuchingizga kuch qo'shyapti 🥗",
                        "Haqiqiy erkakcha harakat, chinakam natija! 🚀",
                        "O'z formangizni tiklayotganingiz shaxdingizdan ma'lum 🦅",
                        "$name, charchoqni yengib, faqat oldinga qarab boryapsiz, hurmat! 🤝",
                        "Kundan-kun qadamlar yengil, qomat kelishgan bo'lib boryapti 🚶‍♂️",
                        "Kuchli xarakter ortida har doim chiroyli natija turadi 💎",
                        "$name, bu iroda bilan tog'ni talqon qilsangiz arziydi 💪",
                        "O'ziga qat'iy talab qo'ya olgan yigitgina shunday natijaga erishadi 👑",
                        "Yashirin energiyangiz ochilib kelyapti, shunchaki daxshat! 🔥",
                        "$name, kundan-kun sportchilar qomatiga yaqinlashyapsiz 🏋️‍♂️",
                        "Bugungi harakat — kelajakdagi zo'r sog'liq va qomat kafolati 🛡️",
                        "Har bir tomchi teringiz irodangizning isbotidir, qoyil! 💧",
                        "$name, qat'iyatingiz sizga o'zgacha bir salobat beryapti 🦁",
                        "Shunchaki oldinga harakat qiling, eng zo'r forma o'zi ergashib keladi 🏃‍♂️",
                        "Sizdagi bu ishtiyoq har qanday maqsadni taslim qiladi 🎯",
                        "$name, kiyimlar ancha o'ziga moslashib qolibdi, natija zo'r! 👕",
                        "O'ziga ishonch va intizom – sizdagi eng kuchli xislatlar ⚡",
                        "Mehnat qilgan yigit elda, mashq qilgan qomat joyida! 🦅",
                        "$name, natija sari aniq va ishonchli ketyapsiz, tormoz bermang! 🚀",
                        "Bugun ham o'z dangasaligingiz ustidan g'alaba qozondingiz, chempion! 🏆",
                        "Kuch — bilakda emas, irodada ekanligini isbotlayapsiz 💪",
                        "$name, o'zingizga bo'lgan e'tibor sizni yanada salobatli ko'rsatyapti 🦍",
                        "Haqiqiy g'ururli inson o'z ustida ishlashdan charchamaydi 🤝",
                        "Natijalar ko'zga ko'rinyapti, formangiz ancha yaxshilangan! 📈",
                        "$name, po'lat irodangiz sizni faqat yuqoriga olib chiqadi 🏔️",
                        "Kundan-kun qomatingiz o'z shakliga kirmoqda, a'lo darajada! 👌",
                        "Bunday intizom bilan har qanday qiyinchilik taslim bo'ladi ⚔️",
                        "$name, sizdagi bu o'zgarish faqat qomat emas, xarakter kuchayganidan darak! 🦅",
                        "Zo'r forma, zo'r xarakter! Harakatdan to'xtamang 🔥",
                        "O'zgarishlar ajoyib, $name, siz o'ylaganingizdan ham baquvvatroqsiz! 💯"
                    )

                    if (isFemale) femaleMessages.random() else maleMessages.random()
                } else _uiState.value.motivationalMessage

                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        selectedDate = date,
                        caloriesRemaining = user.targetCalories - summary.consumedCalories,
                        caloriesConsumed = summary.consumedCalories,
                        caloriesTarget = user.targetCalories,
                        waterConsumed = currentWater,
                        waterTarget = user.targetWaterMl,
                        waterGlasses = calculateGlasses(currentWater, previousWaterAmount),
                        waterJustAdded = waterJustAddedEvent,
                        exerciseJustAddedType = lastExerciseType,
                        exerciseBurnt = currentExerciseBurnt,
                        exerciseTarget = AppConstants.Exercise.DEFAULT_TARGET_CALORIES,
                        carbsConsumed = summary.consumedCarbs.roundToInt(),
                        carbsTarget = macros.carbs,
                        proteinConsumed = summary.consumedProtein.roundToInt(),
                        proteinTarget = macros.protein,
                        fatConsumed = summary.consumedFat.roundToInt(),
                        fatTarget = macros.fat,
                        exerciseLogs = exerciseLogs.sortedByDescending { log ->
                            log.timestamp as? Long ?: 0L
                        },
                        dailyFoodLogs = foodLogs,
                        recommendedExercises = if (exerciseLogs.isEmpty()) UserExercise.getExercisesList(
                            user.gender
                        ).take(4) else emptyList(),
                        isProteinExceeded = summary.consumedProtein > macros.protein + 50,
                        isCarbsExceeded = summary.consumedCarbs > macros.carbs + 70,
                        isFatExceeded = summary.consumedFat > macros.fat + 30,
                        isCalorieOptimal = summary.consumedCalories >= user.targetCalories && summary.consumedCalories <= user.targetCalories + 200,
                        isCalorieExceeded = summary.consumedCalories > user.targetCalories + 200,
                    )
                }
                if (foodJustAddedEvent || isFoodAdditionPending) {
                    viewModelScope.launch { _foodJustAddedEvent.emit(Unit) }
                    isFoodAdditionPending = false
                }
                previousWaterAmount = currentWater
                previousFoodCalories = currentFoodCalories
                previousExerciseBurnt = currentExerciseBurnt
                isFirstLoadCompleted = true
            }

            is DataWrapper.Error -> {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
    }

    fun onFoodAdded() {
        isFoodAdditionPending = true
    }

    fun consumeFoodAddedEvent() {
        _foodJustAddedEvent.resetReplayCache()
    }

    private fun calculateGlasses(current: Int, old: Int): List<WaterGlassState> {
        val totalGlassesNeeded =
            maxOf(AppConstants.Water.DEFAULT_GLASSES_COUNT, (current / glassVolume) + 1)
        return (0 until totalGlassesNeeded).map { i ->
            val threshold = (i + 1) * glassVolume
            val isFilled = current >= threshold
            val isNext = !isFilled && (current >= (i * glassVolume))
            WaterGlassState(
                id = i,
                isFilled = isFilled,
                isNextToAdd = isNext,
                shouldAnimateFill = false
            )
        }
    }

    fun onDateSelected(date: Date) {
        if (DateUtils.isSameDay(date, _uiState.value.selectedDate) && isFirstLoadCompleted) return
        previousWaterAmount = 0
        previousFoodCalories = 0
        isFirstLoadCompleted = false
        _uiState.update { it.copy(selectedDate = date, isInitialLoading = true) }
        startLoadingData(date)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        startLoadingData(_uiState.value.selectedDate)
    }

    fun addWater(amount: Int) {
        val date = uiState.value.selectedDate
        val pointsToAdd = if (amount >= 250) (amount * 5) / 250 else 5

        if (pointsToAdd > 0) {
            val currentPoints = _uiState.value.userPoints
            _uiState.update {
                it.copy(
                    userPoints = currentPoints + pointsToAdd,
                    pointsJustAdded = pointsToAdd
                )
            }
        }

        viewModelScope.launch {
            WaterLogRepository.add(WaterLog(amountMl = amount), date)
            StreakRepository.updateStreak()
            if (pointsToAdd > 0) {
                UserRepository.addPoints(pointsToAdd)
            }
        }
    }

    fun clearPointsJustAdded() = _uiState.update { it.copy(pointsJustAdded = 0) }
    fun clearWaterJustAdded() = _uiState.update { it.copy(waterJustAdded = false) }
    fun clearExerciseJustAdded() = _uiState.update { it.copy(exerciseJustAddedType = null) }

    private fun initDailyTip() {
        if (isTipInitialized) return
        viewModelScope.launch {
            val prefs = appContext.getSharedPreferences(
                "daily_tip_prefs",
                android.content.Context.MODE_PRIVATE
            )
            val lastDismissedDate = prefs.getString("last_dismissed_date", "")
            val isDismissedToday = lastDismissedDate == Date().dateApiFormat()
            var currentIndex = prefs.getInt("current_tip_index", -1)

            if (currentIndex == -1) {
                currentIndex = (0 until DailyTipUtil.tips.size).random()
                prefs.edit { putInt("current_tip_index", currentIndex) }
            }

            val tip = DailyTipUtil.getTip(currentIndex)
            _uiState.update { it.copy(dailyTip = tip, isTipVisible = !isDismissedToday) }
            isTipInitialized = true
        }
    }

    fun onHomeEntered() {
        val prefs =
            appContext.getSharedPreferences("daily_tip_prefs", android.content.Context.MODE_PRIVATE)
        val lastDismissedDate = prefs.getString("last_dismissed_date", "")
        val isDismissedToday = lastDismissedDate == Date().dateApiFormat()
        if (!isDismissedToday && !_uiState.value.isTipVisible) {
            _uiState.update { it.copy(isTipVisible = true) }
        }
    }

    fun onTipDismissed() {
        _uiState.update { it.copy(isTipVisible = false) }
        val prefs =
            appContext.getSharedPreferences("daily_tip_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("last_dismissed_date", Date().dateApiFormat()).apply()
        refreshTip()
    }

    fun onTipUnderstood() {
        refreshTip()
        _uiState.update { it.copy(isTipVisible = true) }
    }

    private fun refreshTip() {
        val prefs =
            appContext.getSharedPreferences("daily_tip_prefs", android.content.Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("current_tip_index", -1)
        val nextIndex = DailyTipUtil.getNewTipIndex(currentIndex)
        prefs.edit().putInt("current_tip_index", nextIndex).apply()
        val tip = DailyTipUtil.getTip(nextIndex)
        _uiState.update { it.copy(dailyTip = tip) }
    }


}
