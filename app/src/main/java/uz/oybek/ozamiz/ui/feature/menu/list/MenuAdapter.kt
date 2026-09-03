package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.databinding.ItemSearchMenuBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.utils.loadPhoto
import androidx.fragment.app.FragmentActivity
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.data.user.local.user

class MenuAdapter(
    private val onMenuClick: (Menu) -> Unit,
    private val nextLoad: () -> Unit
) : BaseAdapter<Menu, ItemSearchMenuBinding>(R.layout.item_search_menu, MenuDiffCallback()) {

    override fun inflate(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemSearchMenuBinding {
        return ItemSearchMenuBinding.inflate(inflater, parent, false)
    }

    override fun bind(holder: ViewHolder<*>, model: Menu, pos: Int) {
        holder.apply {
            if (holder.layoutPosition == itemCount - 1) {
                nextLoad()
            }
            binding.apply {
                (this as ItemSearchMenuBinding)
                val isPremiumUser = user.isPremium()
                val isLocked = !isPremiumUser && pos >= 3

                if (isLocked) {
                    textName.text = root.context.getString(R.string.premium)
                    textMeta.visibility = View.GONE
                    premiumOverlay.visibility = View.VISIBLE
                    root.setOnClickListener {
                        (root.context as? FragmentActivity)?.let { activity ->
                            val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.container) as? androidx.navigation.fragment.NavHostFragment
                            navHostFragment?.navController?.navigate(R.id.premiumFragment)
                        }
                    }
                } else {
                    textName.text = model.name
                    textMeta.visibility = View.VISIBLE
                    textMeta.text = root.context.getString(
                        R.string.calories_format,
                        model.calories
                    ) + "  " + root.context.getString(
                        R.string.duration_hint
                    ) + " " + model.preparationTime
                    premiumOverlay.visibility = View.GONE
                    root.setOnClickListener { onMenuClick(model) }
                }

                if (model.imageUrl != null && !isLocked) {
                    imageMenu.loadPhoto(model.imageUrl!!)
                } else {
                    imageMenu.setImageResource(R.drawable.placeholder_food)
                }
            }
        }
    }

    class MenuDiffCallback : DiffUtil.ItemCallback<Menu>() {
        override fun areItemsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem == newItem
        }
    }
}
