package uz.oybek.ozamiz.ui.foods

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser // Keep if LocalUser.user.admin() is used
import uz.oybek.ozamiz.data.admin // Keep if .admin() extension is used
import uz.oybek.ozamiz.databinding.FragmentFoodListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class FoodListFragment : BaseFragment<FragmentFoodListBinding>() {

    private val viewModel: FoodListViewModel by viewModels()
    private val args: FoodListFragmentArgs by navArgs()
    private lateinit var foodListAdapter: FoodListAdapter
    private lateinit var filterCategoryAdapter: FilterCategoryAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFoodListBinding {
        return FragmentFoodListBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentFoodListBinding) {
        setupToolbar(bind)
        setupRecyclerViews(bind)
        setupPagination(bind)
        setupAdminFeatures(bind) // Setup FAB based on admin status
        observeViewModel(bind)

        viewModel.initialize(args.categoryId, args.searchQuery, args.listTitle)

        bind.swipeRefreshLayoutFoodList.setOnRefreshListener {
            viewModel.loadData(isInitialLoad = false, isRefresh = true)
        }
    }

    private fun setupToolbar(bind: FragmentFoodListBinding) {
        bind.toolbarFoodList.title = args.listTitle // Set title from arguments
        bind.toolbarFoodList.setNavigationOnClickListener { findNavController().popBackStack() }

        val searchItem = bind.toolbarFoodList.menu.findItem(R.id.action_search_foodlist)
        // Show search icon only if not already in search results mode
        searchItem?.isVisible = args.searchQuery == null

        searchItem?.let { menuItem ->
            val searchView = menuItem.actionView as? SearchView
            searchView?.queryHint = getString(R.string.search_food_hint)
            searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (!query.isNullOrBlank()) {
                        // Re-initialize ViewModel for a new search
                        viewModel.initialize(
                            null,
                            query,
                            getString(R.string.search_results_title, query)
                        )
                        searchView.clearFocus()
                        menuItem.collapseActionView() // Close the search view
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return true
                } // Handle suggestions if needed
            })
        }

        val addItem = bind.toolbarFoodList.menu.findItem(R.id.action_add_item_foodlist)
        val isAdmin = LocalUser.user.admin() // Check admin status
        addItem?.isVisible = isAdmin
        addItem?.setOnMenuItemClickListener {
            viewModel.onAddRecipeClick()
            true
        }
    }

    private fun setupRecyclerViews(bind: FragmentFoodListBinding) {
        // Food List Adapter
        foodListAdapter = FoodListAdapter { recipe ->
            viewModel.onRecipeClick(recipe)
        }
        bind.recyclerViewFoodList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = foodListAdapter
            itemAnimator = null
        }

        filterCategoryAdapter = FilterCategoryAdapter { selectedCategory ->
            viewModel.onCategorySelected(selectedCategory?.id)
        }
        bind.recyclerViewFilterCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterCategoryAdapter
            itemAnimator = null // Optional: Disable default animations
        }
    }

    private fun setupPagination(bind: FragmentFoodListBinding) {
        bind.recyclerViewFoodList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return // Only trigger on scroll down

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                val threshold = 5 // Load more when 5 items from the end are visible

                val currentState = viewModel.uiState.value
                // Check if we can load more and are not already loading
                if (currentState.canLoadMore &&
                    !currentState.isLoadingNextPage &&
                    !currentState.isLoadingInitialList && // Ensure initial load is complete
                    !currentState.isRefreshing &&         // Ensure not refreshing
                    totalItemCount > 0 &&                 // Ensure list is not empty
                    totalItemCount <= (lastVisibleItemPosition + threshold)
                ) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupAdminFeatures(bind: FragmentFoodListBinding) {
        val isAdmin = LocalUser.user.admin()
        bind.fabAddFoodListItem.isVisible = isAdmin
        bind.fabAddFoodListItem.setOnClickListener {
            viewModel.onAddRecipeClick()
        }
        val addItem = bind.toolbarFoodList.menu.findItem(R.id.action_add_item_foodlist)
        addItem?.isVisible = isAdmin
    }

    private fun observeViewModel(bind: FragmentFoodListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect the latest UI state
                viewModel.uiState.collectLatest { state ->
                    // Update Toolbar Title (in case it changes dynamically, e.g., for search)
                    bind.toolbarFoodList.title = state.listTitle

                    // Central Progress Bar for initial overall loading (recipes + categories if applicable)
                    bind.progressBarFoodList.isVisible = state.isOverallLoading

                    // Content Visibility (List and Filters)
                    val contentVisible = !state.isOverallLoading && !state.isRefreshing
                    bind.recyclerViewFoodList.isVisible = contentVisible
                    bind.frameLayoutFilter.isVisible =
                        contentVisible && state.isFilterVisible // Show filter only if not searching and content is visible

                    // Submit data to adapters
                    foodListAdapter.submitList(state.recipes)
                    if (state.isFilterVisible) {
                        filterCategoryAdapter.submitList(state.categories, state.selectedCategoryId)
                        // Scroll to the selected category if it exists and is off-screen
                        state.selectedCategoryId?.let { id ->
                            val position = state.categories.indexOfFirst { it.id == id }
                            if (position != -1) {
                                (bind.recyclerViewFilterCategories.layoutManager as? LinearLayoutManager)
                                    ?.scrollToPositionWithOffset(
                                        position,
                                        0
                                    ) // Adjust offset as needed
                            }
                        }
                    }

                    // Empty List/Search Message
                    bind.textViewEmptyList.isVisible = state.recipes.isEmpty() && contentVisible
                    bind.textViewEmptyList.text =
                        if (args.searchQuery != null) { // Use arg here as ViewModel state might update slightly later
                            getString(R.string.empty_search_message)
                        } else {
                            getString(R.string.empty_list_message)
                        }

                    // SwipeRefreshLayout state
                    bind.swipeRefreshLayoutFoodList.isRefreshing = state.isRefreshing

                    // Error Message Handling
                    state.errorMessage?.let { message ->
                        showToast(message) // Show error to the user
                        viewModel.clearError() // Clear the error state in the ViewModel
                    }
                }
            }
        }

        // Observe navigation events
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                // Prevent navigation if not currently on this fragment
                if (findNavController().currentDestination?.id != R.id.foodListFragment) return@let

                when (navEvent) {
                    is FoodListNavigationEvent.NavigateToRecipeDetail -> {
                        val action =
                            FoodListFragmentDirections.actionFoodListFragmentToRecipeDetailFragment(
                                navEvent.recipeJson
                            )
                        findNavController().navigate(action)
                    }

                    is FoodListNavigationEvent.NavigateToAddRecipe -> {
                        // Use type-safe action if destination ID is known, otherwise use resource ID
                        findNavController().navigate(R.id.action_foodListFragment_to_addEditRecipeFragment)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        // Clean up adapters to prevent memory leaks
        binding.recyclerViewFoodList.adapter = null
        binding.recyclerViewFilterCategories.adapter = null
        super.onDestroyView()
    }
}