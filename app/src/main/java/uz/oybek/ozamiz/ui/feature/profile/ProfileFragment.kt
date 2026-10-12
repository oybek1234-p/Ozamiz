package uz.oybek.ozamiz.ui.feature.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.databinding.FragmentProfileBinding
import uz.oybek.ozamiz.databinding.ItemProfileSectionBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumUtils
import uz.oybek.ozamiz.ui.utils.ImagePicker
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.openAppInPlayStore
import uz.oybek.ozamiz.ui.utils.registerImagePicker
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener

@AndroidEntryPoint
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

    private val pickImageLauncher = registerImagePicker { uri ->
        viewModel.updateAvatar(uri)
    }

    override fun viewCreated(bind: FragmentProfileBinding) {
        initClickListeners(bind)
        observeViewModel(bind)
    }

    private fun initClickListeners(bind: FragmentProfileBinding) = with(bind) {
        btnEditAvatar.setOnSafeClickListener {
            pickImageLauncher.launch(ImagePicker.IMAGE)
        }
        imageViewUserAvatar.setOnSafeClickListener {
            pickImageLauncher.launch(ImagePicker.IMAGE)
        }

        setupSection(
            sectionMyInfo, R.drawable.person_ic, getString(R.string.my_information)
        ) {
            navigate(R.id.getMainInfoFragment)
        }
        setupSection(
            sectionMyGoal, R.drawable.trending, getString(R.string.profile_my_goal)
        ) {
            navigate(R.id.goalInfoFragment)
        }
        setupSection(
            sectionSupport, R.drawable.help_ic, getString(R.string.help_and_support)
        ) {
            (activity as? MainActivity)?.showSupportSheet()
        }

        setupSection(
            sectionRateApp, R.drawable.like_hand, getString(R.string.rate_app)
        ) {
            openAppInPlayStore()
        }

        setupSection(
            sectionLogout, R.drawable.close_ic, getString(R.string.logout)
        ) {
            showLogoutConfirmationDialog()
        }

        setupSection(
            sectionAdminPremiumUsersContent,
            R.drawable.admin_ic,
            getString(R.string.admin_premium_verifications)
        ) {
            navigate(R.id.adminPaymentVerificationListFragment)
        }

        setupSection(
            sectionAdminAllUsers, R.drawable.group_ic, getString(R.string.foydalanuvchilar)
        ) {
            navigate(R.id.adminUsersListFragment)
        }

        setupSection(
            sectionAdminPosts, R.drawable.post_ic, getString(R.string.postlar)
        ) {
            navigate(R.id.adminPostManagerFragment)
        }

        premiumButton.setOnSafeClickListener {
            navigate(R.id.premiumFragment)
        }

        premiumView.setOnSafeClickListener {
            navigate(R.id.premiumFragment)
        }

        cardUserLevel.setOnSafeClickListener {
            navigate(R.id.levelFragment)
        }
    }

    private fun setupSection(
        binding: ItemProfileSectionBinding, iconRes: Int, title: String, onClick: () -> Unit
    ) {
        binding.iconSectionProfile.setImageResource(iconRes)
        binding.textSectionTitleProfile.text = title
        binding.root.setOnSafeClickListener { onClick() }
    }

    private fun observeViewModel(bind: FragmentProfileBinding) {
        collectFlowOnStart(
            viewModel.userState
        ) { user ->
            bindUserProfile(bind, user)
        }

        collectFlowOnStart(viewModel.uiEffect) { effect ->
            handleEffect(effect)
        }
    }

    private fun bindUserProfile(bind: FragmentProfileBinding, user: User) = with(bind) {
        if (user.valid) {
            textViewUserName.text = user.name.ifEmpty { getString(R.string.default_user_name) }
            Glide.with(this@ProfileFragment).load(user.avatarUrl)
                .error(R.drawable.user_photo_placeholder)
                .placeholder(R.drawable.user_photo_placeholder)
                .apply(RequestOptions.circleCropTransform()).into(imageViewUserAvatar)

            val level = LevelRepository.getCurrentLevel(user.scorePoints, true)
            textProfileLevelEmoji.text = level.emoji
            textProfileLevelName.text = level.name
            cardUserLevel.isVisible = false

            val isPremiumActive = PremiumUtils.isPremium(user)
            when {
                isPremiumActive -> {
                    premiumStatusText.isVisible = true
                    premiumStatusText.text = getString(R.string.premium)
                    premiumView.isVisible = false
                }

                user.paymentChecking -> {
                    premiumStatusText.isVisible = true
                    premiumStatusText.text = getString(R.string.premium_checking)
                    premiumView.isVisible = false
                }

                else -> {
                    premiumStatusText.isVisible = false
                    premiumView.isVisible = true
                }
            }
            sectionAdminPremiumUsers.isVisible = user.admin
        } else {
            textViewUserName.text = getString(R.string.default_user_name)
            imageViewUserAvatar.setImageResource(R.drawable.user_photo_placeholder)
            cardUserLevel.isVisible = false
            premiumStatusText.isVisible = false
            premiumView.isVisible = false
            sectionAdminPremiumUsers.isVisible = false
        }
    }

    private fun handleEffect(effect: ProfileUiEffect) {
        when (effect) {
            is ProfileUiEffect.NavigateToAuth -> {
                navigate(R.id.authFragment, null) {
                    setPopUpTo(R.id.main_navigation, true)
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.logout_confirmation_title))
            .setMessage(getString(R.string.logout_confirmation_message))
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                viewModel.logout()
            }.setNegativeButton(getString(R.string.cancel), null).show()
    }
}