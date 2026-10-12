package uz.oybek.ozamiz.ui.feature.log.food

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.databinding.FragmentSearchFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.loadBannerAd
import uz.oybek.ozamiz.ui.utils.showAppSnackBar
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class SearchFoodFragment : BaseFragment<FragmentSearchFoodBinding>() {

    private val viewModel: SearchFoodViewModel by viewModels()

    private val adapter by lazy {
        FoodSearchAdapter(onAddClick = { food, btn, progress ->
            directAdd(food, btn, progress)
        }, onItemClick = { food ->
            showSelectionSheet(food)
        })
    }

    private var shouldScrollToTopOnSearch = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSearchFoodBinding) {
        setupUI(bind)
        observeState()
        bind.inputSearchFood.showKeyboard()
    }

    private fun setupUI(bind: FragmentSearchFoodBinding) = with(bind) {
        toolbarSearchFood.setNavigationOnClickListener {
            popBackStack()
        }

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerSearchResults.layoutManager = layoutManager
        recyclerSearchResults.setHasFixedSize(true)
        recyclerSearchResults.adapter = adapter

        adapter.addOnPagesUpdatedListener {
            if (shouldScrollToTopOnSearch) {
                recyclerSearchResults.scrollToPosition(0)
                shouldScrollToTopOnSearch = false
            }
        }

        adapter.addLoadStateListener { loadState ->
            val isRefreshLoading = loadState.refresh is LoadState.Loading
            val isInitialLoading = isRefreshLoading && adapter.itemCount == 0
            val isSearching = isRefreshLoading && adapter.itemCount > 0
            val isNotLoading = loadState.refresh is LoadState.NotLoading
            val isEmpty = isNotLoading && adapter.itemCount == 0

            bind.progressLoading.isVisible = isInitialLoading
            bind.progressSearch.isVisible = isSearching
            bind.layoutEmptyState.isVisible = isEmpty
            bind.recyclerSearchResults.isVisible = adapter.itemCount > 0
        }

        inputSearchFood.addTextChangedListener {
            val query = it?.toString().orEmpty()
            shouldScrollToTopOnSearch = true
            if (adapter.itemCount == 0) {
                bind.progressLoading.isVisible = true
                bind.layoutEmptyState.isVisible = false
            } else {
                bind.progressSearch.isVisible = true
            }
            viewModel.searchFoods(query)
        }

        inputSearchFood.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideSoftInput(requireActivity())
                true
            } else false
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUi(state)
                    }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        handleEffect(effect)
                    }
                }
                launch {
                    viewModel.foodPagingFlow.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
            }
        }
    }

    private fun renderUi(state: SearchFoodUiState) = with(binding) {
        val showCount = state.searchResultCount != null && state.searchResultCount > 0
        textViewSearchResultsCount.isVisible = showCount
        if (showCount) {
            textViewSearchResultsCount.text =
                getString(R.string.search_food_count_format, state.searchResultCount)
        }

        renderBanner(state.isPremium)
    }

    private fun renderBanner(isPremium: Boolean) {
        binding.bannerContainer.loadBannerAd(AdsConfig.BANNER_LOG_FOOD)
    }

    private fun handleEffect(effect: SearchFoodUiEffect) {
        when (effect) {
            is SearchFoodUiEffect.ShowToastRes -> {
                showToast(getString(effect.messageResId))
            }

            is SearchFoodUiEffect.ShowFoodAdded -> {
                val message = if (effect.foodName.isNotBlank()) {
                    getString(R.string.search_food_added_format, effect.foodName)
                } else {
                    getString(R.string.search_food_added_success)
                }
                showAppSnackBar(message = message)
            }
        }
    }

    private fun directAdd(
        foodItem: FoodItem, button: MaterialButton, progress: CircularProgressIndicator
    ) {
        viewModel.logDirectFood(foodItem)

        val animate = {
            button.isEnabled = false
            progress.alpha = 1f
            progress.progress = 0
            progress.visibility = View.VISIBLE

            val animator = ValueAnimator.ofInt(0, 100).apply {
                duration = 200
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    progress.progress = anim.animatedValue as Int
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        progress.animate().alpha(0f).setDuration(100).withEndAction {
                            progress.visibility = View.INVISIBLE
                            progress.alpha = 1f
                            progress.progress = 0
                        }.start()

                        button.setIconResource(R.drawable.check_ic)
                        button.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100)
                            .withEndAction {
                                button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                            }.start()
                    }
                })
            }
            animator.start()

            viewLifecycleOwner.lifecycleScope.launch {
                delay(500.milliseconds)
                button.setIconResource(R.drawable.add_ic)
                button.isEnabled = true
            }
        }
        animate()
    }

    private fun showSelectionSheet(foodItem: FoodItem) {
        val currentMealType = viewModel.uiState.value.mealType
        FoodSelectionBottomSheet(
            foodItem = foodItem, mealType = currentMealType, onLogAdded = { log ->
                viewModel.logFoodFromSelection(log)
                hideSoftInput(requireActivity())
            }).show(parentFragmentManager, null)
    }
}
