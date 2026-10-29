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
        "4" to "🚀 Фора на старте (Headstarts)",
        "5" to "⭐ Бустеры очков (Score Boosters)",
        "6" to "🎟️ Монеты Событий (Event Coins)",
        "12" to "🎃 Сезонные Жетоны (Season Hunt Tokens)"
    )

    val ACHIEVEMENTS_LIST = listOf(
        ClassicAchievementInfo("social_friends_media", "🤝 Друг звёзд", "Добавить в друзья известную медийную личность", 3),
        ClassicAchievementInfo("brawl_stars_crew", "⭐ Коллекция Brawl Stars", "Собрать всех персонажей коллаборации Brawl Stars", 3),
        ClassicAchievementInfo("rivals_conqueror", "👑 Покоритель Соперников", "Собрать и победить всех персонажей из команды Rivals", 3),
        ClassicAchievementInfo("core_crew_assembled", "🏄 Золотой состав", "Собрать всех персонажей Core Crew", 3),
        ClassicAchievementInfo("super_runners_club", "⚡ Клуб Супер-Бегунов", "Собрать всю команду Super Runners", 3),
        ClassicAchievementInfo("jump_jackpot", "💎 Счастливчик Супер-Джека", "Выиграть Супер Джекпот в Mystery Box", 3),
        ClassicAchievementInfo("score_legend", "🏃 Легенда Дистанции", "Набрать более 10 000 000 очков в одном забеге", 3),
        ClassicAchievementInfo("hoverboard_rider", "🛹 Мастер Ховерборда", "Проехать 100 000 метров на ховерборде", 3),
        ClassicAchievementInfo("coin_collector", "💰 Золотая лихорадка", "Собрать 1 000 000 монет на рельсах", 3),
        ClassicAchievementInfo("mission_master", "📜 Ветеран Заданий", "Завершить 100 наборов миссий", 3),
        ClassicAchievementInfo("word_hunt_champ", "🔤 Охотник за словами", "Собрать 50 ежедневных слов Word Hunt", 3),
        ClassicAchievementInfo("top_run_champion", "🏅 Чемпион Страны", "Занять 1-е место в Алмазной или Чемпионской лиге Top Run", 3),
        ClassicAchievementInfo("board_collector", "🛸 Повелитель Досок", "Собрать более 150 ховербордов в коллекцию", 3),
        ClassicAchievementInfo("outfit_fashionista", "👕 Икона Стиля", "Открыть 200 костюмов для персонажей", 3)
    )

    // ==================== РАМКИ ПРОФИЛЯ (profile_frame.json) ====================
    val ALL_FRAMES = listOf(
        "default_frame" to "Стандартная деревянная (Default)",
        "bronze_frame" to "Бронзовая рамка Top Run",
        "silver_frame" to "Серебряная рамка Top Run",
        "gold_frame" to "Золотая рамка Top Run",
        "diamond_frame" to "Алмазная рамка Top Run",
        "champion_frame" to "Рамка Чемпиона Top Run",
        "frame_bobTheBlob" to "🟢 Фирменная рамка Bob the Blob",
        "frame_slime" to "🧪 Слизевая рамка (Slime Frame)",
        "frame_toxic" to "☣️ Токсичная рамка (Toxic Slime)",
        "brawl_frame" to "⭐ Brawl Stars Эксклюзив",
        "brawl_gold_frame" to "👑 Brawl Stars Золотая рамка",
        "rivals_gold_frame" to "⚔️ Рамка победителя Rivals",
        "super_runner_frame" to "⚡ Неоновая рамка Super Runner",
        "cyber_neon_frame" to "Кибернетическая рамка",
        "neon_blue_frame" to "Неоновая синяя рамка",
        "neon_pink_frame" to "Неоновая розовая рамка",
        "halloween_spooky_frame" to "Хэллоуин (Тыквенная)",
        "halloween_bat_frame" to "Летучие мыши (Хэллоуин)",
        "holiday_winter_frame" to "Зимняя морозная рамка",
        "holiday_ice_frame" to "Ледяная кристаллическая рамка",
        "lunar_dragon_frame" to "Лунный Новый Год (Дракон)",
        "lunar_gold_frame" to "Золотой Дракон",
        "lunar_rabbit_frame" to "Лунный Кролик",
        "tokyo_blossom_frame" to "Цветущая Сакура (Токио)",
        "anniversary_vip_frame" to "Юбилейная VIP рамка",
        "anniversary_10th_frame" to "10 лет Subway Surfers",
        "anniversary_11th_frame" to "11 лет Subway Surfers",
        "anniversary_12th_frame" to "12 лет Subway Surfers",
        "subway_city_frame" to "Subway City Неон",
        "space_galaxy_frame" to "Галактическая звёздная рамка",
        "underwater_frame" to "Глубоководная рамка",
        "flame_fire_frame" to "Огненная рамка (Пламя)",
        "electric_lightning_frame" to "Грозовая электрическая рамка",
        "graffiti_urban_frame" to "Граффити (Стрит-арт)",
        "arcade_retro_frame" to "Ретро-Аркада (8-Bit Pixel)",
        "easter_spring_frame" to "Весенняя пасхальная рамка",
        "summer_beach_frame" to "Пляжная летняя рамка",
        "pride_rainbow_frame" to "Радужная рамка Pride",
        "valentines_heart_frame" to "Рамка с сердечками (Валентин)",
        "pirate_treasure_frame" to "Пиратское золото"
    )

    // ==================== АВАТАРКИ / ПОРТРЕТЫ (profile_portrait.json) ====================
    val ALL_PORTRAITS = listOf(
        // Вся линейка Bob the Blob (настоящие внутриигровые ID)
        "portrait_bobTheBlob" to "🟢 Bob the Blob (Оригинальная аватарка)",
        "portrait_bobTheBlob_jingle" to "🔔 Bob the Blob (Jingle Outfit)",
        "portrait_bobTheBlob_slime" to "🧪 Bob the Blob (Slime Outfit)",
        "portrait_bobTheBlob_graffiti" to "🎨 Bob the Blob (Граффити)",

        // Sean Snow и сезонные новинки
        "portrait_seanSnow" to "❄️ Sean Snow (Шон Сноу)",
        "portrait_seanSnow_dragon" to "🐲 Sean Snow (Dragon Outfit)",

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
        "portrait_ninja_yang" to "Yang Ninja (Инь-Ян)",
        "portrait_ninja_flame" to "Flame Ninja (Огонь)",
        "portrait_tasha_default" to "Tasha (Таша)",
        "portrait_tasha_cheer" to "Cheer Tasha (Чирлидер)",
        "portrait_frank_default" to "Frank (Фрэнк)",
        "portrait_frank_clown" to "Clown Frank (Клоун)",
        "portrait_king_default" to "King (Король)",
        "portrait_king_royal" to "Royal King (Монарх)",
        "portrait_zoe_default" to "Zoe (Зои)",
        "portrait_zoe_zombie" to "Zombie Zoe (Зомби)",
        "portrait_ella_default" to "Ella (Элла)",
        "portrait_brody_default" to "Brody (Броуди)",
        "portrait_princek_default" to "Prince K (Принц Кей)",
        "portrait_missmaia_default" to "Miss Maia (Мисс Майя)",
        "portrait_dino" to "Dino (Дино)",

        // Brawl Stars
        "portrait_brawl_colt" to "⭐ Brawl Stars - Colt",
        "portrait_brawl_shelly" to "⭐ Brawl Stars - Shelly",
        "portrait_brawl_spike" to "⭐ Brawl Stars - Spike",
        "portrait_brawl_crow" to "⭐ Brawl Stars - Crow",
        "portrait_brawl_leon" to "⭐ Brawl Stars - Leon",
        "portrait_brawl_special" to "⭐ Brawl Stars - Эмблема",

        // Super Runners & Rivals
        "portrait_superrunner_jake" to "⚡ Super Runner Jake",
        "portrait_superrunner_tricky" to "⚡ Super Runner Tricky",
        "portrait_superrunner_fresh" to "⚡ Super Runner Fresh",
        "portrait_superrunner_yutani" to "⚡ Super Runner Yutani",
        "portrait_catrina" to "⚔️ Catrina (Rivals)",
        "portrait_tankbot" to "⚔️ Tankbot (Rivals)",
        "portrait_zayn" to "⚔️ Zayn (Rivals)",
        "portrait_rosa" to "⚔️ Rosa (Rivals)",
        "portrait_harumi" to "⚔️ Harumi (Rivals)",

        // Звания Top Run
        "portrait_top_run_bronze" to "🥉 Бронзовый призёр Top Run",
        "portrait_top_run_silver" to "🥈 Серебряный призёр Top Run",
        "portrait_top_run_gold" to "🥇 Золотой призёр Top Run",
        "portrait_top_run_diamond" to "💎 Алмазный призёр Top Run",
        "portrait_top_run_champion" to "👑 Гранд-Чемпион Top Run"
    )

    val ALL_TROPHIES = listOf(
        "trophy_top_run_gold", "trophy_marathon_master", "trophy_score_legend",
        "trophy_coins_millionaire", "trophy_mystery_jackpot", "trophy_all_characters",
        "trophy_all_boards", "trophy_season_hunt_conqueror", "trophy_diamond_medal"
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
    val CORE_CREW_SURFERS = setOf("jake", "tricky", "fresh", "spike", "yutani", "lucy", "tagbot", "ninja", "tasha", "frank", "king", "zoe", "ella", "brody", "princek", "missmaia", "dino")

    // ==================== СЁРФЕРЫ (characters_inventory.json) ====================
    val CLASSIC_SURFERS: List<Pair<String, Pair<String, List<String>>>> = listOf(
        // Bob The Blob и Sean Snow с точными ID
        "bobTheBlob" to Pair("🟢 Bob the Blob", listOf("default", "jingle", "slime")),
        "seanSnow" to Pair("❄️ Sean Snow", listOf("default", "dragon")),

        // Core Crew
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
        "brody" to Pair("Brody (Броуди)", listOf("default", "posh", "chill")),
        "princek" to Pair("Prince K (Принц Кей)", listOf("default", "shine", "jaguar", "oldmoney")),
        "missmaia" to Pair("Miss Maia (Мисс Майя)", listOf("default", "lead", "neondemon")),
        "dino" to Pair("Dino (Дино)", listOf("default", "party")),

        // Brawl Stars
        "brawl_colt" to Pair("⭐ Colt (Brawl Stars)", listOf("default", "gunslinger", "outlaw")),
        "brawl_shelly" to Pair("⭐ Shelly (Brawl Stars)", listOf("default", "star", "witch")),
        "brawl_spike" to Pair("⭐ Spike (Brawl Stars)", listOf("default", "sakura", "robo")),
        "brawl_crow" to Pair("⭐ Crow (Brawl Stars)", listOf("default", "white", "phoenix")),
        "brawl_leon" to Pair("⭐ Leon (Brawl Stars)", listOf("default", "shark", "dino")),

        // Rivals & Super Runners
        "rival_catrina" to Pair("⚔️ Catrina (Rivals)", listOf("default", "fiesta", "shadow")),
        "rival_tankbot" to Pair("⚔️ Tankbot (Rivals)", listOf("default", "iron", "gold")),
        "rival_zayn" to Pair("⚔️ Zayn (Rivals)", listOf("default", "stealth", "flame")),
        "rival_rosa" to Pair("⚔️ Rosa (Rivals)", listOf("default", "carnival", "bloom")),
        "rival_harumi" to Pair("⚔️ Harumi (Rivals)", listOf("default", "mecha", "ghost")),
        "superrunner_jake" to Pair("⚡ Super Runner Jake", listOf("default", "neon")),
        "superrunner_tricky" to Pair("⚡ Super Runner Tricky", listOf("default", "neon")),
        "superrunner_fresh" to Pair("⚡ Super Runner Fresh", listOf("default", "bass")),
        "superrunner_yutani" to Pair("⚡ Super Runner Yutani", listOf("default", "alpha")),

        // Популярные герои World Tour
        "akira" to Pair("Akira (Токио)", listOf("default", "rider")),
        "alba" to Pair("Alba (Космос)", listOf("default", "cowgirl")),
        "alex" to Pair("Alex (Москва)", listOf("default", "winter")),
        "ali" to Pair("Ali (Каир)", listOf("default", "tourist")),
        "amira" to Pair("Amira (Марракеш)", listOf("default", "genie")),
        "amy" to Pair("Amy (Шанхай)", listOf("default", "fortune")),
        "andy" to Pair("Andy (Сиэтл)", listOf("default", "gamer")),
        "astrid" to Pair("Astrid (Норвегия)", listOf("default", "valkyrie")),
        "awa" to Pair("Awa (Дакар)", listOf("default", "solar")),
        "bjarki" to Pair("Bjarki (Исландия)", listOf("default", "power")),
        "boombot" to Pair("🤖 Boombot", listOf("default", "bass")),
        "callum" to Pair("Callum (Эдинбург)", listOf("default", "changeling")),
        "carlos" to Pair("Carlos (Буэнос-Айрес)", listOf("default", "gaucho")),
        "carmen" to Pair("Carmen (Рио)", listOf("default", "shake")),
        "cleo" to Pair("Cleo (Лас-Вегас)", listOf("default", "showgirl")),
        "coco" to Pair("Coco (Париж)", listOf("default", "jester")),
        "dummy" to Pair("🦿 Dummy", listOf("default", "crash")),
        "eddy" to Pair("Eddy (Новый Орлеан)", listOf("default", "trick")),
        "edison" to Pair("Edison (Детройт)", listOf("default", "urban")),
        "elena" to Pair("Elena (Амазонка)", listOf("default", "safari")),
        "farkas" to Pair("Farkas (Будапешт)", listOf("default", "wolf")),
        "freya" to Pair("Freya (Викинги)", listOf("default", "viking")),
        "harumi" to Pair("Harumi (Токио)", listOf("default", "fury")),
        "hasina" to Pair("Hasina (Оазис)", listOf("default", "mirage")),
        "jaime" to Pair("Jaime (Барселона)", listOf("default", "fresco")),
        "jazzy" to Pair("Jazzy", listOf("default", "swing")),
        "kareem" to Pair("Kareem (Дубай)", listOf("default", "silk")),
        "lauren" to Pair("Lauren (Техас)", listOf("default", "pilot")),
        "malik" to Pair("Malik (Мумбаи)", listOf("default", "tusk")),
        "manny" to Pair("Manny (Мексика)", listOf("default", "luchador")),
        "mei" to Pair("Mei (Бали)", listOf("default", "islander")),
        "monkbot" to Pair("🧘 Monkbot", listOf("default", "iron")),
        "morgan" to Pair("Morgan (Пиратка)", listOf("default", "captain")),
        "noon" to Pair("Noon (Бангкок)", listOf("default", "siam")),
        "olivia" to Pair("Olivia (Ванкувер)", listOf("default", "skate")),
        "rabbot" to Pair("🐰 Rabbot", listOf("default", "easter")),
        "ramona" to Pair("Ramona (Гавана)", listOf("default", "elegance")),
        "rosa" to Pair("Rosa (Мексика)", listOf("default", "fox")),
        "salma" to Pair("Salma (Марракеш)", listOf("default", "nomad")),
        "scarlett" to Pair("Scarlett (Хэллоуин)", listOf("default", "crimson")),
        "spacebot" to Pair("🚀 Spacebot", listOf("default", "lunar")),
        "tankbot" to Pair("Tankbot (Танкбот)", listOf("default", "iron")),
        "trym" to Pair("Trym (Исландия)", listOf("default", "noble")),
        "wayne" to Pair("Wayne (Голливуд)", listOf("default", "knight")),
        "yassine" to Pair("Yassine (Касабланка)", listOf("default", "runner")),
        "zayn" to Pair("Zayn (Берлин)", listOf("default", "stealth")),
        "zuri" to Pair("Zuri (Кения)", listOf("default", "city"))
    )

    // ==================== ХОВЕРБОРДЫ (boards_inventory.json) ====================
    val CLASSIC_BOARDS: List<Pair<String, Pair<String, List<String>>>> = listOf(
        "default" to Pair("Hoverboard (Стандартная)", emptyList()),
        // Fire Starter и Torito с точными ID
        "fireStarter" to Pair("🔥 Fire Starter", listOf("superSpeed", "trailFire")),
        "torito" to Pair("🐂 Torito (Мексиканский бык)", listOf("superJump", "trailFire")),
        "bobTheBlobBoard" to Pair("🟢 Bob the Blob Board", listOf("superJump", "smoothDrift")),

        // Популярные доски со спец-способностями
        "brawl_board" to Pair("⭐ Brawl Stars Board", listOf("superJump", "doubleJump", "superSpeed")),
        "star" to Pair("Starboard (Звёздная)", listOf("doubleJump", "superSpeed")),
        "bouncer" to Pair("Bouncer (Прыгучая)", listOf("superJump")),
        "lowrider" to Pair("Lowrider (Низкая)", listOf("stayLow")),
        "daredevil" to Pair("Daredevil (Сорвиголова)", listOf("superSpeed")),
        "teleporter" to Pair("Teleporter (Телепорт)", listOf("teleport")),
        "windglider" to Pair("Windglider (Планер)", listOf("smoothDrift", "superJump")),
        "monster" to Pair("Monster (Монстр)", listOf("superJump", "superSpeed")),
        "skull_fire" to Pair("Skull Fire (Череп в огне)", listOf("doubleJump", "superSpeed")),
        "great_white" to Pair("Great White (Белая акула)", listOf("smoothDrift", "superSpeed")),
        "lumberjack" to Pair("Lumberjack (Лесоруб)", listOf("doubleJump", "stayLow")),
        "freestyler" to Pair("Freestyler (Фристайлер)", listOf("superJump", "doubleJump")),
        "hot_rod" to Pair("Hot Rod (Хот-род)", listOf("superSpeed", "speedUp")),
        "miami" to Pair("Miami (Майами)", listOf("superSpeed")),
        "liberty" to Pair("Liberty (Свобода)", listOf("superJump")),
        "outback" to Pair("Outback (Бумеранг)", listOf("stayLow")),
        "tokio" to Pair("Kitty (Токио Китти)", listOf("doubleJump")),
        "panda" to Pair("Panda (Панда)", listOf("superJump")),
        "cherry" to Pair("Cherry (Сакура)", listOf("smoothDrift")),
        "dragon" to Pair("Dragon (Дракон)", listOf("superSpeed", "doubleJump")),
        "surf_board" to Pair("Big Kahuna (Гавайская волна)", listOf("smoothDrift", "stayLow")),
        "toucan" to Pair("Toucan (Тукан Рио)", listOf("superJump")),
        "kick_off" to Pair("Kick-Off (Футбольная)", listOf("superSpeed")),
        "gladiator" to Pair("Scoot (Римский Скутер)", listOf("superSpeed", "zapSideways")),
        "rose" to Pair("Rose (Парижская Роза)", listOf("smoothDrift")),
        "chameleon" to Pair("Chameleon (Хамелеон)", listOf("zapSideways", "superSpeed")),
        "gondola" to Pair("Gondola (Венецианская Гондола)", listOf("smoothDrift")),
        "yellow_cab" to Pair("Yellow Cab (Жёлтое Такси)", listOf("superSpeed")),
        "teddy" to Pair("Teddy (Плюшевый Мишка)", listOf("superJump")),
        "moose" to Pair("Moose (Канадский Лось)", listOf("superJump", "stayLow")),
        "rudolph" to Pair("Rudolph (Олень Рудольф)", listOf("smoothDrift", "superSpeed")),
        "snow_cloud" to Pair("Snow Cloud (Снежное облако)", listOf("smoothDrift")),
        "chug_chug" to Pair("Chug Chug (Паровозик)", listOf("superSpeed")),
        "cruiser" to Pair("Cruiser (Круизёр)", listOf("stayLow", "superSpeed")),
        "birdie" to Pair("Birdie (Птичка)", listOf("smoothDrift")),
        "prickly" to Pair("Prickly (Кактус)", listOf("doubleJump")),
        "phantom" to Pair("Phantom (Призрак)", listOf("zapSideways", "stayLow")),
        "turtle" to Pair("Turtle (Черепаха)", listOf("stayLow")),
        "magic_carpet" to Pair("Magic Carpet (Ковёр-самолёт)", listOf("smoothDrift", "superJump")),
        "Old_Dusty" to Pair("Old Dusty (Пыльная доска)", listOf("stayLow")),
        "solar" to Pair("Solar (Солнечная)", listOf("superSpeed")),
        "ramses" to Pair("Ramses (Фараон Рамзес)", listOf("doubleJump", "superSpeed")),
        "glacier" to Pair("Glacier (Ледник)", listOf("superJump", "doubleJump")),
        "valhalla" to Pair("Hammer (Молот Тора)", listOf("superJump", "superSpeed")),
        "thunder" to Pair("Thunderbolt (Молния Зевса)", listOf("superSpeed", "zapSideways")),
        "pixel_heart" to Pair("Pixel Heart (Пиксельное сердце)", listOf("doubleJump")),
        "chromebook" to Pair("Hover-1 (Кибер-доска)", listOf("superSpeed", "smoothDrift")),
        "rgb_board" to Pair("RGB Wave (Неоновая волна)", listOf("superSpeed", "doubleJump", "smoothDrift")),
        "future_wave" to Pair("Future Wave (Волна Будущего)", listOf("teleport", "superSpeed")),
        "cyber_cycle" to Pair("Cyber Cycle (Кибер-байк)", listOf("superSpeed", "stayLow")),
        "punk_board" to Pair("Anarchy (Панк-доска)", listOf("doubleJump", "superJump")),
        "bass_blaster" to Pair("Bass Blaster (Бас-бластер)", listOf("superJump", "superSpeed")),
        "skatecrow_board" to Pair("Scarecrow (Пугало Хэллоуин)", listOf("smoothDrift", "doubleJump")),
        "casket" to Pair("Casket (Гроб Вампира)", listOf("stayLow", "superSpeed")),
        "pumpkin" to Pair("Pumpkin (Тыква Хэллоуин)", listOf("superJump", "superSpeed"))
    )
}
