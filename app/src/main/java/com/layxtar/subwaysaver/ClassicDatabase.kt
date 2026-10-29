package com.layxtar.subwaysaver

data class ClassicAchievementInfo(
    val id: String,
    val title: String,
    val description: String,
    val maxTier: Int
)

object ClassicDatabase {

    val CURRENCIES = listOf(
        "1" to "🪙 Монеты (Coins)",
        "2" to "🔑 Ключи (Keys)",
        "3" to "🛹 Ховерборды (Hoverboards)",
        "4" to "🚀 Фора на старте (HeadStarts)",
        "5" to "⭐ Бустеры очков (ScoreBoosters)",
        "6" to "🎟️ Монеты Событий (EventCoins)",
        "12" to "🎃 Жетоны Сезонной Охоты (Season Hunt Tokens)"
    )

    val ACHIEVEMENTS_LIST = listOf(
        ClassicAchievementInfo("social_friends_media", "🤝 Друг звёзд", "Добавить в друзья известную медийную личность", 3),
        ClassicAchievementInfo("brawl_stars_crew", "⭐ Коллекция Brawl Stars", "Собрать всех персонажей коллаборации Brawl Stars", 3),
        ClassicAchievementInfo("rivals_conqueror", "👑 Покоритель Соперников", "Победить всех персонажей из команды Rivals", 3),
        ClassicAchievementInfo("core_crew_assembled", "🏄 Золотой состав", "Собрать всех персонажей Core Crew", 3),
        ClassicAchievementInfo("jump_jackpot", "💎 Счастливчик Супер-Джека", "Выиграть Супер Джекпот в Mystery Box", 3),
        ClassicAchievementInfo("score_legend", "🏃 Легенда Дистанции", "Набрать более 10 000 000 очков в одном забеге", 3),
        ClassicAchievementInfo("hoverboard_rider", "🛹 Мастер Ховерборда", "Проехать 100 000 метров на ховерборде", 3),
        ClassicAchievementInfo("coin_collector", "💰 Золотая лихорадка", "Собрать 1 000 000 монет на рельсах", 3),
        ClassicAchievementInfo("mission_master", "📜 Ветеран Заданий", "Завершить 100 наборов миссий", 3),
        ClassicAchievementInfo("word_hunt_champ", "🔤 Охотник за словами", "Собрать 50 ежедневных слов Word Hunt", 3)
    )

    // ==================== ВСЕ РАМКИ ПРОФИЛЯ (profile_frame.json) ====================
    val ALL_FRAMES = listOf(
        "default_frame" to "Стандартная рамка (Default)",
        "bronze_frame" to "Бронзовая рамка Top Run",
        "silver_frame" to "Серебряная рамка Top Run",
        "gold_frame" to "Золотая рамка Top Run",
        "diamond_frame" to "Алмазная рамка Top Run",
        "champion_frame" to "Рамка Чемпиона Top Run",
        "wood_frame" to "Деревянная рамка",
        "stone_frame" to "Каменная рамка",
        "neon_blue_frame" to "Неоновая синяя рамка",
        "neon_pink_frame" to "Неоновая розовая рамка",
        "cyber_neon_frame" to "Кибернетическая рамка",
        "brawl_frame" to "Brawl Stars Эксклюзив",
        "halloween_spooky_frame" to "Хэллоуин (Тыквенная)",
        "halloween_bat_frame" to "Летучие мыши (Хэллоуин)",
        "holiday_winter_frame" to "Зимняя морозная рамка",
        "holiday_ice_frame" to "Ледяная кристаллическая рамка",
        "lunar_dragon_frame" to "Лунный Новый Год (Дракон)",
        "lunar_gold_frame" to "Золотой Дракон",
        "tokyo_blossom_frame" to "Цветущая Сакура (Токио)",
        "anniversary_vip_frame" to "Юбилейная VIP рамка",
        "anniversary_10th_frame" to "10 лет Subway Surfers",
        "subway_city_frame" to "Subway City Неон",
        "space_galaxy_frame" to "Галактическая рамка",
        "underwater_frame" to "Глубоководная рамка",
        "flame_fire_frame" to "Огненная рамка (Пламя)",
        "electric_lightning_frame" to "Грозовая электрическая рамка",
        "graffiti_urban_frame" to "Граффити (Стрит-арт)",
        "slime_blob_frame" to "Слизевая рамка (Blob Slime)",
        "rivals_gold_frame" to "Рамка победителя Rivals",
        "arcade_retro_frame" to "Ретро-Аркада (8-Bit)",
        "easter_spring_frame" to "Весенняя пасхальная рамка",
        "summer_beach_frame" to "Пляжная летняя рамка"
    )

