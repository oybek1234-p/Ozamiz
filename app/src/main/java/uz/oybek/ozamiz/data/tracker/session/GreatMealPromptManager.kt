package uz.oybek.ozamiz.data.tracker.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class GreatMealPrompt(
    val photoUrl: String,
    val foodName: String,
    val calories: Int,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val mealType: String = "",
    val advice: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class GreatMealPromptManager @Inject constructor() {

    private val _prompt = MutableStateFlow<GreatMealPrompt?>(null)
    val prompt: StateFlow<GreatMealPrompt?> = _prompt.asStateFlow()

    fun setPrompt(prompt: GreatMealPrompt) {
        _prompt.value = prompt
    }

    fun clear() {
        _prompt.value = null
    }
}
