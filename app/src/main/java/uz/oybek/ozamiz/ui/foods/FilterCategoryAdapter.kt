package uz.oybek.ozamiz.ui.foods

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.databinding.ItemFilterCategoryBinding
import uz.oybek.ozamiz.ui.loadPhoto
import android.util.Log

class FilterCategoryAdapter(
    private val onItemClick: (FoodCategory?) -> Unit
) : RecyclerView.Adapter<FilterCategoryAdapter.ViewHolder>() {

    private var categories: List<FoodCategory> = emptyList()
    private var selectedCategoryId: String? = null // Joriy tanlangan ID
    private val TAG = "FilterCategoryAdapter"

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newCategories: List<FoodCategory>, currentSelectedId: String?) {
        Log.d(TAG, "submitList called: categories=${newCategories.size}, currentSelectedId=$currentSelectedId")
        val oldSelectedId = selectedCategoryId
        categories = newCategories
        selectedCategoryId = currentSelectedId
        if (oldSelectedId != currentSelectedId || itemCount > 0) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFilterCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(private val binding: ItemFilterCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedCategory = categories[position]
                    Log.d(TAG, "ViewHolder click: clickedCategory=${clickedCategory.name}, currentSelectedId=$selectedCategoryId")
                    if (clickedCategory.id != selectedCategoryId) {
                        Log.d(TAG, "ViewHolder click: Triggering onItemClick")
                        onItemClick(clickedCategory)
                    }
                }
            }
        }

        fun bind(category: FoodCategory) {
            binding.textViewFilterCategoryName.text = category.name
            binding.imageViewFilterCategory.loadPhoto(category.imageUrl ?: R.drawable.food_ic)

            val isSelected = category.id == selectedCategoryId
            val context = itemView.context

            val textColorAttr = if (isSelected) com.google.android.material.R.attr.colorPrimaryFixed else com.google.android.material.R.attr.colorOnSurfaceVariant
            binding.textViewFilterCategoryName.setTextColor(MaterialColors.getColor(context, textColorAttr, ContextCompat.getColor(context, R.color.grey)))

            val strokeColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimarySurface, 0)
            val transparentColor = ContextCompat.getColor(context, android.R.color.transparent)

            binding.cardFilterCategory.strokeColor = if (isSelected) strokeColor else transparentColor
            binding.cardFilterCategory.cardElevation = if (isSelected) 2f.dpToPx() else 0f
        }
        private fun Float.dpToPx(): Float = this * itemView.context.resources.displayMetrics.density
    }
}