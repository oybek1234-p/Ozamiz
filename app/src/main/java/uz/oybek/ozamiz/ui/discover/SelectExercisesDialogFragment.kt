package uz.oybek.ozamiz.ui.discover

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.databinding.DialogSelectExercisesBinding
import uz.oybek.ozamiz.gson

class SelectExercisesDialogFragment : BottomSheetDialogFragment(), ExerciseItemAdminActions {

    companion object {
        const val TAG = "SelectExercisesDialog"
        const val REQUEST_KEY = "SelectExercisesRequestKey"
        const val RESULT_SELECTED_IDS = "ResultSelectedExerciseIds"
        const val ARG_INITIAL_SELECTED_IDS = "ArgInitialSelectedIds"
    }

    private var _binding: DialogSelectExercisesBinding? = null
    private val binding get() = _binding!!

    private val adminWorkoutViewModel: AddEditWorkoutViewModel by activityViewModels()

    private lateinit var selectableAdapter: SelectableExerciseAdapter
    private var initialSelectedIds: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetStyle)
        arguments?.let {
            initialSelectedIds = it.getStringArrayList(ARG_INITIAL_SELECTED_IDS)?.toList() ?: emptyList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSelectExercisesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val parentLayout = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { bottomSheet ->
                val behaviour = BottomSheetBehavior.from(bottomSheet)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                behaviour.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarSelectExercises.setNavigationOnClickListener { dismiss() }
        binding.toolbarSelectExercises.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_done_selection -> {
                    setFragmentResult(
                        REQUEST_KEY, bundleOf(
                            RESULT_SELECTED_IDS to ArrayList(selectableAdapter.getSelectedIds())
                        ))
                    dismiss()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        selectableAdapter = SelectableExerciseAdapter(
            onSelectionChanged = { selectedIds ->
                binding.textViewSelectedCount.text = getString(R.string.selected_count_format, selectedIds.size)
                binding.textViewSelectedCount.isVisible = selectedIds.isNotEmpty()
            },
            adminActionsListener = this
        )
        binding.recyclerViewAllExercises.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = selectableAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchViewExercises.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                selectableAdapter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                selectableAdapter.filter(newText)
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                adminWorkoutViewModel.uiState.collectLatest { parentState ->
                    binding.progressBarSelectExercises.isVisible = parentState.isLoading

                    if (!parentState.isLoading) {
                        if (parentState.allExercises.isNotEmpty()) {
                            selectableAdapter.submitOriginalList(parentState.allExercises)
                            if (binding.recyclerViewAllExercises.adapter?.itemCount ?: 0 > 0 && selectableAdapter.getSelectedIds().isEmpty() && initialSelectedIds.isNotEmpty() && parentState.selectedExerciseIds.containsAll(initialSelectedIds) ) {
                                selectableAdapter.setInitialSelection(initialSelectedIds)
                            } else if (binding.recyclerViewAllExercises.adapter?.itemCount ?: 0 > 0 && selectableAdapter.getSelectedIds().isEmpty() && parentState.selectedExerciseIds.isNotEmpty()){
                                selectableAdapter.setInitialSelection(parentState.selectedExerciseIds)
                            }

                            val currentSelectedCount = selectableAdapter.getSelectedIds().size
                            binding.textViewSelectedCount.text = getString(R.string.selected_count_format, currentSelectedCount)
                            binding.textViewSelectedCount.isVisible = currentSelectedCount > 0
                            binding.recyclerViewAllExercises.isVisible = true
                            binding.textViewNoExercisesAvailable.isVisible = false
                        } else {
                            binding.textViewSelectedCount.isVisible = false
                            binding.recyclerViewAllExercises.isVisible = false
                            binding.textViewNoExercisesAvailable.text = "Mashqlar topilmadi"
                            binding.textViewNoExercisesAvailable.isVisible = true
                            selectableAdapter.submitList(emptyList())
                        }
                    } else {
                        binding.recyclerViewAllExercises.isVisible = false
                        binding.textViewSelectedCount.isVisible = false
                        binding.textViewNoExercisesAvailable.text = "Yuklanmoqda..."
                        binding.textViewNoExercisesAvailable.isVisible = true
                    }
                }
            }
        }
    }

    override fun onEditExercise(exercise: Exercise) {

        val action = AddEditWorkoutFragmentDirections.actionAddEditWorkoutFragmentToAddEditExerciseFragment(
            gson!!.toJson(exercise))
        findNavController().navigate(R.id.addEditExerciseFragment,action.arguments)
       // adminWorkoutViewModel.onEditExerciseClicked(exercise)
        dismiss()
    }

    override fun onDeleteExercise(exercise: Exercise) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Mashqni o'chirish")
            .setMessage("'${exercise.name}' mashqini o'chirmoqchimisiz? Bu mashq barcha workoutlardan ham o'chiriladi.")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                adminWorkoutViewModel.onDeleteExerciseConfirmed(exercise)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewAllExercises.adapter = null
        _binding = null
    }
}