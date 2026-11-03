package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject

object BestFiendsManager {

    private val dir get() = BestFiendsDatabase.GAME_SETTINGS_DIR
    private const val METADATA_PATH =
        "/storage/emulated/0/Android/data/${BestFiendsDatabase.PKG_BEST_FIENDS}/files/il2cpp/Metadata/global-metadata.dat"

    // Полное снятие всех ограничений с папки GameSettings и global-metadata.dat
    fun unlockPermissionsForWrite(log: (String) -> Unit) {
        CryptoAndSyncManager.execSh("chattr -R -i '$dir' 2>/dev/null")
        CryptoAndSyncManager.execSh("chmod 777 '$dir' 2>/dev/null")
        CryptoAndSyncManager.execSh("chmod 666 '$dir'/*.json 2>/dev/null")
    }

    // Безопасный Read-Only: НИКОГДА не ставим chmod 555 на саму папку GameSettings и не блокируем CachedSettingsHashes.json,
    // иначе C#-движок при старте игры падает с UnauthorizedAccessException и зависает на экране загрузки!
    fun lockSettingsReadOnly(lock: Boolean, log: (String) -> Unit) {
        if (lock) {
            CryptoAndSyncManager.execSh("chmod 777 '$dir' 2>/dev/null")
            val moddedFiles = listOf("shops.json", "gachaLevelsFiends.json", "fiendex.json", "powerUpShop.json", "outOfMoves.json", "storyMapGates.json")
            for (f in moddedFiles) {
                CryptoAndSyncManager.execSh("chmod 444 '$dir/$f' 2>/dev/null")
            }
            CryptoAndSyncManager.execSh("chmod 666 '$dir/CachedSettingsHashes.json' 2>/dev/null")
            log("🔒 Мод-файлы защищены (444), а папка и хеши оставлены открытыми (777/666), чтобы игра не зависала при загрузке!")
        } else {
            unlockPermissionsForWrite(log)
            log("🔓 Все права записи (777/666) восстановлены.")
        }
    }

    fun isSettingsLocked(): Boolean {
        val (_, out) = CryptoAndSyncManager.execSh("ls -l '$dir/shops.json' 2>/dev/null")
        return out.startsWith("-r--r--r--")
    }

    // ==================== ЭКСТРЕННАЯ ПОЧИНКА ЗАВИСАНИЯ НА ЗАГРУЗКЕ BEST FIENDS ====================
    fun fixInfiniteLoadingFreeze(log: (String) -> Unit): Boolean {
        CryptoAndSyncManager.stopGameForSync(BestFiendsDatabase.PKG_BEST_FIENDS, true, log)
        log("⏳ 1/4: Снимаем блокировку chmod 555/444 и chattr +i со всех папок Best Fiends...")
        unlockPermissionsForWrite(log)
        CryptoAndSyncManager.execSh("chattr -i '$METADATA_PATH' 2>/dev/null; chmod 666 '$METADATA_PATH' 2>/dev/null")

        log("⏳ 2/4: Восстанавливаем оригинальный global-metadata.dat...")
        val (_, metaBakOut) = CryptoAndSyncManager.execSh(
            "if [ -f '$METADATA_PATH.bak_editor' ]; then cp '$METADATA_PATH.bak_editor' '$METADATA_PATH' && echo 'META_RESTORED'; fi"
        )
        if (!metaBakOut.contains("META_RESTORED")) {
            // Если бэкапа не было, возвращаем все "http://0" обратно в "https://" прямо внутри файла!
            val raw = CryptoAndSyncManager.readFileViaShizuku(METADATA_PATH) {}
            if (raw != null) {
                val broken = "http://0".toByteArray(Charsets.US_ASCII)
                val orig = "https://".toByteArray(Charsets.US_ASCII)
                var fixed = 0
                var i = 0
                while (i <= raw.size - broken.size) {
                    var match = true
                    for (j in broken.indices) {
                        if (raw[i + j] != broken[j]) { match = false; break }
                    }
                    if (match) {
                        for (j in orig.indices) raw[i + j] = orig[j]
                        fixed++
                        i += broken.size
                    } else i++
                }
                if (fixed > 0) {
                    CryptoAndSyncManager.writeFileViaShizuku(METADATA_PATH, raw, log)
                    log("✅ В global-metadata.dat восстановлено $fixed оригинальных https:// ссылок!")
                }
            }
        }

        log("⏳ 3/4: Очищаем конфликтующие конфиги и восстанавливаем оригинальный CachedSettingsHashes.json...")
        val removeSyntheticCmd = """
            for f in dynamicDifficulty.json dynamicDifficultySimpleLevels.json womfRewards.json battlePassConfig.json levelRewards.json ads.json opportunisticAdPopup.json CachedSettingsHashes.json; do
                if [ -f '$dir/'${'$'}f'.bak_editor' ]; then
                    cp '$dir/'${'$'}f'.bak_editor' '$dir/'${'$'}f
                else
                    rm -f '$dir/'${'$'}f 2>/dev/null
                fi
            done
            rm -f '$dir'/*.tmp '$dir'/*.temp 2>/dev/null
            chmod 777 '$dir' 2>/dev/null
            chmod 666 '$dir'/*.json 2>/dev/null
            sync
        """.trimIndent()
        CryptoAndSyncManager.execSh(removeSyntheticCmd, log)

        log("✅ 4/4: Зависание Best Fiends устранено! Теперь нажми «🚀 Запустить Best Fiends».")
        return true
    }

