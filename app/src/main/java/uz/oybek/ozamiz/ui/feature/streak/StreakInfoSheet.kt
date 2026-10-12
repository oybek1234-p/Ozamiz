package uz.oybek.ozamiz.ui.feature.streak

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.streak.model.StreakDay
import uz.oybek.ozamiz.data.streak.model.StreakStatus
import uz.oybek.ozamiz.databinding.DialogStreakInfoBinding
import uz.oybek.ozamiz.databinding.ItemWeekDayBinding
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.appColors

@AndroidEntryPoint
class StreakInfoSheet : BottomSheetDialogFragment() {

    private var _binding: DialogStreakInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StreakViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO

                ViewCompat.setOnApplyWindowInsetsListener(sheet) { v, insets ->
                    val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navigationBars.bottom)
                    insets
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogStreakInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() = with(binding) {
        btnBack.setOnClickListener { dismiss() }
        buttonStreakDialogContinue.setOnClickListener {
            viewModel.onActionClick()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        handleEffect(effect)
                    }
                }
            }
        }
    }

    private fun renderState(state: StreakUiState) = with(binding) {
        val ctx = root.context
        val currentStreak = state.streakInfo.currentStreak

        when (state.status) {
            StreakStatus.ACTIVE -> {
                val descRes = when {
                    currentStreak >= 30 -> R.string.streak_active_desc_30
                    currentStreak >= 14 -> R.string.streak_active_desc_14
                    currentStreak >= 7 -> R.string.streak_active_desc_7
                    currentStreak >= 5 -> R.string.streak_active_desc_5
                    currentStreak >= 3 -> R.string.streak_active_desc_3
                    currentStreak == 1 -> R.string.streak_active_desc_1
                    else -> R.string.streak_active_desc_default
                }
                textStreakDescription.setText(descRes)
                textStreakDescription.setTextColor(VaznimColors.getOnSurface(ctx))
                buttonStreakDialogContinue.setText(R.string.streak_active_btn)
                lottieStreakFire.alpha = 1.0f
                lottieStreakFire.playAnimation()
                textStreakMainCount.text = currentStreak.toString()
                textStreakMainCount.setTextColor(VaznimColors.getOnSurface(ctx))
                textStreakDaysUnit.isVisible = true
                textStreakDaysUnit.setTextColor(ctx.appColors.colorStreakActive)
            }

            StreakStatus.FROZEN -> {
                textStreakDescription.setText(R.string.streak_frozen_desc)
                textStreakDescription.setTextColor(VaznimColors.getOnSurface(ctx))
                buttonStreakDialogContinue.setText(R.string.streak_frozen_btn)
                lottieStreakFire.alpha = 0.65f
                lottieStreakFire.pauseAnimation()
                textStreakMainCount.text = currentStreak.toString()
                textStreakMainCount.setTextColor(ctx.appColors.colorStreakFrozen)
                textStreakDaysUnit.isVisible = true
                textStreakDaysUnit.setTextColor(ctx.appColors.colorStreakFrozen)
            }

            StreakStatus.BROKEN -> {
                textStreakDescription.setText(R.string.streak_broken_desc)
                textStreakDescription.setTextColor(VaznimColors.getOnSurface(ctx))
                buttonStreakDialogContinue.setText(R.string.streak_broken_btn)
                lottieStreakFire.alpha = 0.25f
                lottieStreakFire.pauseAnimation()
                textStreakMainCount.text = "0"
                textStreakMainCount.setTextColor(VaznimColors.getOnSurface(ctx))
                textStreakDaysUnit.isVisible = true
                textStreakDaysUnit.setTextColor(ContextCompat.getColor(ctx, R.color.streak_inactive))
            }

            StreakStatus.IDLE -> {
                textStreakDescription.setText(R.string.streak_idle_desc)
                textStreakDescription.setTextColor(VaznimColors.getOnSurface(ctx))
                buttonStreakDialogContinue.setText(R.string.streak_idle_btn)
                lottieStreakFire.alpha = 0.35f
                lottieStreakFire.pauseAnimation()
                textStreakMainCount.text = "0"
                textStreakMainCount.setTextColor(VaznimColors.getOnSurface(ctx))
                textStreakDaysUnit.isVisible = false
            }
        }

        bindWeekDays(state.weekDays)
    }

    private fun handleEffect(effect: StreakUiEffect) {
        when (effect) {
            is StreakUiEffect.Dismiss, is StreakUiEffect.NavigateToLogFood -> dismiss()
            else -> Unit
        }
    }

    private fun bindWeekDays(weekDays: List<StreakDay>) {
        if (weekDays.size < 7) return
        val dayBindings = listOf(
            binding.dayMon,
            binding.dayTue,
            binding.dayWed,
            binding.dayThu,
            binding.dayFri,
            binding.daySat,
            binding.daySun
        )

        weekDays.take(7).forEachIndexed { index, model ->
            bindSingleDay(dayBindings[index], model)
        }
    }

    private fun bindSingleDay(itemBinding: ItemWeekDayBinding, model: StreakDay) {
        val ctx = context ?: return
        val dayNames = resources.getStringArray(R.array.streak_week_days_short)
        itemBinding.textDayName.text = dayNames.getOrElse(model.dayOfWeek) { "" }
        itemBinding.textDayNumber.text = model.dayNumber.toString()

        if (model.isActive) {
            val activeColor = VaznimColors.getPrimary(ctx)
            itemBinding.containerDayBadge.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(activeColor)
            }
            itemBinding.textDayName.setTextColor(activeColor)
            itemBinding.iconDayCheck.isVisible = true
            itemBinding.iconDayCheck.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.white))
            itemBinding.textDayNumber.isVisible = false
        } else {
            itemBinding.containerDayBadge.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(VaznimColors.getSurfaceContainerHigh(ctx))
            }
            itemBinding.textDayName.setTextColor(VaznimColors.getOnSurfaceVariant(ctx))
            itemBinding.iconDayCheck.isVisible = false
            itemBinding.textDayNumber.isVisible = true
            itemBinding.textDayNumber.setTextColor(VaznimColors.getOnSurfaceVariant(ctx))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "StreakInfoSheet"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                StreakInfoSheet().show(fragmentManager, TAG)
            }
        }
    }
}