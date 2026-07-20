package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetInfoBinding
import uz.oybek.ozamiz.databinding.ItemHungryFoodBinding

class InfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val content = arguments?.getString(ARG_CONTENT) ?: ""

        binding.textTitle.text = title

        if (title.contains("Ochlikda") || title.contains("Hungry")) {
            val mainText = content.substringBefore("\n\n")
            binding.textContent.text = mainText

            val foods = listOf(
                HungryFood("🥒", "Bodring va ko'katlar", "Deyarli kaloriyasiz va oshqozonni to'ldiradi"),
                HungryFood("🥛", "Kefir yoki qatiq", "Past yog'li sut mahsulotlari hazm qilishni yaxshilaydi"),
                HungryFood("🥚", "Tuxum oqi", "Sof oqsil manbai, uzoq vaqt to'q tutadi"),
                HungryFood("🍵", "Ko'k choy", "Shakar qo'shmasdan iching, ochlikni bostiradi"),
                HungryFood("🥬", "Karam salati", "Kletchatkaga boy va kkal darajasi juda past")
            )

            binding.rvHungryFoods.visibility = View.VISIBLE
            binding.rvHungryFoods.layoutManager = LinearLayoutManager(context)
            binding.rvHungryFoods.adapter = HungryFoodAdapter(foods)
        } else {
            binding.textContent.text = content
            binding.rvHungryFoods.visibility = View.GONE
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    data class HungryFood(val emoji: String, val name: String, val desc: String)

    inner class HungryFoodAdapter(private val items: List<HungryFood>) :
        RecyclerView.Adapter<HungryFoodAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemHungryFoodBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHungryFoodBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.textEmoji.text = item.emoji
            holder.binding.textFoodName.text = item.name
            holder.binding.textFoodDesc.text = item.desc
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "InfoBottomSheet"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_CONTENT = "arg_content"

        fun newInstance(title: String, content: String) = InfoBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_CONTENT, content)
            }
        }
    }
}
