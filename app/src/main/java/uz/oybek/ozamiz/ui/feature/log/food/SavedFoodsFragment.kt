package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentSavedFoodsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.util.Date

class SavedFoodsFragment : BaseFragment<FragmentSavedFoodsBinding>() {

    private val viewModel: LogFoodViewModel by viewModels()
    private lateinit var adapter: SavedFoodAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSavedFoodsBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSavedFoodsBinding) {
        setupUI(bind)
        observeData()
    }

    private fun setupUI(bind: FragmentSavedFoodsBinding) {
        bind.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        bind.toolbar.title = "Sevimlilar"

        adapter = SavedFoodAdapter(
            onItemClick = { foodLog ->
                navigate(
                    R.id.calorieResultBottomSheet,
                    Bundle().apply {
                        putString("description", foodLog.description)
                        putDouble("calories", foodLog.calories.toDouble())
                        putDouble("protein", foodLog.protein ?: 0.0)
                        putDouble("fat", foodLog.fat ?: 0.0)
                        putDouble("carbs", foodLog.carbs ?: 0.0)
                        putString("photoUrl", foodLog.photoUrl)
                        putString("mealType", foodLog.mealType)
                        putBoolean("isFromFavorite", true)
                    }
                )
            },
            onAddButtonClick = { foodLog ->
                viewModel.logSavedFood(foodLog)
            },
            onDeleteButtonClick = { foodLog ->
                viewModel.removeFavorite(foodLog)
            },
            layoutResId = R.layout.item_saved_food
        )
        bind.recyclerSavedFoods.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savedFoods.collectLatest { foods ->
                binding.layoutEmptySaved.isVisible = foods.isEmpty()
                binding.recyclerSavedFoods.isVisible = foods.isNotEmpty()
                adapter.submitList(foods)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collectLatest {
                if (it is LogEvent.Message) {
                    com.google.android.material.snackbar.Snackbar.make(binding.root, it.text, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadingFoodIds.collectLatest { ids ->
                adapter.updateLoadingIds(ids)
            }
        }
    }
}
