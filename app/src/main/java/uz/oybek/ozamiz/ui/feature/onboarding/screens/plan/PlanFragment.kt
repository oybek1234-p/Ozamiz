package uz.oybek.ozamiz.ui.feature.onboarding.screens.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.databinding.FragmentPlanBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import uz.oybek.ozamiz.ui.utils.showToast

class PlanFragment : OnBoardingChildFragment<FragmentPlanBinding>() {

    override fun viewCreated(bind: FragmentPlanBinding) {
        val durationText = DietController.getGoalDurationText()
        bind.tvPlanTitle.text = getString(R.string.plan_estimated_title, durationText)

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            bind.buttonLoveIt.isEnabled = !loading
            bind.progressBarLoveIt.isVisible = loading
            bind.buttonLoveIt.text = if (loading) "" else getString(R.string.love_it)
        }

        bind.buttonLoveIt.setOnClickListener {
            savePlanAndComplete()
        }
    }

    private fun savePlanAndComplete() {
        viewModel.completeOnboarding(
            onSuccess = {
                mainActivity()?.recreateUi()
            },
            onError = {
                showRetryDialog()
            }
        )
    }

    private fun showRetryDialog() {
        if (!isAdded || isDetached) return
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xatolik yuz berdi")
            .setMessage("Ma'lumotlarni saqlashda xatolik yuz berdi. Iltimos, internet aloqangizni tekshirib, qayta urinib ko'ring.")
            .setPositiveButton("Qayta urinish") { dialog, _ ->
                dialog.dismiss()
                savePlanAndComplete()
            }
            .setNegativeButton("Bekor qilish", null)
            .setCancelable(false)
            .show()
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPlanBinding {
        return FragmentPlanBinding.inflate(
            inflater, container, false
        )
    }
}