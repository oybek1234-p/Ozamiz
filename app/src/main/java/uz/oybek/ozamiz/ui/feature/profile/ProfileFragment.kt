package uz.oybek.ozamiz.ui.feature.profile

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentProfileBinding
import uz.oybek.ozamiz.databinding.ItemProfileSectionBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.getPremiumExpireDate
import uz.oybek.ozamiz.ui.feature.premium.premiumExpired
import uz.oybek.ozamiz.data.level.LevelManager

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

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.updateAvatar(it) }
    }

    override fun viewCreated(bind: FragmentProfileBinding) {
        bind.btnEditAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        setupSection(
            bind.sectionMyInfo,
            R.drawable.person_filled,
            getString(R.string.my_information)
        ) {
            findNavController().navigate(R.id.getMainInfoFragment)
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
        setupSection(
            bind.sectionUserLessons,
            R.drawable.study_ic,
            "Darsliklar"
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.lessonsHomeFragment)
            }
        }
        setupSection(
            bind.sectionSupport, R.drawable.help_ic, getString(R.string.help_and_support)
        ) {
            (activity as? MainActivity)?.showSupportSheet()
        }
        setupSection(bind.sectionRateApp, R.drawable.star_ic, getString(R.string.rate_app)) {
            openAppInPlayStore()
        }
        setupSection(
            bind.sectionCommunity, R.drawable.people_ic, getString(R.string.hamjamiyat)
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.communityFragment)
            }
        }
        setupSection(
            bind.sectionAdminPremiumUsersContent, R.drawable.admin_ic, "Premium Tekshiruv"
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminPaymentVerificationListFragment)
            }
        }
        setupSection(
            bind.sectionAdminLessons, R.drawable.ic_profile, "Admin: Darsliklar"
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminLessonsFragment)
            }
        }
        setupSection(
            bind.sectionAdminAllUsers, R.drawable.group_ic, "Admin: Foydalanuvchilar"
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminUsersListFragment)
            }
        }
        setupSection(
            bind.sectionAdminPosts, R.drawable.post_ic, "Admin: Postlar"
        ) {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                navigate(R.id.action_profileFragment_to_adminPostManagerFragment)
            }
        }
        bind.buttonLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        bind.premiumButton.setOnClickListener {
            uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog().show(childFragmentManager, uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.TAG)
        }
        
        bind.premiumView.setOnClickListener {
            uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog().show(childFragmentManager, uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.TAG)
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
            val result = bundle.getString(uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                val price = bundle.getLong("selected_price", 29000L)
                val isYearly = bundle.getBoolean("is_yearly", false)
                val args = androidx.core.os.bundleOf("price" to price, "is_yearly" to isYearly)
                findNavController().navigate(R.id.action_profileFragment_to_paymentGetCheckFragment, args)
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


    @SuppressLint("SetTextI18n")
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
                        
                        val level = LevelManager.getCurrentLevel(user.scorePoints)
                        bind.textProfileLevelEmoji.text = level.emoji
                        bind.textProfileLevelName.text = level.name
                        
                        bind.buttonLogout.isVisible = true

                        when {
                            user.premium && !user.premiumExpired() -> {
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
                                bind.premiumView.isVisible = true
                            }
                        }
                        bind.sectionAdminPremiumUsers.isVisible = user.admin
                        bind.containerAdminLessons.isVisible = user.admin
                    } else {
                        bind.textViewUserName.text = getString(R.string.default_user_name)
                        bind.imageViewUserAvatar.setImageResource(R.drawable.ic_profile)
                        bind.buttonLogout.isVisible = false
                        bind.premiumActiveC.isVisible = false
                        bind.premiumView.isVisible = false
                        bind.sectionAdminPremiumUsers.isVisible = false
                        bind.containerAdminLessons.isVisible = false
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
                MainActivity.signOut?.run()
            }.setNegativeButton(
                R.string.cancel,
                null
            ).show()
    }
}