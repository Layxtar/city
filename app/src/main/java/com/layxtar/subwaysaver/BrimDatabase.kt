package com.layxtar.subwaysaver

object BrimDatabase {

    const val PKG_BRIM = "com.sybogames.brim"
    const val BASE_FILES_DIR = "/storage/emulated/0/Android/data/$PKG_BRIM/files"

    // Возможные пути к файлу профиля user_profile (со скриншота)
    val PROFILE_CANDIDATE_PATHS = listOf(
        "$BASE_FILES_DIR/user_profile/user_profile.json",
        "$BASE_FILES_DIR/user_profile.json",
        "$BASE_FILES_DIR/user_profile"
    )

    // Возможные пути к папке кэша tower/gamedata
    val GAMEDATA_CANDIDATE_DIRS = listOf(
        "$BASE_FILES_DIR/tower/gamedata",
        "/storage/emulated/0/Android/data/$PKG_BRIM/cache/tower/gamedata",
        "$BASE_FILES_DIR/gamedata"
    )

    // Валюты в currencyInventory
    val CURRENCIES = listOf(
        "1" to "🪙 Монеты / Золото (Coins)",
        "2" to "💜 Магическая Эссенция (Essence / Shards)",
        "3" to "💎 Редкие Кристаллы / Токены (Currency 3)",
        "4" to "🎟️ Жетоны Подземелий (Currency 4)"
    )

    // Базовые ID героев (дополняются автоматически при сканировании tower/gamedata/characters.json)
    val DEFAULT_CHARACTERS = listOf(
        "5000" to "🗡️ Zuma (Зума — Воин)",
        "5001" to "🔮 Lilith (Лилит — Колдунья)",
        "5002" to "🏹 Farrah (Фарра — Лучница)",
        "5003" to "🥷 Aoko (Аоко — Тень)",
        "5004" to "🛡️발키리 / Val (Валькирия)",
        "5005" to "🔥 Ignis (Игнис)",
        "5006" to "❄️ Frost (Фрост)",
        "5007" to "⚡ Raiden (Райдэн)"
    )

    // Базовые улучшения в straightUpgrades (дополняются из straightupgrades.json)
    val DEFAULT_STRAIGHT_UPGRADES = listOf(
        "extra_score_bonus" to "⭐ Бонус к множителю очков (Extra Score Bonus)",
        "armor_bonus" to "🛡️ Усиление брони (Armor Bonus)",
        "health_bonus" to "❤️ Дополнительные сердца (Health Bonus)",
        "essence_magnet" to "🧲 Магнит эссенции (Essence Magnet)",
        "coin_doubler" to "🪙 Удвоитель золота (Coin Multiplier)",
        "weapon_damage" to "⚔️ Базовый урон оружия (Weapon Damage)",
        "pet_cooldown" to "🐺 Ускорение навыка питомца (Pet Cooldown)",
        "boost_duration" to "⏱️ Длительность зелий и бустов (Boost Duration)"
    )

    // Базовые ID достижений из user_profile.json
    val DEFAULT_ACHIEVEMENT_IDS = listOf(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
        "21", "22", "23", "24", "25", "26", "27", "28", "29", "30"
    )
}
