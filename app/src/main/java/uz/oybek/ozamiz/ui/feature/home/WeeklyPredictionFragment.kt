package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.databinding.FragmentWeeklyPredictionBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.util.Date
import java.util.Locale

class WeeklyPredictionFragment : BaseFragment<FragmentWeeklyPredictionBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWeeklyPredictionBinding {
        return FragmentWeeklyPredictionBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWeeklyPredictionBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            val u = user
            val todaySummary = LogRepository.getDailySummarySync(Date())
            if (!isAdded || isDetached || context == null) return@launch

            val target = if (u.targetCalories > 0) u.targetCalories else DietController.calculateTargetCalories(u)
            val tdee = DietController.calculateTDEE(u)
            val netConsumed = maxOf(target.toDouble(), todaySummary.consumedCalories.toDouble() - todaySummary.burnedCalories.toDouble())
            val dailyDeficit = maxOf(250.0, tdee - netConsumed)
            val weeklyLossKg = (dailyDeficit * 7.0) / 7700.0
            val formattedLoss = String.format(Locale.US, "%.1f", minOf(1.0, maxOf(0.3, weeklyLossKg)))

            bind.imgIllustration.setImageResource(R.drawable.girl_weight)
            context?.let { ctx ->
                bind.txtMessage.text = ctx.getString(R.string.weekly_prediction_screen_text, formattedLoss)
            }
        }

        bind.btnAction.setOnClickListener {
            popBackStack()
        }
    }
}
