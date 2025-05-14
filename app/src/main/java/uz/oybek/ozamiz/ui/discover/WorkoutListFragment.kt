package uz.oybek.ozamiz.ui.discover

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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.databinding.FragmentWorkoutListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.showToast

class WorkoutListFragment : BaseFragment<FragmentWorkoutListBinding>() {

    private val viewModel: WorkoutListViewModel by viewModels()
    private val args: WorkoutListFragmentArgs by navArgs()
    private lateinit var workoutListAdapter: WorkoutListAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentWorkoutListBinding {
        return FragmentWorkoutListBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWorkoutListBinding) {
        viewModel.initialize(args.categoryId, args.searchQuery, args.listTitle)

        setupToolbar(bind)
        setupRecyclerView(bind)
        setupAdminFeatures(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayoutWorkoutList.setOnRefreshListener {
            viewModel.loadWorkouts() // Refresh logikasi
        }
    }

    private fun setupToolbar(bind: FragmentWorkoutListBinding) {
        bind.toolbarWorkoutList.setNavigationOnClickListener { findNavController().popBackStack() }
        bind.toolbarWorkoutList.title = args.listTitle // Argumentdan sarlavhani olish

        // Admin uchun menyu
        val addItem = bind.toolbarWorkoutList.menu.findItem(R.id.action_add_item_list)
        addItem?.isVisible = LocalUser.user.admin()
        addItem?.setOnMenuItemClickListener {
            viewModel.onAddWorkoutClicked()
            true
        }
    }

    private fun setupRecyclerView(bind: FragmentWorkoutListBinding) {
        workoutListAdapter = WorkoutListAdapter { workout ->
            viewModel.onWorkoutClicked(workout.id)
        }
        bind.recyclerViewWorkoutList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutListAdapter
            itemAnimator = null
        }
    }

    private fun setupAdminFeatures(bind: FragmentWorkoutListBinding) {
        val isAdmin = LocalUser.user.admin()
        bind.fabAddWorkoutList.isVisible = isAdmin
        bind.fabAddWorkoutList.setOnClickListener { viewModel.onAddWorkoutClicked() }
    }

    private fun observeViewModel(bind: FragmentWorkoutListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        bind.progressBarWorkoutList.isVisible =
                            state.isLoading && state.workouts.isEmpty()
                        bind.swipeRefreshLayoutWorkoutList.isRefreshing =
                            state.isLoading && state.workouts.isNotEmpty()
                        bind.textViewEmptyWorkoutList.isVisible =
                            state.workouts.isEmpty() && !state.isLoading
                        bind.toolbarWorkoutList.title = state.listTitle // Dinamik o'zgarishi mumkin

                        workoutListAdapter.submitList(state.workouts)

                        state.errorMessage?.let {
                            showToast(it)
                            viewModel.clearError()
                        }
                    }
                }

                viewModel.navigationEvent.observe(viewLifecycleOwner) { event: Event<WorkoutListNavigationEvent>? ->
                    event?.getContentIfNotHandled()?.let { navEvent ->
                        if (findNavController().currentDestination?.id != R.id.workoutListFragment) return@let

                        when (navEvent) {
                            is WorkoutListNavigationEvent.NavigateToWorkoutDetail -> {
                                val action =
                                    WorkoutListFragmentDirections.actionWorkoutListFragmentToWorkoutDetailFragment(
                                        navEvent.workoutId
                                    )
                                findNavController().navigate(action)
                            }

                            is WorkoutListNavigationEvent.NavigateToAddWorkout -> {
                                // Yangi workout qo'shish ekraniga o'tish
                                // Agar kategoriya ID si bo'lsa, uni ham o'tkazish mumkin
                                val action =
                                    WorkoutListFragmentDirections.actionWorkoutListFragmentToAddEditWorkoutFragment(
                                        null
                                    ) // workoutJson null
                                findNavController().navigate(action)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewWorkoutList.adapter = null
        super.onDestroyView()
    }
}