    // ==================== ВСЕ АВАТАРКИ ПРОФИЛЯ (profile_portrait.json) ====================
    val ALL_PORTRAITS = listOf(
        // Семейство Bob The Blob
        "portrait_bob_the_blob" to "🟢 Bob the Blob (Оригинал)",
        "portrait_bob_the_blob_space" to "🚀 Bob the Blob (Космос)",
        "portrait_bob_the_blob_slime" to "🧪 Bob the Blob (Слизь)",
        "portrait_bob_the_blob_golden" to "👑 Bob the Blob (Золотой)",

        // Основной состав (Core Crew)
        "portrait_jake_default" to "Jake (Джейк)",
        "portrait_jake_dark" to "Dark Jake (Тёмный Джейк)",
        "portrait_jake_star" to "Star Jake (Звёздный)",
        "portrait_jake_touchdown" to "Touchdown Jake (Тачдаун)",
        "portrait_jake_zombie" to "Zombie Jake (Зомби-Джейк)",
        "portrait_tricky_default" to "Tricky (Трики)",
        "portrait_tricky_camo" to "Camo Tricky (Камуфляж)",
        "portrait_tricky_heart" to "Heart Tricky (Сердце)",
        "portrait_tricky_skatecrow" to "Skatecrow Tricky (Хэллоуин)",
        "portrait_fresh_default" to "Fresh (Фреш)",
        "portrait_fresh_funk" to "Funk Fresh (Фанк)",
        "portrait_fresh_sport" to "Sport Fresh (Спорт)",
        "portrait_fresh_vampire" to "Vampire Fresh (Вампир)",
        "portrait_spike_default" to "Spike (Спайк)",
        "portrait_spike_punk" to "Punk Spike (Панк)",
        "portrait_spike_hothead" to "Hothead Spike (Пламя)",
        "portrait_yutani_default" to "Yutani (Ютани)",
        "portrait_yutani_gadget" to "Gadget Yutani (Гаджет)",
        "portrait_yutani_lion" to "Lion Yutani (Лев)",
        "portrait_lucy_default" to "Lucy (Люси)",
        "portrait_lucy_goth" to "Goth Lucy (Гот)",
        "portrait_lucy_gravequeen" to "Grave Queen Lucy (Хэллоуин)",
        "portrait_tagbot_default" to "Tagbot (Тагбот)",
        "portrait_tagbot_space" to "Space Tagbot (Космос)",
        "portrait_ninja_default" to "Ninja (Ниндзя)",
        "portrait_ninja_flame" to "Flame Ninja (Огонь)",
        "portrait_ninja_onione" to "Oni One Ninja (Демон Они)",
        "portrait_tasha_default" to "Tasha (Таша)",
        "portrait_tasha_cheer" to "Cheer Tasha (Чирлидер)",
        "portrait_frank_default" to "Frank (Фрэнк)",
        "portrait_frank_clown" to "Clown Frank (Клоун)",
        "portrait_king_default" to "King (Король)",
        "portrait_king_royal" to "Royal King (Монарх)",
        "portrait_zoe_default" to "Zoe (Зои)",
        "portrait_zoe_zombie" to "Zombie Zoe (Зомби Зои)",
        "portrait_ella_default" to "Ella (Элла)",
        "portrait_wayne_default" to "Wayne (Уэйн)",
        "portrait_princek_default" to "Prince K (Принц Кей)",
        "portrait_princek_shine" to "Shine Prince K (Сияющий)",
        "portrait_brody_default" to "Brody (Броуди)",
        "portrait_missmaia_default" to "Miss Maia (Мисс Майя)",
        "portrait_missmaia_neondemon" to "Neon Demon Maia",

        // World Tour Спец-персонажи
        "portrait_amira" to "Amira (Амира)",
        "portrait_bjarki" to "Bjarki (Бьярки)",
        "portrait_callum" to "Callum (Каллум)",
        "portrait_cathy" to "Cathy (Кэти)",
        "portrait_cleo" to "Cleo (Клео)",
        "portrait_dino" to "Dino (Динозаврик)",
        "portrait_edison" to "Edison (Эдисон)",
        "portrait_ezekiel" to "Ezekiel (Иезекииль)",
        "portrait_farkas" to "Farkas (Фаркаш)",
        "portrait_freya" to "Freya (Фрея)",
        "portrait_george" to "George (Джордж)",
        "portrait_harumi" to "Harumi (Харуми)",
        "portrait_hasina" to "Hasina (Хасина)",
        "portrait_jaime" to "Jaime (Хайме)",
        "portrait_jazzy" to "Jazzy (Джаззи)",
        "portrait_kareem" to "Kareem (Карим)",
        "portrait_kora" to "Kora (Кора)",
        "portrait_lauren" to "Lauren (Лорен)",
        "portrait_malik" to "Malik (Малик)",
        "portrait_manny" to "Manny (Мэнни)",
        "portrait_mei" to "Mei (Мэй)",
        "portrait_mira" to "Mira (Мира)",
        "portrait_monkbot" to "Monkbot (Монкбот)",
        "portrait_morgan" to "Morgan (Морган)",
        "portrait_noon" to "Noon (Нун)",
        "portrait_olivia" to "Olivia (Оливия)",
        "portrait_philip" to "Philip (Филип)",
        "portrait_rain" to "Rain (Рейн)",
        "portrait_ramona" to "Ramona (Рамона)",
        "portrait_rosa" to "Rosa (Роза)",
        "portrait_salma" to "Salma (Сальма)",
        "portrait_scarlett" to "Scarlett (Скарлетт)",
        "portrait_spacebot" to "Spacebot (Спейсбот)",
        "portrait_superrunner_jake" to "Super Runner Jake",
        "portrait_superrunner_tricky" to "Super Runner Tricky",
        "portrait_superrunner_fresh" to "Super Runner Fresh",
        "portrait_superrunner_yutani" to "Super Runner Yutani",
        "portrait_tankbot" to "Tankbot (Танкбот)",
        "portrait_trym" to "Trym (Трюм)",
        "portrait_yassine" to "Yassine (Яссин)",
        "portrait_zayn" to "Zayn (Зейн)",
        "portrait_zuri" to "Zuri (Зури)",

        // Brawl Stars Коллаборация
        "portrait_brawl_colt" to "Brawl Stars - Colt",
        "portrait_brawl_shelly" to "Brawl Stars - Shelly",
        "portrait_brawl_spike" to "Brawl Stars - Spike",
        "portrait_brawl_crow" to "Brawl Stars - Crow",
        "portrait_brawl_leon" to "Brawl Stars - Leon",
        "portrait_brawl_special" to "Brawl Stars - Звезда",

        // Турнирные, Трофейные и Наградные
        "portrait_top_run_bronze" to "Бронзовый призёр Top Run",
        "portrait_top_run_silver" to "Серебряный призёр Top Run",
        "portrait_top_run_gold" to "Золотой призёр Top Run",
        "portrait_top_run_diamond" to "Алмазный призёр Top Run",
        "portrait_top_run_champion" to "Гранд-Чемпион Top Run",
        "portrait_super_runner" to "Знак Супер-Бегуна",
        "portrait_gold_collector" to "Золотой миллионер",
        "portrait_key_master" to "Хранитель Ключей",
        "portrait_marathon_legend" to "Марафонец-Легенда",
        "portrait_mystery_jackpot" to "Джекпот Mystery Box",
        "portrait_all_characters" to "Коллекционер Сёрферов",
        "portrait_all_boards" to "Коллекционер Досок",
        "portrait_season_conqueror" to "Покоритель Сезонов"
    )

