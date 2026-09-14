package uz.oybek.ozamiz.ui.utils

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object DailyTipUtil {

    val tips = listOf(
        // 1-10: Kunlik odatlar va Nonushta
        Tip(
            "Tonggi oqsil quvvati",
            "Nonushtaga uglevodlar (non, shirinlik) emas, oqsilga boy taomlar (masalan, 2 ta tuxum) yeyish kun bo'yi ishtahani bo'g'adi va yog' erishini tezlatadi.",
            "Ozuqa",
            "🥚",
            "Nega aynan tuxum? Oqsilni hazm qilish uchun tanamiz o'zidan juda ko'p energiya (kaloriya) sarflaydi. Tasavvur qiling, oqsil bu – pechkaga tashlangan qattiq o'tin, u uzoq yonib, ko'p issiqlik beradi. Shirinlik va non esa qog'ozdek gap, tez yonib tugaydi va yana qorningiz ochadi. Tuxum yeganingizda insulin (semirtiruvchi gormon) keskin ko'tarilmaydi va tanangiz darhol yog' yoqish rejimiga o'tadi.",
            "Ertangi nonushtaga sosiska o'rniga 2 ta qaynatilgan tuxum tayyorlang."
        ),
        Tip(
            "Suyuq kaloriyalarni unuting",
            "Kola, energetiklar va qadoqlangan sharbatlar – bu qoringa to'g'ridan-to'g'ri yog' bo'lib yig'iladigan suyuq shakardir. Ularning o'rniga toza suv iching.",
            "Ozish",
            "💧",
            "Suyuq shakarni miyamiz va oshqozonimiz ovqat sifatida qabul qilmaydi. Masalan, 1 stakan kola ichganingizda, tanaga 10 qoshiq shakar kiradi, lekin qorin to'ymaydi! Bu shakar darhol qon orqali qorindagi yog' zahirasiga aylanadi. Toza suv esa nol kaloriya bo'lib, yog' hujayralarini parchalashda qatnashadigan asosiy suyuqlikdir. Suvsiz yog' umuman erimaydi.",
            "Chanqaganda barcha shirin ichimliklarni oddiy suvga almashtiring."
        ),
        Tip(
            "Tarelka yarmi qoidasi",
            "Tarelkaning yarmini makaron yoki guruch kabi og'ir ovqatlarga emas, pishmagan yangi sabzavotlarga to'ldiring. Bu eng oson kaloriya tejash usulidir.",
            "Odatlar",
            "🥗",
            "Sabzavotlar (bodring, karam, pomidor) asosan suv va tolalar (kletchatka) dan iborat. Misol uchun, 1 tarelka makaronda 300 kaloriya bo'lsa, xuddi shunday hajmdagi karam salatida atigi 30 kaloriya bor. Agar siz ovqatlanishni salat yeyishdan boshlasangiz, oshqozonning katta qismi kaloriyasiz ozuqa bilan to'ladi va siz semirtiradigan asosiy taomdan tabiiy ravishda kamroq yeysiz.",
            "Asosiy ovqatni yeyishdan oldin albatta 1 porsiya salat yeb oling."
        ),
        Tip(
            "Och qoringa do'konga kirmang",
            "Supermarketga qorin och holatda kirish – noto'g'ri va kaloriyali mahsulotlarni sotib olishning asosiy sababidir. Faqat to'q qoringa xarid qiling.",
            "Psixologiya",
            "🛒",
            "Tana och qolganda omon qolish rejimiga o'tadi va eng tez energiya beradigan, yog'li va shirin narsalarni (shokolad, pishiriq, kolbasa) talab qiladi. Och odamning aqli emas, oshqozoni qaror qabul qiladi. To'q qoringa do'konga kirganda esa, inson xotirjam bo'ladi va faqat ro'yxatdagi sog'lom mahsulotlarni oladi, ko'zi zararli ovqatlarga uchmaydi.",
            "Xaridga borishdan avval 1 stakan qatiq yoki ozgina ovqat yeb oling."
        ),
        Tip(
            "Shirinlikning to'g'ri vaqti",
            "Shirinlikni quruq choy bilan och qoringa yeyish eng katta xatodir. Uni har doim asosiy va to'yimli ovqatdan keyin iste'mol qiling.",
            "Ozish",
            "🍰",
            "Agar siz bitta shokoladni och qoringa yesangiz, qondagi shakar va insulin raketadek tepaga otiladi. Insulin ko'paydimi – yog' erishi darhol to'xtaydi. Lekin xuddi shu shokoladni go'shtli yoki salatli taomdan keyin yesangiz, avval yeyilgan oqsil va yog'lar shakarning qonga o'tish tezligini tormozlaydi. Natijada siz shirinlik yeysiz, lekin u birdaniga qoringa yog' bo'lib yopishmaydi.",
            "Sevimli shirinligingizni ovqat oralig'ida emas, tushlikdan so'ng darhol yeng."
        ),
        Tip(
            "Telefonsiz ovqatlanish",
            "Video ko'rib yoki ijtimoiy tarmoq titkilab ovqatlanish miyani chalg'itib, qorin to'yganini sezmay qolishingizga olib keladi.",
            "Psixologiya",
            "📱",
            "Miyaning diqqati telefondagi videoda bo'lsa, u oshqozondan kelayotgan \"men to'ydim\" degan signalni o'tkazib yuboradi. Misol uchun, qiziqarli kino ko'rib qanday qilib bir chelak pop-korn yeb qo'yganingizni sezmay qolasiz. Diqqat faqat taomda bo'lganda esa, lazzatni sezasiz va juda kichik porsiya bilan ham to'yib qolasiz.",
            "Ovqatlanayotganda telefoningizni stoldan olib qo'ying."
        ),
        Tip(
            "Sekin chaynash sehri",
            "Ovqatni shoshilib yutib yubormang. Har bir luqmani uzoq va ezib chaynash – kamroq yeb, tezroq to'yishning eng isbotlangan usulidir.",
            "Odatlar",
            "🧘‍♂️",
            "Oshqozonga tushgan ovqat haqidagi ma'lumot miyaga yetib borishi uchun kamida 15-20 daqiqa vaqt kerak. Agar siz ovqatni 5 daqiqada paqqos tushirsangiz, miya to'yganini sezguncha yana qo'shimcha ovqat yeb qo'yasiz. Sekin chaynagan odam esa shu 20 daqiqada ancha kam kaloriya qabul qilib bo'ladi va shishib ketmasdan dasturxondan turadi.",
            "Har luqmani yutishdan oldin kamida 20 marta chaynang."
        ),
        Tip(
            "Tuz va yig'ilgan suv",
            "Taomdagi tuz miqdorini kamaytirish orqali siz tanangizda yig'ilib qolgan ortiqcha suv va shishlardan tezda xalos bo'lasiz.",
            "Sog'liq",
            "🧂",
            "1 gramm tuz o'zida taxminan 100 ml suvni ushlab qoladi. Ko'p sho'r ovqat yegan kuningiz ertasi kuni yuzingiz shishib, vazningiz 1-2 kiloga ortishi shundan. Tuzni kamaytirsangiz, hujayralardagi ortiqcha va og'ir suv tanadan chiqib ketadi. Natijada siz o'zingizni juda yengil his qilasiz va hajmingiz sezilarli qisqaradi.",
            "Taomga odatdagidan kamroq tuz solib, o'rniga limon suvi bilan ta'm bering."
        ),
        Tip(
            "Kichik idish qoidasi",
            "Katta tarelka o'rniga kichikroq likopchada ovqatlanish – kaloriya va porsiya hajmini nazorat qilishning ko'rinmas psixologik qurolidir.",
            "Psixologiya",
            "🍽️",
            "Miya bizni ovqat hajmi orqali aldaydi. Katta idishga solingan normal ovqat juda oz ko'rinadi va biz qo'shimcha solamiz. Xuddi shu ovqatni kichik idishga solsak, u to'lib-toshib ko'rinadi va miya \"bu ovqat juda ko'p, men to'yaman\" degan xulosaga keladi. Bu usul yordamida siz o'zingizni cheklaganday his qilmasdan, porsiyangizni 20-30% ga kamaytirasiz.",
            "Oshxonadagi katta idishlarni yig'ishtirib, faqat ixcham idishlarda ovqatlaning."
        ),
        Tip(
            "Sof qahva va metabolizm",
            "Katta stakandagi sirop, qaymoq va shakarli qahvalar – bu to'laqonli shirinlikdir. Ozish uchun faqat qora (shakarsiz) qahva iching.",
            "Ozish",
            "☕",
            "Kafelarda sotiladigan 1 stakan karamelli qahvada 400 gacha kaloriya (bitta gamburger bilan bir xil) bo'lishi mumkin. Qora Amerikano yoki Espressoda esa kaloriya nolga teng. Bundan tashqari, sof kofein asab tizimini faollashtirib, jismoniy harakat vaqtida tanani uglevodlardan ko'ra to'g'ridan-to'g'ri yog' zahiralaridan energiya olishga majbur qiladi.",
            "Qahvaga shakar solishni to'xtating, o'zgacha ta'm uchun ozgina dolchin qo'shing."
        ),

        // 11-30: Ozuqa va Mahsulotlar sirlari
        Tip(
            "Butun mevani tanlang",
            "Mevalarning suvi (sharbati) foydali emas. Mevaning o'zini qobig'i bilan butunligicha iste'mol qilish vazn yo'qotish uchun muhimdir.",
            "Ozuqa",
            "🍎",
            "1 stakan olma sharbati qilish uchun 3-4 ta olma kerak bo'ladi va unda kletchatka (tola) umuman qolmaydi – faqat shakarli suv qoladi. Siz 1 stakan sharbatni 10 soniyada ichib qo'yasiz va qondagi shakar keskin oshadi. Ammo siz 3 ta olmani chaynab yeya olmaysiz. Butun mevadagi tolalar oshqozonda shishib, shakarning qonga o'tishini qiyinlashtiradi va uzoq vaqt to'q tutadi.",
            "Sharbat o'rniga bitta butun olma yoki nokni tishlab yeng."
        ),
        Tip(
            "Murakkab uglevodlar",
            "Oq non, bulochka va oq guruchni ratsiondan chiqarib, ularning o'rnini qora non, suli yormasi va grechkaga almashtiring.",
            "Ozuqa",
            "🍞",
            "Oq un mahsulotlari (tezkor uglevodlar) tanada tez parchalanib, darhol shakarga aylanadi va energiya sarflanmasa, yog'ga o'tadi. Qora non va grechka kabi murakkab uglevodlar esa oshqozonda bir necha soat davomida sekin hazm bo'ladi. Ular tanaga energiyani birdaniga emas, tomchilab beradi. Natijada sizda ortiqcha kaloriya yig'ilmaydi va doimiy quvvat bo'ladi.",
            "Garnir uchun makaron o'rniga faqat grechka yoki perlovka ishlating."
        ),
        Tip(
            "Suyuq yog'lar qopqoni",
            "Salatlarga qo'shiladigan mayonez va ko'p miqdordagi yog'lar salatning barcha ozdiruvchi xususiyatini yo'qqa chiqaradi. Soslarni o'zgartiring.",
            "Ozish",
            "🥣",
            "1 osh qoshiq mayonez yoki o'simlik yog'ida taxminan 100-120 kaloriya bor. Tasavvur qiling, siz foydali karam salati qildingiz (30 kaloriya) va unga 3 qoshiq yog' yoki mayonez qo'shdingiz – salatingiz darhol 400 kaloriyali (semirtiruvchi) bombaga aylandi. Qatiq yoki limonda esa kaloriyalar deyarli yo'q.",
            "Salatlarni faqat yog'siz qatiq yoki limon suvi bilan aralashtiring."
        ),
        Tip(
            "Sog'lom yog'lar kerak",
            "Ozish uchun umuman yog' yemay qo'yish xatodir. Yong'oq, urug'lar va avokado tarkibidagi tabiiy yog'lar tanamizdagi yog'larni eritish uchun kerak.",
            "Sog'liq",
            "🥑",
            "Gormonlarimiz, asab tizimi va miyamiz ishlashi uchun to'g'ri yog'lar zarur. Agar yog'ni butunlay to'xtatsangiz, sochlaringiz to'kila boshlaydi, teringiz osilib ketadi va metabolizm buziladi. Bodom yoki yong'oq tarkibidagi foydali yog'lar esa qondagi xolesterinni tushiradi va aslida ortiqcha yog'larni hujayradan chiqarishga yordam beradi.",
            "Kuniga 10-15 dona xom (qovurilmagan) bodom yoki yong'oq yeng."
        ),
        Tip(
            "Qora shokolad antioksidanti",
            "Shirinlik yesangiz, faqat kakao miqdori 70% va undan yuqori bo'lgan qora shokoladni tanlang. Sutli shokoladlar – bu sof shakar.",
            "Ozuqa",
            "🍫",
            "Qora shokolad tarkibidagi kakao antioksidantlarga boy bo'lib, stress gormonini (kortizol) tushiradi. Stress tushganda esa tana yog' yig'ishni to'xtatadi. Shuningdek, uning ta'mi biroz taxir bo'lgani uchun undan ko'p yeb qo'yolmaysiz. 1-2 bo'lak qora shokolad sizning shirinlikka bo'lgan kuchli xumoringizni kaloriyasiz qondiradi.",
            "Uyda shirinlik so'raganda yeyish uchun doim qora shokolad saqlang."
        ),
        Tip(
            "Pishirish usulini o'zgartiring",
            "Taomni tavada ko'p yog'da qovurish uning kaloriya qiymatini ikki barobar oshiradi. Dimlash va duxovkada pishirishga o'ting.",
            "Odatlar",
            "🍲",
            "Misol uchun: 100 gramm xom kartoshkada 70 kaloriya bor. Agar uni suvda qaynatib yesangiz, shu 70 kaloriya o'zgarishsiz qoladi. Lekin uni yog'da qovursangiz (fri qilsangiz), yog'ni o'ziga shimib, kaloriyasi 300 ga ko'tariladi. Go'sht yoki sabzavotlarni folgaga o'rab, o'z suvida duxovkada pishirish orqali siz yuzlab bo'sh kaloriyalardan qutulasiz.",
            "Go'sht va sabzavotlarni yog'da emas, bug'da yoki duxovkada pishiring."
        ),
        Tip(
            "Yashirin shakar va tarkib",
            "Do'kondan mahsulot olayotganda har doim \"Tarkibi\" (Sostav) qismini o'qing. \"Parhezbop\" deb yozilgan ko'p mahsulotlar aslida shakar qopqonidir.",
            "Psixologiya",
            "🔍",
            "Ishlab chiqaruvchilar mahsulotdan yog'ni olib tashlab, \"0% Yog'li\" deb yozishadi. Ammo ta'mni ushlab turish uchun unga katta miqdorda shakar qo'shishadi. Meva bo'lakli qatiqlar, ketchup va har xil fitnes batonkalarida ba'zan shokoladdan ham ko'proq shakar bo'ladi. Tarkib ro'yxatida nima birinchi yozilsa, mahsulotda shu narsa eng ko'p bo'ladi.",
            "Tarkibining birinchi 3 ta so'zida \"Shakar\" (Saxar) qatnashgan mahsulotlarni olmang."
        ),
        Tip(
            "Quruq mevalarga ehtiyot bo'ling",
            "Mayiz, turshak yoki anjir juda foydali bo'lsa-da, ularni hovuchlab yeyish ozishni butunlay to'xtatib qo'yadi. Ular o'ta yuqori kaloriyali.",
            "Ozuqa",
            "🍇",
            "Meva quritilganda ichidagi suvi chiqib ketadi va faqat shakar qoladi. Misol uchun, 10 ta katta yangi o'rikni yeyish qiyin, u qorinni to'ldiradi. Lekin 10 ta quritilgan o'rikni (turshak) 1 daqiqada choy bilan yeb qo'yish juda oson. Ikkalasida ham kaloriya bir xil! Quruq mevalar hajmi kichik bo'lgani uchun odam me'yorni yo'qotib qo'yadi va ortiqcha energiya oladi.",
            "Quruq mevalarni kuniga 4-5 donadan ortiq yemang."
        ),
        Tip(
            "Uy taomlari ustunligi",
            "Ko'chada, kafe va restoranlarda ovqatlanishni kamaytirib, iloji boricha uyda tayyorlangan taomlarni iste'mol qiling.",
            "Odatlar",
            "👨‍🍳",
            "Restoranlarda taom mazali bo'lishi va tez tayyorlanishi uchun uyingizdagidan ko'ra 3-4 barobar ko'proq yog', margarin va tuz ishlatiladi. Siz \"yengil taom\" deb o'ylagan ko'chadagi oddiy ovqat ichida qancha yashirin kaloriya borligini bilmaysiz. Uyda pishirganda esa, siz har bir tomchi yog'ni o'zingiz nazorat qilasiz.",
            "Ish joyingizdagi tushlik uchun uydan sog'lom ovqat olib boring."
        ),
        Tip(
            "Sho'rva bilan boshlash",
            "Katta va quyuq asosiy ovqatdan oldin bir piyola issiq qaynatma sho'rva (bulon) ichish ishtahani so'ndiradi va ozishga yordam beradi.",
            "Ozish",
            "🥣",
            "Issiq suyuqlik oshqozon devorlarini isitib, yozadi va miyaga \"qorin to'lishni boshladi\" degan signal yuboradi. Tadqiqotlar shuni ko'rsatadiki, ovqatlanishni sho'rva bilan boshlagan insonlar, keyingi taomlardan odatdagidan 20% kamroq kaloriya qabul qilishadi. Bu sizga o'zingizni zo'rlamasdan kamroq yeyish imkonini beradi.",
            "Kechki ovqatni albatta yengil issiq sho'rva bilan boshlang."
        ),
        Tip(
            "Tungi ishtahani yengish",
            "Kechasi uxlashdan oldin qorningiz kuchli ochqab ketsa, meva yemang. Meva shirinligi shakarni oshiradi. Yog'siz oqsil iste'mol qiling.",
            "Ozish",
            "🧀",
            "Meva tarkibidagi fruktoza tunda jigar tomonidan yog'ga aylantirilishi oson. Ammo pishgan tuxum oqi yoki bir necha qoshiq yog'siz tvorog faqat sof oqsil beradi. Oqsil insulinni oshirmaydi, hazm bo'lishiga ko'p energiya talab qiladi va tunda tanangiz faqat yog' yoqish rejimida qolishiga yordam beradi.",
            "Tunda ochqasangiz, sariqlari olingan 2 ta tuxum oqini yeng."
        ),
        Tip(
            "Kolbasa va sosiska zarari",
            "Barcha qayta ishlangan go'sht mahsulotlarini – sosiska, kolbasa, dudlangan go'shtlarni menyudan olib tashlang. Faqat tabiiy go'sht oling.",
            "Sog'liq",
            "🌭",
            "Kolbasa tarkibida sof go'sht juda kam. Uning qolgan qismi soya, o'zlashtirilmaydigan kraxmal, sun'iy ta'm beruvchilar va eng yomoni – yashirin yog'lardan iborat. 100 gramm kolbasada xuddi shunday hajmdagi mol go'shtidan ko'ra 2 marta ko'p semirtiruvchi kaloriyalar mavjud. Tabiiy go'sht esa mushaklar uchun zaruriy qurilish materialidir.",
            "Ertalabki sosiska o'rniga, tovuq felesini qovurmasdan pishirib yeng."
        ),
        Tip(
            "Dengiz balig'ining kuchi",
            "Haftada kamida 2 kun ratsioningizga dengiz balig'ini qo'shing. Bu moddalar almashinuvini (metabolizm) tezlatish uchun eng muhim qadamdir.",
            "Ozuqa",
            "🐟",
            "Dengiz baliqlari (skumbriya, losos) tarkibida juda qimmatli Omega-3 yog' kislotalari mavjud. Bu g'alati tuyulishi mumkin, lekin baliqdagi bu \"yaxshi yog'lar\" tanadagi qotib qolgan \"yomon yog'lar\"ni (ayniqsa qorin sohasidagi) parchalash va tanadan chiqarib yuborish mexanizmini ishga tushiradi. Bundan tashqari, u yoshlik gormonlarini ishlab chiqaradi.",
            "Haftaning ma'lum bir kunlarini o'zingiz uchun \"Baliq kuni\" qilib belgilang."
        ),
        Tip(
            "Asal va shakar haqiqati",
            "Shakar o'rniga cheksiz asal iste'mol qilish vazn yo'qotishga yordam bermaydi. Tana uchun ikkalasi ham bir xil ishlaydi.",
            "Ozuqa",
            "🍯",
            "Asal ming dardga davo va tarkibida foydali vitaminlar ko'p, biroq vazn yo'qotish (kaloriya) nuqtai nazaridan uning oddiy shakardan deyarli farqi yo'q. Asalning ham kaloriyasi juda yuqori va ko'p yeyilganda xuddi shakar kabi insulinni keskin oshirib, yog' yoqish jarayonini bloklab qo'yadi. Asalni doridek ozgina qabul qilish kerak.",
            "Kuniga 1 choy qoshiqdan ortiq asal iste'mol qilmang."
        ),
        Tip(
            "Sovutilgan kartoshka (Rezistent kraxmal)",
            "Kartoshkadan butunlay voz kechish shart emas. Agar uni to'g'ri (sovitib) yesangiz, u semirtirmaydigan foydali mahsulotga aylanadi.",
            "Ozish",
            "🥔",
            "Issiq, endi pishgan kartoshka tarkibida oddiy kraxmal bo'ladi va u tezda shakarga aylanib qoringa yog' bo'lib tushadi. Ammo qaynatilgan kartoshkani muzlatgichda bir necha soat sovitib qo'ysangiz, unda \"Rezistent kraxmal\" hosil bo'ladi. Tanamiz bu kraxmalni hazm qila olmaydi, ya'ni u kaloriyasiz qoladi va ichaklarni tozalab chiqib ketadi.",
            "Kartoshkani qaynatgach, muzlatgichga qo'yib, ertasi kuni sovuq salatlarga qo'shib yeng."
        ),

        // 31-45: Suv va Gidratatsiya
        Tip(
            "Uyg'ongach suv ichish",
            "Ertalab ko'z ochishingiz bilan, ovqatlanishdan oldin 1-2 stakan xona haroratidagi toza suv ichish tanani tozalashni boshlaydi.",
            "Gidratatsiya",
            "🌅",
            "Uyqu vaqtida nafas orqali biz taxminan 500 ml gacha suv yo'qotamiz va qonimiz quyuqlashadi. Ertalab ichilgan suv xuddi quruq tuproqqa quyilgan yomg'irdek organizmni uyg'otadi. U qonni suyultiradi, ichaklarda tunda yig'ilgan qoldiqlarni tozalaydi va eng muhimi – sekinlashgan metabolizmni (kaloriya yoqish o'chog'ini) 30% ga tezlashtirib yuboradi.",
            "Yotishdan oldin karavotingiz yoniga bir stakan toza suv tayyorlab qo'ying."
        ),
        Tip(
            "Ochlikmi yoki chanqoq?",
            "Vaqtsiz qorningiz ochganda darhol ovqatlanmang. Tana aslida suv talab qilayotgan, siz esa buni ochlik deb o'ylayotgan bo'lishingiz mumkin.",
            "Gidratatsiya",
            "🤔",
            "Inson miyasida ochlik va chanqoqlik signallarini beruvchi markazlar yonma-yon joylashgan va ko'pincha miya bu ikkisini adashtirib yuboradi. Tana shunchaki namlik yetishmayotganini bildirsa ham, biz buni \"qorin ochdi\" deb qabul qilib, keraksiz kaloriyalarni yeyishni boshlaymiz. 1 stakan suv ichish bu adashishni darhol aniqlab beradi.",
            "Qorin ochishini his qilganda, 1 stakan suv ichib, 15 daqiqa kuting. Ochlik o'tib ketadi."
        ),
        Tip(
            "Ovqatdan oldingi suv qoidasi",
            "Har bir asosiy ovqatlanishdan 20-30 daqiqa oldin 1 stakan toza suv ichish vazn tashlashdagi eng kuchli yordamchilardan biridir.",
            "Ozish",
            "💧",
            "Bu juda oddiy mexanika: suv oshqozonning ma'lum bir qismini to'ldiradi. Natijada siz ovqat yeyishni boshlaganingizda oshqozon odatdagidan ko'ra tezroq \"men to'ldim\" degan signalni yuboradi. Tajribalar ko'rsatishicha, ovqatdan oldin suv ichgan insonlar asosiy taomdan ortiqcha yeb qo'yishdan saqlanib, 20% gacha kamroq kaloriya qabul qilishadi.",
            "Telefoningizga tushlik va kechki ovqatdan 20 daqiqa oldin suv ichish eslatmasini qo'ying."
        ),
        Tip(
            "Muzdek suvning zarari",
            "Ayniqsa ovqat bilan birga yoki undan so'ng muzdek sovuq suv ichishdan butunlay saqlaning. Suv doim iliq yoki xona haroratida bo'lsin.",
            "Hazm qilish",
            "🧊",
            "Muzdek suv taomdagi yog'larning oshqozon ichida muzlab, qattiq massaga aylanishiga sabab bo'ladi. Qotgan yog'ni oshqozon kislotasi parchalay olmaydi va bu taom to'g'ridan-to'g'ri ichaklarga o'tib, yog' zahirasiga (ko'pincha qoringa) yopishadi. Iliq suv esa aksincha, yog'larni eritib, oson hazm bo'lishini ta'minlaydi.",
            "Restoranlarda suv so'raganda albatta \"muzsiz va xona haroratida\" bo'lishini tayinlang."
        ),
        Tip(
            "Yashil choyning yog' yoqish kuchi",
            "Kuningiz davomida qora choy o'rniga shakarsiz yashil choy ichishga odatlaning. U metabolizmni va yog' yoqilishini tabiiy tezlashtiradi.",
            "Metabolizm",
            "🍵",
            "Yashil choy tarkibida \"katexin\" deb nomlangan faol moddalar mavjud. Bu moddalar jigar faoliyatini kuchaytirib, tanadagi qotib qolgan energiya (yog') hujayralarini parchalanish jarayoniga jalb qiladi. Ayniqsa, jismoniy mashg'ulot yoki yurishdan oldin ichilgan bir finjon yashil choy, oddiy vaqtga nisbatan 15% ko'proq yog' yoqilishiga olib keladi.",
            "Tushlikdan so'ng darhol bitta limonli yashil choy damlab iching."
        ),
        Tip(
            "Ovqat bilan birga ichmang",
            "Ovqatni chaynash vaqtida ustidan suv yoki choy ichib yutish – hazm tizimiga qilingan katta xiyonatdir. Ovqatni tuprik bilan chaynab yuting.",
            "Hazm qilish",
            "🍽️",
            "Tuprik tarkibida ovqatni parchalovchi birinchi fermentlar bor. Agar siz ovqatni suv bilan yutsangiz, tuprik ishga tushmaydi. Bundan tashqari, oshqozondagi ovqatni hazm qiluvchi kuchli kislota suv ta'sirida suyulib ketadi. Natijada ovqat hazm bo'lmasdan ichakda chiriydi, bu esa qorinning doimiy shishib yurishi (meteorizm) ga olib keladi.",
            "Ovqatlanish vaqtida choynagingizni stoldan uzoqroqqa olib qo'ying."
        ),
        Tip(
            "Shakarsiz (Zero) ichimliklar yolg'oni",
            "Tarkibida \"Shakarsiz\" yoki \"0 kaloriya\" deb yozilgan gazli ichimliklar (Zero Cola kabi) ozishga foyda bermaydi, aksincha xalaqit beradi.",
            "Sog'liq",
            "🥤",
            "Bu ichimliklarda shakar yo'q, lekin o'rniga sun'iy ta'm beruvchilar (aspartam) bor. Til shirin ta'mni sezganda, miya uglevod kutyapti deb o'ylab, me'da osti bezidan insulin ishlab chiqaradi. Lekin oshqozonga kaloriya tushmaydi. Aldangan tana endi kuchli ochlik hissini chaqiradi va siz bu ichimlikdan keyin boshqa narsalarni ko'p yeb qo'yasiz.",
            "Barcha gazli ichimliklarni voz kechib, ularni oddiy toza mineral suvga almashtiring."
        ),
        Tip(
            "Spirtli ichimliklar va yog' blokadasi",
            "Spirtli ichimliklarda o'ta ko'p kaloriya bor va ular tanada yog' yoqish jarayonini bir necha soatga to'liq to'xtatib qo'yadi.",
            "Ozish",
            "🍷",
            "Tana spirtni \"zahar\" deb qabul qiladi. Jigar unga duch kelganida, tanadagi barcha vazifalarni (jumladan yog'ni yoqishni ham) bir chetga surib, bor kuchini shu spirtni tanadan chiqarib tashlashga sarflaydi. Bu vaqt ichida siz yegan barcha ovqat, ayniqsa yog'li go'shtlar yoki uglevodlar to'g'ridan-to'g'ri qorin va sonlarga zahira sifatida yig'iladi.",
            "Tadbirlar va bayramlarda spirtli ichimlik o'rniga limon va yalpiz solingan mineral suvni tanlang."
        ),

        // 46-70: Harakat va Jismoniy faollik
        Tip(
            "Piyoda yurish mo'jizasi",
            "Ozish uchun qimmat fitness klublarga borib og'ir tosh ko'tarish shart emas. Har kuni muntazam piyoda yurishning o'zi eng katta samara beradi.",
            "Harakat",
            "🚶‍♂️",
            "Nega yugurishdan ko'ra yurish yaxshi? Qattiq yugurganda tana nafas qisishiga uchraydi va asosan uglevodlardan energiya oladi. Ammo xotirjam, davomiy va bir maromda yurganingizda yurak urishi ideal yog' yoqish zonasiga kiradi. Tana energiyani to'g'ridan-to'g'ri yog' hujayralaridan so'rib olishni boshlaydi. Tizzalarga ham zarar yetmaydi.",
            "Telefoningizda qadam o'lchagichni yoqib, kuniga 8-10 ming qadamni maqsad qiling."
        ),
        Tip(
            "Lift o'rniga zinapoya",
            "Harakatni ko'paytirishning eng oson siri – imkon tug'ilishi bilan lift yoki eskalatorlardan voz kechib, oddiy zinalardan ko'tarilishdir.",
            "Harakat",
            "🧗‍♀️",
            "Zinadan yuqoriga ko'tarilish yurishdan ko'ra tana uchun qiyinroq va bu ajoyib qisqa kardio mashg'ulot hisoblanadi. Bunda son va dumba (gavdadagi eng katta mushaklar) faol ishlaydi. Katta mushaklar ishlaganda, tana zaxiradagi energiyani tezroq yondirishga majbur bo'ladi. Bu ozish va qomatni shakllantirish uchun tekin trenajyordir.",
            "Bugundan boshlab uyingizga yoki ishxonangizga faqat zinalardan yurib chiqing."
        ),
        Tip(
            "Kechki ovqatdan keyingi sayr",
            "Kechki ovqatni yeb bo'lishingiz bilan divanga yotib televizor ko'rmang. Atigi 15 daqiqa ochiq havoda sayr qilish mo'jiza yaratadi.",
            "Ozish",
            "🌙",
            "Kechqurun tananing energiya sarfi pasayadi. Agar ovqatlanib darhol yotsangiz, qondagi shakar ko'tariladi va u yog'ga aylanadi. Yengil piyoda yurganingizda esa, mushaklar ishlashni boshlaydi va qonga chiqqan shakarni darhol o'ziga tortib, energiya sifatida yoqib yuboradi. Natijada kechki ovqat qoringa emas, harakatga sarflanadi.",
            "Ovqatlanib bo'lishingiz bilan oila a'zolaringiz bilan ko'chada biroz aylanib keling."
        ),
        Tip(
            "Uy ishlari va kaloriya",
            "Uy yig'ishtirish, pol yuvish yoki chang artish kabi kundalik uy ishlari aslida yog' yoqish uchun ajoyib fizik harakat hisoblanadi.",
            "Metabolizm",
            "🧹",
            "Yog'ni faqatgina sport zalda terlab yoqish mumkin degan tushuncha xato. Odam harakat qilyaptimi, o'z-o'zidan energiya (kaloriya) sarflanyapti degani. 1 soat davomida uyni faol tozalash, derazalarni artish taxminan 200-250 kaloriya yoqadi. Bu esa 40 daqiqa tinimsiz yugurish bilan deyarli bir xil ko'rsatkichdir.",
            "Dam olish kunlari uyni musiqa qo'yib, jismoniy mashg'ulot deb tasavvur qilib yig'ishtiring."
        ),
        Tip(
            "Uzoq o'tirish xavfi",
            "Agar ishingiz asosan kompyuter oldida o'tirish bo'lsa, tana metabolizmni sekinlashtirib uyqu rejimiga o'tadi. Har 45 daqiqada turing.",
            "Sog'liq",
            "⏱️",
            "Stulda qimirlamay o'tirganda oyoqlardagi eng katta mushaklar mutlaqo ishlamaydi va kaloriya yoqish eng past darajaga tushadi. Har soatda o'rningizdan turib atigi 2-3 daqiqa xona bo'ylab aylanish qon aylanishini tezlashtiradi, miyaga kislorod yetkazib beradi va tormozlangan moddalar almashinuvini qayta ishga tushiradi.",
            "Har soatda o'rningizdan turib, bir stakan suv ichib kelishni qoida qiling."
        ),
        Tip(
            "Ertalabki yengil yugurish siri",
            "Ertalab uyqudan turgach, nonushtadan oldin qilingan yengil yugurish yoki tez yurish yog'larni eritish uchun eng oltin vaqtdir.",
            "Ozish",
            "🏃",
            "Kechqurungi uyqu davomida tanada energiyaning asosiy manbai bo'lgan uglevodlar (glikogen) zaxirasi deyarli tugaydi. Agar siz ertalab och qoringa harakatni boshlasangiz, tanada tezkor energiya qolmagani uchun miya majbur bo'lib to'g'ridan-to'g'ri o'jar yog' zahiralaridan energiya olishni boshlaydi. Bu vaqtda yog' yoqish samaradorligi 300% ga oshadi.",
            "Ertalab och qoringa, 1 stakan suv ichib bo'lgach 15 daqiqa faol harakat qiling."
        ),
        Tip(
            "Nafas orqali ozish",
            "Tanadagi parchalanadigan yog'larning katta qismi qayerga ketadi deb o'ylaysiz? Ular karbonat angidrid ko'rinishida nafas bilan chiqib ketadi.",
            "Harakat",
            "🌬️",
            "Bu ilmiy fakt: 10 kg yog'ning 8.4 kg qismi o'pka orqali (nafas bilan), qolgan 1.6 kg qismi peshob va ter orqali chiqib ketadi. Chuqur va davomiy nafas olish qonga ko'proq kislorod yetkazib beradi. Kislorod ko'paysa, hujayralardagi yog'larning yonish (oksidlanish) jarayoni keskin faollashadi. Harakat vaqtida nafasingiz tezlashgani – yog'lar chiqib ketayotganidan dalolatdir.",
            "Ochiq havoda yoki ochiq deraza oldida 10 marta burundan chuqur nafas olib, og'izdan chiqaring."
        ),
        Tip(
            "Telefonda yurib gaplashish",
            "Telefoningizda jiringlaganda yoki o'zingiz kimgadir qo'ng'iroq qilganda, o'tirgan joyingizda emas, darhol turib aylanib yurib gaplashing.",
            "Harakat",
            "📱",
            "Biz odatda telefonda kuniga umumiy hisobda 30 daqiqadan 1 soatgacha vaqt o'tkazamiz. Agar siz bu vaqtda o'tirib olsangiz, hech qanday energiya sarflamaysiz. Lekin turib, uydami, ofisdami yoki hovlidami aylanib yurib gaplashsangiz, kunning oxiriga kelib o'zingiz sezmagan holda 2000-3000 qo'shimcha qadam tashlagan bo'lasiz.",
            "Telefon jiringlashi bilan o'rningizdan turib javob berishni odat qiling."
        ),
        Tip(
            "Mushaklar zaxirasi",
            "Kun davomida mushaklaringizni ishlashga majbur qiling. Mushaklar tanamizdagi eng ko'p energiya va yog' yondiruvchi mashinalardir.",
            "Ozish",
            "💪",
            "Mushaklar o'z faoliyatini va hajmini saqlab turish uchun tinimsiz energiya talab qiladi. Hatto siz divanda harakatsiz yotganingizda ham, agar tanangizda mushaklar faol bo'lsa, ular zaxiradagi yog'larni sekin-asta so'rib olishda davom etadi. Mushaklaringiz qanchalik ko'p ishlasa, tinch holatdagi metabolizmingiz (yog' yoqish tezligi) shuncha yuqori bo'ladi.",
            "Ertalab yoki ish o'rtasida 15-20 marta o'tirib-turish (prisedaniye) mashqini qiling."
        ),
        Tip(
            "Qomatingizni tik tutish",
            "Kun davomida o'tirganda va yurganda qomatni rostlash, yelkani tik tutish va qorinni biroz ichga tortish doimiy ko'rinmas kardiodir.",
            "Ozish",
            "🧍",
            "Yelkani osiltirib, qorinni tashlab o'tirish mushaklarni bo'shashtiradi va qorin sohasida yog' yig'ilishi osonlashadi. Qomatni doimiy tik tutib yurish esa umurtqa va qorin atrofidagi ichki (korset) mushaklarni doimiy taranaglikda ushlab turadi. Bu taranaglik nafaqat qorin osilishini to'xtatadi, balki tonusda ushlab turish uchun organizmdan qo'shimcha kaloriya tortib oladi.",
            "Oynaga ko'zingiz tushganda yoki xotiraga kelganda darhol yelkalarni orqaga tashlab qomatni rostlang."
        ),

        // 71-100: Psixologiya, Odatlar va Uyqu
        Tip(
            "Erta uxlash – ozish garovi",
            "Vazn tashlash va go'zal qomatning eng katta siri bu – vaqtida uxlashdir. Kechasi soat 23:00 dan kechikmay yotishni odat qiling.",
            "Uyqu",
            "😴",
            "Uyqu vaqtida tanamizda somatotropin degan muhim gormon ishlab chiqariladi. Bu gormon to'g'ridan-to'g'ri yog'larni parchalash vazifasini bajaradi va uning asosiy ish vaqti tungi soat 23:00 dan 02:00 gacha bo'ladi. Agar siz shu vaqtda telefonga qarab o'tirsangiz, bu jarayon ishga tushmaydi. Uyqu yetishmasa, ertasiga tana energiyani ovqatdan qidirib, kuchli ochlik hissini chaqiradi.",
            "Soat 22:30 bo'lishi bilan telefonni chetga qo'yib, chiroqlarni o'chiring."
        ),
        Tip(
            "To'q qorong'ilikda uxlash",
            "Uyqu vaqtida xonada umuman yorug'lik bo'lmasligi kerak. Hatto televizor yoki ko'chadagi chiroqning nuri ham yog' yoqilishini to'xtatadi.",
            "Uyqu",
            "🌒",
            "Yog'larni parchalovchi va asablarni tinchlantiruvchi \"melatonin\" gormoni faqatgina mutlaq qorong'ilikda ishlab chiqariladi. Tanamizdagi reseptorlar juda sezgir. Agar xonaga ozgina yorug'lik tushsa ham, miya buni \"tong otdi\" deb qabul qiladi va melatonin ishlab chiqarishni tormozlaydi. Natijada sizning qimmatli uyqu vaqtingiz yog' yoqishsiz o'tadi.",
            "Derazalaringizga qalin (nur o'tkazmaydigan) parda tuting va barcha chiroqlarni o'chiring."
        ),
        Tip(
            "Tishlarni yuvish hiylasi",
            "Kechki ovqatdan dasturxondan turishingiz bilan, darhol vannaxonaga kirib tishlaringizni yalpizli pasta bilan yuvishni odat qiling.",
            "Odatlar",
            "🪥",
            "Bu inson psixologiyasini boshqarishning ajoyib usuli. Og'izdagi tozalik hissi va yalpizning o'tkir ta'mi miyaga \"bugungi ovqatlanish jarayoni yakunlandi\" degan aniq signalni yuboradi. Shundan so'ng tana qo'shimcha shirinlik yoki meva yeyishni xohlamaydi. Tishini yuvgan odam erinchoqlik tufayli ham yana qayta ovqat yeyishga va tishini ikkinchi marta yuvishga harakat qilmaydi.",
            "Ovqat yeb bo'lishingiz bilan tish pastasi va cho'tkani ishlating."
        ),
        Tip(
            "Shirinliklarni ko'zdan panada saqlang",
            "Agar uyda shirinlik bo'lsa, uni ko'z tushadigan joyda (dasturxon yuzida) emas, balki shkafning ichida saqlash kerak.",
            "Psixologiya",
            "🙈",
            "\"Ko'zdan nari, ko'ngildan nari\". Odamning qorni to'q bo'lsa ham, agar uning ko'zi stol ustidagi chiroyli konfetga yoki pechenyega tushsa, miya beixtiyor shu narsani yeb ko'rish istagini uyg'otadi. Mahsulot ko'zga ko'rinmasa, miya uni eslamaydi va unga bo'lgan ehtiyoj nolga tushadi. Bo'sh kaloriyalarga undovchi narsalarni uzoqroqqa olib qo'ying.",
            "Stol ustidagi barcha shirinliklarni yig'ishtirib, o'rniga toza suv va yashil olmalar qo'ying."
        ),
        Tip(
            "Tarozi sizni aldamasin",
            "Vazningizni tekshirish uchun har kuni taroziga chiqmang. Bu faqat motivatsiyani tushiradi va asablarni buzadi. Haftada 1 marta tortiling.",
            "Psixologiya",
            "⚖️",
            "Inson vazni bir kunning o'zidayoq 1-2 kilogacha ko'tarilib-tushishi mumkin. Bu ko'pincha yog' emas, balki mushaklardagi suv, ichaklardagi ozuqa yoki kecha yeyilgan sho'r ovqat hisobiga ushlab qolingan suyuqlikdir. Ertalab tarozida kechagidan +500 gramm ko'rsangiz, tushkunlikka tushib dietani buzasiz. Aslida esa bu shunchaki suv bo'ladi.",
            "Haftaning faqat bitta kunini belgilab, ertalab och qoringa va hojatdan so'ng taroziga chiqing."
        ),
        Tip(
            "Kiyimlar - eng aniq o'lchov",
            "Ozayotganingizni bilishning eng to'g'ri usuli bu tarozi emas, balki sizning kiyimlaringiz va o'lchov tasmasidir (santimetr).",
            "Psixologiya",
            "📏",
            "Yog' va mushakning hajmi har xil. 1 kilogramm yog'ning hajmi, 1 kilogramm mushakka nisbatan 3 barobar katta! Siz to'g'ri ozayotganingizda, avvalambor hajmingiz kichrayadi. Tarozida atigi 1 kilo ozgan bo'lishingiz mumkin, lekin shimingiz sizga keng kela boshlaydi, chunki hajm ketdi. Tarozi sonlarini quvlamang, ko'zgu va kiyimlar sizga haqiqatni aytadi.",
            "Har 10 kunda bel, son va ko'krak aylanangizni santimetr lenta bilan o'lchab yozib boring."
        ),
        Tip(
            "Stressda oshxonadan uzoqroq bo'ling",
            "Asabiylashgan, charchagan yoki g'amgin vaqtingizda hech qachon muzlatgichni ochmang. Stress emotsiyalarini ovqat bilan bosmang.",
            "Psixologiya",
            "🤯",
            "Siqilgan vaqtingizda tanada kortizol (stress gormoni) keskin ko'tariladi. Kortizol miyadan tezroq shirin yoki yog'li narsa yeyishni talab qiladi, chunki bu vaqtinchalik xotirjamlik hissini (endorfin) beradi. Biroq, stress holatida yeyilgan hamma narsa to'g'ridan-to'g'ri qorin atrofidagi visseral yog'larga aylanadi. Yeb bo'lgach esa vijdon azobi boshlanadi va stress yana ortadi.",
            "Jahlingiz chiqqanda ovqat yeyish o'rniga, toza havoga chiqib 15 daqiqa tez-tez yuring yoki toza suv iching."
        ),
        Tip(
            "80/20 Qoidasi",
            "Ozish uchun sevimli taomlardan butunlay yuz o'girish shart emas. Ratsioningizning 80% sog'lom bo'lsa, 20% xohishlarga ruxsat bering.",
            "Ozish",
            "⚖️",
            "Qat'iy taqiqlar baribir qachondir asabni charchatadi va inson bor-yo'g'iga tupurib, yana eski ovqatlanish rejimiga qaytib oladi. Hayot bu marafon. Agar siz hafta davomida sog'lom ovqatlansangiz, suv ichsangiz va harakat qilsangiz, yakshanba kuni bitta kichik muzqaymoq yoki 1 bo'lak pitsa yeyishingiz qomatingizga hech qanday zarar bermaydi. Muhimi – me'yor.",
            "Bitta xato taom yeb qo'ysangiz o'zingizni ayblamang, ertasi kuni yana rejimda davom eting."
        )
    )

    fun isTipsEnabled(context: Context): Boolean = true

    fun getRandomTip(): Tip = tips.random()

    fun getTip(index: Int): Tip? = tips.getOrNull(index)

    fun getNewTipIndex(currentTipIndex: Int): Int {
        val newIndex = currentTipIndex + 1
        return if (newIndex >= tips.size) 0 else newIndex
    }

    @androidx.annotation.Keep
    @Parcelize
    data class Tip(
        val title: String,
        val content: String,
        val category: String,
        val emoji: String,
        val benefits: String,
        val actionableStep: String
    ) : Parcelable
}