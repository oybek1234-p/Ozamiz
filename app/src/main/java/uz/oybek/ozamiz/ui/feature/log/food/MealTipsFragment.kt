package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentMealTipsBinding
import uz.oybek.ozamiz.databinding.ItemMealTipBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener

class MealTipsFragment : BaseFragment<FragmentMealTipsBinding>() {

    init {
        showBottomSheet = false
    }

    private val args: MealTipsFragmentArgs by navArgs()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMealTipsBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentMealTipsBinding) {
        val mealType = args.mealType ?: "unknown"
        setupToolbar()
        setupTips(mealType)
        setupListeners(mealType)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupTips(mealType: String) {
        binding.layoutTipsContainer.removeAllViews()
        
        val titleText = when(mealType) {
            "breakfast" -> "Nonushta tavsiyasi"
            "lunch" -> "Tushlik tavsiyasi"
            "dinner" -> "Kechki ovqat tavsiyasi"
            "snack" -> "Perekus tavsiyasi"
            else -> "Tavsiyalar"
        }

        val tips = if (args.tips.isNotEmpty()) {
            args.tips.toList()
        } else {
            getStaticTipsForMeal(mealType)
        }

        tips.forEach { tip ->
            val tipBinding = ItemMealTipBinding.inflate(layoutInflater, binding.layoutTipsContainer, false)
            tipBinding.textViewTip.text = tip
            binding.layoutTipsContainer.addView(tipBinding.root)
        }
    }

    private fun getStaticTipsForMeal(mealType: String): List<String> {
        val commonFirstTip = "🍽️ So'g'lom tarelka qoidasiga amal qiling. Tarelkaning yarmini sabzavotlar, choragini oqsillar va qolgan choragini murakkab uglevodlar bilan to'ldiring."
        
        return when (mealType) {
            "breakfast" -> listOf(
                commonFirstTip,
                "💧 Nonushtadan 15-20 daqiqa oldin 1 stakan iliq suv ichish shart. Bu oshqozonni uyg'otadi va metabolizmni tezlashtiradi.",
                "🚫 Shirinlik va oq nonni unuting: Ertalabki shirin choy yoki pechenye qon shakarini tez ko'taradi va tezda ochqab qolasiz.",
                "🍳 Oqsilga boy nonushta tanlang. Tuxum, tvorog yoki pishloq sizni uzoq vaqt to'q tutadi va kun davomida kamroq ovqat yeyishingizga yordam beradi.",
                "🥜 Yong'oq va urug'lar qo'shing. Ozgina bodom yoki zig'ir urug'i nonushtani foydali yog'lar bilan boyitadi.",
                "☕ Kofe yoki choyni shakarsiz ichishga odatlaning. Shakar qo'shilgan ichimliklar vazn yo'qotishga to'sqinlik qiladi."
            )
            "lunch" -> listOf(
                commonFirstTip,
                "🥩 Tushlikda oqsil (go'sht, baliq) va kletchatka (sabzavotlar) balansiga e'tibor bering. Bu energiya darajasini barqaror ushlab turadi.",
                "⏳ Ovqatdan keyin darhol choy ichmang, kamida 30 daqiqa kuting. Bu ovqatdan temir so'rilishini yaxshilaydi.",
                "🐢 Ovqatni yaxshilab chaynab yeng. Miya to'qlik signalini qabul qilishi uchun kamida 20 daqiqa vaqt kerak bo'ladi.",
                "🥣 Sho'rvalarni afzal ko'ring. Sabzavotli sho'rvalar past kaloriyali bo'lib, oshqozonni yaxshi to'ldiradi.",
                "🚶 Tushlikdan so'ng qisqa sayr qiling. 10 daqiqalik harakat qon shakarini normallashtirishga yordam beradi."
            )
            "dinner" -> listOf(
                commonFirstTip,
                "⏰ Kechki ovqat uxlashdan kamida 3-4 soat oldin bo'lishi kerak. Bu organizmga ovqatni hazm qilishga ulgurish imkonini beradi.",
                "🐟 Yengil hazm bo'ladigan oqsillar va sabzavotlarni tanlang. Bug'da pishgan baliq yoki tovuq go'shti ideal tanlovdir.",
                "📉 Uglevodlarni kechki payt kamaytiring. Kechasi organizm energiyani kam sarflaydi va ortiqcha uglevodlar yog' bo'lib to'planishi mumkin.",
                "🧂 Tuz miqdorini kamaytiring. Kechki payt sho'r ovqatlar eyish organizmda suv ushlanib qolishiga va shishlarga sabab bo'ladi.",
                "🥛 Kechki ovqatdan so'ng hech narsa yemang. Agar juda ochqasangiz, bir stakan kefir ichish bilan cheklaning."
            )
            "snack" -> listOf(
                "🍎 Perekus uchun meva yoki yong'oqlarni afzal ko'ring. Bir siqim bodom yoki olma asosiy ovqatgacha och qolmasligingizni ta'minlaydi.",
                "⚖️ Porsiya hajmini nazorat qiling. Perekus kaloriyasi 150-200 kkal dan oshmasligi tavsiya etiladi.",
                "🍦 Tabiiy yogurt yoki tvorog iste'mol qiling. Qo'shimcha shakarsiz mahsulotlar oqsil manbai bo'lib, mushaklar uchun foydalidir.",
                "🚰 Suv ichishni unutmang. Ba'zida organizm chanqoqni ochlik bilan adashtiradi, shuning uchun suv ichib ko'ring.",
                "🍇 Tayyor shirinliklar o'rniga quritilgan mevalar tanlang. Turshak yoki mayiz shirinlikka bo'lgan ehtiyojni qondiradi."
            )
            else -> listOf(
                commonFirstTip,
                "🏃 Kun davomida ko'proq harakat qiling.",
                "🥤 Suv balansini saqlang.",
                "😴 Uyqu rejimiga amal qiling."
            )
        }
    }

    private fun setupListeners(mealType: String) {
        binding.buttonGotIt.text = when(mealType) {
            "breakfast" -> "Nonushta menyulari"
            "lunch" -> "Tushlik menyulari"
            "dinner" -> "Kechki ovqat menyulari"
            "snack" -> "Perekus menyulari"
            else -> "Menyularni ko'rish"
        }
        binding.buttonGotIt.setOnSafeClickListener {
            val filterValue = when(mealType) {
                "breakfast" -> "BREAKFAST"
                "lunch" -> "LUNCH"
                "dinner" -> "DINNER"
                "snack" -> "SNACK"
                else -> ""
            }
            if (filterValue.isNotEmpty()) {
                findNavController().navigate(R.id.menuSearchResultsFragment, Bundle().apply {
                    putString("filterType", "MAIN_MEALS")
                    putString("filterValue", filterValue)
                })
            } else {
                findNavController().popBackStack(R.id.home, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingT = null
    }
}
