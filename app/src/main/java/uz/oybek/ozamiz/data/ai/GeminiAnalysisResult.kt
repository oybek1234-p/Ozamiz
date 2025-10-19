package uz.oybek.ozamiz.data.ai

data class AnalyzedIngredient(
    val name: String,
    val estimatedCalories: Int?
)

data class GeminiAnalysisResult(
    val estimatedCalories: Int,
    val estimatedProtein: Double?,
    val estimatedFat: Double?,
    val estimatedCarbs: Double?,
    val recognizedIngredients: List<AnalyzedIngredient>,
    val alternativeFoodSuggestions: String,
    val isHealthyForWeightLoss: String?, // YAXSHI, O'RTACHA, YOMON
    val healthFeedbackShort: String?,
)