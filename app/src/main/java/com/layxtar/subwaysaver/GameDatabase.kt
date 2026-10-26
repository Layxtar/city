package com.layxtar.subwaysaver

data class CitySkinInfo(
    val dataTag: Long,
    val key: String,
    val title: String,
    val rarity: Int
)

object GameDatabase {

    // Исправленные теги валют Subway Surfers City (Золото = -633437229L, Билеты = -635142441L, Возрождения = Банки)
    val CURRENCIES = listOf(
        -633437229L to "🪙 Монеты / Золото (Coins)",
        -2008679869L to "🔑 Ключи (Keys)",
        1669706535L to "🥫 Банки возрождения (Revive Cans)",
        -635142441L to "🎫 Билеты испытаний (Event / Trial Tickets)",
        -1085419483L to "🍬 Конфеты / Жетоны скинов (Skin Candies)",
        -1878560402L to "🎟️ Жетоны сёрферов (Surfer Tokens)",
        -1509662453L to "🛹 Жетоны досок (Board Tokens)",
        326719861L to "⚡ Энергия / Батарейки (Energy)",
        722065917L to "⭐ Кредиты сёрферов (Surfer Credits)"
    )

    // Все 27 сёрферов из актуального surfers.json и boards.json
    val SURFERS = listOf(
        -1836944478L to "Jake (Джейк)",
        1900660162L to "Tricky (Трики)",
        2129411796L to "Fresh (Фреш)",
        1614866432L to "Miss Maia (Мисс Майя)",
        1804257387L to "Ninja One (Ниндзя)",
        1663244716L to "Yutani (Ютани)",
        849273384L to "Spike (Спайк)",
        1200047034L to "Ella (Элла)",
        1120354844L to "Jay / Nicky (Джей)",
        -502265868L to "Noon (Нун)",
        581326566L to "Billy (Билли)",
        -1505268145L to "Rosalita (Розалита)",
        299562833L to "Tasha (Таша)",
        -1733051898L to "Jaewoo (Джэу)",
        1887684367L to "Tagbot (Тагбот)",
        852717139L to "Lucy (Люси)",
        -1534276928L to "Georgie (Джорджи)",
        -2125407733L to "V3ctor (Вектор)",
        823378763L to "Zara (Зара)",
        -518167090L to "Lilah (Лайла)",
        -2075784936L to "Ash (Эш)",
        1936280213L to "Jenny (Дженни)",
        -2116248615L to "Wei (Вэй)",
        966716028L to "Prince K (Принц Кей)",
        1363767693L to "Monique (Моник)",
        135046766L to "Harini (Харини)",
        -496667767L to "Rey Romeo (Рэй Ромео)"
    )

