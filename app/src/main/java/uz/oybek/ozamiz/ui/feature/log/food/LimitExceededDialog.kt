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

        val type = arguments?.getString(ARG_TYPE) ?: TYPE_CALORIES
        setupContent(type)

        binding.buttonGotIt.setOnClickListener {
            dismiss()
        }
    }

    private fun setupContent(type: String) {
        when (type) {
            TYPE_CALORIES -> {
                binding.textTitle.text = "Kunlik me'yor oshdi"
                binding.textMessage.text = "Siz bugun kunlik kaloriya me'yoridan oshib ketdingiz. Lekin tushkunlikka tushmang — bu hali hammasi emas!"
                binding.textAdviceItem1.text = "🥤  1 stakan suv yoki qora choy (kaloriyasiz)"
                binding.textAdviceItem2.text = "🥗  Bodring, pomidor yoki ko'katlar (juda kam kaloriya)"
                binding.textAdviceItem3.text = "🍎  1 dona olma yoki mandarin (~50 kal)"
                binding.textAdviceFooter.text = "Kam kaloriyali narsalarni tanlab, ertaga yangi kun bilan davom eting!"
            }
            TYPE_PROTEIN -> {
                binding.textTitle.text = "Oqsil me'yori oshdi"
                
                binding.textMessage.visibility = View.GONE
                binding.textAdviceTitle.visibility = View.GONE
                binding.textAdviceFooter.visibility = View.GONE

                binding.textAdviceItem1.text = "Ortiqcha oqsilni parchalashda buyraklarga ko'maklashish uchun kunlik suv miqdorini biroz ko'paytiring."
                binding.textAdviceItem2.text = "Bugun boshqa go'sht, tuxum, pishloq va proteinli qo'shimchalarni iste'mol qilmay turing."
                binding.textAdviceItem3.text = "Oqsil organizmda yog'ga aylanishi juda qiyin jarayon, u asosan tanadagi hujayralarni yangilash va tiklashga sarflanadi."
            }
            TYPE_FAT -> {
                binding.textTitle.text = "Yog' me'yori oshdi"
                
                binding.textMessage.visibility = View.GONE
                binding.textAdviceTitle.visibility = View.GONE
                binding.textAdviceFooter.visibility = View.GONE

                binding.textAdviceItem1.text = "Jigarga og'irlik tushmasligi va yog' parchalanishi uchun limonli iliq suv yoki achchiq ko'k choy iching."
                binding.textAdviceItem2.text = "Bugun boshqa qovurilgan taom, pishloq, yong'oq yoki sariyog'li mahsulotlarni yemang."
                binding.textAdviceItem3.text = "To'g'ri miqdordagi yog'lar gormonlar va bo'g'imlar uchun foydali. Ertadan yana me'yorga qaytsangiz kifoya."
            }
            TYPE_CARBS -> {
                binding.textTitle.text = "Uglevod me'yori oshdi"

                binding.textMessage.visibility = View.GONE
                binding.textAdviceTitle.visibility = View.GONE
                binding.textAdviceFooter.visibility = View.GONE

                binding.textAdviceItem1.text = "Qondagi qandni normallashtirish va ortiqcha quvvatni \"yoqish\" uchun 20 daqiqa piyoda yuring."
                binding.textAdviceItem2.text = "Bugun boshqa non, xamir ovqat, shirinlik va hatto shirin mevalarni ham yemay turing."
                binding.textAdviceItem3.text = "Bu ortiqcha uglevod yog'ga aylanib ulgurmaydi, u ertangi mashg'ulotingiz uchun zaxira energiya (glikogen) bo'lib xizmat qiladi."
            }
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
        const val ARG_TYPE = "arg_type"

        const val TYPE_CALORIES = "calories"
        const val TYPE_PROTEIN = "protein"
        const val TYPE_FAT = "fat"
        const val TYPE_CARBS = "carbs"

        fun newInstance(type: String = TYPE_CALORIES): LimitExceededDialog {
            return LimitExceededDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }
}
