package com.layxtar.subwaysaver

object GameDatabase {

    // 9 основных валют из кошелька (wallet)
    val CURRENCIES = listOf(
        -633437229L to "🪙 Монеты (Coins)",
        -1085419483L to "🔑 Ключи (Keys)",
        -2008679869L to "🧬 Жетоны сёрферов (Surfer Tokens)",
        326719861L to "🛹 Жетоны досок (Board Tokens)",
        -1878560402L to "🎟️ Кредиты сёрферов (Surfer Credits)",
        -1509662453L to "🎫 Кредиты досок (Board Credits)",
        722065917L to "🗺️ Билеты кампании (Campaign Tickets)",
        1669706535L to "⚡ Воскрешения (Revives)",
        -635142441L to "🎃 Хэллоуинские конфеты (Event Tokens)"
    )

    // Все 27 сёрферов
    val SURFERS = listOf(
        -1836944478L to "Jake (Джейк)",
        1900660162L to "Tricky (Трики)",
        2129411796L to "Fresh (Фреш)",
        1614866432L to "Miss Maia (Мисс Майя)",
        1804257387L to "NinjaOne (Ниндзя)",
        1663244716L to "Yutani (Ютани)",
        849273384L to "Spike (Спайк)",
        1200047034L to "Ella (Элла)",
        1120354844L to "Nicky / Jay (Ники)",
        -502265868L to "Noon (Нун)",
        581326566L to "Billy (Билли)",
        -1505268145L to "Rosalita (Розалита)",
        299562833L to "Tasha (Таша)",
        -1733051898L to "Jaewoo (Джэу)",
        1887684367L to "Tagbot (Тэгбот)",
        852717139L to "Lucy (Люси — Сезон)",
        -1534276928L to "V3ctor (Вектор — Сезон)",
        -2125407733L to "Georgie (Джорджи — Сезон)",
        823378763L to "Harini (Харини — Сезон)",
        -518167090L to "Wei (Вэй — Сезон)",
        -2075784936L to "Zara (Зара — Сезон)",
        1936280213L to "Prince K (Принц Кей — Сезон)",
        -2116248615L to "Lilah (Лайла — Скрытый)",
        966716028L to "Ash (Эш — Сезон)",
        1363767693L to "Jenny (Дженни — Скрытый)",
        135046766L to "Monique (Моник — Скрытый)",
        -496667767L to "Rey Romeo (Рэй Ромео — Хэллоуин)"
    )

    // Все 29 ховербордов (включая 13 скрытых и Classic)
    val BOARDS = listOf(
        857306595L to "Default Board (Стандартная)",
        821107692L to "Jake Board",
        797961277L to "Tricky Board",
        -2027086747L to "Fresh Board",
        -1569537423L to "Miss Maia Board",
        -573684740L to "NinjaOne Board",
        -2089484430L to "Yutani Board",
        -875752536L to "Spike Board",
        -1886993925L to "Ella Board",
        -425685755L to "Nicky Board",
        463203314L to "Noon Board",
        -1101319815L to "Billy Board",
        -1995581156L to "Rosalita Board",
        -487324792L to "Tasha Board",
        -1946062440L to "Jaewoo Board",
        -2014824727L to "Tagbot Board",
        369661746L to "Lucy Board (Скрытая)",
        1389875337L to "Georgie Board (Скрытая)",
        -268124908L to "V3ctor Board (Скрытая)",
        -762458939L to "Zara Board (Скрытая)",
        -1702093280L to "Lilah Board (Скрытая)",
        -1398156391L to "Ash Board (Скрытая)",
        1016074756L to "Jenny Board (Скрытая)",
        1346993456L to "Wei Board (Скрытая)",
        -2028224723L to "Prince K Board (Скрытая)",
        -282672473L to "Monique Board (Скрытая)",
        1092080220L to "Harini Board (Скрытая)",
        -1017845163L to "Classic Board (Google Play Эксклюзив)",
        1711310740L to "Rey Romeo Board (Хэллоуин)"
    )

