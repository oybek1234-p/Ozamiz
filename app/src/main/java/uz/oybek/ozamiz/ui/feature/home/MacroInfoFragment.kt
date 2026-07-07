package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentMacroInfoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.food.DietController
import kotlin.math.roundToInt

class MacroInfoFragment : BaseFragment<FragmentMacroInfoBinding>() {

    private var macroType: String = MACRO_PROTEIN
    private var consumedAmount: Int = 0
    private var targetAmount: Int = 0

    companion object {
        const val ARG_MACRO_TYPE = "macro_type"
        const val ARG_CONSUMED = "macro_consumed"
        const val ARG_TARGET = "macro_target"

        const val MACRO_PROTEIN = "protein"
        const val MACRO_FAT = "fat"
        const val MACRO_CARBS = "carbs"
        const val MACRO_CALORIES = "calories"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            macroType = it.getString(ARG_MACRO_TYPE, MACRO_PROTEIN)
            consumedAmount = it.getInt(ARG_CONSUMED, 0)
            targetAmount = it.getInt(ARG_TARGET, 0)
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
        val user = UserProfileRepository.user
        val targetCals = if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(user)
        val macros = if (user.goal == "gain_weight") {
            DietController.calculateMacrosByPercent(targetCals, proteinPercent = 0.25, fatPercent = 0.25, carbPercent = 0.50)
        } else {
            DietController.calculateMacrosByPercent(targetCals, proteinPercent = 0.35, fatPercent = 0.25, carbPercent = 0.40)
        }
        val targetProtein = if (user.targetProtein > 0) user.targetProtein else macros.protein
        val targetFat = if (user.targetFat > 0) user.targetFat else macros.fat
        val targetCarbs = if (user.targetCarbs > 0) user.targetCarbs else macros.carbs
        val tdee = DietController.calculateTDEE(user).roundToInt()

        val isExceeded = consumedAmount >= targetAmount && targetAmount > 0
        if (isExceeded) {
            bind.cardLimitWarning.visibility = android.view.View.VISIBLE
            bind.textWarningTitle.text = when (macroType) {
                MACRO_CALORIES -> "Bugunga yetarli, ortiqcha ovqat semirishga olib keladi."
                MACRO_PROTEIN -> "Oqsil limitiga yetdingiz!"
                MACRO_FAT -> "Yog' limitiga yetdingiz!"
                MACRO_CARBS -> "Uglevodlar limitiga yetdingiz!"
                else -> "Limit to'ldi!"
            }
            bind.textWarningDescription.text = when (macroType) {
                MACRO_CALORIES -> {
                    val diff = consumedAmount - targetAmount
                    val diffText = if (diff > 0) "$diff kal oshdi." else "limit to'ldi."
                    "Faqat suv, choy yoki yengil sabazavotli salat bilan cheklaning, $diffText"
                }
                MACRO_PROTEIN -> {
                    val diff = consumedAmount - targetAmount
                    val diffTxt = if (diff > 0) "$diff gr oshdi." else "norma to'ldi."
                    "Bugungi oqsil normangiz oshgan. Kundalik me'yordan $diffTxt\n\n✔️ Yeyish mumkin bo'lgan yeguliklar: Sabzavotli salatlar, mevalar hamda kam oqsilli yengil yeguliklar.\n\n❌ Cheklash shart bo'lgan taomlar: Tovuq go'shti, tuxum oqi, mol go'shti, baliq, tvorog va protein kokteyllari."
                }
                MACRO_FAT -> {
                    val diff = consumedAmount - targetAmount
                    val diffTxt = if (diff > 0) "$diff gr oshdi." else "norma to'ldi."
                    "Bugungi sog'lom yog' normangiz oshgan. Jigarga yuklamani oshirmaslik uchun yog'li ovqatlardan tiyiling. Kundalik me'yordan $diffTxt\n\n✔️ Yeyish mumkin bo'lgan yeguliklar: Qaynatilgan sabzavotlar, pishgan tuxum oqi, teftellar (yog'siz), tolqon va shakarsiz mevalar.\n\n❌ Cheklash shart bo'lgan taomlar: Yong'oq, pista, pista yog'i, sariyog', pishloq, fastfud hamda yog'li go'shtlar."
                }
                MACRO_CARBS -> {
                    val diff = consumedAmount - targetAmount
                    val diffTxt = if (diff > 0) "$diff gr oshdi." else "norma to'ldi."
                    "Bugungi kunlik uglevod limitini oshirib yubordingiz. Qondagi insulin darajasini barqaror ushlash uchun uglevodlarni cheklash tavsiya etiladi. Kundalik me'yordan $diffTxt\n\n✔️ Yeyish mumkin bo'lgan yeguliklar: Tuxum, qaynatilgan tovuq filesi, baliq, tvorog va ko'k salatlar (oqsil va tola).\n\n❌ Cheklash shart bo'lgan taomlar: Guruch, grechika, kartoshka, non, shirinliklar, mevalar hamda har qanday xamirli taomlar."
                }
                else -> ""
            }
        } else {
            bind.cardLimitWarning.visibility = android.view.View.GONE
        }

        when (macroType) {
            MACRO_PROTEIN -> setupProteinInfo(bind, targetProtein, user.goal == "gain_weight")
            MACRO_FAT -> setupFatInfo(bind, targetFat, user.goal == "gain_weight")
            MACRO_CARBS -> setupCarbsInfo(bind, targetCarbs, user.goal == "gain_weight")
            MACRO_CALORIES -> setupCaloriesInfo(bind, targetCals, tdee, user.goal == "gain_weight")
        }
    }

