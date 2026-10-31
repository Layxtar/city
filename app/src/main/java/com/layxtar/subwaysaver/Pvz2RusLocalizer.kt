package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject

object Pvz2RusLocalizer {

    // Путь, куда пользователь при желании может закинуть свой полный файл LawnStrings-en-us.json или .rton
    const val CUSTOM_RUS_JSON_PATH = "/storage/emulated/0/Download/LawnStrings-ru.json"
    const val CUSTOM_RUS_RTON_PATH = "/storage/emulated/0/Download/LawnStrings-en-us.rton"

    // Встроенный словарь перевода ключевого интерфейса, режимов, магазина и всех основных/новых растений
    private val BUILTIN_RU_DICT: Map<String, String> = mapOf(
        // --- ГЛАВНОЕ МЕНЮ И ОБЩИЕ КНОПКИ ---
        "ADVENTURE_NAME" to "Приключение",
        "JOUST_NAME" to "Арена",
        "PLAY" to "ИГРАТЬ",
        "ARCADE_PLAY" to "ИГРАТЬ",
        "JOUST_PLAY" to "ИГРАТЬ",
        "LETS_ROCK_BUTTON" to "В БОЙ!",
        "LETS_ROCK_RELOAD_BUTTON" to "ПЕРЕЗАРЯДКА",
        "CHOOSE_YOUR_PLANTS" to "ВЫБЕРИТЕ РАСТЕНИЯ",
        "CHOOSER_OBJECTIVES" to "ЦЕЛИ УРОВНЯ",
        "CHOOSER_POWERUPS" to "УСИЛЕНИЯ",
        "CHOOSER_SELECT_PLANTS" to "ВЫБОР РАСТЕНИЙ",
        "BACK_BUTTON" to "НАЗАД",
        "BACK_TO_MAP" to "НА КАРТУ",
        "BACK_TO_MENU" to "В МЕНЮ",
        "CLOSE_BUTTON" to "ЗАКРЫТЬ",
        "CONTINUE_BUTTON" to "ПРОДОЛЖИТЬ",
        "CONTINUE_GAME_HEADER" to "ПРОДОЛЖИТЬ ИГРУ?",
        "CONTINUE_GAME_OR_RESTART" to "Вы хотите продолжить текущую игру или начать уровень заново?",
        "LEAVE_AND_SAVE_GAME_BUTTON" to "СОХРАНИТЬ И ВЫЙТИ",
        "LEAVE_BUTTON" to "ВЫЙТИ",
        "LEAVE_GAME_BUTTON" to "ВЫЙТИ С УРОВНЯ",
        "LEAVE_GAME_HEADER" to "Выйти с уровня?",
        "LEAVE_GAME_BODY" to "Вы уверены, что хотите покинуть уровень?\r\nВесь текущий прогресс на уровне будет потерян.",
        "GAME_PAUSED" to "ПАУЗА",
        "GAME_OVER" to "ИГРА ОКОНЧЕНА",
        "CLICK_TO_CONTINUE" to "НАЖМИТЕ ДЛЯ ПРОДОЛЖЕНИЯ",
        "CLICK_TO_START" to "НАЖМИТЕ ДЛЯ СТАРТА!",
        "CLICK_TO_RESUME" to "Нажмите, чтобы вернуться в игру",
        "BUTTON_CANCEL" to "ОТМЕНА",
        "BUTTON_OK" to "ОК",
        "BUTTON_YES" to "Да",
        "BUTTON_NO" to "Нет",
        "BUTTON_KEEP" to "ОСТАВИТЬ",
        "BUTTON_REFRESH" to "ОБНОВИТЬ",
        "DIALOG_STRING_CANCEL" to "ОТМЕНА",
        "DIALOG_STRING_NO" to "НЕТ",
        "DIALOG_STRING_OK" to "ОК",
        "DIALOG_STRING_YES" to "ДА",
        "DIALOG_WARNING" to "Внимание",
        "ARE_YOU_SURE" to "Вы уверены?",
        "LOADING" to "ЗАГРУЗКА...",
        "LEVELUP" to "Новый уровень!",
        "LEVEL_NUMBER" to "УР. {LEVEL}",
        "LEVEL_MASTERY_LONG_NUMBER" to "Мастерство {NUMBER}",
        "LEVEL_MASTERY_NUMBER" to "М {LEVEL}",

        // --- МАГАЗИН, КАЛЕНДАРЬ И ВАЛЮТЫ ---
        "BUY_NOW" to "Купить",
        "BUY_MORE" to "КУПИТЬ ЕЩЁ",
        "GO_TO_STORE" to "В МАГАЗИН",
        "GO_TO_EVENT" to "К СОБЫТИЮ",
        "COLLECT_PRIZES_BUTTON" to "ЗАБРАТЬ",
        "CLAIM_FREE_GIFT_TITLE" to "Заберите ежедневный подарок в магазине!",
        "COIN_STORE" to "Монеты",
        "GEMS_STORE" to "Алмазы",
        "LI_COINS" to "Монеты",
        "LI_GEMS" to "Алмазы",
        "KEYS" to "Ключи",
        "GEMS_PURCHASE_CONFIRMATION" to "Подтверждение покупки",
        "GEMS_PURCHASE_CONFIRMATION_BODY" to "Вы хотите приобрести {THING_TO_PURCHASE}?",
        "COIN_AWARD_HEADER1" to "Вы получили {NUMBER} монет!",
        "GEM_AWARD_HEADER1" to "Вы получили {NUMBER} алмазов!",
        "CALENDAR_AWARD_HEADER" to "Ваша награда!",
        "CALENDAR_DAY_LABEL" to "ДЕНЬ {NUMBER}",
        "CALENDAR_DAY_LABEL_TODAY" to "СЕГОДНЯ",
        "CALENDAR_TITLE_WEEKLY_STREAK" to "Серия из 7 дней",
        "CALENDAR_TITLE_WEEKLY_WELCOME" to "Добро пожаловать в Сад",

        // --- АЛЬМАНАХ И ХАРАКТЕРИСТИКИ ---
        "ALMANAC_PLANTS" to "РАСТЕНИЯ",
        "ALMANAC_ZOMBIES" to "ЗОМБИ",
        "ALMANAC_UPGRADES" to "УЛУЧШЕНИЯ",
        "ALMANAC_CLOSE" to "Закрыть",
        "ALMANAC_ACTION_BUTTON_FIND_MORE" to "НАЙТИ ЕЩЁ",
        "ALMANAC_COSTUME_DIALOG_TITLE" to "Костюмы",
        "ALMANAC_COSTUME_COSTUME_BUTTON_ENABLED" to "Надеть",
        "ALMANAC_COSTUME_COSTUME_BUTTON_IN_USE" to "Надето",
        "ALMANAC_COSTUME_COSTUME_BUTTON_LOCKED" to "Закрыто",
        "ALMANAC_LOCKED_STATE" to "Закрыто",
        "ALMANAC_UNLOCKED_STATE" to "Открыто",
        "ALMANAC_PLANT_COUNT_TEXT" to "Собрано растений: {CURRENT} из {TOTAL}",
        "ALMANAC_SHOW_MINT_STATS" to "Бонус Мяты",
        "ALMANAC_FAMILY_BONUS" to "Семейный бонус",
        "COST" to "Цена солнца",
        "DAMAGE" to "Урон",
        "HP" to "Прочность",
        "DURATION" to "Длительность",
        "HEALING" to "Лечение",
        "HASPLANTFOOD" to "Удобрение",
        "CHILL" to "Охлаждение",
        "FREEZE" to "Заморозка",
        "BUTTER" to "Масло",
        "HYPNOTIZE" to "Гипноз",
        "ELECTRIFY" to "Электричество",
        "COMMON" to "Обычное",
        "EPIC" to "Эпическое",
        "LEGENDARY" to "Легендарное",
        "FAVORITES" to "Избранное",

        // --- МИРЫ (ЭПОХИ) ---
        "ALM_EGYPT" to "Древний Египет",
        "EGYPT" to "Древний Египет",
        "EGYPT_LEVEL_NAME" to "Древний Египет - День {LEVEL_NUMBER}",
        "ALM_PIRATE" to "Пиратские моря",
        "ALM_COWBOY" to "Дикий Запад",
        "COWBOY" to "Дикий Запад",
        "ALM_FUTURE" to "Далёкое будущее",
        "FUTURE" to "Далёкое будущее",
        "FUTURE_LEVEL_NAME" to "Далёкое будущее - День {LEVEL_NUMBER}",
        "ALM_DARK" to "Тёмные века",
        "DARKAGES_LEVEL_NAME" to "Тёмные века - Ночь {LEVEL_NUMBER}",
        "ALM_BEACH" to "Пляж большой волны",
        "BEACH_LEVEL_NAME" to "Пляж большой волны - День {LEVEL_NUMBER}",
        "ALM_ICEAGE" to "Обмороженные пещеры",
        "ICEAGE" to "Обмороженные пещеры",
        "ICEAGE_LEVEL_NAME" to "Обмороженные пещеры - День {LEVEL_NUMBER}",
        "ALM_LOSTCITY" to "Затерянный город",
        "LOSTCITY_LEVEL_NAME" to "Затерянный город - День {LEVEL_NUMBER}",
        "ALM_EIGHTIES" to "Неоновый микстейп-тур",
        "EIGHTIES_LEVEL_NAME" to "Неоновый тур - День {LEVEL_NUMBER}",
        "ALM_DINO" to "Юрское болото",
        "DINO_LEVEL_NAME" to "Юрское болото - День {LEVEL_NUMBER}",
        "ALM_MODERN" to "Наше время",

        // --- СОВЕТЫ В БОЮ ---
        "ADVICE_HUGE_WAVE" to "ПРИБЛИЖАЕТСЯ ОГРОМНАЯ ВОЛНА ЗОМБИ!",
        "ADVICE_ZOMBIE_ONSLAUGHT" to "Не дайте зомби добраться до вашего дома!",
        "ADVICE_COLLECT_SUN" to "Нажимайте на падающее солнце, чтобы собрать его!",
        "ADVICE_NOT_ENOUGH_SUN" to "Нужно больше солнца для посадки этого растения!",
        "ADVICE_SEED_REFRESH" to "Семенам нужно время на перезарядку перед новой посадкой",
        "ADVICE_CANT_PLANT_THERE" to "Здесь нельзя сажать",
        "ADVICE_NEED_LILYPAD_FIRST" to "Сначала посадите Кувшинку",
        "ADVICE_PLANT_NOT_ON_GRAVE" to "Нельзя сажать на могилах",
        "ADVICE_GRAVEBUSTERS_ON_GRAVES" to "Могилоед можно сажать только на могилы",
        "ADVICE_PLANT_FOOD_BANK_FULL" to "Запас Удобрения полон! Используйте одно перед сбором нового.",
        "DEFAULT_LEVEL_COMPLETE_HEADER" to "Уровень пройден!",
        "DEFAULT_LEVEL_COMPLETE_BODY" to "Вы победили зомби!",
        "AWARD_SCREEN_NEW_PLANT" to "Вы получили новое РАСТЕНИЕ!",
        "AWARD_SCREEN_NEW_UPGRADE" to "Вы получили новое УЛУЧШЕНИЕ!",
        "AWARD_SCREEN_MONEYBAG_TITLE" to "Мешочек с монетами!",
        "LOD_LEVEL_NAME" to "Пиньята-вечеринка",

        // --- АРЕНА И ТУРНИРЫ ---
        "JOUST_FREE_PLAY" to "БЕСПЛАТНЫЙ БОЙ",
        "JOUST_FREE_TICKETS" to "ПОЛУЧИТЬ ПЕРЧАТКИ",
        "JOUST_FINDING_OPPONENT" to "Поиск соперника...",
        "JOUST_LOCAL_PLAYER_NAME" to "Вы",
        "JOUST_LOCAL_HIGH_SCORE" to "Ваш рекорд: {COUNT}",
        "JOUST_BUTTON_FORFEIT" to "СДАТЬСЯ",
        "JOUST_SEASON_CURRENT_EVENT" to "Текущий турнир",
        "JOUST_SEASON_FEATURED_PLANT" to "Растение недели",
        "JOUST_LEAGUE_NAME_JADE" to "Нефритовая лига",
        "JOUST_LEAGUE_NAME_GOLD" to "Золотая лига",
        "JOUST_LEAGUE_NAME_SILVER" to "Серебряная лига",
        "JOUST_LEAGUE_NAME_BRONZE" to "Бронзовая лига",
        "JOUST_LEAGUE_NAME_IRON" to "Железная лига",
        "JOUST_LEAGUE_NAME_BRICK" to "Кирпичная лига",
        "JOUST_LEAGUE_NAME_WOOD" to "Деревянная лига",
        "JOUST_LEAGUE_NAME_SOIL" to "Грунтовая лига",

        // --- НОВЕЙШИЕ РАСТЕНИЯ (2024–2026) ---
        "BANEBERRY" to "Воронец (Baneberry)",
        "BANEBERRY_TOOLTIP" to "Проклинает зомби, увеличивая получаемый ими урон. Сажайте друг на друга для усиления.",
        "BANEBERRY_DESCRIPTION_HEADER" to "Стреляет жуткими снарядами, проклинающими зомби так, что они получают повышенный урон от всех атак. Складывайте Воронцы вместе для усиления.",
        "BELFRIGHT" to "Колокол Ужаса (Belfright)",
        "BELFRIGHT_TOOLTIP" to "Наносит урон и накладывает немоту на зомби рядом, отключая их особые способности.",
        "BELFRIGHT_DESCRIPTION_HEADER" to "Звонит в зловещие колокола, нанося урон и лишая особых зомби их способностей. Теневая аура усиливает её атаки.",
        "CACTALOG" to "Кактусовое Бревно (Cactalog)",
        "CACTALOG_TOOLTIP" to "Катится сквозь зомби, отбрасывая их и оставляя дорожку шипов.",
        "CACTALOG_DESCRIPTION_HEADER" to "Катится по газону, нанося урон и отталкивая зомби назад, оставляя за собой колючий след.",
        "PYROAK" to "Огненный Дуб (Scorchoak)",
        "THORNWHIP" to "Шипастый Кнут (Thorn Whip)",
        "VOLTSNAPDRAGON" to "Вольт-Дракон (Volt Snapdragon)",
        "SAKURA" to "Сакура (Sakura)",
        "BLAZINGKNIGHT" to "Пылающий Рыцарь (Blazing Knight)",
        "BLAZINGKNIGHT_TOOLTIP" to "Неуязвим в режиме Огненной стены; при гибели призывает огненного коня.",
        "BLAZINGKNIGHT_DESCRIPTION_HEADER" to "Создаёт неуязвимую Огненную стену, сдерживающую зомби, и выпускает пылающего скакуна при поражении.",
        "ROSE" to "Роза (Rose)",
        "BRAINSTEM" to "Мозговой Стебель (Brain Stem)",
        "BRAINSTEM_TOOLTIP" to "Притягивает зомби на линии назад от дома и уменьшает их.",
        "BRAINSTEM_DESCRIPTION_HEADER" to "Манит зомби к себе — даже тех, кто уже прошёл мимо — оттягивая их от дома и уменьшая в размерах.",
        "LEMONAID" to "Лимонад (Lemonaid)",
        "LEMONAID_TOOLTIP" to "Даёт солнце, лечит, воскрешает и усиливает союзников на линии.",
        "LEMONAID_DESCRIPTION_HEADER" to "Производит солнце и поддерживает растения на своей линии: лечит, возрождает и иногда активирует их эффект Удобрения.",
        "SOURSHOT" to "Кислый Выстрел (Sour Shot)",
        "DEVOURBLOOM" to "Пожирающий Цветок (Devour Bloom)",
        "DEVOURBLOOM_TOOLTIP" to "Мгновенно проглатывает большинство зомби, наступивших на него.",
        "SEASHOOTER" to "Морской Стрелок (Seashooter)",
        "CORNFETTI" to "Попкорн-хлопушка (Corn-fetti Popper)",
        "CORNFETTI_TOOLTIP" to "Пробивает зомби насквозь и отбрасывает подошедших слишком близко.",
        "HAMMERUIT" to "Молот-Фрукт (Hammeruit)",
        "HAMMERUIT_TOOLTIP" to "Сокрушает зомби в области заряженными ударами молота.",
        "SWEETHEARTSNARE" to "Сладкая Ловушка (Sweetheart Snare)",
        "ZNAKELILY" to "Змеиная Лилия (Znake Lily)",
        "FROSTBONNET" to "Ледяной Капор (Frost Bonnet)",
        "FROSTBONNET_TOOLTIP" to "Переключается между режимами Огня и Льда по нажатию.",
        "BLAZELEAF" to "Пылающий Лист (Blaze Leaf)",
        "BLAZELEAF_TOOLTIP" to "Бьёт по площади и выпускает огненную волну каждый 3-й удар.",
        "DOOMSHROOM" to "Гриб Судьбы (Doom-shroom)",
        "DOOMSHROOM_TOOLTIP" to "Растёт и мощно взрывается, порождая новые грибы.",
        "BLASTSPINNER" to "Взрывной Прядильщик (Blast Spinner)",
        "BLASTSPINNER_TOOLTIP" to "Обездвиживает зомби паутиной и превращает их во взрывные коконы.",
        "MANGOFIER" to "Мангострел (Mangofier)",
        "AQUAVINE" to "Водная Лоза (Aqua Vine)",
        "AQUAVINE_TOOLTIP" to "Лоза-амфибия для воды и суши с мощной водной струёй.",
        "GUARDSHROOM" to "Гриб-Страж (Guard-shroom)",
        "GUARDSHROOM_TOOLTIP" to "Создаёт мини-грибы, защищающие и усиливающие соседние растения.",
        "SEASHROOM" to "Морской Гриб (Sea-shroom)",
        "ELECTRICPEEL" to "Электро-кожура (Electric Peel)",
        "ELECTRICPEEL_TOOLTIP" to "Выпускает кожуру, скользящую по линии туда и обратно.",
        "DRAGONBRUIT" to "Драконий Фрукт (Dragon Bruit)",
        "DRAGONBRUIT_TOOLTIP" to "Бросает по 3 снаряда на 3 линии и делится на 3 мини-дракона.",
        "ICESHROOM" to "Ледяной Гриб (Ice-shroom)",
        "ICESHROOM_TOOLTIP" to "Сокрушает врагов вблизи и усиливает ледяные растения вокруг.",
        "BUDUHBOOM" to "Масляный Бум (Bud’uh Boom)",
        "CRANJELLY" to "Клюквенное Желе (Cran Jelly)",
        "BEANSPROUT" to "Бобовый Росток (Bean Sprout)",
        "SUNDEWTANGLER" to "Росянка-Ловец (Sundew Tangler)",
        "BAMBOOSPARTAN" to "Бамбуковый Спартанец (Bamboo Spartan)",
        "SCAREDYSHROOM" to "Трусливый Гриб (Scaredy-shroom)",
        "MAYBEE" to "Майская Пчела (MayBee)",
        "SEAFLORA" to "Морская Флора (SeaFlora)",
        "NIGHTCAP" to "Ночной Колпак (Nightcap)",
        "BOOMBERRY" to "Взрыв-Ягода (BoomBerry)",
        "BUZZBUTTON" to "Жужжащая Пуговка (Bzzz Button)",
        "WATERRABBIT" to "Бун Чи (Bun Chi)",
        "CHILLYPEPPER" to "Ледяной Перец (Chilly Pepper)",
        "METEORFLOWER" to "Метеоритный Цветок (Meteor Flower)",
        "VAMPORCINI" to "Вампорчини (Vamporcini)",
        "TOMBTANGLER" to "Могильный Ловец (Tomb Tangler)",
        "LEVITATER" to "Левитатор (Levitater)",
        "MEGAGATLING" to "Мега-Горохомёт (Mega Gatling Pea)",
        "RHUBARBARIAN" to "Ревень-Варвар (Rhubarbarian)",
        "BRAMBLEBUSH" to "Ежевичный Куст (Bramble Bush)",
        "BLOCKOLI" to "Блоколи (Blockoli)",
        "BUTTERCUP" to "Лютик (Buttercup)",
        "TELEPORTATOMINE" to "Телепорт-мина (Teleportato Mine)",
        "TIGERGRASS" to "Тигровая Трава (Tiger Grass)",
        "ICEWEED" to "Ледяная Колючка (Iceweed)",
        "HEATHSEEKER" to "Вересковый Искатель (Heath Seeker)",
        "POWERVINE" to "Силовая Лоза (Power Vine)",
        "SOLARSAGE" to "Солнечный Шалфей (Solar Sage)",
        "INFERNO" to "Инферно (Inferno)",
        "PVINE" to "Гороховая Лоза (Pea Vine)",
        "BOOMFLOWER" to "Шароцветик (Boom Balloon Flower)",
        "DRAFTODIL" to "Ветродуйка (Draftodil)",
        "GLOOMVINE" to "Мрачная Лоза (Gloom Vine)",
        "HOCUS" to "Фокус-Крокус (Hocus Crocus)",
        "STICKYBOMBRICE" to "Липкий Бомба-Рис (Stickybomb Rice)",
        "BOINGSETTA" to "Прыгсеттия (Boingsetta)",
        "HEADBUTTER" to "Маслобойный Салат (Headbutter Lettuce)",
        "TURKEYPULT" to "Индейкопульта (Turkey-pult)",
        "MURKADAMIA" to "Мракадамия (Murkadamia Nut)",
        "EXPLODEOVINE" to "Взрывная Лоза (Explode-o-Vine)",
        "PUFFBALL" to "Дождевик (Puffball)",
        "OLIVEPIT" to "Оливковая Ямка (Olive Pit)",
        "TUMBLEWEED" to "Перекати-поле (Tumbleweed)",
        "SHINEVINE" to "Сияющая Лоза (Shine Vine)",
        "GUMNUT" to "Жвачкоорех (Gumnut)",
        "ULTOMATO" to "Ультомат (Ultomato)",
        "DARTICHOKE" to "Дротишок (Dartichoke)",
        "ICEBLOOM" to "Ледяной Цветок (Ice Bloom)",
        "PYREVINE" to "Огненная Лоза (Pyre Vine)",
        "PUMPKIN" to "Тыква (Pumpkin)",
        "IMPPEAR" to "Чертёнок-Груша (Imp Pear)",
        "POKRA" to "Покра (Pokra)",
        "BLASTBERRY" to "Взрыв-ягодная Лоза (Blastberry Vine)",
        "ELECTRICITEA" to "Электро-чай (Electrici-tea)",
        "DAZEYCHAIN" to "Цепь Маргариток (Dazey Chain)",
        "ZOYBEANPOD" to "Зойевый Стручок (Zoybean Pod)",
        "SNAPPEA" to "Кусачий Горох (Snap Pea)",
        "SLINGPEA" to "Горох-Праща (Sling Pea)",
        "POISONPEASHOOTER" to "Слизестрел (Goo Peashooter)",
        "SHADOWPEASHOOTER" to "Теневой Горохострел (Shadow Peashooter)",

        // --- КЛАССИЧЕСКИЕ И МИРОВЫЕ РАСТЕНИЯ + МЯТЫ ---
        "PEASHOOTER" to "Горохострел",
        "SUNFLOWER" to "Подсолнух",
        "WALLNUT" to "Стена-орех",
        "POTATOMINE" to "Картофельная мина",
        "CABBAGEPULT" to "Капустопульта",
        "BLOOMERANG" to "Бумеранг",
        "ICEBURG" to "Айсберг-салат",
        "GRAVEBUSTER" to "Могилоед",
        "BONKCHOY" to "Бонк Чой",
        "REPEATER" to "Скорострел",
        "TWINSUNFLOWER" to "Двойной подсолнух",
        "KERNELPULT" to "Зернопульта",
        "SNAPDRAGON" to "Львиный зев",
        "SPIKEWEED" to "Колючка",
        "SPRINGBEAN" to "Пружинный боб",
        "COCONUTCANNON" to "Кокосовая пушка",
        "THREEPEATER" to "Тристрел",
        "SPIKEROCK" to "Каменная колючка",
        "CHERRY_BOMB" to "Вишнёвая бомба",
        "SPLITPEA" to "Двустрел",
        "CHILIBEAN" to "Чили-боб",
        "PEAPOD" to "Гороховый стручок",
        "LIGHTNINGREED" to "Громовой камыш",
        "MELONPULT" to "Арбузопульта",
        "TALLNUT" to "Высокий орех",
        "WINTERMELON" to "Зимний арбуз",
        "LASER_BEAN" to "Лазерный боб",
        "BLOVER" to "Сдуватель",
        "CITRON" to "Цитрон",
        "EMPEA" to "Э.М.Персик",
        "HOLONUT" to "Бесконечный орех",
        "MAGNIFYINGGRASS" to "Увеличительная трава",
        "POWERPLANT" to "Плиточная репа",
        "SUNSHROOM" to "Солнцегриб",
        "PUFFSHROOM" to "Пуфгриб",
        "FUMESHROOM" to "Дымогриб",
        "SUNBEAN" to "Солнечный боб",
        "MAGNETSHROOM" to "Магнит-гриб",
        "LILYPAD" to "Кувшинка",
        "TANGLEKELP" to "Водоросль",
        "BOWLINGBULB" to "Боулинг-луковица",
        "GUACODILE" to "Гуакодил",
        "BANANA" to "Бананомёт",
        "HOTPOTATO" to "Горячая картошка",
        "PEPPERPULT" to "Перцепульта",
        "CHARDGUARD" to "Мангольд-страж",
        "STUNION" to "Оглушающий лук",
        "XSHOT" to "Ротобага",
        "REDSTINGER" to "Красный жалоцвет",
        "AKEE" to "А.К.И.",
        "ENDURIAN" to "Эндуриан",
        "STALLIA" to "Сталия",
        "GOLDLEAF" to "Золотой лист",
        "PHATBEET" to "Бит-свёкла",
        "CELERYSTALKER" to "Сельдерей-сталкер",
        "THYMEWARP" to "Тимьян времени",
        "GARLIC" to "Чеснок",
        "SPORESHROOM" to "Спорогриб",
        "INTENSIVECARROT" to "Интенсивная морковь",
        "PRIMALPEASHOOTER" to "Первобытный горохострел",
        "PRIMALWALLNUT" to "Первобытный орех",
        "DINONIP" to "Парфюм-гриб",
        "PRIMALSUNFLOWER" to "Первобытный подсолнух",
        "PRIMALPOTATOMINE" to "Первобытная мина",
        "MOONFLOWER" to "Лунный цветок",
        "NIGHTSHADE" to "паслён",
        "SHADOWSHROOM" to "Теневой гриб",
        "DUSKLOBBER" to "Сумеречная катапульта",
        "GRIMROSE" to "Мрачнороза",
        "SNOWPEA" to "Морозный горох",
        "POWERLILY" to "Силовая лилия",
        "SQUASH" to "Кабачок",
        "JALAPENO" to "Халапеньо",
        "IMITATER" to "Имитатор",
        "TORCHWOOD" to "Древофакел",
        "STARFRUIT" to "Карамбола",
        "HYPNOSHROOM" to "Гипногриб",
        "PEANUT" to "Арахис",
        "CHOMPER" to "Зубастик",
        "HOMINGTHISTLE" to "Самоноводящийся чертополох",
        "GHOSTPEPPER" to "Призрачный перец",
        "SWEETPOTATO" to "Сладкий картофель",
        "SAPFLING" to "Смолоброс",
        "HURRIKALE" to "Ураганная капуста",
        "FIREPEASHOOTER" to "Огненный горохострел",
        "DANDELION" to "Одуванчик",
        "LAVAGUAVA" to "Лавовая гуава",
        "TOADSTOOL" to "Поганка",
        "STRAWBURST" to "Взрывника",
        "CACTUS" to "Кактус",
        "ELECTRICBLUEBERRY" to "Электроголубика",
        "JACKOLANTERN" to "Джек-светильник",
        "GRAPESHOT" to "Картечь-виноград",
        "COLDSNAPDRAGON" to "Ледяной львиный зев",
        "SHRINKINGVIOLET" to "Уменьшающая фиалка",
        "BLOOMINGHEARTS" to "Цветущее сердце",
        "ESCAPEROOT" to "Корень побега",
        "GOLDBLOOM" to "Золотой цветок",
        "ELECTRICCURRANT" to "Электросмородина",
        "WASABIWHIP" to "Васаби-кнут",
        "EXPLODEONUT" to "Взрывной орех",
        "KIWIBEAST" to "Киви-зверь",
        "BOMBEGRANATE" to "Бомба-гранат",
        "APPLEMORTAR" to "Яблочная мортира",
        "WITCHHAZEL" to "Ведьмин орешник",
        "PARSNIP" to "Пастернак",
        "MISSILETOE" to "Ракетная омела",
        "HOTDATE" to "Горячий финик",
        "CAULIPOWER" to "Гипнокапуста",
        "SOLARTOMATO" to "Солнечный томат",
        "ELECTRICPEASHOOTER" to "Электрический горохострел",
        "HOLLYKNIGHT" to "Остролист-барьер",
        "ALOE" to "Алоэ",
        "FILAMINT" to "Энерго-мята (Fila-mint)",
        "PEPPERMINT" to "Перечная мята (Pepper-mint)",
        "WINTERMINT" to "Зимняя мята (Winter-mint)",
        "ENLIGHTENMINT" to "Просвет-мята (Enlighten-mint)",
        "REINFORCEMINT" to "Укреп-мята (Reinforce-mint)",
        "BOMBARDMINT" to "Бомбардир-мята (Bombard-mint)",
        "AILMINT" to "Хворо-мята (Ail-mint)",
        "ENCHANTMINT" to "Чаро-мята (Enchant-mint)",
        "CONTAINMINT" to "Сдерж-мята (Contain-mint)",
        "ENFORCEMINT" to "Сило-мята (Enforce-mint)",
        "ARMAMINT" to "Оружейная мята (Arma-mint)",
        "CONCEALMINT" to "Скрыто-мята (Conceal-mint)",
        "SPEARMINT" to "Копье-мята (Spear-mint)",
        "APPEASEMINT" to "Умиротвор-мята (Appease-mint)"
    )