    // Полная привязка скинов к каждому персонажу из актуального surferskins.json
    val SURFER_SKINS_MAP: Map<Long, List<CitySkinInfo>> = mapOf(
        // 1. Jake
        -1836944478L to listOf(
            CitySkinInfo(-56800546L, "skins.jake.jake_default", "Default (Стандартный)", 0),
            CitySkinInfo(-960954917L, "skins.jake.jake_redblack", "Red & Black (Красно-чёрный)", 1),
            CitySkinInfo(213457242L, "skins.jake.jake_google", "Google Play Exclusive", 3),
            CitySkinInfo(-848432295L, "skins.jake.jake_apple", "Apple Arcade Exclusive", 3),
            CitySkinInfo(-1186298654L, "skins.jake.jake_yellow", "Yellow (Жёлтый)", 1),
            CitySkinInfo(-1882385935L, "skins.jake.jake_incognito", "Incognito (Инкогнито)", 3),
            CitySkinInfo(2075106495L, "skins.jake.jake_retroneon", "Retro Neon (Ретро-неон)", 1),
            CitySkinInfo(-1087452012L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(-421137594L, "skins.jake.jake_greenvapor", "Green Vapor (Зелёный пар)", 3),
            CitySkinInfo(1013987027L, "skins.jake.jake_touchdown", "Touchdown (Тачдаун)", 3),
            CitySkinInfo(1171279005L, "skins.jake.jake_jakeolantern", "Jake O'Lantern (Хэллоуин)", 3)
        ),
        // 2. Tricky
        1900660162L to listOf(
            CitySkinInfo(-72574065L, "skins.tricky.tricky_default", "Default (Стандартный)", 0),
            CitySkinInfo(1564209559L, "skins.tricky.tricky_redblack", "Red & Black (Красно-чёрный)", 1),
            CitySkinInfo(-1650480580L, "skins.tricky.tricky_bgirl", "B-Girl (Би-гёрл)", 4),
            CitySkinInfo(1013312398L, "skins.tricky.tricky_rainbowrush", "Rainbow Rush (Радужный)", 3),
            CitySkinInfo(8504888L, "skins.tricky.tricky_bubblepop", "Bubble Pop (Бабл-поп)", 1),
            CitySkinInfo(-820187263L, "skins.tricky.tricky_neon", "Neon (Неоновый)", 1),
            CitySkinInfo(-1583432582L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(2016861369L, "skins.tricky.tricky_skatecrow", "Skatecrow (Пугало Хэллоуин)", 3)
        ),
        // 3. Fresh
        2129411796L to listOf(
            CitySkinInfo(1202249944L, "skins.fresh.fresh_default", "Default (Стандартный)", 0),
            CitySkinInfo(-880195487L, "skins.fresh.fresh_cooldrip", "Cool Drip (Стильный)", 3),
            CitySkinInfo(-1292355720L, "skins.fresh.fresh_redblack", "Red & Black (Красно-чёрный)", 1),
            CitySkinInfo(772685907L, "skins.fresh.fresh_purple", "Purple (Фиолетовый)", 1),
            CitySkinInfo(1310049346L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(1208727121L, "skins.fresh.fresh_biteclub", "Bite Club (Вампир Хэллоуин)", 3)
        ),
        // 4. Miss Maia
        1614866432L to listOf(
            CitySkinInfo(-1083767387L, "skins.missmaia.missmaia_default", "Default (Стандартный)", 0),
            CitySkinInfo(1154305282L, "skins.missmaia.missmaia_redblack", "Red & Black (Красно-чёрный)", 1),
            CitySkinInfo(775756685L, "skins.missmaia.missmaia_orangeblue", "Orange & Blue (Оранжево-синий)", 1),
            CitySkinInfo(910350527L, "skins.missmaia.missmaia_neondemon", "Neon Demon (Неоновый демон)", 3),
            CitySkinInfo(577316046L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 5. Ninja One
        1804257387L to listOf(
            CitySkinInfo(1189151463L, "skins.ninjaone.ninja_one_default", "Default (Стандартный)", 0),
            CitySkinInfo(-672918755L, "skins.ninjaone.ninja_one_gray", "Gray (Серый)", 1),
            CitySkinInfo(2065748087L, "skins.ninjaone.ninja_one_crimsonphantom", "Crimson Phantom (Алый фантом)", 1),
            CitySkinInfo(-1253901889L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(-634862949L, "skins.ninjaone.ninja_one_onione", "Oni One (Демон Они)", 4)
        ),
        // 6. Yutani
        1663244716L to listOf(
            CitySkinInfo(460586275L, "skins.yutani.yutani_default", "Default (Стандартный)", 0),
            CitySkinInfo(-258476380L, "skins.yutani.yutani_redblack", "Red & Black (Красно-чёрный)", 1),
            CitySkinInfo(1109772367L, "skins.yutani.yutani_classic", "Classic Alien (Классический)", 4),
            CitySkinInfo(1478947346L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(1176109949L, "skins.yutani.yutani_manecharacter", "Mane Character (Лев)", 3)
        ),
        // 7. Spike
        849273384L to listOf(
            CitySkinInfo(922134811L, "skins.spike.spike_default", "Default (Стандартный)", 0),
            CitySkinInfo(-759788578L, "skins.spike.spike_redash", "Red Ash (Красный пепел)", 1),
            CitySkinInfo(-1655542994L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(2075049185L, "skins.spike.spike_hothead", "Hothead (Огненный панк)", 1)
        ),
        // 8. Ella
        1200047034L to listOf(
            CitySkinInfo(-989991611L, "skins.ella.ella_default", "Default (Стандартный)", 0),
            CitySkinInfo(1508371367L, "skins.ella.ella_purple", "Purple (Фиолетовый)", 1),
            CitySkinInfo(-93943772L, "skins.ella.ella_yellowblack", "Yellow & Black (Жёлто-чёрный)", 1),
            CitySkinInfo(1990369193L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(-1590860557L, "skins.ella.ella_arcadestar", "Arcade Star (Аркадная звезда)", 1)
        ),
        // 9. Jay / Nicky
        1120354844L to listOf(
            CitySkinInfo(-2145465549L, "skins.nicky.jay_default", "Default (Стандартный)", 0),
            CitySkinInfo(-152602730L, "skins.nicky.jay_cali", "Cali (Калифорния)", 1),
            CitySkinInfo(-1566346654L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 10. Noon
        -502265868L to listOf(
            CitySkinInfo(1962475607L, "skins.noon.noon_default", "Default (Стандартный)", 0),
            CitySkinInfo(748483822L, "skins.noon.noon_blue", "Blue (Синий)", 0),
            CitySkinInfo(-651312328L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 11. Billy
        581326566L to listOf(
            CitySkinInfo(27667358L, "skins.billy.billy_default", "Default (Стандартный)", 0),
            CitySkinInfo(-1959135014L, "skins.billy.billy_junglecammo", "Jungle Camo (Джунгли камуфляж)", 1),
            CitySkinInfo(2025202852L, "skins.billy.billy_flames", "Flames (Пламя)", 1),
            CitySkinInfo(-1067636066L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 12. Rosalita
        -1505268145L to listOf(
            CitySkinInfo(-1389912410L, "skins.rosalita.rosalita_default", "Default (Стандартный)", 0),
            CitySkinInfo(389528075L, "skins.rosalita.rosalita_blackburgundy", "Black Burgundy (Бордовый)", 1),
            CitySkinInfo(-1487295210L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(562706612L, "skins.rosalita.rosalita_pachuco", "Pachuco (Пачуко)", 3),
            CitySkinInfo(595861865L, "skins.rosalita.rosalita_diadelosmuertos", "Dia de los Muertos (День мёртвых)", 3)
        ),
        // 13. Tasha
        299562833L to listOf(
            CitySkinInfo(-503301639L, "skins.tasha.tasha_default", "Default (Стандартный)", 0),
            CitySkinInfo(-1312184589L, "skins.tasha.tasha_green", "Green (Зелёный)", 1),
            CitySkinInfo(288125499L, "skins.tasha.tasha_redwhite", "Red & White (Красно-белый)", 1),
            CitySkinInfo(-889435486L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 14. Jaewoo
        -1733051898L to listOf(
            CitySkinInfo(-1312276630L, "skins.jaewoo.jaewoo_default", "Default (Стандартный)", 0),
            CitySkinInfo(-1360992679L, "skins.jaewoo.jaewoo_blackmagenta", "Black Magenta (Пурпурный)", 1),
            CitySkinInfo(-1505271108L, "skins.jaewoo.jaewoo_greenpurple", "Green Purple (Зелёно-фиолетовый)", 1),
            CitySkinInfo(374299079L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(366723518L, "skins.jaewoo.jaewoo_starburst", "Starburst (Звёздный взрыв)", 3)
        ),
        // 15. Tagbot
        1887684367L to listOf(
            CitySkinInfo(-1236251198L, "skins.tagbot.tagbot_default", "Default (Стандартный)", 0),
            CitySkinInfo(1864637985L, "skins.tagbot.tagbot_red", "Red (Красный робот)", 1),
            CitySkinInfo(-209858400L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 16. Lucy
        852717139L to listOf(
            CitySkinInfo(726823100L, "skins.lucy.lucy_default", "Default (Стандартный)", 0),
            CitySkinInfo(1076495376L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(1973379545L, "skins.lucy.lucy_gravequeen", "Grave Queen (Королева могил)", 4),
            CitySkinInfo(-122811436L, "skins.lucy.lucy_purple", "Purple (Фиолетовый)", 1),
            CitySkinInfo(-1212267509L, "skins.lucy.lucy_toxicgreen", "Toxic Green (Токсично-зелёный)", 3)
        ),
        // 17. Georgie
        -1534276928L to listOf(
            CitySkinInfo(2085271833L, "skins.georgie.georgie_default", "Default (Стандартный)", 0),
            CitySkinInfo(-1648267179L, "skins.georgie.georgie_chicagobulls", "Chicago Bulls (Баскетбольный)", 1),
            CitySkinInfo(1136726127L, "skins.georgie.georgie_bling", "Bling (Золотой Блинг)", 4),
            CitySkinInfo(1119532892L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 18. V3ctor
        -2125407733L to listOf(
            CitySkinInfo(666084587L, "skins.v3ctor.v3ctor_default", "Default (Стандартный)", 0),
            CitySkinInfo(-2129570054L, "skins.v3ctor.v3ctor_vr", "VR Cyber (Виртуальный)", 4),
            CitySkinInfo(-1857764588L, "skins.v3ctor.v3ctor_pink", "Pink (Розовый неон)", 1),
            CitySkinInfo(1014902922L, "skins.v3ctor.v3ctor_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 19. Zara
        823378763L to listOf(
            CitySkinInfo(1093072756L, "skins.zara.zara_default", "Default (Стандартный)", 0),
            CitySkinInfo(334108993L, "skins.zara.zara_prowess", "Prowess (Доблесть)", 3),
            CitySkinInfo(1870421642L, "skins.zara.zara_holozebra", "Holo Zebra (Голографическая зебра)", 1),
            CitySkinInfo(-2098605721L, "skins.zara.zara_supersonic", "Supersonic (Сверхзвуковая)", 4),
            CitySkinInfo(2030621953L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 20. Lilah
        -518167090L to listOf(
            CitySkinInfo(-789829533L, "skins.lilah.lilah_default", "Default (Стандартный)", 0),
            CitySkinInfo(-1597270950L, "skins.lilah.lilah_greencammo", "Green Camo (Зелёный камуфляж)", 1),
            CitySkinInfo(-151984922L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 21. Ash
        -2075784936L to listOf(
            CitySkinInfo(816980720L, "skins.ash.ash_default", "Default (Стандартный)", 0),
            CitySkinInfo(1150862217L, "skins.ash.ash_greenorange", "Green & Orange (Зелёно-оранжевый)", 1),
            CitySkinInfo(272931189L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(-325915144L, "skins.ash.ash_lightningstrike", "Lightning Strike (Удар молнии)", 3),
            CitySkinInfo(-1795574191L, "skins.ash.ash_flamethrower", "Flamethrower (Огнемётчик)", 4)
        ),
        // 22. Jenny
        1936280213L to listOf(
            CitySkinInfo(362958220L, "skins.jenny.jenny_default", "Default (Стандартный)", 0),
            CitySkinInfo(-1287581543L, "skins.jenny.jenny_colorpatches", "Color Patches (Цветные нашивки)", 1),
            CitySkinInfo(-1493511016L, "skins.jenny.jenny_wildflower", "Wildflower (Дикий цветок)", 4)
        ),
        // 23. Wei
        -2116248615L to listOf(
            CitySkinInfo(-2092832313L, "skins.wei.wei_default", "Default (Стандартный)", 0),
            CitySkinInfo(-871825427L, "skins.wei.wei_tealorange", "Teal & Orange (Бирюзово-оранжевый)", 1),
            CitySkinInfo(550381294L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(1356103770L, "skins.wei.wei_voyager", "Voyager (Вояджер)", 4)
        ),
        // 24. Prince K
        966716028L to listOf(
            CitySkinInfo(-1112152883L, "skins.princek.princek_default", "Default (Стандартный)", 0),
            CitySkinInfo(1781172277L, "skins.princek.princek_red", "Red Royal (Королевский красный)", 1),
            CitySkinInfo(-1354700793L, "skins.princek.princek_goldenstreak", "Golden Streak (Золотой принц)", 4),
            CitySkinInfo(1228420716L, "skins.princek.princek_oldmoney", "Old Money (Аристократ)", 3),
            CitySkinInfo(492640795L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 25. Monique
        1363767693L to listOf(
            CitySkinInfo(-1415696609L, "skins.monique.monique_default", "Default (Стандартный)", 0),
            CitySkinInfo(1335121268L, "skins.monique.monique_greenmustard", "Green Mustard (Горчично-зелёный)", 1)
        ),
        // 26. Harini
        135046766L to listOf(
            CitySkinInfo(773419542L, "skins.harini.harini_default", "Default (Стандартный)", 0),
            CitySkinInfo(-1447454858L, "skins.harini.harini_medic", "Medic (Медик)", 4),
            CitySkinInfo(-1143369791L, "skins.harini.harini_redteal", "Red & Teal (Красно-бирюзовый)", 1),
            CitySkinInfo(1093744178L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2)
        ),
        // 27. Rey Romeo
        -496667767L to listOf(
            CitySkinInfo(-284965872L, "skins.reyromeo.reyromeo_default", "Default (Стандартный)", 0),
            CitySkinInfo(1384745101L, "skins.all.all_superstar", "Superstar (Суперзвезда)", 2),
            CitySkinInfo(-19413135L, "skins.reyromeo.reyromeo_aceofgraves", "Ace of Graves (Туз могил)", 4),
            CitySkinInfo(-1754777190L, "skins.reyromeo.reyromeo_xrey", "X-Rey (Скелет Рентген)", 1),
            CitySkinInfo(1313220645L, "skins.reyromeo.reyromeo_trickster", "Trickster (Трикстер)", 3)
        )
    )

    // Полный плоский список всех 125 скинов из surferskins.json
    val ALL_SKINS: List<Long> = SURFER_SKINS_MAP.values.flatten().map { it.dataTag }.distinct()

    // Все 29 досок из актуального boards.json
    val BOARDS = listOf(
        857306595L to "Default Board (Стандартная доска)",
        821107692L to "Jake's Board (Доска Джейка)",
        797961277L to "Tricky's Board (Доска Трики)",
        -2027086747L to "Fresh's Board (Доска Фреша)",
        -1569537423L to "Miss Maia's Board (Доска Мисс Майи)",
        -573684740L to "Ninja's Board (Доска Ниндзя)",
        -2089484430L to "Yutani's Board (Доска Ютани)",
        -875752536L to "Spike's Board (Доска Спайка)",
        -1886993925L to "Ella's Board (Доска Эллы)",
        -425685755L to "Jay's Board (Доска Джея)",
        463203314L to "Noon's Board (Доска Нун)",
        -1101319815L to "Billy's Board (Доска Билли)",
        -1995581156L to "Rosalita's Board (Доска Розалиты)",
        -487324792L to "Tasha's Board (Доска Таши)",
        -1946062440L to "Jaewoo's Board (Доска Джэу)",
        -2014824727L to "Tagbot's Board (Доска Тагбота)",
        369661746L to "Lucy's Board (Доска Люси)",
        1389875337L to "Georgie's Board (Доска Джорджи)",
        -268124908L to "V3ctor's Board (Доска Вектора)",
        -762458939L to "Zara's Board (Доска Зары)",
        -1702093280L to "Lilah's Board (Доска Лайлы)",
        -1398156391L to "Ash's Board (Доска Эша)",
        1016074756L to "Jenny's Board (Доска Дженни)",
        1346993456L to "Wei's Board (Доска Вэя)",
        -2028224723L to "Prince K's Board (Доска Принца Кея)",
        -282672473L to "Monique's Board (Доска Моник)",
        1092080220L to "Harini's Board (Доска Харини)",
        -1017845163L to "Classic Hoverboard (Классический ховерборд)",
        1711310740L to "Rey Romeo's Board (Доска Рэя Ромео)"
    )

    // Все 34 аватара из актуального avatars.json
    val ALL_AVATARS = longArrayOf(
        -2018974533L, 1865504338L, -437323474L, -782241132L, -1613049225L,
        2087481287L, -26258735L, -1641504761L, 8505649L, -441356086L,
        -400793105L, -2116945982L, 788191187L, 1383998524L, 421680248L,
        -817891651L, 536715003L, -460774707L, 685395160L, -1115378706L,
        -893091252L, 1125679113L, -819973436L, 1888966466L, -1517921881L,
        1698168951L, 1835072283L, -74699014L, -1414833067L, 1304218915L,
        -64934795L, 1255266723L, 649252816L, 1342239482L
    )

    val DISTRICTS = listOf(
        "district_1", "district_2", "district_3", "district_4", "district_5", "district_6"
    )

    val SEASONS = listOf(
        "season_01" to "Сезон 1 (Звёздный старт)",
        "season_02" to "Сезон 2 (Неоновый драйв)",
        "season_03" to "Сезон 3 (Кибер-волна)",
        "season_04" to "Сезон 4 (Хэллоуин / Rey Romeo)"
    )

    val SCHEDULED_CAMPAIGNS = listOf(
        "scheduled_campaign_01" to "Ивентовая кампания 1",
        "scheduled_campaign_02" to "Ивентовая кампания 2",
        "scheduled_campaign_03" to "Ивентовая кампания 3"
    )

    val PIGGY_BANKS = listOf(
        "shop.piggybankmlp7.1" to (-2000200803L to "Уровень 1 (Малая копилка)"),
        "shop.piggybankmlp7.2" to (-2000200804L to "Уровень 2 (Средняя копилка)"),
        "shop.piggybankmlp7.3" to (-2000200805L to "Уровень 3 (Мега-копилка 250 000)")
    )

    val IAP_CATALOG = listOf(
        "premium_pass" to "🎟️ Премиум Сезонный Пропуск (Season Pass)",
        "shop.remove.ads" to "🚫 Полное отключение рекламы (Remove Ads)",
        "surfers.co.tagbotunlock001" to "🤖 Цепной оффер: Разблокировка Tagbot + Набор",
        "store.chainoffer.iapstepup.2" to "💎 Step-Up Оффер 1 (100 Ключей + Бонусы)",
        "store.chainoffer.iapstepup.4" to "🥫 Step-Up Оффер 2 (5 Банок возрождения + 15 Жетонов досок)",
        "store.chainoffer.iapstepup.6" to "🔑 Step-Up Оффер 3 (200 Ключей + Кредит сёрфера)",
        "store.chainoffer.iapstepup.8" to "🪙 Step-Up Оффер 4 (40 000 Монет)",
        "store.chainoffer.iapstepup.10" to "🎫 Step-Up Оффер 5 (50 Билетов кампании)",
        "store.chainoffer.iapstepup.12" to "👑 Step-Up Оффер Финал (300 Ключей + 5 000 Монет)",
        "store.chainoffer.iapstepup2.1" to "⚡ Step-Up II (100 Ключей + Доски + Кредиты)",
        "store.chainoffer.iapstepup2.4" to "⚡ Step-Up II (5 Банок возрождения)",
        "store.chainoffer.iapstepup2.7" to "⚡ Step-Up II (40 000 Монет)",
        "store.chainoffer.iapstepup2.10" to "⚡ Step-Up II Финал (300 Ключей)",
        "store.chainoffer.iapconv.1" to "🎁 Конверсионный Мега-Набор 1",
        "store.chainoffer.iapconv.2" to "🎁 Конверсионный Набор (100 Ключей)",
        "store.chainoffer.iapconv.6" to "🎁 Конверсионный Набор (15 000 Монет)",
        "store.chainoffer.iapconv.8" to "🎁 Конверсионный Набор (20 Билетов)",
        "shop.starterpack" to "🚀 Стартовый набор (Starter Pack)",
        "shop.intermediatepack" to "🔥 Продвинутый набор (Intermediate Pack)",
        "shop.expertpack" to "👑 Экспертный набор (Expert Pack)",
        "shop.rosalitapack" to "💃 Набор Розалиты (Rosalita Bundle)",
        "shop.classicboard" to "🛹 Набор Классического Ховерборда"
    )

    val ALL_IAP_PURCHASES: List<String> = IAP_CATALOG.map { it.first }

    val HOME_OFFER_NOTIFICATION_IDS = listOf(
        "starterpack", "intermediatepack", "expertpack", "removeads", "premiumpass"
    )

    val SECRET_STAR_TAGS = longArrayOf(
        100001L, 100002L, 100003L, 100004L, 100005L, 100006L
    )

    val ACHIEVEMENT_MISSIONS = listOf(
        1001L to 1000,
        1002L to 1000,
        1003L to 1000,
        1004L to 1000,
        1005L to 1000
    )

    val FTUE_STEPS = listOf(
        "ftue_completed", "tutorial_completed", "shop_ftue", "upgrade_ftue", "crew_ftue"
    )
}
