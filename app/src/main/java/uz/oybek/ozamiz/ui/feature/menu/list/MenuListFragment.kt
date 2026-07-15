package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()

    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: FragmentMenuListBinding) {
        val discoveryAdapter = MenuDiscoveryAdapter { type, item ->
            findNavController().navigate(
                MenuListFragmentDirections.actionMenuListFragmentToMenuSearchResultsFragment(
                    filterType = type.name,
                    filterValue = item.id.name
                )
            )
        }
        bind.recvMenuList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = discoveryAdapter
        }
        viewModel.discoveryItems.observe(viewLifecycleOwner) {
            discoveryAdapter.submitList(it)
        }

        val isAdmin = UserProfileRepository.user.admin
        if (isAdmin) {
            bind.fabAddFood.visibility = View.VISIBLE
            bind.fabAddFood.setOnClickListener {
                findNavController().navigate(
                    MenuListFragmentDirections.actionMenuListFragmentToAddMenuFragment()
                )
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuListBinding {
        return FragmentMenuListBinding.inflate(inflater, container, false)
    }
}