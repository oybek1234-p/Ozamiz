package uz.oybek.ozamiz.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.gamification.Badge
import uz.oybek.ozamiz.databinding.FragmentBadgesBinding
import uz.oybek.ozamiz.databinding.ItemBadgeBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.base.EmptyDiffUtil

class BadgesFragment : BaseFragment<FragmentBadgesBinding>() {

    private val viewModel: BadgesViewModel by viewModels()
    private lateinit var badgeAdapter: BadgeAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBadgesBinding {
        return FragmentBadgesBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentBadgesBinding) {
        bind.toolbarBadges.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        badgeAdapter = BadgeAdapter()
        bind.recyclerViewBadges.apply {
            layoutManager = GridLayoutManager(requireContext(), 3) // 3 ustunli
            adapter = badgeAdapter
        }

        observeViewModel(bind)
    }

    private fun observeViewModel(bind: FragmentBadgesBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    badgeAdapter.submitList(state.badges)
                    // Loading yoki error holatlarini bu yerda ko'rsatish mumkin
                }
            }
        }
    }
}

class BadgeAdapter : BaseAdapter<Badge, ItemBadgeBinding>(
    R.layout.item_badge,
    EmptyDiffUtil()
) {
    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): ItemBadgeBinding {
        return ItemBadgeBinding.inflate(inflater, parent, false)
    }

    override fun bind(holder: ViewHolder<*>, model: Badge, pos: Int) {
        val binding = holder.binding as ItemBadgeBinding
        binding.badgeName.text = model.name
        binding.badgeImage.setImageResource(model.iconResId)

        if (model.isUnlocked) {
            binding.badgeImage.clearColorFilter()
            binding.lockIcon.visibility = View.GONE
            binding.badgeName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black)) // Yoki ?colorOnSurface
        } else {
            binding.badgeImage.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.grey_alpha_50))
            binding.lockIcon.visibility = View.VISIBLE
            binding.badgeName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.grey)) // Yoki ?colorOnSurfaceVariant
        }
        // Click listener qo'shish mumkin (nishon haqida batafsil ma'lumot uchun)
        holder.itemView.setOnClickListener {
            // Toast.makeText(holder.itemView.context, model.description, Toast.LENGTH_SHORT).show()
        }
    }
}