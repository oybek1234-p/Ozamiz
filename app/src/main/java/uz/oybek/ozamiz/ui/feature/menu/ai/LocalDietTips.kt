package uz.oybek.ozamiz.ui.feature.menu.ai

object LocalDietTips {

    fun getAll(): List<AiTip> = listOf(
        AiTip(
            title = "Ko'proq suv iching",
            description = "Kuniga kamida 1.5–2 litr suv iching. Ovqat oldidan 1 stakan suv ichsangiz, kamroq eyasiz.",
            icon = "water"
        ),
        AiTip(
            title = "Ovqat vaqtini saqlang",
            description = "Har kuni bir xil vaqtda ovqatlanish metabolizmni yaxshilaydi va ochlik his qilishni kamaytiradi.",
            icon = "clock"
        ),
        AiTip(
            title = "Harakat qiling",
            description = "Kuniga 30 daqiqa yurish yoki engil jismoniy mashq kaloriya yoqishni tezlashtiradi.",
            icon = "walk"
        ),
        AiTip(
            title = "Sekin yeng",
            description = "Ovqatni yaxshilab chaynamoq to'yinganlik his qilishni tezroq keltirib chiqaradi.",
            icon = "food"
        ),
        AiTip(
            title = "Kech ovqatlanmang",
            description = "Soat 20:00 dan keyin ovqat yemang. Agar och bo'lsangiz, bir stakan qatiq yoki olma yeng.",
            icon = "moon"
        ),
        AiTip(
            title = "Porsiyani nazorat qiling",
            description = "Kichik idishdan ovqatlaning. Ko'z bilan porsiyani o'lchashni o'rganing.",
            icon = "ruler"
        )
    )
}
