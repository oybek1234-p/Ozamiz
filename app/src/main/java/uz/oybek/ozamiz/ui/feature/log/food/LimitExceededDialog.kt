package uz.oybek.ozamiz.ui.feature.log.food

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.DialogLimitExceededBinding

class LimitExceededDialog : BottomSheetDialogFragment() {

    private var _binding: DialogLimitExceededBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogLimitExceededBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textMessage.text = "Siz bugun kunlik kaloriya me'yoridan oshib ketdingiz. Lekin tushkunlikka tushmang — bu hali hammasi emas!"

        binding.textAdviceItem1.text = buildBoldText("", "🥤  1 stakan suv yoki qora choy (kaloriyasiz)")
        binding.textAdviceItem2.text = buildBoldText("", "🥗  Bodring, pomidor yoki ko'katlar (juda kam kaloriya)")
        binding.textAdviceItem3.text = buildBoldText("", "🍎  1 dona olma yoki mandarin (~50 kal)")
        binding.textAdviceFooter.text = "Kam kaloriyali narsalarni tanlab, ertaga yangi kun bilan davom eting!"

        binding.buttonGotIt.setOnClickListener {
            dismiss()
        }
    }

    private fun buildBoldText(vararg parts: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                val start = builder.length
                builder.append(part)
                builder.setSpan(
                    StyleSpan(Typeface.BOLD), start, builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                builder.append(part)
            }
        }
        return builder
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LimitExceededDialog"

        fun newInstance(): LimitExceededDialog {
            return LimitExceededDialog()
        }
    }
}
