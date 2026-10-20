package uz.oybek.ozamiz.data.ai.aiChat

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import uz.oybek.ozamiz.data.dietolog.model.AnalyzedFoodPayload

@Keep
data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    @get:Exclude
    val isThinking: Boolean = false,
    @get:Exclude
    val feedback: String? = null,
    val analyzedFood: AnalyzedFoodPayload? = null,
    val isAddedToTracker: Boolean = false,
    val actions: List<uz.oybek.ozamiz.data.dietolog.model.DietologActionItem> = emptyList()
)
