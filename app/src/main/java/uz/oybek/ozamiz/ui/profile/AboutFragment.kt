package uz.oybek.ozamiz.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.BuildConfig
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAboutBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class AboutFragment : BaseFragment<FragmentAboutBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAboutBinding {
        return FragmentAboutBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAboutBinding) {
        bind.toolbarAbout.setNavigationOnClickListener {
            if (findNavController().currentDestination?.id == R.id.aboutFragment) {
                findNavController().popBackStack()
            }
        }
        bind.toolbarAbout.title = getString(R.string.about_app)

        bind.textViewAppName.text = getString(R.string.app_name_full) // Ilova to'liq nomi
        bind.textViewDevelopedBy.text = getString(R.string.developed_by_format, "Oybek Khamroev")
        bind.textViewContactInfo.text = getString(R.string.contact_info_format, "t.me/oybekmi") // Yoki email
        // Agar kerak bo'lsa, boshqa ma'lumotlar qo'shilishi mumkin
    }
}