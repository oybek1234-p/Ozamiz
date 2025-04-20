package uz.oybek.ozamiz.ui.getinfo // ViewModel bilan bir xil paket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentSetupRemindersBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class SetupRemindersFragment : BaseFragment<FragmentSetupRemindersBinding>(),
    ReminderAdapter.ReminderInteractionListener {

    private val viewModel: SetupRemindersViewModel by viewModels()
    private lateinit var reminderAdapter: ReminderAdapter
    private var pendingTimePickerItemId: String? = null

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentSetupRemindersBinding {
        return FragmentSetupRemindersBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentSetupRemindersBinding) {
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

    private fun setupToolbar(bind: FragmentSetupRemindersBinding) {
        bind.toolbar.title = "Eslatmalarni Sozlash"
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
                hour = bundle.getInt(EditExerciseReminderFragment.RESULT_ITEM_HOUR, -1),
                minute = bundle.getInt(EditExerciseReminderFragment.RESULT_ITEM_MINUTE, -1)
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
                    hour = null,
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
                    reminderAdapter.submitList(list)
                    reminderAdapter.notifyDataSetChanged()
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
        val previousDestinationId = navController.previousBackStackEntry?.destination?.id
        val currentDestinationId = navController.currentDestination?.id

        if (currentDestinationId == R.id.setupRemindersFragment) {
            if (previousDestinationId == R.id.getMainInfo) {
                navController.navigate(R.id.home)
            } else {
                closeFragment()
            }
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
        val canDelete = !isNew

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