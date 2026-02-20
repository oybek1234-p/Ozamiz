package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.databinding.FragmentPhotosonboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class PhotosOnboardFragment
    : OnBoardingChildFragment<FragmentPhotosonboardBinding>() {

    override fun viewCreated(bind: FragmentPhotosonboardBinding) {
        bind.apply {
            if (LocalUser.user.gender == Gender.MALE) {
                img1.setImageResource(R.drawable.man_result_photo_1)
                img3.setImageResource(R.drawable.man_result_photo_3)
            } else {
                img1.setImageResource(R.drawable.woman_result_photo_1)
                img3.setImageResource(R.drawable.woman_result_photo_3)
            }
            button.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPhotosonboardBinding {
        return FragmentPhotosonboardBinding.inflate(
            inflater,
            container,
            false
        )
    }
}