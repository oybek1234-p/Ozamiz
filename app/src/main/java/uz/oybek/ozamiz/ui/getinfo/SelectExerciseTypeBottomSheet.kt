package uz.oybek.ozamiz.ui.getinfo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.DialogSelectExerciseTypeBinding
import uz.oybek.ozamiz.ui.showToast

class SelectExerciseTypeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogSelectExerciseTypeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ExerciseTypeAdapter
    private var addedExerciseIds: List<String> = emptyList()

    companion object {
        const val ARG_ADDED_EXERCISE_IDS = "AddedExerciseIds"
        const val TAG = "SelectExerciseTypeSheet"
        const val REQUEST_KEY = "SelectExerciseTypeRequestKey"
        const val RESULT_EXERCISE_TYPE = "ResultExerciseType"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            addedExerciseIds = it.getStringArrayList(ARG_ADDED_EXERCISE_IDS) ?: emptyList()
        }
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogSelectExerciseTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadAndFilterExerciseTypes()
    }

    private fun setupRecyclerView() {
        adapter = ExerciseTypeAdapter { selectedType ->
            setFragmentResult(REQUEST_KEY, bundleOf(
                RESULT_EXERCISE_TYPE to selectedType.id
            ))

            dismiss()
        }
        binding.recyclerViewExerciseTypes.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewExerciseTypes.adapter = adapter
    }

    private fun loadAndFilterExerciseTypes() {
        val allTypes = ExerciseType.getDefaultExerciseTypes()

        val availableTypes = allTypes.filter { type ->
            !addedExerciseIds.contains(type.id)
        }
        adapter.submitList(availableTypes)
        binding.recyclerViewExerciseTypes.isVisible = availableTypes.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewExerciseTypes.adapter = null
        _binding = null
    }
}