    private fun setupCaloriesInfo(bind: FragmentMacroInfoBinding, targetCals: Int, tdee: Int, isGainWeight: Boolean) {
        bind.toolbar.title = "Kaloriya"

        if (isGainWeight) {
            val diff = (targetCals - tdee).coerceAtLeast(100)
            val massGrams = (diff / 7.7).roundToInt()
            bind.textMacroDescription.text =
                "Sizning tanangiz kuniga ${tdee} kaloriya energiya sarflaydi. Sog'lom vazn orttirish uchun esa kuniga ${targetCals} kaloriya iste'mol qilish tavsiya etiladi. Bu degani siz kuniga ${diff} kaloriya ortiqcha (surplus) qilyapsiz. Ushbu qo'shimcha energiya hisobiga tanangiz yangi to'qimalarni hamda muskullarni shakllantirish uchun yetarli resursga ega bo'ladi va kuniga taxminan ${massGrams} gramm vazn to'play boshlaydi."
        } else {
            val diff = (tdee - targetCals).coerceAtLeast(100)
            val fatGrams = (diff / 7.7).roundToInt()
            bind.textMacroDescription.text =
                "Sizning tanangiz kuniga ${tdee} kaloriya energiya sarflaydi. Sog'lom ozish uchun esa kuniga ${targetCals} kaloriya iste'mol qilish tavsiya etiladi. Bu degani siz kuniga ${diff} kaloriya defitsit qilyapsiz. Ushbu defitsit hisobiga tanangiz yetishmayotgan energiyani o'z zaxirasidagi yog'lardan olishga majbur bo'ladi va kuniga taxminan ${fatGrams} gramm yog' yo'qota boshlaydi."
        }
    }

    private fun hideExtraSections(bind: FragmentMacroInfoBinding) {
        bind.cardDailyRecommendation.visibility = android.view.View.GONE
        bind.cardWhyImportant.visibility = android.view.View.GONE
        bind.cardFoods.visibility = android.view.View.GONE
        bind.cardTips.visibility = android.view.View.GONE

        bind.imgAboutIcon.visibility = android.view.View.GONE
        bind.textAboutTitle.visibility = android.view.View.GONE
    }

