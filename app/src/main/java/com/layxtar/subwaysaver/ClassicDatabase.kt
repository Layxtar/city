package com.layxtar.subwaysaver

object ClassicDatabase {

    val CURRENCIES = listOf(
        "1" to "🪙 Монеты (Coins)",
        "2" to "🔑 Ключи (Keys)",
        "3" to "🛹 Ховерборды (Hoverboards)",
        "4" to "🚀 Фора на старте (HeadStarts)",
        "5" to "⭐ Бустеры очков (ScoreBoosters)",
        "6" to "🎟️ Монеты Событий (EventCoins)",
        "7" to "🧢 Шапки Трикзи (Tricky Hats)",
        "8" to "📻 Бумбоксы Фреша (Fresh Stereos)",
        "9" to "🎸 Гитары Спайка (Spike Guitars)",
        "10" to "🛸 Тарелки НЛО Ютани (Yutani UFOs)",
        "12" to "🎃 Жетоны Сезонной Охоты (Season Hunt Tokens)"
    )

    val LOOTBOX_TYPES = listOf(
        "super_mystery_box" to "🎁 Super Mystery Box",
        "mystery_box" to "📦 Обычный Mystery Box",
        "token_box" to "🎟️ Token Box"
    )

    val ONE_TIME_IAP_PRODUCTS = listOf(
        "no_ads_bundle" to "🚫 Пакет без рекламы",
        "coin_doubler" to "🪙 Удвоитель монет",
        "multiplier_pack" to "⭐ Умножитель очков"
    )

    val ALL_PORTRAITS = (1..114).map { "portrait_$it" }
    val ALL_FRAMES = (1..56).map { "frame_$it" }
    val ALL_TROPHIES = listOf("Trophy1", "Trophy2", "Trophy3", "Trophy4")

    val CLASSIC_SURFERS = listOf(
        "jake" to ("Джейк (Jake)" to listOf("default", "dark", "star")),
        "tricky" to ("Трикзи (Tricky)" to listOf("default", "camo", "heart")),
        "fresh" to ("Фреш (Fresh)" to listOf("default", "funk", "sport")),
        "spike" to ("Спайк (Spike)" to listOf("default", "rock", "punk")),
        "yutani" to ("Ютани (Yutani)" to listOf("default", "gadget")),
        "frank" to ("Фрэнк (Frank)" to listOf("default", "clown", "tiger")),
        "frizzy" to ("Фриззи (Frizzy)" to listOf("default", "rasta")),
        "king" to ("Кинг (King)" to listOf("default", "royal")),
        "lucy" to ("Люси (Lucy)" to listOf("default", "goth")),
        "ninja" to ("Ниндзя (Ninja)" to listOf("default", "yang", "flame")),
        "tagbot" to ("Тагбот (Tagbot)" to listOf("default", "space")),
        "tasha" to ("Таша (Tasha)" to listOf("default", "cheerleader", "fitness")),
        "zoe" to ("Зои (Zoe)" to listOf("default", "zombie")),
        "brody" to ("Броди (Brody)" to listOf("default", "summer")),
        "princek" to ("Принц К (Prince K)" to listOf("default", "shine", "jaguar"))
    )

    val CLASSIC_BOARDS = listOf(
        "default" to ("Hoverboard" to emptyList()),
        "starboard" to ("Starboard" to listOf("double_jump", "trail")),
        "lumberjack" to ("Lumberjack" to listOf("stay_low")),
        "freestyler" to ("Freestyler" to listOf("super_jump", "double_jump")),
        "bounce" to ("Bounce" to listOf("super_jump")),
        "lowrider" to ("Lowrider" to listOf("stay_low")),
        "monster" to ("Monster" to listOf("super_jump", "speed_up"))
    )
}
