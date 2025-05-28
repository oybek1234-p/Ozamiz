package uz.oybek.ozamiz.ui.getinfo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentSetupRemindersBinding
import uz.oybek.ozamiz.reminders.ReminderScheduler
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class SetupRemindersFragment : BaseFragment<FragmentSetupRemindersBinding>(),
    ReminderAdapter.ReminderInteractionListener {

    private val viewModel: SetupRemindersViewModel by viewModels()
    private lateinit var reminderAdapter: ReminderAdapter
    private var pendingTimePickerItemId: String? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                ReminderScheduler.scheduleAllActiveReminders(requireContext().applicationContext)
            } else {
                showToast("Eslatmalar uchun ruxsat berilmadi.")
            }
        }


    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentSetupRemindersBinding {
        return FragmentSetupRemindersBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentSetupRemindersBinding) {
        checkAndRequestNotificationPermission()
        setupToolbar(bind)
        setupRecyclerView(bind)
        setupFragmentResultListeners()
        observeViewModel()

        bind.fabAddExercise.setOnClickListener {
            showSelectExerciseTypeSheet()
        }

        bind.btnContinue.setOnClickListener {
            viewModel.saveSettings()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {}
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Foydalanuvchiga tushuntirish kerak
                    // showToast("Eslatmalar uchun ruxsat kerak.") // Yoki dialog
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }


    private fun setupToolbar(bind: FragmentSetupRemindersBinding) {
        bind.toolbar.title = "Eslatmalarni Sozlash"
        bind.toolbar.setNavigationIcon(R.drawable.arrow_back) // Back button
        bind.toolbar.setNavigationOnClickListener {
            if (isAdded) findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(bind: FragmentSetupRemindersBinding) {
        reminderAdapter = ReminderAdapter(this)
        bind.recyclerViewReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reminderAdapter
            itemAnimator = null
        }
    }

    private fun setupFragmentResultListeners() {
        if (!isAdded) return

        parentFragmentManager.setFragmentResultListener(
            CustomTimePickerDialogFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val hour = bundle.getInt(CustomTimePickerDialogFragment.RESULT_HOUR)
            val minute = bundle.getInt(CustomTimePickerDialogFragment.RESULT_MINUTE)
            pendingTimePickerItemId?.let { itemId ->
                viewModel.userSetReminderTime(itemId, hour, minute)
                pendingTimePickerItemId = null
            }
        }

        parentFragmentManager.setFragmentResultListener(
            EditExerciseReminderFragment.RESULT_KEY_EDIT_EXERCISE, viewLifecycleOwner
        ) { _, bundle ->
            viewModel.processExerciseEditResult(
                itemId = bundle.getString(EditExerciseReminderFragment.RESULT_ITEM_ID),
                action = bundle.getString(EditExerciseReminderFragment.RESULT_ACTION),
                name = bundle.getString(EditExerciseReminderFragment.RESULT_ITEM_NAME),
                iconRes = bundle.getInt(EditExerciseReminderFragment.RESULT_ITEM_ICON_RES),
                hour = bundle.getInt(EditExerciseReminderFragment.RESULT_ITEM_HOUR, -1),
                minute = bundle.getInt(EditExerciseReminderFragment.RESULT_ITEM_MINUTE, -1),
                description = bundle.getString(EditExerciseReminderFragment.RESULT_ITEM_DESCRIPTION),
                imageUrl = bundle.getString(EditExerciseReminderFragment.RESULT_ITEM_IMAGE_URL)
            )
        }

        parentFragmentManager.setFragmentResultListener(
            SelectExerciseTypeBottomSheet.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val selectedTypeId =
                bundle.getString(SelectExerciseTypeBottomSheet.RESULT_EXERCISE_TYPE)

            if (selectedTypeId != null) {
                val defaultExerciseInfo = ExerciseType.getExerciseTypeById(selectedTypeId)
                val newItem = ReminderItem(
                    id = selectedTypeId,
                    name = defaultExerciseInfo.name,
                    iconRes = defaultExerciseInfo.iconRes,
                    type = ReminderType.EXERCISE,
                    description = defaultExerciseInfo.description,
                    imageUrl = defaultExerciseInfo.defaultImageUrl,
                    hour = null, // Vaqt keyin belgilanadi
                    minute = null
                )
                navigateToEditExercise(newItem, true)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.reminderListLiveData.observe(viewLifecycleOwner) { list ->
            if (this::reminderAdapter.isInitialized) {
                binding.recyclerViewReminders.post {
                    reminderAdapter.submitList(list.toMutableList()) // Submit a new list instance
                }
            }
        }
        viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            binding.let {
                it.progressBar.isVisible = isLoading
                it.btnContinue.isEnabled = !isLoading
                it.fabAddExercise.isEnabled = !isLoading
            }
        }
        viewModel.navigationEventLiveData.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { content ->
                when (content) {
                    is SetupReminderNavigationEvent.ShowToast -> showToast(content.message)
                    is SetupReminderNavigationEvent.NavigateToHome -> navigateToHome()
                }
            }
        }
    }

    private fun navigateToHome() {
        if (!isAdded) return
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.setupRemindersFragment) {
            // Check if came from GetMainInfo or Profile
            val cameFromGetInfo = navController.previousBackStackEntry?.destination?.id == R.id.getMainInfo
            mainActivity()?.recreateUi(cameFromGetInfo) // Recreate UI, pass if it's new user flow
        }
    }


    private fun showSelectExerciseTypeSheet() {
        if (!isAdded) return
        val bottomSheet = SelectExerciseTypeBottomSheet()
        val addedExerciseIds =
            viewModel.reminderListLiveData.value?.filter { it.type == ReminderType.EXERCISE }
                ?.map { it.id } ?: emptyList()
        val bundle = bundleOf(
            SelectExerciseTypeBottomSheet.ARG_ADDED_EXERCISE_IDS to ArrayList(addedExerciseIds)
        )
        bottomSheet.arguments = bundle

        if (parentFragmentManager.findFragmentByTag(SelectExerciseTypeBottomSheet.TAG) == null) {
            bottomSheet.show(parentFragmentManager, SelectExerciseTypeBottomSheet.TAG)
        }
    }

    private fun navigateToEditExercise(item: ReminderItem, isNew: Boolean) {
        if (!isAdded) return
        val canDelete = !isNew && (viewModel.reminderListLiveData.value?.count { it.type == ReminderType.EXERCISE && it.id != item.id } ?: 0) > 0


        val bundle = bundleOf(
            EditExerciseReminderFragment.ARG_ITEM_ID to item.id,
            EditExerciseReminderFragment.ARG_ITEM_NAME to item.name,
            EditExerciseReminderFragment.ARG_ITEM_ICON_RES to item.iconRes,
            EditExerciseReminderFragment.ARG_ITEM_HOUR to (item.hour ?: -1),
            EditExerciseReminderFragment.ARG_ITEM_MINUTE to (item.minute ?: -1),
            EditExerciseReminderFragment.ARG_ITEM_DESCRIPTION to item.description,
            EditExerciseReminderFragment.ARG_ITEM_IMAGE_URL to item.imageUrl,
            EditExerciseReminderFragment.ARG_IS_NEW to isNew,
            EditExerciseReminderFragment.ARG_CAN_DELETE to canDelete
        )
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.setupRemindersFragment) {
            navController.navigate(R.id.editExerciseReminderFragment, bundle)
        }
    }

    override fun onMealTimeEditClick(item: ReminderItem, position: Int) {
        if (!isAdded) return
        pendingTimePickerItemId = item.id
        val timePicker = CustomTimePickerDialogFragment.newInstance(
            item.hour, item.minute, "${item.name} vaqtini belgilang"
        )
        if (parentFragmentManager.findFragmentByTag(CustomTimePickerDialogFragment.TAG) == null) {
            timePicker.show(parentFragmentManager, CustomTimePickerDialogFragment.TAG)
        }
    }

}