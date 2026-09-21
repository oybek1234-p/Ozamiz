package uz.oybek.ozamiz.data.food

import uz.oybek.ozamiz.data.diet.DietEngine

@Deprecated(
    message = "DietController is deprecated and replaced by DietEngine. Use DietEngine for all diet calculations.",
    replaceWith = ReplaceWith("DietEngine", "uz.oybek.ozamiz.data.diet.DietEngine")
)
object DietController