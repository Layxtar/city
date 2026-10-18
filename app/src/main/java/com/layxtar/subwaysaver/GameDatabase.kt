package com.cityedit.ultimate

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class CharacterCatalogEntry(
    val id: String,
    val displayName: String,
    val outfits: List<String> = listOf("default"),
    val category: String = "Surfer"
)

data class BoardCatalogEntry(
    val id: String,
    val displayName: String,
    val upgrades: List<String> = emptyList()
)

object GameContentDatabase {

    // =========================================================================
    // 1. БАЗА ДАННЫХ CLASSIC SUBWAY SURFERS (ИЗ ФАЙЛОВ ИГРЫ S125)
    // =========================================================================

    val classicCurrencies = listOf(
        Triple("1", "Монеты (Coins)", 999_999_999),
        Triple("2", "Ключи (Keys)", 999_999),
        Triple("3", "Ховерборды (Hoverboards)", 999_999),
        Triple("4", "Хедстарты (HeadStarts)", 99_999),
        Triple("5", "Бустеры счёта (ScoreBoosters)", 99_999),
        Triple("6", "Монеты Событий (EventCoins)", 999_999),
        Triple("12", "Жетоны Сезона (SeasonHuntTokens)", 99_999)
    )

    val classicCharacters: List<CharacterCatalogEntry> = listOf(
        CharacterCatalogEntry("jake", "Jake", listOf("default", "darkOutfit", "starOutfit"), "Core Crew"),
        CharacterCatalogEntry("tricky", "Tricky", listOf("default", "heartOutfit", "camoOutfit"), "Core Crew"),
        CharacterCatalogEntry("fresh", "Fresh", listOf("default", "funkOutfit", "sportOutfit"), "Core Crew"),
        CharacterCatalogEntry("spike", "Spike", listOf("default", "rockOutfit", "punkOutfit"), "Core Crew"),
        CharacterCatalogEntry("yutani", "Yutani", listOf("default", "gadgetOutfit"), "Core Crew"),
        CharacterCatalogEntry("countCorvin", "Count Corvin", listOf("default"), "Season S125"),
        CharacterCatalogEntry("xoloko", "Xoloko", listOf("default"), "Season S125"),
        CharacterCatalogEntry("whiteRabbit", "White Rabbit", listOf("default", "nightshadeOutfit"), "Season S125"),
        CharacterCatalogEntry("seanSnow", "Sean Snow", listOf("default", "frankenstyleOutfit"), "Season S125"),
        CharacterCatalogEntry("danUnder", "Dan Under", listOf("default"), "Season S125"),
        CharacterCatalogEntry("almaBrava", "Alma Brava", listOf("default"), "Season S125"),
        CharacterCatalogEntry("pumpkinpete", "Pumpkin Pete", listOf("default"), "Halloween"),
        CharacterCatalogEntry("santiGrafito", "Santi Grafito", listOf("default"), "Season S124"),
        CharacterCatalogEntry("matilda", "Matilda", listOf("default", "comicCoreOutfit"), "Season S124"),
        CharacterCatalogEntry("orionStarborn", "Orion Starborn", listOf("default"), "Season S123"),
        CharacterCatalogEntry("alpha", "Alpha", listOf("default", "omegaOutfit"), "Special"),
        CharacterCatalogEntry("bbno$", "bbno$", listOf("default"), "Collab VIP"),
        CharacterCatalogEntry("demi", "Demi Lovato", listOf("default"), "Collab VIP"),
        CharacterCatalogEntry("theBurgerKing", "The Burger King", listOf("default"), "Collab VIP"),
        CharacterCatalogEntry("outatimeJake", "Outatime Jake (BTTF)", listOf("default"), "Collab VIP"),
        CharacterCatalogEntry("amongUsRed", "Crewmate Jake (Red)", listOf("default"), "Among Us"),
        CharacterCatalogEntry("amongUsYellow", "Crewmate Tricky (Yellow)", listOf("default"), "Among Us"),
        CharacterCatalogEntry("amongUsBlack", "Crewmate Spike (Black)", listOf("default"), "Among Us"),
        CharacterCatalogEntry("amongUsCyan", "Crewmate King (Cyan)", listOf("default"), "Among Us"),
        CharacterCatalogEntry("amongUsRose", "Crewmate Tasha (Rose)", listOf("default"), "Among Us"),
        CharacterCatalogEntry("amongUsGreen", "Crewmate Guard (Green)", listOf("default"), "Among Us"),
        CharacterCatalogEntry("amongUsFrank", "Crewmate Frank", listOf("default"), "Among Us"),
        CharacterCatalogEntry("bsPointyGuy", "Brawl Stars Spike", listOf("default"), "Brawl Stars"),
        CharacterCatalogEntry("bsBeardedGuy", "Brawl Stars Dynamike", listOf("default"), "Brawl Stars"),
        CharacterCatalogEntry("bsSkullGuy", "Brawl Stars Poco", listOf("default"), "Brawl Stars"),
        CharacterCatalogEntry("bsFastGirl", "Brawl Stars Max", listOf("default"), "Brawl Stars"),
        CharacterCatalogEntry("bsElegantGirl", "Brawl Stars Piper", listOf("default"), "Brawl Stars"),
        CharacterCatalogEntry("bsToughGuy", "Brawl Stars El Primo", listOf("default"), "Brawl Stars"),
        CharacterCatalogEntry("red", "Angry Birds Red", listOf("default"), "Angry Birds"),
        CharacterCatalogEntry("chuck", "Angry Birds Chuck", listOf("default"), "Angry Birds"),
        CharacterCatalogEntry("eightBallJake", "8 Ball Jake", listOf("default"), "Collab VIP"),
        CharacterCatalogEntry("princeK", "Prince K", listOf("default", "jagOutfit", "shineOutfit"), "Core Crew"),
        CharacterCatalogEntry("brody", "Brody", listOf("default", "poshOutfit", "chillOutfit"), "Core Crew"),
        CharacterCatalogEntry("frank", "Frank", listOf("default", "clownOutfit", "tigerOutfit"), "Core Crew"),
        CharacterCatalogEntry("ella", "Ella", listOf("default", "rastaOutfit", "goldOutfit"), "Core Crew"),
        CharacterCatalogEntry("king", "King", listOf("default", "countOutfit", "royalOutfit"), "Core Crew"),
        CharacterCatalogEntry("ninja", "Ninja", listOf("default", "yangOutfit", "flameOutfit"), "Core Crew"),
        CharacterCatalogEntry("lucy", "Lucy", listOf("default", "gothOutfit", "steamOutfit"), "Core Crew"),
        CharacterCatalogEntry("tasha", "Tasha", listOf("default", "cheeroutfit", "gymOutfit"), "Core Crew"),
        CharacterCatalogEntry("tagbot", "Tagbot", listOf("default", "spaceOutfit", "toyOutfit"), "Core Crew"),
        CharacterCatalogEntry("zoe", "Zoe", listOf("default", "curlyOutfit", "bikerOutfit"), "Core Crew"),
        CharacterCatalogEntry("missMaia", "Miss Maia", listOf("default"), "Core Crew"),
        CharacterCatalogEntry("boombot", "Boombot", listOf("default"), "Core Crew"),
        CharacterCatalogEntry("dino", "Dino", listOf("default"), "Core Crew"),
        CharacterCatalogEntry("zombiejake", "Zombie Jake", listOf("default", "seriousOutfit"), "Halloween"),
        CharacterCatalogEntry("elfTricky", "Elf Tricky", listOf("default", "iceOutfit", "candyOutfit"), "Festive"),
        CharacterCatalogEntry("tony", "Tony", listOf("default", "folkOutfit", "gameOutfit"), "World Tour"),
        CharacterCatalogEntry("carmen", "Carmen", listOf("default", "shakeOutfit", "teamOutfit"), "World Tour"),
        CharacterCatalogEntry("roberto", "Roberto", listOf("default", "fanOutfit", "culesOutfit"), "World Tour"),
        CharacterCatalogEntry("kim", "Kim", listOf("default", "diveOutfit", "coastOutfit"), "World Tour"),
        CharacterCatalogEntry("harumi", "Harumi", listOf("default", "meowOutfit", "furyOutfit"), "World Tour"),
        CharacterCatalogEntry("nick", "Nick", listOf("default", "speedOutfit", "neonOutfit"), "World Tour"),
        CharacterCatalogEntry("coco", "Coco", listOf("default", "artOutfit", "jesterOutfit"), "World Tour"),
        CharacterCatalogEntry("sun", "Sun", listOf("default", "spotOutfit", "rogueOutfit"), "World Tour"),
        CharacterCatalogEntry("alex", "Alex", listOf("default", "technoOutfit"), "World Tour"),
        CharacterCatalogEntry("eddy", "Eddy", listOf("default", "trickOutfit", "vampireOutfit"), "Halloween"),
        CharacterCatalogEntry("jamie", "Jamie", listOf("default", "tophatOutfit", "hatterOutfit"), "World Tour"),
        CharacterCatalogEntry("jay", "Jay", listOf("default", "blueOutfit", "colorMasterOutfit"), "World Tour"),
        CharacterCatalogEntry("mina", "Mina", listOf("default", "roboOutfit", "popOutfit"), "World Tour"),
        CharacterCatalogEntry("rosa", "Rosa", listOf("default", "foxOutfit", "ceiboOutfit"), "World Tour"),
        CharacterCatalogEntry("olivia", "Olivia", listOf("default", "skateOutfit", "derbyMacabreOutfit"), "World Tour"),
        CharacterCatalogEntry("edison", "Edison", listOf("default", "urbanOutfit"), "World Tour"),
        CharacterCatalogEntry("wayne", "Wayne", listOf("default", "chiefOutfit", "knightOutfit"), "World Tour"),
        CharacterCatalogEntry("jasmine", "Jasmine", listOf("default", "safariOutfit", "ankhOutfit"), "World Tour"),
        CharacterCatalogEntry("noon", "Noon", listOf("default", "pinkOutfit", "siamOutfit"), "World Tour"),
        CharacterCatalogEntry("buddy", "Buddy", listOf("default", "sunnyOutfit", "candyOutfit"), "Festive"),
        CharacterCatalogEntry("rex", "Rex", listOf("default", "showOutfit", "winOutfit"), "World Tour"),
        CharacterCatalogEntry("izzy", "Izzy", listOf("default", "alohaOutfit", "lakiOutfit"), "World Tour"),
        CharacterCatalogEntry("amira", "Amira", listOf("default", "genieOutfit", "jewelOutfit"), "World Tour"),
        CharacterCatalogEntry("marco", "Marco", listOf("default", "maskOutfit", "nobleOutfit"), "World Tour"),
        CharacterCatalogEntry("nikos", "Nikos", listOf("default", "heroOutfit", "hopperOutfit"), "World Tour"),
        CharacterCatalogEntry("zuri", "Zuri", listOf("default", "cityOutfit"), "World Tour"),
        CharacterCatalogEntry("mike", "Mike", listOf("default", "howlOutfit", "jagerOutfit"), "Halloween"),
        CharacterCatalogEntry("jenny", "Jenny", listOf("default", "partyOutfit", "pixelOutfit"), "World Tour"),
        CharacterCatalogEntry("jaro", "Jaro", listOf("default", "jokerOutfit"), "World Tour"),
        CharacterCatalogEntry("aina", "Aina", listOf("default", "daisyOutfit", "blueOutfit"), "World Tour"),
        CharacterCatalogEntry("carlos", "Carlos", listOf("default", "incaOutfit", "ponchoOutfit"), "World Tour"),
        CharacterCatalogEntry("jia", "Jia", listOf("default", "wildOutfit", "unicornOutfit"), "World Tour"),
        CharacterCatalogEntry("bjarki", "Bjarki", listOf("default", "powerOutfit", "fisherOutfit"), "World Tour"),
        CharacterCatalogEntry("ramona", "Ramona", listOf("default", "pinaOutfit", "elegantOutfit"), "World Tour"),
        CharacterCatalogEntry("lauren", "Lauren", listOf("default", "tallyOutfit", "pilotOutfit"), "World Tour"),
        CharacterCatalogEntry("malik", "Malik", listOf("default", "tuskOutfit", "penguinOutfit"), "World Tour"),
        CharacterCatalogEntry("jolien", "Jolien", listOf("default", "floristOutfit", "springOutfit"), "World Tour"),
        CharacterCatalogEntry("philip", "Philip", listOf("default", "racerOutfit", "captainOutfit"), "World Tour"),
        CharacterCatalogEntry("freya", "Freya", listOf("default", "vikingOutfit", "winterMagicOutfit"), "World Tour"),
        CharacterCatalogEntry("salma", "Salma", listOf("default", "nomadOutfit", "talismanOutfit"), "World Tour"),
        CharacterCatalogEntry("lee", "Lee", listOf("default", "silkOutfit", "wudangOutfit"), "World Tour"),
        CharacterCatalogEntry("diego", "Diego", listOf("default", "flamencoOutfit", "chivalryoutfit"), "World Tour"),
        CharacterCatalogEntry("manny", "Manny", listOf("default", "mariachiOutfit", "luchadorOutfit"), "Halloween"),
        CharacterCatalogEntry("nicolai", "Nicolai", listOf("default", "frostOutfit", "fauxFurOutfit"), "Festive"),
        CharacterCatalogEntry("ez", "E.Z.", listOf("default", "touchdownOutfit", "JiveOutfit"), "World Tour"),
        CharacterCatalogEntry("sofia", "Sofia", listOf("default", "gauchoOutfit", "tangoOutfit"), "World Tour"),
        CharacterCatalogEntry("dylan", "Dylan", listOf("default", "lifeguardOutfit", "walkmanOutfit"), "World Tour"),
        CharacterCatalogEntry("nina", "Nina", listOf("default", "cyberPunkOutfit", "whitenightOutfit"), "World Tour"),
        CharacterCatalogEntry("brandon", "Brandon", listOf("default", "kungFuOutfit"), "World Tour"),
        CharacterCatalogEntry("scarlett", "Scarlett", listOf("default", "catrinaOutfit", "botanicalHorrorOutfit"), "Halloween"),
        CharacterCatalogEntry("alicia", "Alicia", listOf("default", "tracksuitOutfit", "fallOutfit"), "World Tour"),
        CharacterCatalogEntry("hugo", "Hugo", listOf("default", "mountainOutfit", "piratOutfit"), "World Tour"),
        CharacterCatalogEntry("rabbot", "Rabbot", listOf("default", "ironbunOutfit"), "Special"),
        CharacterCatalogEntry("rin", "Rin", listOf("default", "lunarOutfit"), "World Tour"),
        CharacterCatalogEntry("kareem", "Kareem", listOf("default", "SilkOutfit"), "World Tour"),
        CharacterCatalogEntry("ace", "Ace", listOf("default", "pilotOutfit"), "World Tour"),
        CharacterCatalogEntry("mei", "Mei", listOf("default", "islanderOutfit", "bumiOutfit"), "World Tour"),
        CharacterCatalogEntry("mimi", "Mimi", listOf("default", "folkOutfit"), "World Tour"),
        CharacterCatalogEntry("maeko", "Maeko", listOf("default", "hostessOutfit", "streetOutfit"), "World Tour"),
        CharacterCatalogEntry("alba", "Alba", listOf("default", "cowgirlOutfit", "spaceOutfit"), "World Tour"),
        CharacterCatalogEntry("amy", "Amy", listOf("default", "regularOutfit"), "World Tour"),
        CharacterCatalogEntry("festiveFresh", "Festive Fresh", listOf("default", "hoodieOutfit"), "Festive"),
        CharacterCatalogEntry("festiveJake", "Festive Jake", listOf("default"), "Festive"),
        CharacterCatalogEntry("festiveTricky", "Festive Tricky", listOf("default"), "Festive"),
        CharacterCatalogEntry("festiveYutani", "Festive Yutani", listOf("default"), "Festive"),
        CharacterCatalogEntry("festiveSpike", "Festive Spike", listOf("default"), "Festive"),
        CharacterCatalogEntry("festiveFrank", "Festive Frank", listOf("default", "anubisOutfit", "fortuneOutfit"), "Festive"),
        CharacterCatalogEntry("monkbot", "Monkbot", listOf("default"), "Special"),
        CharacterCatalogEntry("ming", "Ming", listOf("default", "whitesnakeOutfit"), "World Tour"),
        CharacterCatalogEntry("liu", "Liu", listOf("default", "birdwatcherOutfit"), "World Tour"),
        CharacterCatalogEntry("callum", "Callum", listOf("default", "changelingOutfit", "pinkpunkOutfit"), "World Tour"),
        CharacterCatalogEntry("jack", "Jack", listOf("default", "businessOutfit"), "World Tour"),
        CharacterCatalogEntry("tankbot", "Tankbot", listOf("default", "incaOutfit"), "Special"),
        CharacterCatalogEntry("cathy", "Cathy", listOf("default", "visualkeiOutfit", "seasideOutfit"), "World Tour"),
        CharacterCatalogEntry("noel", "Noel", listOf("default", "bigblueOutfit"), "World Tour"),
        CharacterCatalogEntry("morgan", "Morgan", listOf("default", "swampMysteryOutfit"), "Halloween"),
        CharacterCatalogEntry("andy", "Andy", listOf("default", "gamerOutfit", "theGreatOutdoorOutfit"), "World Tour"),
        CharacterCatalogEntry("frankette", "Frankette", listOf("default"), "Special"),
        CharacterCatalogEntry("spacebot", "Spacebot", listOf("default"), "Special"),
        CharacterCatalogEntry("qin", "Qin", listOf("default", "warriorOutfit"), "World Tour"),
        CharacterCatalogEntry("hou", "Hou", listOf("default", "archerOutfit"), "World Tour"),
        CharacterCatalogEntry("zayn", "Zayn", listOf("default", "stealthOutfit", "eastvestOutfit"), "World Tour"),
        CharacterCatalogEntry("darryl", "Darryl", listOf("default", "gonzoOutfit"), "World Tour"),
        CharacterCatalogEntry("lana", "Lana", listOf("default", "nordicOutfit"), "World Tour"),
        CharacterCatalogEntry("bonnie", "Bonnie", listOf("default", "harajukuOutfit", "luckyOutfit"), "World Tour"),
        CharacterCatalogEntry("buzz", "Buzz", listOf("default"), "World Tour"),
        CharacterCatalogEntry("trym", "Trym", listOf("default", "noblenewbieOutfit"), "World Tour"),
        CharacterCatalogEntry("alexandre", "Alexandre", listOf("default", "stanOutfit"), "World Tour"),
        CharacterCatalogEntry("parkourRin", "Parkour Rin", listOf("default"), "Special"),
        CharacterCatalogEntry("akira", "Akira", listOf("default", "nightriderOutfit"), "World Tour"),
        CharacterCatalogEntry("koral", "Koral", listOf("default", "tentaqueenOutfit", "subexplorerOutfit"), "World Tour"),
        CharacterCatalogEntry("cleo", "Cleo", listOf("default", "heartbreakerOutfit", "blossomqueenOutfit"), "World Tour"),
        CharacterCatalogEntry("taha", "Taha", listOf("default", "TropiTahaOutfit", "chilitahaOutfit"), "World Tour"),
        CharacterCatalogEntry("bobTheBlob", "Bob The Blob", listOf("default", "slimeOutfit", "jingleOutfit"), "Special"),
        CharacterCatalogEntry("mala", "Mala", listOf("default", "lunarOutfit", "missmosaicOutfit"), "World Tour"),
        CharacterCatalogEntry("phoenix", "Phoenix", listOf("default", "partyBusinessOutfit", "summerOutfit"), "World Tour"),
        CharacterCatalogEntry("clementine", "Clementine", listOf("default", "frostyOutfit", "hideandsheepOutfit"), "Festive"),
        CharacterCatalogEntry("gingerbot", "Gingerbot", listOf("default", "wrapperOutfit"), "Festive"),
        CharacterCatalogEntry("hasina", "Hasina", listOf("default", "worldCupOutfit", "holidayOutfit"), "World Tour"),
        CharacterCatalogEntry("charlie", "Charlie", listOf("default"), "World Tour"),
        CharacterCatalogEntry("pink", "Pink", listOf("default", "spellcasterOutfit", "witchcapOutfit"), "Halloween"),
        CharacterCatalogEntry("george", "George", listOf("default", "moonbunOutfit", "eggstravaganzaOutfit"), "World Tour"),
        CharacterCatalogEntry("dummy", "Dummy", listOf("default", "skellyOutfit"), "Special"),
        CharacterCatalogEntry("superRunnerJake", "Super Runner Jake", listOf("default"), "Super Runner"),
        CharacterCatalogEntry("superRunnerTricky", "Super Runner Tricky", listOf("default"), "Super Runner"),
        CharacterCatalogEntry("superRunnerFresh", "Super Runner Fresh", listOf("default"), "Super Runner"),
        CharacterCatalogEntry("superRunnerYutani", "Super Runner Yutani", listOf("default"), "Super Runner"),
        CharacterCatalogEntry("moira", "Moira", listOf("default", "thetouristOutfit", "theholiOutfit"), "World Tour"),
        CharacterCatalogEntry("zihao", "Zihao", listOf("default", "vanillastrawberryOutfit"), "World Tour"),
        CharacterCatalogEntry("alia", "Alia", listOf("default", "sneakyblindersOutfit"), "World Tour"),
        CharacterCatalogEntry("fantasma", "Fantasma", listOf("default"), "Halloween"),
        CharacterCatalogEntry("songYi", "Song Yi", listOf("default", "goldencrushOutfit", "luckyKoiOutfit"), "World Tour"),
        CharacterCatalogEntry("fernando", "Fernando", listOf("default", "superscoutOutfit"), "World Tour"),
        CharacterCatalogEntry("soul", "Soul", listOf("default", "centurionOutfit"), "World Tour"),
        CharacterCatalogEntry("tutti", "Tutti", listOf("default"), "Special"),
        CharacterCatalogEntry("frutti", "Frutti", listOf("default"), "Special"),
        CharacterCatalogEntry("spraybot", "Spraybot", listOf("default"), "Special"),
        CharacterCatalogEntry("catrine", "Catrine", listOf("default", "trailmixOutfit"), "World Tour"),
        CharacterCatalogEntry("rain", "Rain", listOf("default", "gardenOutfit"), "World Tour"),
        CharacterCatalogEntry("alfie", "Alfie", listOf("default", "cupidOutfit", "backyardtrollOutfit"), "World Tour"),
        CharacterCatalogEntry("bruno", "Bruno", listOf("default", "pixelProOutfit"), "World Tour"),
        CharacterCatalogEntry("taina", "Taina", listOf("default", "judokaOutfit"), "World Tour"),
        CharacterCatalogEntry("yuto", "Yuto", listOf("default"), "World Tour"),
        CharacterCatalogEntry("hali", "Hali", listOf("default"), "World Tour"),
        CharacterCatalogEntry("thursday", "Thursday", listOf("default", "lilImpOutfit", "jeudiOutfit"), "Halloween"),
        CharacterCatalogEntry("jiYeong", "Ji-Yeong", listOf("default", "celestialOutfit"), "World Tour"),
        CharacterCatalogEntry("vivienne", "Vivienne", listOf("default", "hollywoodglamOutfit"), "World Tour"),
        CharacterCatalogEntry("lawrence", "Lawrence", listOf("default", "snowballerOutfit"), "World Tour"),
        CharacterCatalogEntry("clockworkJohnny", "Clockwork Johnny", listOf("default"), "Special"),
        CharacterCatalogEntry("captainFlash", "Captain Flash", listOf("default"), "Special"),
        CharacterCatalogEntry("skye", "Skye", listOf("default", "skyAttackOutfit"), "World Tour"),
        CharacterCatalogEntry("thunder", "Thunder", listOf("default"), "World Tour"),
        CharacterCatalogEntry("azura", "Azura", listOf("default"), "World Tour"),
        CharacterCatalogEntry("dragonyear", "Dragon", listOf("default"), "Lunar"),
        CharacterCatalogEntry("astra", "Astra", listOf("default"), "World Tour"),
        CharacterCatalogEntry("stella", "Stella", listOf("default"), "World Tour"),
        CharacterCatalogEntry("lucky", "Lucky", listOf("default"), "World Tour"),
        CharacterCatalogEntry("meggan", "Meggan", listOf("default"), "World Tour"),
        CharacterCatalogEntry("redpanda", "Red Panda", listOf("default"), "Special"),
        CharacterCatalogEntry("hamza", "Hamza", listOf("default"), "World Tour"),
        CharacterCatalogEntry("faiza", "Faiza", listOf("default"), "World Tour"),
        CharacterCatalogEntry("teabot", "Teabot", listOf("default"), "Special"),
        CharacterCatalogEntry("finn", "Finn", listOf("default"), "World Tour"),
        CharacterCatalogEntry("electra", "Electra", listOf("default"), "World Tour"),
        CharacterCatalogEntry("hammybee", "Hammy Bee", listOf("default"), "Special"),
        CharacterCatalogEntry("pixeljake", "Pixel Jake", listOf("default"), "Special"),
        CharacterCatalogEntry("ballerinatricky", "Ballerina Tricky", listOf("default"), "Special"),
        CharacterCatalogEntry("guardking", "Guard King", listOf("default"), "Special"),
        CharacterCatalogEntry("riley", "Riley", listOf("default", "aquamarineOutfit"), "World Tour"),
        CharacterCatalogEntry("lily", "Lily", listOf("default", "kangarumiOutfit"), "World Tour"),
        CharacterCatalogEntry("rudyrascal", "Rudy Rascal", listOf("default"), "World Tour"),
        CharacterCatalogEntry("ventura", "Ventura", listOf("default"), "World Tour"),
        CharacterCatalogEntry("glorgalax", "Glorgalax", listOf("default", "galaxyEmpurrorOutfit"), "Special"),
        CharacterCatalogEntry("helene", "Helene", listOf("default", "ogrepowerOutfit"), "World Tour"),
        CharacterCatalogEntry("leo", "Leo", listOf("default"), "World Tour"),
        CharacterCatalogEntry("ozzy", "Ozzy", listOf("default"), "World Tour"),
        CharacterCatalogEntry("pins", "Pins", listOf("default"), "Halloween"),
        CharacterCatalogEntry("audrey", "Audrey", listOf("default"), "World Tour"),
        CharacterCatalogEntry("billybean", "Billy Bean", listOf("default"), "Special"),
        CharacterCatalogEntry("ruhi", "Ruhi", listOf("default"), "World Tour"),
        CharacterCatalogEntry("kedi", "Kedi", listOf("default"), "Special"),
        CharacterCatalogEntry("drflyman", "Dr. Flyman", listOf("default"), "Halloween"),
        CharacterCatalogEntry("logan", "Logan", listOf("default"), "World Tour"),
        CharacterCatalogEntry("ginger", "Ginger", listOf("default"), "World Tour"),
        CharacterCatalogEntry("leif", "Leif", listOf("default", "fireBirdOutfit"), "World Tour"),
        CharacterCatalogEntry("sindri", "Sindri", listOf("default"), "World Tour"),
        CharacterCatalogEntry("snowy", "Snowy", listOf("default"), "Festive"),
        CharacterCatalogEntry("grandjuke", "Grand Juke", listOf("default"), "World Tour"),
        CharacterCatalogEntry("snake", "Snake", listOf("default"), "Lunar"),
        CharacterCatalogEntry("tangyuan", "Tangyuan", listOf("default"), "Lunar"),
        CharacterCatalogEntry("valentina", "Valentina", listOf("default"), "World Tour"),
        CharacterCatalogEntry("kaleo", "Kaleo", listOf("default"), "World Tour"),
        CharacterCatalogEntry("rebecaroar", "Rebeca Roar", listOf("default", "purrfectionOutfit"), "World Tour"),
        CharacterCatalogEntry("gabriel", "Gabriel", listOf("default"), "World Tour"),
        CharacterCatalogEntry("dinorange", "Dinorange", listOf("default"), "Special"),
        CharacterCatalogEntry("rocky", "Rocky", listOf("default"), "World Tour"),
        CharacterCatalogEntry("rani", "Rani", listOf("default"), "World Tour"),
        CharacterCatalogEntry("chickenjake", "Chicken Jake", listOf("default"), "Special"),
        CharacterCatalogEntry("mallardtricky", "Mallard Tricky", listOf("default"), "Special"),
        CharacterCatalogEntry("finlay", "Finlay", listOf("default"), "World Tour"),
        CharacterCatalogEntry("ribbon", "Ribbon", listOf("default"), "Special"),
        CharacterCatalogEntry("oniHime", "Oni Hime", listOf("default", "chillBillOutfit"), "World Tour"),
        CharacterCatalogEntry("saru", "Saru", listOf("default"), "World Tour"),
        CharacterCatalogEntry("loc", "Loc", listOf("default"), "World Tour"),
        CharacterCatalogEntry("stevie", "Stevie", listOf("default"), "World Tour"),
        CharacterCatalogEntry("berta", "Berta", listOf("default"), "World Tour"),
        CharacterCatalogEntry("costa", "Costa", listOf("default"), "World Tour"),
        CharacterCatalogEntry("rochelleRebelle", "Rochelle Rebelle", listOf("default"), "World Tour"),
        CharacterCatalogEntry("asher", "Asher", listOf("default"), "World Tour"),
        CharacterCatalogEntry("peter", "Peter", listOf("default"), "World Tour"),
        CharacterCatalogEntry("tara", "Tara", listOf("default"), "World Tour"),
        CharacterCatalogEntry("henry", "Henry", listOf("default"), "World Tour"),
        CharacterCatalogEntry("sunny", "Sunny", listOf("default"), "World Tour"),
        CharacterCatalogEntry("mateo", "Mateo", listOf("default"), "World Tour"),
        CharacterCatalogEntry("jooWon", "Joo-Won", listOf("default"), "World Tour"),
        CharacterCatalogEntry("siWoo", "Si-Woo", listOf("default", "springtimeOutfit"), "World Tour"),
        CharacterCatalogEntry("mrGobbles", "Mr. Gobbles", listOf("default"), "Festive"),
        CharacterCatalogEntry("rosie", "Rosie", listOf("default"), "World Tour"),
        CharacterCatalogEntry("ballerinaLatticina", "Ballerina Latticina", listOf("default"), "Special"),
        CharacterCatalogEntry("frankNoir", "Frank Noir", listOf("default"), "Special"),
        CharacterCatalogEntry("holly", "Holly", listOf("default", "blobbusterOutfit"), "World Tour"),
        CharacterCatalogEntry("mia", "Mia", listOf("default", "alpineAmourOutfit"), "World Tour"),
        CharacterCatalogEntry("rosalita", "Rosalita", listOf("default"), "Special"),
        CharacterCatalogEntry("lorcan", "Lorcan", listOf("default"), "World Tour"),
        CharacterCatalogEntry("horse", "Horse", listOf("default"), "Lunar"),
        CharacterCatalogEntry("vittorio", "Vittorio", listOf("default"), "World Tour"),
        CharacterCatalogEntry("enzo", "Enzo", listOf("default", "gladiatorOutfit"), "World Tour"),
        CharacterCatalogEntry("sprinkle", "Sprinkle", listOf("default"), "Special"),
        CharacterCatalogEntry("bunnyBetty", "Bunny Betty", listOf("default", "redBunnyOutfit"), "Special"),
        CharacterCatalogEntry("tsukiChan", "Tsuki-Chan", listOf("default"), "World Tour"),
        CharacterCatalogEntry("kaito", "Kaito", listOf("default", "hayaKitsuneOutfit"), "World Tour"),
        CharacterCatalogEntry("toaxx", "Toaxx", listOf("default"), "World Tour"),
        CharacterCatalogEntry("ratak", "Ratak", listOf("default", "radioRativeOutfit"), "World Tour"),
        CharacterCatalogEntry("goldenGuardian", "Golden Guardian", listOf("default"), "Special"),
        CharacterCatalogEntry("lestari", "Lestari", listOf("default", "hotSpotOutfit"), "World Tour"),
        CharacterCatalogEntry("sandy", "Sandy", listOf("default", "sinkOrSwimOutfit"), "World Tour"),
        CharacterCatalogEntry("redino", "Redino", listOf("default"), "Special"),
        CharacterCatalogEntry("marieMacaron", "Marie Macaron", listOf("default"), "World Tour"),
        CharacterCatalogEntry("pierre", "Pierre", listOf("default", "bonVoyageOutfit"), "World Tour"),
        CharacterCatalogEntry("runaDarko", "Runa Darko", listOf("default"), "World Tour"),
        CharacterCatalogEntry("wolf", "Wolf", listOf("default", "heroOfRainOutfit"), "World Tour")
    )

