package uz.oybek.ozamiz.ui.feature.menu.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAiMenuPreferencesBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class AiMenuPreferencesFragment : BaseFragment<FragmentAiMenuPreferencesBinding>() {

    private val viewModel: TodayAiMenuViewModel by viewModels()

    override fun viewCreated(bind: FragmentAiMenuPreferencesBinding) {
        bind.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        bind.toggleGroupMeals.check(R.id.btn3Meals)

        bind.btnGenerate.setOnClickListener {
            val mealsCount = when (bind.toggleGroupMeals.checkedButtonId) {
                R.id.btn2Meals -> 2
                R.id.btn5Meals -> 5
                else -> 3
            }
            val prefs = bind.editPreferences.text?.toString() ?: ""
            
            // Navigate to TodayAiMenuFragment and pass preferences
            val actionId = R.id.action_aiMenuPreferencesFragment_to_todayAiMenuFragment
            val bundle = android.os.Bundle().apply {
                putInt("mealsCount", mealsCount)
                putString("preferences", prefs)
                putBoolean("generate", true)
            }
            findNavController().navigate(actionId, bundle)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAiMenuPreferencesBinding = FragmentAiMenuPreferencesBinding.inflate(inflater, container, false)
}