    // Все 125 скинов
    val ALL_SKINS = longArrayOf(
        -56800546, -960954917, 213457242, -848432295, -1186298654, -1882385935,
        2075106495, -1087452012, -421137594, -72574065, 1564209559, -1650480580,
        1013312398, 8504888, -820187263, -1583432582, 1202249944, -880195487,
        -1292355720, 772685907, 1310049346, -1083767387, 1154305282, 775756685,
        910350527, 577316046, 1189151463, -672918755, 2065748087, -1253901889,
        460586275, -258476380, 1109772367, 1478947346, 922134811, -759788578,
        -1655542994, 2075049185, -989991611, 1508371367, -93943772, 1990369193,
        -2145465549, -152602730, -1566346654, 27667358, -1959135014, 2025202852,
        -1067636066, -1389912410, 389528075, -1487295210, -503301639, -1312184589,
        288125499, -889435486, -1312276630, -1360992679, -1505271108, 374299079,
        -1236251198, 1864637985, -209858400, -789829533, -1597270950, 1093072756,
        334108993, 1870421642, -2098605721, 2030621953, 816980720, 1150862217,
        272931189, -325915144, -1795574191, 362958220, -1287581543, -2092832313,
        -871825427, 550381294, 1356103770, -1112152883, 1781172277, -1354700793,
        1228420716, 492640795, -1415696609, 1335121268, 1962475607, 748483822,
        -651312328, 666084587, -2129570054, -1857764588, 1014902922, 2085271833,
        -1648267179, 1136726127, 1119532892, 726823100, 1076495376, 1973379545,
        -122811436, -1212267509, 773419542, -1447454858, -1143369791, 1093744178,
        -1590860557, 366723518, -634862949, 562706612, 1013987027, 1171279005,
        2016861369, 1208727121, 1176109949, 595861865, -284965872, 1384745101,
        -19413135, -1754777190, 1313220645, -1041693778
    )

    // Все 41 аватар профиля
    val ALL_AVATARS = longArrayOf(
        -2018974533, 1865504338, -437323474, -782241132, -1613049225, 2087481287,
        -26258735, -1641504761, 8505649, -441356086, -400793105, -2116945982,
        788191187, 1383998524, 421680248, -817891651, 536715003, -460774707,
        685395160, -1115378706, -893091252, 1125679113, -819973436, 1888966466,
        -1517921881, 1698168951, 1835072283, -74699014, -1414833067, 1304218915,
        -64934795, 1255266723, 649252816, 1342239482, 942561756, -1390847315,
        -1686024645, -1027977298, 1483759143, 1618580706, -224214846
    )

    // Сезоны Season Pass
    val SEASONS = listOf(
        "617fdd91-5a36-4357-82b3-ecf0dee2bc3f" to "Season Rey (Октябрь 2026)",
        "66d75995-8016-48e0-bd7c-45260312cc6f" to "Season Ash (Сентябрь 2026)",
        "e612925f-d902-46fa-b0c3-fe24d6cb2132" to "Season Prince K (Август 2026)",
        "8a96f48b-af00-4fcf-8ddd-1347db375ac4" to "Season Zara (Июль 2026)",
        "6d8dfab9-cc5b-4f0b-aefb-bfd3a0e495c9" to "Season Wei (Июнь 2026)",
        "342d0c89-035d-4d94-9cd2-1d1a6e9b5ca9" to "Season Harini (Май 2026)",
        "0c284682-d509-49ed-9a12-e555bb9efdff" to "Season Georgie (Апрель 2026)",
        "c7f55cc9-e455-4301-9340-559ecc94c124" to "Season V3ctor (Март 2026)",
        "781915fe-041b-4e26-be8c-6eb5c7be547e" to "Season Lucy (Февраль 2026)"
    )

    // Все районы City Tour
    val DISTRICTS = longArrayOf(
        754815718L, -48406815L, 1011666857L, -846857L, -530046136L, 958198924L
    )

