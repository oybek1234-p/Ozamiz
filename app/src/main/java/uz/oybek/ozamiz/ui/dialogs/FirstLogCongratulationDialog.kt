package uz.oybek.ozamiz.ui.dialogs // Or your preferred dialogs package

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import uz.oybek.ozamiz.databinding.DialogFirstLogCongratulationBinding

class FirstLogCongratulationDialog : DialogFragment() {

    private var _binding: DialogFirstLogCongratulationBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "FirstLogCongratulationDialog"
        fun newInstance(): FirstLogCongratulationDialog {
            return FirstLogCongratulationDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFirstLogCongratulationBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent) // For rounded corners
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        binding.buttonCongratsOk.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}