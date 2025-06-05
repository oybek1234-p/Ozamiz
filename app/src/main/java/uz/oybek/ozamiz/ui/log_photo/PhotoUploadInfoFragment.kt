package uz.oybek.ozamiz.ui.log_photo

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.databinding.FragmentPhotoUploadInfoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class PhotoUploadInfoFragment : BaseFragment<FragmentPhotoUploadInfoBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPhotoUploadInfoBinding {
        return FragmentPhotoUploadInfoBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentPhotoUploadInfoBinding) {
        bind.toolbarPhotoInfo.setNavigationOnClickListener {
            closeFragment()
        }

        bind.buttonConfirmPhotoInfo.setOnClickListener {
          //  LocalUser.user = LocalUser.user.copy(hasSeenWeightPhotoInfo = true)
          //   LocalUser.saveUser()
            closeFragment()
            navigate(R.id.addWeightPhotoFragment)
        }
    }
}