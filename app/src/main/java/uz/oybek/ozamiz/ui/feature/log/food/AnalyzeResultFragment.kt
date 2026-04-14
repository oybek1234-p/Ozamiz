package uz.oybek.ozamiz.ui.feature.log.food

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysisResult
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.databinding.ItemMacroDisplayBinding
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyzeResultFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCalorieResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyzeResultViewModel by viewModels()
    private lateinit var ingredientsAdapter: IngredientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle)
        arguments?.let {
            val logId = it.getString("logId")
            val userId = it.getString("userId")
            if (logId != null) {
                val timestamp = it.getLong("timestamp")
                viewModel.loadLog(logId, Date(timestamp), userId)
            } else {
                val id = it.getInt("id", -1)
                if (id != -1) {
                    viewModel.setResult(FoodAnalyzer.getLastAnalyzed(id))
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
        setupListeners()
        setupUI()
        if (arguments?.getBoolean("isFromFavorite") == true) {
            val args = requireArguments()
            val analysis = FoodAnalysis(
                id = args.getString("description").hashCode(),
                photoUrl = args.getString("photoUrl")?.takeIf { it != "null" },
                result = FoodAnalysisResult(
                    name = args.getString("description") ?: "",
                    calories = args.getDouble("calories").toInt(),
                    protein = args.getDouble("protein"),
                    fat = args.getDouble("fat"),
                    carbs = args.getDouble("carbs"),
                    healthScore = 0,
                    aiInsight = ""
                ),
                mealType = args.getString("mealType") ?: ""
            )
            viewModel.setRecallResult(analysis)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarButton.isVisible = show
        binding.buttonSaveToLog.isEnabled = !show
        binding.buttonReLog.isEnabled = !show
        binding.buttonDelete.isEnabled = !show

        binding.buttonSaveToLog.text = if (show) "" else getString(R.string.save)
        binding.buttonReLog.text = if (show) "" else "Qayta qo'shish"
        binding.buttonReLog.icon =
            if (show) null else ContextCompat.getDrawable(requireContext(), R.drawable.add_ic)

        binding.nestedScrollView.alpha = if (show) 0.5f else 1.0f
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resultLive.collect { state ->
                state?.run {
                    showLoading(isLoading)
                    val analysisResult = result?.result ?: return@run

                    if (analysisResult.isAmbiguous && analysisResult.question != null) {
                        val existingSheet =
                            parentFragmentManager.findFragmentByTag("clarification_sheet")
                        if (existingSheet == null && !isLoading) {
                            ClarificationBottomSheet(
                                question = analysisResult.question,
                                options = analysisResult.options ?: emptyList()
                            ) { selectedOption ->
                                viewModel.submitAnswer(selectedOption)
                            }.show(parentFragmentManager, "clarification_sheet")
                        }
                    }

                    updateMacrosAndCalorie(analysisResult)
                    val photoUrl = result.photoUrl
                    if (!photoUrl.isNullOrEmpty() && photoUrl != "null") {
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
                    } else {
                        binding.imageViewFoodPhoto.isVisible = false
                    }
                    binding.textViewFoodName.text = analysisResult.name
                    val streak = state.currentStreak
                    val milestones = listOf(3, 7, 14, 18, 30, 60, 100, 150, 200, 365)
                    val isMilestone = milestones.contains(streak)

                    if (streak >= 2) {
                        binding.cardStreakMilestone.isVisible = true
                        binding.textViewStreakEmoji.text = when {
                            streak >= 30 -> "👑"
                            streak >= 7 -> "🔥"
                            else -> "⚡"
                        }
                        binding.textViewStreakTitle.text = when (streak) {
                            3 -> "Ajoyib boshlanish!"
                            7 -> "Bir hafta!"
                            14 -> "Ikki hafta!"
                            18 -> "Ajoyib natija!"
                            30 -> "Bir oylik g'alaba!"
                            else -> "Davom eting!"
                        }
                        binding.textViewStreakSubtitle.text =
                            "Siz ketma-ket $streak kundan beri aktivsiz!"

                        if (isMilestone) {
                            binding.cardStreakMilestone.setCardBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.green_alpha_10
                                )
                            )
                            binding.textViewStreakTitle.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.green
                                )
                            )
                        } else {
                            binding.cardStreakMilestone.setCardBackgroundColor(
                                com.google.android.material.color.MaterialColors.getColor(
                                    binding.root,
                                    com.google.android.material.R.attr.colorSurfaceContainer
                                )
                            )
                            binding.textViewStreakTitle.setTextColor(
                                com.google.android.material.color.MaterialColors.getColor(
                                    binding.root,
                                    com.google.android.material.R.attr.colorOnSurface
                                )
                            )
                        }
                    } else {
                        binding.cardStreakMilestone.isVisible = false
                    }
                    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    binding.textViewTimestamp.text =
                        dateFormat.format(Date(analysisResult.timestamp))

                    ingredientsAdapter =
                        IngredientsAdapter(
                            ingredients = analysisResult.ingredients ?: emptyList()
                        )
                    val score = analysisResult.healthScore ?: 0
                    binding.textViewHealthScore.isVisible = false

                    val pointsDelta = when {
                        score >= 8 -> 10
                        score == 7 -> 5
                        score == 6 -> 0
                        score in 4..5 -> -5
                        else -> -10
                    }

                    binding.cardHealthScore.isVisible = score > 0 && pointsDelta > 0
                    binding.textViewPointsDelta.isVisible = pointsDelta > 0
                    binding.textViewPointsDelta.text = "+$pointsDelta ball"
                    binding.textViewPointsDelta.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.green
                        )
                    )
                    binding.textViewPointsDelta.setTextAppearance(R.style.Headline2_16)

                    val pros = analysisResult.pros
                    if (!pros.isNullOrEmpty()) {
                        binding.layoutPros.isVisible = true
                        binding.containerPros.removeAllViews()
                        pros.forEach { pro ->
                            val tv = android.widget.TextView(requireContext()).apply {
                                text = "• $pro"
                                setTextAppearance(R.style.Headline2_16)
                                setTextColor(
                                    com.google.android.material.color.MaterialColors.getColor(
                                        binding.root,
                                        com.google.android.material.R.attr.colorOnSurface
                                    )
                                )
                                layoutParams = android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    topMargin = 16
                                }
                            }
                            binding.containerPros.addView(tv)
                        }
                    } else {
                        binding.layoutPros.isVisible = false
                    }

                    binding.layoutAdvice.isVisible = false

                    binding.cardAiHealth.isVisible =
                        !pros.isNullOrEmpty()


                    val favIcon =
                        if (state.isSaved) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                    binding.toolbar.menu.findItem(R.id.action_favorite)?.setIcon(favIcon)

                    val favColor =
                        if (state.isSaved) ContextCompat.getColor(requireContext(), R.color.red)
                        else com.google.android.material.color.MaterialColors.getColor(
                            binding.root,
                            androidx.appcompat.R.attr.colorPrimary
                        )
                    binding.toolbar.menu.findItem(R.id.action_favorite)?.iconTintList =
                        android.content.res.ColorStateList.valueOf(favColor)

                    binding.buttonSaveToLog.isVisible = !state.isRecallMode
                    binding.buttonReLog.isVisible = state.isRecallMode
                    binding.buttonDelete.isVisible =
                        state.isRecallMode && arguments?.getString("logId") != null
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is AnalyzeResultEvent.Saved -> {
                        findNavController().popBackStack(R.id.home, false)
                    }

                    is AnalyzeResultEvent.Failure -> {
                        android.widget.Toast.makeText(
                            requireContext(),
                            event.message,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                    is AnalyzeResultEvent.PromptShare -> {
                        val bottomSheet = uz.oybek.ozamiz.ui.feature.community.SharePostBottomSheet(
                            foodName = event.name,
                            photoUrl = event.photoUrl,
                            calories = event.calories,
                            onShare = {
                                viewModel.saveFood(shouldShare = true)
                            },
                            onCancel = {
                                viewModel.saveFood(shouldShare = false)
                            }
                        )
                        bottomSheet.show(parentFragmentManager, "SharePostBottomSheet")
                    }

                    is AnalyzeResultEvent.LimitExceeded -> {
                        findNavController().popBackStack(R.id.home, false)
                    }
                }
            }
        }
    }

    private fun updateMacrosAndCalorie(result: FoodAnalysisResult) {
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


    private fun bindMacro(
        layout: ItemMacroDisplayBinding,
        name: String,
        value: Double?
    ) {
        layout.textViewMacroName.text = name
        val totalValue = (value ?: 0.0)
        layout.textViewMacroValue.text = "${totalValue.toInt()}gr"
    }

    private fun setupListeners() {
        binding.buttonSaveToLog.setOnSafeClickListener {
            viewModel.onSaveClick()
        }

        binding.buttonReLog.setOnSafeClickListener {
            viewModel.reLog()
        }

        binding.toolbar.inflateMenu(R.menu.menu_analyze_result)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_favorite) {
                // Immediate feedback
                val isCurrentlySaved = viewModel.resultLive.value?.isSaved == true
                menuItem.setIcon(if (isCurrentlySaved) R.drawable.ic_heart_outline else R.drawable.ic_heart_filled)
                menuItem.iconTintList = android.content.res.ColorStateList.valueOf(
                    if (isCurrentlySaved) com.google.android.material.color.MaterialColors.getColor(
                        binding.root,
                        androidx.appcompat.R.attr.colorPrimary
                    )
                    else ContextCompat.getColor(requireContext(), R.color.red)
                )

                viewModel.toggleFavorite()
                true
            } else false
        }

        binding.buttonDelete.setOnSafeClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("O'chirish")
                .setMessage("Haqiqatan ham bu yozuvni o'chirmoqchimisiz?")
                .setPositiveButton("Ha") { _, _ ->
                    viewModel.deleteLog { success ->
                        if (success) {
                            findNavController().popBackStack()
                        }
                    }
                }
                .setNegativeButton("Yo'q", null)
                .show()
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val nameEditListener = View.OnClickListener {
            val currentName = binding.textViewFoodName.text.toString()
            showEditNameDialog(currentName)
        }
        binding.btnEditName.setOnClickListener(nameEditListener)
        binding.textViewFoodName.setOnClickListener(nameEditListener)


        val editCaloriesListener = View.OnClickListener {
            val currentCalories = binding.textViewCalories.text.toString().toDoubleOrNull() ?: 0.0
            showEditNumberDialog("Kaloriyani o'zgartirish", currentCalories) { newValue ->
                viewModel.updateCalories(newValue)
            }
        }
        binding.cardCalorieBanner.setOnClickListener(editCaloriesListener)
        binding.textViewCalories.setOnClickListener(editCaloriesListener)


        binding.layoutProtein.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.protein ?: 0.0
            showEditNumberDialog("Oqsilni o'zgartirish (gr)", currentVal) { newValue ->
                viewModel.updateProtein(newValue)
            }
        }
        binding.layoutCarbs.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.carbs ?: 0.0
            showEditNumberDialog("Uglevodni o'zgartirish (gr)", currentVal) { newValue ->
                viewModel.updateCarbs(newValue)
            }
        }
        binding.layoutFats.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.fat ?: 0.0
            showEditNumberDialog("Yog'ni o'zgartirish (gr)", currentVal) { newValue ->
                viewModel.updateFat(newValue)
            }
        }
    }

    private fun showEditNameDialog(currentName: String) {
        val input = android.widget.EditText(requireContext())
        input.setText(currentName)
        input.hint = "Ovqat nomi"

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
            .setTitle("Nomni o'zgartirish")
            .setView(container)
            .setPositiveButton("Saqlash") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank() && newName != currentName) {
                    viewModel.updateLog(newName)
                }
            }
            .setNegativeButton("Bekor qilish", null)
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

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Saqlash") { _, _ ->
                val newValue = input.text.toString().toDoubleOrNull()
                if (newValue != null) {
                    onSave(newValue)
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}