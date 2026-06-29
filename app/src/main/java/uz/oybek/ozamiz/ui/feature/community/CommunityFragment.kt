package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentCommunityBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CommunityFragment : BaseFragment<FragmentCommunityBinding>() {

    private val viewModel: CommunityViewModel by viewModels()

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunityBinding {
        return FragmentCommunityBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCommunityBinding) {
        setupToolbar()
        setupViewPager()
        observeNotifications()
    }

    private fun setupToolbar() {
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_notificationsFragment)
        }
        binding.fabAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_addPostFragment)
        }
    }

    private fun setupViewPager() {
        val adapter = CommunityTabAdapter(this)
        binding.viewPager.adapter = adapter

        com.google.android.material.tabs.TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Postlar"
                1 -> "Foydalanuvchilar"
                else -> null
            }
        }.attach()
    }

    private fun observeNotifications() {
        viewModel.observeUnReadMessages { count ->
            binding.tvNotificationBadge.isVisible = count > 0
            binding.tvNotificationBadge.text = count.toString()
        }
    }

    private inner class CommunityTabAdapter(fragment: androidx.fragment.app.Fragment) : 
        androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return when (position) {
                0 -> CommunityPostsFragment()
                1 -> CommunityUsersFragment()
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}
