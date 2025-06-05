package uz.oybek.ozamiz.ui.foods

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels // Agar ViewModel kerak bo'lsa
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.HeavyFoodItem
import uz.oybek.ozamiz.databinding.FragmentHeavyFoodsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class HeavyFoodsFragment : BaseFragment<FragmentHeavyFoodsBinding>() {

    // private val viewModel: HeavyFoodsViewModel by viewModels() // Agar ViewModel kerak bo'lsa
    private lateinit var heavyFoodAdapter: HeavyFoodAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHeavyFoodsBinding {
        return FragmentHeavyFoodsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentHeavyFoodsBinding) {
        bind.toolbarHeavyFoods.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        setupRecyclerView(bind)
        loadHeavyFoods(bind)
    }

    private fun setupRecyclerView(bind: FragmentHeavyFoodsBinding) {
        heavyFoodAdapter = HeavyFoodAdapter() // Listener kerak bo'lsa qo'shiladi
        bind.recyclerViewHeavyFoods.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = heavyFoodAdapter
        }
    }

    private fun loadHeavyFoods(bind: FragmentHeavyFoodsBinding) {
        val heavyFoodsList = listOf(
            HeavyFoodItem(
                id = "plov",
                name = "Osh (Palov)",
                qachon = "Har 10–14 kunda",
                qancha = "1 kosa (150–200g)",
                mahal = "Tushlik",
                estimatedCalories = 500
            ),
            HeavyFoodItem(
                id = "somsa",
                name = "Somsa",
                qachon = "Haftasiga 1 marta",
                qancha = "1 dona",
                mahal = "Tushlik yoki tamaddi",
                estimatedCalories = 300
            ),
            HeavyFoodItem(
                id = "lagmon",
                name = "Lag'mon",
                qachon = "Har 10 kunda",
                qancha = "Yarim porsiya (200g)",
                mahal = "Tushlik",
                estimatedCalories = 400
            ),
            HeavyFoodItem(
                id = "manti",
                name = "Manti / Chuchvara",
                qachon = "Haftasiga 1 marta",
                qancha = "4–5 dona",
                mahal = "Tushlik yoki kechki ovqat",
                estimatedCalories = 350
            ),
            HeavyFoodItem(
                id = "kabob",
                name = "Kabob",
                qachon = "Har 7–10 kunda",
                qancha = "1 shashlik + sabzavot",
                mahal = "Tushlik",
                estimatedCalories = 450
            ),
            HeavyFoodItem(
                id = "qovurmakartoshka",
                name = "Qovurilgan kartoshka",
                qachon = "2 haftada 1 marta",
                qancha = "100–150g",
                mahal = "Tushlik",
                estimatedCalories = 350
            ),
            HeavyFoodItem(
                id = "shirinlik",
                name = "Shirinlik (tort, pirojniy)",
                qachon = "Haftasiga 1 marta",
                qancha = "1 kichik bo‘lak",
                mahal = "Tamaddi yoki bayram payti",
                estimatedCalories = 250
            ),
            HeavyFoodItem(
                id = "shirinichimlik",
                name = "Shirin ichimlik (kola, sharbat)",
                qachon = "Faqat maxsus holatlarda",
                qancha = "1 stakan (200ml)",
                mahal = "Tamaddi",
                estimatedCalories = 150
            )
        )

        heavyFoodAdapter.submitList(heavyFoodsList)
    }

}