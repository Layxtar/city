package com.layxtar.subwaysaver

object Pvz2RusLocalizer {

    const val CUSTOM_RUS_JSON_PATH = "/storage/emulated/0/Download/LawnStrings-ru.json"

    // Полный словарь интерфейса, достижений, советов, описаний Альманаха и сюжетных диалогов Дэйва / Пенни / Зомбосса
    private val BUILTIN_RU_DICT: Map<String, String> = mapOf(
        // ==================== ГЛАВНОЕ МЕНЮ И ИНТЕРФЕЙС ====================
        "\"CHAINED_OFFERS_COMPLETE_TITLE\"," to "\"CHAINED_OFFERS_COMPLETE_TITLE\",",
        "CHAINED_OFFERS_COMPLETE_TITLE" to "Отличная работа!",
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
        "CONTINUE_ONWARDS_BUTTON" to "Двигаемся дальше!",
        "CONTINUE_GAME_HEADER" to "ПРОДОЛЖИТЬ ИГРУ?",
        "CONTINUE_GAME_OR_RESTART" to "Вы хотите продолжить текущую игру или начать уровень заново?",
        "LEAVE_AND_SAVE_GAME_BUTTON" to "СОХРАНИТЬ И ВЫЙТИ",
        "LEAVE_BUTTON" to "ВЫЙТИ",
        "LEAVE_GAME_BUTTON" to "ВЫЙТИ С УРОВНЯ",
        "LEAVE_GAME_HEADER" to "Выйти с уровня?",
        "LEAVE_GAME_BODY" to "Вы уверены, что хотите покинуть уровень?\r\nВесь текущий прогресс на уровне будет потерян.",
        "LEAVE_GAME_BODY_SHORT" to "Вы уверены, что хотите покинуть уровень?",
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
        "BUTTON_NEW_EPIC_QUEST_AVAILABLE" to "ДОСТУПЕН НОВЫЙ ЭПИЧЕСКИЙ КВЕСТ!",
        "BUTTON_NEW_QUEST_AVAILABLE" to "ДОСТУПНО НОВОЕ ЗАДАНИЕ!",
        "BUTTON_QUEST_COMPLETED" to "ЗАДАНИЕ ВЫПОЛНЕНО!",
        "DIALOG_STRING_CANCEL" to "ОТМЕНА",
        "DIALOG_STRING_NO" to "НЕТ",
        "DIALOG_STRING_OK" to "ОК",
        "DIALOG_STRING_YES" to "ДА",
        "DIALOG_WARNING" to "Внимание",
        "DIALOG_ARE_YOU_EXPERIENCED" to "Вы уже играли в Plants vs. Zombies раньше?",
        "DIALOG_NO_HAVENT_PLAYED" to "Кто такие зомби?",
        "DIALOG_YES_PLAYED_PVZ" to "Мы старые знакомые",
        "DIALOG_SKIP_TUTORIAL" to "Хотите пройти обучение основам игры Plants vs. Zombies?",
        "DIALOG_NO_DONT_SKIP" to "ПРОЙТИ ОБУЧЕНИЕ",
        "DIALOG_YES_SKIP_TUTORIAL" to "ПРОПУСТИТЬ",
        "DIALOG_TURBO_DESCRIPTION" to "Кнопка Турбо ускоряет игру: быстрее солнце, быстрее растения и быстрее зомби!",
        "ARE_YOU_SURE" to "Вы уверены?",
        "LOADING" to "ЗАГРУЗКА...",
        "LOADINGTEXT_DEFAULT_1" to "Расставляем единицы и нули по порядку...",
        "LOADINGTEXT_DEFAULT_2" to "Будим растения...",
        "LOADINGTEXT_DEFAULT_3" to "Будим зомби...",
        "LOADINGTEXT_DEFAULT_4" to "Добавляем милоты...",
        "LOADINGTEXT_DEFAULT_5" to "Вводим на 150% больше безумия!",
        "LOADINGTEXT_TIPS_GENERAL_1" to "Оставляйте место в задних рядах для дорогих стрелков.",
        "LOADINGTEXT_TIPS_GENERAL_2" to "Сажайте подсолнухи как можно раньше и чаще.",
        "LOADINGTEXT_TIPS_GENERAL_3" to "Старайтесь высаживать минимум 2 ряда растений, дающих солнце.",
        "LOADINGTEXT_TIPS_GENERAL_4" to "Копите солнце на более дорогие, но мощные растения.",
        "LOADINGTEXT_TIPS_GENERAL_5" to "Используйте Удобрение на Подсолнухах для мгновенного выброса солнца.",
        "LOADINGTEXT_TIPS_GENERAL_6" to "Во время действия Удобрения растение становится ненадолго неуязвимым.",
        "LOADINGTEXT_TIPS_GENERAL_7" to "Не бойтесь сажать растения на правой половине газона.",
        "LOADINGTEXT_TIPS_GENERAL_15" to "Сажайте Бонк Чоя прямо позади Стены-ореха.",
        "LOADINGTEXT_TIPS_GENERAL_16" to "Продолжайте сажать!",
        "LEVELUP" to "Новый уровень!",
        "LEVEL_NUMBER" to "УР. {LEVEL}",
        "LEVEL_MASTERY_LONG_NUMBER" to "Мастерство {NUMBER}",
        "LEVEL_MASTERY_NUMBER" to "М {LEVEL}",

        // ==================== СЮЖЕТНЫЕ ДИАЛОГИ: ОБУЧЕНИЕ И ДОМ ДЭЙВА ====================
        "TUTORIAL_DAVE_01" to "Привет, сосед! Меня зовут Безумный Дэйв! Но ты можешь звать меня просто Безумный Дэйв.",
        "TUTORIAL_DAVE_02" to "Слушай, тут такое дело... На твой газон надвигается толпа зомби, жаждущих мозгов!",
        "TUTORIAL_DAVE_03" to "К счастью, у меня припасено несколько боевых растений! Хватай пакет с семенами и сажай на траву!",
        "TUTORIAL_DAVE_04" to "Для посадки растений нужно солнце! Лови падающие солнышки, пока они не укатились!",
        "TUTORIAL_DAVE_05" to "Отлично! А теперь посади Подсолнух — он будет сам производить для нас дополнительное солнце!",
        "TUTORIAL_DAVE_06" to "Ух ты! Этот зомби светится зелёным! Внутри него точно спрятано супер-питательное Удобрение (Plant Food)!",
        "TUTORIAL_DAVE_07" to "Перетащи листик Удобрения на Горохострел и посмотри, какую мощь он выдаст!",
        "TUTORIAL_DAVE_08" to "Фух, отбились! Ой, смотри, что я нашёл! Это же бутылочка острого соуса для моего ТАКО!",
        "TUTORIAL_DAVE_09" to "Это был самый вкусный тако в моей жизни! Жаль, что я его уже съел...",
        "TUTORIAL_DAVE_10" to "Идея! Давай воспользуемся моей машиной времени Пенни, чтобы вернуться на минуту назад, и я смогу съесть этот тако ЕЩЁ РАЗ!",
        "TUTORIAL_PENNY_01" to "Приветствую, пользователь Дэйв. Я Пенни — ваш бортовой транспорт для путешествий во времени.",
        "TUTORIAL_PENNY_02" to "Внимание: обнаружена временная аномалия. Кажется, мы переместились на 4500 лет назад... в Древний Египет!",
        "TUTORIAL_DAVE_11" to "Древний Египет?! Мумии?! Ладно, разберёмся с мумиями, но от своего повторного тако я не откажусь!",
        "DAVE_CANNONMINIGAME_12_KILLED_1" to "М-М-М-МОНСТР ТАКО!",
        "DAVE_CANNONMINIGAME_12_KILLED_2" to "АРГХЛ-БАРГЛ!",
        "DAVE_CANNONMINIGAME_12_KILLED_3" to "ПРОЖАРЕНО!",
        "DAVE_CANNONMINIGAME_3_KILLED_1" to "Круто!",
        "DAVE_CANNONMINIGAME_3_KILLED_2" to "Неплохо!",
        "DAVE_CANNONMINIGAME_3_KILLED_3" to "Так держать!",
        "DAVE_CANNONMINIGAME_5_KILLED_1" to "Твидли-динкис!",
        "DAVE_CANNONMINIGAME_5_KILLED_2" to "Славно!",
        "DAVE_CANNONMINIGAME_5_KILLED_3" to "Безумие!",
        "DAVE_CANNONMINIGAME_8_KILLED_1" to "ПОТРЯСАЮЩЕ!",
        "DAVE_CANNONMINIGAME_8_KILLED_2" to "Я БЕЗ УМА ОТ КОМБО!",
        "DAVE_CANNONMINIGAME_8_KILLED_3" to "УУУУУУУУУУУУ!",

        // ==================== СЮЖЕТНЫЕ ДИАЛОГИ МИРОВ (ЕГИПЕТ, ПИРАТЫ, ДИКИЙ ЗАПАД, БУДУЩЕЕ И Т.Д.) ====================
        "EGYPT_INTRO_DAVE_1" to "Пенни, почему кругом столько песка и пирамид? Где моя кухня и моё тако?!",
        "EGYPT_INTRO_PENNY_1" to "Мои сенсоры показывают, что мы в Древнем Египте, пользователь Дэйв. И к нам приближаются мумифицированные зомби.",
        "EGYPT_INTRO_DAVE_2" to "Зомби с факелами и саркофагами?! Сосед, выкатывай Капустопульты и Айсберг-салат!",
        "EGYPT_ZOMBOSS_INTRO_1" to "Жалкие путешественники во времени! Вы явились прямо в лапы моего Сфинкс-инатора!",
        "EGYPT_ZOMBOSS_INTRO_2" to "Я — Доктор Эдгар Зомбосс! И вся история человечества (включая ваши мозги) скоро станет моей!",
        "EGYPT_ZOMBOSS_DEFEAT_1" to "Невозможно! Мой прекрасный Сфинкс-инатор рассыпался в песок! Но мы ещё встретимся в других эпохах!",

        "PIRATE_INTRO_DAVE_1" to "Йо-хо-хо и тарелка начос! Пенни, мы что, на пиратском корабле посреди океана?!",
        "PIRATE_INTRO_PENNY_1" to "Точно так, пользователь Дэйв. Это 1679 год, Пиратские моря. Остерегайтесь пролетов между палубами и зомби на канатах!",
        "PIRATE_ZOMBOSS_INTRO_1" to "На абордаж! Мой Зомбот Доскоход пустит ваш жалкий огород на корм рыбам!",
        "PIRATE_ZOMBOSS_DEFEAT_1" to "Проклятье! Мои пушки промокли, а корабль дал течь! Отступаем во времени!",

        "COWBOY_INTRO_DAVE_1" to "И-хааа! Дикий Запад! Смотри, Пенни, тут на газоне проложены рельсы с вагонетками!",
        "COWBOY_INTRO_PENNY_1" to "Вы можете перемещать вагонетки вверх и вниз прямо во время боя, чтобы один стрелок защищал сразу несколько линий.",
        "COWBOY_ZOMBOSS_INTRO_1" to "В этом городке слишком тесно для нас двоих, Дэйв! Мой Боевой Фургон сравняет вас с землёй!",
        "COWBOY_ZOMBOSS_DEFEAT_1" to "Мои механические подковы погнулись! Но будущее всё равно за зомби!",

        "FUTURE_INTRO_DAVE_1" to "Ого! Летающие машины, лазеры и силовые клетки! В далёком будущем тако наверняка делают из чистого хрома!",
        "FUTURE_INTRO_PENNY_1" to "Силовые клетки (Power Tiles) передают заряд Удобрения всем растениям, стоящим на клетках того же цвета.",
        "FUTURE_ZOMBOSS_INTRO_1" to "Добро пожаловать в завтрашний день, где победили зомби! Мой Завтра-трон сотрёт вас в порошок!",
        "FUTURE_ZOMBOSS_DEFEAT_1" to "Системная ошибка! Перезагрузка реактора времени!",

        "DARK_INTRO_DAVE_1" to "Темнотища-то какая! Ни одного солнышка с неба не падает! Хорошо, что в Тёмных веках растут светящиеся грибы!",
        "DARK_INTRO_PENNY_1" to "Остерегайтесь могил с зелёными символами и Зомби-Королей, превращающих обычных крестьян в рыцарей.",
        "DARK_ZOMBOSS_INTRO_1" to "Узрите моего Тёмного Дракона! Его пламя испепелит ваши грибочки!",

        "BEACH_INTRO_DAVE_1" to "Пляж большой волны! Крем от загара, прибой и... зомби в ластах и с осьминогами?!",
        "BEACH_INTRO_PENNY_1" to "Следите за линией прилива! Чтобы сажать обычные растения на воде, сначала разместите Кувшинку.",

        "ICEAGE_INTRO_DAVE_1" to "Бррр! Моя кастрюля на голове примерзла к ушам! Эти ледяные ветра замораживают наши растения прямо в глыбы льда!",
        "ICEAGE_INTRO_PENNY_1" to "Используйте огненные растения вроде Горячей картошки, Перцепульты и Огненного горохострела, чтобы обогревать соседей.",

        "LOSTCITY_INTRO_DAVE_1" to "Затерянный город золота! Если посадить растение на Золотую клетку, она сразу выдаст порцию солнца!",
        "EIGHTIES_INTRO_DAVE_1" to "Неоновый микстейп-тур! Когда на сцене меняется музыка — панк, поп, рэп или метал — зомби начинают вести себя совершенно по-разному!",
        "DINO_INTRO_DAVE_1" to "Юрское болото! Динозавры вроде милые, пока не начинают пинать и переносить зомби к нашему дому! Скорее неси Парфюм-гриб!",
        "MODERN_INTRO_DAVE_1" to "Мы вернулись в Наше время! Но из временных порталов прямо на наш родной газон лезут зомби из всех эпох сразу!",

        // ==================== СОВЕТЫ И ОБУЧЕНИЕ В БОЮ ====================
        "ADVICE_ALMANAC_FROM_PLANTS" to "Нажимайте на растения на карте, чтобы открыть их страницу в Альманахе",
        "ADVICE_BANANA_TUTORIAL_COOLDOWN" to "Бананомёту нужно время на перезарядку",
        "ADVICE_BANANA_TUTORIAL_GOOD_JOB" to "Отлично! Вы также можете перетаскивать прицел на нужную клетку",
        "ADVICE_BANANA_TUTORIAL_SELECT" to "Нажмите на Бананомёт, чтобы выбрать его",
        "ADVICE_BANANA_TUTORIAL_SHOOT" to "Нажмите на клетку газона, чтобы нанести удар",
        "ADVICE_BOUGHT_PLANTFOOD" to "Если битва станет жаркой, запасайтесь Удобрением!",
        "ADVICE_BUY_PLANTFOOD" to "Потратьте монеты на дополнительное Удобрение!",
        "ADVICE_CANNOT_PLANT_ON_RAILCART" to "{PLANT} нельзя посадить в вагонетку.",
        "ADVICE_CANT_PLANT_THERE" to "Здесь нельзя сажать",
        "ADVICE_CHERRY_BOMB_POWERUP" to "Посадите Вишнёвую бомбу, чтобы взорвать зомби!",
        "ADVICE_CLICK_COIN" to "Нажмите на монету, чтобы подобрать её!",
        "ADVICE_CLICK_KEY" to "Нажмите на ключ, чтобы забрать его!",
        "ADVICE_CLICK_ON_GRASS" to "Перетащите пакет с семенами на траву!",
        "ADVICE_CLICK_PEASHOOTER" to "Перетащите пакет Горохострела,\r\nчтобы посадить ещё один!",
        "ADVICE_CLICK_PLANT" to "Перетащите лопату на растение, чтобы выкопать его!",
        "ADVICE_CLICK_SEED_PACKET" to "Перетащите пакет с семенами на газон!",
        "ADVICE_CLICK_SHOVEL" to "Эти растения смотрят не в ту сторону!\r\nПеретащите лопату на растение, чтобы убрать его!",
        "ADVICE_CLICK_SPROUT" to "Вы нашли росток для Сада Дзен!",
        "ADVICE_COLLECT_COINS" to "Собирайте монеты, чтобы покупать крутые вещи!",
        "ADVICE_COLLECT_KEYS" to "Интересно, что открывает этот ключ?",
        "ADVICE_COLLECT_PLANT" to "Нажмите на растение, чтобы добавить\r\nего в свою коллекцию!",
        "ADVICE_COLLECT_SPROUT" to "Он будет ждать вас в вашем Саду Дзен.",
        "ADVICE_COLLECT_SUN" to "Нажимайте на падающее солнце, чтобы собрать его!",
        "ADVICE_COLLECTED_SUN" to "Продолжайте собирать солнце! Оно\r\nнужно для посадки новых растений!",
        "ADVICE_DEFEND_BEFORE_MINES_GONE" to "Картофельные мины мощно взрываются, но им\r\nнужно время на взведение. Испытайте их!",
        "ADVICE_DONE_DIGGING" to "Отличная работа!\r\nПриготовьтесь защищать свой дом!",
        "ADVICE_ENOUGH_SUN" to "Превосходно! Вы собрали достаточно\r\nсолнца для следующего растения!",
        "ADVICE_GOTO_ALMANAC" to "Загляните в Альманах!",
        "ADVICE_GOTO_QUESTS" to "Загляните в Журнал путешествий!",
        "ADVICE_GOTO_STORE" to "Посетите магазин!",
        "ADVICE_GOTO_ZEN_GARDEN" to "Посетите свой Сад Дзен!",
        "ADVICE_GRAVEBUSTERS_ON_GRAVES" to "Могилоед можно сажать только на могилы",
        "ADVICE_HOTPOTATO_ONLY_ON_ICEBLOCKS" to "Горячую картошку можно сажать только на ледяные глыбы.",
        "ADVICE_HUGE_WAVE" to "ПРИБЛИЖАЕТСЯ ОГРОМНАЯ ВОЛНА ЗОМБИ!",
        "ADVICE_INTENSIVECARROT_NOT_ON_PREVIOUSLY_PLANTED_SQUARE" to "Её можно сажать только на месте съеденных растений.",
        "ADVICE_KEEP_DIGGING" to "Продолжайте выкапывать лопатой, чтобы освободить\r\nместо для Стены-ореха!",
        "ADVICE_MORE_SUNFLOWERS" to "Больше Подсолнухов — больше солнца.\r\nБольше солнца — БОЛЬШЕ РАСТЕНИЙ!",
        "ADVICE_NEED_LILYPAD_FIRST" to "Сначала посадите Кувшинку",
        "ADVICE_NEW_LEVEL" to "Открыт следующий уровень!",
        "ADVICE_NEW_PLANT" to "Испытайте своё новое растение!",
        "ADVICE_NOT_ENOUGH_SUN" to "Вам нужно собрать больше солнца для этой посадки!",
        "ADVICE_NOT_ON_CRATERS" to "Нельзя сажать на кратерах.",
        "ADVICE_NOT_ON_ICEBLOCKS" to "Нельзя сажать на ледяных блоках или статуях.",
        "ADVICE_PEASHOOTER_DIED" to "Один из ваших Горохострелов погиб!\r\nСтарайтесь сажать стрелков левее, подальше от зомби!",
        "ADVICE_PLANT_FOOD_BANK_FULL" to "Запас Удобрения полон! Потратьте одно перед сбором нового.",
        "ADVICE_PLANT_SUNFLOWER1" to "Подсолнухи — важнейшие растения.\r\nПосадите один!",
        "ADVICE_PLANT_SUNFLOWER2" to "Подсолнухи дают солнце!",
        "ADVICE_PLANT_SUNFLOWER3" to "Посадка хотя бы 3 Подсолнухов значительно повышает\r\nваши шансы пережить атаку зомби!",
        "ADVICE_PLANT_SUNFLOWER4" to "Посадите хотя бы по 1 Подсолнуху на каждой линии!",
        "ADVICE_PLANTED_PEASHOOTER" to "Отлично! Сажать можно также нажатием\r\nсначала на пакет семян, а затем на клетку газона",
        "ADVICE_SEED_REFRESH" to "Семенам нужно время на перезарядку\r\nперед следующей посадкой",
        "ADVICE_TURBO_BUTTON_OFF" to "Нажмите на активную кнопку Турбо, чтобы выключить ускорение",
        "ADVICE_TURBO_BUTTON_ON" to "Нажмите на кнопку Турбо, чтобы ускорить игру",
        "ADVICE_ZOMBIE_ONSLAUGHT" to "Не дайте зомби добраться до вашего дома!",

        // ==================== НАГРАДЫ ПОСЛЕ УРОВНЕЙ И ДОСТИЖЕНИЯ ====================
        "DEFAULT_LEVEL_COMPLETE_HEADER" to "Уровень пройден!",
        "DEFAULT_LEVEL_COMPLETE_BODY" to "Вы победили зомби!",
        "AWARD_SCREEN_NEW_PLANT" to "Вы получили новое РАСТЕНИЕ!",
        "AWARD_SCREEN_NEW_UPGRADE" to "Вы получили новое УЛУЧШЕНИЕ!",
        "AWARD_SCREEN_MONEYBAG_TITLE" to "Мешочек с монетами!",
        "AWARD_SCREEN_MONEYBAG_DESCRIPTION_01" to "Как ваша голова полна мозгов, так этот мешочек полон монет!",
        "AWARD_SCREEN_MONEYBAG_DESCRIPTION_02" to "Монеты, монеты, монеты! Хватит и на Усиления, и на Удобрение!",
        "AWARD_SCREEN_WORLDKEY_TITLE" to "Ключи Миров",
        "AWARD_SCREEN_WORLDKEY_DESCRIPTION" to "Используйте их для открытия новых Миров и путешествий в другие эпохи!",
        "AWARD_SCREEN_WORLDTROPHY" to "Вы получили ТРОФЕЙ!",
        "AWARD_SCREEN_FINALDAY_COMPLETE" to "Финальный день пройден!",
        "AWARD_SCREEN_LAWN_MOWER_TRIGGERED" to "БЫЛА ЗАДЕЙСТВОВАНА ГАЗОНОКОСИЛКА!",
        "AWARD_SCREEN_GOAL_FAILED" to "^ff0000 Цель провалена",

        // ==================== МАГАЗИН, КАЛЕНДАРЬ И АЛЬМАНАХ ====================
        "BUY_NOW" to "Купить",
        "BUY_MORE" to "КУПИТЬ ЕЩЁ",
        "DRAPER_BUY_NOW" to "КУПИТЬ",
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

        // ==================== МИРЫ И БЕСКОНЕЧНЫЕ ЗОНЫ ====================
        "ALM_EGYPT" to "Древний Египет",
        "EGYPT" to "Древний Египет",
        "EGYPT_LEVEL_NAME" to "Древний Египет - День {LEVEL_NUMBER}",
        "EGYPT_DANGER_ROOM_NAME" to "Пирамида Рока",
        "ALM_PIRATE" to "Пиратские моря",
        "PIRATE_DANGER_ROOM_NAME" to "Добыча мертвеца",
        "ALM_COWBOY" to "Дикий Запад",
        "COWBOY" to "Дикий Запад",
        "COWBOY_DANGER_ROOM_NAME" to "Большой Грозный Холм",
        "ALM_FUTURE" to "Далёкое будущее",
        "FUTURE" to "Далёкое будущее",
        "FUTURE_LEVEL_NAME" to "Далёкое будущее - День {LEVEL_NUMBER}",
        "FUTURE_DANGER_ROOM_NAME" to "Ужас из Завтрашнего Дня",
        "ALM_DARK" to "Тёмные века",
        "DARKAGES_LEVEL_NAME" to "Тёмные века - Ночь {LEVEL_NUMBER}",
        "DARK_DANGER_ROOM_NAME" to "Испытание короля Артура",
        "ALM_BEACH" to "Пляж большой волны",
        "BEACH_LEVEL_NAME" to "Пляж большой волны - День {LEVEL_NUMBER}",
        "BEACH_DANGER_ROOM_NAME" to "Тики-факельщик",
        "ALM_ICEAGE" to "Обмороженные пещеры",
        "ICEAGE" to "Обмороженные пещеры",
        "ICEAGE_LEVEL_NAME" to "Обмороженные пещеры - День {LEVEL_NUMBER}",
        "ICEAGE_DANGER_ROOM_NAME" to "Скованное льдом поле боя",
        "ALM_LOSTCITY" to "Затерянный город",
        "LOSTCITY_LEVEL_NAME" to "Затерянный город - День {LEVEL_NUMBER}",
        "LOSTCITY_DANGER_ROOM_NAME" to "Храм Цветения",
        "ALM_EIGHTIES" to "Неоновый микстейп-тур",
        "EIGHTIES_LEVEL_NAME" to "Неоновый тур - День {LEVEL_NUMBER}",
        "EIGHTIES_DANGER_ROOM_NAME" to "Лучшие хиты",
        "ALM_DINO" to "Юрское болото",
        "DINO_LEVEL_NAME" to "Юрское болото - День {LEVEL_NUMBER}",
        "DINO_DANGER_ROOM_NAME" to "Смоляные ямы Ла-Мозга",
        "ALM_MODERN" to "Наше время",

        // ==================== ПОЛНЫЕ ОПИСАНИЯ, ИСТОРИИ И ЭФФЕКТЫ НОВЕЙШИХ И КЛАССИЧЕСКИХ РАСТЕНИЙ ====================
        "BANEBERRY" to "Воронец (Baneberry)",
        "BANEBERRY_TOOLTIP" to "Проклинает зомби, увеличивая получаемый ими урон. Сажайте друг на друга для усиления.",
        "BANEBERRY_DESCRIPTION_HEADER" to "Стреляет жуткими снарядами, проклинающими зомби так, что они получают бонусный урон от всех источников. Сажайте Воронцы друг на друга, чтобы умножить их силу.",
        "BANEBERRY_DESCRIPTION_HINT" to "Складывайте несколько Воронцов на одной клетке, чтобы усилить его атаку и проклятие, заставляя зомби получать огромный урон от вашей обороны.",
        "BANEBERRY_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Зловещее проклятие: {STAT}Поражённые зомби получают значительно больше урона от всех растений в течение короткого времени.",
        "BANEBERRY_DESCRIPTION" to "{KEYWORD}Взгляд ужаса: {STAT}Выпускает жуткий снаряд в первого зомби на линии, нанося урон и накладывая временное проклятие.\n{KEYWORD}Ослабление зомби: {STAT}Проклятые зомби получают дополнительный урон от атак всех растений.\n{KEYWORD}Складывание: {STAT}Воронец можно сажать поверх другого Воронца. Каждый ярус увеличивает урон, запас здоровья и силу проклятия.\n{KEYWORD}Особенность: {STAT}Воронец можно складывать даже внутри Лозы.\n{FLAVOR}«Я вижу всё!» — шепчет Воронец из тёмного угла газона. Подсолнух считает, что он умеет читать мысли. Стена-орех постоянно оглядывается через плечо. Воронцу нравится наводить таинственность. Только не напоминайте ему, как однажды он сам испугался своего отражения в зеркале.",

        "BELFRIGHT" to "Колокол Ужаса (Belfright)",
        "BELFRIGHT_TOOLTIP" to "Наносит урон и накладывает немоту на зомби рядом, отключая их особые способности.",
        "BELFRIGHT_DESCRIPTION_HEADER" to "Звонит в зловещие колокола, нанося урон и лишая особых зомби их способностей. Теневая аура усиливает её атаки.",
        "BELFRIGHT_DESCRIPTION_HINT" to "Размещайте Колокол Ужаса там, где её звуковые волны накроют группы особых зомби и отключат их умения. Усильте её Теневой аурой для пробития брони.",
        "BELFRIGHT_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Зловещее эхо: {STAT}Испускает мощную звуковую волну вокруг себя, нанося урон, замедляя зомби и блокируя их способности.\n{KEYWORD}Теневая аура: {STAT}Под действием Теневой ауры волна наносит повышенный урон и пробивает броню насквозь.",
        "BELFRIGHT_DESCRIPTION" to "{KEYWORD}По ком звонит колокол: {STAT}Звонит в колокола, выпуская тёмные звуковые волны вокруг себя и поражая ближайших зомби.\n{KEYWORD}Немота: {STAT}Особые зомби, задетые волной, получают немоту и не могут применять свои способности.\n{KEYWORD}Теневая аура: {STAT}Под действием Теневой ауры наносит больше урона, а волны игнорируют броню зомби.\n{FLAVOR}Колокол Ужаса спокойна, мудра и невероятно пунктуальна. Её колокола бьют ровно каждый час, идёт бой или нет. Растениям её перезвон кажется успокаивающим. А вот Безумному Дэйву — нет. «КТО ТРЕЗВОНИТ В 3 ЧАСА НОЧИ?!» — кричит он. Колокол Ужаса знает кто. Но никому не скажет.",

        "CACTALOG" to "Кактусовое Бревно (Cactalog)",
        "CACTALOG_TOOLTIP" to "Катится сквозь зомби, отбрасывая их и оставляя дорожку шипов.",
        "CACTALOG_DESCRIPTION_HEADER" to "Катится по газону сквозь зомби, нанося урон, отталкивая их назад и оставляя за собой колючий след.",
        "CACTALOG_DESCRIPTION_HINT" to "Кактусовое Бревно лучше всего работает против больших толп зомби, отбрасывая орду и устилая линию шипами.",
        "CACTALOG_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Не имеет эффекта от Удобрения.",
        "CACTALOG_DESCRIPTION" to "{KEYWORD}Катящийся таран: {STAT}Катится по линии, нанося урон всем зомби на пути с шансом отбросить их назад.\n{KEYWORD}Неудержимость: {STAT}Когда Кактусовое Бревно начинает катиться, ничто не остановит его до самого края газона.\n{KEYWORD}Тропа шипов: {STAT}Оставляет за собой дорожку колючек, ранящую шагающих по ней зомби.\n{FLAVOR}«Они смеялись, когда я сказал, что заново изобрету колесо. Больше они не смеются. В основном потому, что я по ним проехался».",

        "PYROAK" to "Огненный Дуб (Scorchoak)",
        "THORNWHIP" to "Шипастый Кнут (Thorn Whip)",
        "VOLTSNAPDRAGON" to "Вольт-Дракон (Volt Snapdragon)",
        "SAKURA" to "Сакура (Sakura)",

        "BLAZINGKNIGHT" to "Пылающий Рыцарь (Blazing Knight)",
        "BLAZINGKNIGHT_TOOLTIP" to "Неуязвим в режиме Огненной стены; при гибели призывает огненного коня.",
        "BLAZINGKNIGHT_DESCRIPTION_HEADER" to "Создаёт неуязвимую Огненную стену, сдерживающую зомби, и выпускает пылающего скакуна при поражении.",
        "BLAZINGKNIGHT_DESCRIPTION_HINT" to "Ставьте Пылающего Рыцаря на самых загруженных линиях, чтобы максимизировать урон от Огненной стены и рывка коня.",
        "BLAZINGKNIGHT_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Выпускает огненные сферы, выжигающие всех зомби на линии справа от него.",
        "BLAZINGKNIGHT_DESCRIPTION" to "{KEYWORD}Огненная стена: {STAT}При активации Пылающий Рыцарь создаёт горящий барьер, наносящий периодический урон зомби.\n{KEYWORD}Неуязвимость: {STAT}Находясь в состоянии Огненной стены, Рыцарь полностью неуязвим к любому урону.\n{KEYWORD}Последний рывок: {STAT}При гибели призывает огненного коня, который мчится по газону, сжигая зомби на пути.\n{FLAVOR}Пылающий Рыцарь стоит непоколебимо в самом пекле битвы. Когда загорается его барьер, ни один зомби не способен прорваться. А когда Рыцарь наконец падает, поле боя содрогается от топота огненного скакуна, оставляющего от сильнейших врагов лишь пепел.",

        "BRAINSTEM" to "Мозговой Стебель (Brain Stem)",
        "BRAINSTEM_TOOLTIP" to "Притягивает зомби на линии назад от дома и уменьшает их.",
        "BRAINSTEM_DESCRIPTION_HEADER" to "Манит зомби к себе — даже тех, кто уже прошёл мимо — оттягивая их от дома и уменьшая в размерах.",
        "BRAINSTEM_DESCRIPTION_HINT" to "Посадите несколько Мозговых Стеблей на одной линии, чтобы по цепочке оттягивать зомби назад от дома.",
        "BRAINSTEM_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Уменьшает и мгновенно уничтожает нескольких зомби по всему газону.",
        "BRAINSTEM_DESCRIPTION" to "{KEYWORD}Приманка: {STAT}Привлекает зомби на своей линии, заставляя их идти к Мозговому Стеблю.\n{KEYWORD}Разворот: {STAT}Зомби, которые уже прошли мимо Мозгового Стебля, разворачиваются и идут обратно от дома к нему.\n{KEYWORD}Уменьшение: {STAT}Уменьшает поражённых зомби, снижая их запас здоровья и урон.\n{FLAVOR}Мозговой Стебель не паникует, когда зомби прорывают передний край обороны. Он просто ждёт. И вдруг зомби останавливаются, словно вспомнив о чём-то важном, разворачиваются и послушно бредут обратно — прямо к Мозговому Стеблю, превращаясь в совсем маленькую проблему.",

        "LEMONAID" to "Лимонад (Lemonaid)",
        "LEMONAID_TOOLTIP" to "Даёт солнце, лечит, воскрешает и усиливает союзников на линии.",
        "LEMONAID_DESCRIPTION_HEADER" to "Производит солнце и поддерживает растения на своей линии: лечит, возрождает и иногда активирует их эффект Удобрения.",
        "LEMONAID_DESCRIPTION_HINT" to "Лимонад лечит другие растения в своём ряду и имеет шанс бесплатно активировать на них Удобрение.",
        "LEMONAID_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Лечит все растения на своей линии, воскрешает погибшие и с шансом даёт союзнику эффект Удобрения.",
        "LEMONAID_DESCRIPTION" to "{KEYWORD}Генерация солнца: {STAT}Регулярно производит солнце.\n{KEYWORD}Лечение и воскрешение: {STAT}Восстанавливает здоровье раненому растению и может возродить недавно погибшее растение на своей линии.\n{KEYWORD}Усиление: {STAT}Имеет шанс активировать эффект Удобрения на исцелённом союзнике.\n{FLAVOR}Когда оборона трещит по швам, на помощь приходит освежающий Лимонад! Капля исцеления здесь, возрождение там, а в самый критический момент — внезапный всплеск суперсилы для союзника.",

        "DEVOURBLOOM" to "Пожирающий Цветок (Devour Bloom)",
        "DEVOURBLOOM_TOOLTIP" to "Мгновенно проглатывает большинство зомби, наступивших на него.",
        "DEVOURBLOOM_DESCRIPTION_HEADER" to "Проглатывает зомби, которые идут или летят над ним. Может съесть сразу нескольких зомби, мгновенно уничтожая их, а гигантам наносит огромный урон.",
        "DEVOURBLOOM_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Выпускает клонов под первого зомби на линии, пожирая всех врагов в большой области.",

        "CORNFETTI" to "Попкорн-хлопушка (Corn-fetti Popper)",
        "CORNFETTI_TOOLTIP" to "Пробивает зомби насквозь и отбрасывает подошедших слишком близко.",
        "CORNFETTI_DESCRIPTION_HEADER" to "Стреляет пробивающими початками и взрывным конфетти, отбрасывающим подошедших вплотную зомби.",
        "CORNFETTI_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Даёт мощный залп кукурузного конфетти по 3 линиям, нанося урон и отбрасывая всех зомби назад.",

        "HAMMERUIT" to "Молот-Фрукт (Hammeruit)",
        "HAMMERUIT_TOOLTIP" to "Сокрушает зомби в области заряженными ударами молота.",
        "HAMMERUIT_DESCRIPTION_HEADER" to "Молот-Фрукт расплющивает зомби мощными заряженными ударами. Чем дольше он копит заряд, тем сильнее удар!",
        "HAMMERUIT_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Трижды бьёт молотом по земле с нарастающим уроном и выпускает ударные волны в 3 направлениях.",

        "FROSTBONNET" to "Ледяной Капор (Frost Bonnet)",
        "FROSTBONNET_TOOLTIP" to "Переключается между режимами Огня и Льда по нажатию.",
        "FROSTBONNET_DESCRIPTION_HEADER" to "Может переключаться между Огненным и Ледяным цветением по нажатию, поджигая или замораживая зомби.",
        "FROSTBONNET_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Обрушивает комбинированный шквал огня и льда на ближайших зомби, превращая побеждённых врагов в ледяные стены.",

        "BLAZELEAF" to "Пылающий Лист (Blaze Leaf)",
        "BLAZELEAF_TOOLTIP" to "Бьёт по площади и выпускает огненную волну каждый 3-й удар.",
        "BLAZELEAF_DESCRIPTION_HEADER" to "Атакует зомби в большом радиусе и каждый третий удар выпускает Огненную волну, поражающую несколько линий.",
        "BLAZELEAF_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Выпускает гигантский огненный вихрь, наносящий урон всем зомби на газоне.",

        "DOOMSHROOM" to "Гриб Судьбы (Doom-shroom)",
        "DOOMSHROOM_TOOLTIP" to "Растёт и мощно взрывается, порождая новые грибы.",
        "DOOMSHROOM_DESCRIPTION_HEADER" to "Гриб Судьбы взрывается при уничтожении. По мере роста радиус его взрыва увеличивается, и он может порождать новые Грибы Судьбы.",
        "DOOMSHROOM_DESCRIPTION_PLANTFOOD" to "{KEYWORD}Удобрение: {STAT}Все Грибы Судьбы на газоне вырастают на стадию, взрываются и оставляют после себя новые грибы.",

        "BLASTSPINNER" to "Взрывной Прядильщик (Blast Spinner)",
        "BLASTSPINNER_TOOLTIP" to "Обездвиживает зомби паутиной и превращает их во взрывные коконы.",
        "BLASTSPINNER_DESCRIPTION_HEADER" to "Бросает липкую паутину на соседние линии, превращая зомби в коконы, которые через несколько секунд взрываются и наносят урон всем вокруг.",

        "AQUAVINE" to "Водная Лоза (Aqua Vine)",
        "AQUAVINE_TOOLTIP" to "Лоза-амфибия для воды и суши с мощной водной струёй.",
        "AQUAVINE_DESCRIPTION_HEADER" to "Растение-амфибия: позволяет сажать наземные растения на воду, а водные — на сушу, смывая зомби мощным напором воды.",

        "GUARDSHROOM" to "Гриб-Страж (Guard-shroom)",
        "GUARDSHROOM_TOOLTIP" to "Создаёт мини-грибы, защищающие и усиливающие соседние растения.",
        "GUARDSHROOM_DESCRIPTION_HEADER" to "Гриб-амфибия, создающий вокруг себя мини-копии, которые проглатывают зомби и значительно усиливают грибы и другие растения на своих клетках.",

        "ELECTRICPEEL" to "Электро-кожура (Electric Peel)",
        "ELECTRICPEEL_TOOLTIP" to "Выпускает кожуру, скользящую по линии туда и обратно.",
        "ELECTRICPEEL_DESCRIPTION_HEADER" to "Выпускает наэлектризованную банановую кожуру, которая скользит до конца линии и возвращается обратно, дважды поражая всех зомби.",

        "DRAGONBRUIT" to "Драконий Фрукт (Dragon Bruit)",
        "DRAGONBRUIT_TOOLTIP" to "Бросает по 3 снаряда на 3 линии и делится на 3 мини-дракона.",
        "DRAGONBRUIT_DESCRIPTION_HEADER" to "Бросает 3 взрывных снаряда по 3 смежным линиям. При уничтожении распадается на 3 маленьких дракончика.",

        "ICESHROOM" to "Ледяной Гриб (Ice-shroom)",
        "ICESHROOM_TOOLTIP" to "Сокрушает врагов вблизи и усиливает ледяные растения вокруг.",
        "ICESHROOM_DESCRIPTION_HEADER" to "Мощно бьёт зомби в ближнем бою и излучает ледяную ауру, которая замедляет врагов и усиливает все растения семейства Зимней мяты вокруг.",

        // ==================== КЛАССИЧЕСКИЕ РАСТЕНИЯ И МЯТЫ (ИМЕНА И ОПИСАНИЯ) ====================
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
        "NIGHTSHADE" to "Паслён",
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

    fun installRussianLocalization(
        cdnDir: String,
        translatePlantNames: Boolean = true,
        log: (String) -> Unit
    ): Boolean {
        val allDirs = Pvz2Manager.listAllCdnDirs().ifEmpty { listOf(cdnDir) }.toMutableSet().apply { add(cdnDir) }
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

            var i = 0
            while (i + 1 < locArr.length()) {
                val rawKey = locArr.optString(i, "")
                val cleanKey = rawKey.trim().removeSurrounding("\"").removeSuffix(",")
                val origVal = locArr.optString(i + 1, "")

                val chosenTranslation = externalDict[cleanKey] ?: BUILTIN_RU_DICT[cleanKey]
                if (chosenTranslation != null) {
                    val isPlantShortName = Pvz2Database.ALL_PLANT_IDS.any { it.equals(cleanKey, ignoreCase = true) }
                    if (!isPlantShortName || translatePlantNames) {
                        locArr.put(i + 1, chosenTranslation)
                        replacedStrings++
                    }
                } else {
                    val smartTranslated = smartTranslateDialogsAndDescriptions(cleanKey, origVal)
                    if (smartTranslated != null && smartTranslated != origVal) {
                        locArr.put(i + 1, smartTranslated)
                        replacedStrings++
                    }
                }
                i += 2
            }

            if (Pvz2Manager.writeJsonAsRton(targetPath, json, log)) {
                patchedCount++
                log("🇷🇺 В ${dir.substringAfterLast("/")} переведено строк и диалогов: $replacedStrings (из $totalPairs)")
            }
        }

        return if (patchedCount > 0) {
            log("🎉 Полный русификатор (интерфейс, описания и диалоги) успешно установлен!")
            true
        } else {
            log("❌ Файл LawnStrings-en-us.rton не найден в папке $cdnDir!")
            false
        }
    }

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

