package uz.oybek.ozamiz.ui.diet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.diet.DietController
import uz.oybek.ozamiz.data.diet.Meal
import uz.oybek.ozamiz.data.diet.MealTypes
import uz.oybek.ozamiz.databinding.FoodFeedItemBinding
import uz.oybek.ozamiz.databinding.MealsFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.loadPhoto

class MealsFragment : BaseFragment<MealsFragmentBinding>() {

    private var mealType: String = ""

    class MealsAdapter : BaseAdapter<Meal, FoodFeedItemBinding>(
        R.layout.food_feed_item,
        object : DiffUtil.ItemCallback<Meal>() {
            override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean {
                return oldItem.id == newItem.id
            }
        }) {
        override fun inflate(inflater: LayoutInflater, parent: ViewGroup): FoodFeedItemBinding {
            return FoodFeedItemBinding.inflate(inflater, parent, false)
        }

        override fun bind(holder: ViewHolder<*>, model: Meal, pos: Int) {
            super.bind(holder, model, pos)
            holder.apply {
                binding.apply {
                    (this as FoodFeedItemBinding)
                    photoView.loadPhoto(model.image)
                    titleView.text = model.name
                    calView.text = model.calories.toString() + " kal"
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mealType = arguments?.getString("mealType") ?: ""
    }

    private val listAdapter = MealsAdapter()

    override fun viewCreated(bind: MealsFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@MealsFragment)
            toolbar.title = MealTypes.getName(mealType)
            recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
            recyclerView.adapter = listAdapter
            addButton.setOnClickListener {
                navigate(R.id.mealAddFragment)
            }
        }
        loadMeals()
    }

    private fun loadMeals() {
        val isEmpty = listAdapter.currentList.isEmpty()
        binding?.progressBar?.isVisible = isEmpty
        val last = listAdapter.currentList.lastOrNull()?.id
        DietController.loadMealsWithType(mealType, last) {
            binding?.progressBar?.isVisible = false
            listAdapter.submitList(it)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): MealsFragmentBinding {
        return MealsFragmentBinding.inflate(inflater, container, false)
    }
}