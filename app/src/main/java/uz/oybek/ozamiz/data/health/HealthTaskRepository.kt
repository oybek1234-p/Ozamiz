package uz.oybek.ozamiz.data.health

import uz.oybek.ozamiz.data.health.model.HealthTask

object HealthTaskRepository {

    val tasks = listOf(

        // --- ICHIMLIKLAR VA SUV (1-20) ---
        HealthTask(
            "ht_1",
            "💧",
            "Ovqatlanishdan 20 daqiqa oldin ikki stakan toza suv iching",
            "Oshqozonni oldindan to'ldiradi va ishtahani sezilarli darajada pasaytiradi",
            "Bu vazifaning asosiy maqsadi — miyani aldashdir. Oshqozon devorlari suv bilan kerilganda, miyaga 'men to'lyapman' degan signal boradi. Natijada, asosiy ovqat kelganida siz odatdagidan ancha kamroq yeb to'yasiz. Shuningdek, suv modda almashinuvini 24 foizga tezlashtirishi ilmiy isbotlangan.",
            1
        ),
        HealthTask(
            "ht_2",
            "🌅",
            "Ertalab uyg'onishingiz bilan bir stakan iliq suv iching",
            "Butun tanani uyg'otadi va tunda to'plangan toksinlarni haydaydi",
            "Tun davomida organizm tiklanish jarayonida ko'plab chiqindilarni hosil qiladi. Ertalabki iliq suv ichaklarni 'yuvib' ishga tushiradi. Bu sizga kun bo'yi yengillik hissini beradi va hazm qilish jarayonini 15 foizga tezroq boshlanishiga yordam beradi.",
            1
        ),
        HealthTask(
            "ht_3",
            "🥄",
            "Choyga soladigan shakaringizni 2 barobar kamaytiring",
            "Qonda insulin darajasini tushiradi va yog' yoqishni boshlaydi",
            "Siz kuniga 10-12 qoshiq shakar iste'mol qilasiz. Bu juda ko'p. Shakar qonda insulin gormonini ko'tarib yuboradi, insulin esa yog'ni eritishga yo'l qo'ymaydi. Bugun faqat bir qoshiq soling, shunda tanangiz zaxiradagi yog'larni energiya sifatida ishlata boshlaydi.",
            1
        ),
        HealthTask(
            "ht_4",
            "🍵",
            "Tushlikdan so'ng bir piyola shakarsiz ko'k choy iching",
            "Yog'larni parchalash jarayonini (lipoliz) tezlashtiradi",
            "Ko'k choy tarkibida 'katexinlar' degan modda bor. Ular tanada yog'larni parchalanishiga yordam beradi. Ovqatdan keyin issiq holda ichish hazmni yengillashtiradi va qorin sohasidagi shishlarni yo'qotadi.",
            2
        ),
        HealthTask(
            "ht_5",
            "🍋",
            "Shakarsiz limon choy iching",
            "Ishtahani nazorat qiladi va jigar faoliyatini yaxshilaydi",
            "Limonli suv nafaqat mazali, balki u qondagi ishqoriy muhitni ushlab turadi. Bu esa ortiqcha ovqat yeyish xohishini o'ldiradi. Shuningdek, limon tarkibidagi S vitamini yog'larning oksidlanishiga yordam beradi.",
            1
        ),
        HealthTask(
            "ht_6",
            "🚫🥤",
            "Bugun bitta ham gazli yoki shirin ichimlik ichmang",
            "Jigarni ortiqcha yuklamadan va yog' bosishidan asraydi",
            "Gazli ichimliklarda 'suyuq shakar' bor. U qonga juda tez so'riladi va jigar uni qayta ishlashga ulgurmay, darhol qoringa yog' qilib yig'adi. Bugun faqat suv va oddiy choy bilan kifoyalaning.",
            3
        ),
        HealthTask(
            "ht_7",
            "🥛",
            "Uxlashdan bir soat oldin bir piyola qatiq yoki kefir iching",
            "Ichak mikroflorasini tiklaydi va hazmni yengillashtiradi",
            "Qatiq tarkibidagi foydali bakteriyalar ichaklarni tozalaydi. Bu ertasi kuni ertalab qorin dam bo'lmasligini va yengil uyg'onishingizni ta'minlaydi. Shuningdek, qatiq tunda ochlik hissi qiynashining oldini oladi.",
            2
        ),
        HealthTask(
            "ht_8",
            "☕",
            "Kofeni sut va shakarsiz, faqat qora holda iching",
            "Metabolizmni va quvvat sarfini 10 foizga oshiradi",
            "Sut va shakar kofening foydasini yo'q qiladi. Toza qora kofe esa adrenalin ishlab chiqarishni ko'paytiradi, bu esa yog' hujayralarini parchalab, qonga energiya sifatida chiqarishga yordam beradi.",
            3
        ),
        HealthTask(
            "ht_9",
            "🧊",
            "Muzdek suvdan voz keching va faqat iliq suv iching",
            "Yog'larning qotib qolishini oldini oladi",
            "Ovqat paytida yoki undan keyin muzdek suv ichsangiz, yegan yog'li ovqatlaringiz oshqozonda qotib qoladi va hazm bo'lishi qiyinlashadi. Iliq suv esa yog'larni erigan holda saqlaydi va ularni tana osonroq chiqarib yuboradi.",
            1
        ),
        HealthTask(
            "ht_10",
            "🌿",
            "Choyingizga ozgina yangi zanjabil (imbir) qo'shib iching",
            "Tana haroratini biroz ko'tarib, kaloriyalarni yoqadi",
            "Zanjabil — bu tabiiy 'isituvchi' modda. U qon aylanishini tezlashtiradi va tanani ichkaridan isitadi. Tana bu issiqlikni sovutish uchun qo'shimcha energiya sarflaydi, natijada siz o'tirgan joyingizda ko'proq kaloriya yoqasiz.",
            3
        ),
        HealthTask(
            "ht_11",
            "🍃",
            "Kechqurun ochlik qiynasa, yalpizli choy iching",
            "Miyaga tinchlik beradi va ishtahani jilovlaydi",
            "Yalpiz hidi va ta'mi asab tizimini tinchlantiradi. Ko'pincha biz stress tufayli ovqat yeymiz. Yalpiz esa bu hissiyotni bosadi va sizga tunda ortiqcha narsalar yeb qo'ymaslikka yordam beradi.",
            2
        ),
        HealthTask(
            "ht_12",
            "💧",
            "Har soatda kamida uch-to'rt qultum suv iching",
            "Tanada suv yetishmasligi (degidratatsiya)ni oldini oladi",
            "Tana suvsiz qolsa, barcha jarayonlar, jumladan yog' yoqish ham to'xtaydi. Suvni bir marta ko'p ichgandan ko'ra, kun bo'yi oz-ozdan ichib yurish metabolizmni doimo yuqori darajada ushlab turadi.",
            1
        ),
        HealthTask(
            "ht_13",
            "🥛",
            "Qatiqqa ozgina dolchin (koritsa) seping va iching",
            "Qondagi qand miqdorini turg'unlashtiradi",
            "Dolchin shakar xohishini o'ldiradi. Agar sizga doim shirinlik yetishmayotgan bo'lsa, bu vazifa sizga yordam beradi. U insulin sakrashini oldini olib, tana yog' yig'ishini to'xtatadi.",
            2
        ),
        HealthTask(
            "ht_14",
            "🍼",
            "O'zingiz bilan 1 litrlik suv idishi olib yuring",
            "Kunlik suv normasini nazorat qilishni osonlashtiradi",
            "Inson kunda qancha suv ichganini unuti qo'yadi. Idish bo'lsa, siz aniq ko'rib turasiz. Kun oxirigacha idishni kamida ikki marta bo'shatishga harakat qiling, bu sizning ozishingiz uchun minimal talab.",
            1
        ),
        HealthTask(
            "ht_15",
            "🥤",
            "Meva sharbatlari o'rniga faqat toza suv iching",
            "Keraksiz 'suyuq' kaloriyalardan qutulish imkonini beradi",
            "Hatto uyda siqilgan sharbatda ham kletchatka yo'q, faqat shakar bor. Bir stakan sharbatda 3-4 ta mevaning shakari bo'ladi, buni ichish semirishga to'g'ridan-to'g'ri yo'ldir. Suv esa nol kaloriya.",
            3
        ),

        // --- OVQATLANISH MADANIYATI (21-50) ---
        HealthTask(
            "ht_16",
            "🍽️",
            "Bugun har bir ovqatlanishda likopchangizning 30 foizini qoldiring",
            "Oshqozon hajmini tabiiy ravishda kichraytirishga yordam beradi",
            "Siz o'rgangan porsiya hozirgi 80 kg vazningizni ushlab turish uchun mo'ljallangan. 70 kg bo'lish uchun kamroq yeyishingiz shart. Ovqatni 30 foizini ataylab yemang, shunda tana yetishmayotgan energiyani o'z yog'laridan ola boshlaydi.",
            1
        ),
        HealthTask(
            "ht_17",
            "🧘",
            "Har bir luqmani kamida 20 marta chaynab yuting",
            "Miyaga to'yish signali yetib borishi uchun vaqt beradi",
            "Odam oshqozoni emas, miyasi bilan to'yadi. Miya to'yganini bilishi uchun 20 daqiqa kerak. Agar tez yeb qo'ysangiz, miya ulgurmaydi va siz ortiqcha ovqat yeb yuborasiz. Sekin chaynash hazmni ham 2 barobar yaxshilaydi.",
            1
        ),
        HealthTask(
            "ht_18",
            "📱",
            "Ovqatlanish vaqtida telefonni boshqa xonaga qo'ying",
            "Diqqatni taomga qaratib, ortiqcha yeyishdan saqlaydi",
            "Telefonga qarab ovqat yeganda, inson qancha yeganini sezmay qoladi. Diqqat taomda bo'lsa, tanangiz qachon to'yganini sizga aniq bildiradi. Bu 'ongli ovqatlanish' deb ataladi va vazn tashlashda juda muhim.",
            2
        ),
        HealthTask(
            "ht_19",
            "🥗",
            "Ovqatni har doim yangi sabzavotli salatdan boshlang",
            "Oshqozonni foydali kletchatka bilan to'ldirib oladi",
            "Oldin salat yeng, keyin asosiy ovqatga o'ting. Sabzavotlar oshqozonda joy egallaydi va asosiy (kaloriyali) ovqatga kamroq joy qoladi. Shuningdek, kletchatka yog'larning qonga so'rilishini sekinlashtiradi.",
            2
        ),
        HealthTask(
            "ht_20",
            "🥨",
            "Ovqatlar orasida og'zingizni 'dam oldiring'",
            "Yog'larni yoqish uchun mas'ul gormonlarni faollashtiradi",
            "Har safar biron narsa yeganingizda (hatto bitta pechenye bo'lsa ham), tana yog' yoqishni to'xtatadi va ovqatni hazm qilishga o'tadi. Ovqatlar orasida kamida 4 soat hech narsa yemang, shunda tana o'z yog'ini yoqa boshlaydi.",
            3
        ),
        HealthTask(
            "ht_21",
            "🥚",
            "Nonushtaga faqat oqsilga boy mahsulotlar yeng",
            "Kun davomida ochlik hissini minimal darajaga tushiradi",
            "Ertalab 2 ta tuxum yoki tvorog yeng. Oqsillar uzoq vaqt hazm bo'ladi va siz tushlikkacha hech qanday 'perekus'larsiz bemalol yura olasiz. Bu shirinlikka bo'lgan ehtiyojni ham kamaytiradi.",
            3
        ),
        HealthTask(
            "ht_22",
            "🛑",
            "Qorningiz to'yganini his qilishingiz bilan ovqatlanishni to'xtating",
            "Oshqozonni keraksiz bosim va cho'zilishdan asraydi",
            "Likopchangizda ovqat qolsa ham, uni tugatishga o'zingizni majburlamang. 'Uvol bo'ladi' deb yeyish — o'z sog'lig'ingizga xiyonat qilishdir. To'ydingizmi — tamom, stol ustidan turing.",
            2
        ),
        HealthTask(
            "ht_23",
            "🧺",
            "Ovqatni odatdagidan kichikroq idishda (tarelka) yeng",
            "Ko'zni aldash orqali psixologik to'qlik hissini beradi",
            "Katta tarelkada ozgina ovqat ko'rinsa, odam o'zini och his qiladi. Kichik idishda esa o'sha ovqat to'la ko'rinadi. Bu oddiy psixologik hiyla sizga kamroq kaloriya iste'mol qilishga yordam beradi.",
            1
        ),
        HealthTask(
            "ht_24",
            "🧼",
            "Kechki ovqatdan keyin darhol tishingizni yuving",
            "Kechki 'yana bir nima yeb qo'yish' xohishini o'ldiradi",
            "Tishingizni yuvib bo'lgach, og'izdagi yalpiz ta'mi va tozalik hissi sizni ovqatlanishdan to'xtatadi. Bu miyaga 'ovqatlanish jarayoni bugunga tugadi' degan buyruqni beradi.",
            2
        ),
        HealthTask(
            "ht_25",
            "🥣",
            "Kuniga bir mahal albatta suyuq ovqat (sho'rva) iching",
            "Past kaloriya bilan yuqori to'qlik hissini beradi",
            "Sho'rvalarda suv miqdori ko'p, kaloriya esa kam. Ular oshqozonni yaxshi to'ldiradi va hazm qilish tizimini yumshatadi. Bu sizga ortiqcha vazndan qutulishni ancha osonlashtiradi.",
            2
        ),
        HealthTask(
            "ht_26",
            "🍎",
            "Shirinlik yegingiz kelganida bitta butun olma yeng",
            "Qand miqdorini kletchatka orqali sekin ko'taradi",
            "Olma tarkibidagi 'pektin' moddasi ishtahani bo'g'adi. Konfet yoki pechenye yeyish o'rniga olma yeng — bu nafaqat vitamin, balki ozish uchun ajoyib quroldir.",
            2
        ),
        HealthTask(
            "ht_27",
            "🧂",
            "Ovqatlaringizga qo'shiladigan tuzni bugun ikki barobar kamaytiring",
            "Tanadan ortiqcha suyuqlikni chiqarib, shishlarni yo'qotadi",
            "Tuz tanada suvni ushlab qoladi. Agar siz ko'p tuz yesangiz, tarozi ko'rsatkichining yarmi shunchaki suv bo'lishi mumkin. Tuzni kamaytirsangiz, birinchi haftadayoq 1-2 kg 'suvli vazn' yo'qoladi.",
            2
        ),
        HealthTask(
            "ht_28",
            "🍳",
            "Ovqatni qovurish o'rniga dimlab yoki qaynatib pishiring",
            "Zararli 'trans-yog'lar' va ortiqcha kaloriyadan xalos qiladi",
            "Qovurishda ishlatiladigan yog' ovqatning kaloriyasini 3 barobar oshirib yuboradi. Dimlangan ovqat esa o'zining foydali xususiyatlarini saqlab qoladi va sizni semirtirmaydi.",
            4
        ),
        HealthTask(
            "ht_30",
            "⏲️",
            "Kechki soat 19:00 dan keyin oshxonaga kirmang",
            "Tunda tanaga yog'larni eritish uchun imkoniyat beradi",
            "Kechqurun yeyilgan ovqat energiya sifatida ishlatilmaydi, chunki siz uxlaysiz. U to'g'ridan-to'g'ri zahiraga (yog'ga) aylanadi. Ertaroq ovqatlanish — bu 10 kg dan qutulishning eng qisqa yo'li.",
            5
        ),
        HealthTask(
            "ht_37",
            "🥗",
            "Bugun ovqatingizga albatta ko'katlar (kashnich, ukrop, rayhon) qo'shing",
            "Tana kislotaliligini kamaytiradi va vitaminlar bilan to'yintiradi",
            "Ko'katlar — bu dunyodagi eng kuchli ishqoriy mahsulotlar. Ular qondagi yallig'lanishni kamaytiradi va metabolizmni tabiiy ravishda tezlashtiradi. Kletchatkaga boy bo'lgani uchun qorinni ham to'q tutadi.",
            1
        ),
        HealthTask(
            "ht_38",
            "🥜",
            "Qovurilmagan yong'oq yoki bodomdan 5-6 dona iste'mol qiling",
            "Foydali yog'lar bilan miya va yurak faoliyatini qo'llab-quvvatlaydi",
            "Yong'oqlar o'rnini bosib bo'lmas omega-3 va omega-6 yog' kislotalariga boy. Ular 'yomon' xolesterinni tushiradi. Faqat qovurilmaganini tanlang, chunki issiqlik foydali yog'larni zararli trans-yog'larga aylantiradi.",
            2
        ),
        HealthTask(
            "ht_39",
            "🧂",
            "Tuz donachalari o'rniga ovqatga ko'proq tabiiy ziravorlar qo'shing",
            "Ishtahani tabiiy boshqarish va ta'mni kuchaytirish",
            "Zira, murch, kashnich urug'i kabi ziravorlar ovqatni mazali qiladi va tuzga bo'lgan ehtiyojni kamaytiradi. Tuz cheklanganda tana o'zida suv ushlab turmaydi, bu esa darhol vazn tashlashga yordam beradi.",
            2
        ),
        HealthTask(
            "ht_40",
            "🍳",
            "Teflon yoki yopishmaydigan tovadan foydalanib, yog'siz tuxum pishiring",
            "Ortiqcha qovurish yog'laridan qutulish",
            "Bir osh qoshiq o'simlik yog'i 120 kaloriya. Agar yog'siz pishirishni o'rgansangiz, kuniga 300-400 kaloriyani shunchaki yo'qotgan bo'lasiz. Bu oyiga 1.5 kg sof yog' yo'qotish demakdir.",
            3
        ),
        HealthTask(
            "ht_41",
            "🍵",
            "Choy o'rniga moychechak (romashka) damlamasini iching",
            "Asablarni tinchlantiradi va ovqat hazmini yaxshilaydi",
            "Moychechak damlamasi oshqozon-ichak traktidagi spazmlarni yo'qotadi va sizga tunda xotirjam uyqu beradi. Stress gormoni kortaloz kamaysa, tana yog' yig'ishni to'xtatadi.",
            2
        ),
        HealthTask(
            "ht_42",
            "🍞",
            "Oq non o'rniga bug'doyning butun donidan tayyorlangan non yeng",
            "Qon shakarini barqaror ushlab turadi",
            "Oq non 'tezkor uglevod' bo'lib, darhol qorinni ochqataveradi. Qora non yoki kepakli non esa uzoq vaqt energiya beradi va qandli diabet xavfini kamaytiradi.",
            2
        ),
        HealthTask(
            "ht_43",
            "🥛",
            "Sutli mahsulotlarning kam yog'li (0.5-1%) variantini tanlang",
            "Keraksiz hayvon yog'larini kamaytirish",
            "Sut va qatiqdagi yog'lar hayvon yog'lari bo'lib, ular kaloriya miqdorini oshiradi. Kam yog'li mahsulotlar esa oqsilni to'liq beradi, lekin semirtirmaydi.",
            2
        ),
        HealthTask(
            "ht_44",
            "🥩",
            "Go'shtning faqat yog'siz (lan) qismini iste'mol qiling",
            "Mushaklarni qurish va yog'dan xalos bo'lish",
            "Go'shtdagi ko'rinib turgan yog'larni bugun ataylab kesib tashlang. Faqat oqsilga e'tibor qarating, bu sizning mushaklaringizni saqlab, tanadagi zaxira yog'larni eritishga majbur qiladi.",
            4
        ),
        HealthTask(
            "ht_45",
            "🍅",
            "Har kuni kamida bitta pomidor yeng",
            "Likoppin moddasi bilan hujayralarni yoshartiradi",
            "Pomidor past kaloriyali va suvga boy. U metabolizmni faollashtiradi va yurakni himoya qiladi. Salatda yoki pishirilgan holda ham u juda foydali.",
            1
        ),
        HealthTask(
            "ht_54",
            "📵",
            "Uxlashdan bir soat oldin barcha ekranlarni (telefon, TV) o'chiring",
            "Melatonin gormonining to'liq ishlab chiqarilishini ta'minlaydi",
            "Ekrandagi ko'k nur miyani aldab, kun deb o'ylaydi. Melatonin (uyqu gormoni) ishlab chiqilmasa, tana tunda yog'larni eritmaydi. Kitob o'qing yoki musiqaga quloq soling.",
            3
        ),
        HealthTask(
            "ht_55",
            "🧊",
            "Dush qabul qilayotganda oxirida sovuqroq suvdan foydalaning",
            "Qon aylanishini kuchaytiradi va 'jigarrang' yog'ni faollashtiradi",
            "Sovuq suv tana haroratini saqlab qolish uchun ichkaridagi 'jigarrang' yog'larni yoqishni boshlaydi. Bu immunitetni ham 3 barobar kuchli qiladi.",
            4
        ),
        HealthTask(
            "ht_56",
            "💨",
            "Har yarim soatda chuqur nafas olib chiqaring (10 marta)",
            "Hujayralarni kislorod bilan to'yintiradi",
            "Yog'larning 'yonishi' uchun kislorod kerak. Chuqur nafas olinganda tana ko'proq kislorod oladi va chiqindilar (karbonat angidrid) orqali yog' qoldiqlari chiqib ketadi.",
            1
        ),
        HealthTask(
            "ht_59",
            "🧘‍♂️",
            "Bugun 10 daqiqa hech narsani o'ylamay meditatsiya qiling",
            "Stress darajasini tushirib, emotsional ovqatlanishni to'xtatadi",
            "Ko'pincha biz qornimiz och bo'lgani uchun emas, balki asabimiz buzilgani uchun ovqat yeymiz. Meditatsiya o'zingizni boshqarishga va ishtahangizni jilovlashga yordam beradi.",
            3
        ),
        HealthTask(
            "ht_60",
            "🦷",
            "Ovqatdan keyin saqich (shakarsiz) chaynashni odat qiling",
            "Qo'shimcha 'perekus' istagini bo'g'adi",
            "Tishlarni tozalash imkoni bo'lmasa, shakarsiz saqich miyaga 'ovqat tugadi' degan signalni yuboradi. Lekin 10-15 daqiqadan ortiq chaynash tavsiya etilmaydi.",
            2
        ),
        HealthTask(
            "ht_61",
            "🧗",
            "Bugun liftni o'rniga zinalardan yugurib chiqing",
            "Maksimal kaloriya sarfini ta'minlaydi",
            "Bu oddiy chiqishdan ko'ra 2 barobar samaraliroq. Yurak urishini tezlashtiradi va qon quyuqlashishini oldini oladi. Tanangiz bunga minnatdor bo'ladi.",
            5
        ),
        HealthTask(
            "ht_62",
            "🏠",
            "Uyingizni yoki ishingizni 15 daqiqa davomida tartibga soling",
            "Faol harakat va toza muhit kombinatsiyasi",
            "Harakat — bu faqat sport emas. Uy tozalash, idish yuvish yoki chang artish ham kaloriya sarflaydi. Atrofingiz toza bo'lsa, fikrlaringiz ham toza va intizomli bo'ladi.",
            2
        ),
        HealthTask(
            "ht_63",
            "🤸‍♂️",
            "Bugun o'zingizni 'elastik' his qilib, 5 daqiqa tanani cho'zing (stretching)",
            "Mushaklarni bo'shashtiradi va qon aylanishini yaxshilaydi",
            "Cho'zilish mashqlari mushaklardagi toksinlarni chiqarib yuboradi. Bu mashg'ulotdan so'ng tanangiz o'zini ancha yengil va baquvvat his qiladi.",
            2
        ),
        HealthTask(
            "ht_64",
            "💃",
            "O'zingiz yoqtirgan sho'x musiqaga 5 daqiqa raqsga tushing",
            "Kayfiyatni ko'taradi va kaloriyalarni quvnoq yoqadi",
            "Raqs — eng zo'r psixologik va jismoniy mashg'ulot. Siz charchamaysiz, aksincha energiya olasiz. Bu vaqt ichida tanangiz 50-70 kaloriya yo'qotishi mumkin.",
            1
        ),
        HealthTask(
            "ht_65",
            "🕧",
            "Ovqatlanish o'rtasida 5 soatlik vaqtni (interval) ushlang",
            "Tanaga to'liq hazm qilish uchun vaqt beradi",
            "Har bir luqma insulinni ko'taradi. Agar 5 soat yemay yursangiz, insulin pasayadi va tana yog' zaxiralarini energiya uchun 'ochishga' majbur bo'ladi.",
            4
        ),
        HealthTask(
            "ht_66",
            "🧴",
            "Bugun tanangizga biroz massa qilishingiz (yog'lar bilan) kerak",
            "Limfa aylanishini yaxshilab, sellulitni kamaytiradi",
            "O'zingizning oyoq va qo'llaringizni uqalash orqali limfa markazlarini ishga tushirasiz. Bu tanadagi shishlarni va ortiqcha suvni chiqib ketishini tezlashtiradi.",
            2
        ),
        HealthTask(
            "ht_67",
            "👜",
            "Do'konga borishda mashinadan emas, piyoda yurishdan foydalaning",
            "Har kunlik faollikni tabiiy ravishda oshirish",
            "Yaqin masofalarga mashinada borish — sog'liq uchun dushmandir. Piyoda yurish nafaqat benzinni, balki sizning qoriningizdagi yog'larni ham tejaydi.",
            2
        ),
        HealthTask(
            "ht_68",
            "🚴",
            "Agar imkon bo'lsa, bugun velosipedda 20 daqiqa sayr qiling",
            "Bo'g'imlarga yuklamasiz kardio mashg'ulot",
            "Velosiped oyoq mushaklarini shakllantiradi va nafas tizimini chiniqtiradi. Bu juda quvnoq va samarali vazn yo'qotish usulidir.",
            3
        ),
        HealthTask(
            "ht_69",
            "🧗‍♂️",
            "Bugun osilib turish (turnik) mashqini 30 soniya bajaring",
            "Umurtqani to'g'rilaydi va bo'yni o'stiradi",
            "Osilib turish qorin mushaklarini tabiiy ravishda tortadi va gavda tuzilishini (osanka) yaxshilaydi. Gavda to'g'ri bo'lsa, qorin ham kamroq bilinadi.",
            4
        ),
        HealthTask(
            "ht_70",
            "☀️",
            "Bugun quyosh nuri ostida 15 daqiqa turing",
            "D vitamini bilan immunitetni va metabolizmni quvvatlaydi",
            "D vitamini yetishmovchiligi ko'pincha semirishga sabab bo'ladi. Tabiiy quyosh nuri organizmga yog'larni parchalash uchun kerakli gormonlarni beradi.",
            1
        ),
        HealthTask(
            "ht_71",
            "🥕",
            "Kechki ovqatga faqat sabzavotlar (pishirilgan yoki xom) yeng",
            "Uxlashdan oldin oshqozonga yengillik beradi",
            "Sabzavotlar past kaloriya va yuqori kletchatka manbaidir. Kechasi tana ovqatni hazm qilishga emas, balki yog'larni yanada ko'proq yoqishga e'tibor qaratadi.",
            3
        ),
        HealthTask(
            "ht_72",
            "🍒",
            "Shirinlik o'rniga bugun mavsumiy reza mevalardan yeng",
            "Antioksidantlar bilan tanani yoshartiradi",
            "Gilos, qulupnay yoki malina kabi mevalar shakar miqdori kam va foydasi ko'p. Ular sizni shirinlik xohishidan qutqaradi va qonni tozalaydi.",
            2
        ),
        HealthTask(
            "ht_73",
            "🧅",
            "Ovqatingizga ko'proq piyoz va sarimsoqpiyoz qo'shing",
            "Tabiiy antibiotik va qonni suyultiruvchi vosita",
            "Piyoz va sarimsoqpiyoz qon aylanishini yaxshilab, metabolizmni faollashtiradi. Ular shuningdek, immunitetni mustahkamlab, sizni energiya bilan to'ldiradi.",
            1
        ),
        HealthTask(
            "ht_74",
            "🧺",
            "Bugun ovqat uchun 'to'xtash' (stop) vaqtini belgilang",
            "Ovqat yeyish o'rtasidagi pauzalarni ko'paytirish",
            "Har porsiya ovqatni 5 daqiqa davomida tanaffus bilan yeng. Bu sizga haqiqiy to'qlik hissini bilishga yordam beradi va siz o'ylaganingizdan kamroq yeysiz.",
            2
        ),
        HealthTask(
            "ht_75",
            "🛀",
            "Dengiz tuzi qo'shilgan vannada 15 daqiqa dam oling",
            "Tana kislotaliligini haydaydi va toksinlardan xalos qiladi",
            "Dengiz tuzi teri orqali o'tib, mushaklardagi charchoqni oladi va tanadagi ortiqcha suvni chiqarib yuboradi. Siz o'zingizni ancha yengil his qilasiz.",
            3
        ),
        HealthTask(
            "ht_76",
            "🧘",
            "Bugun 'Vakuum' mashqini 3 marta bajaring",
            "Qorinning ichki (abdominal) mushaklarini tortadi",
            "Vakuum — bu nafasni chiqarib, qorinni ichkariga tortib turishdir. Bu mashq qorin qismini eng samarali to'g'rilovchi usul hisoblanadi.",
            4
        ),
        HealthTask(
            "ht_77",
            "👣",
            "Bugun kamida 5000 qadam bosishga harakat qiling",
            "Yurak va qon tomirlar sog'lig'ini saqlaydi",
            "Piyoda yurish — inson uchun eng tabiiy harakat. 5000 qadam — bu sizning kundalik minimal sog'lomlik normangizdir. Buni albatta bajaring.",
            2
        ),
        HealthTask(
            "ht_78",
            "🥤",
            "Bugun sutli qahva (latte/kapuchino) o'rniga qora choy iching",
            "Kamroq kaloriya va ko'proq energiya beradi",
            "Sutli qahvalarda juda ko'p yashirin kaloriyalar bo'ladi. Qora choy esa metabolizmni quvvatlaydi va sizni ortiqcha semirtirmaydi.",
            3
        ),
        HealthTask(
            "ht_79",
            "🥗",
            "Ovqatga kashnich yoki zira qo'shing",
            "Hazm qilish fermentlarini faollashtiradi",
            "Zira — bu yog'larni eritishda eng kuchli ziravorlardan biri. U oshqozonni ishlashga majbur qiladi va ovqatni juda tez hazm bo'lishiga yordam beradi.",
            1
        ),
        HealthTask(
            "ht_80",
            "🍚",
            "Oq guruch o'rniga bug'doy yoki grechka yeng",
            "Glyukemik indeksni pasaytiradi va to'q tutadi",
            "Grechka — oqsillar va temir moddasiga boy. U qondagi shakarni sakratmaydi, ya'ni siz ovqatdan so'ng darhol ochqab qolmaysiz.",
            2
        ),
        HealthTask(
            "ht_81",
            "🧘‍♀️",
            "Sokin musiqa ostida 5 daqiqa ko'zingizni yumib o'tiring",
            "Miyadagi shovqinlarni tozalab, stressni kamaytiradi",
            "Stress — vazn yig'ishning asosiy sabablaridan biri. Bunday tanaffus qilish orqali siz tanangizga tinchlanish va tiklanish imkonini berasiz.",
            1
        ),
        HealthTask(
            "ht_82",
            "🚶‍♂️",
            "Bugun uzoqroq yo'l bilan piyoda uyga qayting",
            "Kardio mashg'ulot davomiyligini oshiradi",
            "Har bir qo'shimcha 10 daqiqalik yurish — bu qo'shimcha 100 kaloriya yo'qotish demakdir. Bu oddiy odat bir oyda sezilarli natija beradi.",
            2
        ),
        HealthTask(
            "ht_83",
            "🥜",
            "Kuniga faqat bir siqim qovoq urug'i yeng",
            "Rux (sink) moddasi bilan metabolizmni quvvatlaydi",
            "Qovoq urug'i foydali mikroelementlarga boy. U ishtahani boshqarishga va tanani minerallar bilan to'ldirishga yordam beradi.",
            2
        ),
        HealthTask(
            "ht_84",
            "🍵",
            "Damlamangizga bir chimdim qora murch qo'shing",
            "Yog'larni parchalaydigan 'bioperin' moddasini beradi",
            "Qora murch ziravor sifatida ovqatning foydali xususiyatlarini 2000 foizga oshirishi isbotlangan. U yog' hujayralarining hosil bo'lishini to'xtatadi.",
            3
        ),
        HealthTask(
            "ht_85",
            "🥑",
            "Bugun foydali yog' sifatida avokado yoki zaytun yog'ini tanlang",
            "Tana kislotasini muvozanatlashtiradi",
            "Foydali yog'lar o'z-o'zidan ozishga yordam beradi. Ular to'qlik hissini beradi va tana hujayralarini sog'lom yangilanishini ta'minlaydi.",
            4
        ),
        HealthTask(
            "ht_87",
            "💧",
            "Ovqat yeyayotganda suv ichmang, faqat undan oldin yoki keyin",
            "Oshqozon shirasini suyultirmaslik uchun",
            "Ovqat paytida suv ichish hazmni sekinlashtiradi. Toza hazm bo'lishi uchun 20 daqiqalik farqni ushlang, shunda qorin dam bo'lmaydi.",
            2
        ),
        HealthTask(
            "ht_89",
            "🛌",
            "Bugun to'yib, kamida 8 soat uxlang",
            "Maksimal darajada yog' yoqish uyqusini oling",
            "Uyqu yetarli bo'lmaganda tana shirinlikni ko'p talab qiladi. 8 soatlik uyqu esa sizni kun bo'yi tetik va ishtahani barqaror ushlab turadi.",
            1
        ),
        HealthTask(
            "ht_90",
            "🧼",
            "Uxlashdan oldin qattiq kislotali (shirin emas) olmani hidlang",
            "Miyani ishtaha markazlarini aldaydi",
            "Olma hidi tunda ovqat yeyish istagini kamaytirishi haqida tadqiqotlar bor. Bu psixologik usul sizni ortiqcha ovqatlanishdan asrashi mumkin.",
            2
        ), HealthTask(
            "ht_51",
            "🚶",
            "Ovqatdan keyin kamida 5 daqiqa xona bo'ylab yuring",
            "Qondagi qand miqdorini keskin ko'tarilishidan to'sadi",
            "Yeb bo'lib darhol yotish — eng yomon odat. 5 daqiqa tik turish yoki sekin yurish ovqatning yaxshi hazm bo'lishiga va qorin qismining semirmasligiga yordam beradi.",
            1
        ),
        HealthTask(
            "ht_52",
            "🏢",
            "Bugun lift ishlatmang, faqat zinadan foydalaning",
            "Oyoq va dumba mushaklarini kuchli ishlashga majbur qiladi",
            "Zinadan chiqish — bu eng samarali kardio mashg'ulot. U oddiy yurishdan 3 barobar ko'proq kaloriya yoqadi. Har bir zina — bu sizni 70 kg vazn sari yaqinlashtiruvchi qadamdir.",
            3
        ),
        HealthTask(
            "ht_53",
            "📞",
            "Telefonda gaplashayotganingizda faqat tik turib yuring",
            "Kunlik harakat miqdorini bilmasdan oshirish imkonini beradi",
            "Agar siz 20 daqiqa telefonda gaplashsangiz va shu vaqtda xonada yursangiz, sezilarli miqdorda energiya sarflaysiz. O'tirib gaplashish esa faqat dangasalikni oshiradi.",
            1
        ),
        HealthTask(
            "ht_57",
            "🧘‍♂️",
            "Bugun atigi 20 soniya 'planka' mashqida turing",
            "Qorin mushaklarini ichkaridan mustahkamlab, qorinni tortadi",
            "Planka — bu yerga tayanib qimirlamay turishdir. U qorinni osilib qolishidan asraydi va ichki organlarni o'z joyida ushlab turuvchi mushaklarni baquvvat qiladi.",
            4
        ),
        HealthTask(
            "ht_58",
            "🦵",
            "Kunning istalgan paytida 10 marta o'tirib-turishni bajaring",
            "Yirik mushaklarni uyg'otib, metabolizm darajasini ko'taradi",
            "Oyoqlar — tanadagi eng katta mushaklardir. Ular ishlaganda tana juda ko'p kaloriya sarflashni boshlaydi. 10 marta 'prisidaniye' qilish uchun bor-yo'g'i 1 daqiqa kifoya.",
            3
        ),

        // --- UYQU VA TARTIB (81-100) ---
        HealthTask(
            "ht_86",
            "😴",
            "Kechki soat 23:00 gacha uxlashga yoting",
            "Ozish uchun eng muhim bo'lgan 'o'sish gormoni'ni faollashtiradi",
            "Inson aynan uyquda ozadi. Agar siz 23:00 dan kech qolsangiz, tana stress holatiga tushadi va yog' yoqish o'rniga uni saqlab qolishga harakat qiladi. To'yib uxlash — ozishning 30 foiz muvaffaqiyatidir.",
            4
        ),
        HealthTask(
            "ht_88",
            "📸",
            "Bugun hamma yegan narsangizni rasmga olib boring",
            "O'z-o'zingizni nazorat qilish va mas'uliyat hissini uyg'otadi",
            "Inson rasmga olganda nima yeyayotganiga boshqacha nazar bilan qaraydi. Bu sizni keraksiz, zararli ovqatlardan tiyilishga va tartibga o'rganishga majbur qiladi.",
            3
        )

        // ... Qolgan vazifalar ham shunday batafsil davom etadi
    )

    fun getTaskById(id: String): HealthTask? = tasks.find { it.id == id }

    fun getTasksForLevel(level: Int, count: Int): List<HealthTask> {
        val availableTasks = tasks.filter { it.difficulty <= level }
        return if (availableTasks.size <= count) {
            availableTasks
        } else {
            availableTasks.shuffled().take(count)
        }
    }
}