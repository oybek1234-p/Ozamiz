package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAiFeedbackBinding

class AiFeedbackFragment : Fragment() {

    private var _binding: FragmentAiFeedbackBinding? = null
    private val binding get() = _binding!!

    private var warningKeys: List<String> = emptyList()
    private var currentIndex = 0
    private var isGoalReached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            warningKeys = it.getStringArray("warnings")?.toList() ?: emptyList()
            isGoalReached = it.getBoolean("isGoalReached", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showWarning()

        binding.btnAction.setOnClickListener {
            currentIndex++
            if (currentIndex < warningKeys.size) {
                showWarning()
            } else {
                exitFlow()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            exitFlow()
        }
    }

    private fun showWarning() {
        if (currentIndex >= warningKeys.size) return
        val key = warningKeys[currentIndex]
        when (key) {
            "limitBread" -> {
                binding.imgIllustration.setImageResource(R.drawable.girl_no_bread)
                binding.txtMessage.text = getString(R.string.warning_limit_bread_title)
                binding.btnAction.text = getString(R.string.btn_oshirmayman)
            }
            "sodaDrink" -> {
                binding.imgIllustration.setImageResource(R.drawable.girl_sad)
                binding.txtMessage.text = getString(R.string.warning_soda_drink_title)
                binding.btnAction.text = getString(R.string.btn_oshirmayman)
            }
            "heavyDinner" -> {
                binding.imgIllustration.setImageResource(R.drawable.girl_ouu)
                binding.txtMessage.text = getString(R.string.warning_heavy_dinner_title)
                binding.btnAction.text = getString(R.string.btn_tushunarli)
            }
            "limitCalorie" -> {
                binding.imgIllustration.setImageResource(R.drawable.girl_happy)
                binding.txtMessage.text = getString(R.string.warning_limit_calorie_title)
                binding.btnAction.text = getString(R.string.btn_tushunarli)
            }
            "waterBeforeMeal" -> {
                binding.imgIllustration.setImageResource(R.drawable.girl_water)
                binding.txtMessage.text = getString(R.string.warning_water_before_meal_title)
                binding.btnAction.text = getString(R.string.btn_tushunarli)
            }
            else -> {
                currentIndex++
                if (currentIndex < warningKeys.size) {
                    showWarning()
                } else {
                    exitFlow()
                }
            }
        }
    }

    private fun exitFlow() {
        if (isGoalReached) {
            findNavController().navigate(R.id.action_aiFeedbackFragment_to_weeklyPredictionFragment)
        } else {
            findNavController().popBackStack(R.id.home, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
