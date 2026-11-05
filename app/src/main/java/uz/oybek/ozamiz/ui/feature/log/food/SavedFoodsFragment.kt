package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentSavedFoodsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.common.dialog.ConfirmDeleteDialog
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class SavedFoodsFragment : BaseFragment<FragmentSavedFoodsBinding>() {

    private val viewModel: SavedFoodsViewModel by viewModels()
    private var adapter: SavedFoodAdapter? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSavedFoodsBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSavedFoodsBinding) {
        setupUI()
        observeData()
    }

    private fun setupUI() = with(binding) {
        toolbar.setNavigationOnClickListener {
            popBackStack()
        }
        val savedAdapter = SavedFoodAdapter(
            onItemClick = { food ->
                val detailsArgs = AnalyzeResultArgs.SavedFoodDetails(
                    foodName = food.name.ifBlank { food.description },
                    calories = food.calories,
                    protein = food.protein,
                    fat = food.fat,
                    carbs = food.carbs,
                    photoUrl = food.photoUrl,
                    mealType = food.mealType,
                    healthLevel = food.healthScore ?: food.healthLevel
                )
                navigate(R.id.analyzeResultFragment, detailsArgs.toBundle())
            },
            onAddClick = { food ->
                viewModel.logSavedFood(food)
            },
            onDeleteButtonClick = { food ->
                val ctx = context ?: return@SavedFoodAdapter
                ConfirmDeleteDialog.show(
                    context = ctx,
                    titleRes = R.string.log_food_delete_confirm_title,
                    messageRes = R.string.log_food_delete_confirm_msg,
                    onConfirm = {
                        viewModel.deleteSavedFood(food)
                    }
                )
            }
        )
        adapter = savedAdapter
        recyclerSavedFoods.adapter = savedAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.savedFoods.collect { foods ->
                        binding.layoutEmptySaved.isVisible = foods.isEmpty()
                        binding.recyclerSavedFoods.isVisible = foods.isNotEmpty()
                        adapter?.submitList(foods)
                    }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is SavedFoodsUiEffect.ShowToast -> {
                                context?.let { ctx -> showToast(effect.message, ctx) }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }
}
