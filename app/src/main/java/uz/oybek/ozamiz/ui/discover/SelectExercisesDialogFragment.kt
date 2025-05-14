package uz.oybek.ozamiz.ui.discover

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.DialogSelectExercisesBinding
import uz.oybek.ozamiz.ui.showToast

class SelectExercisesDialogFragment : BottomSheetDialogFragment() {

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
                val layoutParams = bottomSheet.layoutParams
                // Match parent qilsa bo'ladi, lekin scroll ishlamay qolishi mumkin
                // layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                // bottomSheet.layoutParams = layoutParams
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
        selectableAdapter = SelectableExerciseAdapter { selectedIds ->
            binding.textViewSelectedCount.text = getString(R.string.selected_count_format, selectedIds.size)
            binding.textViewSelectedCount.isVisible = selectedIds.isNotEmpty()
        }
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
// Fayl: uz/oybek/ozamiz/ui/admin/SelectExercisesDialogFragment.kt

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // collectLatest o'rniga collect ishlatib ko'ramiz
                adminWorkoutViewModel.uiState.collect { parentState ->
                    Log.d(SelectExercisesDialogFragment.TAG, "Observing parent state: isLoading=${parentState.isLoading}, exercises=${parentState.allExercises.size}") // Log qo'shamiz

                    // Progress Bar holatini sozlash (agar dialogda bo'lsa)
                    // binding.progressBarSelectExercises.isVisible = parentState.isLoading

                    if (!parentState.isLoading) { // Faqat yuklanish tugaganda
                        if (parentState.allExercises.isNotEmpty()) {
                            Log.d(SelectExercisesDialogFragment.TAG, "Submitting original list with ${parentState.allExercises.size} exercises")
                            selectableAdapter.submitOriginalList(parentState.allExercises)
                            // Boshlang'ich tanlovni faqat bir marta o'rnatish
                            if (binding.recyclerViewAllExercises.adapter?.itemCount ?: 0 > 0 && selectableAdapter.getSelectedIds().isEmpty()) {
                                Log.d(SelectExercisesDialogFragment.TAG, "Setting initial selection: $initialSelectedIds")
                                selectableAdapter.setInitialSelection(initialSelectedIds)
                            }
                            val currentSelectedCount = selectableAdapter.getSelectedIds().size
                            binding.textViewSelectedCount.text = getString(R.string.selected_count_format, currentSelectedCount)
                            binding.textViewSelectedCount.isVisible = currentSelectedCount > 0
                            binding.recyclerViewAllExercises.isVisible = true // RecyclerView ni ko'rsatish
                        } else {
                            Log.d(SelectExercisesDialogFragment.TAG, "No exercises found in parent state after loading.")
                            binding.textViewSelectedCount.text = "Mashqlar topilmadi"
                            binding.textViewSelectedCount.isVisible = true
                            binding.recyclerViewAllExercises.isVisible = false // RecyclerView ni yashirish
                            selectableAdapter.submitList(emptyList()) // Adapterni tozalash
                        }
                    } else {
                        // Yuklanayotganda RecyclerView ni yashirish yoki progress ko'rsatish
                        binding.recyclerViewAllExercises.isVisible = false
                        binding.textViewSelectedCount.isVisible = false
                        Log.d(SelectExercisesDialogFragment.TAG,"Parent state is loading...")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewAllExercises.adapter = null
        _binding = null
    }
}