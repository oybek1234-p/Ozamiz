package uz.oybek.ozamiz.ui.feature.log.food

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysis
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysisResult
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.utils.formatToHhMmA
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.databinding.ItemMacroDisplayBinding
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import java.util.Date

class AnalyzeResultFragment : Fragment() {

    private var _binding: BottomSheetCalorieResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyzeResultViewModel by viewModels()
    private lateinit var ingredientsAdapter: IngredientsAdapter
    private var hasPlayedConfetti = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val logId = it.getString(KEY_LOG_ID)
            val userId = it.getString(KEY_USER_ID)
            val timestamp = it.getLong(KEY_TIMESTAMP, -1L)
            if (timestamp != -1L) {
                viewModel.setLogDate(Date(timestamp))
            }
            if (logId != null) {
                viewModel.loadLog(logId, Date(timestamp), userId)
            } else {
                val id = it.getInt(KEY_ID, -1)
                if (id != -1) {
                    viewModel.setResult(FoodAnalyzer.getLastAnalyzed(id))
                } else {
                    val imageUri = it.getString(KEY_IMAGE_URI)?.toUri()
                    val audioUri = it.getString(KEY_AUDIO_URI)?.toUri()
                    val description = it.getString(KEY_PENDING_DESCRIPTION) ?: ""
                    val meal = it.getString(KEY_MEAL_TYPE) ?: ""
                    if (audioUri != null) {
                        viewModel.analyzeAudio(audioUri, meal, Date(timestamp))
                    } else {
                        viewModel.startAnalyze(imageUri, description, meal, Date(timestamp))
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCalorieResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.inflateMenu(R.menu.menu_analyze_result)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_favorite) {
                viewModel.toggleFavorite()
                true
            } else {
                false
            }
        }
        setupListeners()
        setupUI()

        if (arguments?.getBoolean(KEY_IS_FROM_FAVORITE) == true) {
            val args = requireArguments()
            val analysis = FoodAnalysis(
                id = args.getString(KEY_DESCRIPTION).hashCode(),
                photoUrl = args.getString(KEY_PHOTO_URL)?.takeIf { it != "null" },
                result = FoodAnalysisResult(
                    name = args.getString(KEY_DESCRIPTION) ?: "",
                    calories = args.getDouble(KEY_CALORIES).toInt(),
                    protein = args.getDouble(KEY_PROTEIN),
                    fat = args.getDouble(KEY_FAT),
                    carbs = args.getDouble(KEY_CARBS),
                    healthScore = 0
                ),
                mealType = args.getString(KEY_MEAL_TYPE) ?: ""
            )
            viewModel.setRecallResult(analysis)
        }
    }

    private fun showAnalyzing(isAnalyzing: Boolean) {
        val wasAnalyzing = binding.lottieLoadingCalories.isVisible && !isAnalyzing

        binding.layoutMacros.isVisible = !isAnalyzing

        binding.lottieLoadingCalories.isVisible = isAnalyzing
        binding.imageViewFire.isVisible = !isAnalyzing

        val favoriteItem = binding.toolbar.menu.findItem(R.id.action_favorite)
        favoriteItem?.isVisible = !isAnalyzing && viewModel.state.value.result != null

        if (isAnalyzing) {
            binding.layoutBottomActions.visibility = View.VISIBLE
            binding.buttonSaveToLog.isVisible = false
            binding.layoutSaveContainer.isVisible = false
            binding.buttonReLog.isVisible = true
            binding.buttonReLog.text = getString(R.string.cancel)
            binding.buttonReLog.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.arrow_back)

            binding.calorieCard.isVisible = false
            binding.healthBall.isVisible = false
            binding.loadingCard.isVisible = true
            binding.cardAiHealth.isVisible = false
        } else {
            binding.calorieCard.isVisible = true
            binding.loadingCard.isVisible = false
            binding.cardAiHealth.isVisible = true
            binding.healthBall.isVisible = true

            val state = viewModel.state.value
            binding.buttonSaveToLog.isVisible = !state.isRecallMode
            binding.layoutSaveContainer.isVisible = !state.isRecallMode
            binding.buttonReLog.isVisible = !state.isRecallMode
            binding.buttonReLog.text = getString(R.string.cancel)
            binding.buttonReLog.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.arrow_back)

