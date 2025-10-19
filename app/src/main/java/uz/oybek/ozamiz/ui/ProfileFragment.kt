package uz.oybek.ozamiz.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.MainActivity
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentProfileBinding
import uz.oybek.ozamiz.databinding.ItemProfileSectionBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.premium.getPremiumExpireDate

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels()

    init {
        showBottomSheet = true
    }
    
    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentProfileBinding {
        Thread.currentThread().stackTrace
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentProfileBinding) {
        setupSection(
            bind.sectionMyInfo, R.drawable.person_filled, getString(R.string.my_information)
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                val action =
                    ProfileFragmentDirections.actionProfileFragmentToGetMainInfoFragment(new = false)
                findNavController().navigate(action)
            }
        }
        setupSection(
            bind.sectionWeightHistory,
            R.drawable.history_24px,
            getString(R.string.weight_history_title)
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.weightPhotoHistoryFragment)
            }
        }
//        setupSection(bind.sectionReminders, R.drawable.notify_ic, getString(R.string.reminders)) {
//            if (findNavController().currentDestination?.id == R.id.profileFragment) {
//                val action = ProfileFragmentDirections.actionProfileFragmentToSetupRemindersFragment()
//                findNavController().navigate(action)
//            }
//        }
        setupSection(
            bind.sectionSupport, R.drawable.help_ic, getString(R.string.help_and_support)
        ) {
            (activity as? MainActivity)?.showSupportSheet()
        }
        setupSection(bind.sectionRateApp, R.drawable.star_ic, getString(R.string.rate_app)) {
            openAppInPlayStore()
        }
        setupSection(
            bind.sectionAdminPremiumUsersContent, R.drawable.admin_ic, "Premium Tekshiruv"
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminPaymentVerificationListFragment)
            }
        }

        bind.buttonLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        bind.premiumButton.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                findNavController().navigate(R.id.paymentGetCheckFragment)
            }
        }
        observeViewModel(bind)
    }

    private fun setupSection(
        binding: ItemProfileSectionBinding, iconRes: Int, title: String, onClick: () -> Unit
    ) {
        binding.iconSectionProfile.setImageResource(iconRes)
        binding.textSectionTitleProfile.text = title
        binding.root.setOnClickListener { onClick() }
    }


    private fun observeViewModel(bind: FragmentProfileBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userState.collect { user ->
                    if (user.valid) {
                        bind.textViewUserName.text =
                            user.name.ifEmpty { getString(R.string.default_user_name) }
                        Glide.with(this@ProfileFragment).load(user.avatarUrl)
                            .error(R.drawable.user_photo_placeholder)
                            .placeholder(R.drawable.user_photo_placeholder)
                            .apply(RequestOptions.circleCropTransform())
                            .into(bind.imageViewUserAvatar)
                        bind.buttonLogout.isVisible = true

                        when {
                            user.premium -> {
                                bind.premiumActiveC.isVisible = true
                                bind.premiumView.isVisible = false
                                bind.premiumExpireDate.text =
                                    "Premium ${user.getPremiumExpireDate()} gacha amal qiladi"
                            }

                            user.paymentChecking -> {
                                bind.premiumActiveC.isVisible = true
                                bind.premiumView.isVisible = false
                                bind.premiumExpireDate.text = "Premium tekshirilmoqda..."
                            }

                            else -> {
                                bind.premiumActiveC.isVisible = false
                            }
                        }
                        bind.sectionAdminPremiumUsers.isVisible = user.admin
                    } else {
                        bind.textViewUserName.text = getString(R.string.default_user_name)
                        bind.imageViewUserAvatar.setImageResource(R.drawable.ic_profile)
                        bind.buttonLogout.isVisible = false
                        bind.premiumActiveC.isVisible = false
                        bind.premiumView.isVisible = false
                        bind.sectionAdminPremiumUsers.isVisible = false
                    }
                }
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (findNavController().currentDestination?.id != R.id.profileFragment && navEvent !is ProfileNavigationEvent.NavigateToAuth) {
                    return@let
                }
                when (navEvent) {
                    ProfileNavigationEvent.NavigateToAuth -> {
                        (activity as? MainActivity)?.recreateUi(false)
                    }

                    is ProfileNavigationEvent.ShowToast -> {
                        showToast(navEvent.message)
                    }
                }
            }
        }
    }

    private fun openAppInPlayStore() {
        val appPackageName = requireActivity().packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }

    private fun showLogoutConfirmationDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext()).setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                viewModel.logout()
            }.setNegativeButton(R.string.cancel, null).show()
    }
}