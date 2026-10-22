package com.layxtar.subwaysaver

object GameDatabase {

    val CURRENCIES = listOf(
        -1878560402L to "🪙 Монеты (Coins)",
        -1509662453L to "💎 Алмазы (Gems)",
        -1085419483L to "🔑 Ключи (Keys)",
        -635142441L to "🎟️ Билеты Трюков (Trick Tickets)",
        -2008679869L to "🔋 Батарейки (Power Batteries)",
        326719861L to "⭐ Звёздные Очки (Star Points)",
        1669706535L to "🏆 Кубки (Trophies)",
        -633437229L to "🎟️ Золотые Билеты (Golden Tickets)",
        722065917L to "⚡ Мега Жетоны (Mega Tokens)"
    )

    // Привязка скинов к каждому сёрферу City: ID -> (Имя, Список пар (НомерСкина to НазваниеСкина))
    val SURFERS_WITH_SKINS = listOf(
        1L to ("Джейк (Jake)" to listOf(0 to "Обычный (Default)", 1 to "Инкогнито (Incognito)", 2 to "Звёздный (Star)", 3 to "Хэллоуин (Dark)")),
        2L to ("Трикзи (Tricky)" to listOf(0 to "Обычный (Default)", 1 to "Кэжуал (Casual)", 2 to "Камуфляж (Camo)")),
        3L to ("Фреш (Fresh)" to listOf(0 to "Обычный (Default)", 1 to "Фанк (Funk)", 2 to "Спорт (Sport)")),
        4L to ("Спайк (Spike)" to listOf(0 to "Обычный (Default)", 1 to "Рок (Rock)", 2 to "Панк (Punk)")),
        5L to ("Ютани (Yutani)" to listOf(0 to "Обычный (Default)", 1 to "Гаджет (Gadget)")),
        6L to ("Фрэнк (Frank)" to listOf(0 to "Обычный (Default)", 1 to "Клоун (Clown)")),
        7L to ("Фриззи (Frizzy)" to listOf(0 to "Обычный (Default)", 1 to "Диско (Disco)")),
        8L to ("Кинг (King)" to listOf(0 to "Обычный (Default)", 1 to "Корона (Royal)")),
        9L to ("Люси (Lucy)" to listOf(0 to "Обычный (Default)", 1 to "Гот (Goth)")),
        10L to ("Ниндзя (Ninja)" to listOf(0 to "Обычный (Default)", 1 to "Янси (Yang)")),
        11L to ("Тагбот (Tagbot)" to listOf(0 to "Обычный (Default)", 1 to "Спейс (Space)")),
        12L to ("Таша (Tasha)" to listOf(0 to "Обычный (Default)", 1 to "Фитнес (Fitness)")),
        13L to ("Зои (Zoe)" to listOf(0 to "Обычный (Default)", 1 to "Зомби (Zombie)")),
        14L to ("Броди (Brody)" to listOf(0 to "Обычный (Default)", 1 to "Лето (Summer)")),
        15L to ("Принц К (Prince K)" to listOf(0 to "Обычный (Default)", 1 to "Блеск (Shine)")),
        16L to ("Дино (Dino)" to listOf(0 to "Обычный (Default)", 1 to "Юрский (Jurassic)")),
        17L to ("Буми (Boomi)" to listOf(0 to "Обычный (Default)")),
        18L to ("Розалита (Rosalita)" to listOf(0 to "Обычный (Default)", 1 to "Карнавал (Carnival)")),
        19L to ("Клементина (Clementine)" to listOf(0 to "Обычный (Default)")),
        20L to ("Джинджер (Ginger)" to listOf(0 to "Обычный (Default)")),
        21L to ("Филипп (Philip)" to listOf(0 to "Обычный (Default)")),
        22L to ("Стиви (Stevie)" to listOf(0 to "Обычный (Default)")),
        23L to ("Майя (Maya)" to listOf(0 to "Обычный (Default)")),
        24L to ("Альба (Alba)" to listOf(0 to "Обычный (Default)")),
        25L to ("Карлос (Carlos)" to listOf(0 to "Обычный (Default)")),
        26L to ("Мин (Min)" to listOf(0 to "Обычный (Default)")),
        27L to ("Кэлвин (Calvin)" to listOf(0 to "Обычный (Default)"))
    )

