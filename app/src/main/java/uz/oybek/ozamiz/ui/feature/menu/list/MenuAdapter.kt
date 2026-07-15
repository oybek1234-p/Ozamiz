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
                textName.text = model.name
                textMeta.text = binding.root.context.getString(
                    R.string.calories_format,
                    model.calories
                ) + "  " + binding.root.context.getString(
                    R.string.duration_hint
                ) + " " + model.preparationTime

                if (model.imageUrl != null) {
                    imageMenu.loadPhoto(model.imageUrl!!)
                } else {
                    imageMenu.setImageResource(R.drawable.placeholder_food)
                }

                premiumOverlay.visibility = View.GONE

                binding.root.setOnClickListener { onMenuClick(model) }
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
