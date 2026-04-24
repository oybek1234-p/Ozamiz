package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.data.health.HealthTaskRepository
import uz.oybek.ozamiz.databinding.BottomSheetSingleTaskBinding

class SingleTaskBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSingleTaskBinding? = null
    private val binding get() = _binding!!
    
    private var onUnderstood: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSingleTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val taskId = arguments?.getString(ARG_TASK_ID) ?: return
        val task = HealthTaskRepository.getTaskById(taskId) ?: return
        
        binding.textTaskEmoji.text = task.emoji
        binding.textTaskTitle.text = task.title
        binding.textTaskBenefit.text = task.benefit
        binding.textTaskDescription.text = task.description
        
        binding.btnUnderstood.setOnClickListener {
            onUnderstood?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK_ID = "arg_task_id"
        const val TAG = "SingleTaskBottomSheet"
        
        fun newInstance(taskId: String, onUnderstood: () -> Unit) = SingleTaskBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_TASK_ID, taskId) }
            this.onUnderstood = onUnderstood
        }
    }
}
