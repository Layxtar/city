package com.layxtar.subwaysaver

object BestFiendsDatabase {

    const val PKG_BEST_FIENDS = "com.seriously.bestfiends"
    const val BASE_FILES_DIR = "/storage/emulated/0/Android/data/$PKG_BEST_FIENDS/files"
    const val GAME_SETTINGS_DIR = "$BASE_FILES_DIR/GameSettings"

    // ID предметов (SKU IDs)
    const val SKU_GOLD = 4300003
    const val SKU_KEYS = 4300004
    const val SKU_YELLOW_MITES = 4300005
    const val SKU_BLUE_MITES = 4300006
    const val SKU_MEGA_BOOST_MIN = 4300007
    const val SKU_VIP_HOURS = 4300008
    const val SKU_UNLIMITED_ENERGY_HOURS = 4300009
    const val SKU_GOLDEN_LEVEL = 4300011
    const val SKU_ROCKET = 4300013
    const val SKU_CLUSTER = 4300014
    const val SKU_FURY = 4300015
    const val SKU_CONVERTER = 4300016
    const val SKU_SLUG_SPRAY = 4300017
    const val SKU_FIEND_OR_STYLE = 4300034
    const val SKU_ENERGY = 4300041
    const val SKU_INCREASE_CAP_ENERGY = 4300042
    const val SKU_REFILL_ENERGY = 4300056

    val CURRENCY_SKUS = listOf(
        SKU_GOLD to "🪙 Золотые слитки (Gold)",
        SKU_KEYS to "🔑 Ключи (Keys)",
        SKU_YELLOW_MITES to "🟡 Жёлтые метеоры/клещи (Yellow Mites)",
        SKU_BLUE_MITES to "🔵 Синие метеоры/клещи (Blue Mites)",
        SKU_VIP_HOURS to "👑 Часы VIP (Vip Hours)",
        SKU_UNLIMITED_ENERGY_HOURS to "⚡ Часы бесконечной энергии"
    )

    // Эксклюзивные донатные персонажи (пропущенные в обычной гаче номера по цветам)
    val DONATE_GREEN_FIENDS = listOf("Green4", "Green6", "Green8", "Green12")
    val DONATE_YELLOW_FIENDS = listOf("Yellow7", "Yellow8", "Yellow12")
    val DONATE_BLUE_FIENDS = listOf("Blue4", "Blue8", "Blue11", "Blue12")
    val DONATE_PURPLE_FIENDS = listOf("Purple7", "Purple8", "Purple9", "Purple12")
    val DONATE_RED_FIENDS = listOf("Red5", "Red6", "Red8", "Red12")

    val ALL_DONATE_FIENDS = DONATE_GREEN_FIENDS +
            DONATE_YELLOW_FIENDS +
            DONATE_BLUE_FIENDS +
            DONATE_PURPLE_FIENDS +
            DONATE_RED_FIENDS

    // Стандартные персонажи из оригинального gachaLevelsFiends.json
    val STANDARD_FIENDS = listOf(
        "Green1", "Yellow1", "Blue1", "Purple1", "Red1",
        "Green2", "Blue2", "Red2", "Purple6", "Yellow2",
        "Red3", "Purple3", "Green5", "Blue3", "Yellow3",
        "Purple2", "Red4", "Green3", "Yellow6", "Blue9",
        "Purple4", "Red9", "Blue5", "Green10", "Yellow4",
        "Green7", "Blue6", "Purple10", "Red7", "Yellow11",
        "Blue7", "Yellow9", "Red10", "Purple5", "Green9",
        "Yellow10", "Blue10", "Purple11", "Red11", "Green11", "Yellow5"
    )

    val COLORS = listOf("Green", "Yellow", "Blue", "Purple", "Red")

    // Все ключи из CachedSettingsHashes.json для обнуления проверок хешей
    val HASH_KEYS = listOf(
        "starterPack", "powerUpShop", "mcettWheelFallback", "storyMapGates",
        "gachaLevelsFiends", "ftuePopups", "play_points_purchases", "alternativeLevels",
        "storyLevelsNodesConfig", "storyLevelsVisualWorldConfig", "googleDtc",
        "dynamicDifficultySimpleLevels", "powerUpTutorial", "storyLevelsCommonSettings",
        "storyLevelsOpeningConfig", "eliteLevelsOpeningConfig", "fiendex", "clientSales",
        "boostMode", "shops", "heroLevelsRewards", "levelRewards", "outOfMoves",
        "battlePassConfig", "dynamicDifficulty", "earthwormLevels", "ftueCratesPopup",
        "iconsOrder", "lobbyFTUE", "goldShopUnlock", "decorations", "economyVersion",
        "eventsArchive", "sales", "womfSettings", "womfRewards", "ads", "opportunisticAdPopup"
    )
}