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
        when (macroType) {
            MACRO_PROTEIN -> setupProteinInfo(bind)
            MACRO_FAT -> setupFatInfo(bind)
            MACRO_CARBS -> setupCarbsInfo(bind)
        }
    }

    private fun setupProteinInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Oqsil (Protein)"
        bind.imgMacroIcon.setImageResource(R.drawable.gosht_ic)
        bind.textMacroTitle.text = "Oqsil (Protein)"
        bind.textMacroSubtitle.text = "Muskullar va to'qimalar uchun asosiy qurilish materiali"
        
        bind.textDailyRecommendation.text = "Vazningizning har 1 kg uchun 1.6-2.2 g oqsil iste'mol qiling.\n\n70 kg odam uchun bu 112-154 g kuniga.\n\nMashg'ulot qiladigan odamlar uchun bu ko'rsatkich yuqoriroq bo'lishi kerak — 2-2.5 g/kg."
        
        bind.textAboutTitle.text = "Oqsil nima?"
        bind.imgAboutIcon.setImageResource(R.drawable.gosht_ic)
        bind.textMacroDescription.text = "Oqsil — tananing asosiy qurilish materiali. U muskullarni tiklash, immunitet tizimini mustahkamlash va to'yimlilik hissini oshirishda muhim rol o'ynaydi.\n\nOqsil aminokislotalardan tashkil topgan. Tana 20 xil aminokislota ishlatadi, ulardan 9 tasi tashqaridan olinishi kerak — bular 'muhim' aminokislotalar deyiladi.\n\nHayvon oqsillari (go'sht, tuxum, sut) to'liq aminokislota tarkibiga ega, o'simlik oqsillari esa turli manbalardan aralashtirilishi kerak."
        
        bind.textWhyTitle.text = "Nima uchun muhim?"
        bind.textWhyDescription.text = "✅ Muskullarni qurish va tiklash\n\n✅ To'yimlilik hissini uzoq vaqt saqlash\n\n✅ Metabolizmni tezlashtirish (oqsil hazm qilish ko'proq kaloriya sarflaydi)\n\n✅ Immunitet tizimini mustahkamlash\n\n✅ Sog'lom soch, tirnoq va terini ta'minlash\n\n✅ Gormonlar va fermentlar ishlab chiqarish"

        val foods = listOf(
            FoodItem("🍗 Tovuq ko'kragi", "31g / 100g"),
            FoodItem("🥩 Mol go'shti", "26g / 100g"),
            FoodItem("🐟 Baliq (salmon)", "25g / 100g"),
            FoodItem("🥚 Tuxum", "13g / 1 dona"),
            FoodItem("🧀 Tvorog", "18g / 100g"),
            FoodItem("🫘 Loviya", "21g / 100g"),
            FoodItem("🥜 Yasmiq", "24g / 100g"),
            FoodItem("🥛 Yogurt (grek)", "10g / 100g")
        )
        populateFoods(foods, bind)

        bind.textTips.text = "💡 Har bir ovqatda oqsil bo'lishiga harakat qiling\n\n💡 Mashg'ulotdan keyin 30-60 daqiqa ichida oqsil iste'mol qiling\n\n💡 Hayvon va o'simlik oqsillarini aralashtirib yeng\n\n💡 Ertalabki nonushta uchun tuxum yoki tvorog yaxshi tanlov\n\n💡 Oqsil kam bo'lsa, mushaklar zaiflashadi va vazn yo'qotish qiyinlashadi"
    }

    private fun setupFatInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Yog' (Fat)"
        bind.imgMacroIcon.setImageResource(R.drawable.yongoq_ic)
        bind.textMacroTitle.text = "Yog' (Fat)"
        bind.textMacroSubtitle.text = "Gormonlar, energiya va vitaminlar uchun zarur"
        
        bind.textDailyRecommendation.text = "Kunlik kaloriyangizning 25-35% yog'lardan bo'lishi kerak.\n\n2000 kaloriyalik ovqatda bu 55-78 g yog'.\n\nTo'yingan yog'lar (hayvon yog'lari) 10% dan oshmasligi kerak."
        
        bind.textAboutTitle.text = "Yog' nima?"
        bind.imgAboutIcon.setImageResource(R.drawable.yongoq_ic)
        bind.textMacroDescription.text = "Yog' — tananing eng katta energiya manbai. 1 gramm yog' 9 kaloriya beradi (oqsil va uglevodlar 4 kaloriya).\n\nSog'lom yog'lar gormonlar ishlab chiqarish, A, D, E, K vitaminlarini so'rish va miya faoliyati uchun zarur.\n\nLekin ortiqcha yog' iste'moli vazn ortishiga olib keladi, chunki u juda kaloriyali."
        
        bind.textWhyTitle.text = "Nima uchun muhim?"
        bind.textWhyDescription.text = "✅ Gormonlar (testosteron, estrogen) ishlab chiqarish\n\n✅ Miya faoliyatini qo'llab-quvvatlash (miya 60% yog'dan iborat)\n\n✅ A, D, E, K vitaminlarini so'rish\n\n✅ Hujayra membranalarini qurish\n\n✅ Ichki organlarni himoya qilish\n\n✅ Teri va sochlarni sog'lom saqlash"

        val foods = listOf(
            FoodItem("🥑 Avokado", "15g / 100g"),
            FoodItem("🥜 Yong'oq", "65g / 100g"),
            FoodItem("🫒 Zaytun yog'i", "100g / 100g"),
            FoodItem("🥜 Bodom", "50g / 100g"),
            FoodItem("🥚 Tuxum sarig'i", "27g / 100g"),
            FoodItem("🐟 Salmon", "13g / 100g"),
            FoodItem("🥜 Pista", "45g / 100g"),
            FoodItem("🌱 Chia urug'lari", "31g / 100g")
        )
        populateFoods(foods, bind)

        bind.textTips.text = "💡 Trans yog'lardan (fast food, qovurilgan) qoching\n\n💡 Zaytun yog'i, avokado va yong'oq — eng yaxshi manbalar\n\n💡 Baliq haftasiga 2-3 marta yeng (omega-3 uchun)\n\n💡 Yog'li ovqatdan keyin to'yimlilik uzoq davom etadi\n\n💡 Kam yog'li parhezlar gormonal muammolarga olib kelishi mumkin"
    }

    private fun setupCarbsInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Uglevodlar (Carbs)"
        bind.imgMacroIcon.setImageResource(R.drawable.non_ic)
        bind.textMacroTitle.text = "Uglevodlar (Carbs)"
        bind.textMacroSubtitle.text = "Tana va miyaning asosiy yoqilg'isi"
        
        bind.textDailyRecommendation.text = "Kunlik kaloriyangizning 45-55% uglevodlardan bo'lishi kerak.\n\n2000 kaloriyalik ovqatda bu 225-275 g.\n\nFaol sport bilan shug'ullanuvchilar uchun bu ko'rsatkich 60% gacha bo'lishi mumkin."
        
        bind.textAboutTitle.text = "Uglevodlar nima?"
        bind.imgAboutIcon.setImageResource(R.drawable.non_ic)
        bind.textMacroDescription.text = "Uglevodlar — tananing eng tez energiya manbai. Ular oddiy (shakar, meva) va murakkab (don, sabzavotlar) turlarga bo'linadi.\n\nMurakkab uglevodlar sekin hazm bo'ladi va uzoq vaqt energiya beradi. Oddiy uglevodlar tez so'riladi va qondagi shakar darajasini keskin oshiradi.\n\nOrtiqcha uglevodlar yog' sifatida to'planadi, shuning uchun miqdorni nazorat qilish muhim."
        
        bind.textWhyTitle.text = "Nima uchun muhim?"
        bind.textWhyDescription.text = "✅ Miya faqat glyukozadan energiya oladi\n\n✅ Mushaklar uchun asosiy yoqilg'i (sport vaqtida)\n\n✅ Tolali uglevodlar hazm qilishni yaxshilaydi\n\n✅ Kayfiyat va energiya darajasini boshqaradi\n\n✅ Jigarda glikogen zaxirasini to'ldiradi\n\n✅ Vitaminlar va minerallar manbayi"

        val foods = listOf(
            FoodItem("🍚 Guruch", "28g / 100g"),
            FoodItem("🍞 Non", "49g / 100g"),
            FoodItem("🍝 Makaron", "75g / 100g"),
            FoodItem("🥔 Kartoshka", "17g / 100g"),
            FoodItem("🍌 Banan", "23g / 100g"),
            FoodItem("🫘 Suli yormasi", "66g / 100g"),
            FoodItem("🌾 Grechixa", "62g / 100g"),
            FoodItem("🍎 Olma", "14g / 100g")
        )
        populateFoods(foods, bind)

        bind.textTips.text = "💡 Oq non va shakar o'rniga to'liq don mahsulotlarini tanlang\n\n💡 Sabzavotlar — kam kaloriyali uglevodlar manbai\n\n💡 Sport oldidan murakkab uglevodlar yeng\n\n💡 Kechqurun uglevodlarni kamaytiring\n\n💡 Tolali ovqatlar (sabzavotlar, mevalar) to'yimlilikni oshiradi"
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