    // Умный контекстный переводчик всех остальных диалогов Дэйва/Пенни/Зомбосса, квестов, условий уровней и описаний Альманаха
    private fun smartTranslateDialogsAndDescriptions(key: String, text: String): String? {
        if (text.isBlank() || text.startsWith("http") || text.startsWith("POPANIM_")) return null

        var res = text

        // 1. Перевод тегов Альманаха и характеристик растений
        res = res
            .replace("{KEYWORD}Plant Food: {STAT}No Plant Food effect.", "{KEYWORD}Удобрение: {STAT}Не имеет эффекта от Удобрения.")
            .replace("{KEYWORD}Plant Food: {STAT}", "{KEYWORD}Удобрение: {STAT}")
            .replace("{KEYWORD}Special: {STAT}", "{KEYWORD}Особенность: {STAT}")
            .replace("{KEYWORD}Usage: {STAT}", "{KEYWORD}Применение: {STAT}")
            .replace("{KEYWORD}Weakness: {STAT}", "{KEYWORD}Слабость: {STAT}")
            .replace("{KEYWORD}Sun Production: {STAT}", "{KEYWORD}Производство солнца: {STAT}")
            .replace("{KEYWORD}Shadow Aura: {STAT}", "{KEYWORD}Теневая аура: {STAT}")

        // 2. Перевод сюжетных обращений и фраз в диалогах Дэйва, Пенни и Доктора Зомбосса
        val isDialogOrStory = key.contains("DAVE") || key.contains("PENNY") || key.contains("ZOMBOSS") ||
                key.contains("DIALOG") || key.contains("NARRATIVE") || key.contains("STORY") ||
                key.contains("INTRO") || key.contains("OUTRO") || key.contains("TUTORIAL") ||
                key.endsWith("_DESCRIPTION") || key.endsWith("_HEADER") || key.endsWith("_HINT") ||
                key.endsWith("_TOOLTIP") || key.endsWith("_DESC") || key.endsWith("_TITLE")

        if (isDialogOrStory || res != text) {
            res = res
                .replace("Crazy Dave", "Безумный Дэйв")
                .replace("User Dave", "Пользователь Дэйв")
                .replace("Dr. Zomboss", "Доктор Зомбосс")
                .replace("Penny", "Пенни")
                .replace("Neighbor", "Сосед")
                .replace("Plant Food", "Удобрение")
                .replace("Seed Packets", "пакеты семян")
                .replace("seed packets", "пакеты семян")
                .replace("Ancient Egypt", "Древний Египет")
                .replace("Pirate Seas", "Пиратские моря")
                .replace("Wild West", "Дикий Запад")
                .replace("Far Future", "Далёкое будущее")
                .replace("Dark Ages", "Тёмные века")
                .replace("Big Wave Beach", "Пляж большой волны")
                .replace("Frostbite Caves", "Обмороженные пещеры")
                .replace("Lost City", "Затерянный город")
                .replace("Neon Mixtape Tour", "Неоновый микстейп-тур")
                .replace("Jurassic Marsh", "Юрское болото")
                .replace("Modern Day", "Наше время")
                .replace("Zen Garden", "Сад Дзен")
                .replace("Endless Zone", "Бесконечная зона")
                .replace("World Key", "Ключ Мира")
                .replace("Power Tiles", "Силовые клетки")
                .replace("Gold Tile", "Золотая клетка")
                .replace("Gargantuars", "Гаргантюа")
                .replace("Gargantuar", "Гаргантюа")
                .replace("Zombies", "Зомби")
                .replace("zombies", "зомби")
                .replace("Zombie", "Зомби")
                .replace("zombie", "зомби")
                .replace("Plants", "Растения")
                .replace("plants", "растения")
                .replace("on the lawn", "на газоне")
                .replace("down the lane", "по линии")
                .replace("in its lane", "на своей линии")
                .replace("in a 3x3 area", "в области 3x3")
                .replace("deals heavy damage", "наносит огромный урон")
                .replace("dealing heavy damage", "нанося огромный урон")
                .replace("deals massive damage", "наносит сокрушительный урон")
                .replace("deals damage", "наносит урон")
                .replace("dealing damage", "нанося урон")
                .replace("for a short time", "на короткое время")
                .replace("for a short duration", "на короткое время")
                .replace("over time", "со временем")
                .replace("temporary boost", "временное усиление")
                .replace("when planted", "при посадке")
                .replace("when eaten", "когда его съедают")
                .replace("knocks back", "отбрасывает назад")
                .replace("knocking them back", "отбрасывая их назад")
                .replace("stuns", "оглушает")
                .replace("stunning", "оглушая")
                .replace("chills", "охлаждает")
                .replace("freezes", "замораживает")
                .replace("Play with ", "Сыграйте с растением ")
                .replace("Complete ", "Пройдите ")
                .replace("Defeat ", "Победите ")
                .replace("Destroy ", "Уничтожьте ")
                .replace("Collect ", "Соберите ")
                .replace("Unlock ", "Откройте ")
                .replace("Win ", "Выиграйте ")
                .replace("Beat ", "Пройдите ")
                .replace("Survive ", "Выживите ")
                .replace("Don't lose more than ", "Не потеряйте больше ")
                .replace("Don't spend more than ", "Не тратьте больше ")
                .replace("Never have more than ", "Не сажайте больше ")
                .replace("Produce at least ", "Произведите минимум ")
                .replace("Early Access Unlock", "Ранний доступ: Разблокировка")
                .replace("Early Access Level Up", "Ранний доступ: Прокачка")
                .replace("Mega Bundle", "Мега-набор")
                .replace("Super Bundle", "Супер-набор")
                .replace("Ultimate Bundle", "Ультимативный набор")
                .replace("Starter Bundle", "Стартовый набор")
                .replace("Pinata Pack", "Набор пиньят")
                .replace("Premium Pinata", "Премиум-пиньята")
                .replace("Ad Watch Reward", "Награда за рекламу")
                .replace("Epic Quest:", "Эпический квест:")
        }

        return if (res != text) res else null
    }
}
