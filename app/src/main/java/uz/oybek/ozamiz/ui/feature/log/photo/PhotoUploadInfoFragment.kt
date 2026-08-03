package uz.oybek.ozamiz.ui.feature.log.photo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import uz.oybek.ozamiz.R
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
            setFragmentResult("photo_info_confirmed", bundleOf("confirmed" to true))
            closeFragment()
        }
    }
}