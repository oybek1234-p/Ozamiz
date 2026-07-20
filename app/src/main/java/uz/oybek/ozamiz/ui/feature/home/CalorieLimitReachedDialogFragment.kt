package uz.oybek.ozamiz.ui.feature.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.databinding.DialogCalorieLimitReachedBinding
import java.util.Locale

class CalorieLimitReachedDialogFragment : DialogFragment() {

    private var _binding: DialogCalorieLimitReachedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        _binding = DialogCalorieLimitReachedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val u = user
        val tdee = DietController.calculateTDEE(u)
        val target = u.targetCalories.toDouble()
        val dailyDeficit = maxOf(0.0, tdee - target)
        val weeklyLossKg = (dailyDeficit * 7.0) / 7700.0
        val finalLoss = if (weeklyLossKg > 0.05) weeklyLossKg else 0.5
        val lossStr = String.format(Locale.US, "%.1f", finalLoss)

        binding.textDesc.text = getString(R.string.limit_congrats_desc, lossStr)
        binding.lottieConfetti.playAnimation()
    }

    private fun setupListeners() {
        binding.btnStop.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CalorieLimitReachedDialogFragment"

        fun newInstance(): CalorieLimitReachedDialogFragment {
            return CalorieLimitReachedDialogFragment()
        }
    }
}
