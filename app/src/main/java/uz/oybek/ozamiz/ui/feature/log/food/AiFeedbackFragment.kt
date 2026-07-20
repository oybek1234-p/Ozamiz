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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            warningKeys = it.getStringArray("warnings")?.toList() ?: emptyList()
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
                binding.txtMessage.text = getString(R.string.warning_limit_bread_title)
                binding.btnAction.text = getString(R.string.btn_oshirmayman)
            }
            "sodaDrink" -> {
                binding.txtMessage.text = getString(R.string.warning_soda_drink_title)
                binding.btnAction.text = getString(R.string.btn_oshirmayman)
            }
            "heavyDinner" -> {
                binding.txtMessage.text = getString(R.string.warning_heavy_dinner_title)
                binding.btnAction.text = getString(R.string.btn_tushunarli)
            }
            "limitCalorie" -> {
                binding.txtMessage.text = getString(R.string.warning_limit_calorie_title)
                binding.btnAction.text = getString(R.string.btn_tushunarli)
            }
            "waterBeforeMeal" -> {
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
        findNavController().popBackStack(R.id.home, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
