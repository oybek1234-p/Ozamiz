package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.menu.CalorieFilter
import uz.oybek.ozamiz.data.menu.CategoryType
import uz.oybek.ozamiz.data.menu.FilterId
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.databinding.ItemMenuHorizontalRecyclerBinding
import uz.oybek.ozamiz.databinding.ItemMenuListHeaderBinding

class MenuDiscoveryAdapter(
    private val onFilterClick: (type: CategoryType, item: FilterItem) -> Unit,
    private val onDietitianClick: () -> Unit
) : ListAdapter<MenuUiItem, RecyclerView.ViewHolder>(DiscoveryDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MenuUiItem.Header -> TYPE_HEADER
            is MenuUiItem.PopularCategoriesList -> TYPE_POPULAR
            is MenuUiItem.CalorieList -> TYPE_CALORIE
            is MenuUiItem.MealTimeList -> TYPE_MEAL
            is MenuUiItem.MethodList -> TYPE_METHOD
            is MenuUiItem.DietList -> TYPE_DIET
            is MenuUiItem.IngredientsList -> TYPE_INGREDIENTS
            is MenuUiItem.DietitianCard -> TYPE_DIETITIAN
            else -> TYPE_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemMenuListHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_DIETITIAN -> DietitianCardViewHolder(
                uz.oybek.ozamiz.databinding.ItemMenuDietitianCardBinding.inflate(inflater, parent, false)
            )
            else -> GridViewHolder(
                ItemMenuHorizontalRecyclerBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MenuUiItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MenuUiItem.PopularCategoriesList -> (holder as GridViewHolder).bindCircularHorizontal(item.items)
            is MenuUiItem.CalorieList -> (holder as GridViewHolder).bindCalorieGrid(item.items, 3)
            is MenuUiItem.MealTimeList -> (holder as GridViewHolder).bindLargeHorizontal(item.items, CategoryType.MAIN_MEALS)
            is MenuUiItem.MethodList -> (holder as GridViewHolder).bindLargeHorizontal(item.items, CategoryType.METHODS)
            is MenuUiItem.DietList -> (holder as GridViewHolder).bindGrid(item.items, CategoryType.DIETS, 3)
            is MenuUiItem.IngredientsList -> (holder as GridViewHolder).bindGrid(item.items, CategoryType.INGREDIENTS, 3)
            is MenuUiItem.DietitianCard -> (holder as DietitianCardViewHolder).bind(item)
            else -> {}
        }
    }

    class HeaderViewHolder(
        private val binding: ItemMenuListHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuUiItem.Header) {
            binding.textHeaderTitle.setText(item.titleResId)
        }
    }

    inner class GridViewHolder(
        private val binding: ItemMenuHorizontalRecyclerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindGrid(items: List<FilterItem>, type: CategoryType, spanCount: Int) {
            binding.innerRecyclerView.apply {
                layoutManager = GridLayoutManager(context, spanCount)
                adapter = MenuListSquareAdapter { onFilterClick(type, it) }.also {
                    it.submitList(items)
                }
                isNestedScrollingEnabled = false
            }
        }

        fun bindCalorieGrid(items: List<CalorieFilter>, spanCount: Int) {
            val filterItems = items.map {
                FilterItem(
                    id = FilterId.valueOf(it.name),
                    name = "${it.min}-${it.max} kal",
                    emoji = it.emoji
                )
            }
            bindGrid(filterItems, CategoryType.CALORIE, spanCount)
        }

        fun bindCircularHorizontal(items: List<FilterItem>) {
            binding.innerRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = MenuCircularFilterAdapter { onFilterClick(CategoryType.POPULAR, it) }.also {
                    it.submitList(items)
                }
            }
        }

        fun bindHorizontal(items: List<FilterItem>, type: CategoryType) {
            binding.innerRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = MenuListSquareAdapter(isHorizontal = true) { onFilterClick(type, it) }.also {
                    it.submitList(items)
                }
            }
        }

        fun bindLargeHorizontal(items: List<FilterItem>, type: CategoryType) {
            binding.innerRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = MenuLargeFilterAdapter { onFilterClick(type, it) }.also {
                    it.submitList(items)
                }
            }
        }
    }

    inner class DietitianCardViewHolder(
        private val binding: uz.oybek.ozamiz.databinding.ItemMenuDietitianCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuUiItem.DietitianCard) {
            if (item.hasMenu) {
                binding.textDietitianTitle.text = "Bugungi AI Menyu"
                binding.textDietitianSubtitle.text = "Sizning shaxsiy menyuingiz tayyor"
            } else {
                binding.textDietitianTitle.text = "Dietolog siz uchun menyu tuzadi"
                binding.textDietitianSubtitle.text = "Shaxsiy menyu tuzish uchun bosing"
            }
            binding.root.setOnClickListener {
                onDietitianClick()
            }
        }
    }

    private class DiscoveryDiffCallback : DiffUtil.ItemCallback<MenuUiItem>() {
        override fun areItemsTheSame(oldItem: MenuUiItem, newItem: MenuUiItem): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: MenuUiItem, newItem: MenuUiItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_POPULAR = 1
        const val TYPE_CALORIE = 2
        const val TYPE_MEAL = 3
        const val TYPE_METHOD = 4
        const val TYPE_DIET = 5
        const val TYPE_INGREDIENTS = 6
        const val TYPE_DIETITIAN = 7
    }
}
