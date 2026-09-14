package uz.oybek.ozamiz.data.menu

import androidx.annotation.Keep
import uz.oybek.ozamiz.R

@Keep
enum class FilterId {
    BREAKFAST, LUNCH, DINNER, SNACK,

    // Preparation Methods
    QUICKLY_PREPARED, ON_THE_GO, FEW_INGREDIENTS, BAKING, CASSEROLE, EASY, BASIC,

    // Diets
    VEGETARIAN, VEGAN, LOW_CARB, LOW_FAT, LOW_CALORIE, HIGH_PROTEIN, HIGH_FIBER,
    CLEAN_EATING, KETOGENIC, PESCATARIAN, SUGAR_FREE, LACTOSE_FREE, GLUTEN_FREE, DETOX,

    // Ingredients
    VEGETABLES, FRUITS, MEAT, FISH, SWEET,

    // Calorie Ranges
    CAL_50_100, CAL_100_200, CAL_200_300, CAL_300_400, CAL_400_500, CAL_500_600,
}

@Keep
data class FilterItem(
    val id: FilterId,
    val name: String,
    val emoji: String,
    val iconResId: Int? = null
)

enum class CategoryType(val nameRes: Int) {
    POPULAR(R.string.mashhur_kategoriyalar),
    CALORIE(R.string.kaloriya_boyicha_retseptlar),
    MAIN_MEALS(R.string.kashf_eting),
    METHODS(R.string.usulni_tanlang),
    DIETS(R.string.parhezni_tanlang),
    INGREDIENTS(R.string.masalliqlar_boyicha)
}

enum class CalorieFilter(val min: Int, val max: Int, val emoji: String) {
    CAL_50_100(50, 100, "🍉"),
    CAL_100_200(100, 200, "🥪"),
    CAL_200_300(200, 300, "🥯"),
    CAL_300_400(300, 400, "🥞"),
    CAL_400_500(400, 500, "🍛"),
    CAL_500_600(500, 600, "🍱")
}

object MenuCategories {

    val calorieFilters = CalorieFilter.entries.map {
        FilterItem(
            id = FilterId.valueOf(it.name),
            name = "${it.min}-${it.max} kal",
            emoji = it.emoji
        )
    }

    val mainMeals = listOf(
        FilterItem(
            id = FilterId.BREAKFAST,
            name = "Nonushta",
            emoji = "☕"
        ),
        FilterItem(
            id = FilterId.LUNCH,
            name = "Tushlik",
            emoji = "🍲"
        ),
        FilterItem(
            id = FilterId.DINNER,
            name = "Kechki ovqat",
            emoji = "🥗"
        ),
        FilterItem(
            id = FilterId.SNACK,
            name = "Orada (Tamaddi)",
            emoji = "🍎"
        )
    )

    val methods = listOf(
        FilterItem(
            id = FilterId.QUICKLY_PREPARED,
            name = "Tez tayyorlanadigan",
            emoji = "⏱️"
        ),
        FilterItem(
            id = FilterId.BASIC,
            name = "Oddiy",
            emoji = "🥣"
        ),
        FilterItem(
            id = FilterId.EASY,
            name = "Oson",
            emoji = "👍"
        ),
        FilterItem(
            id = FilterId.ON_THE_GO,
            name = "Yo'l-yo'lakay",
            emoji = "🍌"
        ),
        FilterItem(
            id = FilterId.FEW_INGREDIENTS,
            name = "Kam masalliqli",
            emoji = "🛒"
        ),
        FilterItem(
            id = FilterId.BAKING,
            name = "Pishiriqlar",
            emoji = "🥧"
        )
    )

    val diets = listOf(
        FilterItem(
            id = FilterId.LOW_CALORIE,
            name = "Kam kaloriyali",
            emoji = "🍏"
        ),
        FilterItem(
            id = FilterId.HIGH_PROTEIN,
            name = "Oqsillga boy",
            emoji = "🍳"
        ),
        FilterItem(
            id = FilterId.LOW_CARB,
            name = "Kam uglevodli",
            emoji = "🥜"
        ),
        FilterItem(
            id = FilterId.LOW_FAT,
            name = "Kam yog'li",
            emoji = "🥒"
        ),
        FilterItem(
            id = FilterId.DETOX,
            name = "Detoks",
            emoji = "🍵"
        ),
        FilterItem(
            id = FilterId.HIGH_FIBER,
            name = "Kletchatkaga boy",
            emoji = "🍠"
        ),
        FilterItem(
            id = FilterId.KETOGENIC,
            name = "Ketogenik",
            emoji = "🥓"
        ),
        FilterItem(
            id = FilterId.SUGAR_FREE,
            name = "Shakarsiz",
            emoji = "🚫"
        ),
        FilterItem(
            id = FilterId.LACTOSE_FREE,
            name = "Laktosiz",
            emoji = "🥛"
        ),
        FilterItem(
            id = FilterId.GLUTEN_FREE,
            name = "Glutensiz",
            emoji = "🌾"
        )
    )

    val ingredients = listOf(
        FilterItem(
            id = FilterId.VEGETABLES,
            name = "Sabzavotlar",
            emoji = "🥦"
        ),
        FilterItem(
            id = FilterId.FRUITS,
            name = "Mevalar",
            emoji = "🍍"
        ),
        FilterItem(
            id = FilterId.MEAT,
            name = "Go'sht",
            emoji = "🥩"
        ),
        FilterItem(
            id = FilterId.FISH,
            name = "Baliq",
            emoji = "🍣"
        ),
        FilterItem(
            id = FilterId.SWEET,
            name = "Shirinliklar",
            emoji = "🍬"
        )
    )

    val popularCategories = listOfNotNull(
        mainMeals.find { it.id == FilterId.BREAKFAST },
        mainMeals.find { it.id == FilterId.LUNCH },
        mainMeals.find { it.id == FilterId.DINNER },
        diets.find { it.id == FilterId.HIGH_PROTEIN },
        diets.find { it.id == FilterId.LOW_FAT },
        diets.find { it.id == FilterId.LOW_CALORIE }
    )
}