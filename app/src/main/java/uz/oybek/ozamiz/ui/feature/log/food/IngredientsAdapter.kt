package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.data.analyzer.model.Macronutrient
import uz.oybek.ozamiz.databinding.ItemAnalyzedIngredientBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.modelUtils.getTitleRes
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.dpToPx

class IngredientsAdapter(
    private var ingredients: List<AnalyzedIngredient> = emptyList(),
    private var isGainWeight: Boolean = false,
    private val onItemClick: ((AnalyzedIngredient, Int) -> Unit)? = null,
    private val onEditClick: ((AnalyzedIngredient, Int) -> Unit)? = null,
    private val onDeleteClick: ((AnalyzedIngredient, Int) -> Unit)? = null
) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

    fun updateData(newIngredients: List<AnalyzedIngredient>, newIsGainWeight: Boolean = isGainWeight) {
        ingredients = newIngredients
        isGainWeight = newIsGainWeight
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalyzedIngredientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ingredients[position], position)
    }

    override fun getItemCount(): Int = ingredients.size

    inner class ViewHolder(private val binding: ItemAnalyzedIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalyzedIngredient, position: Int) {
            binding.textViewIngredientName.text = item.name

            // Subtitle: Amount & Calories (e.g. "350 g • 420 kkal")
            val calText = NutritionFormatter.kcal(item.calories)
            val amountStr = NutritionFormatter.ingredientAmount(item.amount, item.unit)
            val amountParts = mutableListOf<String>()
            if (amountStr.isNotEmpty()) amountParts.add(amountStr)
            if (calText.isNotEmpty()) amountParts.add(calText)

            val macro = item.tags.asSequence()
                .map { Macronutrient.fromTag(it) }
                .firstOrNull { it != Macronutrient.UNKNOWN }
                ?: Macronutrient.fromTag(item.primaryNutrient)
            val titleRes = macro.getTitleRes()
            if (macro != Macronutrient.UNKNOWN && titleRes != null) {
                amountParts.add(binding.root.context.getString(titleRes))
            }

            val amountText = amountParts.joinToString(separator = " • ")
            binding.textViewIngredientAmount.text = amountText
            binding.textViewIngredientAmount.isVisible = amountText.isNotEmpty()

            val tags = item.tags.take(3)
            if (tags.isNotEmpty()) {
                binding.scrollTags.isVisible = true
                binding.layoutTagsContainer.removeAllViews()
                for (tag in tags) {
                    val tagChip = android.widget.TextView(binding.root.context).apply {
                        text = tag
                        textSize = 11.5f
                        setTextColor(VaznimColors.getOnSurfaceVariant(context))
                        setBackgroundResource(uz.oybek.ozamiz.R.drawable.rounded_bg_surface_variant)
                        setPadding(
                            context.dpToPx(8),
                            context.dpToPx(3),
                            context.dpToPx(8),
                            context.dpToPx(3)
                        )
                        val params = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = context.dpToPx(6)
                        }
                        layoutParams = params
                    }
                    binding.layoutTagsContainer.addView(tagChip)
                }
            } else {
                binding.scrollTags.isVisible = false
            }

            binding.viewStatusDot.isVisible = false
            binding.textViewNutrientBadge.isVisible = false

            // Compatibility stubs
            binding.textViewIngredientAdvice.isVisible = false
            binding.layoutTags.isVisible = false
            binding.textViewSuitabilityBadge.isVisible = false
            binding.layoutIngredientAdvice.isVisible = false
            binding.textViewIngredientCalories.isVisible = false
            binding.textViewIngredientMacros.isVisible = false

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick?.invoke(item, position)
            }

            binding.buttonEditIngredient.setOnClickListener {
                onEditClick?.invoke(item, position) ?: onItemClick?.invoke(item, position)
            }

            binding.buttonDeleteIngredient.setOnClickListener {
                onDeleteClick?.invoke(item, position)
            }
        }
    }
}