    // Свинья-копилка (Piggy Bank Sets из piggybank.json: ID -> Pair(MissionId, Описание))
    val PIGGY_BANKS = listOf(
        "shop.piggybankmlp7.3" to (-2000200805L to "Ур. 7.3 MAX (50k / 250k Upsell, 5x)"),
        "shop.piggybankmlp6.3" to (-1933733375L to "Ур. 6.3 (50k / 250k Upsell, 5x)"),
        "shop.piggybankmlp5.3" to (124549527L to "Ур. 5.3 (50k / 250k Upsell, 5x)"),
        "shop.piggybankmlp4.3" to (-1743352494L to "Ур. 4.3 (50k / 100k Upsell, 5x)"),
        "shop.piggybankmlp3.3" to (915248653L to "Ур. 3.3 (30k / 100k Upsell, 3x)"),
        "shop.piggybankmlp2.3" to (1133997251L to "Ур. 2.3 (30k / 100k Upsell, 3x)"),
        "shop.piggybankmlp1.3" to (-1010454968L to "Ур. 1.3 (30k / 50k Upsell, 3x)"),
        "shop.piggybankmlp0.3" to (1403785926L to "Ур. 0.3 Базовая (30k, 3x)"),
        "shop.piggybankmlp0.2" to (-1907218307L to "Ур. 0.2 Базовая (30k, 3x)"),
        "shop.piggybankmlp0.1" to (1981335447L to "Ур. 0.1 Стартовая (30k, 3x)")
    )

    // Дополнительные еженедельные трассы (Weekly Wonders из scheduledcampaigndata.json)
    val SCHEDULED_CAMPAIGNS = listOf(
        "weekly1" to "⚡ Speedy Stages 1 (Ускорители — Бруклин/Гавань)",
        "weekly2" to "⚡ Speedy Stages 2 (Ускорители — Коммерческий)",
        "weekly-campaign-rails-grinding-1" to "🛤️ Rails Grinding 1 (Езда по рельсам 1)",
        "weekly-campaign-rails-grinding-2" to "🛤️ Rails Grinding 2 (Езда по рельсам 2)",
        "weekly-campaign-slow-motion-1" to "⏱️ Slow Down 1 (Замедление времени)",
        "weekly-campaign-halloween-pumpkin-1" to "🎃 Halloween Pumpkin Неделя 1",
        "weekly-campaign-halloween-pumpkin-2" to "🎃 Halloween Pumpkin Неделя 2",
        "weekly-campaign-halloween-pumpkin-3" to "🎃 Halloween Pumpkin Неделя 3",
        "weekly-campaign-halloween-pumpkin-4" to "🎃 Halloween Pumpkin Неделя 4"
    )

    // Квесты и события (из questscollection.json: QuestID -> Pair(MissionId, Название))
    val QUESTS = listOf(
        "jimothy-racoon" to (965321873L to "🦝 Jimothy Racoon (Енот Джимоти)"),
        "tour-de-france" to (-1372795066L to "🚴 Tour de France (Велогонка)"),
        "gamer-week" to (-1232142887L to "🎮 Google Gamer Week (Неделя геймеров)"),
        "india-independence-harini" to (1192805612L to "🇮🇳 Harini Independence (День Индии)"),
        "fullmoon-2026" to (656300265L to "🌕 Full Moon Festival 2026 (Полнолуние)"),
        "city-skating-halloween" to (-857294965L to "🎃 City Skating Halloween 2026"),
        "mega-coin-hunt-halloween" to (-1032804383L to "🎃 Mega Coin Hunt Halloween 2026"),
        "street-market-halloween" to (2137084127L to "🎃 Street Market Halloween 2026"),
        "manic-monday" to (-214026141L to "📅 Manic Monday (Понедельник)"),
        "wacky-wednesday" to (2095401401L to "📅 Wacky Wednesday (Среда)"),
        "funky-friday" to (1435071493L to "🕺 Funky Friday (Пятница)"),
        "city-skating" to (1554727828L to "🛹 City Skating (Скейтинг)"),
        "mega-coin-hunt" to (-105144031L to "🪙 Mega Coin Hunt (Охота за монетами)"),
        "street-market" to (-256745548L to "🛍️ Street Market (Уличный рынок)"),
        "theme-park-days" to (-546746722L to "🎡 Theme Park Days (Парк аттракционов)"),
        "soda-time" to (-1571357937L to "🥤 Soda Time (Время газировки)")
    )

