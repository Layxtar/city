package com.layxtar.subwaysaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ActiveGame { CITY, CLASSIC, PVZ2, BEST_FIENDS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CryptoAndSyncManager.registerPermissionListener()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MultiGameEditorRoot(this)
                }
            }
        }
    }

    override fun onDestroy() {
        CryptoAndSyncManager.unregisterPermissionListener()
        super.onDestroy()
    }
}

@Composable
fun MultiGameEditorRoot(activity: ComponentActivity) {
    var activeGame by remember { mutableStateOf(ActiveGame.CITY) }
    var statusText by remember { mutableStateOf("🔄 Готов к работе...") }
    var logs by remember { mutableStateOf("🚀 Старт модульного редактора...\n") }

    fun log(msg: String) {
        statusText = msg
        logs = "$msg\n$logs"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = Color(0xFF161616), tonalElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Ultimate", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(end = 4.dp))

                Button(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = { activeGame = ActiveGame.CITY },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeGame == ActiveGame.CITY) Color(0xFF1976D2) else Color.DarkGray
                    )
                ) { Text("🏙️ CITY", fontSize = 11.sp) }

                Button(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = { activeGame = ActiveGame.CLASSIC },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeGame == ActiveGame.CLASSIC) Color(0xFFE65100) else Color.DarkGray
                    )
                ) { Text("🛹 CLASSIC", fontSize = 11.sp) }

                Button(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = { activeGame = ActiveGame.PVZ2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeGame == ActiveGame.PVZ2) Color(0xFF2E7D32) else Color.DarkGray
                    )
                ) { Text("🌻 PVZ 2", fontSize = 11.sp) }

                Button(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = { activeGame = ActiveGame.BEST_FIENDS },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeGame == ActiveGame.BEST_FIENDS) Color(0xFF8E24AA) else Color.DarkGray
                    )
                ) { Text("🐛 FIENDS", fontSize = 11.sp) }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeGame) {
                ActiveGame.CITY -> CityScreen(activity, statusText, logs, ::log)
                ActiveGame.CLASSIC -> ClassicScreen(activity, statusText, logs, ::log)
                ActiveGame.PVZ2 -> Pvz2Screen(activity, statusText, logs, ::log)
                ActiveGame.BEST_FIENDS -> BestFiendsScreen(activity, statusText, logs, ::log)
            }
        }
    }
}