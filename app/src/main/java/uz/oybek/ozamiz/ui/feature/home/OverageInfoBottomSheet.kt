package uz.oybek.ozamiz.ui.feature.home

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetOverageInfoBinding
import uz.oybek.ozamiz.ui.utils.VaznimColors

class OverageInfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOverageInfoBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOverageInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exceededTypes = arguments?.getStringArrayList(ARG_TYPES) ?: emptyList()
        val overages = (arguments?.getSerializable(ARG_OVERAGES) as? Map<*, *>)
            ?.mapKeys { it.key.toString() }
            ?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap()

        val cardsContainer = binding.dynamicCardsContainer
        val context = requireContext()

        val headerView = TextView(context).apply {
            text = "Bugungi ko'rsatkichlar ✨"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(VaznimColors.getOnSurface(context))
            gravity = Gravity.START
            setPadding(0, 0, 0, dpToPx(16))
        }
        cardsContainer.addView(headerView)

        exceededTypes.forEach { type ->
            val overageAmount = overages[type] ?: 0
            if (overageAmount > 0) {
                val card = CardView(context).apply {
                    radius = dpToPx(16).toFloat()
                    cardElevation = 0f
                    setCardBackgroundColor(VaznimColors.getSurfaceLow(context))
                    useCompatPadding = true

                    val innerLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
                    }

                    val topRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    // Ranglar va emojilar (Yumshoqroq va chiroyli)
                    val accentColor = when (type) {
                        "kaloriya" -> VaznimColors.getPrimary(context)
                        "oqsil" -> VaznimColors.getPrimary(context)
                        "yog'" -> VaznimColors.getYellow(context)
                        "uglevod" -> VaznimColors.getOrange(context)
                        else -> VaznimColors.getOutline(context)
                    }

                    val emoji = when (type) {
                        "kaloriya" -> "⚡"
                        "oqsil" -> "💪"
                        "yog'" -> "🥑"
                        "uglevod" -> "🌾"
                        else -> "📊"
                    }

                    // Motivatsion va qisqa matnlar
                    val advice = when (type) {
                        "kaloriya" -> "Bugun energiya faol bo'ldi! Bir oz ko'proq piyoda yurish balansni tiklaydi."
                        "oqsil" -> "Mushaklar uchun ajoyib quvvat! Suv ichishni unutmang."
                        "yog'" -> "Yog' me'yori bajarildi. Keyingi safar yengilroq taomlarni tanlaymiz."
                        "uglevod" -> "Uglevodlar yetarli darajada. Endi sabzavot va ko'katlarga joy ajratamiz."
                        else -> "Me'yor to'ldirildi."
                    }

                    val nameText = TextView(context).apply {
                        text = "$emoji ${type.replaceFirstChar { it.uppercase() }}"
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(VaznimColors.getOnSurface(context))
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    }
                    topRow.addView(nameText)

                    val unitStr = if (type == "kaloriya") "kkal" else "g"
                    val valueText = TextView(context).apply {
                        text = "+$overageAmount $unitStr"
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(accentColor)
                    }
                    topRow.addView(valueText)
                    innerLayout.addView(topRow)

                    val adviceText = TextView(context).apply {
                        text = advice
                        textSize = 13f
                        setTextColor(VaznimColors.getOnSurfaceVariant(context))
                        setPadding(0, dpToPx(6), 0, 0)
                    }
                    innerLayout.addView(adviceText)
                    addView(innerLayout)

                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                        setMargins(0, 0, 0, dpToPx(10))
                    }
                }
                cardsContainer.addView(card)
            }
        }

        binding.btnGotIt.setOnClickListener { dismiss() }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "OverageInfoBottomSheet"
        private const val ARG_TYPES = "exceeded_types"
        private const val ARG_OVERAGES = "overages"

        fun newInstance(exceededTypes: List<String>, overages: Map<String, Int>): OverageInfoBottomSheet {
            return OverageInfoBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_TYPES, ArrayList(exceededTypes))
                    putSerializable(ARG_OVERAGES, HashMap(overages))
                }
            }
        }
    }
}