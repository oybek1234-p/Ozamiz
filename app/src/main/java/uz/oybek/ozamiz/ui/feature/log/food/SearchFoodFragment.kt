package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.limit.FoodLimitManager
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.FragmentSearchFoodBinding
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showKeyboard
import java.util.Date

class SearchFoodFragment : BaseFragment<FragmentSearchFoodBinding>() {

    private val viewModel: SearchFoodViewModel by viewModels()

    private val adapter by lazy {
        FoodSearchAdapter(
            onAddClick = { food, btn, progress ->
                directAdd(food, btn, progress)
            },
            onItemClick = { food ->
                showSelectionSheet(food)
            }
        )
    }

    private var mealType: String = ""
    private var timestamp: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val rawTs = it.getLong(ARG_TIMESTAMP, -1L)
            timestamp = if (rawTs > 0) rawTs else System.currentTimeMillis()
            val rawMeal = it.getString(ARG_MEAL_TYPE) ?: it.getString("meal")
            mealType = uz.oybek.ozamiz.data.food.model.MealTime.getValidMeal(rawMeal, timestamp)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSearchFoodBinding) {
        setupUI(bind)
        observeData(bind)
        viewModel.updateMealType(mealType)
        bind.inputSearchFood.showKeyboard()
        loadBanner()
    }

    private fun loadBanner() {
        if (user.isPremium()) {
            binding.bannerContainer.isVisible = false
            return
        }
        try {
            val bannerView = AdsManager.createBannerView(requireContext(), AdsConfig.BANNER_LOG_FOOD)
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.addView(bannerView)
            binding.bannerContainer.isVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupUI(bind: FragmentSearchFoodBinding) = with(bind) {
        toolbarSearchFood.setNavigationOnClickListener {
            popBackStack()
        }

        recyclerSearchResults.layoutManager = SafeLinearLayoutManager(requireContext())
        recyclerSearchResults.adapter = adapter

        adapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is LoadState.Loading
            bind.progressSearch.isVisible = isLoading
            val isEmpty = loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0
            bind.layoutEmptyState.isVisible = isEmpty
        }

        inputSearchFood.addTextChangedListener {
            viewModel.searchFoods(it.toString())
        }

        inputSearchFood.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideSoftInput(requireActivity())
                true
            } else false
        }
    }

    private fun observeData(bind: FragmentSearchFoodBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.foodPagingFlow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
                bind.recyclerSearchResults.scrollToPosition(0)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResultCount.collectLatest { count ->
                val showCount = count != null && count > 0
                bind.textViewSearchResultsCount.isVisible = showCount
                if (showCount && count != null) {
                    bind.textViewSearchResultsCount.text =
                        bind.root.context.getString(R.string.search_foods_count_format, count)
                }
            }
        }
    }

    private fun directAdd(foodItem: FoodItem, button: MaterialButton, progress: View) {
        animateClick(button)

        viewLifecycleOwner.lifecycleScope.launch {
            button.isEnabled = false
            button.setIconResource(R.drawable.check_ic)
            progress.isVisible = true

            val defaultServing = foodItem.servingSizes.firstOrNull()
            val weight = defaultServing?.grams ?: 100.0
            val weightInt = weight.toInt()
            val quantityDescription = when {
                defaultServing == null -> "${weightInt} g"
                defaultServing.description.contains("g)", ignoreCase = true) || defaultServing.description.equals("100 g", ignoreCase = true) -> defaultServing.description
                defaultServing.description.equals("gramm", ignoreCase = true) -> "${weightInt} g"
                else -> "1 ${defaultServing.description.lowercase()}"
            }

            val calories = (foodItem.caloriesPer100g * weight / 100).toInt()
            val protein = (foodItem.proteinPer100g * weight / 100)
            val fat = (foodItem.fatPer100g * weight / 100)
            val carbs = (foodItem.carbsPer100g * weight / 100)

            val log = FoodLog(
                description = "${foodItem.name} ($quantityDescription)",
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                mealType = mealType,
                aiGenerated = false,
                timestamp = System.currentTimeMillis()
            )

            viewModel.logFood(log, Date(timestamp))
            Snackbar.make(binding.root, "${foodItem.name} qo'shildi!", Snackbar.LENGTH_SHORT).show()

            delay(200)

            progress.isVisible = false
            button.setIconResource(R.drawable.add_ic)
            button.isEnabled = true
        }
    }

    private fun animateClick(view: View) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setInterpolator(DecelerateInterpolator())
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun showSelectionSheet(foodItem: FoodItem) {
        FoodSelectionBottomSheet(
            foodItem = foodItem,
            mealType = mealType,
            onLogAdded = { log ->
                viewModel.logFood(log, Date(timestamp))
                Snackbar.make(binding.root, "Qo'shildi!", Snackbar.LENGTH_SHORT).show()
                binding.inputSearchFood.setText("")
                hideSoftInput(requireActivity())
            }
        ).show(parentFragmentManager, FoodSelectionBottomSheet.TAG)
    }

    companion object {
        const val ARG_MEAL_TYPE = "mealType"
        const val ARG_TIMESTAMP = "timestamp"

        fun navigate(
            fragment: BaseFragment<*>,
            mealType: String,
            timestamp: Long
        ) {
            val bundle = Bundle().apply {
                putString(ARG_MEAL_TYPE, mealType)
                putLong(ARG_TIMESTAMP, timestamp)
            }
            fragment.navigate(
                R.id.searchFoodFragment,
                bundle
            )
        }

        fun navigate(
            navController: NavController,
            mealType: String,
            timestamp: Long
        ) {
            val bundle = Bundle().apply {
                putString(ARG_MEAL_TYPE, mealType)
                putLong(ARG_TIMESTAMP, timestamp)
            }
            try {
                navController.navigate(
                    R.id.searchFoodFragment,
                    bundle
                )
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

private class SafeLinearLayoutManager(context: android.content.Context) : LinearLayoutManager(context) {
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("SafeLayoutManager", "IndexOutOfBoundsException in RecyclerView: ${e.message}")
        }
    }
}
