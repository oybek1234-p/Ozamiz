package uz.oybek.ozamiz.ui.feature.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.tracker.api.DailyTrackerRepo
import uz.oybek.ozamiz.databinding.FragmentLipidsGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class LipidsGuideFragment : BaseFragment<FragmentLipidsGuideBinding>() {

    @Inject
    lateinit var dailyTrackerRepo: DailyTrackerRepo

    @Inject
    lateinit var profileRepo: ProfileRepo

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentLipidsGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentLipidsGuideBinding) {
        bind.btnBack.setOnClickListener {
            navigateUp()
        }

        loadDailyMacroData(bind)
    }

    private fun loadDailyMacroData(bind: FragmentLipidsGuideBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            dailyTrackerRepo.observeDailyCalMacros(Date()).collect { macro ->
                if (!isAdded || isDetached) return@collect
                val user = profileRepo.currentUser
                val consumed = macro.fatConsumed
                val target = if (user.targetFat > 0) user.targetFat else macro.fatTarget
                val isExceeded = macro.isFatExceeded || (target > 0 && consumed > target)

                val progress = if (target > 0) ((consumed.toFloat() / target) * 100).toInt().coerceIn(0, 100) else 0

                bind.apply {
                    progressTodayMacro.progress = progress
                    if (isExceeded) {
                        val exceededGrams = (consumed - target).toDouble()
                        textTodayMacroValue.text = getString(R.string.macro_today_exceeded_format, NutritionFormatter.gr(exceededGrams))
                        textExceededDesc.isVisible = true
                    } else {
                        val remainingGrams = (target - consumed).coerceAtLeast(0).toDouble()
                        textTodayMacroValue.text = getString(R.string.macro_today_remaining_format, NutritionFormatter.gr(remainingGrams))
                        textExceededDesc.isVisible = false
                    }
                }
            }
        }
    }
}
