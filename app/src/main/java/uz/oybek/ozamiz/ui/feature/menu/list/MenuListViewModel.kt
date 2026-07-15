package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.CalorieFilter
import uz.oybek.ozamiz.data.menu.MenuCategories

class MenuListViewModel : ViewModel() {

    private val _discoveryItems = MutableLiveData<List<MenuUiItem>>()
    val discoveryItems: LiveData<List<MenuUiItem>> = _discoveryItems

    init {
        buildDiscoveryItems()
    }

    private fun buildDiscoveryItems() {
        val items = mutableListOf<MenuUiItem>()

        items += MenuUiItem.Header(R.string.mashhur_kategoriyalar)
        items += MenuUiItem.PopularCategoriesList(
            MenuCategories.popularCategories
        )
        items += MenuUiItem.Header(R.string.kaloriya_boyicha_retseptlar)
        items += MenuUiItem.CalorieList(
            CalorieFilter.entries.toList()
        )

        items += MenuUiItem.Header(R.string.kashf_eting)
        items += MenuUiItem.MealTimeList(
            MenuCategories.mainMeals
        )

        items += MenuUiItem.Header(R.string.usulni_tanlang)
        items += MenuUiItem.MethodList(
            MenuCategories.methods
        )

        items += MenuUiItem.Header(R.string.parhezni_tanlang)
        items += MenuUiItem.DietList(
            MenuCategories.diets
        )

        items += MenuUiItem.Header(R.string.masalliqlar_boyicha)
        items += MenuUiItem.IngredientsList(
            MenuCategories.ingredients
        )

        _discoveryItems.value = items
    }
}