    // Все платные пропуска, донатные бандлы и покупки для инжекта в purchaseHistory
    val ALL_IAP_PURCHASES = listOf(
        // Платные пропуска трасс (Growth Funds для всех 6 районов)
        "districttrial.premiumladder.001",
        "districttrial.premiumladder.002",
        "districttrial.premiumladder.003",
        "districttrial.premiumladder.004",
        "districttrial.premiumladder.005",
        "districttrial.premiumladder.006",
        // Отключение рекламы и Сезонный пропуск
        "sample.remove.ads",
        "premium_pass",
        // Стартовые, средние, экспертные и мега-наборы
        "home.offer.jakestarterpack",
        "shop.offer.jakestarterpack",
        "home.offer.missmaiaintermediatepack",
        "shop.offer.maiaintermediatepack",
        "home.offer.freshexpertpack",
        "shop.offer.freshexpertpack",
        "shop.offer.lucy.outfitbundle.001",
        "shop.offer.jake_trackstar_outfit",
        "shop.offer.jake_incognito_outfit",
        "shop.offer.subsurfclassicboardunlock001",
        "shop.offer.minibundle002",
        "shop.offer.combobundle001",
        "home.offer.combobundle001",
        "shop.offer.ticketbundle001",
        "home.offer.ticketbundle001",
        "shop.offer.revive_bundle001",
        "home.offer.revive_bundle001",
        "shop.megabundle.mlp0.p1",
        "shop.megabundle.mlp4.p1",
        "shop.megabundle.mlp7.p1",
        "shop.megabundlehs.mlp7.p1",
        "free.offer.rosalitagooglecampaign",
        // Пакеты жетонов всех персонажей
        "shop.offer.jaketokenbundle001", "shop.offer.jaketokenbundle002",
        "shop.offer.trickytokenbundle001", "shop.offer.trickytokenbundle002",
        "shop.offer.freshtokenbundle001", "shop.offer.freshtokenbundle002",
        "shop.offer.yutanitokenbundle001", "shop.offer.yutanitokenbundle002",
        "shop.offer.billytokenbundle001", "shop.offer.billytokenbundle002",
        "shop.offer.nickytokenbundle002", "shop.offer.rosalitatokenbundle002",
        "shop.offer.missmaiatokenbundle002", "shop.offer.tagbottokenbundle002",
        "shop.offer.spiketokenbundle002", "shop.offer.ellatokenbundle002",
        "shop.offer.tashatokenbundle002", "shop.offer.lilahtokenbundle002",
        "shop.offer.ninjatokenbundle002", "shop.offer.zaratokenbundle002",
        "shop.offer.jaewootokenbundle002", "shop.offer.ashtokenbundle002",
        "shop.offer.jennytokenbundle002", "shop.offer.lucytokenbundle002",
        "shop.offer.noontokenbundle002", "shop.offer.moniquetokenbundle002",
        "shop.offer.princektokenbundle002",
        // Хэллоуинские аватары
        "shop.avatars.jakeolantern", "shop.avatars.scary_smiley",
        "shop.avatars.smiling_horse", "shop.avatars.werewolf_mask",
        "shop.avatars.calavera_skull", "shop.avatars.candy_bat"
    )

