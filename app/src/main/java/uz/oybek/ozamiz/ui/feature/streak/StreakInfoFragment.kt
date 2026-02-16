package uz.oybek.ozamiz.ui.feature.streak

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.databinding.FragmentStreakInfoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

class StreakInfoFragment : BaseFragment<FragmentStreakInfoBinding>() {

    private var streakInfo: StreakInfo? = null

    companion object {
        const val ARG_STREAK_INFO = "streak_info_arg"
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStreakInfoBinding {
        return FragmentStreakInfoBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentStreakInfoBinding) {
        arguments?.let {
            streakInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_STREAK_INFO, StreakInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_STREAK_INFO) as? StreakInfo
            }
        }

        bind.btnBack.setOnClickListener {
            closeFragment()
        }

        streakInfo?.let { info ->
            setupViews(info, bind)
        } ?: run {
            bind.textStreakMainCount.text = "0"
            bind.textBestStreak.text = "0"
            bind.textTotalActiveDays.text = "0"
            bind.textActiveWeeks.text = "0"
        }
    }

    private fun setupViews(info: StreakInfo, bind: FragmentStreakInfoBinding) {
        // Main Count
        bind.textStreakMainCount.text = info.currentStreak.toString()

        // Best Streak
        bind.textBestStreak.text = info.bestStreak.toString()

        // Active Days
        bind.textTotalActiveDays.text = info.activeDays.toString()
        
        // Active Weeks - calculate from activity history
        val activeWeeks = calculateActiveWeeks(info.activityHistory)
        bind.textActiveWeeks.text = activeWeeks.toString()

        // Weekly Progress
        setupWeeklyProgress(info, bind)

        // Animate Fire
        animateFire(bind.imageFireMain)
        animateFire(bind.imageFireBg, 1.15f)

        // Update motivational text based on streak
        updateMotivationalText(info.currentStreak, bind)
    }
    
    private fun calculateActiveWeeks(activityHistory: List<String>): Int {
        if (activityHistory.isEmpty()) return 0
        
        val weeks = mutableSetOf<String>()
        activityHistory.forEach { dateStr ->
            try {
                val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                weeks.add(weekStart.toString())
            } catch (e: Exception) {
                // Skip invalid dates
            }
        }
        return weeks.size
    }

    private fun updateMotivationalText(streak: Int, bind: FragmentStreakInfoBinding) {
        val text = when {
            streak == 0 -> "Bugun birinchi qadam qo'ying! 🚀"
            streak < 3 -> "Zo'r boshladingiz! Davom eting! 💪"
            streak < 7 -> "Ajoyib! Bir haftalikka yaqinlashyapsiz! 🔥"
            streak < 14 -> "Bir hafta! Siz zo'rsiz! 🌟"
            streak < 30 -> "Ishonchli davom etyapsiz! 💎"
            else -> "Siz haqiqiy chempionsiz! 🏆"
        }
        bind.textMotivation.text = text
    }

    private fun setupWeeklyProgress(info: StreakInfo, bind: FragmentStreakInfoBinding) {
        val historySet = info.activityHistory.toSet()
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val daysOfWeekParam = listOf("D", "S", "C", "P", "J", "S", "Y")

        val dayViews = listOf(
            bind.day1,
            bind.day2,
            bind.day3,
            bind.day4,
            bind.day5,
            bind.day6,
            bind.day7
        )

        for (i in 0 until 7) {
            val dateToCheck = monday.plusDays(i.toLong())
            val dateStr = dateToCheck.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val isActive = historySet.contains(dateStr)
            val isToday = dateToCheck.isEqual(today)

            val dayRoot: View = dayViews[i].root
            val textCircle = dayRoot.findViewById<TextView>(R.id.text_day_circle)

            textCircle.text = daysOfWeekParam[i]

            when {
                isActive -> {
                    textCircle.setBackgroundResource(R.drawable.bg_circle_filled)
                    textCircle.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
                isToday -> {
                    textCircle.setBackgroundResource(R.drawable.bg_circle_today)
                    textCircle.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                }
                else -> {
                    textCircle.setBackgroundResource(R.drawable.bg_circle_outline)
                    textCircle.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                }
            }
        }
    }

    private fun animateFire(view: View, scale: Float = 1.1f) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(1200)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(1200)
                    .withEndAction {
                        if (bindingT != null) animateFire(view, scale)
                    }
                    .start()
            }
            .start()
    }
}
