package com.layxtar.subwaysaver

object ClassicDatabase {

    // Только реальные, работающие валюты из wallet.json
    val CURRENCIES = listOf(
        "1" to "🪙 Монеты (Coins)",
        "2" to "🔑 Ключи (Keys)",
        "3" to "🛹 Ховерборды (Hoverboards)",
        "4" to "🚀 Фора на старте (HeadStarts)",
        "5" to "⭐ Бустеры очков (ScoreBoosters)",
        "6" to "🎟️ Монеты Событий (EventCoins)",
        "12" to "🎃 Жетоны Сезонной Охоты (Hunt Tokens)"
    )

    // Специальные и социальные достижения для разблокировки
    val SPECIAL_ACHIEVEMENTS = listOf(
        "achievement_facebook" to "Добавить друга в Facebook / соцсетях",
        "achievement_media_friend" to "Связать профиль с медийным аккаунтом",
        "achievement_brawl_stars" to "Кроссоверная коллекция Brawl / Friends",
        "achievement_high_roller" to "Собрать всех персонажей особой линейки",
        "achievement_social_butterfly" to "Социальные забеги с друзьями",
        "achievement_crossover_all" to "Все кроссоверные герои и скины"
    )

    val LOOTBOX_TYPES = listOf(
        "super_mystery_box" to "🎁 Super Mystery Box",
        "mysterybox_regular" to "📦 Обычный Mystery Box",
        "token_box" to "🪙 Коробка жетонов сёрферов",
        "holiday_gift_box" to "🎄 Праздничный подарок"
    )

    // Категории для фильтрации сёрферов
    val SURFER_CATEGORIES = listOf("Все", "Базовые", "Города / World Tour", "Сезонные / Хэллоуин", "Особые")

    val ALL_PORTRAITS = listOf(
        "countcorvin_illustration_portrait", "frank_portrait", "yutani_portrait", "spike_portrait",
        "fresh_portrait", "tricky_portrait", "jake_portrait", "lucy_portrait", "tagbot_portrait",
        "ninja_portrait", "tasha_portrait", "zoe_portrait", "brody_portrait", "princek_portrait"
    )

    val ALL_FRAMES = listOf(
        "collection_revamp_15_frame", "season_hunt_frame_gold", "halloween_frame_2025",
        "champions_frame_diamond", "lunar_new_year_frame", "anniversary_frame_10"
    )

    val ALL_TROPHIES = listOf(
        "Trophy_TopRun_Gold", "Trophy_TopRun_Diamond", "Trophy_TopRun_Champion",
        "Trophy_Marathon_Complete", "Trophy_MysteryHurdles"
    )

    // Полный список сёрферов с их костюмами и категорией для поиска
    val CLASSIC_SURFERS = listOf(
        "jake" to Triple("Jake (Джейк)", listOf("default", "dark", "star"), "Базовые"),
        "tricky" to Triple("Tricky (Трики)", listOf("default", "camo", "heart"), "Базовые"),
        "fresh" to Triple("Fresh (Фреш)", listOf("default", "funk", "sport"), "Базовые"),
        "spike" to Triple("Spike (Спайк)", listOf("default", "rock", "punk"), "Базовые"),
        "yutani" to Triple("Yutani (Ютани)", listOf("default", "gadget"), "Базовые"),
        "lucy" to Triple("Lucy (Люси)", listOf("default", "goth", "steampunk"), "Базовые"),
        "tagbot" to Triple("Tagbot (Тагбот)", listOf("default", "space"), "Базовые"),
        "ninja" to Triple("Ninja (Ниндзя)", listOf("default", "yang", "shadow"), "Базовые"),
        "tasha" to Triple("Tasha (Таша)", listOf("default", "cheerleader", "fitness"), "Базовые"),
        "zoe" to Triple("Zoe (Зои)", listOf("default", "zombie", "cowgirl"), "Базовые"),
        "brody" to Triple("Brody (Броди)", listOf("default", "surfer", "chill"), "Базовые"),
        "princek" to Triple("Prince K (Принц Кей)", listOf("default", "shine", "king"), "Базовые"),
        "frank" to Triple("Frank (Франк)", listOf("default", "clown", "tiger"), "Базовые"),
        "king" to Triple("King (Король)", listOf("default", "royal"), "Базовые"),
        "countcorvin" to Triple("Count Corvin (Граф Корвин)", listOf("default", "vampire", "noble"), "Сезонные / Хэллоуин"),
        "carmen" to Triple("Carmen (Кармен)", listOf("default", "shake", "festive"), "Города / World Tour"),
        "roberto" to Triple("Roberto (Роберто)", listOf("default", "fan"), "Города / World Tour"),
        "kim" to Triple("Kim (Ким)", listOf("default", "diver"), "Города / World Tour"),
        "harumi" to Triple("Harumi (Харуми)", listOf("default", "fury"), "Города / World Tour"),
        "nick" to Triple("Nick (Ник)", listOf("default", "speed"), "Города / World Tour"),
        "coco" to Triple("Coco (Коко)", listOf("default", "jester"), "Города / World Tour"),
        "sun" to Triple("Sun (Сун)", listOf("default", "spot"), "Города / World Tour"),
        "alex" to Triple("Alex (Алекс)", listOf("default", "skater"), "Города / World Tour"),
        "eddy" to Triple("Eddy (Эдди)", listOf("default", "trick"), "Сезонные / Хэллоуин"),
        "jamie" to Triple("Jamie (Джейми)", listOf("default", "explorer"), "Города / World Tour"),
        "mina" to Triple("Mina (Мина)", listOf("default", "robe"), "Города / World Tour"),
        "rosa" to Triple("Rosa (Роза)", listOf("default", "carnival"), "Города / World Tour"),
        "olivia" to Triple("Olivia (Оливия)", listOf("default", "scout"), "Города / World Tour"),
        "zayn" to Triple("Zayn (Зайн)", listOf("default", "stealth"), "Особые"),
        "dummy" to Triple("Crash Dummy (Манекен)", listOf("default", "test"), "Особые")
    )

    // Полный список досок со способностями
    val CLASSIC_BOARDS = listOf(
        "default" to Pair("Hoverboard (Базовая)", emptyList()),
        "star" to Pair("Star (Звезда)", listOf("double_jump")),
        "bouncer" to Pair("Bouncer (Прыгун)", listOf("super_jump")),
        "lowrider" to Pair("Lowrider (Лоурайдер)", listOf("stay_low")),
        "daredevil" to Pair("Daredevil (Сорвиголова)", listOf("speed_up")),
        "superhero" to Pair("Superhero (Супергерой)", listOf("super_jump", "zap_sideways")),
        "hotrod" to Pair("Hot Rod (Хот-род)", listOf("speed_up")),
        "monster" to Pair("Monster (Монстр)", listOf("super_jump", "speed_up")),
        "skull_fire" to Pair("Skull Fire (Череп в огне)", listOf("double_jump", "trail")),
        "teleporter" to Pair("Teleporter (Телепорт)", listOf("zap_sideways")),
        "lumina" to Pair("Lumina (Люмина)", listOf("trail", "double_jump")),
        "windglider" to Pair("Windglider (Дельтаплан)", listOf("smooth_drift"))
    )
}
