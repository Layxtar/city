                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                                items(filteredBoards, key = { it.first }) { (id, pair) ->
                                    val (name, upgrades) = pair
                                    val isUnlocked = ClassicCryptoAndSyncManager.isBoardUnlocked(st, id)
                                    val ownedUpgrades = ClassicCryptoAndSyncManager.getOwnedUpgradesForBoard(st, id)
                                    val isExpanded = expandedBoardId == id || (boardSearchQuery.isNotBlank() && upgrades.any { it.contains(boardSearchQuery, true) })

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        if (isUnlocked) "✅ ОТКРЫТА (Способностей: ${ownedUpgrades.size}/${upgrades.size})" else "🔒 ЗАКРЫТА",
                                                        color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Switch(checked = isUnlocked, onCheckedChange = { checked ->
                                                        pushUndo()
                                                        ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, checked, upgrades)
                                                        uiTrigger++
                                                    })
                                                    Button(
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        onClick = {
                                                            pushUndo()
                                                            ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, true, upgrades)
                                                            ClassicCryptoAndSyncManager.selectBoard(st, id)
                                                            uiTrigger++
                                                            log("🛹 Выбрана доска: $name")
                                                        }
                                                    ) { Text(if (currentSelectedBoard == id) "⭐ Взята" else "Выбрать", fontSize = 11.sp) }
                                                }
                                            }

                                            if (upgrades.isNotEmpty()) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedBoardId = if (expandedBoardId == id) null else id }
                                                ) {
                                                    Text(if (isExpanded) "🔼 Скрыть способности" else "🔽 Выбрать способности (${upgrades.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExpanded) {
                                                    upgrades.forEach { upgId ->
                                                        val hasUpg = ownedUpgrades.contains(upgId)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Checkbox(
                                                                checked = hasUpg,
                                                                onCheckedChange = { checked ->
                                                                    pushUndo()
                                                                    ClassicCryptoAndSyncManager.toggleBoardUpgrade(st, id, upgId, checked)
                                                                    uiTrigger++
                                                                }
                                                            )
                                                            Text("Способность: $upgId", fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
