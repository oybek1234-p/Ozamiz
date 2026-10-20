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
import uz.oybek.ozamiz.ui.feature.diet.recommendations.MealRecommendationsFragment
import uz.oybek.ozamiz.ui.feature.limit.FoodLimitArgs
import uz.oybek.ozamiz.ui.feature.log.food.history.RecentLogsAdapter
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
            viewModel.onAddFoodClicked()
        }

        buttonActionSearch.setOnSafeClickListener {
            val state = viewModel.uiState.value
            val args = SearchFoodArgs(mealType = state.mealType)
            navigate(R.id.searchFoodFragment, args.toBundle())
        }

        buttonActionWrite.setOnSafeClickListener {
            val state = viewModel.uiState.value
            val args = WriteFoodArgs(
                mealType = state.mealType,
                timestamp = state.date.time
            )
            navigate(R.id.writeFoodFragment, args.toBundle())
        }

        btnMealReminder.setOnSafeClickListener {
            navigate(R.id.remindersFragment)
        }

        cardMealRecommendations.setOnSafeClickListener {
            showMealRecommendations()
        }
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

            is LogFoodUiEffect.ShowLimitSheet -> {
                val args = FoodLimitArgs(
                    remainingLimit = effect.remainingLimit,
                    targetLimit = effect.targetLimit,
                    mealType = effect.mealType
                )
                navigate(R.id.foodLimitBottomSheet, args.toBundle())
            }
        }
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
        recentLogsAdapter = null
        super.onDestroyView()
    }
}
