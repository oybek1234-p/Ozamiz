package uz.oybek.ozamiz.ui.getinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.info.ActivityLevel
import uz.oybek.ozamiz.data.info.Gender
import uz.oybek.ozamiz.data.info.Problems
import uz.oybek.ozamiz.data.location.City
import uz.oybek.ozamiz.databinding.GetInfoMainFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast
import java.util.Date

class GetMainInfoFragment : BaseFragment<GetInfoMainFragmentBinding>() {

    private val viewModel: GetInfoViewModel by viewModels()
    private var selectedGender: Int = -1
    private val selectedProblems = mutableListOf<String>()
    private var isNewUser: Boolean = true

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
            title = if (isNewUser) "Profil ma'lumotlari" else "Profilni tahrirlash"
            if (!isNewUser) {
                setNavigationIcon(R.drawable.arrow_back)
                setNavigationOnClickListener { findNavController().popBackStack() }
            } else {
                navigationIcon = null
            }
        }
    }

    var manzilSelected = ""
    private var activityLevelSelected: String = ""

    private fun loadInitialData(bind: GetInfoMainFragmentBinding) {
        val user = LocalUser.user
        bind.apply {
            nameView.editText?.setText(user.name)
            ageView.editText?.setText(if (user.age > 0) user.age.toString() else "")
            heightLay.editText?.setText(if (user.height > 0) user.height.toString() else "")
            weightInput.editText?.setText(
                if (user.weight > 0) String.format(
                    "%.1f", user.weight
                ).replace(',', '.') // Vergulni nuqtaga almashtirish
                else ""
            )

            val activityAdapter = ArrayAdapter(
                requireContext(),
                R.layout.list_item,
                ActivityLevel.entries.map { "${it.title} (${it.subtitle})" }
            )
            (harakatView.editText as? AutoCompleteTextView)?.apply {
                val selected = ActivityLevel.entries.find { it.id == LocalUser.user.activityLevel }
                selected?.let {
                    activityLevelSelected = it.id
                    setText("${it.title} (${it.subtitle})", false)
                }
                setAdapter(activityAdapter)
            }

            targetWeightView.editText?.setText(user.targetWeight?.let { String.format("%.1f", it).replace(',', '.') }
                ?: "")

            mobilRaqamView.editText?.setText(LocalUser.user.phoneNumber)
            selectedGender = user.gender
            updateGenderSelectionUI(bind)
            calculateAndDisplayIdealWeightIfNeeded(bind)

            updateTargetWeightInputVisibility(bind)

            selectedProblems.clear()
            user.problems?.let { selectedProblems.addAll(it) }

            // Clear old chips before adding new ones
            chipGroup.removeAllViews()
            Problems.entries.forEach { problem ->
                val chip = Chip(
                    requireContext(),
                    null,
                    com.google.android.material.R.style.Widget_Material3_Chip_Filter
                ).apply {
                    text = problem.text
                    isCheckable = true
                    isClickable = true
                    tag = problem.name // Store enum name as tag
                    isChecked = selectedProblems.contains(problem.name)
                }
                chipGroup.addView(chip)
            }


            val manzilAdapter =
                ArrayAdapter(requireContext(), R.layout.list_item, City.asListNames(false))
            (manzilView.editText as? AutoCompleteTextView)?.apply {
                val currentManzilName = LocalUser.user.manzil
                val selectedCity = City.entries.find { city -> getString(city.resId) == currentManzilName || city.name == currentManzilName }

                selectedCity?.let {
                    manzilSelected = getString(it.resId) // Save localized name
                    setText(getString(it.resId), false)
                }
                setAdapter(manzilAdapter)
            }
        }
    }


    private fun setupListeners(bind: GetInfoMainFragmentBinding) {
        bind.apply {
            male.setOnClickListener {
                selectedGender = Gender.MALE
                updateGenderSelectionUI(bind)
                calculateAndDisplayIdealWeightIfNeeded(bind)
            }
            female.setOnClickListener {
                selectedGender = Gender.FEMALE
                updateGenderSelectionUI(bind)
                calculateAndDisplayIdealWeightIfNeeded(bind)
            }

            heightLay.editText?.doAfterTextChanged {
                calculateAndDisplayIdealWeightIfNeeded(bind)
            }
            weightInput.editText?.doAfterTextChanged {
                validateTargetWeight(bind)
            }
            targetWeightView.editText?.doAfterTextChanged {
                validateTargetWeight(bind)
            }


            chipGroup.setOnCheckedStateChangeListener { group, _ ->
                selectedProblems.clear()
                group.checkedChipIds.forEach { chipId ->
                    val chip = group.findViewById<Chip>(chipId)
                    chip.tag?.toString()?.let { selectedProblems.add(it) }
                }
            }

            (harakatView.editText as? AutoCompleteTextView)?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                activityLevelSelected = ActivityLevel.entries[position].id
            }

            (manzilView.editText as? AutoCompleteTextView)?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val cityEnumEntry = City.entries.filter { it != City.Hammasi }[position]
                manzilSelected = getString(cityEnumEntry.resId) // Save localized name
            }

            save.setOnClickListener {
                // If it's a new user and registerDate is not set, set it now.
                if (isNewUser && LocalUser.user.registerDate == null) {
                    LocalUser.user.registerDate = Date()
                }

                viewModel.saveUserInfoAndGeneratePlan( // Yangilangan funksiyani chaqirish
                    name = nameView.editText?.text?.toString(),
                    gender = selectedGender,
                    ageStr = ageView.editText?.text?.toString(),
                    heightStr = heightLay.editText?.text?.toString(),
                    weightStr = weightInput.editText?.text?.toString()?.replace(',', '.'),
                    targetWeightStr = targetWeightView.editText?.text?.toString()?.replace(',', '.'),
                    problems = selectedProblems.ifEmpty { null },
                    manzilSelected = manzilSelected,
                    phoneNumber = binding.mobilRaqamView.editText?.text?.toString(),
                    activityLevel = activityLevelSelected
                )
            }
        }
    }

    private fun calculateAndDisplayIdealWeightIfNeeded(bind: GetInfoMainFragmentBinding) {
        val heightStr = bind.heightLay.editText?.text?.toString()
        val height = heightStr?.toIntOrNull()
        viewModel.calculateAndSetIdealWeight(height, selectedGender)
    }

    private fun updateGenderSelectionUI(bind: GetInfoMainFragmentBinding) {
        val maleSelected = selectedGender == Gender.MALE
        val femaleSelected = selectedGender == Gender.FEMALE
        bind.male.strokeWidth = if (maleSelected) 3.dpToPx() else 1.dpToPx()
        bind.female.strokeWidth = if (femaleSelected) 3.dpToPx() else 1.dpToPx()
    }


    private fun updateTargetWeightInputVisibility(bind: GetInfoMainFragmentBinding) {
        bind.targetWeightView.isVisible = true
    }

    private fun validateTargetWeight(bind: GetInfoMainFragmentBinding) {
        val currentWeightStr = bind.weightInput.editText?.text?.toString()?.replace(',', '.')
        val targetWeightStr = bind.targetWeightView.editText?.text?.toString()?.replace(',', '.')
        if (currentWeightStr.isNullOrBlank() || targetWeightStr.isNullOrBlank()) {
            bind.targetWeightWarning.isVisible = false
            return
        }

        val currentWeight = currentWeightStr.toDoubleOrNull()
        val targetWeight = targetWeightStr.toDoubleOrNull()

        if (currentWeight == null || targetWeight == null) {
            bind.targetWeightWarning.isVisible = false
            return
        }

        var warningMessage: String? = null

        if (targetWeight >= currentWeight) {
            warningMessage = "Ozish uchun maqsad vazn joriy vazndan kichik bo'lishi kerak."
        } else if (currentWeight - targetWeight > 30) {
            warningMessage =
                "Bu maqsad juda keskin. Kichikroq qadamlar bilan boshlash tavsiya etiladi."
        }
        if (warningMessage != null) {
            bind.targetWeightWarning.text = warningMessage.trim()
            bind.targetWeightWarning.isVisible = true
        } else {
            bind.targetWeightWarning.isVisible = false
        }
    }

    private fun observeViewModel(bind: GetInfoMainFragmentBinding) {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            bind.progressBar.isVisible = isLoading
            bind.save.isEnabled = !isLoading
        }

        viewModel.idealWeightRangeText.observe(viewLifecycleOwner) { rangeText ->
            if (rangeText != null) {
                bind.textViewIdealWeightInfo.isVisible = true
                bind.idealWeightCon.isVisible = true
                bind.textViewIdealWeightRange.text = rangeText
                bind.textViewIdealWeightRange.isVisible = true
            } else {
                bind.idealWeightCon.isVisible = false
                bind.textViewIdealWeightInfo.isVisible = false
                bind.textViewIdealWeightRange.isVisible = false
            }
            validateTargetWeight(bind)
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { content ->
                when (content) {
                    is GetInfoNavigationEvent.ShowToast -> showToast(content.message)
                    is GetInfoNavigationEvent.NavigateToCongratulation -> { // O'zgartirilgan event
                        if (isAdded && findNavController().currentDestination?.id == R.id.getMainInfo) {
                            navigate(R.id.infoCongratulationFragment)
                        }
                    }
                    is GetInfoNavigationEvent.NavigateToHome -> {
                        if (isAdded && findNavController().currentDestination?.id == R.id.getMainInfo) {
                            mainActivity()?.recreateUi(isNewUser)
                        }
                    }
                }
            }
        }
    }
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}