package uz.oybek.ozamiz.ui.feature.onboarding

import uz.oybek.ozamiz.R

sealed class OnBoardingDirection {
    data class SingleScreen(val id: Int) : OnBoardingDirection()

    data class MultipleAnswer(val type: MultipleAnswersQuestion)
        : OnBoardingDirection()

    data object Back : OnBoardingDirection()

    data object Finished: OnBoardingDirection()
}

object OnBoardingDirections {

    val screensNavList = listOf(
        OnBoardingDirection.SingleScreen(R.id.genderFragment),
        OnBoardingDirection.SingleScreen(R.id.nameFragment),
        OnBoardingDirection.MultipleAnswer(MultipleAnswersQuestion.GOAL),
        OnBoardingDirection.MultipleAnswer(MultipleAnswersQuestion.HISTORY),
        OnBoardingDirection.MultipleAnswer(MultipleAnswersQuestion.STRUGGLES),
        OnBoardingDirection.SingleScreen(R.id.diagramFragment),
        OnBoardingDirection.MultipleAnswer(MultipleAnswersQuestion.CALORIES),
        OnBoardingDirection.SingleScreen(R.id.calorieInfoFragment),
        OnBoardingDirection.MultipleAnswer(MultipleAnswersQuestion.FASTING),
        OnBoardingDirection.SingleScreen(R.id.ageFragment),
        OnBoardingDirection.SingleScreen(R.id.heightFragment),
        OnBoardingDirection.SingleScreen(R.id.activityLevelFragment),
        OnBoardingDirection.SingleScreen(R.id.weightFragment),
        OnBoardingDirection.SingleScreen(R.id.targetWeightFragment),
        OnBoardingDirection.SingleScreen(R.id.seeEffectFragment),
        OnBoardingDirection.MultipleAnswer(MultipleAnswersQuestion.CHALLENGE),
        OnBoardingDirection.SingleScreen(R.id.photosOnboardFragment),
        OnBoardingDirection.SingleScreen(R.id.planFragment)
    )
}