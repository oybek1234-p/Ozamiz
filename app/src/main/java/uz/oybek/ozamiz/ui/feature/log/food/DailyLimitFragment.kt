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
                binding.textTitle.text = "Bugun yetarlicha ovqatlandingiz"
                binding.textExplanation.text = "Tanangiz kerakli quvvatni oldi. Endi u yog'larni eritishga o'tadi"

                binding.card1.isVisible = true
                binding.textEmoji1.text = "🥤"
                binding.textAdvice1.text = "Suv yoki qora choy iching. Ochlikni yengadi va moddalar almashinuvini tezlashtiradi."

                binding.card2.isVisible = true
                binding.textEmoji2.text = "🥗"
                binding.textAdvice2.text = "Bodring, pomidor yoki ko'katlar — kaloriyasi yo'q, lekin qorinni to'q tutadi."

                binding.card3.isVisible = true
                binding.textEmoji3.text = "🥚"
                binding.textAdvice3.text = "1-2 ta qaynatilgan tuxum oqi. Sof oqsil mushaklar uchun ayni muddao."

                binding.card4.isVisible = true
                binding.textEmoji4.text = "🥛"
                binding.textAdvice4.text = "Bir stakan yog'siz qatiq yoki kefir. Uyqudan oldin oshqozonga yengillik beradi."

                binding.card5.isVisible = true
                binding.textEmoji5.text = "🥣"
                binding.textAdvice5.text = "Suyuq sabzavotli sho'rva. Qorinni isitadi va tinchlantiradi."
            }
            TYPE_PROTEIN -> {
                binding.textTitle.text = "Oqsil miqdori yetarli! 💪"
                binding.textExplanation.text = "Xavotirga o'rin yo'q! Ortiqcha oqsil yog'ga aylanmaydi, balki mushaklarni tiklashga yordam beradi."

                binding.card1.isVisible = true
                binding.textEmoji1.text = "🍶"
                binding.textAdvice1.text = "Ko'proq suv iching. Bu buyraklarga ortiqcha oqsilni hazm qilishda yordam beradi."

                binding.card2.isVisible = true
                binding.textEmoji2.text = "🚫"
                binding.textAdvice2.text = "Buguncha go'sht, tuxum va pishloqni to'xtatib turamiz."

                binding.card3.isVisible = true
                binding.textEmoji3.text = "ℹ️"
                binding.textAdvice3.text = "Ertadan yana odatiy rejimda davom etamiz!"

                binding.card4.isVisible = false
                binding.card5.isVisible = false
            }
            TYPE_FAT -> {
                binding.textTitle.text = "Yog' miqdori biroz oshdi"
                binding.textExplanation.text = "Foydali yog'lar gormonlar uchun muhim. Shunchaki ertaga me'yorni saqlashga harakat qilamiz."

                binding.card1.isVisible = true
                binding.textEmoji1.text = "🍵"
                binding.textAdvice1.text = "Iliq limonli suv yoki ko'k choy iching. Bu jigarga yengillik beradi."

                binding.card2.isVisible = true
                binding.textEmoji2.text = "🚫"
                binding.textAdvice2.text = "Bugun qovurilgan ovqatlar, sariyog' va yong'oqlarni chetga surib turamiz."

                binding.card3.isVisible = true
                binding.textEmoji3.text = "ℹ️"
                binding.textAdvice3.text = "Sog'lom yog'lar kerak, faqat me'yorida bo'lgani yaxshi."

                binding.card4.isVisible = false
                binding.card5.isVisible = false
            }
            TYPE_CARBS -> {
                binding.textTitle.text = "Uglevodlar biroz ko'paydi"
                binding.textExplanation.text = "Bu energiya darhol yog'ga aylanmaydi, aksincha, ertangi kun uchun ajoyib zaxira quvvat bo'ladi."

                binding.card1.isVisible = true
                binding.textEmoji1.text = "🚶"
                binding.textAdvice1.text = "20 daqiqa piyoda yuring. Bu qondagi qand miqdorini tushirib, energiyani sarflaydi."

                binding.card2.isVisible = true
                binding.textEmoji2.text = "🚫"
                binding.textAdvice2.text = "Buguncha non, xamir ovqat va shirinliklarni to'xtatib turamiz."

                binding.card3.isVisible = true
                binding.textEmoji3.text = "ℹ️"
                binding.textAdvice3.text = "Ertaga bu zaxira energiyani mashg'ulotlarda ishlatib yuboramiz!"

                binding.card4.isVisible = false
                binding.card5.isVisible = false
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
        const val TYPE_PROTEIN = "protein"
        const val TYPE_FAT = "fat"
        const val TYPE_CARBS = "carbs"

        fun newInstance(type: String = TYPE_CALORIES): DailyLimitFragment {
            return DailyLimitFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }
}