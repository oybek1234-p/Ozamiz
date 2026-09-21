package uz.oybek.ozamiz.ui.feature.onboarding

import androidx.annotation.StringRes
import uz.oybek.ozamiz.R

enum class MultipleAnswersQuestion(
    @StringRes val titleRes: Int,
    val answersRes: List<Int>
) {
    GOAL(
        titleRes = R.string.onboarding_multiple_goal_title,
        answersRes = listOf(
            R.string.onboarding_multiple_goal_1,
            R.string.onboarding_multiple_goal_2,
            R.string.onboarding_multiple_goal_3,
            R.string.onboarding_multiple_goal_4,
            R.string.onboarding_multiple_goal_5
        )
    ),
    HISTORY(
        titleRes = R.string.onboarding_multiple_history_title,
        answersRes = listOf(
            R.string.onboarding_multiple_history_1,
            R.string.onboarding_multiple_history_2,
            R.string.onboarding_multiple_history_3,
            R.string.onboarding_multiple_history_4
        )
    ),

    STRUGGLES(
        titleRes = R.string.onboarding_multiple_struggles_title,
        answersRes = listOf(
            R.string.onboarding_multiple_struggles_1,
            R.string.onboarding_multiple_struggles_2,
            R.string.onboarding_multiple_struggles_3,
            R.string.onboarding_multiple_struggles_4,
            R.string.onboarding_multiple_struggles_5
        )
    ),
    CALORIES(
        titleRes = R.string.onboarding_multiple_calories_title,
        answersRes = listOf(
            R.string.onboarding_multiple_calories_1,
            R.string.onboarding_multiple_calories_2
        )
    ),
    FASTING(
        titleRes = R.string.onboarding_multiple_fasting_title,
        answersRes = listOf(
            R.string.onboarding_multiple_fasting_1,
            R.string.onboarding_multiple_fasting_2,
            R.string.onboarding_multiple_fasting_3,
            R.string.onboarding_multiple_fasting_4,
            R.string.onboarding_multiple_fasting_5
        )
    ),
    CHALLENGE(
        titleRes = R.string.onboarding_multiple_challenge_title,
        answersRes = listOf(
            R.string.onboarding_multiple_challenge_1,
            R.string.onboarding_multiple_challenge_2,
            R.string.onboarding_multiple_challenge_3,
            R.string.onboarding_multiple_challenge_4
        )
    )
}