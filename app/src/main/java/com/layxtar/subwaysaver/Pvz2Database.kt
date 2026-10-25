package com.layxtar.subwaysaver

object Pvz2Database {

    const val PKG_PVZ2_ROW = "com.ea.game.pvz2_row"
    const val PKG_PVZ2_NA = "com.ea.game.pvz2_na"

    val CURRENCIES = listOf(
        "c" to "🪙 Монеты (Coins)",
        "g" to "💎 Алмазы / Кристаллы (Gems)",
        "m" to "🌿 Мяты для Арены (Mints)",
        "t" to "🥊 Перчатки Арены (Gauntlets)",
        "pf" to "⛽ Топливо Погони Пенни (Penny Fuel)",
        "s" to "🌱 Ростки Сада Дзен (Sprouts)"
    )

    // Полный список всех 214 растений из твоего magento_filter.json и products.json (включая самые новые!)
    val ALL_PLANT_IDS = listOf(
        // Самые новые растения (включая Scorchoak = pyroak)
        "pyroak", "thornwhip", "voltsnapdragon", "sakura", "blazingknight",
        "rose", "brainstem", "lemonaid", "sourshot", "devourbloom",
        "seashooter", "cornfetti", "hammeruit", "sweetheartsnare", "znakelily",
        "frostbonnet", "blazeleaf", "doomshroom", "blastspinner", "blastspinnercocoon",
        "mangofier", "aquavine", "minishroom", "guardshroom", "seashroom",
        "electricpeel", "dragonbabybruit", "dragonbruit", "iceshroom", "buduhboom",
        "cranjelly", "beansprout", "sundewtangler", "bamboospartan", "scaredyshroom",
        "maybee", "seaflora", "nightcap", "boomberry", "buzzbutton",
        "waterrabbit", "chillypepper", "meteorflower", "vamporcini", "tombtangler",
        "levitater", "megagatling", "rhubarbarian", "bramblebush", "blockoli",
        "buttercup", "teleportatomine", "tigergrass", "iceweed", "heathseeker",
        "noctarinecloud", "noctarine", "powervine", "solarsage", "inferno",
        "pvine", "boomflower", "draftodil", "gloomvine", "hocus",
        "stickybombrice", "boingsetta", "headbutter", "turkeypult", "murkadamia",
        "explodeovine", "puffball", "olivepit", "tumbleweed", "shinevine",
        "gumnut", "ultomato", "dartichoke", "icebloom", "pyrevine",
        "pumpkin", "imppear", "pokra", "blastberry", "electricitea",
        "dazeychain", "zoybeanpod", "burnade", "snappea", "minipeashooter",
        "appeasemint", "slingpea", "poisonpeashooter", "shadowpeashooter", "spearmint",
        "concealmint", "armamint", "enforcemint", "containmint", "enchantmint",
        "ailmint", "bombardmint", "reinforcemint", "enlightenmint", "wintermint",
        "peppermint", "filamint", "electriccurrant", "aloe", "hollybarrierleafplantfood",
        "hollybarrierleaf", "hollyknight", "solartomato", "goldbloom", "electricpeashooter",
        "caulipower", "hotdate", "parsnip", "witchhazel", "missiletoe",
        "bombegranate", "kiwibeast", "applemortar", "explodeonut", "wasabiwhip",
        "escaperoot", "bloominghearts", "shrinkingviolet", "coldsnapdragon", "grapeshot",
        "jackolantern", "electricblueberry", "cactus", "strawburst", "toadstool",
        "lavaguava", "dandelion", "firepeashooter", "hurrikale", "sapfling",
        "sweetpotato", "ghostpepper", "homingthistle", "chomper", "peanut",
        "hypnoshroom", "starfruit", "squash", "torchwood", "snowpea",
        "imitater", "jalapeno", "powerlily", "grimrose", "dusklobber",
        "shadowshroom", "nightshade", "moonflower", "primalpotatomine", "primalsunflower",
        "perfumeshroom", "primalwallnut", "primalpeashooter", "intensivecarrot", "sporeshroom",
        "garlic", "thymewarp", "celerystalker", "phatbeet", "sprout",
        "sungun", "sunpod", "goldleaf", "stallia", "endurian",
        "akee", "redstinger", "xshot", "stunion", "chardguard",
        "pepperpult", "hotpotato", "banana", "guacodile", "bowlingbulb",
        "tanglekelp", "lilypad", "magnetshroom", "sunbean", "fumeshroom",
        "puffshroom", "sunshroom", "powerplant", "magnifyinggrass", "holonut",
        "empea", "citron", "blover", "laser_bean", "marigold",
        "wintermelon", "melonpult", "peapod", "tallnut", "lightningreed",
        "chilibean", "splitpea", "threepeater", "spikerock", "springbean",
        "cherry_bomb", "coconutcannon", "spikeweed", "snapdragon", "kernelpult",
        "repeater", "bonkchoy", "twinsunflower", "gravebuster", "iceburg",
        "cabbagepult", "bloomerang", "potatomine", "wallnut", "peashooter",
        "sunflower"
    )

