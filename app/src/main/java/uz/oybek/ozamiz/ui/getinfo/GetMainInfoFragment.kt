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
                ) else ""
            )

            val activityAdapter = ArrayAdapter(
                requireContext(),
                R.layout.list_item,
                ActivityLevel.entries.map { "${it.title} (${it.subtitle})" }
            )
            harakatView.editText?.apply {
                (this as AutoCompleteTextView).apply {
                    setAdapter(activityAdapter)

                    val selected = ActivityLevel.entries.find { it.id == LocalUser.user.activityLevel }
                    selected?.let {
                        activityLevelSelected = it.id
                        setText(it.title, false)
                    }

                    onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                        val level = ActivityLevel.entries[position]
                        activityLevelSelected = level.id
                    }
                }
            }

            targetWeightView.editText?.setText(user.targetWeight?.let { String.format("%.1f", it) }
                ?: "")

            selectedGender = user.gender
            updateGenderSelectionUI(bind)
            calculateAndDisplayIdealWeightIfNeeded(bind)

            updateTargetWeightInputVisibility(bind)

            selectedProblems.clear()
            user.problems?.let { selectedProblems.addAll(it) }
            chipGroup.clearCheck()
            selectedProblems.forEach { problemName ->
                (chipGroup.findViewWithTag(problemName) as? Chip)?.isChecked = true
            }
            val manzilAdapter =
                ArrayAdapter(requireContext(), R.layout.list_item, City.asListNames(false))
            manzilView.editText?.apply {
                (this as AutoCompleteTextView).apply {
                    val selectedType = City.entries.find { it.name == LocalUser.user.manzil }
                    selectedType?.let {
                        manzilSelected = it.name
                        setText(getString(it.resId), false)
                    }
                    setAdapter(manzilAdapter)
                }

                onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
                    val cityCurrent = City.entries.filter { it.name != City.Hammasi.name }[position]
                    manzilSelected = cityCurrent.name
                }
            }
        }
    }

    private fun setSelectedRadioButton(radioGroup: RadioGroup, valueId: String?) {
        for (i in 0 until radioGroup.childCount) {
            val radioButton = radioGroup.getChildAt(i) as? RadioButton
            if (radioButton?.tag == valueId) {
                radioButton?.isChecked = true
                return
            }
        }
    }

    private fun getSelectedRadioButtonTag(radioGroup: RadioGroup): String? {
        val checkedId = radioGroup.checkedRadioButtonId
        return if (checkedId != -1) {
            radioGroup.findViewById<RadioButton>(checkedId)?.tag as? String
        } else {
            null
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
                    selectedProblems.add(chip.tag.toString()) // Store tag as problem identifier
                }
            }
            Problems.entries.forEach { problem ->
                val chip = Chip(
                    requireContext(),
                    null,
                    com.google.android.material.R.style.Widget_Material3_Chip_Filter
                ).apply {
                    text = problem.text
                    isCheckable = true
                    isClickable = true
                    tag = problem.name
                }
                chipGroup.addView(chip)
            }
            save.setOnClickListener {
                viewModel.saveUserInfo(
                    name = nameView.editText?.text?.toString(),
                    gender = selectedGender,
                    ageStr = ageView.editText?.text?.toString(),
                    heightStr = heightLay.editText?.text?.toString(),
                    weightStr = weightInput.editText?.text?.toString(),
                    targetWeightStr = targetWeightView.editText?.text?.toString(),
                    problems = selectedProblems.ifEmpty { null },
                    manzilSelected,
                    phoneNumber = binding.mobilRaqamView.editText?.text?.toString(),
                    activityLevelSelected
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
        val male = selectedGender == Gender.MALE
        val female = selectedGender == Gender.FEMALE
        bind.male.strokeWidth = if (male) 3.dpToPx() else 1
        bind.female.strokeWidth = if (female) 3.dpToPx() else 1
    }

    private fun updateTargetWeightInputVisibility(bind: GetInfoMainFragmentBinding) {
        bind.targetWeightView.isVisible = true
    }

    private fun validateTargetWeight(bind: GetInfoMainFragmentBinding) {
        val currentWeightStr = bind.weightInput.editText?.text?.toString()
        val targetWeightStr = bind.targetWeightView.editText?.text?.toString()
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
            // Boshqa inputlarni ham disable qilish mumkin
        }

        viewModel.idealWeightRangeText.observe(viewLifecycleOwner) { rangeText ->
            if (rangeText != null) {
                bind.textViewIdealWeightInfo.isVisible = true
                bind.textViewIdealWeightRange.text = rangeText
                bind.textViewIdealWeightRange.isVisible = true
            } else {
                bind.textViewIdealWeightInfo.isVisible = false
                bind.textViewIdealWeightRange.isVisible = false
            }
            validateTargetWeight(bind) // Ideal vazn hisoblangandan keyin maqsadni qayta tekshirish
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { content ->
                when (content) {
                    is GetInfoNavigationEvent.ShowToast -> showToast(content.message)
                    is GetInfoNavigationEvent.NavigateToNextScreen -> {
                        if (isAdded && findNavController().currentDestination?.id == R.id.getMainInfo) {
                            val action =
                                GetMainInfoFragmentDirections.actionGetMainInfoToSetupRemindersFragment()
                            findNavController().navigate(action)
                        }
                    }

                    is GetInfoNavigationEvent.NavigateToHome -> {
                        if (isAdded && findNavController().currentDestination?.id == R.id.getMainInfo) {
                            mainActivity()?.recreateUi(isNewUser) // isNewUser holatini hisobga olish
                        }
                    }
                }
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}