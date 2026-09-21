package uz.oybek.ozamiz.ui.feature.onboarding

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.common.onFailure
import uz.oybek.ozamiz.data.common.onSuccess
import uz.oybek.ozamiz.data.diet.DietEngine
import uz.oybek.ozamiz.data.diet.model.DietPlan
import uz.oybek.ozamiz.data.diet.model.DietPlanSpeed
import uz.oybek.ozamiz.data.diet.model.GoalDuration
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.data.profile.model.UserActivityLevel
import uz.oybek.ozamiz.data.profile.model.UserGoal
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.ui.feature.onboarding.screens.DietPlanUiState
import uz.oybek.ozamiz.util.handleException
import javax.inject.Inject

data class OnBoardingUiState(
    val user: User = User(),
    val currentScreenPos: Int = 0,
    val progress: Float = 0f,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val dietPlanState: DietPlanUiState = DietPlanUiState(),
    val idealWeight: Pair<Int, Int> = Pair(0, 0),
    val goalDuration: GoalDuration = GoalDuration()
) {
    val isMale: Boolean get() = user.gender == Gender.MALE
}

sealed interface OnBoardingEffect {
    data class NavigateToScreen(val resId: Int, val args: Bundle? = null) : OnBoardingEffect
    data object NavigateBack : OnBoardingEffect
    data object SaveSuccess : OnBoardingEffect
    data object ShowError : OnBoardingEffect
}

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo,
    private val dietEngine: DietEngine
) : ViewModel() {

    private val screensNavList = OnBoardingDirections.screensNavList

    private val _uiState = MutableStateFlow(
        OnBoardingUiState(
            user = profileRepo.currentUser
        )
    )
    val uiState: StateFlow<OnBoardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnBoardingEffect>(Channel.BUFFERED)
    val effect: Flow<OnBoardingEffect> = _effect.receiveAsFlow()

    init {
        updateProgress(0)
    }

    fun initDietPlan(speed: DietPlanSpeed = DietPlanSpeed.NORMAL) {
        _uiState.update { state ->
            val normalPlan = dietEngine.calculateDietPlan(state.user, DietPlanSpeed.NORMAL)
            val fastPlan = dietEngine.calculateDietPlan(state.user, DietPlanSpeed.FAST)
            val selectedPlan = if (speed == DietPlanSpeed.NORMAL) normalPlan else fastPlan

            val dietPlanUiState = DietPlanUiState(
                selectedSpeed = speed,
                normalPlan = normalPlan,
                fastPlan = fastPlan,
                selectedPlan = selectedPlan
            )

            state.copy(
                dietPlanState = dietPlanUiState,
                goalDuration = selectedPlan.duration,
                user = state.user.applyDietPlan(selectedPlan)
            )
        }
    }

    fun setPlanSpeed(speed: DietPlanSpeed) {
        _uiState.update { state ->
            val currentDietPlan = state.dietPlanState
            val selectedPlan = if (speed == DietPlanSpeed.NORMAL)
                currentDietPlan.normalPlan else currentDietPlan.fastPlan
            val updatedDietPlan = currentDietPlan.copy(
                selectedSpeed = speed,
                selectedPlan = selectedPlan
            )
            state.copy(
                dietPlanState = updatedDietPlan,
                goalDuration = selectedPlan?.duration ?: state.goalDuration,
                user = state.user.applyDietPlan(selectedPlan)
            )
        }
    }

    private fun User.applyDietPlan(plan: DietPlan?): User {
        if (plan == null) return this
        return copy(
            targetCalories = plan.targetCalories,
            targetProtein = plan.macros.protein,
            targetFat = plan.macros.fat,
            targetCarbs = plan.macros.carbs
        )
    }

    fun setGender(isMale: Boolean) {
        updateUser { it.copy(gender = if (isMale) Gender.MALE else Gender.FEMALE) }
    }

    fun setGoal(isLoseWeight: Boolean) {
        updateUser {
            it.copy(goal = if (isLoseWeight) UserGoal.LOSE_WEIGHT.key else UserGoal.GAIN_WEIGHT.key)
        }
    }

    fun setActivityLevel(activityLevel: UserActivityLevel) {
        updateUser { it.copy(activityLevel = activityLevel.name) }
    }

    fun setName(name: String) {
        updateUser { it.copy(name = name) }
    }

    fun setAge(age: Int) {
        updateUser { it.copy(age = age) }
    }

    fun setHeight(height: Int) {
        _uiState.update { state ->
            val updatedUser = state.user.copy(height = height)
            state.copy(
                user = updatedUser,
                idealWeight = dietEngine.getIdealWeight(height.toDouble())
            )
        }
    }

    fun setWeight(weight: Double) {
        updateUser {
            it.copy(
                weight = weight,
                startWeight = it.startWeight ?: weight
            )
        }
    }

    fun setTargetWeight(targetWeight: Double) {
        updateUser { it.copy(targetWeight = targetWeight) }
    }

    fun setPhoneNumber(phone: String) {
        updateUser { it.copy(phoneNumber = phone) }
    }

    fun updateUser(transform: (User) -> User) {
        _uiState.update { it.copy(user = transform(it.user)) }
    }

    fun moveBack() {
        if (_uiState.value.isSaving) return
        val currentPos = _uiState.value.currentScreenPos
        if (currentPos > 0) {
            val newPos = currentPos - 1
            viewModelScope.launch {
                _effect.send(OnBoardingEffect.NavigateBack)
            }
            updateProgress(newPos)
        }
    }

    fun moveToNextScreen() {
        if (_uiState.value.isSaving) return
        val currentPos = _uiState.value.currentScreenPos
        val lastPos = screensNavList.lastIndex
        if (currentPos < lastPos) {
            val newPos = currentPos + 1
            viewModelScope.launch {
                _effect.send(
                    OnBoardingEffect.NavigateToScreen(screensNavList[newPos])
                )
            }
            updateProgress(newPos)
        }
    }

    private fun updateProgress(newPos: Int) {
        val currentProgress = ((newPos + 1) / screensNavList.size.toFloat()) * 100
        _uiState.update {
            it.copy(
                currentScreenPos = newPos,
                progress = currentProgress
            )
        }
    }

    fun saveOnboardingInfo() {
        if (uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            profileManagerRepo.saveUserProfile(uiState.value.user)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                    _effect.send(OnBoardingEffect.SaveSuccess)
                }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.send(OnBoardingEffect.ShowError)
                    it?.let { handleException(it) }
                }
        }
    }
}