    private fun writeSettingJson(
        fileName: String,
        content: String,
        stopGame: Boolean,
        autoLockAfterWrite: Boolean,
        log: (String) -> Unit
    ): Boolean {
        if (stopGame) {
            CryptoAndSyncManager.stopGameForSync(BestFiendsDatabase.PKG_BEST_FIENDS, true, log)
        }
        unlockPermissionsForWrite(log)

        // Аккуратно обнуляем только хеш изменяемого файла внутри существующего CachedSettingsHashes.json (не ломая его структуру!)
        val hashesPath = "$dir/CachedSettingsHashes.json"
        val existingHashesBytes = CryptoAndSyncManager.readFileViaShizuku(hashesPath) {}
        val hashesObj = runCatching {
            if (existingHashesBytes != null) JSONObject(String(existingHashesBytes, Charsets.UTF_8)) else JSONObject()
        }.getOrDefault(JSONObject())

        val featureKey = fileName.removeSuffix(".json")
        hashesObj.put(featureKey, JSONObject.NULL)
        CryptoAndSyncManager.writeFileViaShizuku(
            hashesPath,
            hashesObj.toString().toByteArray(Charsets.UTF_8),
            log
        )

        val ok = CryptoAndSyncManager.writeFileViaShizuku(
            "$dir/$fileName",
            content.toByteArray(Charsets.UTF_8),
            log
        )
        if (autoLockAfterWrite) {
            lockSettingsReadOnly(true, log)
        }
        return ok
    }

    fun restoreAllBackups(log: (String) -> Unit) {
        fixInfiniteLoadingFreeze(log)
        val cmd = """
            for f in '$dir'/*.bak_editor; do
                if [ -f "${'$'}f" ]; then
                    orig="${'$'}{f%.bak_editor}"
                    cp "${'$'}f" "${'$'}orig"
                fi
            done
            chmod 666 '$dir'/*.json 2>/dev/null
            sync
        """.trimIndent()
        CryptoAndSyncManager.execSh(cmd, log)
        log("⏪ Все оригинальные конфиги Best Fiends полностью восстановлены из бэкапов!")
    }

    private fun makeSku(skuId: Int, amount: Int, debugName: String): JSONObject {
        return JSONObject().apply {
            put("skuId", skuId)
            put("skuData", JSONObject().put("amount", amount))
            put("_skuNameDebug", debugName)
        }
    }

    private fun makeFiendSku(fiendName: String, styleId: Int = 0): JSONObject {
        return JSONObject().apply {
            put("skuId", BestFiendsDatabase.SKU_FIEND_OR_STYLE)
            put("skuData", JSONObject().put("fiendTypeUniqueName", fiendName).put("styleId", styleId))
        }
    }