    val ALL_TROPHIES = listOf(
        "trophy_top_run_gold", "trophy_marathon_master", "trophy_score_legend",
        "trophy_coins_millionaire", "trophy_mystery_jackpot", "trophy_all_characters",
        "trophy_all_boards", "trophy_season_hunt_conqueror"
    )

    val ONE_TIME_IAP_PRODUCTS = listOf(
        "double_coins" to "🪙 Удвоитель монет навсегда",
        "starterpack" to "🚀 Стартовый пакет с ключами",
        "boosterpack" to "⚡ Пакет бустеров и форы",
        "all_access_bundle" to "👑 Полный доступ к игре"
    )

    val LOOTBOX_TYPES = listOf(
        "mysterybox_regular" to "Обычный Mystery Box",
        "mysterybox_super" to "Super Mystery Box (Золотой)",
        "mysterybox_token" to "Token Mystery Box"
    )

    val BRAWL_STARS_SURFERS = setOf("brawl_colt", "brawl_shelly", "brawl_spike", "brawl_crow", "brawl_leon")
    val RIVALS_SURFERS = setOf("rival_catrina", "rival_tankbot", "rival_zayn", "rival_rosa", "rival_harumi")
    val CORE_CREW_SURFERS = setOf("jake", "tricky", "fresh", "yutani", "spike", "lucy", "tagbot", "ninja", "frank", "king")

