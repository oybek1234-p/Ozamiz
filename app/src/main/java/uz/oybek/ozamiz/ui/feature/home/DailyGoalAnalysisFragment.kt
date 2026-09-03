package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.databinding.FragmentDailyGoalAnalysisBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.util.Date
import kotlin.math.abs

class DailyGoalAnalysisFragment : BaseFragment<FragmentDailyGoalAnalysisBinding>() {

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDailyGoalAnalysisBinding {
        return FragmentDailyGoalAnalysisBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentDailyGoalAnalysisBinding) {
        bind.toolbar.setNavigationOnClickListener {
            popBackStack(R.id.home, false)
        }

        bind.btnContinue.setOnClickListener {
            popBackStack(R.id.home, false)
        }

        loadData(bind)
    }

    private fun loadData(bind: FragmentDailyGoalAnalysisBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = UserProfileRepository.user
            val isGainWeight = user.goal == "gain_weight"
            val summary = LogRepository.getDailySummarySync(Date())
            if (!isAdded || isDetached || context == null) return@launch

            val targetCals = if (user.targetCalories > 0) user.targetCalories else 2000
            val consumedCals = summary.consumedCalories
            val waterConsumed = summary.consumedWaterMl
            val waterTarget = if (user.targetWaterMl > 0) user.targetWaterMl else 2500
            val exerciseBurnt = summary.burnedCalories
            val proteinConsumed = summary.consumedProtein.toInt()
            val proteinTarget = if (user.targetProtein > 0) user.targetProtein else 90
            val exerciseTarget = 300

            // --- Har bir metrika uchun "yaxshi" belgilash ---
            val caloriePercent = if (targetCals > 0) ((consumedCals.toFloat() / targetCals) * 100).toInt().coerceIn(0, 100) else 0
            val proteinPercent = if (proteinTarget > 0) ((proteinConsumed.toFloat() / proteinTarget) * 100).toInt().coerceIn(0, 100) else 0
            val waterPercent = if (waterTarget > 0) ((waterConsumed.toFloat() / waterTarget) * 100).toInt().coerceIn(0, 100) else 0
            val exercisePercent = ((exerciseBurnt.toFloat() / exerciseTarget) * 100).toInt().coerceIn(0, 100)

            val goodCalorie = caloriePercent in 80..110  // ortiqcha emas, yetarli
            val goodProtein = proteinPercent >= 80
            val goodWater = waterPercent >= 80
            val goodExercise = exerciseBurnt >= 150

            val allGood = goodCalorie && goodProtein && goodWater && goodExercise

            bind.apply {
                // --- Sarlavha ---
                if (allGood) {
                    tvHeaderTitle.text = "Mukammal kun! \uD83C\uDF1F"
                    tvHeaderSubtitle.text = if (isGainWeight)
                        "Bugun hamma narsani to'g'ri qildingiz — kaloriya, oqsil, suv va mashq! Zo'r natija, davom eting!"
                    else
                        "Bugun kaloriya, oqsil, suv va mashq bo'yicha hammasi a'lo! Siz o'z maqsadingizga tez yetasiz!"
                } else {
                    tvHeaderTitle.text = "Bugungi maqsad bajarildi!"
                    tvHeaderSubtitle.text = if (isGainWeight)
                        "Bugungi semirish va kaloriya maqsadingiz bajarildi. Mushak hamda sog'lom vazn yig'ishda davom eting!"
                    else
                        "Bugun boshqa ovqat yemaslikka harakat qiling. Tanangiz tez orada yog' yoqish rejimiga o'tadi."
                }

                // --- Kaloriya ---
                tvCalorieLine1.text = "$consumedCals / $targetCals kkal"
                progressCalorie.progress = caloriePercent

                if (isGainWeight) {
                    val surplus = if (targetCals > 2000) targetCals - 2000 else 400
                    val monthlyKg = String.format(java.util.Locale.US, "%.1f", (surplus * 30) / 7700.0)
                    tvCalorieLine2.text = if (goodCalorie)
                        "Ajoyib! Kaloriya maqsadingiz to'g'ri bajarildi \uD83D\uDCAA"
                    else
                        "Kunlik profitsit: $surplus kkal • Oyiga ~$monthlyKg kg semirish kutiladi"
                    cardCalorieExceededWarning.isVisible = false
                } else {
                    if (consumedCals >= targetCals + 100) {
                        val exceededCals = consumedCals - targetCals
                        cardCalorieExceededWarning.isVisible = true
                        tvWarningLine1.text = "Maqsaddan $exceededCals kkal oshib ketdi"
                        tvWarningLine2.text = "Vazningiz o'zgarmasligi uchun keyingi taomlarda e'tiborli bo'ling."
                    } else {
                        cardCalorieExceededWarning.isVisible = false
                    }
                    val deficit = if (targetCals in 1..2500) abs(2200 - targetCals) else 500
                    val monthlyKg = String.format(java.util.Locale.US, "%.1f", (deficit * 30) / 7700.0)
                    tvCalorieLine2.text = if (goodCalorie)
                        "Zo'r! Bugun kaloriyaga a'lo amal qildingiz \uD83D\uDCAA"
                    else
                        "Kunlik defitsit: $deficit kkal • Oyiga ~$monthlyKg kg ozish kutiladi"
                }

                // --- Oqsil ---
                tvProteinLine1.text = "${proteinConsumed}g / ${proteinTarget}g"
                progressProtein.progress = proteinPercent
                tvProteinLine2.text = when {
                    goodProtein && proteinPercent >= 100 ->
                        "Barakalla! Oqsil me'yorini to'liq bajardingiz \uD83D\uDCAA Mushaklar yaxshi tiklanadi!"
                    goodProtein ->
                        "Yaxshi! Oqsil darajasi me'yorga yaqin — mushaklar tiklanishi uchun zo'r."
                    proteinConsumed < proteinTarget * 0.5 ->
                        "Oqsil juda kam. Tuxum, tvorog, tovuq go'shti yoki grechka qo'shing."
                    else ->
                        "Ertaga oqsilga boy ovqatlar yeng: Tuxum, tvorog, tovuq go'shti, grechka."
                }

                // --- Suv ---
                tvWaterLine1.text = "$waterConsumed ml / $waterTarget ml"
                progressWater.progress = waterPercent
                tvWaterLine2.text = when {
                    waterConsumed >= waterTarget ->
                        "Ajoyib! Suv me'yorini to'liq bajardingiz \uD83D\uDCA7 Metabolizmingiz a'lo ishlayapti!"
                    goodWater ->
                        "Yaxshi! Suv me'yoriga deyarli yetdingiz — bir-ikki qultum qoldi."
                    waterConsumed < waterTarget * 0.5 ->
                        "Suv kam ichildi. Kechga qadar yana bir-ikki stakan suv iching."
                    else ->
                        "Suv metabolizmni oshiradi va yog' parchalanishini tezlashtiradi."
                }

                // --- Mashq ---
                tvExerciseLine1.text = "$exerciseBurnt kkal yoqildi"
                progressExercise.progress = exercisePercent
                tvExerciseLine2.text = when {
                    exerciseBurnt >= exerciseTarget ->
                        "Zo'r! Bugun mukammal mashq qildingiz \uD83C\uDFC3 Yog' yoqish tezlashdi!"
                    goodExercise ->
                        "Yaxshi faollik! Jismoniy mashq mushaklar tonusini oshirdi."
                    exerciseBurnt in 1..149 ->
                        "Boshliq bo'ldi! Ertaga biroz ko'proq harakat qiling."
                    else ->
                        "15-20 daqiqa tez yurish yog' yoqish tezligini oshirishga yordam beradi."
                }
            }
        }
    }
}
