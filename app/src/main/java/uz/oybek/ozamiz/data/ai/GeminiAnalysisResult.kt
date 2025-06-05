package uz.oybek.ozamiz.data.ai

data class GeminiAnalysisResult(
    val estimatedCalories: Int,
    val estimatedProtein: Double?,
    val estimatedFat: Double?,
    val estimatedCarbs: Double?,
    val recognizedIngredients: List<String>,
    val insights: String,
    val alternativeFoodSuggestions: String
)