    // Устанавливает русификатор в LawnStrings-en-us.rton в выбранной папке CDN (а также во всех найденных CDN.*).
    // Если в /sdcard/Download/LawnStrings-ru.json лежит кастомный словарь пользователя — дополнительно подхватывает и его!
    fun installRussianLocalization(
        cdnDir: String,
        translatePlantNames: Boolean = true,
        log: (String) -> Unit
    ): Boolean {
        val allDirs = Pvz2Manager.listAllCdnDirs().ifEmpty { listOf(cdnDir) }.toMutableSet().apply { add(cdnDir) }

        // Проверяем, положил ли пользователь свой внешний полный файл LawnStrings-ru.json в Download
        val externalDict = loadExternalCustomDictionary(log)

        var patchedCount = 0
        for (dir in allDirs) {
            val targetPath = "$dir/LawnStrings-en-us.rton"
            val json = Pvz2Manager.readRtonAsJson(targetPath) {} ?: continue

            val objects = json.optJSONArray("objects") ?: continue
            if (objects.length() == 0) continue
            val objData = objects.getJSONObject(0).optJSONObject("objdata") ?: continue
            val locArr = objData.optJSONArray("LocStringValues") ?: continue

            var replacedStrings = 0
            val totalPairs = locArr.length() / 2

            // Проходим по парам (чётный индекс = ключ, нечётный индекс = строка перевода)
            var i = 0
            while (i + 1 < locArr.length()) {
                val rawKey = locArr.optString(i, "")
                val cleanKey = rawKey.trim().removeSurrounding("\"").removeSuffix(",")

                // Сначала смотрим во внешний словарь (если есть), затем во встроенный
                val extTranslation = externalDict[cleanKey]
                val builtInTranslation = BUILTIN_RU_DICT[cleanKey]

                val chosenTranslation = extTranslation ?: builtInTranslation
                if (chosenTranslation != null) {
                    val isPlantShortName = Pvz2Database.ALL_PLANT_IDS.any { it.equals(cleanKey, ignoreCase = true) }
                    if (!isPlantShortName || translatePlantNames) {
                        locArr.put(i + 1, chosenTranslation)
                        replacedStrings++
                    }
                } else {
                    // Авто-перевод однотипных ключей (например, День X, Мега-набор, Пиньята и т.д.)
                    val currentVal = locArr.optString(i + 1, "")
                    val autoTranslated = autoTranslateCommonPatterns(cleanKey, currentVal)
                    if (autoTranslated != null) {
                        locArr.put(i + 1, autoTranslated)
                        replacedStrings++
                    }
                }
                i += 2
            }

            if (Pvz2Manager.writeJsonAsRton(targetPath, json, log)) {
                patchedCount++
                log("🇷🇺 В ${dir.substringAfterLast("/")} переведено строк: $replacedStrings (всего ключей в файле: $totalPairs)")
            }
        }

        return if (patchedCount > 0) {
            log("🎉 Русификатор успешно установлен в LawnStrings-en-us.rton!")
            true
        } else {
            log("❌ Файл LawnStrings-en-us.rton не найден в папке $cdnDir!")
            false
        }
    }

