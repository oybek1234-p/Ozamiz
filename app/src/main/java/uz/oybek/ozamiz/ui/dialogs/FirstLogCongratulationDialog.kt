package uz.oybek.ozamiz.ui.dialogs // Or your preferred dialogs package

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment // BottomSheetDialogFragment o'rniga DialogFragment
import androidx.fragment.app.setFragmentResult
import uz.oybek.ozamiz.databinding.DialogFirstLogCongratulationBinding

class FirstLogCongratulationDialog : DialogFragment() { //DialogFragment dan meros olamiz

    private var _binding: DialogFirstLogCongratulationBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "FirstLogCongratulationDialog"
        fun newInstance(): FirstLogCongratulationDialog {
            return FirstLogCongratulationDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFirstLogCongratulationBinding.inflate(inflater, container, false)

        // Agar sizning layout (dialog_first_log_congratulation.xml) faylingiz
        // o'zining foni va yumaloq burchaklariga ega bo'lsa, bu qatorni qoldiring.
        // Bu dialog oynasining o'zini shaffof qiladi, layout ko'rinadi.
        // Agar to'liq ekran va shaffof bo'lmagan fon kerak bo'lsa, olib tashlashingiz yoki
        // o'zgartirishingiz mumkin (masalan, ColorDrawable(Color.WHITE)).
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        binding.buttonCongratsOk.setOnClickListener {
            setFragmentResult("logCongra", Bundle())
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Dialog o'lchamlarini to'liq ekranga moslashtirish
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}