    // Все 57 ID всплывающих донатных окон из homeoffernotifications.json (для блокировки спама)
    val HOME_OFFER_NOTIFICATION_IDS = listOf(
        "0c7a6db7-3c3b-4a6f-8f31-7c9d7c3b8d01", "1b7d5d2a-8c1a-4c6d-9f4d-0c0c4fd3e6a2",
        "2a84b55e-7f9a-4d4b-a2b7-8f3d7b8a2d13", "4d9f1c2b-62c8-4d3e-9e2c-7a8b6c5d4e15",
        "5e2a1b0c-2f6c-4c1d-8b7a-1c2d3e4f5a16", "6f3b2c1d-3a7d-4d2e-9c8b-2d3e4f5a6b17",
        "7a4c3d2e-4b8e-4e3f-a9dc-3e4f5a6b7c18", "8b5d4e3f-5c9f-4f40-b0ed-4f5a6b7c8d19",
        "9c6e5f40-6da0-4041-c1fe-5a6b7c8d9e1a", "a7d7a651-7eb1-4142-d20f-6b7c8d9e0f1b",
        "b8e8b762-8fc2-4243-e310-7c8d9e0f1a2c", "c9f9c873-90d3-4344-f411-8d9e0f1a2b3d",
        "d0a0d984-a1e4-4445-a512-9e0f1a2b3c4e", "e1b1ea95-b2f5-4546-b613-0f1a2b3c4d5f",
        "f2c2fb06-c306-4647-c714-1a2b3c4d5e60", "03d30c17-d417-4748-d815-2b3c4d5e6f71",
        "14e41d28-e528-4849-e916-3c4d5e6f7082", "25f52e39-f639-494a-f017-4d5e6f708193",
        "36063f4a-074a-4a4b-a118-5e6f708192a4", "4717405b-185b-4b4c-b219-6f708192a3b5",
        "5828516c-296c-4c4d-c31a-708192a3b4c6", "6939627d-3a7d-4d4e-d41b-8192a3b4c5d7",
        "7a4a738e-4b8e-4e4f-e51c-92a3b4c5d6e8", "8b5b849f-5c9f-4f50-f61d-a3b4c5d6e7f9",
        "9c6c95a0-6da0-4051-a71e-b4c5d6e7f801", "ad7da6b1-7eb1-4152-b81f-c5d6e7f80112",
        "be8eb7c2-8fc2-4253-c920-d6e7f8011233", "cf9fc8d3-90d3-4354-da21-e7f801123344",
        "d0a0d9e4-a1e4-4455-eb22-f80112334455", "e1b1eaf5-b2f5-4556-fc23-012334455667",
        "14e41d28-e528-4859-cf26-34455667890a", "25f52e39-f639-495a-d027-455667890abc",
        "36063f4a-074a-4a5b-e128-5667890abcde", "4717405b-185b-4b5c-f229-67890abcdef0",
        "5828516c-296c-4c5d-a32a-7890abcdef01", "6939627d-3a7d-4d5e-b42b-890abcdef012",
        "7a4a738e-4b8e-4e5f-c52c-90abcdef0123", "1defa511-646d-4fd0-9e6c-b375cbfaba4e",
        "23e34fbb-13bd-49ba-85f1-a4bf8347bb97", "57a45dc2-b0ab-4889-8c7a-f7a4a18c25fa",
        "9158f7ff-9fdf-463e-9085-5ee7f11a65b5", "e30cb274-fe98-440b-9796-e1dfbe902ac2",
        "3ce91619-e2b2-4fb9-bb34-a9b22d49598a", "6fe0685a-727b-4c4a-ade7-b1cfac845cf1",
        "96758b0b-b00a-4a9d-bd07-c1f5cb652c17", "7776fb3b-5a96-47b1-8c7c-4aa61592c892",
        "1bfd27bf-41af-4de8-b498-72394330512f", "6b687778-d92f-452a-8e4a-6166b1c59301",
        "b53e7d1b-865a-49de-8be5-e87249d47c34", "53c479c0-428d-4eff-a529-9212a15ce51c",
        "6dc2664b-117f-42ae-b47d-646235f4c539", "9f39e522-e808-4740-9dda-ceec56e8503e",
        "133ede8d-ba99-4e82-8d77-88665ccb5fdb", "6a4afa91-aa1f-4dfd-a188-ebf926513f34",
        "b1bd57de-8373-4241-af3d-93b17f9d2de7", "2b694ab8-8fe1-4a3a-8f03-d4df08b0e3ec",
        "3104d8fd-b0dd-4457-a592-cd28d7d87894"
    )

    // Все 12 достижений из achievements.json (MissionId -> целевой прогресс)
    val ACHIEVEMENT_MISSIONS = listOf(
        -1048301224L to 100,
        1149701091L to 2,
        -83190620L to 5000,
        -1344576354L to 5,
        133030444L to 100,
        1739487940L to 20,
        -818023335L to 5,
        1836095912L to 5,
        1347182739L to 10,
        1724951749L to 50,
        -2072915534L to 200000,
        41756756L to 5
    )

    // Теги секретных звёзд на картах (SecretStarCounter)
    val SECRET_STAR_TAGS = longArrayOf(
        -533936783, -2145989581, -586907066, 879832767, -606690560, -419943634,
        -399840750, 737186562, -504369381, -101012205, -608805007, 52993644,
        1493141777, 477048382, -405818240, -40973798, 166990695, -1572152506,
        2127993918, -1327161860, 471277129, -1077266123, -2051997889, -1742950628
    )
}
