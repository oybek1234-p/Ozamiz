package uz.oybek.ozamiz.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentSplashBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    override fun viewCreated(bind: FragmentSplashBinding) {
        bind.apply {
            showBottomSheet = false
            Glide.with(requireContext()).load(R.drawable.splash_photo).into(photo)
            start.setOnClickListener {
                navigate(R.id.authFragment)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSplashBinding {
        return FragmentSplashBinding.inflate(inflater)
    }
}