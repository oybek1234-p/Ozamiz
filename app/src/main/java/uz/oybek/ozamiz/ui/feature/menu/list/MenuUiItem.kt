package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.menu.CalorieFilter
import uz.oybek.ozamiz.data.menu.FilterItem

@Keep
sealed interface MenuUiItem {
    
    data class Header(val titleResId: Int) : MenuUiItem

    data class PopularCategoriesList(
        val items: List<FilterItem>
    ) : MenuUiItem

    data class MealTimeList(
        val items: List<FilterItem>
    ) : MenuUiItem

    data class MethodList(
        val items: List<FilterItem>
    ) : MenuUiItem

    data class DietList(
        val items: List<FilterItem>
    ) : MenuUiItem

    data class CalorieList(
        val items: List<CalorieFilter>
    ) : MenuUiItem

    data class IngredientsList(
        val items: List<FilterItem>
    ) : MenuUiItem

    data class DietitianCard(val hasMenu: Boolean) : MenuUiItem
}
