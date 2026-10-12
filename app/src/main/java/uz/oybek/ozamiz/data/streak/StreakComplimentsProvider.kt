package uz.oybek.ozamiz.data.streak

// import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.info.Gender

object StreakComplimentsProvider {

    fun getComplimentForStreak(streakDays: Int): String {
        // val user = UserProfileRepository.user
        val user = LocalUser.user
        val name = user.name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: ""
        val isMale = user.gender == Gender.MALE

        return if (isMale) {
            getMaleCompliment(name, streakDays)
        } else {
            getFemaleCompliment(name, streakDays)
        }
    }

    private fun getMaleCompliment(name: String, days: Int): String {
        val prefix = if (name.isNotBlank()) "$name, " else ""
        val list = when {
            days == 1 -> listOf(
                "${prefix}birinchi qadam — eng qiyin qadam. Uddadingiz! 💪",
                "${prefix}zo'r boshlanish! Maqsadingizga qarab keting! 🎯",
                "${prefix}bugun sog'lom hayot yo'lini tanladingiz. Davom eting! 🔥",
                "${prefix}kuchli odamlar shu yerdan boshlaydi. Oldinga! 🏆",
                "${prefix}bugungi qadam ertangi natijangiz! Zo'rsiz! ⚡",
                "${prefix}qodir odamlar shunday qilib boshlaydi. Tabriklaymiz! 💯",
                "${prefix}birinchi kun — bu irodaning belgisi! 🎖️",
                "${prefix}maqsadingiz aniq, yo'lingiz to'g'ri. Boshlang! 🚀",
                "${prefix}bugun qaror qildingiz — bu katta qadamdir! 💪",
                "${prefix}hammasi bitta qadam bilan boshlanadi. Siz boshladingiz! 🔥"
            )
            days == 2 -> listOf(
                "${prefix}ikkinchi kun ham keldingiz — bu intizomning belgisi! 💪",
                "${prefix}ikkita kun ketma-ket. Siz to'xtatib bo'lmaydigansiz! 🔥",
                "${prefix}kuch — bu har kuni qayta tanlashdir. Siz tanladingiz! 🎯",
                "${prefix}zo'r! Davom eting, natija yaqin! ⚡",
                "${prefix}har kun qaytib kelish — mardlikning belgisi! 🏆",
                "${prefix}ikkita kun — bu oddiy odam qilmaydigan ish! 💯",
                "${prefix}intizomingiz kuchayib bormoqda! Davom eting! 🎖️",
                "${prefix}siz to'g'ri yo'ldasiz — to'xtamang! 🚀",
                "${prefix}har kuni o'zini majburlash qiyin — siz uddalayapsiz! 💪",
                "${prefix}zo'r natijalarga zo'r odatlar olib keladi! 🔥"
            )
            days == 3 -> listOf(
                "${prefix}uch kun — bu allaqachon odat bo'la boshladi! 🔥",
                "${prefix}uch kunlik ketma-ketlik — bu kuchli odam qiladi! 💪",
                "${prefix}siz odatni shakllantiryapsiz — maqsadga yaqinlashyapsiz! 🎯",
                "${prefix}uch kun ketma-ket — qoyil! Siz uddalayapsiz! 🏆",
                "${prefix}natijalar ko'rinishiga sal qoldi, ${prefix}davom eting! ⚡",
                "${prefix}uch kun — bu iroda! Zo'rsiz! 💯",
                "${prefix}har kuni o'sib borayapsiz — ko'rinmasa ham! 🎖️",
                "${prefix}uch kunlik g'alaba muborak! Oldinga! 🚀",
                "${prefix}siz to'g'ri yo'lda — orqaga qaramang! 💪",
                "${prefix}uch kun bilan faqat boshlanish. Davom eting! 🔥"
            )
            days in 4..6 -> listOf(
                "${prefix}bir haftaga yaqinlashyapsiz — to'xtamang! 🔥",
                "${prefix}kundan-kunga kuchlanib bormoqdasiz! 💪",
                "${prefix}haftaga yaqin ketma-ketlik — zo'r natija! 🏆",
                "${prefix}maqsadingizga yaqinlashyapsiz — davom eting! 🎯",
                "${prefix}siz odatingizni mustahkamlamoqdasiz! ⚡",
                "${prefix}kuchli odamlar shu yo'lni tanlaydi — xuddi siz! 💯",
                "${prefix}haftalik maqsad oldida bir qadam qoldi! 🎖️",
                "${prefix}zo'r! Haftani yakunlang va yangi rekord qo'ying! 🚀",
                "${prefix}iroda — bu har kuni tanlov. Siz to'g'ri tanlamoqdasiz! 💪",
                "${prefix}hafta tugashidan oldin to'xtamang! 🔥"
            )
            days % 7 == 0 -> listOf(
                "${prefix}to'liq hafta! Siz ajoyib natijaga erishdingiz! 🏆",
                "${prefix}haftalik g'alaba muborak! Siz zo'rsiz! 🎉",
                "${prefix}hafta tamom — iroda va intizomingiz kuchli! 💪",
                "${prefix}zo'r! Yana bir hafta — yana bir zafar! 🔥",
                "${prefix}haqiqiy natijalar sabr va intizomdan keladi — siz qildingi! 🎯",
                "${prefix}haftalik rekord! Davom etavering! ⚡",
                "${prefix}siz faqat so'z bilan emas, amal bilan isbotlayapsiz! 💯",
                "${prefix}bu hafta muvaffaqiyatli o'tdi — keyingisi ham shunday bo'lsin! 🎖️",
                "${prefix}kuchli odamlar shu yerda — to'xtamang! 🚀",
                "${prefix}hafta tugadi, yangi maqsad boshlang! 🏆"
            )
            else -> listOf(
                "${prefix}ketma-ketligingiz davom etmoqda — zo'rsiz! 🔥",
                "${prefix}har kun shu yerda bo'lganligi uchun tabriklaymiz! 💪",
                "${prefix}siz o'z so'zingizda turyapsiz! 🎯",
                "${prefix}intizomingiz kuchayib bormoqda! 🏆",
                "${prefix}bunday iroda kamdan-kam odamda bo'ladi! ⚡",
                "${prefix}zo'r! Natijalar o'zlari kelib qoladi! 💯",
                "${prefix}har kun bir qadam — natija muqarrar! 🎖️",
                "${prefix}siz to'g'ri yo'ldasiz — davom eting! 🚀",
                "${prefix}har kun kelib turish — bu mardlikning belgisi! 💪",
                "${prefix}ketma-ketligingiz davom etsin! 🔥"
            )
        }
        return list.random()
    }

