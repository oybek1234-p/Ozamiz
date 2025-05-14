package uz.oybek.ozamiz.ui.foods

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
import uz.oybek.ozamiz.databinding.FragmentSimpleMealListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class SimpleMealListFragment : BaseFragment<FragmentSimpleMealListBinding>() {

    private val viewModel: SimpleMealListViewModel by viewModels()
    private val args: SimpleMealListFragmentArgs by navArgs()
    private lateinit var simpleMealListAdapter: SimpleMealListAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentSimpleMealListBinding {
        return FragmentSimpleMealListBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentSimpleMealListBinding) {
        setupToolbar(bind)
        setupRecyclerView(bind)
        setupPagination(bind)
        observeViewModel(bind)

        viewModel.loadInitialMeals(args.mealType, args.listTitle)

        bind.swipeRefreshLayoutSimpleMealList.setOnRefreshListener {
            viewModel.loadInitialMeals(args.mealType, args.listTitle, isRefresh = true)
        }
    }

    private fun setupToolbar(bind: FragmentSimpleMealListBinding) {
        bind.toolbarSimpleMealList.title = args.listTitle
        bind.toolbarSimpleMealList.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(bind: FragmentSimpleMealListBinding) {
        simpleMealListAdapter = SimpleMealListAdapter {
        }
        bind.recyclerViewSimpleMealList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = simpleMealListAdapter
            itemAnimator = null
        }
    }

    private fun setupPagination(bind: FragmentSimpleMealListBinding) {
        bind.recyclerViewSimpleMealList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                val threshold = 5

                val currentState = viewModel.uiState.value
                if (currentState.canLoadMore &&
                    !currentState.isLoadingNextPage &&
                    !currentState.isLoading &&
                    totalItemCount > 0 &&
                    totalItemCount <= (lastVisibleItemPosition + threshold)
                ) {
                    viewModel.loadNextPage(args.mealType)
                }
            }
        })
    }

    private fun observeViewModel(bind: FragmentSimpleMealListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.progressBarSimpleMealList.isVisible = state.isLoading && state.meals.isEmpty()
                    bind.swipeRefreshLayoutSimpleMealList.isRefreshing = state.isLoading && state.meals.isNotEmpty()
                    bind.textViewEmptySimpleMealList.isVisible = state.meals.isEmpty() && !state.isLoading
                    bind.toolbarSimpleMealList.title = state.listTitle

                    simpleMealListAdapter.submitList(state.meals)

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewSimpleMealList.adapter = null
        super.onDestroyView()
    }
}