            if (wasAnalyzing) {
                binding.layoutBottomActions.alpha = 0f
                binding.layoutBottomActions.visibility = View.VISIBLE
                binding.layoutBottomActions.animate().alpha(1f).setDuration(500).start()
            } else {
                binding.layoutBottomActions.visibility = View.VISIBLE
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarButton.isVisible = show
        binding.buttonSaveToLog.isEnabled = !show
        binding.buttonReLog.isEnabled = !show
        binding.buttonReLogFull.isEnabled = !show
        binding.buttonDelete.isEnabled = !show

        binding.buttonSaveToLog.text = if (show) "" else getString(R.string.save)

        if (show) {
            binding.buttonSaveToLog.animate().alpha(0f).setDuration(200).start()
            binding.buttonReLog.animate().alpha(0f).setDuration(200).start()
            binding.buttonDelete.animate().alpha(0f).setDuration(200).start()
        } else {
            binding.buttonSaveToLog.animate().alpha(1f).setDuration(500).start()
            binding.buttonReLog.animate().alpha(1f).setDuration(500).start()
            binding.buttonDelete.animate().alpha(1f).setDuration(500).start()
        }

        binding.nestedScrollView.alpha = if (show) 0.5f else 1.0f
    }

    private fun setupUI() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                showAnalyzing(state.isAnalyzing)

                // Handle pending states (Voice input or Image preview)
                if (state.isAnalyzing) {
                    binding.loadingText.text = getString(R.string.analyzing)

                    if (!state.pendingDescription.isNullOrBlank()) {
                        binding.textViewFoodName.text =
                            if (state.pendingDescription == "pending_description_voice") {
                                getString(R.string.voice_input_pending)
                            } else {
                                state.pendingDescription
                            }
                        binding.layoutFoodName.isVisible = true
                    }

                    if (state.pendingImageUri != null) {
                        binding.imageViewFoodPhoto.isVisible = true
                        Glide.with(this@AnalyzeResultFragment)
                            .load(state.pendingImageUri)
                            .into(binding.imageViewFoodPhoto)
                    }
                    return@collect
                }

                showLoading(state.isLoading)
                val analysisResult = state.result?.result ?: run {
                    // If no result and not analyzing, but we have a pending description (e.g. error state or initial)
                    if (!state.pendingDescription.isNullOrBlank()) {
                        binding.textViewFoodName.text =
                            if (state.pendingDescription == "pending_description_voice") {
                                getString(R.string.voice_input_pending)
                            } else {
                                state.pendingDescription
                            }
                        binding.layoutFoodName.isVisible = true
                    }
                    return@collect
                }

                updateMacrosAndCalorie(analysisResult)
                val photoUrl = state.result.photoUrl
                if (!photoUrl.isNullOrEmpty() && photoUrl != "null") {
                    binding.imageViewFoodPhoto.isVisible = true
                    Glide
                        .with(this@AnalyzeResultFragment)
                        .load(photoUrl)
                        .placeholder(R.drawable.placeholder_food)
                        .into(binding.imageViewFoodPhoto)

                    binding.imageViewFoodPhoto.setOnClickListener {
                        uz.oybek.ozamiz.ui.common.dialog.FullScreenImageDialog.newInstance(
                            photoUrl
                        )
                            .show(parentFragmentManager, "full_image")
                    }
                } else if (state.pendingImageUri != null) {
                    binding.imageViewFoodPhoto.isVisible = true
                    Glide.with(this@AnalyzeResultFragment)
                        .load(state.pendingImageUri)
                        .into(binding.imageViewFoodPhoto)
                } else {
                    binding.imageViewFoodPhoto.isVisible = false
                }
                binding.textViewFoodName.text = analysisResult.name
                binding.layoutFoodName.isVisible = true

