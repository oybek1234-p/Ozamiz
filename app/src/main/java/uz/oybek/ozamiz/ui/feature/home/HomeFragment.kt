package uz.oybek.ozamiz.ui.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var dateAdapter: DateScrollerAdapter

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(
            inflater,
            container,
            false
        )
    }

    override fun viewCreated(bind: FragmentHomeBinding) {
        setupToolbar(bind)
        setupDateScroller(bind)
        setupClickListeners(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
            bind.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupDateScroller(bind: FragmentHomeBinding) {
        dateAdapter = DateScrollerAdapter { date ->
            viewModel.onDateSelected(date)
        }
        bind.dateScrollerRecyclerView.apply {
            adapter = dateAdapter
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            itemAnimator = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // checkWeightLog()
    }

    override fun onResume() {
        super.onResume()
        requestNotificationPermission()
    }

    private fun currentDestinationId() =
        findNavController().currentDestination?.id

    private fun isHome() = currentDestinationId() == R.id.home

    private val user: User get() = LocalUser.user

    private fun checkWeightLog() {
        lifecycleScope.launch {
            if (isHome() && user.valid) {
                if (WeightLogRepository.timeToWeightLog()) {
                    navigate(R.id.addWeightPhotoFragment)
                }
            }
        }
    }

    private fun setupToolbar(bind: FragmentHomeBinding) {
        bind.streakDisplayLayout.setOnClickListener {
            viewModel.uiState.value.streakInfo?.let { streak ->
                if (isAdded) {
                    StreakInfoSheet
                        .newInstance(streak)
                        .show(
                            childFragmentManager,
                            StreakInfoSheet.TAG
                        )
                }
            }
        }
    }

    private fun setupClickListeners(bind: FragmentHomeBinding) {
        bind.buttonAddWater.setOnClickListener {
            navigate(R.id.logWaterDialog)
        }
        bind.buttonAddExerciseLog.setOnClickListener {
            val args = Bundle().apply {
                putLong(
                    "date",
                    viewModel.uiState.value.selectedDate.time
                )
            }
            navigate(R.id.logExerciseFragment, args)
        }

        bind.mainProgress.addFood.setOnClickListener {
            val args = Bundle().apply {
                putLong(
                    "date",
                    viewModel.uiState.value.selectedDate.time
                )
            }
            navigate(R.id.logFoodFragment, args)
        }
    }

    private fun setupDateScroller() {

    }

    private fun observeViewModel(bind: FragmentHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.map { it.datesForScroller }.distinctUntilChanged()
                        .collectLatest { dates ->
                            if (dates.isNotEmpty()) {
                                dateAdapter.submitList(dates)
                            }
                        }
                }

                launch {
                    viewModel.uiState.map { it.selectedDate }.distinctUntilChanged()
                        .collectLatest { selectedDate ->
                            dateAdapter.setSelectedDate(selectedDate)

                            val today = Calendar.getInstance().time
                            bind.toolbar.title =
                                if (DateUtils.isSameDay(selectedDate.time, today.time)) {
                                    "Bugun"
                                } else {
                                    SimpleDateFormat(
                                        "d MMMM",
                                        Locale("uz", "UZ")
                                    ).format(selectedDate)
                                }

                            bind.dateScrollerRecyclerView.post {
                                val selectedIndex =
                                    viewModel.uiState.value.datesForScroller.indexOfFirst {
                                        DateUtils.isSameDay(
                                            it.time, selectedDate.time
                                        )
                                    }
                                if (selectedIndex != -1) {
                                    (bind.dateScrollerRecyclerView.layoutManager as LinearLayoutManager)
                                        .scrollToPositionWithOffset(
                                        selectedIndex,
                                            bind.root.width / 2 - 32.dpToPx()
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    0
                )
            }
        }
    }

    private fun navigateToLogFood(mealType: String) {
        val bundle = Bundle().apply {
            putString("mealType", mealType)
            putLong("date", viewModel.uiState.value.selectedDate.time)
        }
        navigate(R.id.logFoodFragment, bundle)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}