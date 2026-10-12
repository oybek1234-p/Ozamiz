package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.loadBannerAd
import uz.oybek.ozamiz.ui.utils.toBundle
import uz.oybek.ozamiz.ui.feature.premium.isPremium

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()

    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: FragmentMenuListBinding) {
        val discoveryAdapter = MenuDiscoveryAdapter(
            onFilterClick = { type, item ->
                val args = MenuSearchResultsArgs(
                    filterType = type.name,
                    filterValue = item.id.name
                )
                navigate(R.id.menuSearchResultsFragment, args.toBundle())
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
                navigate(R.id.addMenuFragment)
            }
        }
        loadBanner()
    }

    private fun loadBanner() {
        binding.bannerContainer.loadBannerAd(AdsConfig.BANNER_HOME)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuListBinding {
        return FragmentMenuListBinding.inflate(inflater, container, false)
    }
}