                binding.healthBall.apply {
                    if (analysisResult.healthScore != null) {
                        isVisible = true
                        binding.ballCount.setTextColor(
                            when (analysisResult.healthScore) {
                                in 0..3 -> VaznimColors.getOnSurface()
                                in 4..5 -> VaznimColors.getOrange()
                                else -> VaznimColors.getGreen()
                            }
                        )
                        binding.ballCount.text = analysisResult.healthScore.toString() + " ball"
                    }
                }
                binding.textViewTimestamp.text = Date(analysisResult.timestamp).formatToHhMmA()

                ingredientsAdapter =
                    IngredientsAdapter(
                        ingredients = analysisResult.ingredients ?: emptyList()
                    )

                if (!hasPlayedConfetti) {
                    val score = analysisResult.healthScore ?: 0

                    if (score == 10) {
                        hasPlayedConfetti = true
                        binding.confettiAnimation.isVisible = true
                        binding.confettiAnimation.playAnimation()
                    }
                }

                val adviceText = analysisResult.advice
                if (!adviceText.isNullOrBlank()) {
                    binding.layoutAdvice.isVisible = true
                    binding.textViewAdvice.text = adviceText
                } else {
                    binding.layoutAdvice.isVisible = false
                }

                binding.cardAiHealth.isVisible = !adviceText.isNullOrBlank()

                val favoriteItem = binding.toolbar.menu.findItem(R.id.action_favorite)
                favoriteItem?.isVisible = true && !state.isAnalyzing
                if (favoriteItem != null && state.result != null) {
                    favoriteItem.setIcon(
                        if (state.isSaved) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                    )
                }

