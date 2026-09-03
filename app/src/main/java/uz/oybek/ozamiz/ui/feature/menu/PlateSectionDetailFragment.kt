package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentPlateSectionDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class PlateSectionDetailFragment : BaseFragment<FragmentPlateSectionDetailBinding>() {

    private val args: PlateSectionDetailFragmentArgs by navArgs()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentPlateSectionDetailBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentPlateSectionDetailBinding) {
        bind.btnBack.setOnClickListener {
            navigateUp()
        }

        setupContent(bind)
    }

    private fun setupContent(bind: FragmentPlateSectionDetailBinding) {
        when (args.sectionType) {
            "VEGETABLES" -> {
                bind.tvToolbarTitle.text = "Sabzavotlar"
                bind.tvBenefits.text = "Vitamin A, C va K, antioksidantlar hamda kletchatka manbai. Oshqozonni to'ydiradi, lekin semirtirmaydi."
                bind.tvListTitle.text = "Tavsiya etilgan sabzavotlar"
                bind.tvFoodList.text = """
                    🍅 Pomidor
                    🥒 Bodring
                    🥬 Karam
                    🌿 Ko'katlar
                    🍆 Baqlajon
                    🫑 Bulg'or qalampiri
                    🥕 Sabzi
                    🥔 Lavlagi
                    🥦 Brokkoli
                    🥗 Ismaloq
                    🥒 Kabachki
                    🎃 Qovoq
                    🧅 Piyoz
                    🧄 Sarimsoq
                    🥬 Salat bargi
                    🌱 Seldr
                    🥦 Gulkaram
                    🪵 Turp
                    🍃 Rayhon
                    🌿 Ukrop
                    🌿 Petrushka
                    🌿 Kashnich
                    🌶 Qalampir
                    🥣 Rediska
                """.trimIndent()
                bind.tvExpertTip.text = "Kuniga kamida 400-500 gramm turli xil sabzavotlar iste'mol qilish tavsiya etiladi."
            }
            "PROTEINS" -> {
                bind.tvToolbarTitle.text = "Oqsillar"
                bind.tvBenefits.text = "Mushaklar uchun qurilish materiali bo'lib xizmat qiladi va uzoq vaqt to'qlik hissini beradi."
                bind.tvListTitle.text = "Tavsiya etilgan oqsillar"
                bind.tvFoodList.text = """
                    🥩 Mol go'shti
                    🍗 Tovuq go'shti
                    🐟 Baliq
                    🥚 Tuxum
                    🧀 Tvorog
                    🥛 Sut mahsulotlari
                    🥣 Fasol
                    🍲 No'xat
                    🥘 Mosh
                    🍲 Loviya
                    🥣 Yasmiq (Chechevitsa)
                    🦃 Kurka go'shti
                    🧀 Pishloq
                    🥛 Qatiq
                    🥜 Yong'oqlar
                    🌰 Bodom
                    🥜 Araxis
                    🍤 Krevetka
                    🍣 Kalmar
                    🌱 Soya mahsulotlari
                    🌾 Chiy urug'i
                    🌻 Kunjut
                """.trimIndent()
                bind.tvExpertTip.text = "Oqsilni har bir mahal ovqatlanishda (nonushta, tushlik, kechki ovqat) bo'lishiga harakat qiling."
            }
            "CARBS" -> {
                bind.tvToolbarTitle.text = "Uglevodlar"
                bind.tvBenefits.text = "Organizm uchun asosiy energiya manbai bo'lib, miya faoliyatini va jismoniy faollikni yaxshilaydi."
                bind.tvListTitle.text = "Tavsiya etilgan uglevodlar"
                bind.tvFoodList.text = """
                    🍚 Guruch (qo'ng'ir yoki oq)
                    🥣 Grechka
                    🥣 Suli yormasi (Ovsyanka)
                    🍝 Makaron (qattiq bug'doydan)
                    🥔 Kartoshka
                    🍞 Non (javdar yoki butun donli)
                    🍠 Batat
                    🌾 Bug'doy yormasi
                    🥣 Arpa yormasi
                    🥣 Tariq (Pšeno)
                    🍲 Kinoa
                    🍲 Bulghur
                    🌽 Makkajo'xori
                    🍎 Olma
                    🍐 Nok
                    🍑 Shaftoli
                    🍌 Banan
                    🍇 Uzum (me'yorida)
                    🍉 Tarvuz
                    🍈 Qovun
                """.trimIndent()
                bind.tvExpertTip.text = "Murakkab uglevodlarga (yormalar, butun donli mahsulotlar) ustunlik bering, ular energiyani uzoq vaqt ushlab turadi."
            }
        }
    }
}
