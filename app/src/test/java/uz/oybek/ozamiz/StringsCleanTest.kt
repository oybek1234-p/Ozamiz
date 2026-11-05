package uz.oybek.ozamiz

import org.junit.Test
import java.io.File
//
//class StringsCleanTest {
//
//    @Test
//    fun cleanStringsXmlEncoding() {
//        val file = File("D:/Projects/Ozamiz/app/src/main/res/values/strings.xml")
//        if (!file.exists()) return
//
//        var content = file.readText(Charsets.UTF_8)
//
//        val replacements = mapOf(
//            "â€˜" to "'",
//            "â€™" to "'",
//            "â€“" to "–",
//            "â€”" to "—",
//            "â€¢" to "•",
//            "âœ”" to "✔",
//            "Â©" to "©",
//            "ðŸ¥¤" to "🥤",
//            "ðŸ¥š" to "🥚",
//            "ðŸ¥¦" to "🥦",
//            "ðŸ§" to "🍩",
//            "âŒ" to "❌",
//            "ðŸ’¤" to "🛌",
//            "ðŸ¥—" to "🥗",
//            "ðŸŒŸ" to "🌟",
//            "ðŸ’§" to "💧",
//            "ðŸ’ª" to "💪",
//            "ðŸ”¥" to "🔥",
//            "âœ¨" to "✨",
//            "ðŸ§" to "🧠",
//            "ðŸ”‹" to "🔋",
//            "ðŸ¦·" to "🦷",
//            "ðŸŒ¡ï¸" to "🌡️",
//            "ðŸŒ¡" to "🌡️",
//            "ðŸ§©" to "🧩",
//            "ðŸ›¡ï¸" to "🛡️",
//            "ðŸ›¡" to "🛡️",
//            "ðŸŒŠ" to "🌊",
//            "âš¡" to "⚡",
//            "â¤ï¸" to "❤️",
//            "â¤" to "❤️",
//            "ðŸ¦´" to "🦴",
//            "ðŸ§˜" to "🧘",
//            "ðŸ©¸" to "🩺",
//            "ðŸƒ" to "🏃",
//            "ðŸ§¬" to "🧬",
//            "ðŸ" to "🍏",
//            "ðŸ¥’" to "🥒",
//            "ðŸ¥›" to "🥛",
//            "ðŸµ" to "🍵",
//            "ðŸ¥¬" to "🥬",
//            "ðŸŒ¾" to "🌾",
//            "ðŸš¶" to "🚶",
//            "ðŸŽ‰" to "🎉",
//            "ðŸŽ¯" to "🎯",
//            "ðŸ“‰" to "📉",
//            "ðŸƒ" to "🌲",
//            "ðŸš€" to "🚀",
//            "ðŸ†" to "🏆",
//            "ðŸ‘Œ" to "👌",
//            "ðŸ‘" to "👍",
//            "ðŸ‘" to "👏",
//            "ðŸ½ï¸" to "🍽️",
//            "ðŸ½" to "🍽️",
//            "ðŸ“±" to "📱",
//            "ðŸ”’" to "🔒",
//            "âœ“" to "✓",
//            "â€“" to "–",
//            "â€”" to "—",
//            "â€¢" to "•",
//            "â€˜" to "‘",
//            "â€™" to "’",
//            "â€œ" to "“",
//            "â€" to "”"
//        )
//
//        for ((old, new) in replacements) {
//            content = content.replace(old, new)
//        }
//
//        file.writeText(content, Charsets.UTF_8)
//        println("strings.xml successfully cleaned of mojibake encoding issues!")
//    }
//}
