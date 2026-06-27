package uz.oybek.ozamiz.data.ai

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzerConfig
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DateUtils
import java.util.Calendar

object DailyAiAnalysisRepository {
    private const val PREFS_NAME = "daily_ai_analysis_prefs"
    private const val KEY_ADVICE_DATE = "advice_date"
    private const val KEY_ADVICE_TEXT = "advice_text"
    private const val KEY_SHOWN_DATE = "shown_date"

    private val _currentAdvice = MutableStateFlow<String?>(null)
    val currentAdvice: StateFlow<String?> = _currentAdvice.asStateFlow()

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            FoodAnalyzerConfig.MODEL_NAME,
            systemInstruction = content {
                text("Sen foydalanuvchining shaxsiy dietologisan va g'amxo'r, to'g'ri so'z do'stisan. Juda aniq, toza va adabiy o'zbek tilida gaplash. Hech qachon shevada yoki g'alati so'zlar bilan gapirma (masalan 'yongansan', 'bo'g'ilding' dema). Foydalanuvchining kechagi kunidagi yegan ovqatlari va mashqlariga qarab, bugun uchun qisqa (ko'pi bilan 2 ta jumla), aniq bitta tavsiya va bitta ruhlantiruvchi gap ayt. Har kuni takrorlanmas va har xil maslahat ber.")
            }
        )
    }

    suspend fun checkAndGenerateAdvice() {
        val user = UserRepository.user
        if (!user.valid || user.uid.isNullOrBlank()) return

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = DateUtils.getFormattedDateString(java.util.Date(), "yyyy-MM-dd")
        val savedDate = prefs.getString(KEY_ADVICE_DATE, "")
        val shownDate = prefs.getString(KEY_SHOWN_DATE, "")

        if (savedDate == todayStr) {
            if (shownDate != todayStr) {
                // Return to flow only if we haven't shown it to the user yet today
                _currentAdvice.value = prefs.getString(KEY_ADVICE_TEXT, null)
            }
            return
        }

        try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = DateUtils.getFormattedDateString(cal.time, "yyyy-MM-dd")

            val snapshot = FirebasePaths.dailySummaryReference(user.uid, yesterdayStr).get().await()
            val summary = snapshot.getValue(DailySummary::class.java)

            val name = user.name.ifBlank { "Do'stim" }
            val targetCals = if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(user)

            if (summary == null || (summary.consumedCalories == 0 && summary.consumedWaterMl == 0 && summary.burnedCalories == 0)) {
                val defaultPrompt = "Kecha ($yesterdayStr) $name dasturga deyarli hech qanday ma'lumot kiritmadi yoki faol bo'lmadi. Unga bugun albatta kundaliklarni to'ldirib borishini eslat va sog'lig'i uchun qayg'urayotganingni sezdrib, 2 jumlali do'stona turtki ber."
                val response = generativeModel.generateContent(defaultPrompt)
                val advice = response.text ?: return
                saveAdvice(prefs, todayStr, advice)
            } else {
                val inputPrompt = """
                    Kechagi kungi ($yesterdayStr) natijalar:
                    Ismi: $name
                    Maqsad qilingan kaloriya limit: $targetCals kcal
                    Amalda yeyilgan ovqatlardan olingan kaloriya: ${summary.consumedCalories} kcal
                    Ichilgan suv miqdori: ${summary.consumedWaterMl} ml
                    Jismoniy mashg'ulotda yoqilgan kaloriya: ${summary.burnedCalories} kcal
                """.trimIndent()
                val response = generativeModel.generateContent(inputPrompt)
                val advice = response.text ?: return
                saveAdvice(prefs, todayStr, advice)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun markAdviceAsShown() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = DateUtils.getFormattedDateString(java.util.Date(), "yyyy-MM-dd")
        prefs.edit().putString(KEY_SHOWN_DATE, todayStr).apply()
        _currentAdvice.value = null
    }

    private fun saveAdvice(prefs: android.content.SharedPreferences, date: String, text: String) {
        prefs.edit()
            .putString(KEY_ADVICE_DATE, date)
            .putString(KEY_ADVICE_TEXT, text)
            .apply()
        _currentAdvice.value = text
    }
}
