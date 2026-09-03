package uz.oybek.ozamiz.ui.feature.natija

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.BottomSheetEditWeightGoalsBinding
import uz.oybek.ozamiz.ui.utils.showToast

class EditWeightGoalsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditWeightGoalsBinding? = null
    private val binding get() = _binding!!

    var onSavedListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditWeightGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateData()
        setupListeners()
    }

    private fun populateData() {
        val user = LocalUser.user
        val startW = user.startWeight ?: user.weight
        val targetW = user.targetWeight ?: 65.0

        binding.etStartWeight.setText(
            if (startW > 0) String.format(
                java.util.Locale.US,
                "%.1f",
                startW
            ) else ""
        )
        binding.etTargetWeight.setText(
            if (targetW > 0) String.format(
                java.util.Locale.US,
                "%.1f",
                targetW
            ) else ""
        )
    }

    private fun setupListeners() {
        binding.btnSaveWeightGoals.setOnClickListener {
            val startW = binding.etStartWeight.text.toString().toDoubleOrNull()
            val targetW = binding.etTargetWeight.text.toString().toDoubleOrNull()

            if (targetW == null || targetW <= 0) {
                showToast("Maqsadli vaznni to'g'ri kiriting")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                UserProfileRepository.updateUser {
                    if (startW != null && startW > 0) this.startWeight = startW
                    this.targetWeight = targetW
                    this.startWeight = startW
                    DietController.updateTargetValues(this)
                }
                onSavedListener?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditWeightGoalsBottomSheet"
        fun newInstance(): EditWeightGoalsBottomSheet {
            return EditWeightGoalsBottomSheet()
        }
    }
}
