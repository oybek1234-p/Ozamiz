package uz.oybek.ozamiz.ui.feature.log.water

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.DialogLogWaterBinding
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.feature.home.HomeViewModel
import uz.oybek.ozamiz.ui.utils.showToast
import androidx.fragment.app.setFragmentResult

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class LogWaterDialog : BottomSheetDialogFragment() {

    private var _binding: DialogLogWaterBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    private val glassAmount = 250
    private val smallBottleAmount = 500
    private val largeBottleAmount = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLogWaterBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        return binding.root
    }

    private val waterTips = listOf(
        "Suv moddalar almashinuvini (metabolizm) 30% gacha tezlashtiradi.",
        "Suv terining elastikligini oshiradi va ajinlar tushishining oldini oladi.",
        "Etarlicha suv ichish bosh og'rig'i va charchoqni kamaytiradi.",
        "Suv organizmdan toksinlar va ortiqcha tuzlarni chiqarib yuboradi.",
        "Ovqatdan oldin 1 stakan suv ichish ortiqcha yeyishning oldini oladi.",
        "Suv bo'g'inlar harakatchanligini va mushaklar salomatligini ta'minlaydi.",
        "Etarli suv ichish miya faoliyatini va diqqatni jamlashni oshiradi.",
        "Suv buyraklar va ovqat hazm qilish tizimining to'g'ri ishlashiga yordam beradi."
    )

    private var currentTipIndex = (0..7).random()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateTipText()
    }

    private fun updateTipText() {
        if (_binding != null) {
            val tip = waterTips[currentTipIndex % waterTips.size]
            currentTipIndex++
            binding.tvWaterTip.text = tip
        }
    }

    private fun animateButton(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(70)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(90)
                    .withEndAction { onEnd() }
                    .start()
            }
            .start()
    }

    private fun setupClickListeners() {
        binding.apply {
            cardGlass250.setOnClickListener { animateButton(it) { addWater(250) } }
            cardBottle500.setOnClickListener { animateButton(it) { addWater(500) } }
            cardBottle1000.setOnClickListener { animateButton(it) { addWater(1000) } }
            btnCloseWater.setOnClickListener { dismiss() }
        }
        
        // Observe ViewModel state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    val target = if (state.waterTarget > 0) state.waterTarget else 2500
                    binding.progressWaterCircular.max = target
                    binding.progressWaterCircular.setProgressCompat(
                        state.waterConsumed.coerceAtMost(target),
                        true
                    )
                    binding.tvWaterProgress.text = "${state.waterConsumed} ml"
                    binding.tvWaterTarget.text = "/ $target ml"

                    binding.cardWaterGoalReached.isVisible = state.waterConsumed >= target
                }
            }
        }
    }

    private var waterAdded = false

    private fun addWater(amountMl: Int) {
        homeViewModel.addWater(amountMl)
        waterAdded = true
        updateTipText()
    }

    override fun onDestroyView() {
        if (waterAdded) {
            setFragmentResult("water_added_request", android.os.Bundle().apply {
                putBoolean("added", true)
            })
        }
        super.onDestroyView()
        _binding = null
    }
}