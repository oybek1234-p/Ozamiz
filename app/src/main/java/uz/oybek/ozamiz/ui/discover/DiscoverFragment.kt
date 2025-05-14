package uz.oybek.ozamiz.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.databinding.FragmentDiscoverBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class DiscoverFragment : BaseFragment<FragmentDiscoverBinding>() {

    private val viewModel: DiscoverViewModel by viewModels()
    private lateinit var workoutCategoryAdapter: WorkoutCategoryAdapter
    private lateinit var searchAdapter: WorkoutSearchAdapter

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentDiscoverBinding {
        return FragmentDiscoverBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentDiscoverBinding) {
        setupToolbar(bind)
        setupRecyclerViews(bind)
        setupAdminFeatures(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayoutDiscover.setOnRefreshListener {
            viewModel.loadInitialData(isRefresh = true)
        }
    }

    private fun setupToolbar(bind: FragmentDiscoverBinding) {

    }

    private fun setupRecyclerViews(bind: FragmentDiscoverBinding) {
        // Asosiy kategoriya va workout listi uchun adapter
        workoutCategoryAdapter =
            WorkoutCategoryAdapter(onWorkoutClick = { workout -> viewModel.onWorkoutClicked(workout.id) },
                onSeeAllClick = { category ->
                    // "See All" bosilganda WorkoutListFragment ga o'tish
                    val action =
                        DiscoverFragmentDirections.actionDiscoverFragmentToWorkoutListFragment(
                            categoryId = category.id, listTitle = category.name
                        )
                    findNavController().navigate(action)
                })
        bind.recyclerViewDiscoverMain.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutCategoryAdapter
            itemAnimator = null
        }

        // Qidiruv natijalari uchun adapter
        searchAdapter = WorkoutSearchAdapter { workout ->
            viewModel.onWorkoutClicked(workout.id) // Detal ekraniga o'tish
        }
        bind.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
            itemAnimator = null
        }
    }

    private fun setupAdminFeatures(bind: FragmentDiscoverBinding) {
        val isAdmin = LocalUser.user.admin()
        bind.fabAddDiscoverItem.isVisible = isAdmin // FAB ni ko'rsatish/yashirish

        if (isAdmin) {
            bind.fabAddDiscoverItem.setOnClickListener {
                showAdminAddOptionsDialog()
            }
        }
    }

    private fun showAdminAddOptionsDialog() {
        val options = arrayOf("Workout Qo'shish", "Kategoriya Qo'shish", "Mashq Qo'shish")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Admin Amallari")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.action_discoverFragment_to_addEditWorkoutFragment) // Workout qo'shish
                    1 -> findNavController().navigate(R.id.action_discoverFragment_to_addEditWorkoutCategoryFragment) // Kategoriya qo'shish
                    2 -> findNavController().navigate(R.id.action_discoverFragment_to_addEditExerciseFragment) // Mashq qo'shish
                }
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel, null).show()
    }


    private fun observeViewModel(bind: FragmentDiscoverBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.progressBarDiscover.isVisible = state.isOverallLoading
                    bind.swipeRefreshLayoutDiscover.isRefreshing = state.isRefreshing

                    // Qidiruv holatini tekshirish
                    val isSearching = state.searchResults != null
                    bind.recyclerViewDiscoverMain.isVisible =
                        !isSearching && !state.isOverallLoading
                    bind.recyclerViewSearchResults.isVisible =
                        isSearching && !state.isLoadingWorkouts // Qidiruv yuklanmayotgan bo'lsa
                    bind.textViewEmptyDiscover.isVisible = false // Default yopiq

                    if (isSearching) {
                        searchAdapter.submitList(state.searchResults)
                        bind.textViewEmptyDiscover.isVisible =
                            state.searchResults?.isEmpty() == true && !state.isLoadingWorkouts
                        bind.textViewEmptyDiscover.text =
                            getString(R.string.search_no_results) // Qidiruv uchun xabar
                    } else {
                        // Kategoriyalar va workoutlarni yig'ish
                        val categorySections = state.categories.map { category ->
                            category to (state.workoutsByCategory[category.id] ?: emptyList())
                        }
                            .filter { it.second.isNotEmpty() || state.isLoadingWorkouts } // Faqat workouti bor yoki yuklanayotganlarni ko'rsatish

                        workoutCategoryAdapter.submitList(categorySections)
                        bind.textViewEmptyDiscover.isVisible =
                            categorySections.isEmpty() && !state.isOverallLoading && !state.isRefreshing
                        bind.textViewEmptyDiscover.text =
                            getString(R.string.no_workouts_found) // Umumiy xabar
                    }

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearError()
                    }
                }
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (findNavController().currentDestination?.id != R.id.discoverFragment) return@let

                when (navEvent) {
                    is DiscoverNavigationEvent.NavigateToWorkoutDetail -> {
                        val action =
                            DiscoverFragmentDirections.actionDiscoverFragmentToWorkoutDetailFragment(
                                navEvent.workoutId
                            )
                        findNavController().navigate(action)
                    }

                    else -> {}
                }
            }
        }
    }

}