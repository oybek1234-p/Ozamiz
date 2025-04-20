package uz.oybek.ozamiz.ui.getinfo // Yoki mos paket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentEditExerciseReminderBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class EditExerciseReminderFragment : BaseFragment<FragmentEditExerciseReminderBinding>() {

    companion object {
        const val RESULT_KEY_EDIT_EXERCISE = "EditExerciseResultKey"
        const val RESULT_ACTION = "ResultAction"
        const val RESULT_ITEM_ID = "ResultItemId"
        const val RESULT_ITEM_NAME = "ResultItemName"
        const val RESULT_ITEM_HOUR = "ResultItemHour"
        const val RESULT_ITEM_MINUTE = "ResultItemMinute"
        const val RESULT_ITEM_ICON_RES = "ResultItemIconRes"
        const val RESULT_ITEM_DESCRIPTION = "ResultItemDescription"
        const val RESULT_ITEM_IMAGE_URL = "ResultItemImageUrl"

        const val ARG_ITEM_ID = "ArgItemId"
        const val ARG_ITEM_NAME = "ArgItemName"
        const val ARG_ITEM_ICON_RES = "ArgItemIconRes"
        const val ARG_ITEM_HOUR = "ArgItemHour"
        const val ARG_ITEM_MINUTE = "ArgItemMinute"
        const val ARG_ITEM_DESCRIPTION = "ArgItemDescription"
        const val ARG_ITEM_IMAGE_URL = "ArgItemImageUrl"
        const val ARG_IS_NEW = "ArgIsNew"
        const val ARG_CAN_DELETE = "ArgCanDelete"
    }

    private var currentReminderItem: ReminderItem? = null
    private var isNewExercise: Boolean = true
    private var canDelete: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseArguments()
        if (currentReminderItem == null && !isNewExercise) {

            if (isAdded) {
                findNavController().popBackStack()
            }
        }
    }

    private fun parseArguments() {
        arguments?.let { args ->
            isNewExercise = args.getBoolean(ARG_IS_NEW, true)
            canDelete = args.getBoolean(ARG_CAN_DELETE, false)
            val itemId = args.getString(ARG_ITEM_ID)
            val itemName = args.getString(ARG_ITEM_NAME)
            val itemIconRes = args.getInt(ARG_ITEM_ICON_RES)
            val itemHour = args.getInt(ARG_ITEM_HOUR, -1)
            val itemMinute = args.getInt(ARG_ITEM_MINUTE, -1)
            val itemDesc = args.getString(ARG_ITEM_DESCRIPTION)
            val itemImgUrl = args.getString(ARG_ITEM_IMAGE_URL)

            if (itemId != null && itemName != null) {
                currentReminderItem = ReminderItem(
                    id = itemId,
                    name = itemName,
                    iconRes = itemIconRes,
                    hour = if (itemHour == -1) null else itemHour,
                    minute = if (itemMinute == -1) null else itemMinute,
                    type = ReminderType.EXERCISE,
                    description = itemDesc,
                    imageUrl = itemImgUrl
                )
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentEditExerciseReminderBinding {
        return FragmentEditExerciseReminderBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentEditExerciseReminderBinding) {
        setupToolbar(bind)

        currentReminderItem?.let {
            loadData(bind, it)
            setupListeners(bind, it)
            setupTimePickerResultListener()
        } ?: run {
            if (isAdded) findNavController().popBackStack()
        }
    }

    private fun setupToolbar(bind: FragmentEditExerciseReminderBinding) {
        bind.toolbar.title =
            if (isNewExercise) "Yangi Mashq Qo'shish" else currentReminderItem?.name
                ?: "Mashqni Tahrirlash"
        bind.toolbar.setUpBackButton(this)
    }

    private fun loadData(bind: FragmentEditExerciseReminderBinding, item: ReminderItem) {
        Glide.with(this).load(item.iconRes)
            .into(bind.ivExerciseImage)
        bind.tvExerciseName.text = item.name
        bind.tvExerciseDescription.text = item.description ?: "Tavsif mavjud emas"
        bind.tvSelectedTime.text = item.getTimeAsString()
        bind.btnDeleteExercise.isVisible = !isNewExercise && canDelete
    }

    private fun setupListeners(bind: FragmentEditExerciseReminderBinding, item: ReminderItem) {
        bind.cardSetTime.setOnClickListener { showCustomTimePicker(item) }
        bind.tvSelectedTime.setOnClickListener { showCustomTimePicker(item) }
        bind.btnSaveExercise.setOnClickListener { saveChanges(item) }
        bind.btnDeleteExercise.setOnClickListener { showDeleteConfirmation(item) }
    }

    private fun showCustomTimePicker(item: ReminderItem) {
        val timePicker = CustomTimePickerDialogFragment.newInstance(
            item.hour, item.minute, "${item.name} vaqtini belgilang"
        )
        if (isAdded) {
            timePicker.show(parentFragmentManager, CustomTimePickerDialogFragment.TAG)
        }
    }

    private fun setupTimePickerResultListener() {
        if (!isAdded) return
        parentFragmentManager.setFragmentResultListener(
            CustomTimePickerDialogFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val hour = bundle.getInt(CustomTimePickerDialogFragment.RESULT_HOUR)
            val minute = bundle.getInt(CustomTimePickerDialogFragment.RESULT_MINUTE)
            currentReminderItem?.hour = hour
            currentReminderItem?.minute = minute
            binding.tvSelectedTime.text = currentReminderItem?.getTimeAsString()
        }
    }

    private fun saveChanges(item: ReminderItem) {
        if (!item.isValidTimeSet()) {
            showToast("Iltimos, eslatma vaqtini belgilang.")
            return
        }
        setFragmentResult(
            RESULT_KEY_EDIT_EXERCISE, bundleOf(
                RESULT_ACTION to "save",
                RESULT_ITEM_ID to item.id,
                RESULT_ITEM_NAME to item.name,
                RESULT_ITEM_HOUR to (item.hour ?: -1),
                RESULT_ITEM_MINUTE to (item.minute ?: -1),
                RESULT_ITEM_ICON_RES to item.iconRes,
                RESULT_ITEM_DESCRIPTION to item.description,
                RESULT_ITEM_IMAGE_URL to item.imageUrl
            )
        )
        if (isAdded) {
            findNavController().popBackStack()
        }
    }

    private fun showDeleteConfirmation(item: ReminderItem) {
        context?.let { ctx ->
            AlertDialog.Builder(ctx).setTitle("${item.name}ni o'chirish")
                .setMessage("Haqiqatan ham bu mashq eslatmasini o'chirmoqchimisiz?")
                .setPositiveButton("Ha, o'chirish") { dialog, _ ->
                    setFragmentResult(
                        RESULT_KEY_EDIT_EXERCISE, bundleOf(
                            RESULT_ACTION to "delete", RESULT_ITEM_ID to item.id
                        )
                    )
                    if (isAdded) findNavController().popBackStack()
                    dialog.dismiss()
                }.setNegativeButton(R.string.cancel, null).show()
        }
    }
}