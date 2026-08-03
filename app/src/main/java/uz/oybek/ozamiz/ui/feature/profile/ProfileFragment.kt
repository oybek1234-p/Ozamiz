package uz.oybek.ozamiz.ui.feature.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.databinding.FragmentProfileBinding
import uz.oybek.ozamiz.databinding.ItemProfileSectionBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumUtils

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels()

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.updateAvatar(it) }
        }

    override fun viewCreated(bind: FragmentProfileBinding) {
        bind.btnEditAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        setupSection(
            bind.sectionMyInfo, R.drawable.person_filled, getString(R.string.my_information)
        ) {
            findNavController().navigate(R.id.getMainInfoFragment)
        }

        setupSection(
            bind.sectionSupport, R.drawable.help_ic, getString(R.string.help_and_support)
        ) {
            (activity as? MainActivity)?.showSupportSheet()
        }

        setupSection(
            bind.sectionRateApp, R.drawable.star_ic, getString(R.string.rate_app)
        ) {
            openAppInPlayStore()
        }

        setupSection(
            bind.sectionLogout, R.drawable.cancel_ic, getString(R.string.logout)
        ) {
            showLogoutConfirmationDialog()
        }

        setupSection(
            bind.sectionAdminPremiumUsersContent,
            R.drawable.admin_ic,
            getString(R.string.premium_tekshiruv)
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminPaymentVerificationListFragment)
            }
        }

        setupSection(
            bind.sectionAdminAllUsers, R.drawable.group_ic, getString(R.string.foydalanuvchilar)
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminUsersListFragment)
            }
        }

        setupSection(
            bind.sectionAdminPosts, R.drawable.post_ic, getString(R.string.postlar)
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminPostManagerFragment)
            }
        }

        bind.premiumButton.setOnClickListener {
            uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog().show(
                childFragmentManager,
                uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.TAG
            )
        }

        bind.premiumView.setOnClickListener {
            uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog().show(
                childFragmentManager,
                uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.TAG
            )
        }

        bind.cardUserLevel.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.levelFragment)
            }
        }

        setupPaymentCheckListener()
        observeViewModel(bind)
    }

    private fun setupPaymentCheckListener() {
        childFragmentManager.setFragmentResultListener(
            uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val result =
                bundle.getString(uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                val price = bundle.getLong("selected_price", 29000L)
                val isYearly = bundle.getBoolean("is_yearly", false)
                val args = androidx.core.os.bundleOf("price" to price, "is_yearly" to isYearly)
                findNavController().navigate(
                    R.id.action_profileFragment_to_paymentGetCheckFragment, args
                )
            }
        }
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

                        val level = LevelRepository.getCurrentLevel(user.scorePoints, true)
                        bind.textProfileLevelEmoji.text = level.emoji
                        bind.textProfileLevelName.text = level.name
                        bind.textProfilePoints.text =
                            getString(R.string.text_profile_points_format, user.scorePoints)

                        bind.cardUserLevel.isVisible = true

                        val isPremiumActive = PremiumUtils.isPremium(user)

                        when {
                            isPremiumActive -> {
                                bind.premiumStatusText.isVisible = true
                                bind.premiumStatusText.text = "Premium"
                                bind.premiumView.isVisible = false
                            }

                            user.paymentChecking -> {
                                bind.premiumStatusText.isVisible = true
                                bind.premiumStatusText.text = getString(R.string.premium_checking)
                                bind.premiumView.isVisible = false
                            }

                            else -> {
                                bind.premiumStatusText.isVisible = false
                                bind.premiumView.isVisible = true
                            }
                        }
                        bind.sectionAdminPremiumUsers.isVisible = user.admin
                    } else {
                        bind.textViewUserName.text = getString(R.string.default_user_name)
                        bind.imageViewUserAvatar.setImageResource(R.drawable.user_photo_placeholder)
                        bind.cardUserLevel.isVisible = false
                        bind.premiumStatusText.isVisible = false
                        bind.premiumView.isVisible = false
                        bind.sectionAdminPremiumUsers.isVisible = false
                    }
                }
            }
        }
    }

    private fun openAppInPlayStore() {
        val context = context ?: return
        val appPackageName = context.packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout_confirmation_title))
            .setMessage(getString(R.string.logout_confirmation_message))
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                viewModel.logout()
                MainActivity.signOut?.run()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}