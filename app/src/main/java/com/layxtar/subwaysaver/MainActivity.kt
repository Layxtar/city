package com.cityedit.ultimate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CityEditTheme {
                MainScreen(
                    onRequestPermission = {
                        ShizukuShellManager.requestPermission()
                    }
                )
            }
        }
    }
}

@Composable
fun CityEditTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00E676),
            secondary = Color(0xFF7C4DFF),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.Black,
            onSurface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onRequestPermission: () -> Unit) {
    var selectedEdition by remember { mutableStateOf(GameEdition.CLASSIC) }
    var statusText by remember { mutableStateOf("Готов к работе. Проверьте статус Shizuku.") }
    var searchQuery by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CityEdit Ultimate: Subway Surfers", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Переключатель версий игры
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Выбор версии игры:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { selectedEdition = GameEdition.CLASSIC },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedEdition == GameEdition.CLASSIC) MaterialTheme.colorScheme.primary else Color.DarkGray
                                )
                            ) {
                                Text("Classic", color = if (selectedEdition == GameEdition.CLASSIC) Color.Black else Color.White)
                            }
                            Button(
                                onClick = { selectedEdition = GameEdition.CITY },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedEdition == GameEdition.CITY) MaterialTheme.colorScheme.primary else Color.DarkGray
                                )
                            ) {
                                Text("City", color = if (selectedEdition == GameEdition.CITY) Color.Black else Color.White)
                            }
                        }
                    }
                }
            }

            // Статус и Shizuku
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val isShizukuOn = ShizukuShellManager.isShizukuAvailable()
                        val hasPerm = ShizukuShellManager.hasPermission()
                        Text("Shizuku Статус: ${if (isShizukuOn) "Активен" else "Не найден"}", color = if (isShizukuOn) Color.Green else Color.Red)
                        Text(
    text = "Права ROOT/ADB: ${if (hasPerm) "Выданы" else "Требуются"}",
    color = if (hasPerm) Color.Green else Color.Yellow
)

                        
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!hasPerm) {
                            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                                Text("Запросить права Shizuku")
                            }
                        }
                    }
                }
            }

            // Быстрые действия для Classic / City
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Управление и Читы (${selectedEdition.title}):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedEdition == GameEdition.CLASSIC) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isWorking = true
                                        statusText = "Патчим Classic сохранения..."
                                        val success = performClassicMaxPatch()
                                        statusText = if (success) "Classic успешно пропатчен! Все перки, валюты и сёрферы разблокированы." else "Ошибка при патче Classic."
                                        isWorking = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Максимальный взлом Classic (Валюта + Сёрферы + Доски + Ачивки)", color = Color.Black)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isWorking = true
                                        statusText = "Открываем триалы и копилку в Subway Surfers City..."
                                        val success = performCityMaxPatch()
                                        statusText = if (success) "City успешно пропатчен! Все триалы и копилка открыты." else "Ошибка при патче City."
                                        isWorking = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Открыть все Триалы и Свинью-копилку (City)", color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        ShizukuShellManager.launchGame(selectedEdition)
                                        statusText = "Запуск ${selectedEdition.title}..."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Запустить игру")
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        ShizukuShellManager.forceStopGame(selectedEdition)
                                        statusText = "Игра ${selectedEdition.title} остановлена."
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Stop", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Поиск по персонажам и доскам
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Поиск по базе персонажей и досок:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Введите имя (например: Jake, Bat Luck, Spike)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Фильтрация списка персонажей
            val filteredChars = GameContentDatabase.classicCharacters.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
            }

            items(filteredChars) { character ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(character.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("ID: ${character.id} | Категория: ${character.category}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isWorking = true
                                    statusText = "Добавляем персонажа ${character.displayName}..."
                                    val success = addSingleCharacterToSave(character)
                                    statusText = if (success) "Персонаж ${character.displayName} добавлен!" else "Ошибка добавления."
                                    isWorking = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Открыть", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Лог статуса
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Box(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text(
                            text = if (isWorking) "⏳ Выполняется операция..." else statusText,
                            color = Color(0xFF00E676),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ МОДИФИКАЦИИ СОХРАНЕНИЙ ЧЕРЕЗ SHIZUKU
// =============================================================================

suspend fun performClassicMaxPatch(): Boolean = withContext(Dispatchers.IO) {
    val dir = ShizukuShellManager.resolveActiveSaveDir(GameEdition.CLASSIC)
    val files = ShizukuShellManager.listSaveFiles(dir)
    if (files.isEmpty()) return@withContext false

    var success = true

    // 1. Wallet (монеты, ключи, скейты)
    val walletFile = files.find { it.contains("wallet", ignoreCase = true) }
    if (walletFile != null) {
        val bytes = ShizukuShellManager.readFileBytes("$dir/$walletFile")
        if (bytes != null) {
            val json = CryptoEngine.decryptClassic(bytes)
            val modified = GameContentDatabase.patchClassicWallet(json, 999_999_999, 999_999, 999_999, 99_999, 99_999, 999_999, 99_999, 10)
            val enc = CryptoEngine.encryptClassic(modified)
            if (!ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, "$dir/$walletFile", enc)) success = false
        }
    }

    // 2. Characters Inventory
    val charsFile = files.find { it.contains("characters_inventory", ignoreCase = true) }
    if (charsFile != null) {
        val bytes = ShizukuShellManager.readFileBytes("$dir/$charsFile")
        if (bytes != null) {
            val json = CryptoEngine.decryptClassic(bytes)
            val modified = GameContentDatabase.patchClassicCharacters(json)
            val enc = CryptoEngine.encryptClassic(modified)
            if (!ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, "$dir/$charsFile", enc)) success = false
        }
    }

    // 3. Boards Inventory
    val boardsFile = files.find { it.contains("boards_inventory", ignoreCase = true) }
    if (boardsFile != null) {
        val bytes = ShizukuShellManager.readFileBytes("$dir/$boardsFile")
        if (bytes != null) {
            val json = CryptoEngine.decryptClassic(bytes)
            val modified = GameContentDatabase.patchClassicBoards(json)
            val enc = CryptoEngine.encryptClassic(modified)
            if (!ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, "$dir/$boardsFile", enc)) success = false
        }
    }

    // 4. Upgrades & Score Multipliers
    val upgFile = files.find { it.contains("upgrades", ignoreCase = true) }
    if (upgFile != null) {
        val bytes = ShizukuShellManager.readFileBytes("$dir/$upgFile")
        if (bytes != null) {
            val json = CryptoEngine.decryptClassic(bytes)
            val modified = GameContentDatabase.patchClassicUpgradesMax(json, 50)
            val enc = CryptoEngine.encryptClassic(modified)
            if (!ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, "$dir/$upgFile", enc)) success = false
        }
    }

    // 5. Challenges (ивенты, ежедневные задания)
    val chalFile = files.find { it.contains("generic_challenges", ignoreCase = true) }
    if (chalFile != null) {
        val bytes = ShizukuShellManager.readFileBytes("$dir/$chalFile")
        if (bytes != null) {
            val json = CryptoEngine.decryptClassic(bytes)
            val modified = GameContentDatabase.patchClassicChallengesComplete(json)
            val enc = CryptoEngine.encryptClassic(modified)
            if (!ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, "$dir/$chalFile", enc)) success = false
        }
    }

    // 6. Season Hunt
    val huntFile = files.find { it.contains("season_hunt", ignoreCase = true) }
    if (huntFile != null) {
        val bytes = ShizukuShellManager.readFileBytes("$dir/$huntFile")
        if (bytes != null) {
            val json = CryptoEngine.decryptClassic(bytes)
            val modified = GameContentDatabase.patchClassicSeasonHuntMax(json)
            val enc = CryptoEngine.encryptClassic(modified)
            if (!ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, "$dir/$huntFile", enc)) success = false
        }
    }

    // 7. Achievements
    val achFile = files.find { it.contains("achievements", ignoreCase = true) }
    if (achFile != null) {
        val bytes = ShizukuShellManager.readFileBytes("$dir/$achFile")
        if (bytes != null) {
            val json = CryptoEngine.decryptClassic(bytes)
            val modified = GameContentDatabase.patchClassicAchievementsAll(json)
            val enc = CryptoEngine.encryptClassic(modified)
            if (!ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, "$dir/$achFile", enc)) success = false
        }
    }

    return@withContext success
}

