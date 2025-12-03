package uz.oybek.ozamiz.data.food.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzerConfig.MODEL_VERSION
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzerConfig.foodAnalysisResultSchema
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysisResult

object FoodAnalyzer {

    private val foodAnalysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_VERSION,
            generationConfig = GenerationConfig.builder()
                .setResponseMimeType("application/json")
                .setResponseSchema(foodAnalysisResultSchema)
                .build()
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    private suspend fun uriToBitmap(
        context: Context, imageUri: Uri
    ): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            context.contentResolver,
                            imageUri
                        )
                    )
                } else {
                    MediaStore.Images.Media.getBitmap(
                        context.contentResolver,
                        imageUri
                    )
                }
            } catch (e: Exception) {
                return@withContext null
            }
        }
    }

    suspend fun analyzeFood(
        imageUri: Uri?,
        description: String
    ): DataWrapper<FoodAnalysisResult> {
        if (
            description.isBlank() &&
            imageUri == null
        ) {
            return DataWrapper.Error("")
        }
        val prompt = FoodAnalyzerConfig
            .foodAnalyzePrompt(description)
        return try {
            val imageBitmap = imageUri?.let {
                uriToBitmap(appContext, it)
            }
            val inputContent = content {
                if (imageBitmap != null) {
                    image(imageBitmap)
                }
                if (prompt.isNotEmpty()) {
                    text(prompt)
                }
            }
            val response = withContext(Dispatchers.IO) {
                foodAnalysisModel.generateContent(
                    prompt = inputContent
                )
            }
            val responseJson = response.text
            if (responseJson.isNullOrBlank()) {
                return DataWrapper.Error(Errors.EMPTY_RESULT)
            }
            val result = json.decodeFromString<FoodAnalysisResult>(responseJson)
            DataWrapper.Success(result)
        } catch (e: Exception) {
            DataWrapper.Error("")
        }
    }

    object Errors {
        const val EMPTY_RESULT = "empty"
    }
}