package uz.oybek.ozamiz.ui.base

open class ListClickListener<Model> {

    open fun onClick(holder: BaseAdapter.ViewHolder<*>, model: Model) { }
    open fun onLongClick(holder: BaseAdapter.ViewHolder<*>, model: Model) { }
}