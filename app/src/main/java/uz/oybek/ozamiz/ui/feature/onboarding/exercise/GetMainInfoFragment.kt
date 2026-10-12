package uz.oybek.ozamiz.ui.feature.onboarding.exercise

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.data.profile.model.UserActivityLevel
import uz.oybek.ozamiz.data.profile.model.UserGender
import uz.oybek.ozamiz.data.profile.model.UserGoal
import uz.oybek.ozamiz.databinding.GetInfoMainFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.modelUtils.getFormattedTitle
import uz.oybek.ozamiz.ui.utils.appColors
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.doubleOrNull
import uz.oybek.ozamiz.ui.utils.dpToPx
import uz.oybek.ozamiz.ui.utils.intOrNull
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.textOrEmpty

@AndroidEntryPoint
class GetMainInfoFragment : BaseFragment<GetInfoMainFragmentBinding>() {

    private val viewModel: GetInfoViewModel by viewModels()
    private var isInitialFormPopulated: Boolean = false

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): GetInfoMainFragmentBinding {
        return GetInfoMainFragmentBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: GetInfoMainFragmentBinding) {
        initToolbar(bind)
        initFormInputs(bind)
        initSelectionCards(bind)
        initSaveButton(bind)
        observeViewModel(bind)
    }

    private fun initToolbar(bind: GetInfoMainFragmentBinding) = with(bind) {
        toolbar.apply {
            setNavigationIcon(R.drawable.arrow_back)
            setNavigationOnClickListener { popBackStack() }
        }
    }

    private fun initFormInputs(bind: GetInfoMainFragmentBinding) = with(bind) {
        val activityAdapter = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            UserActivityLevel.entries.map { it.getFormattedTitle(requireContext()) }
        )

        (harakatView.editText as? AutoCompleteTextView)?.apply {
            setAdapter(activityAdapter)
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val selectedLevel = UserActivityLevel.entries.getOrNull(position)?.name
                    ?: return@OnItemClickListener
                viewModel.onActivityLevelChanged(selectedLevel)
            }
        }

        val onMetricsChangedAction: (CharSequence?) -> Unit = { triggerMetricsRecalculation(bind) }
        heightLay.editText?.doAfterTextChanged(onMetricsChangedAction)
        weightInput.editText?.doAfterTextChanged(onMetricsChangedAction)
        targetWeightView.editText?.doAfterTextChanged(onMetricsChangedAction)
        ageView.editText?.doAfterTextChanged(onMetricsChangedAction)
    }

    private fun initSelectionCards(bind: GetInfoMainFragmentBinding) = with(bind) {
        male.setOnSafeClickListener { viewModel.onGenderChanged(UserGender.MALE.id) }
        female.setOnSafeClickListener { viewModel.onGenderChanged(UserGender.FEMALE.id) }

        cardLoseWeight.setOnSafeClickListener {
            viewModel.onGoalChanged(UserGoal.LOSE_WEIGHT)
        }
        cardGainWeight.setOnSafeClickListener {
            viewModel.onGoalChanged(UserGoal.GAIN_WEIGHT)
        }
    }

    private fun initSaveButton(bind: GetInfoMainFragmentBinding) = with(bind) {
        save.setOnSafeClickListener {
            viewModel.saveUserInfo(
                name = nameView.textOrEmpty(),
                phone = mobilRaqamView.textOrEmpty(),
                height = heightLay.intOrNull(),
                weight = weightInput.doubleOrNull(),
                targetWeight = targetWeightView.doubleOrNull(),
                age = ageView.intOrNull()
            )
        }
    }

    private fun observeViewModel(bind: GetInfoMainFragmentBinding) {
        collectFlowOnStart(
            viewModel.uiState.map { it.isLoading }.distinctUntilChanged()
        ) { isLoading ->
            bindLoadingState(bind, isLoading)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.user }.distinctUntilChanged()
        ) { user ->
            bindUserData(bind, user)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.user.gender }.distinctUntilChanged()
        ) { gender ->
            bindGenderSelection(bind, gender)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.user.userGoal }.distinctUntilChanged()
        ) { goal ->
            bindGoalSelection(bind, goal)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.idealWeightRange }.distinctUntilChanged()
        ) { idealRange ->
            bindIdealWeightRange(bind, idealRange)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.targetWeightWarningRes }.distinctUntilChanged()
        ) { warningRes ->
            bindTargetWeightWarning(bind, warningRes)
        }

        collectFlowOnStart(viewModel.effects, ::handleEffect)
    }

    private fun bindUserData(bind: GetInfoMainFragmentBinding, user: User) = with(bind) {
        if (!isInitialFormPopulated && user.valid) {
            nameView.editText?.setText(user.name)
            mobilRaqamView.editText?.setText(user.phoneNumber)

            if (user.age > 0) ageView.editText?.setText(user.age.toString())
            if (user.height > 0) heightLay.editText?.setText(user.height.toString())
            if (user.weight > 0.0) weightInput.editText?.setText(user.weight.readableFormat())
            user.targetWeight?.takeIf { it > 0 }?.let {
                targetWeightView.editText?.setText(it.readableFormat())
            }

            isInitialFormPopulated = true
        }

        val formattedActivity = user.userActivityLevel.getFormattedTitle(requireContext())
        (harakatView.editText as? AutoCompleteTextView)?.setText(formattedActivity, false)
    }

    private fun bindGenderSelection(bind: GetInfoMainFragmentBinding, gender: Int) = with(bind) {
        bindGenderButton(male, gender == UserGender.MALE.id)
        bindGenderButton(female, gender == UserGender.FEMALE.id)
    }

    private fun bindGenderButton(button: MaterialButton, isSelected: Boolean) {
        val colors = appColors
        button.strokeWidth = if (isSelected) dpToPx(2) else dpToPx(1)
        button.strokeColor =
            ColorStateList.valueOf(if (isSelected) colors.colorPrimary else colors.colorOutlineVariant)
        button.backgroundTintList =
            ColorStateList.valueOf(if (isSelected) colors.colorSelectedCard else colors.colorSurfaceContainerLowest)
    }

    private fun bindGoalSelection(bind: GetInfoMainFragmentBinding, goal: UserGoal) = with(bind) {
        bindGoalCard(cardLoseWeight, goal.isLose)
        bindGoalCard(cardGainWeight, goal.isGain)
    }

    private fun bindGoalCard(card: MaterialCardView, isSelected: Boolean) {
        val colors = appColors
        card.strokeWidth = if (isSelected) dpToPx(2) else dpToPx(1)
        card.strokeColor = if (isSelected) colors.colorPrimary else colors.colorOutlineVariant
        card.setCardBackgroundColor(if (isSelected) colors.colorSelectedCard else colors.colorSurfaceContainerLowest)
        card.cardElevation = 0f
    }

    private fun bindIdealWeightRange(
        bind: GetInfoMainFragmentBinding,
        idealWeightRange: Pair<Int, Int>?
    ) = with(bind) {
        val hasIdealRange = idealWeightRange != null
        textViewIdealWeightRange.isVisible = hasIdealRange
        if (idealWeightRange != null) {
            val rangeFormatted = getString(
                R.string.ideal_weight_range_format,
                idealWeightRange.first.toString(),
                idealWeightRange.second.toString()
            )
            textViewIdealWeightRange.text =
                getString(R.string.ideal_weight_label_format, rangeFormatted)
        }
    }

    private fun bindTargetWeightWarning(
        bind: GetInfoMainFragmentBinding,
        warningRes: Int?
    ) = with(bind) {
        targetWeightWarning.isVisible = warningRes != null
        if (warningRes != null) {
            targetWeightWarning.setText(warningRes)
        }
    }

    private fun bindLoadingState(
        bind: GetInfoMainFragmentBinding,
        isLoading: Boolean
    ) = with(bind) {
        progressBar.isVisible = isLoading
        save.isEnabled = !isLoading
        save.text = if (isLoading) "" else getString(R.string.save)
    }

    private fun handleEffect(effect: GetMainInfoUiEffect) {
        when (effect) {
            is GetMainInfoUiEffect.ShowToast -> Toast.makeText(
                requireContext(),
                effect.messageRes,
                Toast.LENGTH_SHORT
            ).show()

            is GetMainInfoUiEffect.ShowToastMessage -> Toast.makeText(
                requireContext(),
                effect.message,
                Toast.LENGTH_SHORT
            ).show()

            is GetMainInfoUiEffect.NavigateBack -> popBackStack()
        }
    }

    private fun triggerMetricsRecalculation(bind: GetInfoMainFragmentBinding) {
        viewModel.onMetricsChanged(
            height = bind.heightLay.intOrNull(),
            weight = bind.weightInput.doubleOrNull(),
            targetWeight = bind.targetWeightView.doubleOrNull(),
            age = bind.ageView.intOrNull()
        )
    }
}