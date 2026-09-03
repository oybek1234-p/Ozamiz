package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.navArgs
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWriteFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast

class WriteFoodFragment : BaseFragment<FragmentWriteFoodBinding>() {

    private val args: WriteFoodFragmentArgs by navArgs()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentWriteFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(binding: FragmentWriteFoodBinding) {
        setupUI(binding)
        setupKeyboardListener(binding)
    }

    private fun setupKeyboardListener(binding: FragmentWriteFoodBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val bottomPadding = if (imeInsets.bottom == 0) {
                0
            } else {
                maxOf(0, imeInsets.bottom - systemBars.bottom)
            }
            v.setPadding(0, 0, 0, bottomPadding)
            insets
        }
    }

    private fun setupUI(binding: FragmentWriteFoodBinding) = with(binding) {
        toolbar.setNavigationOnClickListener { popBackStack() }
        
        inputFoodDescription.postDelayed({
            inputFoodDescription.showKeyboard()
        }, 200)

        buttonAnalyze.setOnClickListener {
            val text = inputFoodDescription.text.toString().trim()
            if (text.isEmpty()) {
                showToast("Iltimos, nima yeganingizni yozing")
                return@setOnClickListener
            }

            checkLimitAndProceed {
                activity?.let { hideSoftInput(it) }
                val ts = if (args.timestamp > 0) args.timestamp else System.currentTimeMillis()
                val meal = uz.oybek.ozamiz.data.food.model.MealTime.getValidMeal(args.mealType, ts)
                val bundle = Bundle().apply {
                    putString(AnalyzeResultFragment.KEY_PENDING_DESCRIPTION, text)
                    putString(AnalyzeResultFragment.KEY_MEAL_TYPE, meal)
                    putLong(AnalyzeResultFragment.KEY_TIMESTAMP, ts)
                }
                navigate(R.id.action_writeFoodFragment_to_analyzeResultFragment, bundle)
            }
        }
    }
}
