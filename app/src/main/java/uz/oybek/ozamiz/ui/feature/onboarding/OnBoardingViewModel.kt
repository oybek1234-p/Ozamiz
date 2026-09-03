package uz.oybek.ozamiz.ui.feature.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.ui.utils.postVal
import uz.oybek.ozamiz.util.handleException

class OnBoardingViewModel : ViewModel() {

    private val screensNavList = OnBoardingDirections.screensNavList
    private var currentScreenPos = 0
    val currentScreenDirection = MutableLiveData(screensNavList[0])
    val progressLive = MutableLiveData<Float>()

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        updateProgress()
        UserProfileRepository.onboardingStarted = true
    }

    fun updateTargetWeight(targetWeight: Double) {
        user.targetWeight = targetWeight
        DietController.updateTargetValues(user)
    }

    fun moveBack() {
        if (_isLoading.value == true) return
        if (currentScreenPos > 0) {
            currentScreenDirection.postVal(OnBoardingDirection.Back)
            currentScreenPos -= 1
            updateProgress()
        }
    }

    fun moveToNextScreen() {
        if (_isLoading.value == true) return
        val lastPos = screensNavList.lastIndex
        if (currentScreenPos < lastPos) {
            currentScreenPos += 1
            currentScreenDirection.postVal(
                screensNavList[currentScreenPos]
            )
            updateProgress()
        }
    }

    private fun updateProgress() {
        val progress = ((currentScreenPos + 1) / screensNavList.size.toFloat()) * 100
        progressLive.postVal(progress)
    }

    fun completeOnboarding(onSuccess: () -> Unit, onError: () -> Unit) {
        if (_isLoading.value == true) return
        _isLoading.postVal(true)
        viewModelScope.launch {
            try {
                UserProfileRepository.updateUser()
                val success = UserProfileRepository.saveUserNetworkSuspend(LocalUser.user)
                UserProfileRepository.onboardingStarted = false
                _isLoading.postVal(false)
                if (success) {
                    onSuccess()
                } else {
                    onError()
                }
            } catch (e: Exception) {
                handleException(e)
                UserProfileRepository.onboardingStarted = false
                _isLoading.postVal(false)
                onError()
            }
        }
    }
}