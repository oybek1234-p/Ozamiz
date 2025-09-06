package uz.oybek.ozamiz.data.lessons

data class LessonModel(
    val id: String,
    val name: String,
    val order: Int,
    val about: String,
    var imageUrl: String
) {
    constructor(): this("", "", 0, "", "")
}