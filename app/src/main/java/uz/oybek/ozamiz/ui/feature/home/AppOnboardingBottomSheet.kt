package uz.oybek.ozamiz.ui.feature.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetAppOnboardingBinding

class AppOnboardingBottomSheet(
    private val text: String,
    private val buttonText: String,
    private val title: String = "Xush kelibsiz! ✨",
    private val onButtonClick: () -> Unit,
    private val onLaterClick: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAppOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, uz.oybek.ozamiz.R.style.BottomSheetDialogTheme)
        isCancelable = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as com.google.android.material.bottomsheet.BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.peekHeight = com.google.android.material.bottomsheet.BottomSheetBehavior.PEEK_HEIGHT_AUTO

                ViewCompat.setOnApplyWindowInsetsListener(sheet) { v, insets ->
                    val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navigationBars.bottom)
                    insets
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAppOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textEmoji.text = "\uD83D\uDE0A"
        binding.textTitle.text = title
        binding.textDescription.text = text
        binding.btnNext.text = buttonText

        binding.btnNext.setOnClickListener {
            onButtonClick()
            dismiss()
        }

        binding.btnLater.visibility = if (onLaterClick != null) View.VISIBLE else View.GONE
        binding.btnLater.setOnClickListener {
            onLaterClick?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AppOnboardingBottomSheet"
    }
}
