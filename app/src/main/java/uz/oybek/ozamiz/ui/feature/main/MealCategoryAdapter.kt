package uz.oybek.ozamiz.ui.feature.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.databinding.ItemMealTypeCategoryBinding

class MealCategoryAdapter(
    private val onClick: (MealTime) -> Unit
) : RecyclerView.Adapter<MealCategoryAdapter.ViewHolder>() {

    private val items = MealTime.mealTypes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMealTypeCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemMealTypeCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MealTime) {
            binding.apply {
                textViewMealTypeName.setText(item.nameRes)
                Glide.with(root.context)
                    .load(item.iconRes)
                    .into(imageViewMealType)

                root.setOnClickListener {
                    onClick(item)
                }
            }
        }
    }
}