    private fun getFemaleCompliment(name: String, days: Int): String {
        val prefix = if (name.isNotBlank()) "$name, " else ""
        val list = when {
            days == 1 -> listOf(
                "${prefix}bugun o'zingiz uchun zo'r qadam tashladingiz! 🌸",
                "${prefix}birinchi qadam — eng muhim qadam. Tabriklaymiz! ✨",
                "${prefix}o'zingizni tanlash — bu kuch! Davom eting! 💖",
                "${prefix}zo'r boshlanish! Sog'lom hayot shu yerdan boshlanadi! 🌷",
                "${prefix}bugungi harakatingiz ertangi natijangiz! 🌟",
                "${prefix}ajoyib! Siz o'z salomatingizga e'tibor berdingiz! 💐",
                "${prefix}sog'lom hayot yo'lini tanladingiz — bu juda chiroyli! 💖",
                "${prefix}birinchi qadam — hamma narsa shu yerdan boshlanadi! 🌸",
                "${prefix}bugun o'zingizga g'amxo'rlik qildingiz — davom eting! ✨",
                "${prefix}zo'r! Maqsadingizga qarab intiling! 🌷"
            )
            days == 2 -> listOf(
                "${prefix}ikkinchi kun ham keldingiz — zo'r! 🌸",
                "${prefix}davom etyapsiz — bu ajoyib! ✨",
                "${prefix}ikki kun ketma-ket — bu intizomning belgisi! 💖",
                "${prefix}siz to'g'ri yo'lda, davom eting! 🌟",
                "${prefix}har kuni o'zingizni tanlash — bu kuch! 💐",
                "${prefix}zo'r! Natija yaqin, to'xtamang! 🌷",
                "${prefix}ikki kunlik ketma-ketlik — tabriklaymiz! 🌸",
                "${prefix}siz odatni shakllantiryapsiz — zo'r! ✨",
                "${prefix}har kuni bu yerga kelish — bu sabotning belgisi! 💖",
                "${prefix}ikkinchi kun — davom eting! 🌟"
            )
            days == 3 -> listOf(
                "${prefix}uch kun ketma-ket — zo'r natija! 🌸",
                "${prefix}uch kun bilan odat shakllanib bormoqda! ✨",
                "${prefix}siz maqsadingizga yaqinlashyapsiz! 💖",
                "${prefix}uch kunlik ketma-ketlik — tabriklaymiz! 🌟",
                "${prefix}zo'r! Siz uddalayapsiz! 💐",
                "${prefix}uch kun — bu faqat boshlash! Davom eting! 🌷",
                "${prefix}natijalar ko'rinishiga oz qoldi! 🌸",
                "${prefix}uch kunlik g'alaba muborak! ✨",
                "${prefix}siz to'g'ri yo'lda — orqaga qaramang! 💖",
                "${prefix}uch kun — bu iroda! Zo'rsiz! 🌟"
            )
            days in 4..6 -> listOf(
                "${prefix}haftaga yaqinlashyapsiz — zo'r! 🌸",
                "${prefix}kundan-kunga o'sib bormoqdasiz! ✨",
                "${prefix}haftalik maqsad oldida bir qadam! 💖",
                "${prefix}siz odatingizni mustahkamlamoqdasiz! 🌟",
                "${prefix}zo'r! Haftani yakunlang! 💐",
                "${prefix}intizomingiz kuchayib bormoqda! 🌷",
                "${prefix}to'xtamang — natijalar shu yerda boshlanadi! 🌸",
                "${prefix}maqsadingizga yaqinlashyapsiz! ✨",
                "${prefix}zo'r! Davom etavering! 💖",
                "${prefix}hafta tugashidan oldin to'xtamang! 🌟"
            )
            days % 7 == 0 -> listOf(
                "${prefix}to'liq hafta — zo'r natija! 🎉🌸",
                "${prefix}haftalik g'alaba muborak! ✨",
                "${prefix}hafta tamom — sabr va intizomingiz kuchli! 💖",
                "${prefix}zo'r! Yana bir hafta — yana bir zafar! 🌟",
                "${prefix}siz maqsadingizga bir qadam yaqinlashdingiz! 💐",
                "${prefix}haftalik rekord — tabriklaymiz! 🌷",
                "${prefix}siz so'z bilan emas, amal bilan isbotlayapsiz! 🌸",
                "${prefix}bu hafta muvaffaqiyatli o'tdi! ✨",
                "${prefix}zo'r! Keyingi hafta ham shunday bo'lsin! 💖",
                "${prefix}hafta tugadi, yangi maqsad boshlang! 🌟"
            )
            else -> listOf(
                "${prefix}ketma-ketligingiz davom etmoqda — zo'rsiz! 🌸",
                "${prefix}har kun shu yerda bo'lganligi uchun tabriklaymiz! ✨",
                "${prefix}siz o'z so'zingizda turyapsiz! 💖",
                "${prefix}intizomingiz kuchayib bormoqda! 🌟",
                "${prefix}zo'r! Natijalar o'zlari kelib qoladi! 💐",
                "${prefix}har kun bir qadam — natija muqarrar! 🌷",
                "${prefix}siz to'g'ri yo'ldasiz — davom eting! 🌸",
                "${prefix}zo'r! Davom etavering! ✨",
                "${prefix}har kun kelib turish — sabotning belgisi! 💖",
                "${prefix}ketma-ketligingiz davom etsin! 🌟"
            )
        }
        return list.random()
    }
}
