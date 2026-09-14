package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()

    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: FragmentMenuListBinding) {
        val discoveryAdapter = MenuDiscoveryAdapter(
            onFilterClick = { type, item ->
                navigate(
                    MenuListFragmentDirections.actionMenuListFragmentToMenuSearchResultsFragment(
                        filterType = type.name,
                        filterValue = item.id.name
                    )
                )
            },
            onDietitianClick = {}
        )
        bind.recvMenuList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = discoveryAdapter
        }
        viewModel.discoveryItems.observe(viewLifecycleOwner) {
            discoveryAdapter.submitList(it)
        }

        val isAdmin = user.admin
        if (isAdmin) {
            bind.fabAddFood.visibility = View.VISIBLE
            bind.fabAddFood.setOnClickListener {
                navigate(
                    MenuListFragmentDirections.actionMenuListFragmentToAddMenuFragment()
                )
            }
        }
        loadBanner()
    }

    private fun loadBanner() {
        if (user.isPremium()) {
            binding.bannerContainer.visibility = View.GONE
            return
        }
        try {
            val bannerView = AdsManager.createBannerView(requireContext(), AdsConfig.BANNER_HOME)
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.addView(bannerView)
            binding.bannerContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuListBinding {
        return FragmentMenuListBinding.inflate(inflater, container, false)
    }
}