                binding.buttonSaveToLog.isVisible = !state.isRecallMode
                binding.layoutSaveContainer.isVisible = !state.isRecallMode
                binding.buttonSaveToLog.text = getString(R.string.save)
                binding.buttonSaveToLog.icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.check_ic)

                binding.buttonReLog.isVisible = !state.isRecallMode
                binding.buttonReLog.text = getString(R.string.cancel)
                binding.buttonReLog.icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.arrow_back
                )

                binding.buttonReLogFull.isVisible = state.isRecallMode
                binding.buttonDelete.isVisible =
                    state.isRecallMode && arguments?.getString(KEY_LOG_ID) != null
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is AnalyzeResultEvent.Saved -> {
                        findNavController().popBackStack(R.id.home, false)
                    }


                    is AnalyzeResultEvent.Failure -> {
                        val resId = resources.getIdentifier(
                            event.message,
                            "string",
                            requireContext().packageName
                        )
                        val message = if (resId != 0) getString(resId) else event.message
                        android.widget.Toast.makeText(
                            requireContext(),
                            message,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                    is AnalyzeResultEvent.PromptShare -> {}

                    is AnalyzeResultEvent.LimitExceeded -> {
                        findNavController().popBackStack(R.id.home, false)
                    }
                }
            }
        }
    }

    private fun updateMacrosAndCalorie(result: FoodAnalysisResult) {
        binding.textViewFoodName.text = result.name
        val totalCalories = result.calories
        binding.textViewCalories.text = totalCalories.toString()

        bindMacro(
            binding.layoutProtein,
            getString(R.string.protein),
            result.protein
        )
        bindMacro(
            binding.layoutCarbs,
            getString(R.string.carbs_hint),
            result.carbs
        )
        bindMacro(
            binding.layoutFats,
            getString(R.string.fat_hint),
            result.fat
        )
    }


    @SuppressLint("SetTextI18n")
    private fun bindMacro(
        layout: ItemMacroDisplayBinding,
        name: String,
        value: Double?
    ) {
        layout.textViewMacroName.text = name
        val totalValue = (value ?: 0.0)
        layout.textViewMacroValue.text = getString(R.string.gram_format, totalValue.toInt())
    }

    private fun setupListeners() {
        binding.buttonSaveToLog.setOnSafeClickListener {
            viewModel.onSaveClick()
        }

        binding.buttonReLog.setOnSafeClickListener {
            findNavController().popBackStack()
        }

        binding.buttonReLogFull.setOnSafeClickListener {
            viewModel.reLog()
        }


        binding.buttonDelete.setOnSafeClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_log_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.deleteLog { success ->
                        if (success) {
                            findNavController().popBackStack()
                        }
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
        binding.toolbar.setNavigationOnClickListener {
            val isRecall = viewModel.resultLive.value?.isRecallMode == true
            if (isRecall) {
                findNavController().popBackStack()
            } else {
                findNavController().popBackStack(R.id.home, false)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val isRecall = viewModel.resultLive.value?.isRecallMode == true
                    if (isRecall) {
                        findNavController().popBackStack()
                    } else {
                        findNavController().popBackStack(R.id.home, false)
                    }
                }
            }
        )

        val nameEditListener = View.OnClickListener {
            val currentName = binding.textViewFoodName.text.toString()
            showEditNameDialog(currentName)
        }
        binding.layoutFoodName.setOnClickListener(nameEditListener)


        val editCaloriesListener = View.OnClickListener {
            val currentCalories = binding.textViewCalories.text.toString().toDoubleOrNull() ?: 0.0
            showEditNumberDialog(
                getString(R.string.edit_calories_title),
                currentCalories
            ) { newValue ->
                viewModel.updateCalories(newValue)
            }
        }
        binding.cardCalorieBanner.setOnClickListener(editCaloriesListener)


        binding.layoutProtein.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.protein ?: 0.0
            showEditNumberDialog(getString(R.string.edit_protein_title), currentVal) { newValue ->
                viewModel.updateProtein(newValue)
            }
        }
        binding.layoutCarbs.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.carbs ?: 0.0
            showEditNumberDialog(getString(R.string.edit_carbs_title), currentVal) { newValue ->
                viewModel.updateCarbs(newValue)
            }
        }
        binding.layoutFats.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.fat ?: 0.0
            showEditNumberDialog(getString(R.string.edit_fat_title), currentVal) { newValue ->
                viewModel.updateFat(newValue)
            }
        }
    }

    private fun showEditNameDialog(currentName: String) {
        val input = android.widget.EditText(requireContext())
        input.setText(currentName)
        input.hint = getString(R.string.food_name_hint)

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginStart = resources.getDimensionPixelSize(R.dimen.dimen_24)
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.dimen_24)
        input.layoutParams = params
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_food_title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank() && newName != currentName) {
                    viewModel.updateLog(newName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditNumberDialog(
        title: String,
        currentValue: Double,
        onSave: (Double) -> Unit
    ) {
        val input = android.widget.EditText(requireContext())
        input.setText(
            if (currentValue % 1.0 == 0.0) currentValue.toInt()
                .toString() else currentValue.toString()
        )
        input.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginStart = resources.getDimensionPixelSize(R.dimen.dimen_24)
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.dimen_24)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val newValue = input.text.toString().toDoubleOrNull()
                if (newValue != null) {
                    onSave(newValue)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startSaveButtonBounce() {
        if (_binding == null || !isAdded || view == null) return
        val btn = binding.buttonSaveToLog
        if (!btn.isVisible) return

        btn.animate().scaleX(0.9f).scaleY(0.9f).setDuration(300).withEndAction {
            if (_binding == null || !isAdded) return@withEndAction
            btn.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).withEndAction {
                if (_binding == null || !isAdded) return@withEndAction
                btn.animate().scaleX(1f).scaleY(1f).setDuration(300).withEndAction {
                    viewLifecycleOwner.lifecycleScope.launch {
                        kotlinx.coroutines.delay(500)
                        startSaveButtonBounce()
                    }
                }.start()
            }.start()
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_IMAGE_URI = "imageUri"
        const val KEY_AUDIO_URI = "audioUri"
        const val KEY_PENDING_DESCRIPTION = "pendingDescription"
        const val KEY_LOG_ID = "logId"
        const val KEY_USER_ID = "userId"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_ID = "id"
        const val KEY_IS_FROM_FAVORITE = "isFromFavorite"
        const val KEY_DESCRIPTION = "description"
        const val KEY_CALORIES = "calories"
        const val KEY_PROTEIN = "protein"
        const val KEY_FAT = "fat"
        const val KEY_CARBS = "carbs"
        const val KEY_PHOTO_URL = "photoUrl"
        const val KEY_MEAL_TYPE = "mealType"
    }
}