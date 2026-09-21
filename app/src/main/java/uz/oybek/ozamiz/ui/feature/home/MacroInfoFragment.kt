package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        arguments?.let {
            macroType = it.getString(ARG_MACRO_TYPE, MACRO_PROTEIN)
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
        hideExtraSections(bind)
        setupUI(bind)
    }

    private fun setupUI(bind: FragmentMacroInfoBinding) {
        bind.cardLimitWarning.visibility = View.GONE

        when (macroType) {
            MACRO_PROTEIN -> setupProteinInfo(bind)
            MACRO_FAT -> setupFatInfo(bind)
            MACRO_CARBS -> setupCarbsInfo(bind)
            MACRO_CALORIES -> setupCaloriesInfo(bind)
            else -> setupCaloriesInfo(bind)
        }
    }

    private fun setupCaloriesInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Kaloriya"
        bind.textMacroDescription.text =
            "Kaloriya — organizmning yashashi va harakatlanishi uchun zarur bo'lgan energiya o'lchov birligidir.\n\n" +
            "Tana barcha jarayonlar: nafas olish, qon aylanishi, miya faoliyati va har qanday jismoniy harakat uchun doimiy ravishda kaloriya sarflaydi.\n\n" +
            "🔥 Asosiy qoida: Agar organizm sarflaganidan ko'proq kaloriya qabul qilsa — vazn oshadi, kamroq qabul qilsa — tana o'z zaxirasidagi yog'larni yoqib ozadi."
    }

    private fun setupProteinInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Oqsil"
        bind.textMacroDescription.text =
            "Oqsil — organizmning asosiy qurilish materialidir. U muskullar, suyaklar, teri, soch va ichki a'zolar to'qimalarini tiklash va yangilash uchun xizmat qiladi.\n\n" +
            "Shuningdek, oqsil uzoq vaqt to'qlik hissini beradi va moddalar almashinuvini yaxshilaydi.\n\n" +
            "🍗 Qaysi ovqatlarda ko'p: Go'sht (tovuq, mol go'shti), baliq, tuxum, tvorog, sut mahsulotlari, loviya, no'xat va yasmiq."
    }

    private fun setupFatInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Yog'"
        bind.textMacroDescription.text =
            "Yog'lar — organizm uchun yuqori energiya manbai bo'lib, gormonlar sintezi, vitaminlar (A, D, E, K) so'rilishi va miya faoliyati uchun juda muhimdir.\n\n" +
            "Sog'lom yog'larni muntazam iste'mol qilish tana a'zolari va terining sog'lom bo'lishini ta'minlaydi.\n\n" +
            "🥑 Qaysi ovqatlarda ko'p: Yong'oqlar, pista, avokado, zaytun yog'i, yog'li baliq va tuxum sarig'i."
    }

    private fun setupCarbsInfo(bind: FragmentMacroInfoBinding) {
        bind.toolbar.title = "Uglevodlar"
        bind.textMacroDescription.text =
            "Uglevodlar — tananing eng asosiy va tezkor energiya manbaidir. Ular miya faoliyati va harakatlar uchun asosiy quvvatni ta'minlaydi.\n\n" +
            "Murakkab uglevodlar energiyani bir tekis beradi va uzoq vaqt charchoq sezmaslikka yordam beradi.\n\n" +
            "🍚 Qaysi ovqatlarda ko'p: Guruch, grechixa, suli yormasi (ovsyanka), kartoshka, mevalar, sabzavotlar va dukkakli mahsulotlar."
    }

    private fun hideExtraSections(bind: FragmentMacroInfoBinding) {
        bind.cardDailyRecommendation.visibility = View.GONE
        bind.cardWhyImportant.visibility = View.GONE
        bind.cardFoods.visibility = View.GONE
        bind.cardTips.visibility = View.GONE

        bind.imgAboutIcon.visibility = View.GONE
        bind.textAboutTitle.visibility = View.GONE
    }
}