suspend fun performCityMaxPatch(): Boolean = withContext(Dispatchers.IO) {
    val dir = ShizukuShellManager.resolveActiveSaveDir(GameEdition.CITY)
    val files = ShizukuShellManager.listSaveFiles(dir)
    val saveFile = files.find { it.contains("game", ignoreCase = true) || it.endsWith(".sav") || it.endsWith(".json") }
        ?: files.firstOrNull() ?: "game.sav"

    val remotePath = "$dir/$saveFile"
    val bytes = ShizukuShellManager.readFileBytes(remotePath) ?: return@withContext false

    val json = CryptoEngine.decryptCity(bytes)
    val modified = GameContentDatabase.patchCityMaxEverything(json)
    val enc = CryptoEngine.encryptCity(modified)

    return@withContext ShizukuShellManager.writeFileBytes(GameEdition.CITY, remotePath, enc)
}

suspend fun addSingleCharacterToSave(character: CharacterCatalogEntry): Boolean = withContext(Dispatchers.IO) {
    val dir = ShizukuShellManager.resolveActiveSaveDir(GameEdition.CLASSIC)
    val files = ShizukuShellManager.listSaveFiles(dir)
    val charsFile = files.find { it.contains("characters_inventory", ignoreCase = true) } ?: return@withContext false

    val remotePath = "$dir/$charsFile"
    val bytes = ShizukuShellManager.readFileBytes(remotePath) ?: return@withContext false

    val json = CryptoEngine.decryptClassic(bytes)
    val modified = GameContentDatabase.patchClassicCharacters(json, listOf(character))
    val enc = CryptoEngine.encryptClassic(modified)

    return@withContext ShizukuShellManager.writeFileBytes(GameEdition.CLASSIC, remotePath, enc)
}