    // Все 40 игровых апгрейдов из magento_filter.json и products.json
    val GAME_UPGRADES = listOf(
        "7_slots", "sunshovel_lvl1", "sunshovel_lvl2", "pf_refresh",
        "pf_slots_lvl1", "starting_sun_lvl1", "wallnut_firstaid", "manual_mowers_1",
        "seedslot2", "startingsun2", "pf_slots_lvl2", "sunshovel3",
        "feature_quest_pinata_hunt_slot_1", "feature_quest_pinata_hunt_slot_2",
        "feature_powerup_snowball", "feature_powerup_wizardfinger",
        "feature_powerup_flick_zombie", "feature_powerup_flamethrower",
        "feature_powerup_vasebreaker_reveal", "feature_powerup_vasebreaker_butter",
        "feature_powerup_vasebreaker_move", "feature_powerup_beghouled_shuffle",
        "feature_powerup_beghouled_shovel", "feature_powerup_beghouled_wizardfinger",
        "world_trophy_egypt", "world_trophy_pirate", "world_trophy_cowboy",
        "world_trophy_future", "world_trophy_dark", "world_trophy_beach",
        "world_trophy_iceage", "world_trophy_lostcity", "world_trophy_eighties",
        "world_trophy_dino", "valuable_mowers", "upgrade_powerup_duration_1",
        "upgrade_powerup_duration_2", "feature_powerup_pinch",
        "world_trophy_modern", "feature_collectable_bacon"
    )

    // Градации наборов семян для вкладок Seeds, Seeds2, Seeds3 в магазине (как в файлах LinhYM)
    val SEED_PACK_AMOUNTS = listOf(1, 100, 10000, 100000, 1000000)

    // Новейшие растения для наглядного отображения в UI
    val NEWEST_PLANTS_HIGHLIGHT = listOf(
        "pyroak" to "🔥 Scorchoak / Pyroak (Огненный дуб)",
        "thornwhip" to "🌿 Thornwhip (Шипастый кнут)",
        "voltsnapdragon" to "⚡ Volt Snapdragon (Вольт-дракон)",
        "sakura" to "🌸 Sakura (Сакура)",
        "blazingknight" to "🗡️ Blazing Knight (Пылающий рыцарь)",
        "rose" to "🌹 Rose (Роза)",
        "brainstem" to "🧠 Brainstem (Мозговой стебель)",
        "lemonaid" to "🍋 Lemon-Aid (Лимонад)",
        "sourshot" to "🍏 Sour Shot (Кислый выстрел)",
        "devourbloom" to "🌺 Devour Bloom (Пожирающий цветок)",
        "seashooter" to "🌊 Seashooter (Морской стрелок)",
        "cornfetti" to "🍿 Cornfetti Popper (Корнфетти)",
        "hammeruit" to "🔨 Hammeruit (Молот-фрукт)",
        "sweetheartsnare" to "💖 Sweetheart Snare (Сладкая ловушка)",
        "znakelily" to "🐍 Znake Lily (Змеиная лилия)",
        "frostbonnet" to "❄️ Frost Bonnet (Ледяной капор)",
        "blazeleaf" to "🍂 Blaze Leaf (Пылающий лист)",
        "doomshroom" to "🍄 Doom-shroom (Гриб судьбы)"
    )
}
