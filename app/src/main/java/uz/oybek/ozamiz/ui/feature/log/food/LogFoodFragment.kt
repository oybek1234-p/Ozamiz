package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.ui.modelUtils.MealTime
import uz.oybek.ozamiz.data.tracker.model.FoodModel
import uz.oybek.ozamiz.data.tracker.model.MealType
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.common.dialog.ConfirmDeleteDialog
import uz.oybek.ozamiz.data.dietolog.engine.DietologMessagePool
import uz.oybek.ozamiz.ui.feature.diet.recommendations.MealRecommendationsFragment
import uz.oybek.ozamiz.ui.feature.limit.FoodLimitArgs
import uz.oybek.ozamiz.ui.feature.log.food.history.RecentLogsAdapter
import uz.oybek.ozamiz.data.consultant.MealConsultantMode
import uz.oybek.ozamiz.ui.feature.consultant.CanIEatBottomSheet
import uz.oybek.ozamiz.ui.feature.consultant.MealConsultantResultFragment
import uz.oybek.ozamiz.ui.feature.consultant.WhatToEatBottomSheet
import uz.oybek.ozamiz.ui.utils.appColors
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.loadBannerAd
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by viewModels()
    private var recentLogsAdapter: RecentLogsAdapter? = null
    private var currentEmptyTip: String? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLogFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentLogFoodBinding) {
        setupUI()
        observeState()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = navigateToHome()
            }
        )
    }

    private fun setupUI() = with(binding) {
        toolbarLogFood.setNavigationOnClickListener { navigateToHome() }

        val adapter = RecentLogsAdapter(
            onItemClick = ::onFoodLogClicked,
            onDeleteClick = ::showDeleteDialog
        )
        recentLogsAdapter = adapter
        recyclerRecentLogs.apply {
            this.adapter = adapter
            itemAnimator = DefaultItemAnimator().apply {
                supportsChangeAnimations = false
                removeDuration = 200L
                addDuration = 200L
                moveDuration = 200L
            }
        }

        buttonSavedFoods.setOnSafeClickListener {
            val state = viewModel.uiState.value
            val args = SavedFoodsArgs(
                mealType = state.mealType,
                timestamp = state.date.time
            )
            navigate(R.id.savedFoodsFragment, args.toBundle())
        }

        buttonActionCamera.setOnSafeClickListener {
            val state = viewModel.uiState.value
            if (state.canLog) {
                viewModel.onAddFoodClicked()
            } else {
                navigateToPremium(state.mealType)
            }
        }

        buttonActionSearch.setOnSafeClickListener {
            val state = viewModel.uiState.value
            val args = SearchFoodArgs(mealType = state.mealType)
            navigate(R.id.searchFoodFragment, args.toBundle())
        }

        buttonActionWrite.setOnSafeClickListener {
            val state = viewModel.uiState.value
            if (state.canLog) {
                val args = WriteFoodArgs(
                    mealType = state.mealType,
                    timestamp = state.date.time
                )
                navigate(R.id.writeFoodFragment, args.toBundle())
            } else {
                navigateToPremium(state.mealType)
            }
        }

        btnCanIEat.setOnSafeClickListener {
            val state = viewModel.uiState.value
            if (state.canLog) {
                openCanIEatBottomSheet()
            } else {
                navigateToPremium(state.mealType)
            }
        }

        btnWhatToEat.setOnSafeClickListener {
            val state = viewModel.uiState.value
            if (state.canLog) {
                openWhatToEatBottomSheet()
            } else {
                navigateToPremium(state.mealType)
            }
        }

        btnMealReminder.setOnSafeClickListener {
            navigate(R.id.remindersFragment)
        }

        cardMealRecommendations.setOnSafeClickListener {
            showMealRecommendations()
        }
    }

    private fun openCanIEatBottomSheet() {
        val currentMealType = viewModel.uiState.value.mealType
        CanIEatBottomSheet.newInstance { userInput, imageUri ->
            val bundle = MealConsultantResultFragment.createBundle(
                mode = MealConsultantMode.CAN_I_EAT,
                userInput = userInput,
                imageUri = imageUri,
                mealType = currentMealType
            )
            navigate(R.id.mealConsultantResultFragment, bundle)
        }.show(childFragmentManager, CanIEatBottomSheet.TAG)
    }

    private fun openWhatToEatBottomSheet() {
        val currentMealType = viewModel.uiState.value.mealType
        WhatToEatBottomSheet.newInstance(mealType = currentMealType) { selectedType ->
            if (selectedType == uz.oybek.ozamiz.data.consultant.WhatToEatType.SMART_RECOMMENDATION) {
                val bundle = MealConsultantResultFragment.createBundle(
                    mode = MealConsultantMode.WHAT_TO_EAT,
                    subType = selectedType,
                    userInput = "",
                    mealType = currentMealType
                )
                navigate(R.id.mealConsultantResultFragment, bundle)
            } else {
                uz.oybek.ozamiz.ui.feature.consultant.WhatToEatInputBottomSheet.newInstance(selectedType) { userInput ->
                    val bundle = MealConsultantResultFragment.createBundle(
                        mode = MealConsultantMode.WHAT_TO_EAT,
                        subType = selectedType,
                        userInput = userInput,
                        mealType = currentMealType
                    )
                    navigate(R.id.mealConsultantResultFragment, bundle)
                }.show(childFragmentManager, uz.oybek.ozamiz.ui.feature.consultant.WhatToEatInputBottomSheet.TAG)
            }
        }.show(childFragmentManager, WhatToEatBottomSheet.TAG)
    }

    private fun showMealRecommendations() {
        val mealType = MealType.fromId(viewModel.uiState.value.mealType)
        navigate(
            R.id.mealRecommendationsFragment,
            bundleOf(MealRecommendationsFragment.ARG_MEAL_TYPE to mealType.name)
        )
    }

    private fun observeState() {
        collectFlowOnStart(viewModel.uiState, ::renderUi)
        collectFlowOnStart(viewModel.effects, ::handleEffect)
    }

    private fun renderUi(state: LogFoodUiState) = with(binding) {
        val meal = MealTime.fromId(state.mealType)
        val mealName = meal?.let { getString(it.nameRes) } ?: ""
        toolbarLogFood.title = mealName
        cardMealRecommendations.isVisible = false

        tvPremiumBadgeCamera.isVisible = !state.canLog
        tvPremiumBadgeWrite.isVisible = !state.canLog

        if (!state.mealTime.isNullOrBlank()) {
            textToolbarMealTime.text = state.mealTime
            btnMealReminder.isVisible = true
        } else {
            btnMealReminder.isVisible = false
        }

        meal?.let {
            imgEmptyMealIcon.setImageResource(it.iconRes)
            imgEmptyMealIcon.imageTintList = appColors.colorStateList(it.getColor(appColors))
        }

        if (state.isLoaded) {
            val hasLogs = state.logs.isNotEmpty()
            if (!hasLogs) {
                val mealType = MealType.fromId(state.mealType, state.date)
                val message = currentEmptyTip ?: when (mealType) {
                    MealType.BREAKFAST -> DietologMessagePool.breakfastLoseWeightMessages.random()
                    MealType.LUNCH -> DietologMessagePool.lunchAndActivityMessages.random()
                    MealType.DINNER -> DietologMessagePool.dinnerMessages.random()
                    MealType.SNACK -> DietologMessagePool.snackIntervalMessages.random()
                }.also { currentEmptyTip = it }

                textEmptyTitle.setText(R.string.log_food_empty_title)
                textEmptySubtitle.text = message
            }
            recentLogsAdapter?.submitList(state.logs) {
                recyclerRecentLogs.isVisible = hasLogs
                textRecentHistoryHeader.isVisible = hasLogs
                layoutEmptyHistory.isVisible = !hasLogs
            }
        } else {
            recyclerRecentLogs.isVisible = false
            textRecentHistoryHeader.isVisible = false
            layoutEmptyHistory.isVisible = false
        }

        bannerAdContainerLogFood.loadBannerAd(AdsConfig.BANNER_LOG_FOOD)
    }

    private fun handleEffect(effect: LogFoodUiEffect) {
        when (effect) {
            is LogFoodUiEffect.ShowToastRes -> {
                context?.let { ctx -> showToast(ctx.getString(effect.messageResId)) }
            }

            is LogFoodUiEffect.NavigateToCamera -> {
                openFoodCamera(effect.mealType)
            }

            is LogFoodUiEffect.NavigateToPremium -> {
                navigateToPremium(effect.mealType)
            }
        }
    }

    private fun navigateToPremium(mealType: String) {
        navigate(R.id.premiumFragment, bundleOf("mealType" to mealType))
    }

    private fun onFoodLogClicked(foodLog: FoodModel) {
        val args = AnalyzeResultArgs.ExistingLog(
            logId = foodLog.logId,
            timestamp = viewModel.uiState.value.date.time
        )
        navigate(R.id.analyzeResultFragment, args.toBundle())
    }

    private fun showDeleteDialog(foodLog: FoodModel) {
        val ctx = context ?: return
        ConfirmDeleteDialog.show(
            context = ctx,
            titleRes = R.string.log_food_delete_confirm_title,
            messageRes = R.string.log_food_delete_confirm_msg,
            onConfirm = {
                viewModel.deleteFoodLog(foodLog)
            }
        )
    }

    private fun openFoodCamera(mealType: String) {
        val args = FoodCameraArgs(mealType = mealType)
        navigate(R.id.foodCameraFragment, args.toBundle()) {
            setPopUpTo(R.id.logFoodFragment, true)
        }
    }

    private fun navigateToHome() {
        popBackStack()
    }

    override fun onDestroyView() {
        currentEmptyTip = null
        recentLogsAdapter = null
        super.onDestroyView()
    }
}
