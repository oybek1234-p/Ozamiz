package uz.oybek.ozamiz.ui.feature.log.food

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentFoodPhotoReviewBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
class FoodPhotoReviewFragment : BaseFragment<FragmentFoodPhotoReviewBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFoodPhotoReviewBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentFoodPhotoReviewBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomInset = if (imeInsets.bottom > 0) imeInsets.bottom else navInsets.bottom
            val density = resources.displayMetrics.density

            bind.buttonContainer.setPadding(
                (20 * density).toInt(),
                (12 * density).toInt(),
                (20 * density).toInt(),
                bottomInset + (12 * density).toInt()
            )
            insets
        }

        val imageUriString = arguments?.getString("imageUri")
        val rawTimestamp = arguments?.getLong("timestamp", -1L) ?: -1L
        val timestamp = if (rawTimestamp > 0) rawTimestamp else System.currentTimeMillis()
        val rawMeal = arguments?.getString("mealType") ?: arguments?.getString("meal")
        val mealType = uz.oybek.ozamiz.data.food.model.MealTime.getValidMeal(rawMeal, timestamp)

        if (imageUriString != null) {
            bind.imageViewPhotoPreview.setImageURI(Uri.parse(imageUriString))
        }

        bind.editTextFoodDescription.setText("")
        bind.editTextFoodDescription.requestFocus()

        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        bind.editTextFoodDescription.postDelayed({
            if (isAdded) {
                imm?.showSoftInput(bind.editTextFoodDescription, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 100)

        bind.buttonBack.setOnClickListener {
            imm?.hideSoftInputFromWindow(bind.editTextFoodDescription.windowToken, 0)
            popBackStack()
        }

        bind.buttonTekshirish.setOnClickListener {
            val userDescription = bind.editTextFoodDescription.text?.toString()?.trim().orEmpty()
            imm?.hideSoftInputFromWindow(bind.editTextFoodDescription.windowToken, 0)

            val bundle = bundleOf(
                AnalyzeResultFragment.KEY_IMAGE_URI to imageUriString,
                AnalyzeResultFragment.KEY_MEAL_TYPE to mealType,
                AnalyzeResultFragment.KEY_TIMESTAMP to timestamp,
                AnalyzeResultFragment.KEY_PENDING_DESCRIPTION to userDescription
            )
            navigate(
                R.id.analyzeResultFragment,
                bundle
            )
        }
    }
}