    val classicBoards: List<BoardCatalogEntry> = listOf(
        BoardCatalogEntry("default", "Hoverboard", listOf("default")),
        BoardCatalogEntry("batLuck", "Bat Luck (S125)", listOf("superJump", "highSpeed")),
        BoardCatalogEntry("broom", "Broom (S125)", listOf("doubleJump")),
        BoardCatalogEntry("noxDrift", "Nox Drift (S125)"),
        BoardCatalogEntry("plankenstein", "Plankenstein (S124)", listOf("superJump", "glider")),
        BoardCatalogEntry("digiDash", "Digi-Dash (S124)"),
        BoardCatalogEntry("sketchySkater", "Sketchy Skater (S124)", listOf("highSpeed")),
        BoardCatalogEntry("monster", "Monster", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("skullFire", "Skull Fire", listOf("doubleJump", "highSpeed")),
        BoardCatalogEntry("bouncer", "Bouncer"),
        BoardCatalogEntry("teleporter", "Teleporter"),
        BoardCatalogEntry("greatWhite", "Great White", listOf("speed", "glider")),
        BoardCatalogEntry("sunset", "Sunset", listOf("trail", "glider")),
        BoardCatalogEntry("starboard", "Starboard", listOf("trail", "doubleJump")),
        BoardCatalogEntry("superhero", "Superhero", listOf("teleport", "jumper")),
        BoardCatalogEntry("freestyler", "Freestyler", listOf("doubleJump", "jumper")),
        BoardCatalogEntry("bigKahuna", "Big Kahuna", listOf("glider", "lowrider")),
        BoardCatalogEntry("lumberjack", "Lumberjack", listOf("doubleJump", "lowrider")),
        BoardCatalogEntry("scoot", "Scoot", listOf("teleport", "highSpeed")),
        BoardCatalogEntry("chicky", "Chicky", listOf("trail", "highSpeed")),
        BoardCatalogEntry("jingles", "Jingles", listOf("trail", "superJump")),
        BoardCatalogEntry("bassBlaster", "Bass Blaster", listOf("trail", "jumper")),
        BoardCatalogEntry("colorCloud", "Color Cloud", listOf("highSpeed", "teleport")),
        BoardCatalogEntry("magicCarpet", "Magic Carpet", listOf("glider", "jumper")),
        BoardCatalogEntry("rocket", "Rocket", listOf("trail", "highSpeed")),
        BoardCatalogEntry("4thOfJuly", "4th Of July", listOf("trail", "highSpeed")),
        BoardCatalogEntry("stallion", "Stallion", listOf("trail", "jumper")),
        BoardCatalogEntry("jazz", "Jazz", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("aero", "Aero", listOf("highSpeed", "glider")),
        BoardCatalogEntry("scarab", "Scarab", listOf("glider", "highSpeed")),
        BoardCatalogEntry("mapleLeaf", "Maple Leaf", listOf("glider", "superJump")),
        BoardCatalogEntry("smokingSlime", "Smoking Slime", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("dangerdasher", "Danger Dasher", listOf("lowrider", "doubleJump")),
        BoardCatalogEntry("futureWave", "Future Wave", listOf("highSpeed", "doubleJump")),
        BoardCatalogEntry("raveRider", "Rave Rider", listOf("doubleJump", "glider")),
        BoardCatalogEntry("flamingdoll", "Flaming Doll", listOf("lowrider", "superJump")),
        BoardCatalogEntry("backtothefuture", "Back to the Future"),
        BoardCatalogEntry("energy", "Energy", listOf("teleport", "superJump")),
        BoardCatalogEntry("sharpeed", "Sharpeed", listOf("superJump", "highSpeed")),
        BoardCatalogEntry("electroshock", "Electro Shock", listOf("doubleJump", "glider")),
        BoardCatalogEntry("skyrocket", "Sky Rocket", listOf("highSpeed", "doubleJump")),
        BoardCatalogEntry("roaringheart", "Roaring Heart", listOf("highSpeed", "doubleJump")),
        BoardCatalogEntry("interstellar", "Interstellar", listOf("glider", "lowrider")),
        BoardCatalogEntry("astrotune", "Astro Tune", listOf("doubleJump", "highSpeed")),
        BoardCatalogEntry("meggabite", "Megga Bite", listOf("highSpeed", "lowrider")),
        BoardCatalogEntry("jak", "Jak", listOf("glider", "highSpeed")),
        BoardCatalogEntry("teatimer", "Tea Timer", listOf("highSpeed", "glider")),
        BoardCatalogEntry("yellowsub", "Yellow Sub", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("mayomania", "Mayo Mania", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("alarmon", "Alarm On", listOf("highSpeed", "glider")),
        BoardCatalogEntry("sunnysailor", "Sunny Sailor", listOf("superJump", "glider")),
        BoardCatalogEntry("riskyride", "Risky Ride", listOf("highSpeed", "lowrider")),
        BoardCatalogEntry("heavymetal", "Heavy Metal", listOf("highSpeed", "lowrider")),
        BoardCatalogEntry("champ", "Champ", listOf("doubleJump", "glider")),
        BoardCatalogEntry("raggedyrunner", "Raggedy Runner", listOf("superJump", "glider")),
        BoardCatalogEntry("brasswhizz", "Brass Whizz", listOf("doubleJump", "glider")),
        BoardCatalogEntry("cookexpress", "Cook-Express", listOf("doubleJump", "glider")),
        BoardCatalogEntry("veggievelocity", "Veggie Velocity", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("couchSurfer", "Couch Surfer", listOf("glider", "doubleJump")),
        BoardCatalogEntry("feartractor", "Fear Tractor", listOf("doubleJump", "glider")),
        BoardCatalogEntry("scifly", "Sci-Fly", listOf("superJump", "glider")),
        BoardCatalogEntry("foxtrail", "Fox Trail", listOf("superJump", "glider")),
        BoardCatalogEntry("cozycruiser", "Cozy Cruiser", listOf("highSpeed", "lowrider")),
        BoardCatalogEntry("jingledingle", "Jingle Dingle", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("lantern", "Lantern", listOf("doubleJump", "glider")),
        BoardCatalogEntry("heartstopper", "Heartstopper", listOf("highSpeed", "glider")),
        BoardCatalogEntry("roarsome", "Roarsome", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("purplespin", "Purple Spin", listOf("superJump", "lowrider")),
        BoardCatalogEntry("bloomster", "Bloomster", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("guardboard", "Guard Board", listOf("glider", "highSpeed")),
        BoardCatalogEntry("trespasser", "Trespasser", listOf("superJump", "glider")),
        BoardCatalogEntry("youarehere", "You Are Here", listOf("doubleJump", "highSpeed")),
        BoardCatalogEntry("floatBooster", "Float Booster", listOf("highSpeed", "glider")),
        BoardCatalogEntry("streetStyle", "Street Style", listOf("lowrider", "superJump")),
        BoardCatalogEntry("businessTime", "Business Time", listOf("lowrider", "highSpeed")),
        BoardCatalogEntry("brainstormer", "Brainstormer", listOf("superJump", "glider")),
        BoardCatalogEntry("theDelyreious", "The De-Lyre-ious", listOf("superJump", "highSpeed")),
        BoardCatalogEntry("motorMania", "Motor Mania", listOf("doubleJump", "lowrider")),
        BoardCatalogEntry("fiesta", "Fiesta", listOf("highSpeed", "glider")),
        BoardCatalogEntry("biasBoom", "Bias Boom", listOf("superJump", "highSpeed")),
        BoardCatalogEntry("gobbleTrouble", "Gobble Trouble", listOf("glider", "highSpeed")),
        BoardCatalogEntry("meanGreenMachine", "Mean Green Machine", listOf("superJump", "glider")),
        BoardCatalogEntry("frankmobile", "Frankmobile", listOf("superJump", "glider")),
        BoardCatalogEntry("fireStarter", "Fire Starter", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("theGreenFlame", "The Green Flame", listOf("glider", "doubleJump")),
        BoardCatalogEntry("marbleous", "Marbleous", listOf("highSpeed", "doubleJump")),
        BoardCatalogEntry("pixelPerfect", "Pixel Perfect", listOf("doubleJump", "glider")),
        BoardCatalogEntry("fastFurward", "Fast Furward", listOf("superJump", "highSpeed")),
        BoardCatalogEntry("sewerSurfer", "Sewer Surfer", listOf("glider", "superJump")),
        BoardCatalogEntry("14thBirthday", "14th Birthday"),
        BoardCatalogEntry("bombBastic", "Bomb-Bastic"),
        BoardCatalogEntry("templeRush", "Temple Rush", listOf("doubleJump", "lowrider")),
        BoardCatalogEntry("offRoadRanger", "Off-Road Ranger", listOf("highSpeed", "superJump")),
        BoardCatalogEntry("sweetElite", "Sweet Elite", listOf("highSpeed", "glider")),
        BoardCatalogEntry("vulkarion", "Vulkarion", listOf("glider", "highSpeed")),
        BoardCatalogEntry("astroBelt", "Astro-Belt", listOf("highSpeed", "lowrider")),
        BoardCatalogEntry("eightBall", "8 Ball"),
        BoardCatalogEntry("hexed", "Hexed"),
        BoardCatalogEntry("pumpkin", "Pumpkin"),
        BoardCatalogEntry("shinSupreme", "Shin Supreme", listOf("doubleJump")),
        BoardCatalogEntry("goat", "Goat")
    )

    // =========================================================================
    // 2. БАЗА ДАННЫХ SUBWAY SURFERS CITY (ПЕРСОНАЖИ, ТРИАЛЫ, КОПИЛКА, РАЙОНЫ)
    // =========================================================================

    val cityCharacters: List<CharacterCatalogEntry> = listOf(
        CharacterCatalogEntry("jake", "Jake (City)", listOf("default", "street", "neon"), "Core"),
        CharacterCatalogEntry("tricky", "Tricky (City)", listOf("default", "beat", "cyber"), "Core"),
        CharacterCatalogEntry("fresh", "Fresh (City)", listOf("default", "boom", "gold"), "Core"),
        CharacterCatalogEntry("yutani", "Yutani (City)", listOf("default", "alien", "tech"), "Core"),
        CharacterCatalogEntry("spike", "Spike (City)", listOf("default", "punk", "chrome"), "Core"),
        CharacterCatalogEntry("brizzy", "Brizzy", listOf("default", "roller"), "City Exclusive"),
        CharacterCatalogEntry("celeste", "Celeste", listOf("default", "starlight"), "City Exclusive"),
        CharacterCatalogEntry("zane", "Zane", listOf("default", "shadow"), "City Exclusive"),
        CharacterCatalogEntry("clara", "Clara", listOf("default", "floral"), "City Exclusive"),
        CharacterCatalogEntry("dex", "Dex", listOf("default", "arcade"), "City Exclusive"),
        CharacterCatalogEntry("billy", "Billy", listOf("default", "hazard"), "City Exclusive"),
        CharacterCatalogEntry("nova", "Nova", listOf("default", "plasma"), "City Exclusive"),
        CharacterCatalogEntry("roxy", "Roxy", listOf("default", "glam"), "City Exclusive"),
        CharacterCatalogEntry("kilo", "Kilo", listOf("default", "heavy"), "City Exclusive"),
        CharacterCatalogEntry("veda", "Veda", listOf("default", "mystic"), "City Exclusive")
    )

    val cityTrialsIds = listOf(
        "trial_daily_rush",
        "trial_character_showcase",
        "trial_board_mastery",
        "trial_coin_blitz",
        "trial_survival_sprint",
        "trial_mystery_obstruction",
        "trial_high_voltage",
        "trial_weekend_grand_prix",
        "trial_district_tour",
        "trial_boss_chase"
    )

    val cityDistrictIds = listOf(
        "district_the_docks",
        "district_southline",
        "district_sunrise_blvd",
        "district_delorean_park",
        "district_neon_heights",
        "district_industrial_bay",
        "district_cyber_plaza"
    )

    // =========================================================================
    // 3. ПАТЧЕРЫ ДЛЯ SUBWAY SURFERS CITY (GAME.SAV)
    // =========================================================================

    /**
     * Открывает ВСЕ Триалы (Ивентовые забеги) в Subway Surfers City,
     * сбрасывает кулдауны, даёт бесконечные билеты входа и открывает все награды.
     */
    fun patchCityUnlockAllTrials(gameJson: String): String {
        val root = JSONObject(gameJson)
        val nowSec = Instant.now().epochSecond

        // 1. Разблокируем фичу триалов и билеты
        val currencies = root.optJSONObject("currencies") ?: JSONObject().also { root.put("currencies", it) }
        currencies.put("TrialTickets", 9999)
        currencies.put("EventTickets", 9999)
        currencies.put("Tickets", 9999)

        // 2. Патчим существующие или создаём секции trials / liveEvents / eventRuns
        val trialKeys = listOf("trials", "trialEvents", "eventRuns", "liveEvents")
        for (sectionKey in trialKeys) {
            val section = root.optJSONObject(sectionKey) ?: JSONObject().also { root.put(sectionKey, it) }
            section.put("isUnlocked", true)
            section.put("enabled", true)
            section.put("cooldownEndTime", 0)
            section.put("dailyAttemptsUsed", 0)
            section.put("maxDailyAttempts", 999)

            val activeMap = section.optJSONObject("entries") ?: section.optJSONObject("states") ?: JSONObject().also {
                section.put("entries", it)
            }

            // Проходим по существующим триалам
            val existingKeys = activeMap.keys().asSequence().toList()
            val allIds = (existingKeys + cityTrialsIds).distinct()

            for (trialId in allIds) {
                val entry = activeMap.optJSONObject(trialId) ?: JSONObject()
                entry.put("id", trialId)
                entry.put("unlocked", true)
                entry.put("isAvailable", true)
                entry.put("state", "Unlocked")
                entry.put("cooldown", 0)
                entry.put("nextAvailableTime", 0)
                entry.put("attemptsLeft", 99)
                entry.put("currentScore", 999999)
                entry.put("highScore", 999999)
                entry.put("startTime", nowSec - 3600)
                entry.put("endTime", nowSec + 86400 * 30)

                val milestones = entry.optJSONArray("milestones") ?: JSONArray()
                for (i in 0 until milestones.length()) {
                    milestones.optJSONObject(i)?.apply {
                        put("unlocked", true)
                        put("claimed", false) // Готово к сбору в игре!
                    }
                }
                activeMap.put(trialId, entry)
            }
        }
        return root.toString(2)
    }

    /**
     * Прокачивает Свинью-копилку (Piggy Bank) в Subway Surfers City:
     * заполняет до максимума, поднимает уровень копилки и убирает кулдаун.
     */
    fun patchCityPiggyBankMax(gameJson: String, gemsAmount: Int = 50000): String {
        val root = JSONObject(gameJson)
        val piggyKeys = listOf("piggyBank", "piggy_bank", "savingsBank")

        for (key in piggyKeys) {
            val piggy = root.optJSONObject(key) ?: JSONObject().also { root.put(key, it) }
            piggy.put("isUnlocked", true)
            piggy.put("enabled", true)
            piggy.put("level", 20)
            piggy.put("tier", 20)
            piggy.put("currentAmount", gemsAmount)
            piggy.put("storedGems", gemsAmount)
            piggy.put("storedCoins", gemsAmount * 10)
            piggy.put("capacity", gemsAmount)
            piggy.put("maxCapacity", gemsAmount)
            piggy.put("isFull", true)
            piggy.put("canClaimFree", true)
            piggy.put("cooldownEndTime", 0)
        }
        return root.toString(2)
    }

    /**
     * Максимальная прокачка аккаунта Subway Surfers City (Валюта + Районы + Персонажи 15 ур. + Доски 15 ур.)
     */
    fun patchCityMaxEverything(gameJson: String): String {
        var modified = patchCityUnlockAllTrials(gameJson)
        modified = patchCityPiggyBankMax(modified, 99999)
        val root = JSONObject(modified)

        // Валюта
        val currencies = root.optJSONObject("currencies") ?: JSONObject().also { root.put("currencies", it) }
        currencies.put("Coins", 999_999_999)
        currencies.put("Gems", 9_999_999)
        currencies.put("Keys", 9_999_999)
        currencies.put("Cans", 999_999)
        currencies.put("XP", 9_999_999)
        currencies.put("SkipAdTickets", 99_999)

        // Персонажи до 15 уровня
        val chars = root.optJSONObject("characters") ?: JSONObject().also { root.put("characters", it) }
        val allCharIds = (chars.keys().asSequence().toList() + cityCharacters.map { it.id }).distinct()
        for (cid in allCharIds) {
            val cObj = chars.optJSONObject(cid) ?: JSONObject()
            cObj.put("id", cid)
            cObj.put("unlocked", true)
            cObj.put("level", 15)
            cObj.put("xp", 99999)
            cObj.put("tokens", 9999)
            chars.put(cid, cObj)
        }

        // Районы (Districts)
        val districts = root.optJSONObject("districts") ?: JSONObject().also { root.put("districts", it) }
        for (did in cityDistrictIds) {
            val dObj = districts.optJSONObject(did) ?: JSONObject()
            dObj.put("id", did)
            dObj.put("unlocked", true)
            dObj.put("level", 10)
            dObj.put("stars", 30)
            districts.put(did, dObj)
        }

        return root.toString(2)
    }

    // =========================================================================
    // 4. ПАТЧЕРЫ ДЛЯ CLASSIC SUBWAY SURFERS (ПО ФАЙЛАМ ПРОФИЛЯ)
    // =========================================================================

    /**
     * Обновляет валюты в wallet.json и может добавить нераспакованные сундуки в очередь!
     */
    fun patchClassicWallet(
        walletJson: String,
        coins: Int = 999_999_999,
        keys: Int = 999_999,
        hoverboards: Int = 999_999,
        headstarts: Int = 99_999,
        scoreBoosters: Int = 99_999,
        eventCoins: Int = 999_999,
        seasonTokens: Int = 99_999,
        injectSuperMysteryBoxes: Int = 0
    ): String {
        val root = JSONObject(walletJson)
        root.put("lastSaved", Instant.now().toString())

        val currencies = root.optJSONObject("currencies") ?: JSONObject().also { root.put("currencies", it) }
        val map = mapOf(
            "1" to coins,
            "2" to keys,
            "3" to hoverboards,
            "4" to headstarts,
            "5" to scoreBoosters,
            "6" to eventCoins,
            "12" to seasonTokens
        )

        for ((id, amount) in map) {
            val entry = currencies.optJSONObject(id) ?: JSONObject()
            entry.put("value", amount)
            if (!entry.has("expirationType")) entry.put("expirationType", 0)
            if (!entry.has("contextType")) entry.put("contextType", 0)
            if (!entry.has("contextData")) entry.put("contextData", JSONObject())
            currencies.put(id, entry)
        }

        if (injectSuperMysteryBoxes > 0) {
            val queueObj = root.optJSONObject("lootboxQueue") ?: JSONObject().also { root.put("lootboxQueue", it) }
            val unopened = queueObj.optJSONArray("unopenedLootboxes") ?: JSONArray().also { queueObj.put("unopenedLootboxes", it) }
            repeat(injectSuperMysteryBoxes) {
                unopened.put(JSONObject().apply {
                    put("lootboxId", "super_mystery_box")
                    put("source", "editor_gift")
                })
            }
        }

        return root.toString(2)
    }

    /**
     * Разблокирует выбранного или ВСЕХ персонажей со всеми костюмами в characters_inventory.json
     */
    fun patchClassicCharacters(
        charsJson: String,
        targetEntries: List<CharacterCatalogEntry> = classicCharacters
    ): String {
        val root = JSONObject(charsJson)
        root.put("lastSaved", Instant.now().toString())
        val owned = root.optJSONObject("owned") ?: JSONObject().also { root.put("owned", it) }

        for (entry in targetEntries) {
            val existing = owned.optJSONObject(entry.id) ?: JSONObject()
            existing.put("id", entry.id)

            val outfitsArray = JSONArray()
            val mergedOutfits = mutableSetOf<String>()
            val oldArr = existing.optJSONArray("ownedOutfits")
            if (oldArr != null) {
                for (i in 0 until oldArr.length()) mergedOutfits.add(oldArr.getString(i))
            }
            mergedOutfits.addAll(entry.outfits)
            mergedOutfits.forEach { outfitsArray.put(it) }

            existing.put("ownedOutfits", outfitsArray)
            existing.put("lastOutfit", existing.optString("lastOutfit", "default"))
            owned.put(entry.id, existing)
        }
        return root.toString(2)
    }

    /**
     * Разблокирует выбранную или ВСЕ доски со всеми способностями в boards_inventory.json
     */
    fun patchClassicBoards(
        boardsJson: String,
        targetBoards: List<BoardCatalogEntry> = classicBoards
    ): String {
        val root = JSONObject(boardsJson)
        root.put("lastSaved", Instant.now().toString())
        val owned = root.optJSONObject("owned") ?: JSONObject().also { root.put("owned", it) }

        for (board in targetBoards) {
            val existing = owned.optJSONObject(board.id) ?: JSONObject()
            existing.put("id", board.id)
            val upgradesObj = existing.optJSONObject("ownedUpgrades") ?: JSONObject()
            for (upg in board.upgrades) {
                upgradesObj.put(upg, true)
            }
            existing.put("ownedUpgrades", upgradesObj)
            owned.put(board.id, existing)
        }
        return root.toString(2)
    }

    /**
     * Даёт Удвоение Монет, все перманентные множители счёта (+5, +3, +1, коллекционные)
     * и прокачивает все джетпаки/магниты/кроссовки на 6 уровень в upgrades.json
     */
    fun patchClassicUpgradesMax(upgradesJson: String, customExtraMultiplier: Int = 30): String {
        val root = JSONObject(upgradesJson)
        root.put("lastSaved", Instant.now().toString())

        val scoreMods = root.optJSONObject("scoreModifiers") ?: JSONObject().also { root.put("scoreModifiers", it) }
        val allScoreModIds = mapOf(
            "permanent_score_multiplier" to 5,
            "permanent_score_multiplier_3" to 3,
            "permanent_score_multiplier_1" to 1,
            "permanent_score_multiplier_collection_revamp_1a" to 1,
            "permanent_score_multiplier_collection_revamp_1b" to 1,
            "permanent_score_multiplier_collection_revamp_1c" to 1,
            "permanent_score_multiplier_collection_revamp_1d" to 1,
            "custom_editor_boost" to customExtraMultiplier
        )

        for ((id, value) in allScoreModIds) {
            scoreMods.put(id, JSONObject().apply {
                put("value", JSONObject().apply {
                    put("id", id)
                    put("type", 2)
                    put("subType", 3)
                    put("value", value)
                })
                put("expirationType", 0)
                put("contextType", 0)
                put("contextData", JSONObject())
            })
        }

        val pickupMods = root.optJSONObject("currencyPickupModifiers") ?: JSONObject().also { root.put("currencyPickupModifiers", it) }
        pickupMods.put("doubleCoins", JSONObject().apply {
            put("value", JSONObject().apply {
                put("id", "doubleCoins")
                put("type", 0)
                put("subType", 0)
                put("value", 2)
            })
            put("expirationType", 0)
            put("contextType", 0)
            put("contextData", JSONObject())
        })
        pickupMods.put("token_multiplier_high", JSONObject().apply {
            put("value", JSONObject().apply {
                put("id", "token_multiplier_high")
                put("type", 0)
                put("subType", 2)
                put("value", 4)
            })
            put("expirationValue", 999999999999999L)
            put("expirationType", 1)
            put("contextType", 0)
            put("contextData", JSONObject())
        })

        val powerupLevels = root.optJSONObject("powerupLevels") ?: JSONObject().also { root.put("powerupLevels", it) }
        powerupLevels.put("jetpack", 6)
        powerupLevels.put("superSneakers", 6)
        powerupLevels.put("magnet", 6)
        powerupLevels.put("doubleScore", 6)

        return root.toString(2)
    }

    /**
     * Завершает все этапы в generic_challenges.json (Daily, Marathon, Floor is Lava, Trick or Treat, Showdown)
     * и сбрасывает таймеры ожидания персонажей!
     */
    fun patchClassicChallengesComplete(challengesJson: String): String {
        val root = JSONObject(challengesJson)
        root.put("lastSaved", Instant.now().toString())
        val states = root.optJSONObject("challengeStates") ?: return challengesJson

        val keys = states.keys()
        while (keys.hasNext()) {
            val cid = keys.next()
            val ch = states.optJSONObject(cid) ?: continue

            val rewardStates = ch.optJSONArray("rewardStates")
            var maxReqScore = 500_000
            if (rewardStates != null) {
                for (i in 0 until rewardStates.length()) {
                    val rs = rewardStates.optJSONObject(i) ?: continue
                    val req = rs.optInt("RequiredScore", 0)
                    if (req > maxReqScore) maxReqScore = req
                    // Статус 2 = разблокировано и готово к сбору (10 = уже собрано)
                    if (rs.optInt("State", 0) == 0) {
                        rs.put("State", 2)
                    }
                    rs.put("AdRequired", false)
                }
            }
            ch.put("currentScore", maxReqScore)
            ch.put("highScore", maxReqScore)
            ch.put("lastSeenScore", maxReqScore)

            // Очищаем кулдауны на персонажей в ивентах
            ch.optJSONObject("requirements")?.put("cooldownItems", JSONObject())
        }
        return root.toString(2)
    }

    /**
     * Разблокирует все 40+ тиров Сезонной Охоты (season_hunt.json)
     */
    fun patchClassicSeasonHuntMax(seasonHuntJson: String): String {
        val root = JSONObject(seasonHuntJson)
        root.put("lastSaved", Instant.now().toString())
        root.put("collectedTokens", 99999)

        val rewardsArr = JSONArray()
        for (tier in 0..42) {
            rewardsArr.put(JSONObject().apply {
                if (tier > 0) put("tierIndex", tier)
                put("mainRewardStatus", JSONArray().put(1)) // 1 = готово к сбору!
            })
        }
        root.put("rewardsThisSeason", rewardsArr)
        root.put("lastUnlockedTierSeen", 42)
        return root.toString(2)
    }

    /**
     * Выполняет все достижения (achievements_2.json) на 100% (золотой/бриллиантовый тир)
     */
    fun patchClassicAchievementsAll(achievementsJson: String): String {
        val root = JSONObject(achievementsJson)
        root.put("lastSaved", Instant.now().toString())
        val entries = root.optJSONObject("achievementEntries") ?: return achievementsJson

        val keys = entries.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val ach = entries.optJSONObject(k) ?: continue
            val goals = ach.optJSONArray("goals") ?: continue
            val maxGoal = if (goals.length() > 0) goals.optInt(goals.length() - 1, 100) else 100

            ach.put("currentTier", maxOf(0, goals.length() - 1))
            ach.put("progress", maxGoal)
            ach.put("highestProgress", maxGoal)

            val claims = JSONArray()
            for (i in 0 until goals.length()) claims.put(true)
            ach.put("claimState", claims)
            ach.put("markAsViewed", true)
        }
        return root.toString(2)
    }
}
