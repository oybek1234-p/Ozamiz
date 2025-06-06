package uz.oybek.ozamiz.data.ai

import android.net.Uri
import android.util.Log
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.foods.FoodItem
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.handleException
import java.time.LocalDate

object ChatGptFoodAnalyzer {

    private const val API_KEY =
        "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"
    private const val TAG = "ChatGptFoodAnalyzer"

    private val openAI: OpenAI? by lazy {
        OpenAI(API_KEY)
    }

    suspend fun analyzeFood(
        imageUri: Uri?, description: String, mealTime: String
    ): DataWrapper<GeminiAnalysisResult> {
        return DataWrapper.Error("")
    }

}