package uz.oybek.ozamiz.ui.feature.limit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetPremiumLimitBinding

class PremiumLimitSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPremiumLimitBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPremiumLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    var onDismissAction: (() -> Unit)? = null
    var onFoodListClick: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = arguments?.getString(ARG_TITLE)
        val message = arguments?.getString(ARG_MESSAGE)
        with(binding) {
            textLimitTitle.text = title ?: getString(R.string.ai_chat_limit_title)
            textLimitMessage.text = message ?: getString(R.string.ai_chat_limit_hint)
            btnFoodList.setOnClickListener {
                dismiss()
                onFoodListClick?.invoke()
            }
            btnOk.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissAction?.invoke()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"

        fun show(
            fragmentManager: FragmentManager,
            title: String? = null,
            message: String? = null,
            onDismissAction: (() -> Unit)? = null,
            onFoodListClick: (() -> Unit)? = null
        ) {
            val sheet = PremiumLimitSheet().apply {
                arguments = Bundle().apply {
                    if (title != null) putString(ARG_TITLE, title)
                    if (message != null) putString(ARG_MESSAGE, message)
                }
                this.onDismissAction = onDismissAction
                this.onFoodListClick = onFoodListClick
            }
            sheet.show(fragmentManager, "PremiumLimitSheet")
        }
    }
}
