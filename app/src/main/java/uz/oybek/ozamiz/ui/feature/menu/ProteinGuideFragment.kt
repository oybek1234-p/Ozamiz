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
import uz.oybek.ozamiz.databinding.FragmentProteinGuideBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class ProteinGuideFragment : BaseFragment<FragmentProteinGuideBinding>() {

    @Inject
    lateinit var dailyTrackerRepo: DailyTrackerRepo

    @Inject
    lateinit var profileRepo: ProfileRepo

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentProteinGuideBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentProteinGuideBinding) {
        bind.btnBack.setOnClickListener {
            navigateUp()
        }

        loadDailyMacroData(bind)
    }

    private fun loadDailyMacroData(bind: FragmentProteinGuideBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            dailyTrackerRepo.observeDailyCalMacros(Date()).collect { macro ->
                if (!isAdded || isDetached) return@collect
                val user = profileRepo.currentUser
                val consumed = macro.proteinConsumed
                val target = if (user.targetProtein > 0) user.targetProtein else macro.proteinTarget
                val isExceeded = macro.isProteinExceeded || (target > 0 && consumed > target)

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
