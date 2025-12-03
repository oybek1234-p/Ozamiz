package uz.oybek.ozamiz.ui.feature.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uz.oybek.ozamiz.ui.utils.postVal

class OnBoardingViewModel : ViewModel() {

    private val screensNavList = OnBoardingDirections.screensNavList
    private var currentScreenPos = 0
    val currentScreenDirection = MutableLiveData(screensNavList[0])
    val progressLive = MutableLiveData<Float>()

    init {
        updateProgress()
    }

    fun moveBack() {
        if (currentScreenPos > 0) {
            currentScreenDirection.postVal(OnBoardingDirection.Back)
            currentScreenPos -= 1
            updateProgress()
        }
    }

    fun moveToNextScreen() {
        val lastPos = screensNavList.lastIndex
        if (currentScreenPos == lastPos) {
            currentScreenDirection.postVal(
                OnBoardingDirection.Finished
            )
            return
        }
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
}