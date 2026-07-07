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
        val mealType = arguments?.getString("mealType") ?: ""
        val timestamp = arguments?.getLong("timestamp") ?: 0L
        if (mealType.isNotBlank() && timestamp > 0L) {
            viewModel.setDateAndMeal(Date(timestamp), mealType)
        }
        setupUI(bind)
        observeData()
    }

    private fun setupUI(bind: FragmentSavedFoodsBinding) {
        bind.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        adapter = SavedFoodAdapter(
            onItemClick = { foodLog ->
                navigate(
                    R.id.analyzeResultFragment,
                    Bundle().apply {
                        putString(AnalyzeResultFragment.KEY_DESCRIPTION, foodLog.description)
                        putDouble(AnalyzeResultFragment.KEY_CALORIES, foodLog.calories.toDouble())
                        putDouble(AnalyzeResultFragment.KEY_PROTEIN, foodLog.protein ?: 0.0)
                        putDouble(AnalyzeResultFragment.KEY_FAT, foodLog.fat ?: 0.0)
                        putDouble(AnalyzeResultFragment.KEY_CARBS, foodLog.carbs ?: 0.0)
                        putString(AnalyzeResultFragment.KEY_PHOTO_URL, foodLog.photoUrl)
                        putString(AnalyzeResultFragment.KEY_MEAL_TYPE, arguments?.getString("mealType") ?: foodLog.mealType)
                        putBoolean(AnalyzeResultFragment.KEY_IS_FROM_FAVORITE, true)
                        putLong(AnalyzeResultFragment.KEY_TIMESTAMP, arguments?.getLong("timestamp") ?: System.currentTimeMillis())
                        putString(AnalyzeResultFragment.KEY_LOG_ID, foodLog.logId)
                    }
                )
            },
            onAddButtonClick = { foodLog ->
                viewModel.relogFood(foodLog)
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
