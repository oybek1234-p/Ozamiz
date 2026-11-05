package extractor

import java.io.File

fun populateFoodDatabase(
    foodsTxtPath: String = "foods.txt",
    databaseKtPath: String = "data/src/main/java/uz/oybek/ozamiz/data/food/model/FoodDatabase.kt"
) {
    val projectRoot = findProjectRoot() ?: File(".")
    val foodsFile = File(projectRoot, foodsTxtPath).takeIf { it.exists() } ?: File(foodsTxtPath)
    if (!foodsFile.exists()) {
        println("Error: $foodsTxtPath not found (checked ${foodsFile.absolutePath}).")
        return
    }

    val databaseFile = File(projectRoot, databaseKtPath).takeIf { it.exists() } ?: File(databaseKtPath)
    if (!databaseFile.exists()) {
        println("Error: $databaseKtPath not found (checked ${databaseFile.absolutePath}).")
        return
    }

    val foodsContent = foodsFile.readText(Charsets.UTF_8).trim()
    val items = parseFoodItems(foodsContent)
    if (items.isEmpty()) {
        println("Error: No FoodItems found in $foodsTxtPath")
        return
    }

    val chunks = items.chunked(100)

    val generatedCode = buildString {
        appendLine("package uz.oybek.ozamiz.data.food.model")
        appendLine()
        appendLine("import kotlinx.coroutines.Dispatchers")
        appendLine("import kotlinx.coroutines.withContext")
        appendLine("import uz.oybek.ozamiz.common.util.SearchNormalizer")
        appendLine("import uz.oybek.ozamiz.data.tracker.model.MealType")
        appendLine()
        appendLine("object FoodDatabase {")
        appendLine("    fun createAllFoods(): List<FoodItem> =")
        chunks.indices.forEach { i ->
            val isLast = i == chunks.size - 1
            val plus = if (isLast) "" else " +"
            appendLine("        createFoodsPart${i + 1}()$plus")
        }
        appendLine()

        chunks.forEachIndexed { chunkIndex, chunkItems ->
            appendLine("    private fun createFoodsPart${chunkIndex + 1}(): List<FoodItem> = listOf(")
            chunkItems.forEachIndexed { index, item ->
                val isLastInChunk = index == chunkItems.size - 1
                appendLine(item)
                if (!isLastInChunk) {
                    appendLine(",")
                }
            }
            appendLine("    )")
            appendLine()
        }

        appendLine("    @Volatile")
        appendLine("    private var cachedSearchableFoods: List<SearchableFood>? = null")
        appendLine()
        appendLine("    suspend fun getSearchableFoods(): List<SearchableFood> =")
        appendLine("        withContext(Dispatchers.Default) {")
        appendLine("            cachedSearchableFoods ?: synchronized(FoodDatabase) {")
        appendLine("                cachedSearchableFoods ?: createAllFoods().map { food ->")
        appendLine("                    val lowerName = food.name.lowercase()")
        appendLine("                    val normName = SearchNormalizer.normalize(food.name)")
        appendLine("                    val words = SearchNormalizer.getWords(food.name)")
        appendLine("                    val lowerAliases = food.searchAliases.map { it.lowercase() }")
        appendLine("                    val normAliases = food.searchAliases.map { SearchNormalizer.normalize(it) }")
        appendLine("                    val aliasWords = food.searchAliases.flatMap { SearchNormalizer.getWords(it) }")
        appendLine("                    SearchableFood(")
        appendLine("                        food = food,")
        appendLine("                        lowerName = lowerName,")
        appendLine("                        normName = normName,")
        appendLine("                        words = words,")
        appendLine("                        lowerAliases = lowerAliases,")
        appendLine("                        normAliases = normAliases,")
        appendLine("                        aliasWords = aliasWords")
        appendLine("                    )")
        appendLine("                }.also { cachedSearchableFoods = it }")
        appendLine("            }")
        appendLine("        }")
        appendLine()
        appendLine("    suspend fun getAllFoods(): List<FoodItem> = getSearchableFoods().map { it.food }")
        appendLine("}")
        appendLine()
        appendLine("data class SearchableFood(")
        appendLine("    val food: FoodItem,")
        appendLine("    val lowerName: String,")
        appendLine("    val normName: String,")
        appendLine("    val words: List<String>,")
        appendLine("    val lowerAliases: List<String>,")
        appendLine("    val normAliases: List<String>,")
        appendLine("    val aliasWords: List<String>")
        appendLine(") {")
        appendLine("    fun calculateScore(rawQuery: String, normQuery: String, lowerQuery: String): Int {")
        appendLine("        val isExactNormalized = normName == normQuery")
        appendLine("        val isExactName = lowerName == lowerQuery")
        appendLine("        val isStartName = lowerName.startsWith(lowerQuery)")
        appendLine("        val normStartName = normName.startsWith(normQuery)")
        appendLine("        val firstWordExact = words.firstOrNull()?.lowercase() == lowerQuery")
        appendLine("        val anyWordExact = words.any { it.lowercase() == lowerQuery }")
        appendLine("        val anyWordStarts = words.any { it.lowercase().startsWith(lowerQuery) }")
        appendLine("        val containsQuery = normName.contains(normQuery) || lowerName.contains(lowerQuery)")
        appendLine()
        appendLine("        val exactAliasMatch =")
        appendLine("            lowerAliases.any { it == lowerQuery } || normAliases.any { it == normQuery }")
        appendLine("        val startAliasMatch = lowerAliases.any { it.startsWith(lowerQuery) } || normAliases.any {")
        appendLine("            it.startsWith(normQuery)")
        appendLine("        }")
        appendLine("        val aliasWordExact = aliasWords.any { it.lowercase() == lowerQuery }")
        appendLine("        val aliasWordStarts = aliasWords.any { it.lowercase().startsWith(lowerQuery) }")
        appendLine("        val aliasContains =")
        appendLine("            normAliases.any { it.contains(normQuery) } || lowerAliases.any { it.contains(lowerQuery) }")
        appendLine()
        appendLine("        val matchScore = when {")
        appendLine("            isExactName || isExactNormalized -> 5000")
        appendLine("            exactAliasMatch -> 4500")
        appendLine("            firstWordExact -> 4000")
        appendLine("            isStartName || normStartName -> 3500")
        appendLine("            startAliasMatch -> 3000")
        appendLine("            anyWordExact -> 2800")
        appendLine("            aliasWordExact -> 2200")
        appendLine("            anyWordStarts -> 1500")
        appendLine("            aliasWordStarts -> 600")
        appendLine("            containsQuery -> 400")
        appendLine("            aliasContains -> 200")
        appendLine("            else -> 0")
        appendLine("        }")
        appendLine()
        appendLine("        return if (matchScore > 0) {")
        appendLine("            val popularBonus = food.popularityRank * 50")
        appendLine("            matchScore + popularBonus")
        appendLine("        } else {")
        appendLine("            0")
        appendLine("        }")
        appendLine("    }")
        appendLine("}")
    }

    databaseFile.writeText(generatedCode, Charsets.UTF_8)
    println("Successfully populated and chunked ${databaseFile.absolutePath} from ${foodsFile.absolutePath} (${items.size} items in ${chunks.size} parts)")
}

fun parseFoodItems(content: String): List<String> {
    val items = mutableListOf<String>()
    var depth = 0
    var startIndex = -1
    var i = 0
    val target = "FoodItem("
    while (i <= content.length - target.length) {
        if (content.startsWith(target, i)) {
            if (depth == 0) {
                startIndex = i
            }
            depth++
            i += target.length
        } else if (content[i] == '(') {
            if (depth > 0) depth++
            i++
        } else if (content[i] == ')') {
            if (depth > 0) {
                depth--
                if (depth == 0 && startIndex != -1) {
                    items.add(content.substring(startIndex, i + 1).trim())
                    startIndex = -1
                }
            }
            i++
        } else {
            i++
        }
    }
    return items
}

private fun findProjectRoot(startDir: File = File(".")): File? {
    var dir: File? = startDir.absoluteFile
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").exists() || File(dir, "data").exists()) {
            return dir
        }
        dir = dir.parentFile
    }
    return null
}

fun main() {
    populateFoodDatabase()
}
