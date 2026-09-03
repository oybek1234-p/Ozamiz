package uz.oybek.ozamiz.ui.feature.log.food

object SearchNormalizer {

    private val cyrillicToLatinMap = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "yo", 'ж' to "j", 'з' to "z", 'и' to "i",
        'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "x", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sh", 'ъ' to "", 'ы' to "i", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya", 'ў' to "o", 'қ' to "q",
        'ғ' to "g", 'ҳ' to "h"
    )

    fun cyrillicToLatin(text: String): String {
        val sb = StringBuilder()
        for (ch in text.lowercase()) {
            val converted = cyrillicToLatinMap[ch]
            if (converted != null) {
                sb.append(converted)
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun normalize(text: String): String {
        val latinText = cyrillicToLatin(text)
        return latinText
            .lowercase()
            .replace("o'", "o")
            .replace("o‘", "o")
            .replace("o`", "o")
            .replace("g'", "g")
            .replace("g‘", "g")
            .replace("g`", "g")
            .replace("coca", "koka")
            .replace("cola", "kola")
            .replace("c", "k")
            .replace("-", "")
            .replace("'", "")
            .replace("‘", "")
            .replace("`", "")
            .replace(" ", "")
    }

    fun getWords(text: String): List<String> {
        val latin = cyrillicToLatin(text)
        return latin.split(Regex("[\\s()\\-,.]+")).filter { it.isNotBlank() }
    }
}
