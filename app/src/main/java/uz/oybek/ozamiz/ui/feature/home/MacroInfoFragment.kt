package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentMacroInfoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class MacroInfoFragment : BaseFragment<FragmentMacroInfoBinding>() {

    private var macroType: String = MACRO_PROTEIN

    companion object {
        const val ARG_MACRO_TYPE = "macro_type"
        const val MACRO_PROTEIN = "protein"
        const val MACRO_FAT = "fat"
        const val MACRO_CARBS = "carbs"
        const val MACRO_CALORIES = "calories"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_MACRO_TYPE)?.let {
            macroType = it
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMacroInfoBinding {
        return FragmentMacroInfoBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentMacroInfoBinding) {
        bind.toolbar.setNavigationOnClickListener { closeFragment() }
        bind.btnGotIt.setOnClickListener { closeFragment() }
        setupUI(bind)
    }

    private fun setupUI(bind: FragmentMacroInfoBinding) {
        hideExtraSections(bind)
        when (macroType) {
            MACRO_PROTEIN -> setupProteinInfo(bind)
            MACRO_FAT -> setupFatInfo(bind)
            MACRO_CARBS -> setupCarbsInfo(bind)
            MACRO_CALORIES -> setupCaloriesInfo(bind)
        }
    }

    private fun setupCaloriesInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Kaloriya (Calories)"

        bind.textMacroDescription.text =
            "Kaloriya — bu oziq-ovqatlardan oladigan energiyamiz o'lchovi. Insonning ozishi yoki semirishi yig'ilgan kaloriyalar balansiga bog'liq.\n\n" +
            "⚡ Vazn tashlash: Kunlik sarflaydiganingizdan kamroq energiya qabul qilishingiz kerak (kaloriya defitsiti).\n\n" +
            "⚖️ Me'yor: Har bir insonning o'z maqsadi (ozish / saqlash / orttirish) asosida individual kunlik ehtiyoj belgilanadi."
    }

    private fun hideExtraSections(bind: FragmentMacroInfoBinding) {
        bind.cardDailyRecommendation.visibility = android.view.View.GONE
        bind.cardWhyImportant.visibility = android.view.View.GONE
        bind.cardFoods.visibility = android.view.View.GONE
        bind.cardTips.visibility = android.view.View.GONE

        bind.imgAboutIcon.visibility = android.view.View.GONE
        bind.textAboutTitle.visibility = android.view.View.GONE
    }

    private fun setupProteinInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Oqsil (Protein)"

        bind.textMacroDescription.text =
            "Oqsil — tananing asosiy qurilish materiali. U muskullarni tiklash, immunitet tizimini mustahkamlash va to'yimlilik hissini oshirishda muhim rol o'ynaydi.\n\n" +
                    "🍗 Qaysi ovqatlarda ko'p: Go'sht (tovuq, mol go'shti), baliq, tuxum, tvorog, sut mahsulotlari, loviya va yasmiq.\n\n" +
                    "⚖️ Qancha yeyish kerak: Kuniga har 1 kg vazningiz uchun 1.5-2 gramm oqsil iste'mol qilish tavsiya etiladi. Masalan, 70 kg vaznli odam uchun 105-140 gramm oqsil kerak."
    }

    private fun setupFatInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Yog' (Fat)"

        bind.textMacroDescription.text =
            "Yog' — tananing eng katta energiya manbai. Sog'lom yog'lar gormonlar ishlab chiqarish, vitaminlarni so'rish va miya faoliyati uchun o'ta zarur.\n\n" +
                    "🥑 Qaysi ovqatlarda ko'p: Yong'oqlar, pista, avokado, zaytun yog'i, yog'li baliq (salmon) va tuxum sarig'i.\n\n" +
                    "⚖️ Qancha yeyish kerak: Kunlik kaloriyaning 25-30% qismini sog'lom yog'lar tashkil qilishi kerak. Odatda bu kuniga 50-70 gramm atrofida bo'ladi."
    }

    private fun setupCarbsInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Uglevodlar (Carbs)"

        bind.textMacroDescription.text =
            "Uglevodlar — tananing asosiy energiya manbai. Miya va muskullar ishlashi uchun uglevodlar zarur. Murakkab uglevodlar sekin hazm bo'ladi va uzoq vaqt energiya beradi.\n\n" +
                    "🍚 Qaysi ovqatlarda ko'p: Guruch, grechixa, suli yormasi (ovsyanka), kartoshka, mevalar va sabzavotlar.\n\n" +
                    "⚖️ Qancha yeyish kerak: Kunlik kaloriyaning 45-55% qismini uglevodlar tashkil qilishi tavsiya etiladi. Murakkab uglevodlarga ko'proq urg'u bering."
    }

    private fun populateFoods(foods: List<FoodItem>, bind: FragmentMacroInfoBinding) {
        bind.foodsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        foods.forEach { food ->
            val itemView = inflater.inflate(R.layout.item_macro_food, bind.foodsContainer, false)
            itemView.findViewById<TextView>(R.id.text_food_name).text = food.name
            itemView.findViewById<TextView>(R.id.text_food_amount).text = food.amount
            bind.foodsContainer.addView(itemView)
        }
    }

    data class FoodItem(val name: String, val amount: String)
}