    private fun makeOffer(offerName: String, costSkus: JSONArray, gainSkus: JSONArray): JSONObject {
        return JSONObject().apply {
            put("offerName", offerName)
            put("cost", JSONObject().put("skus", costSkus))
            put("gain", JSONObject().put("skus", gainSkus))
        }
    }

    fun installDonateFiendsShopFor1Gold(
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val oneGoldCost = JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, 1, "Gold"))

        fun fiendArray(names: List<String>, styleId: Int = 0): JSONArray {
            val arr = JSONArray()
            names.forEach { arr.put(makeFiendSku(it, styleId)) }
            return arr
        }

        val energyOffers = JSONArray().apply {
            put(makeOffer("RefillEnergy", oneGoldCost, fiendArray(listOf("Green4"))))
            put(makeOffer("UnlimitedEnergy", oneGoldCost, fiendArray(listOf("Yellow7"))))
            put(makeOffer("IncreaseCapEnergy", oneGoldCost, fiendArray(listOf("Blue4"))))
        }

        val keyOffers = JSONArray().apply {
            put(makeOffer("SmallKeyPack", oneGoldCost, fiendArray(BestFiendsDatabase.DONATE_GREEN_FIENDS)))
            put(makeOffer("MediumKeyPack", oneGoldCost, fiendArray(BestFiendsDatabase.DONATE_YELLOW_FIENDS)))
            put(makeOffer("LargeKeyPack", oneGoldCost, fiendArray(BestFiendsDatabase.DONATE_BLUE_FIENDS)))
            put(makeOffer("ExtraLargeKeyPack", oneGoldCost, fiendArray(BestFiendsDatabase.DONATE_PURPLE_FIENDS)))
            put(makeOffer("GachaKeyPack", oneGoldCost, fiendArray(BestFiendsDatabase.DONATE_RED_FIENDS)))
        }

        val vipOffers = JSONArray().apply {
            put(makeOffer("OneDayOffer", oneGoldCost, fiendArray(listOf("Purple7"))))
            put(makeOffer("TwoDaysOffer", oneGoldCost, fiendArray(listOf("Red5"))))
            put(makeOffer("ThreeDaysOffer", oneGoldCost, fiendArray(listOf("Red6"))))
        }

        val config = JSONObject().apply {
            put("energyShop", JSONObject().put("offers", energyOffers))
            put("keyShop", JSONObject().apply {
                put("keysAmountForAds", JSONObject().put("skus", fiendArray(listOf("Green6"))))
                put("offers", keyOffers)
            })
            put("vipShop", JSONObject().apply {
                put("offers", vipOffers)
                put("vipTimeFallbackReward", JSONObject().put("skus", oneGoldCost))
                put("unlimitedEnergyFallbackReward", JSONObject().put("skus", oneGoldCost))
            })
        }

        val root = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", config)
            put("featureId", "shops")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok = writeSettingJson("shops.json", root.toString(), !hotSwapMode, autoLock, log)
        if (ok) log("🐛 Магазин донатных букашек за 1 Золото установлен!")
        return ok
    }

    fun installCurrencyDrainerShop(
        drainKeysAmount: Int,
        drainYellowMitesAmount: Int,
        drainBlueMitesAmount: Int,
        drainGoldAmount: Int,
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val oneKeyGain = JSONArray().put(makeSku(BestFiendsDatabase.SKU_KEYS, 1, "Keys"))
        val oneGoldGain = JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, 1, "Gold"))
        val oneEnergyGain = JSONArray().put(makeSku(BestFiendsDatabase.SKU_REFILL_ENERGY, 1, "Refill Energy"))
        val oneVipGain = JSONArray().put(makeSku(BestFiendsDatabase.SKU_VIP_HOURS, 1, "Vip Hours"))

        val costYellow = JSONArray().put(makeSku(BestFiendsDatabase.SKU_YELLOW_MITES, drainYellowMitesAmount, "Yellow Mites"))
        val costBlue = JSONArray().put(makeSku(BestFiendsDatabase.SKU_BLUE_MITES, drainBlueMitesAmount, "Blue Mites"))
        val costKeys = JSONArray().put(makeSku(BestFiendsDatabase.SKU_KEYS, drainKeysAmount, "Keys"))
        val costGold = JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, drainGoldAmount, "Gold"))
        val costHalfYellow = JSONArray().put(makeSku(BestFiendsDatabase.SKU_YELLOW_MITES, (drainYellowMitesAmount / 2).coerceAtLeast(1), "Yellow Mites"))

        val energyOffers = JSONArray().apply {
            put(makeOffer("RefillEnergy", costYellow, oneEnergyGain))
            put(makeOffer("UnlimitedEnergy", costBlue, oneEnergyGain))
            put(makeOffer("IncreaseCapEnergy", costKeys, oneEnergyGain))
        }

        val keyOffers = JSONArray().apply {
            put(makeOffer("SmallKeyPack", costYellow, oneKeyGain))
            put(makeOffer("MediumKeyPack", costBlue, oneKeyGain))
            put(makeOffer("LargeKeyPack", costKeys, oneKeyGain))
            put(makeOffer("ExtraLargeKeyPack", costGold, oneKeyGain))
            put(makeOffer("GachaKeyPack", costHalfYellow, oneKeyGain))
        }

        val vipOffers = JSONArray().apply {
            put(makeOffer("OneDayOffer", costYellow, oneVipGain))
            put(makeOffer("TwoDaysOffer", costBlue, oneVipGain))
            put(makeOffer("ThreeDaysOffer", costKeys, oneVipGain))
        }

        val config = JSONObject().apply {
            put("energyShop", JSONObject().put("offers", energyOffers))
            put("keyShop", JSONObject().apply {
                put("keysAmountForAds", JSONObject().put("skus", oneKeyGain))
                put("offers", keyOffers)
            })
            put("vipShop", JSONObject().apply {
                put("offers", vipOffers)
                put("vipTimeFallbackReward", JSONObject().put("skus", oneGoldGain))
                put("unlimitedEnergyFallbackReward", JSONObject().put("skus", oneGoldGain))
            })
        }

        val root = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", config)
            put("featureId", "shops")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok = writeSettingJson("shops.json", root.toString(), !hotSwapMode, autoLock, log)
        if (ok) {
            log("🗑️ Магазин списания установлен! Пак 1 = -$drainYellowMitesAmount Жёлтых, Пак 2 = -$drainBlueMitesAmount Синих, Пак 3 = -$drainKeysAmount Ключей, Пак 4 = -$drainGoldAmount Золота!")
        }
        return ok
    }

    fun installMegaResourcesShop(
        goldGain: Int,
        keysGain: Int,
        yellowGain: Int,
        blueGain: Int,
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val zeroCost = JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, 0, "Gold"))
        val oneGoldCost = JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, 1, "Gold"))

        val energyOffers = JSONArray().apply {
            put(makeOffer("RefillEnergy", zeroCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, goldGain, "Gold"))))
            put(makeOffer("UnlimitedEnergy", zeroCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_UNLIMITED_ENERGY_HOURS, 720, "Unlimited Energy Hours"))))
            put(makeOffer("IncreaseCapEnergy", zeroCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_INCREASE_CAP_ENERGY, 50, "Increase Cap Energy"))))
        }

        val keyOffers = JSONArray().apply {
            put(makeOffer("SmallKeyPack", oneGoldCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, goldGain, "Gold"))))
            put(makeOffer("MediumKeyPack", oneGoldCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_KEYS, keysGain, "Keys"))))
            put(makeOffer("LargeKeyPack", oneGoldCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_YELLOW_MITES, yellowGain, "Yellow Mites"))))
            put(makeOffer("ExtraLargeKeyPack", oneGoldCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_BLUE_MITES, blueGain, "Blue Mites"))))
            put(makeOffer("GachaKeyPack", zeroCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_KEYS, keysGain, "Keys"))))
        }

        val vipOffers = JSONArray().apply {
            put(makeOffer("OneDayOffer", zeroCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_VIP_HOURS, 720, "Vip Hours"))))
            put(makeOffer("TwoDaysOffer", zeroCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_YELLOW_MITES, yellowGain, "Yellow Mites"))))
            put(makeOffer("ThreeDaysOffer", zeroCost, JSONArray().put(makeSku(BestFiendsDatabase.SKU_BLUE_MITES, blueGain, "Blue Mites"))))
        }

        val config = JSONObject().apply {
            put("energyShop", JSONObject().put("offers", energyOffers))
            put("keyShop", JSONObject().apply {
                put("keysAmountForAds", JSONObject().put("skus", JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, goldGain, "Gold"))))
                put("offers", keyOffers)
            })
            put("vipShop", JSONObject().apply {
                put("offers", vipOffers)
                put("vipTimeFallbackReward", JSONObject().put("skus", JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, goldGain, "Gold"))))
                put("unlimitedEnergyFallbackReward", JSONObject().put("skus", JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, goldGain, "Gold"))))
            })
        }

        val root = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", config)
            put("featureId", "shops")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok = writeSettingJson("shops.json", root.toString(), !hotSwapMode, autoLock, log)
        if (ok) log("💎 Мега-Магазин ресурсов установлен!")
        return ok
    }

    fun installAllFiendsAndSkinsGacha(
        maxStyleId: Int = 9,
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val levelsRangeConfigs = JSONArray()

        fun addSingleGachaBlock(fiendName: String, styleId: Int) {
            levelsRangeConfigs.put(JSONObject().apply {
                put("levelsRange", JSONObject().put("fromLevel", 1))
                put("probability", 1.0)
                put("fiendRewards", JSONArray().put(makeFiendSku(fiendName, styleId)))
            })
        }

        for (donateFiend in BestFiendsDatabase.ALL_DONATE_FIENDS) {
            addSingleGachaBlock(donateFiend, 0)
        }
        for (stdFiend in BestFiendsDatabase.STANDARD_FIENDS) {
            addSingleGachaBlock(stdFiend, 0)
        }
        for (style in 1..maxStyleId) {
            for (color in BestFiendsDatabase.COLORS) {
                for (num in 1..12) {
                    addSingleGachaBlock("$color$num", style)
                }
            }
        }

        val root = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().put("levelsRangeConfigs", levelsRangeConfigs))
            put("featureId", "gachaLevelsFiends")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok = writeSettingJson("gachaLevelsFiends.json", root.toString(), !hotSwapMode, autoLock, log)
        if (ok) log("🎁 В сундуки записано ${levelsRangeConfigs.length()} отдельных блоков (все букашки + все скины 1..$maxStyleId)!")
        return ok
    }

    fun installFreeFiendexAndBoosters(
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val zeroGold = JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, 0, "Gold"))

        val fiendTypes = listOf("EpicFiend", "NewFiend", "SpecialAttackFiend", "SingleAttackFiend", "DefaultFiend")
        val fiendsCosts = JSONArray()
        for (ft in fiendTypes) {
            fiendsCosts.put(JSONObject().put("fiendType", ft).put("cost", JSONObject().put("skus", zeroGold)))
        }

        val upgradeCosts = JSONArray().apply {
            put(JSONObject().put("fiendType", "EpicFiend").put("cost", JSONObject().put("skus", zeroGold)).put("perLevelMultiplier", 0.0))
            put(JSONObject().put("fiendType", "NonEpicFiend").put("cost", JSONObject().put("skus", zeroGold)).put("perLevelMultiplier", 0.0))
        }

        val skinCosts = JSONArray().apply {
            put(JSONObject().put("fiendType", "EpicFiend").put("cost", JSONObject().put("skus", zeroGold)))
            put(JSONObject().put("fiendType", "UniqueFiend").put("cost", JSONObject().put("skus", zeroGold)))
            put(JSONObject().put("fiendType", "DefaultFiend").put("cost", JSONObject().put("skus", zeroGold)))
        }

        val fiendexRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().apply {
                put("fiends", JSONObject().put("costs", fiendsCosts))
                put("fiendUpgradeCosts", JSONObject().put("costs", upgradeCosts))
                put("fiendInstantEvolveCosts", JSONObject().put("costs", upgradeCosts).put("minPlayerGoldToShowNewFiendInstantUpgradePopup", 0))
                put("fiendSkinCosts", JSONObject().put("costs", skinCosts))
                put("fiendSalesActivation", JSONObject().put("fiendSaleActiveAfterLevel", 1).put("fiendSaleTeasedAfterLevel", 1))
                put("fiendStyleFallbackReward", JSONObject().put("skus", JSONArray().put(makeSku(BestFiendsDatabase.SKU_GOLD, 1000, "Gold"))))
            })
            put("featureId", "fiendex")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        fun boosterOffers(skuId: Int, debugName: String): JSONArray {
            val arr = JSONArray()
            repeat(3) {
                arr.put(JSONObject().apply {
                    put("powerUpGain", JSONObject().put("skus", JSONArray().put(makeSku(skuId, 999, debugName))))
                    put("costInGold", 0)
                    put("discount", 0)
                    put("purchaseWithGoldOfferOriginalCost", 0)
                })
            }
            return arr
        }

        val powerUpRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().apply {
                put("slugSprayOverFlowDamageMultiplier", 10.0)
                put("inventoryVersion", 100)
                put("allowPurchaseWithGold", true)
                put("showDiscountInMoreFormat", false)
                put("purchaseWithGold", JSONObject().apply {
                    put("rocket", boosterOffers(BestFiendsDatabase.SKU_ROCKET, "Rocket"))
                    put("cluster", boosterOffers(BestFiendsDatabase.SKU_CLUSTER, "Cluster"))
                    put("fury", boosterOffers(BestFiendsDatabase.SKU_FURY, "Fury"))
                    put("converter", boosterOffers(BestFiendsDatabase.SKU_CONVERTER, "Converter"))
                    put("slugHit", boosterOffers(BestFiendsDatabase.SKU_SLUG_SPRAY, "Slug Spray"))
                })
                put("powerUpConverter", JSONObject().put("storyProgressMin", 1).put("amountMultiplier", 10.0).put("goldMin", 100).put("goldMax", 1000))
            })
            put("featureId", "powerUpShop")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok1 = writeSettingJson("fiendex.json", fiendexRoot.toString(), !hotSwapMode, false, log)
        val ok2 = writeSettingJson("powerUpShop.json", powerUpRoot.toString(), false, autoLock, log)
        if (ok1 && ok2) log("🚀 Fiendex за 0 золота и Магазин Бустеров (по 999 шт. бесплатно) установлены!")
        return ok1 && ok2
    }

    fun installEasyWinAndRemoveGates(
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val oomSettings = JSONArray()
        for (ord in 1..4) {
            oomSettings.put(JSONObject().apply {
                put("outOfMovesOrdinal", ord)
                put("goldPrice", 0)
                put("movesAdded", 50)
            })
        }
        val oomRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().put("segmentName", "OOM_2024_12_03_Control").put("ftueMinLevel", 1).put("outOfMovesSettings", oomSettings))
            put("featureId", "outOfMoves")
            put("startTimeSec", 1733216400)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val gatesArr = JSONArray().put(JSONObject().put("level", 10).put("minFiendLevel", 1).put("minFiendAmount", 1).put("isHidden", true))
        val gatesRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().put("mapGatesConfigs", gatesArr))
            put("featureId", "storyMapGates")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok1 = writeSettingJson("outOfMoves.json", oomRoot.toString(), !hotSwapMode, false, log)
        val ok2 = writeSettingJson("storyMapGates.json", gatesRoot.toString(), false, autoLock, log)
        if (ok1 && ok2) {
            log("⚔️ Ворота на карте убраны, а продолжение даёт +50 ходов за 0 золота!")
        }
        return ok1 && ok2
    }

    fun scanGlobalMetadataForFiendIds(log: (String) -> Unit): List<String> {
        val raw = CryptoAndSyncManager.readFileViaShizuku(METADATA_PATH, log) ?: return emptyList()
        val text = String(raw, Charsets.ISO_8859_1)
        val regex = Regex("\\b(Green|Yellow|Blue|Purple|Red)([1-9][0-9]?)\\b")
        val found = regex.findAll(text).map { it.value }.distinct().sorted().toList()
        log("🔍 В global-metadata.dat найдено ${found.size} уникальных ID букашек: ${found.joinToString(", ")}")
        return found
    }
}
