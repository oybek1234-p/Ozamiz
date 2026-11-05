package uz.oybek.ozamiz.ui.feature.home

import androidx.core.view.isVisible
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.tracker.model.MealType
import uz.oybek.ozamiz.data.tracker.session.GreatMealPrompt
import uz.oybek.ozamiz.databinding.LayoutHomeGreatMealPromptBinding
import uz.oybek.ozamiz.ui.modelUtils.getDisplayName
import uz.oybek.ozamiz.ui.utils.loadPhoto

object GreatMealPromptBinder {

    fun bind(
        binding: LayoutHomeGreatMealPromptBinding,
        prompt: GreatMealPrompt?,
        onCloseClick: () -> Unit,
        onPostClick: (photoUrl: String, textDescription: String) -> Unit
    ) {
        if (prompt != null) {
            binding.root.isVisible = true
            binding.tvGreatMealDesc.text = binding.root.context.getString(
                R.string.home_great_meal_desc_format,
                prompt.foodName
            )

            if (prompt.photoUrl.isNotBlank()) {
                binding.imgGreatMealPhoto.loadPhoto(prompt.photoUrl)
            } else {
                binding.imgGreatMealPhoto.setImageResource(R.drawable.placeholder_food)
            }

            binding.btnGreatMealClose.setOnClickListener {
                onCloseClick()
            }

            binding.btnGreatMealPost.setOnClickListener {
                val context = binding.root.context
                val mealTypeName = MealType.fromId(prompt.mealType, prompt.timestamp).getDisplayName(context)
                val hasMacros = prompt.protein > 0 || prompt.carbs > 0 || prompt.fat > 0
                val textDesc = if (hasMacros) {
                    context.getString(
                        R.string.home_great_meal_post_text_format,
                        mealTypeName,
                        prompt.foodName,
                        prompt.calories,
                        prompt.protein,
                        prompt.carbs,
                        prompt.fat
                    )
                } else {
                    context.getString(
                        R.string.home_great_meal_post_no_advice_format,
                        mealTypeName,
                        prompt.foodName,
                        prompt.calories
                    )
                }
                onPostClick(prompt.photoUrl, textDesc)
            }
        } else {
            binding.root.isVisible = false
        }
    }
}