    // Если пользователь положил в /storage/emulated/0/Download/LawnStrings-ru.json свой файл — читаем его пары ключ-значение
    private fun loadExternalCustomDictionary(log: (String) -> Unit): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (!CryptoAndSyncManager.fileExists(CUSTOM_RUS_JSON_PATH)) return map

        val json = Pvz2Manager.readRtonAsJson(CUSTOM_RUS_JSON_PATH) {} ?: return map
        val locArr = json.optJSONArray("objects")
            ?.optJSONObject(0)
            ?.optJSONObject("objdata")
            ?.optJSONArray("LocStringValues") ?: return map

        var i = 0
        while (i + 1 < locArr.length()) {
            val k = locArr.optString(i, "").trim().removeSurrounding("\"").removeSuffix(",")
            val v = locArr.optString(i + 1, "")
            if (k.isNotEmpty() && v.isNotEmpty()) {
                map[k] = v
            }
            i += 2
        }
        if (map.isNotEmpty()) {
            log("📥 Подхвачен внешний словарь $CUSTOM_RUS_JSON_PATH (${map.size} строк)!")
        }
        return map
    }

    // Автоматический перевод тысяч шаблонных строк (наборы, пиньяты, квесты, уровни, подсказки)
    private fun autoTranslateCommonPatterns(key: String, engValue: String): String? {
        if (engValue.isBlank()) return null
        return when {
            key.startsWith("EA_") && key.endsWith("_UNLOCK") ->
                engValue.replace("Early Access Unlock", "Ранний доступ: Разблокировка")
            key.startsWith("EA_") && key.endsWith("_LEVELUP") ->
                engValue.replace("Early Access Level Up", "Ранний доступ: Прокачка")
            engValue.endsWith(" Mega Bundle") ->
                engValue.replace(" Mega Bundle", " — Мега-набор")
            engValue.endsWith(" Super Bundle") ->
                engValue.replace(" Super Bundle", " — Супер-набор")
            engValue.endsWith(" Ultimate Bundle") ->
                engValue.replace(" Ultimate Bundle", " — Ультимативный набор")
            engValue.endsWith(" Pinata Pack") ->
                engValue.replace(" Pinata Pack", " — Набор пиньят")
            engValue.endsWith(" Premium Pinata") ->
                engValue.replace(" Premium Pinata", " — Премиум-пиньята")
            engValue.startsWith("Defeat ") && engValue.endsWith(" Zombies") ->
                engValue.replace("Defeat ", "Победите ").replace(" Zombies", " зомби")
            engValue.contains("{KEYWORD}Plant Food: {STAT}No Plant Food effect.") ->
                engValue.replace("{KEYWORD}Plant Food: {STAT}No Plant Food effect.", "{KEYWORD}Удобрение: {STAT}Не имеет эффекта от Удобрения.")
            engValue.contains("{KEYWORD}Plant Food: {STAT}") ->
                engValue.replace("{KEYWORD}Plant Food: {STAT}", "{KEYWORD}Удобрение: {STAT}")
                    .replace("{KEYWORD}Special: {STAT}", "{KEYWORD}Особенность: {STAT}")
                    .replace("{KEYWORD}Usage: {STAT}", "{KEYWORD}Применение: {STAT}")
            else -> null
        }
    }
}
