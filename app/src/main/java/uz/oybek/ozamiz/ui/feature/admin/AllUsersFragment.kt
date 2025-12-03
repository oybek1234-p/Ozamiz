package uz.oybek.ozamiz.ui.feature.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.AllUsersFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class AllUsersFragment : BaseFragment<AllUsersFragmentBinding>() {

    override fun viewCreated(bind: AllUsersFragmentBinding) {

    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): AllUsersFragmentBinding {
        return AllUsersFragmentBinding.inflate(
            inflater,
            container,
            false
        )
    }


}