package uz.oybek.ozamiz.ui.feature.log.food

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentDailyLimitBinding

class DailyLimitFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentDailyLimitBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = arguments?.getString(ARG_TYPE) ?: TYPE_CALORIES
        setupContent(type)

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupContent(type: String) {
        when (type) {
            TYPE_CALORIES -> {
                binding.textTitle.isVisible = true
                binding.textTitle.text = "Tanangiz kerakli quvvatni oldi.\nEndi u yog'larni eritishga o'tadi"

                binding.card1.isVisible = true
                binding.textEmoji1.text = "🥤"
                binding.textAdvice1.text = "Suv yoki choy iching"

                binding.card2.isVisible = true
                binding.textEmoji2.text = "🥗"
                binding.textAdvice2.text = "Bodring, pomidor yoki ko'katlar"

                binding.card3.isVisible = true
                binding.textEmoji3.text = "🥚"
                binding.textAdvice3.text = "Qaynatilgan tuxum oqi"

                binding.card4.isVisible = true
                binding.textEmoji4.text = "🥛"
                binding.textAdvice4.text = "Yog'siz qatiq yoki kefir"

                binding.card5.isVisible = true
                binding.textEmoji5.text = "🥣"
                binding.textAdvice5.text = "Suyuq sabzavotli sho'rva"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DailyLimitFragment"
        const val ARG_TYPE = "arg_type"

        const val TYPE_CALORIES = "calories"

        fun newInstance(type: String = TYPE_CALORIES): DailyLimitFragment {
            return DailyLimitFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }
}