    val CLASSIC_SURFERS = listOf(
        "jake" to Pair("Jake (Джейк)", listOf("default", "dark", "star", "touchdown", "zombie")),
        "tricky" to Pair("Tricky (Трики)", listOf("default", "camo", "heart", "ice", "skatecrow")),
        "fresh" to Pair("Fresh (Фреш)", listOf("default", "funk", "sport", "winter", "vampire")),
        "spike" to Pair("Spike (Спайк)", listOf("default", "rock", "punk", "hothead")),
        "yutani" to Pair("Yutani (Ютани)", listOf("default", "gadget", "classic", "lion")),
        "lucy" to Pair("Lucy (Люси)", listOf("default", "goth", "steam", "gravequeen")),
        "tagbot" to Pair("Tagbot (Тагбот)", listOf("default", "space", "gold", "cyber")),
        "ninja" to Pair("Ninja (Ниндзя)", listOf("default", "yang", "flame", "onione")),
        "tasha" to Pair("Tasha (Таша)", listOf("default", "cheer", "gym", "retro")),
        "frank" to Pair("Frank (Фрэнк)", listOf("default", "clown", "tiger", "gold")),
        "king" to Pair("King (Король)", listOf("default", "royal", "count")),
        "zoe" to Pair("Zoe (Зои)", listOf("default", "zombie", "cowgirl")),
        "ella" to Pair("Ella (Элла)", listOf("default", "racer", "disco")),
        "wayne" to Pair("Wayne (Уэйн)", listOf("default", "knight", "detective")),
        "princek" to Pair("Prince K (Принц Кей)", listOf("default", "shine", "jaguar", "oldmoney")),
        "brody" to Pair("Brody (Броуди)", listOf("default", "posh", "chill")),
        "missmaia" to Pair("Miss Maia (Мисс Майя)", listOf("default", "lead", "neondemon")),
        "brawl_colt" to Pair("Colt (Кольт Brawl Stars)", listOf("default", "gunslinger", "outlaw")),
        "brawl_shelly" to Pair("Shelly (Шелли Brawl Stars)", listOf("default", "star", "witch")),
        "brawl_spike" to Pair("Spike (Спайк Brawl Stars)", listOf("default", "sakura", "robo")),
        "brawl_crow" to Pair("Crow (Ворон Brawl Stars)", listOf("default", "white", "phoenix")),
        "brawl_leon" to Pair("Leon (Леон Brawl Stars)", listOf("default", "shark", "dino")),
        "rival_catrina" to Pair("Catrina (Катрина Rivals)", listOf("default", "fiesta", "shadow")),
        "rival_tankbot" to Pair("Tankbot (Танкбот Rivals)", listOf("default", "iron", "gold")),
        "rival_zayn" to Pair("Zayn (Зейн Rivals)", listOf("default", "stealth", "flame")),
        "rival_rosa" to Pair("Rosa (Роза Rivals)", listOf("default", "carnival", "bloom")),
        "rival_harumi" to Pair("Harumi (Харуми Rivals)", listOf("default", "mecha", "ghost"))
    )

    val CLASSIC_BOARDS = listOf(
        "default" to Pair("Hoverboard (Стандартный)", emptyList()),
        "star" to Pair("Starboard (Звёздная)", listOf("doubleJump", "superSpeed")),
        "bouncer" to Pair("Bouncer (Прыгучая)", listOf("superJump")),
        "lowrider" to Pair("Lowrider (Низкая)", listOf("stayLow")),
        "daredevil" to Pair("Daredevil (Сорвиголова)", listOf("superSpeed")),
        "teleporter" to Pair("Teleporter (Телепорт)", listOf("teleport")),
        "windglider" to Pair("Windglider (Планер)", listOf("glide", "smoothDrift")),
        "monster" to Pair("Monster (Монстр)", listOf("superJump", "superSpeed")),
        "skull_fire" to Pair("Skull Fire (Череп в огне)", listOf("trailFire", "superSpeed")),
        "brawl_board" to Pair("Brawl Stars Hoverboard", listOf("superJump", "doubleJump", "superSpeed")),
        "great_white" to Pair("Great White (Белая акула)", listOf("smoothDrift", "superJump")),
        "lumberjack" to Pair("Lumberjack (Лесоруб)", listOf("doubleJump", "stayLow")),
        "freestyler" to Pair("Freestyler (Фристайлер)", listOf("superJump", "doubleJump")),
        "hot_rod" to Pair("Hot Rod (Хот-род)", listOf("superSpeed", "speedUp"))
    )
}
