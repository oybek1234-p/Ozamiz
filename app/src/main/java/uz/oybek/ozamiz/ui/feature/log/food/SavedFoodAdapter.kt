package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.savedFood.model.SaveFoodModel

class SavedFoodAdapter(
    private val onItemClick: (SaveFoodModel) -> Unit,
    private val onDeleteButtonClick: ((SaveFoodModel) -> Unit)? = null
) : ListAdapter<SaveFoodModel, SavedFoodAdapter.SavedFoodViewHolder>(SAVED_FOOD_DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedFoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_food, parent, false)
        return SavedFoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedFoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedFoodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameText = view.findViewById<TextView>(R.id.textView_saved_food_name)
        private val caloriesText = view.findViewById<TextView>(R.id.textView_saved_food_calories)
        private val foodImage = view.findViewById<ImageView>(R.id.imageView_saved_food)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { food -> onItemClick(food) }
                }
            }
            view.findViewById<View>(R.id.btn_delete_saved)?.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { food -> onDeleteButtonClick?.invoke(food) }
                }
            }
        }

        fun bind(food: SaveFoodModel) {
            nameText.text = food.name.ifBlank { food.description }
            caloriesText.text =
                itemView.context.getString(R.string.calories_format, food.calories)

            if (!food.photoUrl.isNullOrEmpty() && food.photoUrl != "null") {
                foodImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(food.photoUrl)
                    .placeholder(R.drawable.placeholder_food)
                    .into(foodImage)
            } else {
                foodImage.visibility = View.GONE
            }
        }
    }

    companion object {
        private val SAVED_FOOD_DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaveFoodModel>() {
            override fun areItemsTheSame(oldItem: SaveFoodModel, newItem: SaveFoodModel): Boolean =
                oldItem.id == newItem.id || oldItem.logId == newItem.logId

            override fun areContentsTheSame(oldItem: SaveFoodModel, newItem: SaveFoodModel): Boolean =
                oldItem == newItem
        }
    }
}
