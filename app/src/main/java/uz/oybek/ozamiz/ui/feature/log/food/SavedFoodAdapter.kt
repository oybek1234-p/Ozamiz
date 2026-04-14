package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.ItemSavedFoodBinding

class SavedFoodAdapter(
    private val onItemClick: (FoodLog) -> Unit,
    private val onAddButtonClick: ((FoodLog) -> Unit)? = null,
    private val onDeleteButtonClick: ((FoodLog) -> Unit)? = null,
    private val layoutResId: Int = R.layout.item_saved_food
) : ListAdapter<FoodLog, SavedFoodAdapter.SavedFoodViewHolder>(SAVED_FOOD_DIFF_CALLBACK) {

    private var loadingIds: Set<String> = emptySet()

    fun updateLoadingIds(ids: Set<String>) {
        val oldIds = loadingIds
        loadingIds = ids
        // Optimization: only notify items that changed
        (oldIds + ids).forEach { id ->
            val index = currentList.indexOfFirst { it.logId == id }
            if (index != -1) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedFoodViewHolder {
        val binding = ItemSavedFoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // If we want to use a different layout, we might need a different binding or custom view
        // For simplicity, I'll stick to a single binding if the IDs match, 
        // but since I created a new layout file with different structure, 
        // I should probably use a custom view or ViewBinding.
        // Let's use ViewBinding for the specific layout.
        
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return SavedFoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedFoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedFoodViewHolder(view: android.view.View) :
        RecyclerView.ViewHolder(view) {

        private val nameText = view.findViewById<android.widget.TextView>(R.id.textView_saved_food_name)
        private val caloriesText = view.findViewById<android.widget.TextView>(R.id.textView_saved_food_calories)
        private val foodImage = view.findViewById<android.widget.ImageView>(R.id.imageView_saved_food)
        private val addIcon = view.findViewById<android.widget.ImageView>(R.id.img_add_icon)
        private val progressAdd = view.findViewById<android.widget.ProgressBar>(R.id.progress_add)

        init {
            view.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { food -> onItemClick(food) }
                }
            }
            view.findViewById<android.view.View>(R.id.btn_add_saved)?.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { food -> onAddButtonClick?.invoke(food) }
                }
            }
            view.findViewById<android.view.View>(R.id.btn_delete_saved)?.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { food -> onDeleteButtonClick?.invoke(food) }
                }
            }
        }

        fun bind(food: FoodLog) {
            nameText.text = food.description
            caloriesText.text =
                itemView.context.getString(R.string.calories_format, food.calories)

            if (!food.photoUrl.isNullOrEmpty() && food.photoUrl != "null") {
                foodImage.visibility = android.view.View.VISIBLE
                Glide.with(itemView.context)
                    .load(food.photoUrl)
                    .placeholder(R.drawable.placeholder_food)
                    .into(foodImage)
            } else {
                foodImage.visibility = android.view.View.GONE
            }
            
            val isLoading = loadingIds.contains(food.logId)
            progressAdd?.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            addIcon?.visibility = if (isLoading) android.view.View.INVISIBLE else android.view.View.VISIBLE
            itemView.findViewById<android.view.View>(R.id.btn_add_saved)?.isEnabled = !isLoading
        }
    }

    companion object {
        private val SAVED_FOOD_DIFF_CALLBACK = object : DiffUtil.ItemCallback<FoodLog>() {
            override fun areItemsTheSame(oldItem: FoodLog, newItem: FoodLog): Boolean =
                oldItem.logId == newItem.logId

            override fun areContentsTheSame(oldItem: FoodLog, newItem: FoodLog): Boolean =
                oldItem == newItem
        }
    }
}
