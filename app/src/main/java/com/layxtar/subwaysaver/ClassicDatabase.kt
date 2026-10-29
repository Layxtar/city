package com.layxtar.subwaysaver

data class ClassicAchievementInfo(
    val id: String,
    val title: String,
    val description: String,
    val maxTier: Int
)

object ClassicDatabase {

    // Только чистые локальные валюты игры (без билетов рекламы и PvP)
    val CURRENCIES = listOf(
        "1" to "🪙 Монеты (Coins)",
        "2" to "🔑 Ключи (Keys)",
        "3" to "🛹 Ховерборды (Hoverboards)",
        "4" to "🚀 Фора на старте (HeadStarts)",
        "5" to "⭐ Бустеры очков (ScoreBoosters)",
        "6" to "🎟️ Монеты Событий (EventCoins)",
        "12" to "🎃 Жетоны Сезонной Охоты (Season Hunt Tokens)"
    )

    // Специальные достижения: Brawl Stars, медийные друзья, соцсети и т.д.
    val ACHIEVEMENTS_LIST = listOf(
        ClassicAchievementInfo("social_friends_media", "🤝 Друг звёзд", "Добавить в друзья известную медийную личность", 3),
        ClassicAchievementInfo("brawl_stars_crew", "⭐ Коллекция Brawl Stars", "Собрать всех персонажей коллаборации Brawl Stars", 3),
        ClassicAchievementInfo("rivals_conqueror", "👑 Покоритель Соперников", "Победить всех персонажей из команды Rivals", 3),
        ClassicAchievementInfo("core_crew_assembled", "🏄 Золотой состав", "Собрать всех персонажей Core Crew (Jake, Tricky, Fresh, Yutani)", 3),
        ClassicAchievementInfo("jump_jackpot", "💎 Счастливчик Супер-Джека", "Выиграть Супер Джекпот в Mystery Box", 3),
        ClassicAchievementInfo("score_legend", "🏃 Легенда Дистанции", "Набрать более 10 000 000 очков в одном забеге", 3),
        ClassicAchievementInfo("hoverboard_rider", "🛹 Мастер Ховерборда", "Проехать 100 000 метров на ховерборде", 3),
        ClassicAchievementInfo("coin_collector", "💰 Золотая лихорадка", "Собрать 1 000 000 монет на рельсах", 3),
        ClassicAchievementInfo("mission_master", "📜 Ветеран Заданий", "Завершить 100 наборов миссий", 3),
        ClassicAchievementInfo("word_hunt_champ", "🔤 Охотник за словами", "Собрать 50 ежедневных слов Word Hunt", 3)
    )

    // Список всех рамок профиля из profile_frame.json
    val ALL_FRAMES = listOf(
        "default_frame" to "Стандартная деревянная рамка",
        "silver_frame" to "Серебряная рамка бегуна",
        "gold_frame" to "Золотая рамка чемпиона",
        "diamond_frame" to "Алмазная сверкающая рамка",
        "champion_frame" to "Рамка Гранд-Чемпиона Top Run",
        "brawl_frame" to "Эксклюзивная рамка Brawl Stars",
        "cyber_neon_frame" to "Кибернетическая неоновая рамка",
        "halloween_spooky_frame" to "Хэллоуинская рамка с тыквами",
        "holiday_winter_frame" to "Зимняя морозная рамка",
        "lunar_dragon_frame" to "Рамка Лунного Дракона",
        "tokyo_blossom_frame" to "Рамка Токийской Сакуры",
        "anniversary_vip_frame" to "VIP Юбилейная золотая рамка"
    )

    // Список всех аватарок профиля из profile_portrait.json
    val ALL_PORTRAITS = listOf(
        "portrait_jake_default" to "Джейк (Классика)",
        "portrait_tricky_default" to "Трики (Классика)",
        "portrait_fresh_default" to "Фреш (Классика)",
        "portrait_spike_default" to "Спайк (Панк)",
        "portrait_yutani_default" to "Ютани (Пришелец)",
        "portrait_frank_default" to "Фрэнк (Кролик)",
        "portrait_king_default" to "Король (King)",
        "portrait_lucy_default" to "Люси (Гот)",
        "portrait_tagbot_default" to "Тагбот (Робот)",
        "portrait_ninja_default" to "Ниндзя",
        "portrait_princek_default" to "Принц Кей",
        "portrait_zombie_jake" to "Зомби-Джейк (Хэллоуин)",
        "portrait_brawl_special" to "Звезда Brawl Stars",
        "portrait_super_runner" to "Супер-Бегун (Турнир)",
        "portrait_grand_champion" to "Абсолютный Чемпион Top Run",
        "portrait_gold_collector" to "Золотой миллионер"
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

    // Коллекции для фильтрации сёрферов
    val BRAWL_STARS_SURFERS = setOf("brawl_colt", "brawl_shelly", "brawl_spike", "brawl_crow", "brawl_leon")
    val RIVALS_SURFERS = setOf("rival_catrina", "rival_tankbot", "rival_zayn", "rival_rosa", "rival_harumi")
    val CORE_CREW_SURFERS = setOf("jake", "tricky", "fresh", "yutani", "spike", "lucy", "tagbot", "ninja", "frank", "king")

    // Полная база сёрферов и их подскинов
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
        // Brawl Stars герои
        "brawl_colt" to Pair("Colt (Кольт Brawl Stars)", listOf("default", "gunslinger", "outlaw")),
        "brawl_shelly" to Pair("Shelly (Шелли Brawl Stars)", listOf("default", "star", "witch")),
        "brawl_spike" to Pair("Spike (Спайк Brawl Stars)", listOf("default", "sakura", "robo")),
        "brawl_crow" to Pair("Crow (Ворон Brawl Stars)", listOf("default", "white", "phoenix")),
        "brawl_leon" to Pair("Leon (Леон Brawl Stars)", listOf("default", "shark", "dino")),
        // Rivals соперники
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
