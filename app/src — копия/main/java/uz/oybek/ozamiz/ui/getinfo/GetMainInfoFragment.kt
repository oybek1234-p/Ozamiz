package uz.oybek.ozamiz.ui.getinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ozcanalasalvar.datepicker.view.popup.TimePickerPopup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.info.Gender
import uz.oybek.ozamiz.data.info.Problems
import uz.oybek.ozamiz.databinding.GetInfoMainFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class GetMainInfoFragment : BaseFragment<GetInfoMainFragmentBinding>() {

    private fun update() {
        binding?.apply {
            if (LocalUser.user.gender == Gender.MALE) {
                male.strokeWidth = 2
                female.strokeWidth = 0
            } else if (LocalUser.user.gender == Gender.FEMALE) {
                male.strokeWidth = 0
                female.strokeWidth = 2
            }
        }
    }

    private fun nameInput() = binding?.nameView?.editText?.text?.toString()
    private fun nameValid() = (nameInput()?.length ?: 0) > 2

    var gender = LocalUser.user.gender

    override fun viewCreated(bind: GetInfoMainFragmentBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@GetMainInfoFragment)

            update()
            male.setOnClickListener {
                gender = Gender.MALE
            }
            female.setOnClickListener {
                gender = Gender.FEMALE
            }
            LocalUser.user.problems?.forEach {
                when (it) {
                    Problems.QANDLI_DIABET.name -> {
                        diabet.isChecked = true
                    }

                    Problems.EMIZIKLI.name -> {
                        emizikli.isChecked = true
                    }

                    Problems.HOMILADOR.name -> {
                        homilador.isChecked = true
                    }

                }
            }
            chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
                val list = arrayListOf<String>()
                checkedIds.forEach {
                    when(it) {
                        R.id.diabet -> {
                            list.add(Problems.QANDLI_DIABET.name)
                        }
                        R.id.emizikli -> {
                            list.add(Problems.EMIZIKLI.name)
                        }
                    }
                }
                problems = list
            }
        }
    }

    private var problems = listOf<String>()

    fun save() {
        binding?.apply {
            val h = heightLay.editText?.text.toString().trim()
            if (h.length < 3) {
                showToast("Bo'yingizni kiriting")
                return
            }
            val w = weightInput.editText?.text.toString().trim()
            if (w.length < 2) {
                showToast("Vazningizni kiriting")
                return
            }
            if (nameValid().not()) {
                showToast("Ismingizni kiriting!")
                return
            }
            val age = ageView.editText?.text.toString().trim()
            if (age.length < 2) {
                showToast("Yoshingizni kiriting")
                return
            }
            if (gender != Gender.MALE && gender != Gender.FEMALE) {
                showToast("Jinsingizni tanlang")
                return
            }
            UserRepository.updateUser {
                LocalUser.user.gender = gender
                LocalUser.user.age = age.toInt()
                LocalUser.user.height = h.toInt()
                LocalUser.user.weight = w.toInt()
                LocalUser.user.problems = problems.toSet()
                LocalUser.user.name = nameInput() ?: ""
            }

        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): GetInfoMainFragmentBinding {
        return GetInfoMainFragmentBinding.inflate(inflater)
    }
}