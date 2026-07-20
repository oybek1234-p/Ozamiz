package uz.oybek.ozamiz.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils

object TodayAiMenuRepository {

    private fun getModel() = Firebase.ai.generativeModel(
        modelName = AiPromptBuilder.MODEL_NAME,
        generationConfig = generationConfig {
            temperature = 0.9f
            topK = 40
            topP = 0.95f
        }
    )

    fun generateTodayMenu(mealsCount: Int, preferences: String): Flow<DataWrapper<String>> = flow {

        try {
            val systemPrompt = AiPromptBuilder.buildTodayMenuInstruction(mealsCount, preferences)
            val modelInstance = getModel()
            val response = modelInstance.generateContent(systemPrompt)
            val text = response.text ?: ""
            
            val jsonExtracted = extractJson(text)
            
            if (jsonExtracted.isNotEmpty()) {
                AppPreference.update { 
                    it.copy(
                        todayAiMenuDate = DateUtils.todayString(),
                        todayAiMenuJson = jsonExtracted,
                        aiMenuGenerationCount = it.aiMenuGenerationCount + 1
                    )
                }
                emit(DataWrapper.Success(jsonExtracted))
            } else {
                emit(DataWrapper.Error("Xatolik yuz berdi. Natija JSON formatida emas."))
            }
        } catch (e: Exception) {
            emit(DataWrapper.Error(e.localizedMessage ?: "Noma'lum xatolik"))
        }
    }

    fun hasValidMenuForToday(): Boolean {
        return AppPreference.todayAiMenuDate == DateUtils.todayString() && AppPreference.todayAiMenuJson.isNotEmpty()
    }

    private fun extractJson(text: String): String {
        return try {
            val start = text.indexOf("{")
            val end = text.lastIndexOf("}")
            if (start != -1 && end != -1) {
                text.substring(start, end + 1)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
