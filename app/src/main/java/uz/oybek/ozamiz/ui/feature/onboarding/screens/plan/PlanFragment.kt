package uz.oybek.ozamiz.ui.feature.onboarding.screens.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.FragmentPlanBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import java.text.SimpleDateFormat
import java.util.Locale

class PlanFragment : OnBoardingChildFragment<FragmentPlanBinding>() {

    val dateFormat = SimpleDateFormat(
        "dd-MMMM",
        Locale("uz")
    )

    override fun viewCreated(bind: FragmentPlanBinding) {
        val user = LocalUser.user
        val estimatedDate = DietController.calculateEstimatedGoalDate()
        val dateStr = dateFormat.format(estimatedDate)
        val targetWeight = user.targetWeight ?: 0.0
        bind.tvPlanTitle.text = getString(
            R.string.plan_estimated_title,
            dateStr,
            String.format(
                buildString { append("%.1f") },
                targetWeight
            )
        )

        bind.buttonLoveIt.setOnClickListener {
            moveToNextScreen()
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPlanBinding {
        return FragmentPlanBinding.inflate(
            inflater, container, false
        )
    }
}