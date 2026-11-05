package extractor

import java.io.File

data class FoodFullLog(
    val userQuestion: String,
    val aiResponse: String
)

/**
 * foods.csv faylidan barcha yoki belgilangan miqdordagi (limit)
 * User savollari va unga berilgan AI javoblarini to'liq tozalab ajratib oladi.
 *
 * @param inputFile Manba CSV fayli (default: "extractor/foods.csv")
 * @param outputFile Natijaviy to'liq fayl (default: "extractor/foodsOutputFull.txt")
 * @param limit Nechta yozuv chiqarilishi (agar null yoki <= 0 bo'lsa barchasi yoziladi)
 */
fun extractFoodsFull(
    inputFile: String = "extractor/foods.csv",
    outputFile: String = "extractor/foodsOutputFull.txt",
    limit: Int? = null
): List<FoodFullLog> {
    val input = resolveFile(inputFile)
    println("Qidirilayotgan input fayl: ${input.absolutePath}")
    if (!input.exists()) {
        println("Input file $inputFile not found at ${input.absolutePath}")
        return emptyList()
    }

    val content = input.readText(Charsets.UTF_8)
    val logMarker = Regex("""'\[analyzeFood(?:Ver2)?\] Log:\s*\{""")
    val matches = logMarker.findAll(content).toList()
    println("Topilgan [analyzeFood] log bloklari soni: ${matches.size}")

    val entries = mutableListOf<FoodFullLog>()
    val descRegex = Regex("""description:\s*'((?:[^'\\]|\\.)*)'""")
    val closeRegex = Regex("""\n\}[\r\n]+","'""")

    for (i in matches.indices) {
        val start = matches[i].range.last + 1
        val end = if (i + 1 < matches.size) matches[i + 1].range.first else content.length
        val rawBlock = content.substring(start, end)
        val closeMatch = closeRegex.find(rawBlock)
        val block = if (closeMatch != null) rawBlock.substring(0, closeMatch.range.first) else rawBlock

        val descMatch = descRegex.find(block)
        val userQuestion = descMatch?.groupValues?.get(1)?.unescapeJs()?.trim()

        if (userQuestion.isNullOrBlank()) continue

        val aiIndex = block.indexOf("ai:")
        val aiResponse = if (aiIndex != -1) {
            cleanAiResponse(block.substring(aiIndex + 3).trim())
        } else {
            ""
        }

        if (aiResponse.isNotBlank()) {
            entries.add(FoodFullLog(userQuestion = userQuestion, aiResponse = aiResponse))
            if (limit != null && limit > 0 && entries.size >= limit) {
                break
            }
        }
    }

    val output = resolveFile(outputFile)
    output.parentFile?.mkdirs()

    val outputText = buildString {
        entries.forEachIndexed { index, entry ->
            appendLine("=".repeat(80))
            appendLine("# ${index + 1}")
            appendLine("USER:")
            appendLine(entry.userQuestion)
            appendLine()
            appendLine("AI:")
            appendLine(entry.aiResponse)
            appendLine()
        }
    }
    output.writeText(outputText, Charsets.UTF_8)

    println(">>> MUVAFFAQIYATLI SAQLANDI: ${entries.size} ta log -> ${output.absolutePath}")
    return entries
}

private fun resolveFile(path: String): File {
    val direct = File(path)
    if (direct.exists()) return direct.absoluteFile

    var dir: File? = File(".").absoluteFile
    while (dir != null) {
        val candidate = File(dir, path)
        if (candidate.exists()) return candidate.absoluteFile
        if (File(dir, "settings.gradle.kts").exists()) {
            return candidate.absoluteFile
        }
        dir = dir.parentFile
    }
    return direct.absoluteFile
}

private fun cleanAiResponse(rawAi: String): String {
    val lines = rawAi.lines()
    val sb = StringBuilder()
    for (line in lines) {
        var trimmed = line.trim()
        if (trimmed.endsWith("+")) {
            trimmed = trimmed.dropLast(1).trim()
        }
        if ((trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length >= 2) ||
            (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length >= 2)
        ) {
            trimmed = trimmed.substring(1, trimmed.length - 1)
        }
        sb.append(trimmed)
    }
    return sb.toString()
        .replace("\"\"", "\"")
        .replace("\\n", "\n")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .trim()
}

private fun String.unescapeJs(): String {
    return this
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}

fun extractFoodDescriptions(
    inputFile: String = "extractor/foods.csv",
    outputFile: String = "extractor/foodsOutput.txt"
) {
    val input = resolveFile(inputFile)
    if (!input.exists()) {
        println("Input file $inputFile not found.")
        return
    }

    val content = input.readText(Charsets.UTF_8)
    val regex = "description:\\s*'([^']*)'".toRegex()
    val descriptions = regex.findAll(content).map { it.groupValues[1] }.toList()

    val output = resolveFile(outputFile)
    output.parentFile?.mkdirs()
    output.writeText(descriptions.joinToString("\n"), Charsets.UTF_8)

    println("Extracted ${descriptions.size} descriptions from ${input.absolutePath} to ${output.absolutePath}")
}

fun main(args: Array<String>) {
    val limit = args.firstOrNull()?.toIntOrNull()
    extractFoodsFull(limit = limit)
}
