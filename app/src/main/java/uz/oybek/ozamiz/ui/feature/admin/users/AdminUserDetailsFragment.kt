package uz.oybek.ozamiz.ui.feature.admin.users

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.databinding.FragmentAdminUserDetailsBinding
import uz.oybek.ozamiz.databinding.ItemAdminUserFieldBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUserDetailsFragment : BaseFragment<FragmentAdminUserDetailsBinding>() {

    private val viewModel: AdminUserDetailsViewModel by viewModels()
    private var argUserId: String? = null
    private val fieldBindings = mutableListOf<FieldHolder>()

    private data class FieldHolder(
        val fieldName: String,
        val binding: ItemAdminUserFieldBinding,
        val type: FieldType,
        val originalValue: Any?
    )

    private enum class FieldType {
        STRING, INT, DOUBLE, LONG, BOOLEAN, DATE_LONG, LIST_STRING
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        argUserId = arguments?.getString("userId")
        argUserId?.let { viewModel.loadUser(it) }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminUserDetailsBinding {
        return FragmentAdminUserDetailsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminUserDetailsBinding) {
        setupToolbar(bind)
        setupListeners(bind)
        observeViewModel(bind)
    }

    private fun setupToolbar(bind: FragmentAdminUserDetailsBinding) {
        bind.toolbar.setNavigationOnClickListener {
            navigateUp()
        }
    }

    private fun setupListeners(bind: FragmentAdminUserDetailsBinding) {
        bind.buttonWeightLogs.setOnClickListener {
            argUserId?.let { userId ->
                val bundle = androidx.core.os.bundleOf("userId" to userId)
                navigate(R.id.weightPhotoHistoryFragment, bundle)
            }
        }

        bind.buttonRecentFoodLogs.setOnClickListener {
            argUserId?.let { userId ->
                val bundle = androidx.core.os.bundleOf("userId" to userId)
                navigate(
                    R.id.adminFoodLogsFragment,
                    bundle
                )
            }
        }

        bind.buttonDeleteProfile.setOnClickListener {
            showDeleteFirstConfirmation()
        }

        bind.buttonSaveAll.setOnClickListener {
            saveAllChanges()
        }
    }

    private fun buildFieldRows(bind: FragmentAdminUserDetailsBinding, user: User) {
        fieldBindings.clear()
        bind.layoutFieldsContainer.removeAllViews()

        val streak = user.streakInfo ?: StreakInfo()
        val streakPrefix = "${User::streakInfo.name}."

        val fields = listOf(
            Triple(User::uid.name, user.uid, FieldType.STRING),
            Triple(User::name.name, user.name, FieldType.STRING),
            Triple(User::phoneNumber.name, user.phoneNumber, FieldType.STRING),
            Triple(User::gmail.name, user.gmail, FieldType.STRING),
            Triple(User::lastSeenTime.name, user.lastSeenTime, FieldType.DATE_LONG),
            Triple(User::unreadMessages.name, user.unreadMessages, FieldType.INT),
            Triple(User::premium.name, user.premium, FieldType.BOOLEAN),
            Triple(User::premiumDate.name, user.premiumDate, FieldType.DATE_LONG),
            Triple("isPremiumYearly", user.isPremiumYearly, FieldType.BOOLEAN),
            Triple("isMonthlyPremium", user.isMonthlyPremium, FieldType.BOOLEAN),
            Triple(User::gender.name, user.gender, FieldType.INT),
            Triple(User::region.name, user.region, FieldType.STRING),
            Triple(User::age.name, user.age, FieldType.INT),
            Triple(User::height.name, user.height, FieldType.INT),
            Triple(User::weight.name, user.weight, FieldType.DOUBLE),
            Triple(User::targetWeight.name, user.targetWeight, FieldType.DOUBLE),
            Triple(User::startWeight.name, user.startWeight, FieldType.DOUBLE),
            Triple(User::goal.name, user.goal, FieldType.STRING),
            Triple(User::activityLevel.name, user.activityLevel, FieldType.STRING),
            Triple(User::targetCalories.name, user.targetCalories, FieldType.INT),
            Triple(User::targetProtein.name, user.targetProtein, FieldType.INT),
            Triple(User::targetFat.name, user.targetFat, FieldType.INT),
            Triple(User::targetCarbs.name, user.targetCarbs, FieldType.INT),
            Triple(User::targetWaterMl.name, user.targetWaterMl, FieldType.INT),
            Triple(
                "${streakPrefix}${StreakInfo::currentStreak.name}",
                streak.currentStreak,
                FieldType.INT
            ),
            Triple(
                "${streakPrefix}${StreakInfo::bestStreak.name}",
                streak.bestStreak,
                FieldType.INT
            ),
            Triple(
                "${streakPrefix}${StreakInfo::activeDays.name}",
                streak.activeDays,
                FieldType.INT
            ),
            Triple(
                "${streakPrefix}${StreakInfo::streakGoal.name}",
                streak.streakGoal,
                FieldType.INT
            ),
            Triple(
                "${streakPrefix}${StreakInfo::lastActionDate.name}",
                streak.lastActionDate,
                FieldType.STRING
            ),
            Triple(
                User::lastWeightLogTimestamp.name,
                user.lastWeightLogTimestamp,
                FieldType.DATE_LONG
            ),
            Triple(User::avatarUrl.name, user.avatarUrl, FieldType.STRING),
            Triple(User::registerDate.name, user.registerDate, FieldType.DATE_LONG),
            Triple(User::paymentChecking.name, user.paymentChecking, FieldType.BOOLEAN),
            Triple(User::paymentCheckPhotoUrl.name, user.paymentCheckPhotoUrl, FieldType.STRING),
            Triple(User::admin.name, user.admin, FieldType.BOOLEAN),
            Triple(User::scorePoints.name, user.scorePoints, FieldType.INT),
            Triple(User::problems.name, user.problems, FieldType.LIST_STRING),
        )

        val inflater = LayoutInflater.from(requireContext())

        for ((fieldName, value, type) in fields) {
            val itemBinding =
                ItemAdminUserFieldBinding.inflate(inflater, bind.layoutFieldsContainer, false)
            itemBinding.textFieldName.text = fieldName

            when (type) {
                FieldType.BOOLEAN -> {
                    itemBinding.textFieldValue.isVisible = false
                    itemBinding.inputLayoutField.isVisible = false
                    itemBinding.checkboxFieldValue.isVisible = true
                    itemBinding.checkboxFieldValue.isChecked = value as? Boolean ?: false
                    itemBinding.checkboxFieldValue.text = getString(R.string.admin_field_enabled)
                }

                FieldType.DATE_LONG -> {
                    val longVal = extractDateLong(value)
                    val formatted = formatDateReadable(longVal)
                    itemBinding.textFieldValue.text = formatted
                    itemBinding.checkboxFieldValue.isVisible = false
                    itemBinding.inputLayoutField.isVisible = true
                    itemBinding.editTextField.inputType = InputType.TYPE_CLASS_NUMBER
                    itemBinding.editTextField.setText(if (longVal != 0L) longVal.toString() else "")
                }

                FieldType.LIST_STRING -> {
                    val listVal = extractListString(value)
                    itemBinding.textFieldValue.text = if (listVal.isNullOrEmpty()) {
                        getString(R.string.admin_field_not_set)
                    } else {
                        listVal.joinToString(", ")
                    }
                    itemBinding.checkboxFieldValue.isVisible = false
                    itemBinding.inputLayoutField.isVisible = false
                }

                FieldType.INT -> {
                    itemBinding.textFieldValue.text =
                        formatIntDisplay(fieldName, value as? Int ?: 0)
                    itemBinding.checkboxFieldValue.isVisible = false
                    itemBinding.inputLayoutField.isVisible = true
                    itemBinding.editTextField.inputType =
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                    itemBinding.editTextField.setText((value as? Int ?: 0).toString())
                }

                FieldType.DOUBLE -> {
                    val dVal = value as? Double
                    itemBinding.textFieldValue.text =
                        dVal?.toString() ?: getString(R.string.admin_field_not_set)
                    itemBinding.checkboxFieldValue.isVisible = false
                    itemBinding.inputLayoutField.isVisible = true
                    itemBinding.editTextField.inputType =
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    itemBinding.editTextField.setText(dVal?.toString() ?: "")
                }

                FieldType.STRING -> {
                    val sVal = value?.toString() ?: ""
                    itemBinding.textFieldValue.text =
                        sVal.ifEmpty { getString(R.string.admin_field_not_set) }
                    itemBinding.checkboxFieldValue.isVisible = false
                    itemBinding.inputLayoutField.isVisible = true
                    itemBinding.editTextField.inputType = InputType.TYPE_CLASS_TEXT
                    itemBinding.editTextField.setText(sVal)
                }

                FieldType.LONG -> {
                    val lVal = value as? Long ?: 0L
                    itemBinding.textFieldValue.text = lVal.toString()
                    itemBinding.checkboxFieldValue.isVisible = false
                    itemBinding.inputLayoutField.isVisible = true
                    itemBinding.editTextField.inputType = InputType.TYPE_CLASS_NUMBER
                    itemBinding.editTextField.setText(lVal.toString())
                }
            }

            bind.layoutFieldsContainer.addView(itemBinding.root)
            fieldBindings.add(FieldHolder(fieldName, itemBinding, type, value))
        }

        bind.buttonSaveAll.isVisible = true
    }

    private fun formatIntDisplay(fieldName: String, value: Int): String {
        if (fieldName == User::gender.name) {
            return when (value) {
                0 -> getString(R.string.admin_gender_male)
                1 -> getString(R.string.admin_gender_female)
                else -> getString(R.string.admin_gender_unknown)
            } + " ($value)"
        }
        return value.toString()
    }

    private fun extractDateLong(value: Any?): Long {
        if (value == null) return 0L
        return DateUtils.toLongTimestamp(value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractListString(value: Any?): List<String>? {
        return try {
            value as? List<String>
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDateReadable(timeMs: Long): String {
        if (timeMs == 0L) return getString(R.string.admin_field_not_set)
        val date = Date(timeMs)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun saveAllChanges() {
        val userId = argUserId ?: return
        val updates = mutableMapOf<String, Any?>()

        for (holder in fieldBindings) {
            when (holder.type) {
                FieldType.BOOLEAN -> {
                    val newVal = holder.binding.checkboxFieldValue.isChecked
                    val origVal = holder.originalValue as? Boolean ?: false
                    if (newVal != origVal) {
                        updates[holder.fieldName] = newVal
                    }
                }

                FieldType.INT -> {
                    val text = holder.binding.editTextField.text.toString()
                    val newVal = text.toIntOrNull()
                    val origVal = holder.originalValue as? Int
                    if (newVal != null && newVal != origVal) {
                        updates[holder.fieldName] = newVal
                    }
                }

                FieldType.DOUBLE -> {
                    val text = holder.binding.editTextField.text.toString()
                    val newVal = if (text.isEmpty()) null else text.toDoubleOrNull()
                    val origVal = holder.originalValue as? Double
                    if (newVal != origVal) {
                        updates[holder.fieldName] = newVal
                    }
                }

                FieldType.STRING -> {
                    val text = holder.binding.editTextField.text.toString()
                    val newVal = text.ifEmpty { null }
                    val origVal = (holder.originalValue?.toString())?.ifEmpty { null }
                    if (newVal != origVal) {
                        updates[holder.fieldName] = newVal
                    }
                }

                FieldType.LONG -> {
                    val text = holder.binding.editTextField.text.toString()
                    val newVal = text.toLongOrNull()
                    val origVal = holder.originalValue as? Long
                    if (newVal != null && newVal != origVal) {
                        updates[holder.fieldName] = newVal
                    }
                }

                FieldType.DATE_LONG -> {
                    val text = holder.binding.editTextField.text.toString()
                    val newVal = text.toLongOrNull()
                    val origVal = extractDateLong(holder.originalValue)
                    if (newVal != null && newVal != origVal) {
                        updates[holder.fieldName] = newVal
                    }
                }

                FieldType.LIST_STRING -> {
                }
            }
        }

        if (updates.isNotEmpty()) {
            viewModel.saveAllFields(userId, updates)
            Toast.makeText(context, R.string.admin_saved_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.admin_field_not_set, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteFirstConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.admin_delete_confirm_title_1)
            .setMessage(R.string.admin_delete_confirm_msg_1)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                showDeleteSecondConfirmation()
            }
            .setNegativeButton(R.string.delete_cancel, null)
            .show()
    }

    private fun showDeleteSecondConfirmation() {
        var secondsLeft = 5
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.admin_delete_confirm_title_2)
            .setMessage(R.string.admin_delete_confirm_msg_2)
            .setPositiveButton(getString(R.string.wait_seconds, secondsLeft), null)
            .setNegativeButton(R.string.delete_cancel, null)
            .create()

        dialog.setOnShowListener {
            val deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            deleteButton.isEnabled = false

            dialog.window?.decorView?.post(object : Runnable {
                override fun run() {
                    if (dialog.isShowing) {
                        if (secondsLeft > 0) {
                            deleteButton.text = getString(R.string.wait_seconds, secondsLeft)
                            secondsLeft--
                            dialog.window?.decorView?.postDelayed(this, 1000)
                        } else {
                            deleteButton.text = getString(R.string.delete_confirm)
                            deleteButton.isEnabled = true
                            deleteButton.setOnClickListener {
                                argUserId?.let { userId ->
                                    viewModel.deleteUser(
                                        userId,
                                        onSuccess = {
                                            dialog.dismiss()
                                            Toast.makeText(
                                                context,
                                                R.string.profile_deleted_success,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navigateUp()
                                        },
                                        onError = { exp ->
                                            dialog.dismiss()
                                            Toast.makeText(
                                                context,
                                                "Error: ${exp.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            })
        }
        dialog.show()
    }

    private fun observeViewModel(bind: FragmentAdminUserDetailsBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { user ->
                        if (user != null) {
                            bind.textViewName.text =
                                user.name.ifEmpty { getString(R.string.admin_field_unknown) }
                            bind.textViewGmail.text =
                                user.gmail.ifEmpty { getString(R.string.admin_field_not_set) }
                            bind.textViewPhone.text =
                                user.phoneNumber.ifEmpty { getString(R.string.admin_field_not_set) }
                            bind.textViewUid.text = getString(R.string.admin_user_uid, user.uid)

                            Glide.with(this@AdminUserDetailsFragment)
                                .load(user.avatarUrl)
                                .error(R.drawable.user_photo_placeholder)
                                .placeholder(R.drawable.user_photo_placeholder)
                                .apply(RequestOptions.circleCropTransform())
                                .into(bind.imageViewAvatar)

                            buildFieldRows(bind, user)
                        }
                    }
                }
            }
        }
    }
}
