package uz.oybek.ozamiz.ui.feature.log.food

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentDailyLimitBinding

class DailyLimitFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentDailyLimitBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = arguments?.getString(ARG_TYPE) ?: TYPE_CALORIES
        val lastFoodTimestamp = arguments?.getLong(ARG_LAST_FOOD_TIME, 0L)?.takeIf { it > 0 }
        val remainingCalories = arguments?.getInt(ARG_REMAINING_CALORIES, 0) ?: 0

        setupContent(type, lastFoodTimestamp, remainingCalories)

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupContent(type: String, lastFoodTimestamp: Long?, remainingCalories: Int) {
        when (type) {
            TYPE_CALORIES -> {
                binding.textTitle.isVisible = true
                binding.textTitle.text = "Limitga yetdingiz"

                binding.textExplanation.text = "Bugungi kaloriya miqdori to'ldi, ovqat yemasangiz tanangiz yog' yoqishni boshlaydi."

                binding.textUnifiedAdvice.text = "🥛 Yog'siz qatiq yoki kefir\n" +
                        "🥗 Bodring, pomidor yoki ko'katlar\n" +
                        "🥚 Qaynatilgan tuxum oqi\n" +
                        "🥤 Toza suv yoki shakarsiz choy"
            }
            TYPE_REMAINING -> {
                binding.textTitle.isVisible = true
                binding.textTitle.text = "Ajoyib! 😊"
                binding.textExplanation.text = "Sizda yana $remainingCalories kaloriya ovqat qoldi. Limitdan oshirmasangiz, tanangiz yog' yoqishni boshlaydi, limitdan oshirmaslikka harakat qiling."
                binding.cardHungerAdvice.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DailyLimitFragment"
        const val ARG_TYPE = "arg_type"
        const val ARG_LAST_FOOD_TIME = "arg_last_food_time"
        const val ARG_REMAINING_CALORIES = "arg_remaining_calories"
        const val TYPE_CALORIES = "calories"
        const val TYPE_REMAINING = "remaining"

        fun newInstance(
            type: String = TYPE_CALORIES,
            lastFoodTimestamp: Long? = null,
            remainingCalories: Int? = null
        ): DailyLimitFragment {
            return DailyLimitFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                    if (lastFoodTimestamp != null) {
                        putLong(ARG_LAST_FOOD_TIME, lastFoodTimestamp)
                    }
                    if (remainingCalories != null) {
                        putInt(ARG_REMAINING_CALORIES, remainingCalories)
                    }
                }
            }
        }
    }
}