    val SURFERS = SURFERS_WITH_SKINS.map { it.first to it.second.first }

    val BOARDS = listOf(
        1L to "Hoverboard (Классический)",
        2L to "Starboard (Звёздный)",
        3L to "Lumberjack (Дровосек)",
        4L to "Freestyler (Фристайлер)",
        5L to "Lowrider (Лоурайдер)",
        6L to "Bounce (Прыгун)",
        7L to "Miami (Майами)",
        8L to "Monster (Монстр)",
        9L to "Jaguar (Ягуар)",
        10L to "Sunset (Закат)",
        11L to "Chameleone (Хамелеон)",
        12L to "Scooter (Самокат)",
        13L to "Skull Fire (Огненный Череп)",
        14L to "Superhero (Супергерой)",
        15L to "Futuristic (Киберпанк)",
        16L to "Teleporter (Телепорт)",
        17L to "Cruiser (Круизёр)",
        18L to "Daredevil (Сорвиголова)",
        19L to "Glider (Планер)",
        20L to "Hot Rod (Хот-род)",
        21L to "Jumping Jack (Джек-Попрыгун)",
        22L to "Phantom (Призрак)",
        23L to "Prankster (Шутник)",
        24L to "Rose (Роза)",
        25L to "Shark (Акула)",
        26L to "Space Cruiser (Космолёт)",
        27L to "Speckle (Пятнистый)",
        28L to "Star Cruiser (Звёздный крейсер)",
        29L to "Turtle (Черепаха)"
    )

    val ALL_SKINS = (1..125).toList()
    val ALL_AVATARS = (1..41).toList()
    val DISTRICTS = listOf("district1", "district2", "district3", "district4", "district5", "district6")

    val PIGGY_BANKS = listOf(
        "shop.piggybankmlp7.3" to (-2000200805L to "🐷 Золотая Свинья Tier 3 (250K)"),
        "shop.piggybankmlp7.2" to (-2000200806L to "🐷 Серебряная Свинья Tier 2 (100K)"),
        "shop.piggybankmlp7.1" to (-2000200807L to "🐷 Бронзовая Свинья Tier 1 (40K)")
    )

    val SEASONS = listOf(
        "season_pass_s1" to "🌟 Сезон 1: Запуск Сабвей Сити",
        "season_pass_s2" to "🎃 Сезон 2: Хэллоуинский Карнавал",
        "season_pass_s3" to "❄️ Сезон 3: Зимняя Сказка",
        "season_pass_s4" to "🌸 Сезон 4: Весенний Фестиваль"
    )

    val SCHEDULED_CAMPAIGNS = listOf(
        "campaign_trick_or_treat" to "🎃 Сладость или Гадость (Trick or Treat)",
        "campaign_floor_is_lava" to "🌋 Пол — это лава (Floor Is Lava)",
        "campaign_speed_run" to "⚡ Ускоренный забег (Speed Run)",
        "campaign_tag_time" to "⏱️ Забег на время (Tag Time Attack)"
    )

    val ALL_IAP_PURCHASES = listOf(
        "remove.ads", "premium_pass", "starterpack_bundle",
        "intermediatepack_bundle", "expertpack_bundle"
    )

    val IAP_CATALOG = listOf(
        "remove.ads" to "🚫 Отключение Рекламы (No Ads)",
        "premium_pass" to "🎟️ Премиум Пропуск Сезона",
        "starterpack_bundle" to "📦 Стартовый Набор Игрока",
        "intermediatepack_bundle" to "💎 Продвинутый Набор",
        "expertpack_bundle" to "👑 Экспертный Мега-Набор"
    )
}
