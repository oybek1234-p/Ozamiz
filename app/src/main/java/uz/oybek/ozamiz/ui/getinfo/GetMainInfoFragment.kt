package uz.oybek.ozamiz.ui.getinfo // Yoki mos paketga joylang

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.info.Gender
import uz.oybek.ozamiz.data.info.Problems
import uz.oybek.ozamiz.databinding.GetInfoMainFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class GetMainInfoFragment : BaseFragment<GetInfoMainFragmentBinding>() {

    private val viewModel: GetInfoViewModel by viewModels()
    private var selectedGender: Int = -1
    private val selectedProblems = mutableListOf<String>()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): GetInfoMainFragmentBinding {
        return GetInfoMainFragmentBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: GetInfoMainFragmentBinding) {
        setupToolbar()
        loadInitialData()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Shaxsiy Ma'lumotlar"
        }
    }

    private fun loadInitialData() {
        val user = LocalUser.user
        binding.apply {
            nameView.editText?.setText(user.name)
            ageView.editText?.setText(if (user.age > 0) user.age.toString() else "")
            heightLay.editText?.setText(if (user.height > 0) user.height.toString() else "")
            weightInput.editText?.setText(if (user.weight > 0) user.weight.toString() else "")

            selectedGender = user.gender
            updateGenderSelection()

            selectedProblems.clear()
            user.problems?.let { selectedProblems.addAll(it) }
            chipGroup.clearCheck() // Avval tozalash
            selectedProblems.forEach { problemName ->
                when (problemName) {
                    Problems.QANDLI_DIABET.name -> diabet.isChecked = true
                    Problems.EMIZIKLI.name -> emizikli.isChecked = true
                    Problems.HOMILADOR.name -> homilador.isChecked = true
                }
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Jins tanlash
            male.setOnClickListener {
                selectedGender = Gender.MALE
                updateGenderSelection()
            }
            female.setOnClickListener {
                selectedGender = Gender.FEMALE
                updateGenderSelection()
            }

            // Muammolar tanlash (ChipGroup)
            chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
                selectedProblems.clear()
                checkedIds.forEach { chipId ->
                    val chip = group.findViewById<Chip>(chipId)
                    when (chip?.id) {
                        R.id.diabet -> selectedProblems.add(Problems.QANDLI_DIABET.name)
                        R.id.emizikli -> selectedProblems.add(Problems.EMIZIKLI.name)
                        R.id.homilador -> selectedProblems.add(Problems.HOMILADOR.name)
                    }
                }
            }

            // Saqlash tugmasi
            save.setOnClickListener {
                // Ma'lumotlarni ViewModel'ga yuborish
                viewModel.saveUserInfo(name = nameView.editText?.text?.toString(),
                    gender = selectedGender,
                    ageStr = ageView.editText?.text?.toString(),
                    heightStr = heightLay.editText?.text?.toString(),
                    weightStr = weightInput.editText?.text?.toString(),
                    problems = selectedProblems.ifEmpty { null } // Bo'sh bo'lsa null yuborish
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.save.isEnabled = !isLoading

            binding.nameView.isEnabled = !isLoading
            binding.ageView.isEnabled = !isLoading
            binding.heightLay.isEnabled = !isLoading
            binding.weightInput.isEnabled = !isLoading
            binding.male.isEnabled = !isLoading
            binding.female.isEnabled = !isLoading
            binding.chipGroup.isEnabled =
                !isLoading
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { content ->
                when (content) {
                    is GetInfoNavigationEvent.ShowToast -> showToast(content.message)
                    is GetInfoNavigationEvent.NavigateToNextScreen -> {
                        navigate(R.id.setupRemindersFragment)
                    }
                }
            }
        }
    }

    private fun updateGenderSelection() {
        binding.apply {
            val activeStrokeWidth = 4
            val inactiveStrokeWidth = 0

            male.strokeWidth =
                if (selectedGender == Gender.MALE) activeStrokeWidth else inactiveStrokeWidth
            female.strokeWidth =
                if (selectedGender == Gender.FEMALE) activeStrokeWidth else inactiveStrokeWidth
        }
    }
}