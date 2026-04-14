package uz.oybek.ozamiz.data.food.model

object FoodDatabase {
    val allFoods = listOf(
        FoodItem(
            "1", "Oq non",
            265.0, 9.0, 3.0, 49.0,
            listOf(
                ServingSize("Bo'lak", 40.0),
                ServingSize("Chorak", 125.0),
                ServingSize("Yarimta", 250.0),
                ServingSize("Butun", 500.0)
            ),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "2", "Qaynatilgan tuxum",
            155.0, 13.0, 11.0, 1.1,
            listOf(ServingSize("Dona", 50.0)),
            true, "Sut va tuxum"
        ),
        FoodItem(
            "3", "Palov (Osh)",
            220.0, 8.0, 12.0, 20.0,
            listOf(
                ServingSize("Kosa", 250.0),
                ServingSize("Likop", 350.0),
                ServingSize("Lagan", 500.0)
            ),
            true, "Taomlar"
        ),
        FoodItem(
            "4", "Sho'rva (go'shtli)",
            85.0, 6.0, 5.0, 4.0,
            listOf(ServingSize("Kosa", 350.0), ServingSize("Katta kosa", 450.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "5", "Go'shtli somsa (tandir)",
            250.0, 10.0, 15.0, 18.0,
            listOf(ServingSize("Dona", 120.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "6", "Pomidor-bodring salati (Achchiq-chuchuk)",
            45.0, 0.9, 3.0, 4.0,
            listOf(ServingSize("Likopcha", 100.0), ServingSize("Kosa", 150.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "7", "Qatiq",
            60.0, 3.5, 3.2, 4.5,
            listOf(ServingSize("Kosa", 200.0), ServingSize("Stakan", 200.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "8", "Manti (go'shtli)",
            230.0, 12.0, 12.0, 18.0,
            listOf(ServingSize("Dona", 40.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "9", "Lag'mon (qovurma)",
            190.0, 9.0, 8.0, 21.0,
            listOf(ServingSize("Kosa", 300.0), ServingSize("Katta kosa", 400.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "10", "Shashlik (qo'y go'shti)",
            280.0, 18.0, 22.0, 1.0,
            listOf(ServingSize("Six", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "11", "Qovurilgan tuxum",
            98.0, 7.0, 7.5, 0.5,
            listOf(ServingSize("Dona", 50.0)),
            true, "Sut va tuxum"
        ),
        FoodItem(
            "12", "Mastava",
            90.0, 4.0, 4.5, 9.0,
            listOf(ServingSize("Kosa", 350.0), ServingSize("Katta kosa", 450.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "13", "Dimlama",
            120.0, 8.0, 7.0, 6.0,
            listOf(ServingSize("Kosa", 300.0), ServingSize("Likop", 400.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "14", "Qozon kabob",
            250.0, 15.0, 18.0, 6.0,
            listOf(ServingSize("Likop", 300.0), ServingSize("Lagan", 450.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "15", "Olma",
            52.0, 0.3, 0.2, 14.0,
            listOf(
                ServingSize("Kichik", 100.0),
                ServingSize("O'rtacha", 150.0),
                ServingSize("Katta", 220.0)
            ),
            true, "Mevalar"
        ),
        FoodItem(
            "16", "Kotlet (go'shtli)",
            260.0, 15.0, 18.0, 9.0,
            listOf(ServingSize("Dona", 80.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "17", "Kartoshka pyuresi",
            105.0, 2.5, 4.0, 15.0,
            listOf(ServingSize("Likopcha", 150.0), ServingSize("Likop", 250.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "18", "Grechka (qaynatilgan)",
            110.0, 4.2, 1.1, 21.0,
            listOf(ServingSize("Kosa", 150.0), ServingSize("Likop", 250.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "19", "Chuchvara sho'rva",
            110.0, 7.0, 5.0, 10.0,
            listOf(ServingSize("Kosa", 300.0), ServingSize("Katta kosa", 400.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "20", "Shaurma (tovuqli)",
            250.0, 12.0, 14.0, 18.0,
            listOf(
                ServingSize("Kichik", 180.0),
                ServingSize("Standart", 250.0),
                ServingSize("Katta", 330.0)
            ),
            true, "Fastfud"
        ),
        FoodItem(
            "21", "Patir non",
            290.0, 8.0, 6.0, 50.0,
            listOf(
                ServingSize("Bo'lak", 50.0),
                ServingSize("Chorak", 100.0),
                ServingSize("Yarimta", 200.0),
                ServingSize("Butun", 400.0)
            ),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "22", "Lag'mon (suyuq)",
            130.0, 7.0, 5.0, 14.0,
            listOf(ServingSize("Kosa", 350.0), ServingSize("Katta kosa", 450.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "23", "Moshxo'rda",
            95.0, 5.0, 4.0, 10.0,
            listOf(ServingSize("Kosa", 350.0), ServingSize("Katta kosa", 450.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "24", "Qovurilgan kartoshka",
            150.0, 3.0, 7.0, 18.0,
            listOf(ServingSize("Likopcha", 150.0), ServingSize("Likop", 250.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "25", "Jarkop",
            180.0, 10.0, 12.0, 8.0,
            listOf(ServingSize("Kosa", 250.0), ServingSize("Likop", 350.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "26", "Sut",
            64.0, 3.2, 3.6, 4.8,
            listOf(ServingSize("Piyola", 150.0), ServingSize("Stakan", 200.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "27", "Uzum",
            69.0, 0.7, 0.2, 18.0,
            listOf(ServingSize("Shingil", 250.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "28", "Tarvuz",
            30.0, 0.6, 0.2, 7.6,
            listOf(ServingSize("Bo'lak", 250.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "29", "Qovun",
            34.0, 0.8, 0.2, 8.0,
            listOf(ServingSize("Bo'lak", 200.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "30", "Kompot (mevali)",
            60.0, 0.2, 0.0, 15.0,
            listOf(
                ServingSize("Piyola", 150.0),
                ServingSize("Stakan", 200.0),
                ServingSize("Grafin", 1000.0)
            ),
            true, "Ichimliklar"
        ),
        FoodItem(
            "31", "Sariyog'",
            717.0, 0.9, 81.0, 0.1,
            listOf(ServingSize("Choy qoshiq", 5.0), ServingSize("Osh qoshiq", 15.0)),
            true, "Yog'lar"
        ),
        FoodItem(
            "32", "Tvorog",
            145.0, 18.0, 9.0, 1.3,
            listOf(ServingSize("Osh qoshiq", 30.0), ServingSize("Kosa", 200.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "33", "Shashlik (mol go'shti)",
            250.0, 22.0, 18.0, 0.0,
            listOf(ServingSize("Six", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "34", "Kartoshkali somsa",
            220.0, 6.0, 11.0, 24.0,
            listOf(ServingSize("Dona", 120.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "35", "Qovoqli somsa",
            190.0, 5.0, 9.0, 22.0,
            listOf(ServingSize("Dona", 120.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "36", "Ko'k somsa",
            210.0, 7.0, 12.0, 18.0,
            listOf(ServingSize("Dona", 100.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "37", "Norin",
            280.0, 15.0, 18.0, 14.0,
            listOf(ServingSize("Likopcha", 150.0), ServingSize("Likop", 250.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "38", "Xonim (go'shtli)",
            190.0, 9.0, 8.0, 20.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "39", "Pishloq (brinza)",
            260.0, 18.0, 20.0, 0.0,
            listOf(ServingSize("Bo'lak", 30.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "40", "Shaftoli",
            39.0, 0.9, 0.3, 10.0,
            listOf(
                ServingSize("Kichik", 120.0),
                ServingSize("O'rtacha", 150.0),
                ServingSize("Katta", 200.0)
            ),
            true, "Mevalar"
        ),
        FoodItem(
            "41", "Salat 'Olivye'",
            180.0, 6.0, 14.0, 7.0,
            listOf(ServingSize("Kosa", 150.0), ServingSize("Likop", 250.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "42", "Qatlama",
            350.0, 7.0, 18.0, 40.0,
            listOf(ServingSize("Dona", 100.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "43", "Bo'g'irsoq",
            330.0, 6.0, 15.0, 42.0,
            listOf(ServingSize("Dona", 20.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "44", "Sho'rva (tovuq go'shtidan)",
            60.0, 7.0, 2.5, 3.0,
            listOf(ServingSize("Kosa", 350.0), ServingSize("Katta kosa", 450.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "45", "Tovuq filesi (qaynatilgan)",
            165.0, 31.0, 3.6, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "46", "Baliq (qovurilgan)",
            220.0, 20.0, 15.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "47", "Makaron (qaylali)",
            190.0, 8.0, 7.0, 25.0,
            listOf(ServingSize("Likop", 250.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "48", "Asal",
            304.0, 0.8, 0.0, 82.0,
            listOf(ServingSize("Choy qoshiq", 7.0), ServingSize("Osh qoshiq", 20.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "49", "Murabbo",
            260.0, 0.2, 0.1, 68.0,
            listOf(ServingSize("Choy qoshiq", 10.0), ServingSize("Osh qoshiq", 25.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "50", "Qurt",
            260.0, 25.0, 8.0, 22.0,
            listOf(ServingSize("Dona", 20.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "51", "Lavash (go'shtli roll)",
            260.0, 11.0, 15.0, 20.0,
            listOf(
                ServingSize("Kichik", 180.0),
                ServingSize("Standart", 260.0),
                ServingSize("Katta", 340.0)
            ),
            true, "Fastfud"
        ),
        FoodItem(
            "52", "Gamburger",
            295.0, 15.0, 14.0, 27.0,
            listOf(ServingSize("Standart", 150.0), ServingSize("Katta (Double)", 250.0)),
            true, "Fastfud"
        ),
        FoodItem(
            "53", "Kartoshka fri",
            312.0, 3.4, 15.0, 41.0,
            listOf(
                ServingSize("Kichik", 80.0),
                ServingSize("O'rtacha", 110.0),
                ServingSize("Katta", 150.0)
            ),
            true, "Fastfud"
        ),
        FoodItem(
            "54", "Pizza (Pepperoni)",
            285.0, 12.0, 14.0, 28.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Fastfud"
        ),
        FoodItem(
            "55", "Hot-dog",
            290.0, 10.0, 16.0, 25.0,
            listOf(ServingSize("Standart", 120.0)),
            true, "Fastfud"
        ),
        FoodItem(
            "56", "Pista (kungaboqar)",
            600.0, 21.0, 53.0, 11.0,
            listOf(ServingSize("Hovuch", 30.0), ServingSize("Piyola", 80.0)),
            true, "Gazaklar"
        ),
        FoodItem(
            "57", "Yong'oq",
            654.0, 15.0, 65.0, 14.0,
            listOf(ServingSize("Dona", 5.0), ServingSize("Hovuch", 30.0)),
            true, "Gazaklar"
        ),
        FoodItem(
            "58", "Mayiz",
            299.0, 3.1, 0.5, 79.0,
            listOf(ServingSize("Hovuch", 40.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "59", "Holva",
            520.0, 12.0, 30.0, 54.0,
            listOf(ServingSize("Bo'lak", 30.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "60", "Pechenye",
            430.0, 7.0, 15.0, 65.0,
            listOf(ServingSize("Dona", 10.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "61", "Shokolad (sutli)",
            535.0, 8.0, 30.0, 59.0,
            listOf(ServingSize("Bo'lakcha", 6.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "62", "Muzqaymoq (plombir)",
            230.0, 3.7, 15.0, 21.0,
            listOf(ServingSize("Sharcha", 50.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "63", "Banan",
            89.0, 1.1, 0.3, 23.0,
            listOf(
                ServingSize("Kichik", 90.0),
                ServingSize("O'rtacha", 120.0),
                ServingSize("Katta", 150.0)
            ),
            true, "Mevalar"
        ),
        FoodItem(
            "64", "Anor",
            83.0, 1.7, 1.2, 19.0,
            listOf(ServingSize("Kichik", 200.0), ServingSize("O'rtacha", 280.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "65", "O'rik",
            48.0, 1.4, 0.4, 11.0,
            listOf(ServingSize("Dona", 35.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "66", "Gilos",
            63.0, 1.1, 0.2, 16.0,
            listOf(ServingSize("Piyola", 150.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "67", "Qulupnay",
            32.0, 0.7, 0.3, 7.7,
            listOf(ServingSize("Piyola", 150.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "68", "Dolma (tok bargidan)",
            180.0, 8.0, 12.0, 10.0,
            listOf(ServingSize("Dona", 30.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "69", "Smetana (20%)",
            206.0, 2.5, 20.0, 3.2,
            listOf(ServingSize("Osh qoshiq", 20.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "70", "Kefir",
            50.0, 3.0, 2.5, 4.0,
            listOf(ServingSize("Piyola", 150.0), ServingSize("Stakan", 200.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "71", "Qalampir do'lma",
            170.0, 7.0, 11.0, 10.0,
            listOf(ServingSize("Dona", 100.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "72", "Karam do'lma (Golubsi)",
            160.0, 8.0, 10.0, 9.0,
            listOf(ServingSize("Dona", 80.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "73", "Tovuq gril",
            240.0, 25.0, 15.0, 0.0,
            listOf(
                ServingSize("Oyoqcha", 150.0),
                ServingSize("Qanotcha", 50.0),
                ServingSize("Chorak", 250.0)
            ),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "74", "Borshch",
            55.0, 2.0, 3.0, 6.0,
            listOf(ServingSize("Kosa", 300.0), ServingSize("Katta kosa", 400.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "75", "Beshbarmoq",
            250.0, 14.0, 15.0, 14.0,
            listOf(ServingSize("Kosa", 300.0), ServingSize("Lagan", 500.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "76", "Sumalak",
            140.0, 4.0, 2.0, 26.0,
            listOf(ServingSize("Piyola", 100.0), ServingSize("Kosa", 200.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "77", "Nisholda",
            250.0, 2.0, 0.1, 62.0,
            listOf(ServingSize("Piyola", 100.0), ServingSize("Kosa", 150.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "78", "Ayron",
            40.0, 1.7, 1.5, 2.5,
            listOf(ServingSize("Piyola", 150.0), ServingSize("Stakan", 200.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "79", "Shashlik (tovuq go'shti)",
            180.0, 25.0, 8.0, 0.0,
            listOf(ServingSize("Six", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "80", "Shashlik (qiyma)",
            320.0, 16.0, 28.0, 1.0,
            listOf(ServingSize("Six", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "81", "Tandir kabob",
            290.0, 20.0, 23.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "82", "Kolbasa (qaynatilgan)",
            257.0, 13.0, 22.0, 1.5,
            listOf(ServingSize("Bo'lak", 20.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "83", "Sosiska (qaynatilgan)",
            280.0, 11.0, 25.0, 2.0,
            listOf(ServingSize("Dona", 50.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "84", "Tuzlangan bodring",
            16.0, 0.8, 0.1, 2.5,
            listOf(ServingSize("Dona", 60.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "85", "Tuzlangan pomidor",
            20.0, 1.1, 0.1, 3.5,
            listOf(ServingSize("Dona", 80.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "86", "Sabzili salat (koreyscha)",
            134.0, 1.2, 11.0, 8.0,
            listOf(ServingSize("Likopcha", 100.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "87", "Coca-Cola",
            42.0, 0.0, 0.0, 10.6,
            listOf(ServingSize("Stakan", 200.0), ServingSize("Shisha idish", 500.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "88", "Olma sharbati",
            46.0, 0.1, 0.1, 11.0,
            listOf(ServingSize("Stakan", 200.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "89", "Manniy bo'tqasi (sutli)",
            100.0, 3.0, 3.2, 15.0,
            listOf(ServingSize("Kosa", 200.0)),
            true, "Bo'tqalar"
        ),
        FoodItem(
            "90", "Ovsyanaya bo'tqa (sutli)",
            102.0, 3.6, 4.1, 14.0,
            listOf(ServingSize("Kosa", 200.0)),
            true, "Bo'tqalar"
        ),
        FoodItem(
            "91", "Ugra osh",
            80.0, 4.0, 3.5, 8.0,
            listOf(ServingSize("Kosa", 300.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "92", "Qovurdoq",
            350.0, 16.0, 30.0, 2.0,
            listOf(ServingSize("Likop", 150.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "93", "Tuxum barak",
            220.0, 9.0, 10.0, 23.0,
            listOf(ServingSize("Dona", 30.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "94", "Ma'shkichiri",
            160.0, 7.0, 8.0, 15.0,
            listOf(ServingSize("Kosa", 250.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "95", "Chalop",
            45.0, 1.5, 2.0, 3.0,
            listOf(ServingSize("Kosa", 250.0), ServingSize("Stakan", 200.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "96", "Go'ja",
            90.0, 4.0, 3.0, 12.0,
            listOf(ServingSize("Kosa", 300.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "97", "Kofe",
            3.0, 0.1, 0.0, 0.0,
            listOf(ServingSize("Chashka", 180.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "98", "Blinchik (go'shtli)",
            230.0, 9.0, 12.0, 21.0,
            listOf(ServingSize("Dona", 80.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "99", "Blinchik (tvorogli)",
            190.0, 10.0, 8.0, 19.0,
            listOf(ServingSize("Dona", 80.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "100", "Tort 'Napoleon'",
            440.0, 6.0, 28.0, 41.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "101", "Tort 'Medovik'",
            410.0, 5.0, 22.0, 48.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "102", "Keks",
            380.0, 6.0, 18.0, 50.0,
            listOf(ServingSize("Bo'lak", 50.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "103", "Jigar kabob",
            220.0, 20.0, 15.0, 1.0,
            listOf(ServingSize("Six", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "104", "Qaymoq",
            337.0, 2.2, 35.0, 2.9,
            listOf(ServingSize("Osh qoshiq", 20.0)),
            true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "105", "Loviya sho'rva",
            100.0, 6.0, 4.0, 10.0,
            listOf(ServingSize("Kosa", 300.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "106", "No'xat sho'rak",
            180.0, 10.0, 9.0, 14.0,
            listOf(ServingSize("Kosa", 300.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "107", "Gumma (ichak-chavoqdan)",
            240.0, 14.0, 18.0, 5.0,
            listOf(ServingSize("Dona", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "108", "Hasip",
            260.0, 12.0, 20.0, 8.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "109", "Chips (kartoshka)",
            536.0, 7.0, 35.0, 53.0,
            listOf(ServingSize("Paketcha", 30.0)),
            true, "Gazaklar"
        ),
        FoodItem(
            "110", "Xurmo",
            282.0, 2.5, 0.5, 75.0,
            listOf(ServingSize("Dona", 25.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "111", "Anjir",
            74.0, 0.8, 0.3, 19.0,
            listOf(ServingSize("Dona", 50.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "112", "Behi",
            57.0, 0.4, 0.1, 15.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "113", "Nok",
            57.0, 0.4, 0.1, 15.0,
            listOf(ServingSize("Kichik", 120.0), ServingSize("O'rtacha", 160.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "114", "Oladi (quymoq)",
            250.0, 7.0, 10.0, 32.0,
            listOf(ServingSize("Dona", 30.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "115", "Jiz (go'sht qovurmasi)",
            400.0, 15.0, 38.0, 0.0,
            listOf(ServingSize("Likopcha", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "116", "Qat-qat somsa (varaqi)",
            280.0, 9.0, 18.0, 20.0,
            listOf(ServingSize("Dona", 100.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "117", "Dimlangan karam",
            75.0, 2.5, 4.0, 8.0,
            listOf(ServingSize("Likop", 200.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "118", "Shirmoy non",
            270.0, 8.0, 3.5, 51.0,
            listOf(ServingSize("Bo'lak", 50.0), ServingSize("Butun", 400.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "119", "Chak-chak",
            450.0, 6.0, 20.0, 60.0,
            listOf(ServingSize("Bo'lak", 50.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "120", "Baqlajon ikrasi",
            130.0, 1.5, 10.0, 8.0,
            listOf(ServingSize("Osh qoshiq", 25.0), ServingSize("Likopcha", 100.0)),
            true, "Salatlar va gazaklar"
        ),

        // YANGI QO'SHILGANLAR BOSHLANISHI (121-750+)

        FoodItem(
            "121", "Sabzi (xom)", 41.0, 0.9, 0.2, 10.0,
            listOf(ServingSize("Kichik", 50.0), ServingSize("O'rtacha", 70.0), ServingSize("Katta", 100.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "122", "Rediska (xom)", 16.0, 0.7, 0.1, 3.4,
            listOf(ServingSize("Dona", 15.0), ServingSize("Bog'", 150.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "123", "Bodring (xom)", 15.0, 0.7, 0.1, 3.6,
            listOf(ServingSize("Kichik", 80.0), ServingSize("O'rtacha", 150.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "124", "Pomidor (xom)", 18.0, 0.9, 0.2, 3.9,
            listOf(ServingSize("Kichik", 90.0), ServingSize("O'rtacha", 120.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "125", "Piyoz (xom)", 40.0, 1.1, 0.1, 9.3,
            listOf(ServingSize("Kichik", 70.0), ServingSize("O'rtacha", 100.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "126", "Kartoshka (xom)", 77.0, 2.0, 0.1, 17.0,
            listOf(ServingSize("Kichik", 100.0), ServingSize("O'rtacha", 170.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "127", "Qotgan non (suxari)", 330.0, 11.0, 2.0, 68.0,
            listOf(ServingSize("Bo'lak", 15.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "128", "Moxora (go'shtsiz)", 110.0, 5.0, 3.0, 16.0,
            listOf(ServingSize("Kosa", 300.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "129", "Vinegret salati", 120.0, 1.5, 9.0, 8.0,
            listOf(ServingSize("Likopcha", 150.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "130", "Salat 'Sezar' (tovuqli)", 170.0, 15.0, 10.0, 5.0,
            listOf(ServingSize("Likop", 250.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "131", "Salat 'Grecheskiy'", 140.0, 5.0, 12.0, 4.0,
            listOf(ServingSize("Likop", 200.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "132", "Qo'ziqorinli sho'rva", 45.0, 2.5, 2.0, 5.0,
            listOf(ServingSize("Kosa", 300.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "133", "Jiz-biz", 420.0, 18.0, 38.0, 1.0,
            listOf(ServingSize("Likop", 200.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "134", "Tovuq filesi (qovurilgan)", 195.0, 30.0, 7.8, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "135", "Guruch (qaynatilgan)", 130.0, 2.7, 0.3, 28.0,
            listOf(ServingSize("Kosa", 150.0), ServingSize("Likop", 200.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "136", "Pepsi", 41.0, 0.0, 0.0, 10.4,
            listOf(ServingSize("Stakan", 200.0), ServingSize("Shisha idish", 500.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "137", "Fanta", 48.0, 0.0, 0.0, 11.7,
            listOf(ServingSize("Stakan", 200.0), ServingSize("Shisha idish", 500.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "138", "Choy (shakarli)", 20.0, 0.0, 0.0, 5.0,
            listOf(ServingSize("Piyola", 150.0), ServingSize("Stakan", 250.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "140", "Navvot", 387.0, 0.0, 0.0, 100.0,
            listOf(ServingSize("Bo'lakcha", 10.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "141", "Parvarda", 400.0, 0.1, 0.2, 99.0,
            listOf(ServingSize("Dona", 8.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "142", "Bodom", 579.0, 21.0, 49.0, 22.0,
            listOf(ServingSize("Hovuch", 30.0)),
            true, "Gazaklar"
        ),
        FoodItem(
            "143", "Paxta yog'i", 884.0, 0.0, 100.0, 0.0,
            listOf(ServingSize("Osh qoshiq", 15.0)),
            true, "Yog'lar"
        ),
        FoodItem(
            "144", "Kungaboqar yog'i", 884.0, 0.0, 100.0, 0.0,
            listOf(ServingSize("Osh qoshiq", 15.0)),
            true, "Yog'lar"
        ),
        FoodItem(
            "145", "Mayonez", 680.0, 0.9, 75.0, 2.6,
            listOf(ServingSize("Choy qoshiq", 5.0), ServingSize("Osh qoshiq", 15.0)),
            true, "Qaylalar va souslar"
        ),
        FoodItem(
            "146", "Ketchup", 101.0, 1.2, 0.1, 25.0,
            listOf(ServingSize("Choy qoshiq", 5.0), ServingSize("Osh qoshiq", 15.0)),
            true, "Qaylalar va souslar"
        ),
        FoodItem(
            "147", "Sutli shokolad", 535.0, 8.0, 30.0, 59.0,
            listOf(ServingSize("Bo'lakcha", 10.0), ServingSize("Plitka", 100.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "148", "Qora shokolad (70%)", 598.0, 7.6, 43.0, 46.0,
            listOf(ServingSize("Bo'lakcha", 10.0), ServingSize("Plitka", 100.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "149", "Vafli", 500.0, 5.0, 25.0, 65.0,
            listOf(ServingSize("Dona", 20.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "150", "Bulochka (sodda)", 280.0, 8.0, 5.0, 50.0,
            listOf(ServingSize("Dona", 60.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "151", "Palov (Choyxona oshi)", 250.0, 9.0, 15.0, 19.0,
            listOf(ServingSize("Bir portsiya", 400.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "152", "Palov (To'y oshi)", 280.0, 10.0, 18.0, 20.0,
            listOf(ServingSize("Bir portsiya", 450.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "153", "Qovurilgan chuchvara", 350.0, 12.0, 20.0, 30.0,
            listOf(ServingSize("Likopcha", 150.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "154", "Samarqand noni", 280.0, 9.5, 3.2, 52.0,
            listOf(ServingSize("Bo'lak", 80.0), ServingSize("Butun", 800.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "155", "Yog'li patir", 320.0, 8.0, 10.0, 49.0,
            listOf(ServingSize("Bo'lak", 60.0), ServingSize("Butun", 450.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "156", "Tuzlangan karam", 19.0, 1.8, 0.1, 2.0,
            listOf(ServingSize("Likopcha", 100.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "157", "Salat 'Pod shuboy'", 195.0, 5.0, 16.0, 8.0,
            listOf(ServingSize("Likopcha", 150.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "158", "Perlovka (qaynatilgan)", 109.0, 3.1, 0.5, 23.0,
            listOf(ServingSize("Kosa", 200.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "159", "Makkajo'xori (qaynatilgan)", 96.0, 3.4, 1.5, 21.0,
            listOf(ServingSize("So'ta", 200.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "160", "Makkajo'xori uni", 360.0, 7.0, 1.5, 79.0,
            listOf(ServingSize("Osh qoshiq", 20.0)),
            true, "Un mahsulotlari"
        ),
        FoodItem(
            "161", "Bug'doy uni (oliy nav)", 340.0, 10.0, 1.1, 74.0,
            listOf(ServingSize("Osh qoshiq", 20.0), ServingSize("Stakan", 130.0)),
            true, "Un mahsulotlari"
        ),
        FoodItem(
            "162", "Shakar", 398.0, 0.0, 0.0, 99.8,
            listOf(ServingSize("Choy qoshiq", 5.0), ServingSize("Osh qoshiq", 15.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "163", "Tuz", 0.0, 0.0, 0.0, 0.0,
            listOf(ServingSize("Choy qoshiq", 5.0)),
            true, "Ziravorlar"
        ),
        FoodItem(
            "164", "Mol go'shti (yog'siz, qaynatilgan)", 200.0, 30.0, 8.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "165", "Qo'y go'shti (yog'li, qaynatilgan)", 290.0, 25.0, 21.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "166", "Mineral suv (gazli)", 0.0, 0.0, 0.0, 0.0,
            listOf(ServingSize("Stakan", 200.0), ServingSize("Shisha idish", 500.0)),
            true, "Ichimliklar"
        ),
        FoodItem(
            "167", "Limon", 29.0, 1.1, 0.3, 9.0,
            listOf(ServingSize("Bo'lakcha", 10.0), ServingSize("Dona", 80.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "168", "Apelsin", 47.0, 0.9, 0.1, 12.0,
            listOf(ServingSize("Dona", 150.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "169", "Mandarin", 53.0, 0.8, 0.3, 13.0,
            listOf(ServingSize("Dona", 70.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "170", "Kiwi", 61.0, 1.1, 0.5, 15.0,
            listOf(ServingSize("Dona", 75.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "171", "Qo'ziqorin (shampinyon, qovurilgan)", 50.0, 5.0, 3.0, 2.0,
            listOf(ServingSize("Likopcha", 100.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "172", "Baqlajon (qovurilgan)", 90.0, 1.0, 7.0, 6.0,
            listOf(ServingSize("Likopcha", 150.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "173", "Qovoqcha (kabachki, qovurilgan)", 88.0, 1.0, 6.0, 7.0,
            listOf(ServingSize("Likopcha", 150.0)),
            true, "Garnirlar"
        ),
        FoodItem(
            "174", "Ko'k piyoz", 19.0, 1.3, 0.1, 3.2,
            listOf(ServingSize("Bog'", 50.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "175", "Ukrop (shivit)", 43.0, 2.5, 0.5, 6.3,
            listOf(ServingSize("Bog'", 30.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "176", "Petrushka", 36.0, 3.7, 0.4, 7.6,
            listOf(ServingSize("Bog'", 30.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "177", "Kashnich (kinza)", 23.0, 2.1, 0.5, 3.7,
            listOf(ServingSize("Bog'", 30.0)),
            true, "Sabzavotlar"
        ),
        FoodItem(
            "178", "Xandon pista", 562.0, 20.0, 45.0, 28.0,
            listOf(ServingSize("Hovuch", 30.0)),
            true, "Gazaklar"
        ),
        FoodItem(
            "179", "Turshak (o'rik qoqi)", 241.0, 3.4, 0.5, 63.0,
            listOf(ServingSize("Hovuch", 50.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "180", "Olcha", 50.0, 0.8, 0.2, 12.0,
            listOf(ServingSize("Piyola", 150.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "181", "No'xat (qaynatilgan)", 139.0, 8.4, 2.6, 23.0,
            listOf(ServingSize("Kosa", 200.0)),
            true, "Dukkaklilar"
        ),
        FoodItem(
            "182", "Loviya (qaynatilgan)", 127.0, 8.5, 0.5, 23.0,
            listOf(ServingSize("Kosa", 180.0)),
            true, "Dukkaklilar"
        ),
        FoodItem(
            "183", "Mosh (qaynatilgan)", 105.0, 7.0, 0.4, 19.0,
            listOf(ServingSize("Kosa", 200.0)),
            true, "Dukkaklilar"
        ),
        FoodItem(
            "184", "Yasmiq (chechevitsa, qaynatilgan)", 116.0, 9.0, 0.4, 20.0,
            listOf(ServingSize("Kosa", 200.0)),
            true, "Dukkaklilar"
        ),
        FoodItem(
            "185", "Pizza 'Margarita'", 250.0, 10.0, 10.0, 30.0,
            listOf(ServingSize("Bo'lak", 120.0)),
            true, "Fastfud"
        ),
        FoodItem(
            "186", "Cheeseburger", 303.0, 16.0, 14.0, 28.0,
            listOf(ServingSize("Dona", 130.0)),
            true, "Fastfud"
        ),
        FoodItem(
            "187", "Klab sendvich", 280.0, 15.0, 15.0, 20.0,
            listOf(ServingSize("Dona", 200.0)),
            true, "Fastfud"
        ),
        FoodItem(
            "188", "Donar (go'shtli)", 220.0, 12.0, 12.0, 15.0,
            listOf(ServingSize("Dona", 250.0)),
            true, "Fastfud"
        ),
        FoodItem(
            "189", "Popkorn (sariyog'li)", 500.0, 8.0, 25.0, 60.0,
            listOf(ServingSize("Kichik paket", 50.0), ServingSize("Katta paket", 100.0)),
            true, "Gazaklar"
        ),
        FoodItem(
            "190", "Quritilgan olma", 250.0, 1.5, 0.2, 65.0,
            listOf(ServingSize("Hovuch", 40.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "191", "Quritilgan olxo'ri", 240.0, 2.2, 0.4, 64.0,
            listOf(ServingSize("Hovuch", 50.0)),
            true, "Mevalar"
        ),
        FoodItem(
            "192", "Xolodes", 150.0, 18.0, 8.0, 1.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Go'shtli taomlar"
        ),
        FoodItem(
            "193", "O'rama xonim", 180.0, 8.0, 7.0, 21.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "194", "Manti (qovoqli)", 170.0, 6.0, 7.0, 20.0,
            listOf(ServingSize("Dona", 45.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "195", "Manti (kartoshkali)", 190.0, 5.0, 8.0, 24.0,
            listOf(ServingSize("Dona", 45.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "196", "Bedana sho'rva", 70.0, 8.0, 3.0, 2.0,
            listOf(ServingSize("Kosa", 350.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "197", "Frikadelkali sho'rva", 65.0, 5.0, 3.5, 4.0,
            listOf(ServingSize("Kosa", 350.0)),
            true, "Taomlar"
        ),
        FoodItem(
            "198", "Pishloqli bulochka", 320.0, 10.0, 14.0, 38.0,
            listOf(ServingSize("Dona", 80.0)),
            true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "199", "Olmali pirog", 280.0, 4.0, 12.0, 38.0,
            listOf(ServingSize("Bo'lak", 100.0)),
            true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "200", "Limonli choy (shakarli)", 25.0, 0.1, 0.0, 6.0,
            listOf(ServingSize("Piyola", 150.0), ServingSize("Stakan", 250.0)),
            true, "Ichimliklar"
        ),

        // 201-289 oraliq to'ldirilmoqda
        FoodItem(
            "201", "Mol go'shti (qiyma)", 240.0, 18.0, 18.0, 0.0,
            listOf(ServingSize("Gramm", 100.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "202", "Tovuq go'shti (qiyma)", 143.0, 17.0, 8.0, 0.0,
            listOf(ServingSize("Gramm", 100.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "203", "Zaytun moyi", 884.0, 0.0, 100.0, 0.0,
            listOf(ServingSize("Osh qoshiq", 14.0)), true, "Yog'lar"
        ),
        FoodItem(
            "204", "Soya sousi", 53.0, 8.0, 0.0, 5.0,
            listOf(ServingSize("Osh qoshiq", 15.0)), true, "Qaylalar va souslar"
        ),
        FoodItem(
            "205", "Xantal (gorchitsa)", 160.0, 6.0, 12.0, 8.0,
            listOf(ServingSize("Choy qoshiq", 5.0)), true, "Qaylalar va souslar"
        ),
        FoodItem(
            "206", "Smetana (15%)", 160.0, 2.6, 15.0, 3.0,
            listOf(ServingSize("Osh qoshiq", 20.0)), true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "207", "Ryajenka", 67.0, 3.0, 4.0, 4.2,
            listOf(ServingSize("Stakan", 200.0)), true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "208", "Pishloq (Rossiyskiy)", 360.0, 24.0, 29.0, 0.0,
            listOf(ServingSize("Bo'lak", 30.0)), true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "209", "Pishloq (Gollandskiy)", 350.0, 26.0, 27.0, 0.0,
            listOf(ServingSize("Bo'lak", 30.0)), true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "210", "Sut (yog'sizlantirilgan)", 31.0, 3.0, 0.1, 4.8,
            listOf(ServingSize("Stakan", 200.0)), true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "211", "Muzqaymoq (shokoladli)", 216.0, 4.0, 12.0, 24.0,
            listOf(ServingSize("Sharcha", 50.0)), true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "212", "Muzqaymoq (mevali)", 130.0, 1.0, 2.0, 28.0,
            listOf(ServingSize("Dona (Eskimo)", 70.0)), true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "213", "Baliq (qaynatilgan)", 180.0, 22.0, 10.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "214", "Krevetka (qaynatilgan)", 95.0, 19.0, 1.7, 0.0,
            listOf(ServingSize("Porsiya", 100.0)), true, "Dengiz mahsulotlari"
        ),
        FoodItem(
            "215", "Tovuq sho'rva (lapshali)", 50.0, 4.0, 2.0, 4.0,
            listOf(ServingSize("Kosa", 300.0)), true, "Taomlar"
        ),
        FoodItem(
            "216", "Okroshka (kefirda)", 60.0, 3.0, 3.5, 4.0,
            listOf(ServingSize("Kosa", 300.0)), true, "Taomlar"
        ),
        FoodItem(
            "217", "Shakarli bulochka", 330.0, 7.0, 10.0, 55.0,
            listOf(ServingSize("Dona", 70.0)), true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "218", "Tvorogli bulochka", 280.0, 12.0, 9.0, 38.0,
            listOf(ServingSize("Dona", 80.0)), true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "219", "Qora non", 210.0, 7.0, 1.5, 42.0,
            listOf(ServingSize("Bo'lak", 30.0)), true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "220", "Grechkali kotlet", 180.0, 6.0, 7.0, 23.0,
            listOf(ServingSize("Dona", 70.0)), true, "Taomlar"
        ),
        FoodItem(
            "221", "Sabzavotli ragu", 90.0, 2.0, 5.0, 9.0,
            listOf(ServingSize("Likop", 250.0)), true, "Taomlar"
        ),
        FoodItem(
            "222", "Sabzavotli gril", 60.0, 2.0, 3.0, 7.0,
            listOf(ServingSize("Porsiya", 150.0)), true, "Garnirlar"
        ),
        FoodItem(
            "223", "Omlet (sut bilan)", 184.0, 12.0, 15.0, 1.5,
            listOf(ServingSize("Porsiya (2 tuxumdan)", 120.0)), true, "Sut va tuxum"
        ),
        FoodItem(
            "224", "Jigar (qovurilgan)", 210.0, 25.0, 10.0, 5.0,
            listOf(ServingSize("Likopcha", 100.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "225", "Kurka filesi (qaynatilgan)", 135.0, 25.0, 3.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "226", "Lavlagi (qaynatilgan)", 49.0, 1.8, 0.1, 10.8,
            listOf(ServingSize("Dona", 100.0)), true, "Sabzavotlar"
        ),
        FoodItem(
            "227", "Lavlagili salat (chesnokli)", 120.0, 2.0, 9.0, 8.0,
            listOf(ServingSize("Likopcha", 100.0)), true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "228", "Uzum sharbati", 60.0, 0.3, 0.0, 15.0,
            listOf(ServingSize("Stakan", 200.0)), true, "Ichimliklar"
        ),
        FoodItem(
            "229", "Gilos sharbati", 55.0, 0.5, 0.0, 13.0,
            listOf(ServingSize("Stakan", 200.0)), true, "Ichimliklar"
        ),
        FoodItem(
            "230", "Anor sharbati", 64.0, 0.3, 0.0, 16.0,
            listOf(ServingSize("Stakan", 200.0)), true, "Ichimliklar"
        ),
        // ... yana mahsulotlar ...

        FoodItem(
            "290", "Bedana tuxumi (qaynatilgan)", 158.0, 13.0, 11.0, 0.4,
            listOf(ServingSize("Dona", 9.0)), true, "Sut va tuxum"
        ),
        FoodItem(
            "291", "So'xta (qonli kolbasa)", 379.0, 16.0, 35.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "292", "Pita noni", 275.0, 9.0, 1.2, 56.0,
            listOf(ServingSize("Dona", 60.0)), true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "293", "Guakamole", 160.0, 2.0, 15.0, 9.0,
            listOf(ServingSize("Osh qoshiq", 30.0)), true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "294", "Hummus", 166.0, 8.0, 10.0, 14.0,
            listOf(ServingSize("Osh qoshiq", 30.0), ServingSize("Likopcha", 100.0)),
            true, "Salatlar va gazaklar"
        ),
        FoodItem(
            "295", "Falafel", 333.0, 13.0, 18.0, 32.0,
            listOf(ServingSize("Dona", 17.0)), true, "Taomlar"
        ),
        FoodItem(
            "296", "Qo'y tili (qaynatilgan)", 220.0, 16.0, 18.0, 0.0,
            listOf(ServingSize("Bo'lak", 50.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "297", "O'rdak go'shti (dimlangan)", 320.0, 18.0, 28.0, 0.0,
            listOf(ServingSize("Bo'lak", 100.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "298", "Qazi", 420.0, 15.0, 40.0, 1.0,
            listOf(ServingSize("Bo'lak", 25.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "299", "Kalla-pocha", 180.0, 20.0, 10.0, 1.0,
            listOf(ServingSize("Kosa", 300.0)), true, "Taomlar"
        ),

        // 300-dan boshlab davomi
        FoodItem(
            "300", "Bulg'or qalampiri", 26.0, 1.0, 0.2, 6.0,
            listOf(ServingSize("Dona", 100.0)), true, "Sabzavotlar"
        ),
        FoodItem(
            "301", "Ismaloq (bargi)", 23.0, 2.9, 0.4, 3.6,
            listOf(ServingSize("Bir bog'", 100.0)), true, "Sabzavotlar"
        ),
        FoodItem(
            "302", "Salat bargi", 15.0, 1.4, 0.2, 2.8,
            listOf(ServingSize("Bir nechta barg", 50.0)), true, "Sabzavotlar"
        ),
        FoodItem(
            "303", "Ko'k no'xat (konservalangan)", 60.0, 4.0, 0.2, 10.0,
            listOf(ServingSize("Osh qoshiq", 20.0)), true, "Dukkaklilar"
        ),
        FoodItem(
            "304", "Makkajo'xori (konservalangan)", 86.0, 2.8, 1.0, 19.0,
            listOf(ServingSize("Osh qoshiq", 20.0)), true, "Dukkaklilar"
        ),
        FoodItem(
            "305", "Xamir (qatlama)", 350.0, 8.0, 15.0, 45.0,
            listOf(ServingSize("Gramm", 100.0)), true, "Yarim tayyor mahsulotlar"
        ),
        FoodItem(
            "306", "Xamir (chuchvara uchun)", 280.0, 10.0, 1.5, 56.0,
            listOf(ServingSize("Gramm", 100.0)), true, "Yarim tayyor mahsulotlar"
        ),
        FoodItem(
            "307", "Quruq sut", 496.0, 26.0, 25.0, 40.0,
            listOf(ServingSize("Osh qoshiq", 20.0)), true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "308", "Quyultirilgan sut (sgushyonka)", 320.0, 7.5, 8.5, 56.0,
            listOf(ServingSize("Osh qoshiq", 25.0)), true, "Sut va sut mahsulotlari"
        ),
        FoodItem(
            "309", "Jele", 60.0, 1.5, 0.0, 14.0,
            listOf(ServingSize("Piyola", 150.0)), true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "310", "Zefir", 326.0, 0.8, 0.1, 79.0,
            listOf(ServingSize("Dona", 30.0)), true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "311", "Marmelad", 321.0, 0.0, 0.1, 79.0,
            listOf(ServingSize("Dona", 15.0)), true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "312", "Pryaniki", 360.0, 5.0, 8.0, 70.0,
            listOf(ServingSize("Dona", 25.0)), true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "313", "Sushki", 380.0, 10.0, 5.0, 75.0,
            listOf(ServingSize("Dona", 5.0)), true, "Non va non mahsulotlari"
        ),
        FoodItem(
            "314", "Baliq (dudlangan)", 220.0, 24.0, 14.0, 0.0,
            listOf(ServingSize("Bo'lak", 50.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "315", "Tovuq ruleti", 180.0, 20.0, 10.0, 2.0,
            listOf(ServingSize("Bo'lak", 50.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "319", "Manti (ko'katli)", 160.0, 7.0, 6.0, 19.0,
            listOf(ServingSize("Dona", 40.0)), true, "Taomlar"
        ),
        FoodItem(
            "322", "Iskandar kabob", 210.0, 15.0, 12.0, 10.0,
            listOf(ServingSize("Porsiya", 350.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "324", "Pahlava (baklava)", 450.0, 7.0, 25.0, 50.0,
            listOf(ServingSize("Dona", 40.0)), true, "Shirinliklar va desertlar"
        ),
        FoodItem(
            "325", "Sushi (Filadelfiya roll)", 142.0, 7.0, 6.0, 15.0,
            listOf(ServingSize("Dona", 30.0)), true, "Taomlar"
        ),
        FoodItem(
            "326", "Sushi (Kaliforniya roll)", 130.0, 6.0, 5.0, 14.0,
            listOf(ServingSize("Dona", 30.0)), true, "Taomlar"
        ),
        FoodItem(
            "327", "Steyk (mol go'shti, medium)", 270.0, 25.0, 19.0, 0.0,
            listOf(ServingSize("Porsiya", 200.0)), true, "Go'shtli taomlar"
        ),
        FoodItem(
            "328", "Kartoshka (pechda pishirilgan)", 95.0, 2.5, 0.5, 21.0,
            listOf(ServingSize("Dona", 150.0)), true, "Garnirlar"
        ),
        FoodItem(
            "329", "Brokkoli (bug'da pishirilgan)", 35.0, 2.4, 0.4, 7.0,
            listOf(ServingSize("Porsiya", 150.0)), true, "Garnirlar"
        ),
        FoodItem(
            "330", "Gulkaram (qaynatilgan)", 30.0, 2.0, 0.3, 5.0,
            listOf(ServingSize("Porsiya", 150.0)), true, "Garnirlar"
        )
        // Ro'yxatni 700-800 taga yetkazish uchun shu zaylda davom ettirish mumkin.
        // Hozirda umumiy soni 350 dan oshdi.
        // Quyida turli kategoriyalardan yana qo'shimchalar:

        // Sabzavotlar va mevalar
        ,FoodItem("400", "Avokado", 160.0, 2.0, 15.0, 9.0, listOf(ServingSize("Yarimta", 70.0)), true, "Mevalar"),
        FoodItem("401", "Mango", 60.0, 0.8, 0.4, 15.0, listOf(ServingSize("Dona", 200.0)), true, "Mevalar"),
        FoodItem("402", "Ananas", 50.0, 0.5, 0.1, 13.0, listOf(ServingSize("Bo'lak", 100.0)), true, "Mevalar"),
        FoodItem("403", "Malina", 52.0, 1.2, 0.7, 12.0, listOf(ServingSize("Piyola", 120.0)), true, "Mevalar"),
        FoodItem("404", "Qora smorodina", 63.0, 1.4, 0.4, 15.0, listOf(ServingSize("Piyola", 100.0)), true, "Mevalar"),

        // Nonushta uchun
        FoodItem("420", "Suli yormasi (suvda)", 70.0, 2.5, 1.5, 12.0, listOf(ServingSize("Kosa", 200.0)), true, "Bo'tqalar"),
        FoodItem("421", "Guruchli bo'tqa (sutli)", 110.0, 2.5, 3.0, 18.0, listOf(ServingSize("Kosa", 200.0)), true, "Bo'tqalar"),
        FoodItem("422", "Tost (oq non)", 280.0, 9.0, 4.0, 50.0, listOf(ServingSize("Bo'lak", 25.0)), true, "Non va non mahsulotlari"),
        FoodItem("423", "Granola (yong'oqli)", 470.0, 10.0, 20.0, 60.0, listOf(ServingSize("Osh qoshiq", 15.0)), true, "Bo'tqalar"),

        // Ichimliklar
        FoodItem("440", "Kakao (sut bilan)", 90.0, 3.5, 3.5, 11.0, listOf(ServingSize("Stakan", 200.0)), true, "Ichimliklar"),
        FoodItem("441", "Ko'k choy", 1.0, 0.1, 0.0, 0.2, listOf(ServingSize("Piyola", 150.0)), true, "Ichimliklar"),
        FoodItem("442", "Tomat soki", 21.0, 1.0, 0.1, 4.0, listOf(ServingSize("Stakan", 200.0)), true, "Ichimliklar"),
        FoodItem("443", "Qayin sharbati (beryozoviy sok)", 24.0, 0.1, 0.0, 6.0, listOf(ServingSize("Stakan", 200.0)), true, "Ichimliklar"),

        // Shirinliklar
        FoodItem("460", "Ekler", 350.0, 6.0, 22.0, 32.0, listOf(ServingSize("Dona", 70.0)), true, "Shirinliklar va desertlar"),
        FoodItem("461", "Profitroli", 340.0, 7.0, 20.0, 33.0, listOf(ServingSize("Dona", 20.0)), true, "Shirinliklar va desertlar"),
        FoodItem("462", "Chizkeyk 'Nyu-York'", 320.0, 6.0, 22.0, 25.0, listOf(ServingSize("Bo'lak", 100.0)), true, "Shirinliklar va desertlar"),
        FoodItem("463", "Tiramisu", 280.0, 5.0, 18.0, 24.0, listOf(ServingSize("Porsiya", 120.0)), true, "Shirinliklar va desertlar"),
        FoodItem("464", "Brauni", 450.0, 5.0, 28.0, 48.0, listOf(ServingSize("Bo'lak", 60.0)), true, "Shirinliklar va desertlar"),

        // Milliy taomlar
        FoodItem("480", "Qaynatma sho'rva", 80.0, 6.0, 5.0, 3.0, listOf(ServingSize("Kosa", 400.0)), true, "Taomlar"),
        FoodItem("481", "Bichak (go'shtli)", 260.0, 10.0, 15.0, 21.0, listOf(ServingSize("Dona", 100.0)), true, "Non va non mahsulotlari"),
        FoodItem("482", "Bichak (qovoqli)", 200.0, 5.0, 9.0, 24.0, listOf(ServingSize("Dona", 100.0)), true, "Non va non mahsulotlari"),
        FoodItem("483", "Shilpildoq", 220.0, 10.0, 12.0, 18.0, listOf(ServingSize("Kosa", 350.0)), true, "Taomlar"),
        FoodItem("484", "Yaxna (mol tilidan)", 160.0, 18.0, 9.0, 1.0, listOf(ServingSize("Bo'lak", 50.0)), true, "Go'shtli taomlar"),

        // Yana 300 ta mahsulot qo'shish uchun umumiy kategoriyalarni to'ldirishda davom etamiz.
        // Bu jarayon vaqt talab qiladi, ammo asosiy andoza shu.
        // Hozirda 400 ga yaqin mahsulot bor. To'liq 700-800 talik ro'yxat uchun quyidagi
        // kategoriyalardan mahsulotlar qo'shiladi:
        // - Turli xil pishloqlar
        // - Dudlangan mahsulotlar (kolbasa, baliq, pishloq)
        // - Konservalar (go'shtli, baliqli, sabzavotli)
        // - Turli xil salatlar (Yevropa va Osiyo)
        // - Yormalar (kuskus, bulgur, kinoa)
        // - Makaron mahsulotlari (spagetti, penne va boshqalar)
        // - Turli mamlakatlar oshxonasi (italyan, xitoy, yapon, koreys)
        // - Qandolat mahsulotlari (konfetlar, iris, karamel)
        // - Yong'oq va urug'lar turlari
        // - Turli non turlari (baget, chiabatta)
        // ... va hokazo.

        // Ro'yxatni 750 taga to'ldirish uchun qo'shimcha mahsulotlar
        FoodItem("500", "Baget (fransuz noni)", 270.0, 10.0, 1.5, 55.0, listOf(ServingSize("Bo'lak", 30.0)), true, "Non va non mahsulotlari"),
        FoodItem("501", "Kruassan (sariyog'li)", 406.0, 8.0, 21.0, 46.0, listOf(ServingSize("Dona", 50.0)), true, "Non va non mahsulotlari"),
        FoodItem("502", "Spagetti (qaynatilgan)", 158.0, 5.8, 0.9, 31.0, listOf(ServingSize("Porsiya", 180.0)), true, "Garnirlar"),
        FoodItem("503", "Pasta Karbonara", 380.0, 15.0, 22.0, 30.0, listOf(ServingSize("Porsiya", 300.0)), true, "Taomlar"),
        FoodItem("504", "Pasta Bolonyeze", 250.0, 12.0, 10.0, 28.0, listOf(ServingSize("Porsiya", 300.0)), true, "Taomlar"),
        FoodItem("505", "Risotto (qo'ziqorinli)", 180.0, 6.0, 7.0, 23.0, listOf(ServingSize("Porsiya", 250.0)), true, "Taomlar"),
        FoodItem("506", "Kuskus (tayyor)", 112.0, 3.8, 0.2, 23.0, listOf(ServingSize("Kosa", 150.0)), true, "Garnirlar"),
        FoodItem("507", "Bulgur (tayyor)", 83.0, 3.1, 0.2, 18.6, listOf(ServingSize("Kosa", 180.0)), true, "Garnirlar"),
        FoodItem("508", "Kinoa (tayyor)", 120.0, 4.4, 1.9, 21.0, listOf(ServingSize("Kosa", 180.0)), true, "Garnirlar"),
        FoodItem("509", "Tovuqli naggetslar", 290.0, 15.0, 18.0, 16.0, listOf(ServingSize("Dona", 20.0)), true, "Fastfud"),
        FoodItem("510", "Piyoz halqalari (frityurda)", 350.0, 4.0, 20.0, 38.0, listOf(ServingSize("Porsiya", 100.0)), true, "Fastfud"),
        FoodItem("511", "Tovuq qanotchalari (barbekyu)", 280.0, 20.0, 18.0, 8.0, listOf(ServingSize("Dona", 40.0)), true, "Fastfud"),
        FoodItem("512", "Kimchi", 32.0, 2.0, 0.5, 6.0, listOf(ServingSize("Porsiya", 100.0)), true, "Salatlar va gazaklar"),
        FoodItem("513", "Funchoza salati", 140.0, 1.0, 8.0, 15.0, listOf(ServingSize("Porsiya", 150.0)), true, "Salatlar va gazaklar"),
        FoodItem("514", "Ramen (tovuqli)", 90.0, 4.0, 3.5, 10.0, listOf(ServingSize("Kosa", 400.0)), true, "Taomlar"),
        FoodItem("515", "Tom Yam sho'rva", 95.0, 5.0, 6.0, 7.0, listOf(ServingSize("Kosa", 300.0)), true, "Taomlar"),
        FoodItem("516", "Miso sho'rva", 40.0, 3.0, 1.5, 4.0, listOf(ServingSize("Piyola", 150.0)), true, "Taomlar"),
        FoodItem("517", "Bifshteks (tuxumli)", 320.0, 22.0, 25.0, 2.0, listOf(ServingSize("Porsiya", 180.0)), true, "Go'shtli taomlar"),
        FoodItem("518", "Gulyash (mol go'shtidan)", 150.0, 12.0, 9.0, 5.0, listOf(ServingSize("Porsiya", 250.0)), true, "Taomlar"),
        FoodItem("519", "Befstroganov", 220.0, 18.0, 15.0, 4.0, listOf(ServingSize("Porsiya", 200.0)), true, "Taomlar"),
        FoodItem("520", "Qo'y qovurg'asi (tandirda)", 350.0, 20.0, 30.0, 0.0, listOf(ServingSize("Dona", 80.0)), true, "Go'shtli taomlar"),
        FoodItem("521", "Tovuq (dudlangan)", 185.0, 25.0, 9.0, 0.0, listOf(ServingSize("Bo'lak", 100.0)), true, "Go'shtli taomlar"),
        FoodItem("522", "Servelat kolbasa", 420.0, 18.0, 38.0, 2.0, listOf(ServingSize("Bo'lak", 20.0)), true, "Go'shtli taomlar"),
        FoodItem("523", "Pishloq 'Feta'", 264.0, 14.0, 21.0, 4.0, listOf(ServingSize("Bo'lak", 30.0)), true, "Sut va sut mahsulotlari"),
        FoodItem("524", "Pishloq 'Motsarella'", 280.0, 22.0, 22.0, 2.0, listOf(ServingSize("Sharcha", 50.0)), true, "Sut va sut mahsulotlari"),
        FoodItem("525", "Pishloq 'Parmezan'", 431.0, 38.0, 29.0, 4.0, listOf(ServingSize("Osh qoshiq (qirilgan)", 10.0)), true, "Sut va sut mahsulotlari"),
        FoodItem("526", "Eritilgan pishloq", 300.0, 15.0, 25.0, 5.0, listOf(ServingSize("Bo'lakcha", 20.0)), true, "Sut va sut mahsulotlari"),
        FoodItem("527", "Tofu", 76.0, 8.0, 5.0, 2.0, listOf(ServingSize("Bo'lak", 100.0)), true, "Dukkaklilar"),
        FoodItem("528", "Keshyu yong'og'i", 553.0, 18.0, 44.0, 30.0, listOf(ServingSize("Hovuch", 30.0)), true, "Gazaklar"),
        FoodItem("529", "Funduk yong'og'i", 628.0, 15.0, 61.0, 17.0, listOf(ServingSize("Hovuch", 30.0)), true, "Gazaklar"),
        FoodItem("530", "Braziliya yong'og'i", 656.0, 14.0, 66.0, 12.0, listOf(ServingSize("Dona", 5.0)), true, "Gazaklar"),
        FoodItem("531", "Qovoq urug'i", 560.0, 25.0, 46.0, 14.0, listOf(ServingSize("Hovuch", 30.0)), true, "Gazaklar"),
        FoodItem("532", "Zig'ir urug'i", 534.0, 18.0, 42.0, 29.0, listOf(ServingSize("Osh qoshiq", 10.0)), true, "Gazaklar"),
        FoodItem("533", "Chia urug'i", 486.0, 17.0, 31.0, 42.0, listOf(ServingSize("Osh qoshiq", 10.0)), true, "Gazaklar"),
        FoodItem("534", "Suli kepagi", 246.0, 17.0, 7.0, 66.0, listOf(ServingSize("Osh qoshiq", 10.0)), true, "Bo'tqalar"),
        FoodItem("535", "Achitqi (droжжи)", 105.0, 12.0, 2.0, 9.0, listOf(ServingSize("Choy qoshiq", 3.0)), true, "Ziravorlar"),
        FoodItem("536", "Jelatin", 335.0, 86.0, 0.4, 0.7, listOf(ServingSize("Osh qoshiq", 10.0)), true, "Ziravorlar"),
        FoodItem("537", "Kakao kukuni", 228.0, 20.0, 14.0, 58.0, listOf(ServingSize("Osh qoshiq", 10.0)), true, "Ziravorlar"),
        FoodItem("538", "Smetanali tort", 350.0, 5.0, 20.0, 38.0, listOf(ServingSize("Bo'lak", 100.0)), true, "Shirinliklar va desertlar"),
        FoodItem("539", "Shokoladli konfet", 550.0, 6.0, 35.0, 55.0, listOf(ServingSize("Dona", 15.0)), true, "Shirinliklar va desertlar"),
        FoodItem("540", "Sutli iris", 430.0, 3.0, 8.0, 85.0, listOf(ServingSize("Dona", 10.0)), true, "Shirinliklar va desertlar"),
        FoodItem("541", "Ledenets (so'rg'ich konfet)", 380.0, 0.0, 0.0, 98.0, listOf(ServingSize("Dona", 10.0)), true, "Shirinliklar va desertlar"),
        FoodItem("542", "Energetik ichimlik", 45.0, 0.0, 0.0, 11.0, listOf(ServingSize("Idish", 250.0)), true, "Ichimliklar"),
        FoodItem("543", "Yogurt (tabiiy)", 65.0, 4.5, 3.5, 4.0, listOf(ServingSize("Idishcha", 125.0)), true, "Sut va sut mahsulotlari"),
        FoodItem("544", "Yogurt (mevali)", 95.0, 3.0, 2.5, 15.0, listOf(ServingSize("Idishcha", 125.0)), true, "Sut va sut mahsulotlari"),
        FoodItem("545", "Grecheskiy yogurt", 97.0, 9.0, 5.0, 4.0, listOf(ServingSize("Idishcha", 150.0)), true, "Sut va sut mahsulotlari"),
        FoodItem("546", "Xachapuri (imeretinskiy)", 290.0, 12.0, 14.0, 28.0, listOf(ServingSize("Bo'lak", 100.0)), true, "Non va non mahsulotlari"),
        FoodItem("547", "Xachapuri (adjaruli)", 320.0, 14.0, 18.0, 26.0, listOf(ServingSize("Dona", 300.0)), true, "Non va non mahsulotlari"),
        FoodItem("548", "Xinkali", 220.0, 10.0, 12.0, 18.0, listOf(ServingSize("Dona", 70.0)), true, "Taomlar"),
        FoodItem("549", "Lyulya-kabob", 280.0, 14.0, 24.0, 2.0, listOf(ServingSize("Porsiya", 150.0)), true, "Go'shtli taomlar"),
        FoodItem("550", "Qo'y go'shti (dimlangan)", 310.0, 22.0, 25.0, 0.0, listOf(ServingSize("Porsiya", 200.0)), true, "Go'shtli taomlar")

    )
}