    private fun setupProteinInfo(bind: FragmentMacroInfoBinding, targetProtein: Int, isGainWeight: Boolean) {
        bind.toolbar.title = "Oqsil"

        if (isGainWeight) {
            bind.textMacroDescription.text =
                "Sizning kunlik oqsil me'yoringiz: ${targetProtein} gramm.\n\n" +
                "Oqsil — tananing asosiy qurilish materialidir. Vazn oshirish va yangi muskul to'qimalarini muvaffaqiyatli barpo etishda oqsil birinchi darajali ahamiyatga ega.\n\n" +
                "🍗 Qaysi ovqatlarda ko'p: Go'sht (tovuq, mol go'shti), baliq, tuxum, tvorog, sut mahsulotlari, loviya va yasmiq."
        } else {
            bind.textMacroDescription.text =
                "Sizning kunlik oqsil me'yoringiz: ${targetProtein} gramm.\n\n" +
                "Oqsil — tananing asosiy qurilish materialidir. Ayniqsa vazn tashlash jarayonida muskullarni saqlab qolish va uzoq vaqt to'q yurish uchun u juda muhim. Oqsil hazm bo'lishiga tana juda ko'p energiya yo'qotadi, bu esa ortiqcha yog'larni yoqishga yanada yordam beradi.\n\n" +
                "🍗 Qaysi ovqatlarda ko'p: Go'sht (tovuq, mol go'shti), baliq, tuxum, tvorog, sut mahsulotlari, loviya va yasmiq."
        }
    }

    private fun setupFatInfo(bind: FragmentMacroInfoBinding, targetFat: Int, isGainWeight: Boolean) {
        bind.toolbar.title = "Yog'"

        if (isGainWeight) {
            bind.textMacroDescription.text =
                "Sizning kunlik yog' me'yoringiz: ${targetFat} gramm.\n\n" +
                "Yog'lar — yuqori kaloriyali sog'lom energiya manbai hisoblanib, gormonlar sintezi va sog'lom vazn oshirish jarayonini jadallashtirish uchun zarurdir.\n\n" +
                "🥑 Qaysi ovqatlarda ko'p: Yong'oqlar, pista, avokado, zaytun yog'i, yog'li baliq va tuxum sarig'i."
        } else {
            bind.textMacroDescription.text =
                "Sizning kunlik yog' me'yoringiz: ${targetFat} gramm.\n\n" +
                "Yog'lar — gormonal barqarorlik, teri go'zalligi va hujayralar tiklanishi uchun zarurdir. Ozish jarayonida sog'lom yog'larni yetarli miqdorda yeyish tanaga ortiqcha zaxira yog'larni erkinroq safarbar qilishiga (yoqishiga) ishora bo'ladi.\n\n" +
                "🥑 Qaysi ovqatlarda ko'p: Yong'oqlar, pista, avokado, zaytun yog'i, yog'li baliq va tuxum sarig'i."
        }
    }

    private fun setupCarbsInfo(bind: FragmentMacroInfoBinding, targetCarbs: Int, isGainWeight: Boolean) {
        bind.toolbar.title = "Uglevodlar"

        if (isGainWeight) {
            bind.textMacroDescription.text =
                "Sizning kunlik uglevod me'yoringiz: ${targetCarbs} gramm.\n\n" +
                "Uglevodlar — og'ir mashg'ulotlar uchun yetarli kuch-quvvat beradi. Vazn va muskul massasini oshirish rejimida murakkab uglevodlar hisobiga doimiy energiya va tiklanish ta'minlanadi.\n\n" +
                "🍚 Qaysi ovqatlarda ko'p: Guruch, grechixa, suli yormasi, kartoshka, mevalar va sabzavotlar."
        } else {
            bind.textMacroDescription.text =
                "Sizning kunlik uglevod me'yoringiz: ${targetCarbs} gramm.\n\n" +
                "Uglevodlar — tananing asosiy quvvat manbai. Ozish jarayonida murakkab uglevodlarga e'tibor qaratsangiz, insulin darajasining keskin sakrashidan saqlanasiz va tanangiz faqat yog' yoqish rejimida ishlashda davom etadi.\n\n" +
                "🍚 Qaysi ovqatlarda ko'p: Guruch, grechixa, suli yormasi, kartoshka, mevalar va sabzavotlar."
        }
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
