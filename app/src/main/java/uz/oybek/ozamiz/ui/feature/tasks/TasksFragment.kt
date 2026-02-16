package uz.oybek.ozamiz.ui.feature.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.databinding.FragmentTasksBinding
import uz.oybek.ozamiz.ui.common.dialog.TaskInfoDialog
import uz.oybek.ozamiz.data.points.model.Task

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TasksViewModel by viewModels()
    private val tasksAdapter = TasksAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeState()
    }

    private fun setupRecyclerView() {
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
        }

        tasksAdapter.setOnTaskClickListener(object : TasksAdapter.OnTaskClickListener {
            override fun onCheck(task: Task) {
                // Play animation? 
                viewModel.onTaskChecked(task)
            }

            override fun onInfo(task: Task) {
                TaskInfoDialog(requireContext(), task).show()
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.apply {
                        tvLevelName.text = state.currentLevel.name
                        val totalPointsFormatted = String.format("%,d", state.currentPoints)
                        tvTotalPoints.text = "💎 $totalPointsFormatted"
                        
                        // Level Progress
                        levelProgress.progress = (state.levelProgress * 100).toInt()
                        
                        if (state.nextLevel != null) {
                            tvNextLevelInfo.text = "Keyingi daraja: ${state.nextLevel.minPoints} ball"
                        } else {
                            tvNextLevelInfo.text = "Max Level"
                        }


                        
                        // Update Adapter
                        tasksAdapter.submitList(state.tasks, state.completedTaskIds)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
