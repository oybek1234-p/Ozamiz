package uz.oybek.ozamiz.ui.diet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.diet.DietPlan
import uz.oybek.ozamiz.databinding.DietPlanItemBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.getinfo.formatHour
import uz.oybek.ozamiz.ui.getinfo.formatHourMin
import uz.oybek.ozamiz.ui.loadPhoto

class DietPlanAdapter : BaseAdapter<DietPlan,DietPlanItemBinding>(R.layout.diet_plan_item,object : DiffUtil.ItemCallback<DietPlan>() {
    override fun areContentsTheSame(oldItem: DietPlan, newItem: DietPlan): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: DietPlan, newItem: DietPlan): Boolean {
        return oldItem.id == newItem.id
    }
}) {
    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): DietPlanItemBinding {
        return DietPlanItemBinding.inflate(inflater,parent,false)
    }

    override fun bind(holder: ViewHolder<*>, model: DietPlan, pos: Int) {
        super.bind(holder, model, pos)
        holder.apply {
            binding.apply {
                (this as DietPlanItemBinding).apply {
                    nameView.text = model.name
                    timeView.text = formatHourMin(model.hourAndMin.first,model.hourAndMin.second)
                    foodView.text = model.foods
                    descriptionView.text = model.description
                    warningView.text = model.warning
                    photoView.loadPhoto(model.photo)
                    foodView.isVisible = model.foods.isNotEmpty()
                    descriptionView.isVisible = model.description.isNotEmpty()
                    warningView.isVisible = model.warning.isNotEmpty()
                    openFoodsButton.isVisible = model.hasFoods
                }
            }
        }
    }
}