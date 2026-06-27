package uz.oybek.ozamiz.ui.feature.streak

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.databinding.DialogStreakInfoBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class StreakInfoSheet : BottomSheetDialogFragment() {

    private var _binding: DialogStreakInfoBinding? = null
    private val binding get() = _binding!!

    private var streakInfo: StreakInfo? = null

    companion object {
        const val TAG = "StreakInfoSheet"
        private const val ARG_STREAK_INFO = "streak_info_arg"

        fun newInstance(streakInfo: StreakInfo, isNewAction: Boolean = false): StreakInfoSheet {
            val fragment = StreakInfoSheet()
            fragment.arguments = androidx.core.os.bundleOf(
                ARG_STREAK_INFO to streakInfo,
                "is_new_action" to isNewAction
            )
            return fragment
        }

        fun show(fragmentManager: androidx.fragment.app.FragmentManager, isNewAction: Boolean = false) {
            val streakInfo = uz.oybek.ozamiz.data.user.UserRepository.user.streakInfo ?: return
            newInstance(streakInfo, isNewAction).show(fragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            streakInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_STREAK_INFO, StreakInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_STREAK_INFO) as? StreakInfo
            }
        }
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStreakInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        val isNewAction = arguments?.getBoolean("is_new_action", false) ?: false

        streakInfo?.let { info ->
            setupViews(info)
            if (isNewAction) {
                animateStreakIncrement()
            }
        } ?: run {
            binding.textStreakMainCount.text = "0"
            binding.textMotivationChip.text = "Faollikni boshlang! 💪"
        }

        binding.buttonStreakDialogContinue.setOnClickListener {
            dismiss()
        }
    }

    private fun animateStreakIncrement() {
        val finalStreak = streakInfo?.currentStreak ?: return
        if (finalStreak <= 0) return
        
        val startStreak = finalStreak - 1
        binding.textStreakMainCount.text = startStreak.toString()
        
        binding.textStreakMainCount.postDelayed({
            android.animation.ValueAnimator.ofInt(startStreak, finalStreak).apply {
                duration = 1000
                addUpdateListener { animator ->
                    binding.textStreakMainCount.text = animator.animatedValue.toString()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: android.animation.Animator) {
                        binding.textStreakMainCount.animate()
                            .scaleX(1.3f)
                            .scaleY(1.3f)
                            .setDuration(500)
                            .withEndAction {
                                binding.textStreakMainCount.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(500)
                                    .start()
                            }
                            .start()
                    }
                })
                start()
            }
        }, 500)
    }

    private fun setupViews(info: StreakInfo) {
        // Main Count
        binding.textStreakMainCount.text = info.currentStreak.toString()
        
        // Best Streak
        binding.textBestStreak.text = info.bestStreak.toString()

        // Active Days
        binding.textTotalActiveDays.text = info.activeDays.toString()

        // Active Weeks - kamida 1 hafta
        val weeks = maxOf(1, (info.activeDays + 6) / 7)
        binding.textActiveWeeks.text = "$weeks Hafta"

        // Motivational Chip
        binding.textMotivationChip.text = getMotivationText(info.currentStreak)

        // Week Range
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("uz"))
        binding.textWeekRange.text = "${monday.format(formatter)} - ${sunday.format(formatter)}"

        setupWeeklyProgress(info)
    }

    private fun getMotivationText(streak: Int): String {
        return when {
            streak == 0 -> "Faollikni boshlang! 💪"
            streak == 1 -> "Ajoyib boshlanish! Davom eting! 🌟"
            streak < 7 -> "Siz $streak kun ketma-ket faolsiz! 🔥"
            streak < 14 -> "Bir hafta o'tdi! Zo'r! 💪"
            streak < 30 -> "Ajoyib natija! $streak kun! 🏆"
            else -> "Siz shuncha yo'lni bosib o'tdingiz! 👑"
        }
    }

    private fun setupWeeklyProgress(info: StreakInfo) {
        val historySet = info.activityHistory.toSet()
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        
        // Uzbek hafta kunlari
        val daysOfWeekParam = listOf("DU", "SE", "CH", "PA", "JU", "SH", "YA")

        val dayViews = listOf(
            binding.day1,
            binding.day2,
            binding.day3,
            binding.day4,
            binding.day5,
            binding.day6,
            binding.day7
        )

        val typedValue = TypedValue()
        val theme = requireContext().theme
        
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data
        
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
        val colorOnPrimary = typedValue.data
        
        theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true)
        val colorOutlineVariant = typedValue.data

        for (i in 0 until 7) {
            val dateToCheck = monday.plusDays(i.toLong())
            val dateStr = dateToCheck.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val isActive = historySet.contains(dateStr)
            val isToday = dateToCheck == today

            val dayRoot: View = dayViews[i].root
            val textCircle = dayRoot.findViewById<TextView>(R.id.text_day_circle)

            textCircle.text = daysOfWeekParam[i]

            when {
                isActive -> {
                    textCircle.setBackgroundResource(R.drawable.bg_circle_primary)
                    textCircle.setTextColor(colorOnPrimary)
                }
                isToday -> {
                    textCircle.setBackgroundResource(R.drawable.bg_circle_primary_outline)
                    textCircle.setTextColor(colorPrimary)
                }
                else -> {
                    textCircle.setBackgroundResource(R.drawable.bg_circle_outline)
                    textCircle.setTextColor(colorOutlineVariant)
                }
            }
        }
    }
    
    private fun animateFire(view: View, scale: Float = 1.05f) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(1000)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(1000)
                    .withEndAction {
                         if (_binding != null) animateFire(view, scale)
                    }
                    .start()
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}