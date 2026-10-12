package uz.oybek.ozamiz.ui.feature.premium

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import uz.oybek.ozamiz.R

class PremiumBottomSheetDialog : DialogFragment() {

    companion object {
        const val REQUEST_KEY = "PremiumBottomSheetRequestKey"
        const val RESULT_ACTION = "PremiumResultAction"
        const val KEY_SELECTED_PRICE = "selected_price"
        const val KEY_IS_YEARLY = "is_yearly"
        const val ACTION_GO_TO_PAYMENT = "go_to_payment"
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val navHostFragment = manager.findFragmentById(R.id.container) as? NavHostFragment
            ?: manager.fragments.firstOrNull()?.activity?.supportFragmentManager?.findFragmentById(R.id.container) as? NavHostFragment
            ?: manager.fragments.firstOrNull { it is NavHostFragment } as? NavHostFragment
        val navController = navHostFragment?.navController
        if (navController != null) {
            try {
                navController.navigate(R.id.premiumFragment)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.show(manager, tag)
    }
}