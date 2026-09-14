package uz.oybek.ozamiz.ui.feature.onboarding.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import uz.oybek.ozamiz.databinding.GetInfoMainFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import kotlin.math.roundToInt

class GetMainInfoFragment : BaseFragment<GetInfoMainFragmentBinding>() {

    private val viewModel: GetInfoViewModel by viewModels()
    private var selectedGender: Int = -1
    private val selectedProblems = mutableListOf<String>()
    private var isNewUser: Boolean = true
    private var activityLevelSelected: String = UserActivityLevel.LIGHTLY_ACTIVE.name
    private var manzilSelected: String = "Uzbekistan"

    private var selectedGoal: String = "lose_weight"
    private var selectedPace: Int = 500

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): GetInfoMainFragmentBinding {
        return GetInfoMainFragmentBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNewUser = arguments?.getBoolean("new", true) ?: true
        viewModel.setSetupMode(isNewUser)
    }

    override fun viewCreated(bind: GetInfoMainFragmentBinding) {
        setupToolbar(bind)
        loadInitialData(bind)
        setupListeners(bind)
        observeViewModel(bind)
    }

    private fun setupToolbar(bind: GetInfoMainFragmentBinding) {
        bind.toolbar.apply {
            setNavigationIcon(R.drawable.arrow_back)
            setNavigationOnClickListener { popBackStack() }
        }
    }

    private fun loadInitialData(bind: GetInfoMainFragmentBinding) = with(bind) {
        val user = LocalUser.user

        nameView.editText?.setText(user.name)
        ageView.editText?.setText(if (user.age > 0) user.age.toString() else "")
        mobilRaqamView.editText?.setText(user.phoneNumber)

        heightLay.editText?.setText(if (user.height > 0) user.height.toString() else "")
        weightInput.editText?.setText(
            if (user.weight > 0) String.format(java.util.Locale.US, "%.1f", user.weight) else ""
        )
        targetWeightView.editText?.setText(user.targetWeight?.let {
            String.format(java.util.Locale.US, "%.1f", it)
        } ?: "")

        val activityAdapter = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            UserActivityLevel.entries.map { "${it.title} - ${it.subtitle}" }
        )

        (harakatView.editText as? AutoCompleteTextView)?.apply {
            setAdapter(activityAdapter)
            UserActivityLevel.entries.find { it.name == user.activityLevel }?.let {
                activityLevelSelected = it.name
                setText("${it.title} - ${it.subtitle}", false)
            }
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                activityLevelSelected = UserActivityLevel.entries[position].name
                recalculate(bind)
            }
        }

        selectedGender = user.gender
        updateGenderSelectionUI(this)
        calculateAndDisplayIdealWeightIfNeeded(this)

        selectedProblems.clear()
        user.problems?.let { selectedProblems.addAll(it) }

        selectedGoal = if (!user.goal.isNullOrBlank()) user.goal else {
            val targetW = user.targetWeight ?: 65.0
            if (targetW > user.weight) "gain_weight" else "lose_weight"
        }
        val tdee = DietController.calculateTDEE(user).roundToInt()
        val diff = if (selectedGoal == "gain_weight") (user.targetCalories - tdee) else (tdee - user.targetCalories)
        selectedPace = if (selectedGoal == "gain_weight") {
            if (diff >= 500) 600 else 400
        } else {
            if (diff >= 600) 700 else 500
        }

        updateGoalCardsUI(bind)
        updatePaceCardsUI(bind)
        recalculate(bind)

        val targetCals = if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(user)
        bind.editCalories.setText(targetCals.toString())
        bind.editProtein.setText(user.targetProtein.toString())
        bind.editFat.setText(user.targetFat.toString())
        bind.editCarbs.setText(user.targetCarbs.toString())
    }

    private fun setupListeners(bind: GetInfoMainFragmentBinding) = with(bind) {
        male.setOnClickListener {
            selectedGender = Gender.MALE
            updateGenderSelectionUI(bind)
            calculateAndDisplayIdealWeightIfNeeded(bind)
            recalculate(bind)
        }
        female.setOnClickListener {
            selectedGender = Gender.FEMALE
            updateGenderSelectionUI(bind)
            calculateAndDisplayIdealWeightIfNeeded(bind)
            recalculate(bind)
        }

        heightLay.editText?.doAfterTextChanged {
            calculateAndDisplayIdealWeightIfNeeded(bind)
            recalculate(bind)
        }
        weightInput.editText?.doAfterTextChanged {
            validateTargetWeight(bind)
            recalculate(bind)
        }
        targetWeightView.editText?.doAfterTextChanged {
            validateTargetWeight(bind)
            recalculate(bind)
        }
        ageView.editText?.doAfterTextChanged {
            recalculate(bind)
        }

        // Goal Cards (Ozish / Semirish)
        cardLoseWeight.setOnClickListener {
            selectedGoal = "lose_weight"
            selectedPace = 500
            updateGoalCardsUI(bind)
            updatePaceCardsUI(bind)
            recalculate(bind)
        }
        cardGainWeight.setOnClickListener {
            selectedGoal = "gain_weight"
            selectedPace = 400
            updateGoalCardsUI(bind)
            updatePaceCardsUI(bind)
            recalculate(bind)
        }

        // Speed Cards (Normal / Tezroq)
        cardPaceNormal.setOnClickListener {
            selectedPace = if (selectedGoal == "gain_weight") 400 else 500
            updatePaceCardsUI(bind)
            recalculate(bind)
        }
        cardPaceFast.setOnClickListener {
            selectedPace = if (selectedGoal == "gain_weight") 600 else 700
            updatePaceCardsUI(bind)
            recalculate(bind)
        }

        save.setOnClickListener {
            handleSaveClick(bind)
        }
    }

    private fun updateGoalCardsUI(bind: GetInfoMainFragmentBinding) {
        val primaryColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, 0)
        val outlineColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant, 0)
        val cardBgLowest = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerLowest, 0)
        val primaryContainer = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryContainer, 0)

        val isLose = selectedGoal == "lose_weight"

        bind.cardLoseWeight.strokeWidth = if (isLose) 2.dpToPx() else 1.dpToPx()
        bind.cardLoseWeight.strokeColor = if (isLose) primaryColor else outlineColor
        bind.cardLoseWeight.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(if (isLose) primaryContainer else cardBgLowest))
        bind.cardLoseWeight.cardElevation = if (isLose) 6.dpToPx().toFloat() else 0f

        bind.cardGainWeight.strokeWidth = if (!isLose) 2.dpToPx() else 1.dpToPx()
        bind.cardGainWeight.strokeColor = if (!isLose) primaryColor else outlineColor
        bind.cardGainWeight.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(if (!isLose) primaryContainer else cardBgLowest))
        bind.cardGainWeight.cardElevation = if (!isLose) 6.dpToPx().toFloat() else 0f
    }

    private fun updatePaceCardsUI(bind: GetInfoMainFragmentBinding) {
        val selectedBg = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerHigh, 0)
        val unselectedBg = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerLowest, 0)

        val selectedStroke = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, 0)
        val unselectedStroke = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant, 0)

        val onSurfaceColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, 0)
        val onSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val surfaceContainerHighest = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerHighest, 0)

        val isGain = selectedGoal == "gain_weight"
        val isNormalSelected = selectedPace == 500 || selectedPace == 400

        bind.textSpeedSectionTitle.text = if (isGain) "Semirish tezligini tanlang" else "Ozish tezligini tanlang"

        // --- NORMAL CARD ---
        val normalBg = if (isNormalSelected) selectedBg else unselectedBg
        bind.cardPaceNormal.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(normalBg))
        bind.cardPaceNormal.strokeWidth = if (isNormalSelected) 2.dpToPx() else 1.dpToPx()
        bind.cardPaceNormal.strokeColor = if (isNormalSelected) selectedStroke else unselectedStroke
        bind.cardPaceNormal.cardElevation = if (isNormalSelected) 2.dpToPx().toFloat() else 0f

        bind.framePaceNormalIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isNormalSelected) surfaceContainerHighest else unselectedBg
        )
        bind.iconPaceNormal.imageTintList = android.content.res.ColorStateList.valueOf(
            if (isNormalSelected) onSurfaceColor else onSurfaceVariant
        )
        bind.textPaceNormalLabel.setTextColor(if (isNormalSelected) onSurfaceColor else onSurfaceVariant)
        bind.textPaceNormalTitle.setTextColor(if (isNormalSelected) onSurfaceColor else onSurfaceVariant)

        // --- FAST CARD ---
        val fastBg = if (!isNormalSelected) selectedBg else unselectedBg
        bind.cardPaceFast.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(fastBg))
        bind.cardPaceFast.strokeWidth = if (!isNormalSelected) 2.dpToPx() else 1.dpToPx()
        bind.cardPaceFast.strokeColor = if (!isNormalSelected) selectedStroke else unselectedStroke
        bind.cardPaceFast.cardElevation = if (!isNormalSelected) 2.dpToPx().toFloat() else 0f

        bind.framePaceFastIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (!isNormalSelected) surfaceContainerHighest else unselectedBg
        )
        bind.iconPaceFast.imageTintList = android.content.res.ColorStateList.valueOf(
            if (!isNormalSelected) onSurfaceColor else onSurfaceVariant
        )
        bind.textPaceFastLabel.setTextColor(if (!isNormalSelected) onSurfaceColor else onSurfaceVariant)
        bind.textPaceFastTitle.setTextColor(if (!isNormalSelected) onSurfaceColor else onSurfaceVariant)

        // Update Subtitle Texts
        if (isGain) {
            bind.textPaceNormalTitle.text = "+1.6 kg / oy"
            bind.textPaceFastTitle.text = "+2.4 kg / oy"
        } else {
            bind.textPaceNormalTitle.text = "-1.8 kg / oy"
            bind.textPaceFastTitle.text = "-2.8 kg / oy"
        }
    }

    private fun recalculate(bind: GetInfoMainFragmentBinding) {
        val curW = bind.weightInput.editText?.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: LocalUser.user.weight
        val tarW = bind.targetWeightView.editText?.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: (LocalUser.user.targetWeight ?: 65.0)
        val h = bind.heightLay.editText?.text?.toString()?.toIntOrNull() ?: LocalUser.user.height
        val a = bind.ageView.editText?.text?.toString()?.toIntOrNull() ?: LocalUser.user.age

        val tempUser = User(
            gender = if (selectedGender != -1) selectedGender else Gender.MALE,
            weight = if (curW > 0) curW else 70.0,
            height = if (h > 0) h else 170,
            age = if (a > 0) a else 25,
            activityLevel = activityLevelSelected,
            goal = selectedGoal
        )

        val tdee = DietController.calculateTDEE(tempUser).roundToInt()

        val isGain = selectedGoal == "gain_weight"
        val deltaCals = if (selectedPace > 0) selectedPace else (if (isGain) 400 else 500)

        val minCals = if (tempUser.gender == Gender.MALE) 1200 else 1000
        val targetCals = if (isGain) {
            tdee + deltaCals
        } else {
            maxOf(tdee - deltaCals, minCals)
        }

        bind.editCalories.setText(targetCals.toString())

        val macros = if (isGain) {
            DietController.calculateMacrosByPercent(targetCals, 0.25, 0.25, 0.50)
        } else {
            DietController.calculateMacrosByPercent(targetCals, 0.35, 0.25, 0.40)
        }

        bind.editProtein.setText(macros.protein.toString())
        bind.editFat.setText(macros.fat.toString())
        bind.editCarbs.setText(macros.carbs.toString())

        val actualDeficit = if (isGain) deltaCals else maxOf(0, tdee - targetCals)

        val weeklyKg = if (isGain) {
            if (deltaCals == 600) 0.6 else 0.4
        } else {
            val calcW = (actualDeficit * 7.0) / 7700.0
            if (calcW > 0) calcW else 0.5
        }

        val (weeklyStr, monthlyStr) = if (isGain) {
            if (deltaCals == 600) Pair("0.6", "2.4") else Pair("0.4", "1.6")
        } else {
            val calcW = (actualDeficit * 7.0) / 7700.0
            val calcM = (actualDeficit * 30.0) / 7700.0
            Pair(
                String.format(java.util.Locale.US, "%.1f", calcW),
                String.format(java.util.Locale.US, "%.1f", calcM)
            )
        }

        val durationStr = getDurationText(curW, tarW, weeklyKg)

        if (isGain) {
            bind.textEnergySummary.text =
                "• Sizning tanangiz kuniga taxminan $tdee kkal energiya sarflaydi,\n\n• $deltaCals kkal ortiqcha yesak vazn yig'ish boshlanadi.\n\n• Natijada haftasiga $weeklyStr kg, oyiga $monthlyStr kg semirasiz.\n\n• Maqsadga esa $durationStr erishasiz."
        } else {
            val deficitAmount = tdee - targetCals
            val defVal = if (deficitAmount > 0) deficitAmount else deltaCals
            bind.textEnergySummary.text =
                "• Sizning tanangiz kuniga taxminan $tdee kkal energiya sarflaydi,\n\n• $defVal kkal kamroq ovqat yeymiz natijada haftasiga $weeklyStr kg, oyiga $monthlyStr kg ozasiz.\n\n• Maqsadga esa $durationStr erishasiz."
        }
    }

    private fun getDurationText(currentWeight: Double, targetWeight: Double, weeklyKg: Double): String {
        val diff = kotlin.math.abs(currentWeight - targetWeight)
        if (diff <= 0.1 || weeklyKg <= 0.0) return "erishgansiz"
        val weeksNeeded = diff / weeklyKg
        val daysNeeded = (weeksNeeded * 7).roundToInt()

        val months = daysNeeded / 30
        val remainingDays = daysNeeded % 30
        return when {
            months > 0 && remainingDays > 0 -> "$months oy $remainingDays kunda"
            months > 0 -> "$months oyda"
            else -> "$daysNeeded kunda"
        }
    }

    private fun handleSaveClick(bind: GetInfoMainFragmentBinding) {
        val name = bind.nameView.editText?.text?.toString()?.trim()
        val ageStr = bind.ageView.editText?.text?.toString()
        val heightStr = bind.heightLay.editText?.text?.toString()
        val weightStr = bind.weightInput.editText?.text?.toString()?.replace(',', '.')
        val targetWeightStr = bind.targetWeightView.editText?.text?.toString()?.replace(',', '.')
        val phone = bind.mobilRaqamView.editText?.text?.toString()?.trim()

        if (name.isNullOrEmpty()) {
            showToast("Iltimos, ismingizni kiriting")
            return
        }

        if (selectedGender == -1) {
            showToast("Iltimos, jinsingizni belgilang")
            return
        }

        val age = ageStr?.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            showToast("Iltimos, yoshingizni to'g me'yoriy to'g'ri kiriting")
            return
        }

        val height = heightStr?.toIntOrNull()
        if (height == null || height < 50 || height > 250) {
            showToast("Iltimos, bo'yingizni to'g'ri kiriting (sm)")
            return
        }

        val weight = weightStr?.toDoubleOrNull()
        if (weight == null || weight < 30.0 || weight > 500.0) {
            showToast("Iltimos, vazningizni to'g'ri kiriting (30–500 kg)")
            return
        }

        var targetWeight: Double? = null
        if (!targetWeightStr.isNullOrEmpty()) {
            targetWeight = targetWeightStr.toDoubleOrNull()
            if (targetWeight == null || targetWeight < 30.0 || targetWeight > 500.0) {
                showToast("Maqsad vaznni to'g'ri kiriting (30–500 kg)")
                return
            }
        }

        if (activityLevelSelected.isEmpty()) {
            showToast("Iltimos, harakat darajasini tanlang")
            return
        }

        val calories = bind.editCalories.text.toString().toIntOrNull() ?: 0
        val protein = bind.editProtein.text.toString().toIntOrNull() ?: 0
        val fat = bind.editFat.text.toString().toIntOrNull() ?: 0
        val carbs = bind.editCarbs.text.toString().toIntOrNull() ?: 0

        viewModel.saveUserInfoAndGeneratePlan(
            name = name,
            gender = selectedGender,
            age = age,
            height = height,
            weight = weight,
            targetWeight = targetWeight,
            problems = selectedProblems.ifEmpty { null },
            phoneNumber = phone.toString(),
            activityLevel = activityLevelSelected,
            manzil = manzilSelected,
            goal = selectedGoal,
            targetCalories = calories,
            targetProtein = protein,
            targetFat = fat,
            targetCarbs = carbs
        )
    }

    private fun calculateAndDisplayIdealWeightIfNeeded(bind: GetInfoMainFragmentBinding) {
        val heightStr = bind.heightLay.editText?.text?.toString()
        val height = heightStr?.toIntOrNull()
        viewModel.calculateAndSetIdealWeight(height, selectedGender)
    }

    private fun validateTargetWeight(bind: GetInfoMainFragmentBinding) {
        val currentWeight =
            bind.weightInput.editText?.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        val targetWeight =
            bind.targetWeightView.editText?.text?.toString()?.replace(',', '.')?.toDoubleOrNull()

        if (currentWeight == null || targetWeight == null) {
            bind.targetWeightWarning.isVisible = false
            return
        }
    }

    private fun updateGenderSelectionUI(bind: GetInfoMainFragmentBinding) {
        val primaryColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, 0)
        val outlineColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant, 0)
        val cardBgLowest = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerLowest, 0)
        val primaryContainer = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryContainer, 0)

        val maleSelected = selectedGender == Gender.MALE
        val femaleSelected = selectedGender == Gender.FEMALE

        bind.male.strokeWidth = if (maleSelected) 2.dpToPx() else 1.dpToPx()
        bind.male.strokeColor = android.content.res.ColorStateList.valueOf(if (maleSelected) primaryColor else outlineColor)
        bind.male.backgroundTintList = android.content.res.ColorStateList.valueOf(if (maleSelected) primaryContainer else cardBgLowest)

        bind.female.strokeWidth = if (femaleSelected) 2.dpToPx() else 1.dpToPx()
        bind.female.strokeColor = android.content.res.ColorStateList.valueOf(if (femaleSelected) primaryColor else outlineColor)
        bind.female.backgroundTintList = android.content.res.ColorStateList.valueOf(if (femaleSelected) primaryContainer else cardBgLowest)
    }

    private fun observeViewModel(bind: GetInfoMainFragmentBinding) {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            bind.progressBar.isVisible = isLoading
            bind.save.isEnabled = !isLoading
        }

        viewModel.idealWeightRangeText.observe(viewLifecycleOwner) { rangeText ->
            val hasValue = rangeText != null
            bind.textViewIdealWeightRange.isVisible = hasValue
            if (hasValue) {
                bind.textViewIdealWeightRange.text = "Sizning ideal vazningiz: $rangeText"
            }
            validateTargetWeight(bind)
        }
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is GetInfoNavigationEvent.ShowToast -> showToast(event.message)
                is GetInfoNavigationEvent.NavigateToHome -> {
                    popBackStack()
                }

                is GetInfoNavigationEvent.NavigateToCongratulation -> {
                    popBackStack()
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}