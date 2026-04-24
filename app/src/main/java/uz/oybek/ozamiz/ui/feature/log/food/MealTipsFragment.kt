package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentMealTipsBinding
import uz.oybek.ozamiz.databinding.ItemMealTipBinding
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener

class MealTipsFragment : Fragment() {

    private var _binding: FragmentMealTipsBinding? = null
    private val binding get() = _binding!!

    private val args: MealTipsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTips()
        setupListeners()
    }

    private fun setupTips() {
        binding.layoutTipsContainer.removeAllViews()
        args.tips.forEach { tip ->
            val tipBinding = ItemMealTipBinding.inflate(layoutInflater, binding.layoutTipsContainer, false)
            tipBinding.textViewTip.text = tip
            binding.layoutTipsContainer.addView(tipBinding.root)
        }
    }

    private fun setupListeners() {
        binding.buttonGotIt.setOnSafeClickListener {
            findNavController().popBackStack(R.id.home, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
