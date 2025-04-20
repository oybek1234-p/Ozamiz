package uz.oybek.ozamiz.ui.base

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil

abstract class SelectableAdapter<Model, B : ViewDataBinding>(
    val multiple: Boolean, itemResId: Int, diffUtil: DiffUtil.ItemCallback<Model>
) : BaseAdapter<Model, B>(itemResId, diffUtil) {

    var selectable = true
    val selectedPositions = mutableSetOf<Int>()

    val selectedPosition: Int? get() = selectedPositions.lastOrNull()

    fun isSelected(pos: Int) = selectedPositions.contains(pos)

    abstract fun onSelected(pos: Int, selected: Boolean)
    abstract fun bind(holder: ViewHolder<*>, model: Model, pos: Int, selected: Boolean)

    override fun bind(holder: ViewHolder<*>, model: Model, pos: Int) {
        bind(holder, model, pos, selectedPositions.contains(pos))
    }

    fun setSelected(pos: Int) {
        if (multiple) {
            val selected = if (isSelected(pos)) {
                selectedPositions.remove(pos)
                false
            } else {
                selectedPositions.add(pos)
                true
            }
            notifyItemChanged(pos)
            onSelected(pos, selected)
        } else {
            val lastSelected = selectedPosition
            if (lastSelected == pos) {
                return
            } else {
                if (lastSelected != null) {
                    selectedPositions.remove(lastSelected)
                    onSelected(lastSelected, false)
                    notifyItemChanged(lastSelected)
                }
                selectedPositions.add(pos)
                onSelected(pos, true)
                notifyItemChanged(pos)
            }
        }
    }

    init {
        addClickListener(object : ListClickListener<Model>() {
            override fun onClick(holder: ViewHolder<*>, model: Model) {
                if (selectable) {
                    setSelected(holder.adapterPosition)
                }
            }
        })
    }
}