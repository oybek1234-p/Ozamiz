package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.PlannedMealItem
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.databinding.ItemPlannedMealBinding

class PlannedMealAdapter(
    private val onReplaceClick: (mealTypeTag: String) -> Unit,
    private val isPremium: () -> Boolean
) : ListAdapter<Triple<String, PlannedMealItem?, Boolean>, PlannedMealAdapter.ViewHolder>(PlannedMealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlannedMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPlannedMealBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mealData: Triple<String, PlannedMealItem?, Boolean>) {
            val (mealTypeTag, meal, isLoading) = mealData
            val mealInfo = MealsType.fromId(mealTypeTag)

            binding.imageViewMealTypeIconPlan.setImageResource(mealInfo?.iconRes ?: R.drawable.food_ic)
            binding.textViewMealTypePlan.text = mealInfo?.name ?: "Noma'lum"

            // Set loading state for the specific item
            val showReplaceButton = isPremium() && meal != null && !isLoading
            binding.buttonReplaceMealPlan.visibility = if (showReplaceButton) View.VISIBLE else View.INVISIBLE
            binding.progressReplaceMeal.isVisible = isLoading

            if (meal != null) {
                binding.textViewMealNamePlan.text = meal.name
                binding.textViewCaloriesPlan.text = meal.calories?.let { "${it} kkal" } ?: "N/A"
                binding.textViewProteinPlan.text = meal.protein?.let { "${it}g" } ?: "N/A"
                binding.textViewFatPlan.text = meal.fat?.let { "${it}g" } ?: "N/A"
                binding.textViewCarbsPlan.text = meal.carbs?.let { "${it}g" } ?: "N/A"

                // Ingredientlar
                if (meal.ingredients.isNotEmpty()) {
                    binding.textViewIngredientsTitlePlan.visibility = View.VISIBLE
                    binding.layoutIngredientsPlan.removeAllViews() // Eskilarini tozalash
                    meal.ingredients.forEach { ingredient ->
                        val textView = TextView(itemView.context).apply {
                            text = "• $ingredient"
                            setTextAppearance(R.style.Text1_16)
                            setTextColor(MaterialColors.getColor(binding.root,
                                com.google.android.material.R.attr.colorOnSurface))
                            setPadding(0, 2 * itemView.context.resources.displayMetrics.density.toInt(), 0, 2 * itemView.context.resources.displayMetrics.density.toInt())
                        }
                        binding.layoutIngredientsPlan.addView(textView)
                    }
                } else {
                    binding.textViewIngredientsTitlePlan.visibility = View.GONE
                    binding.layoutIngredientsPlan.visibility = View.GONE
                }

                // Tayyorlash qadamlari
                if (!meal.preparationSteps.isNullOrEmpty()) {
                    binding.textViewPreparationTitlePlan.visibility = View.VISIBLE
                    binding.layoutPreparationPlan.removeAllViews()
                    meal.preparationSteps?.let { step ->
                        val textView = TextView(itemView.context).apply {
                            text = step
                            setTextAppearance(R.style.Text2_15)
                            setLineSpacing(1.0f,1.2f)
                            setTextColor(MaterialColors.getColor(binding.root,
                                com.google.android.material.R.attr.colorOnSurface))
                            setPadding(0, 2 * itemView.context.resources.displayMetrics.density.toInt(), 0, 2 * itemView.context.resources.displayMetrics.density.toInt())
                        }
                        binding.layoutPreparationPlan.addView(textView)
                    }
                } else {
                    binding.textViewPreparationTitlePlan.visibility = View.GONE
                    binding.layoutPreparationPlan.visibility = View.GONE
                }

                // Foydali jihatlari
                if (!meal.healthBenefits.isNullOrBlank()) {
                    binding.textViewHealthBenefitsTitlePlan.visibility = View.VISIBLE
                    binding.textViewHealthBenefitsPlan.text = meal.healthBenefits
                } else {
                    binding.textViewHealthBenefitsTitlePlan.visibility = View.GONE
                    binding.textViewHealthBenefitsPlan.visibility = View.GONE
                }

                binding.buttonReplaceMealPlan.setOnClickListener { onReplaceClick(mealTypeTag) }

            } else {
                binding.textViewMealNamePlan.text = "Reja tuzilmagan..."
                binding.textViewCaloriesPlan.text = ""
                binding.textViewProteinPlan.text = ""
                binding.textViewFatPlan.text = ""
                binding.textViewCarbsPlan.text = ""
                binding.textViewIngredientsTitlePlan.visibility = View.GONE
                binding.textViewPreparationTitlePlan.visibility = View.GONE
                binding.textViewHealthBenefitsTitlePlan.visibility = View.GONE
                binding.layoutIngredientsPlan.visibility = View.GONE
                binding.layoutPreparationPlan.visibility = View.GONE
                binding.textViewHealthBenefitsPlan.visibility = View.GONE
                binding.buttonReplaceMealPlan.visibility = View.GONE
            }
        }
    }
}

class PlannedMealDiffCallback : DiffUtil.ItemCallback<Triple<String, PlannedMealItem?, Boolean>>() {
    override fun areItemsTheSame(oldItem: Triple<String, PlannedMealItem?, Boolean>, newItem: Triple<String, PlannedMealItem?, Boolean>): Boolean {
        return oldItem.first == newItem.first
    }

    override fun areContentsTheSame(oldItem: Triple<String, PlannedMealItem?, Boolean>, newItem: Triple<String, PlannedMealItem?, Boolean>): Boolean {
        // isLoading holati o'zgarganda ham item yangilanishi uchun to'liq tekshiramiz
        return oldItem == newItem
    }
}