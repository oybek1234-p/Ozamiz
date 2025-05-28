package uz.oybek.ozamiz.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.databinding.FragmentDiscoverBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class DiscoverFragment : BaseFragment<FragmentDiscoverBinding>() {

    private val viewModel: DiscoverViewModel by viewModels()
    private lateinit var workoutCategoryAdapter: WorkoutCategoryAdapter
    private lateinit var videoSectionAdapter: VideoCategorySectionAdapter
    private lateinit var mainConcatAdapter: ConcatAdapter
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
        bind.toolbarDiscover.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search_discover -> true
                R.id.action_add_workout_category -> {
                    if (LocalUser.user.admin()) {
                        findNavController().navigate(R.id.action_discoverFragment_to_addEditWorkoutCategoryFragment)
                    }
                    true
                }
                R.id.action_add_workout -> {
                    if (LocalUser.user.admin()) {
                        findNavController().navigate(R.id.action_discoverFragment_to_addEditWorkoutFragment)
                    }
                    true
                }
                R.id.action_add_exercise -> {
                    if (LocalUser.user.admin()) {
                        findNavController().navigate(R.id.action_discoverFragment_to_addEditExerciseFragment)
                    }
                    true
                }
                R.id.action_add_video -> {
                    if (LocalUser.user.admin()) {
                        findNavController().navigate(R.id.action_discoverFragment_to_addEditVideoFragment)
                    }
                    true
                }
                else -> false
            }
        }

        val searchItem = bind.toolbarDiscover.menu.findItem(R.id.action_search_discover)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.queryHint = getString(R.string.search_workouts_hint)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.search(query ?: "")
                searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank() && (viewModel.uiState.value.searchResults != null || viewModel.uiState.value.videoSearchResults != null) ) {
                    viewModel.clearSearch()
                }
                return true
            }
        })
        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.clearSearch()
                return true
            }
        })
    }

    private fun setupRecyclerViews(bind: FragmentDiscoverBinding) {
        workoutCategoryAdapter = WorkoutCategoryAdapter(
            onWorkoutClick = { workout -> viewModel.onWorkoutClicked(workout.id) },
            onSeeAllClick = { category -> viewModel.onSeeAllWorkoutsClicked(category) }
        )

        videoSectionAdapter = VideoCategorySectionAdapter(
            onVideoClick = { video -> viewModel.onVideoClicked(video) },
            onSeeAllClick = { category ->
                viewModel.onSeeAllVideosClicked(category.id, category.name)
            }
        )

        mainConcatAdapter = ConcatAdapter(workoutCategoryAdapter, videoSectionAdapter)

        bind.recyclerViewDiscoverMain.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mainConcatAdapter
            itemAnimator = null
        }

        searchAdapter = WorkoutSearchAdapter { workout ->
            viewModel.onWorkoutClicked(workout.id)
        }
        bind.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
            itemAnimator = null
        }
    }

    private fun setupAdminFeatures(bind: FragmentDiscoverBinding) {
        val isAdmin = LocalUser.user.admin()
        bind.fabAddDiscoverItem.isVisible = isAdmin
        if (isAdmin) {
            bind.fabAddDiscoverItem.setOnClickListener {
                showAdminAddOptionsDialog()
            }
        }
    }

    private fun showAdminAddOptionsDialog() {
        val options = arrayOf("Workout Qo'shish", "Workout Kategoriya Qo'shish", "Mashq Qo'shish", "Video Qo'shish")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Admin Amallari")
            .setItems(options) { dialog, which ->
                val navController = findNavController()
                if (navController.currentDestination?.id != R.id.discoverFragment) return@setItems

                when (which) {
                    0 -> navController.navigate(R.id.action_discoverFragment_to_addEditWorkoutFragment)
                    1 -> navController.navigate(R.id.action_discoverFragment_to_addEditWorkoutCategoryFragment)
                    2 -> navController.navigate(R.id.action_discoverFragment_to_addEditExerciseFragment)
                    3 -> navController.navigate(R.id.action_discoverFragment_to_addEditVideoFragment)
                }
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun observeViewModel(bind: FragmentDiscoverBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // SwipeRefreshLayout'ni faqatgina refresh bo'layotganda ko'rsatamiz
                    bind.swipeRefreshLayoutDiscover.isRefreshing = state.isRefreshing

                    // Asosiy ProgressBar'ni faqatgina dastlabki yuklanishda (refresh bo'lmaganda) va qidiruv bo'lmaganda ko'rsatamiz
                    val showOverallProgress = state.isOverallLoading && !state.isRefreshing && state.searchResults == null && state.videoSearchResults == null
                    bind.progressBarDiscover.isVisible = showOverallProgress

                    val isSearching = state.searchResults != null // Yoki videoSearchResults != null (agar video qidiruvi qo'shilsa)
                    // Asosiy kontent (kategoriyalar bilan workout va videolar)
                    // Faqatgina qidiruv bo'lmaganda va umumiy yuklanish tugaganda ko'rsatiladi
                    bind.recyclerViewDiscoverMain.isVisible = !isSearching && !showOverallProgress && !state.isRefreshing

                    // Qidiruv natijalari
                    // Faqatgina qidiruv bo'lganda va qidiruv natijalari yuklanib bo'lganda (yoki yuklanmayotgan bo'lsa) ko'rsatiladi
                    bind.recyclerViewSearchResults.isVisible = isSearching && !(state.isLoadingWorkouts /* || state.isLoadingVideos */) // isLoadingVideos qidiruv uchun alohida bo'lishi kerak

                    bind.textViewEmptyDiscover.isVisible = false // Default

                    if (isSearching) {
                        searchAdapter.submitList(state.searchResults ?: emptyList())
                        bind.textViewEmptyDiscover.isVisible = (state.searchResults?.isEmpty() == true) && !state.isLoadingWorkouts
                        bind.textViewEmptyDiscover.text = getString(R.string.search_no_results)
                    } else {
                        // Agar qidiruv bo'lmasa, ViewModel'dan kelgan section'larni adapterlarga beramiz
                        // Bu yerda sectionlar allaqachon faqat kontenti borlar uchun filtrlangan bo'lishi kerak (ViewModel'da)
                        workoutCategoryAdapter.submitList(state.workoutCategorySections)
                        videoSectionAdapter.submitList(state.videoCategorySections)

                        val noContentAvailable = state.workoutCategorySections.isEmpty() && state.videoCategorySections.isEmpty()
                        // Bo'sh holat xabari faqatgina qidiruv bo'lmaganda, yuklanish tugaganda va kontent yo'q bo'lganda ko'rsatiladi
                        bind.textViewEmptyDiscover.isVisible = noContentAvailable && !showOverallProgress && !state.isRefreshing
                        if (noContentAvailable) {
                            bind.textViewEmptyDiscover.text = getString(R.string.no_workouts_found) // Yoki "Ma'lumotlar topilmadi"
                        }
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
                        val action = DiscoverFragmentDirections.actionDiscoverFragmentToWorkoutDetailFragment(navEvent.workoutId)
                        findNavController().navigate(action)
                    }
                    is DiscoverNavigationEvent.NavigateToVideoDetail -> {
                        val action = DiscoverFragmentDirections.actionDiscoverFragmentToVideoDetailFragment(videoJson = Gson().toJson(navEvent.video))
                        findNavController().navigate(action)
                    }
                    is DiscoverNavigationEvent.NavigateToCategoryWorkoutList -> {
                        val action = DiscoverFragmentDirections.actionDiscoverFragmentToWorkoutListFragment(
                            categoryId = navEvent.categoryId, listTitle = navEvent.categoryName
                        )
                        findNavController().navigate(action)
                    }
                    is DiscoverNavigationEvent.NavigateToCategoryVideoList -> {
                        val action = DiscoverFragmentDirections.actionDiscoverFragmentToVideoListFragment(
                            categoryId = navEvent.categoryId,
                            listTitle = navEvent.categoryName
                        )
                        findNavController().navigate(action)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewDiscoverMain.adapter = null
        binding.recyclerViewSearchResults.adapter = null
        super.onDestroyView()
    }
}