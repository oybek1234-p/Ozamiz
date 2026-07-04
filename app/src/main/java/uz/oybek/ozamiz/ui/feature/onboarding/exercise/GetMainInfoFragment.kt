package uz.oybek.ozamiz.ui.feature.onboarding.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import uz.oybek.ozamiz.databinding.GetInfoMainFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.util.Date

class GetMainInfoFragment : BaseFragment<GetInfoMainFragmentBinding>() {

    private val viewModel: GetInfoViewModel by viewModels()
    private var selectedGender: Int = -1
    private val selectedProblems = mutableListOf<String>()
    private var isNewUser: Boolean = true
    private var activityLevelSelected: String = ""
    private var manzilSelected: String = "Uzbekistan"

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
            setNavigationOnClickListener { findNavController().popBackStack() }
        }
    }

    private fun loadInitialData(bind: GetInfoMainFragmentBinding) = with(bind) {
        val user = LocalUser.user

        nameView.editText?.setText(user.name)
        ageView.editText?.setText(if (user.age > 0) user.age.toString() else "")
        mobilRaqamView.editText?.setText(user.phoneNumber)

        heightLay.editText?.setText(if (user.height > 0) user.height.toString() else "")
        weightInput.editText?.setText(
            if (user.weight > 0) String.format("%.1f", user.weight).replace(',', '.') else ""
        )
        targetWeightView.editText?.setText(user.targetWeight?.let {
            String.format("%.1f", it).replace(',', '.')
        } ?: "")

        val activityAdapter = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            UserActivityLevel.entries.map { "${it.title} (${it.subtitle})" }
        )

        (harakatView.editText as? AutoCompleteTextView)?.apply {
            setAdapter(activityAdapter)
            UserActivityLevel.entries.find { it.name == user.activityLevel }?.let {
                activityLevelSelected = it.name
                setText("${it.title} (${it.subtitle})", false)
            }
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                activityLevelSelected = UserActivityLevel.entries[position].name
            }
        }

        selectedGender = user.gender
        updateGenderSelectionUI(this)
        calculateAndDisplayIdealWeightIfNeeded(this)

        selectedProblems.clear()
        user.problems?.let { selectedProblems.addAll(it) }
    }

    private fun setupListeners(bind: GetInfoMainFragmentBinding) = with(bind) {
        male.setOnClickListener {
            selectedGender = Gender.MALE
            updateGenderSelectionUI(this)
            calculateAndDisplayIdealWeightIfNeeded(this)
        }
        female.setOnClickListener {
            selectedGender = Gender.FEMALE
            updateGenderSelectionUI(this)
            calculateAndDisplayIdealWeightIfNeeded(this)
        }

        heightLay.editText?.doAfterTextChanged { calculateAndDisplayIdealWeightIfNeeded(this) }
        weightInput.editText?.doAfterTextChanged { validateTargetWeight(this) }
        targetWeightView.editText?.doAfterTextChanged { validateTargetWeight(this) }

        chipGroup.setOnCheckedStateChangeListener { group, _ ->
            selectedProblems.clear()
            group.checkedChipIds.forEach { chipId ->
                val chip = group.findViewById<Chip>(chipId)
                chip.tag?.toString()?.let { selectedProblems.add(it) }
            }
        }

        save.setOnClickListener {
            handleSaveClick(bind)
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
            showToast("Iltimos, yoshingizni to'g'ri kiriting")
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
            manzil = manzilSelected
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
        val maleSelected = selectedGender == Gender.MALE
        val femaleSelected = selectedGender == Gender.FEMALE
        bind.male.strokeWidth = if (maleSelected) 3.dpToPx() else 1.dpToPx()
        bind.female.strokeWidth = if (femaleSelected) 3.dpToPx() else 1.dpToPx()
    }

    private fun observeViewModel(bind: GetInfoMainFragmentBinding) {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            bind.progressBar.isVisible = isLoading
            bind.save.isEnabled = !isLoading
        }

        viewModel.idealWeightRangeText.observe(viewLifecycleOwner) { rangeText ->
            val hasValue = rangeText != null
            bind.idealWeightCon.isVisible = hasValue
            bind.textViewIdealWeightInfo.isVisible = hasValue
            bind.textViewIdealWeightRange.isVisible = hasValue
            if (hasValue) {
                bind.textViewIdealWeightRange.text = rangeText
            }
            validateTargetWeight(bind)
        }
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is GetInfoNavigationEvent.ShowToast -> showToast(event.message)
                is GetInfoNavigationEvent.NavigateToHome -> {
                    findNavController().popBackStack()
                }

                is GetInfoNavigationEvent.NavigateToCongratulation -> {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}