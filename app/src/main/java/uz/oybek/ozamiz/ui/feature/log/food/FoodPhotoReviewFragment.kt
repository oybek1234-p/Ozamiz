package uz.oybek.ozamiz.ui.feature.log.food

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentFoodPhotoReviewBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showKeyboard
import androidx.core.net.toUri
import androidx.navigation.NavOptions
import uz.oybek.ozamiz.ui.utils.toBundle
import uz.oybek.ozamiz.ui.utils.loadPhoto

@AndroidEntryPoint
class FoodPhotoReviewFragment : BaseFragment<FragmentFoodPhotoReviewBinding>() {

    private val viewModel: FoodPhotoReviewViewModel by viewModels()

    private val popReviewNavOptions by lazy {
        NavOptions.Builder()
            .setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim)
            .setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit)
            .setPopUpTo(R.id.foodPhotoReviewFragment, true)
            .build()
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFoodPhotoReviewBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentFoodPhotoReviewBinding) {
        initControlsUI(bind)
        renderPhoto(viewModel.uiState.value.imageUri)
        focusInputField(bind)
        observeEvents()
    }

    private fun initControlsUI(bind: FragmentFoodPhotoReviewBinding) = with(bind) {
        buttonBack.setOnSafeClickListener { onBackButtonClicked() }
        buttonTekshirish.setOnSafeClickListener { onReviewSubmitClicked() }
    }

    private fun focusInputField(bind: FragmentFoodPhotoReviewBinding) {
        bind.editTextFoodDescription.setText("")
        bind.editTextFoodDescription.requestFocus()
        bind.editTextFoodDescription.showKeyboard()
    }

    private fun onBackButtonClicked() {
        activity?.let { hideSoftInput(it) }
        viewModel.onBackClicked()
    }

    private fun onReviewSubmitClicked() {
        activity?.let { hideSoftInput(it) }
        val description = binding.editTextFoodDescription.text?.toString().orEmpty()
        viewModel.onSubmitClicked(description)
    }

    private fun observeEvents() {
        collectFlowOnStart(viewModel.events) { event ->
            handleEvent(event)
        }
    }

    private fun renderPhoto(imageUri: String) {
        if (imageUri.isNotEmpty()) {
            binding.imageViewPhotoPreview.loadPhoto(imageUri)
        }
    }

    private fun handleEvent(event: FoodPhotoReviewEvent) {
        when (event) {
            is FoodPhotoReviewEvent.NavigateToAnalyzeResult -> {
                navigateToAnalyzeResult(event)
            }

            is FoodPhotoReviewEvent.NavigateBack -> {
                popBackStack()
            }
        }
    }

    private fun navigateToAnalyzeResult(event: FoodPhotoReviewEvent.NavigateToAnalyzeResult) {
        val args = AnalyzeResultArgs.NewAnalysis(
            imageUri = event.imageUri,
            mealType = event.mealType,
            description = event.description
        )
        navigate(R.id.analyzeResultFragment, args.toBundle(), popReviewNavOptions)
    }
}
