package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.FoodLogFeedbackRepository
import uz.oybek.ozamiz.databinding.DialogFoodLogFeedbackBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment

class FoodLogFeedbackDialogFragment : BaseFragment<DialogFoodLogFeedbackBinding>() {

    private var dateStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dateStr = arguments?.getString(ARG_DATE) ?: ""
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogFoodLogFeedbackBinding {
        return DialogFoodLogFeedbackBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: DialogFoodLogFeedbackBinding) {
        setupListeners()
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                popBackStack()
            }
            btnOption1.setOnClickListener { selectOption(btnOption1.text.toString()) }
            btnOption2.setOnClickListener { selectOption(btnOption2.text.toString()) }
            btnOption3.setOnClickListener { selectOption(btnOption3.text.toString()) }
            btnOption4.setOnClickListener { selectOption(btnOption4.text.toString()) }

            btnAddNow.setOnClickListener {
                popBackStack()
            }
        }
    }

    private fun selectOption(option: String) {
        lifecycleScope.launch {
            try {
                FoodLogFeedbackRepository.saveFeedback(dateStr, option)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        showEncouragement()
    }

    private fun showEncouragement() {
        binding.apply {
            layoutQuestionnaire.visibility = View.GONE
            layoutEncouragement.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val ARG_DATE = "date_str"
        const val TAG = "FoodLogFeedbackDialogFragment"
    }
}
