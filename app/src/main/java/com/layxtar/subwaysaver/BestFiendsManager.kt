package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject

object BestFiendsManager {

    private val dir get() = BestFiendsDatabase.GAME_SETTINGS_DIR
    private const val METADATA_PATH =
        "/storage/emulated/0/Android/data/${BestFiendsDatabase.PKG_BEST_FIENDS}/files/il2cpp/Metadata/global-metadata.dat"

    fun unlockPermissionsForWrite(log: (String) -> Unit) {
        CryptoAndSyncManager.execSh("chattr -R -i '$dir' 2>/dev/null")
        CryptoAndSyncManager.execSh("chmod 777 '$dir' 2>/dev/null")
        CryptoAndSyncManager.execSh("chmod 666 '$dir'/*.json 2>/dev/null")
    }

    fun lockSettingsReadOnly(lock: Boolean, log: (String) -> Unit) {
        if (lock) {
            CryptoAndSyncManager.execSh("chmod 444 '$dir'/*.json 2>/dev/null")
            CryptoAndSyncManager.execSh("chmod 555 '$dir' 2>/dev/null")
            CryptoAndSyncManager.execSh("chattr +i '$dir'/*.json 2>/dev/null")
            log("🔒 Файлы GameSettings заморожены (Read-Only 444)! Игра не сможет их сбросить.")
        } else {
            unlockPermissionsForWrite(log)
            log("🔓 Защита Read-Only снята (права 777 восстановлены).")
        }
    }

    fun isSettingsLocked(): Boolean {
        val (_, out) = CryptoAndSyncManager.execSh("ls -ld '$dir' 2>/dev/null")
        return out.startsWith("dr-xr-xr-x")
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

        val hashesObj = JSONObject()
        for (k in BestFiendsDatabase.HASH_KEYS) {
            hashesObj.put(k, JSONObject.NULL)
        }
        CryptoAndSyncManager.writeFileViaShizuku(
            "$dir/CachedSettingsHashes.json",
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
        CryptoAndSyncManager.stopGameForSync(BestFiendsDatabase.PKG_BEST_FIENDS, true, log)
        unlockPermissionsForWrite(log)
        val cmd = """
            for f in '$dir'/*.bak_editor; do
                if [ -f "${'$'}f" ]; then
                    orig="${'$'}{f%.bak_editor}"
                    cp "${'$'}f" "${'$'}orig"
                fi
            done
            if [ -f '$METADATA_PATH.bak_editor' ]; then
                chmod 666 '$METADATA_PATH' 2>/dev/null
                cp '$METADATA_PATH.bak_editor' '$METADATA_PATH'
            fi
            sync
        """.trimIndent()
        CryptoAndSyncManager.execSh(cmd, log)
        log("⏪ Оригинальные конфиги и global-metadata.dat восстановлены из бэкапов!")
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

    // ==================== 1. МАГАЗИН: ВЫДАЧА ВСЕХ ДОНАТНЫХ БУКАШЕК ЗА 1 ЗОЛОТО ====================

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

    // ==================== 2. МАГАЗИН: СПИСАНИЕ (СЛИВ) ЛИШНИХ КЛЮЧЕЙ, МЕТЕОРОВ И ЗОЛОТА ====================

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

    // ==================== 3. МАГАЗИН: НАКРУТКА РЕСУРСОВ ЗА 1 ЗОЛОТО ====================

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

    // ==================== 4. СУНДУКИ (GACHA): ВСЕ ДОНАТНЫЕ ПЕРСОНАЖИ + ВСЕ СКИНЫ ====================

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

    // ==================== 5. ГЕЙМПЛЕЙНЫЕ МОДЫ (FIENDEX, БУСТЕРЫ, ДИНАМИЧЕСКАЯ СЛОЖНОСТЬ, БАТТЛ-ПАСС, КОЛЕСО, БЕЗ РЕКЛАМЫ) ====================

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

    // Авто-победа с 1-й попытки (+50 ходов со старта, ослабление слизней на 95%, снятие ворот на карте)
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

        // Динамическая сложность: сразу даёт +50 ходов и снижает ХП слизней до 5% с 0-й попытки (без проигрышей!)
        val dynAdjustments = JSONArray().put(JSONObject().apply {
            put("attemptsRequired", 0)
            put("extraMoves", 50)
            put("slugHealthMultiplier", 0.05)
            put("converterDropRateMultiplier", 10.0)
            put("bombChargeMultiplier", 10.0)
        })
        val dynConfig = JSONObject().apply {
            put("isEnabled", true)
            put("minLevel", 1)
            put("adjustments", dynAdjustments)
            put("levels", dynAdjustments)
        }
        val dynRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", dynConfig)
            put("featureId", "dynamicDifficulty")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })
        val dynSimpleRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", dynConfig)
            put("featureId", "dynamicDifficultySimpleLevels")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok1 = writeSettingJson("outOfMoves.json", oomRoot.toString(), !hotSwapMode, false, log)
        val ok2 = writeSettingJson("storyMapGates.json", gatesRoot.toString(), false, false, log)
        val ok3 = writeSettingJson("dynamicDifficulty.json", dynRoot.toString(), false, false, log)
        val ok4 = writeSettingJson("dynamicDifficultySimpleLevels.json", dynSimpleRoot.toString(), false, autoLock, log)
        if (ok1 && ok2 && ok3 && ok4) {
            log("⚔️ Авто-победа (dynamicDifficulty: +50 ходов с 1-й попытки, -95% ХП слизней) и снятие ворот установлены!")
        }
        return ok1 && ok2 && ok3 && ok4
    }

    // Джекпоты Колеса Фортуны (womfRewards.json), Боевого Пропуска (battlePassConfig.json) и Наград за уровни (levelRewards.json)
    fun installWheelBattlePassAndLevelJackpots(
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val megaJackpotSkus = JSONArray().apply {
            put(makeSku(BestFiendsDatabase.SKU_GOLD, 10000, "Gold"))
            put(makeSku(BestFiendsDatabase.SKU_KEYS, 500, "Keys"))
            put(makeSku(BestFiendsDatabase.SKU_VIP_HOURS, 168, "Vip Hours"))
            put(makeSku(BestFiendsDatabase.SKU_UNLIMITED_ENERGY_HOURS, 168, "Unlimited Energy Hours"))
        }

        // 1. Колесо Фортуны (womfRewards.json + mcettWheelFallback.json)
        val wheelSlices = JSONArray()
        for (i in 0 until 8) {
            wheelSlices.put(JSONObject().apply {
                put("sliceIndex", i)
                put("weight", 100)
                put("reward", JSONObject().put("skus", megaJackpotSkus))
            })
        }
        val womfRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().put("slices", wheelSlices).put("rewards", wheelSlices))
            put("featureId", "womfRewards")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        // 2. Боевой пропуск за 1 очко на ярус (battlePassConfig.json)
        val bpTiers = JSONArray()
        for (tier in 1..35) {
            bpTiers.put(JSONObject().apply {
                put("tier", tier)
                put("pointsRequired", 1)
                put("freeReward", JSONObject().put("skus", megaJackpotSkus))
                put("premiumReward", JSONObject().put("skus", megaJackpotSkus))
            })
        }
        val bpRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().apply {
                put("pointsPerLevel", 100)
                put("tiers", bpTiers)
            })
            put("featureId", "battlePassConfig")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        // 3. Награды за прохождение обычных и Героических уровней (levelRewards.json + heroLevelsRewards.json)
        val lvlRewardRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().apply {
                put("defaultReward", JSONObject().put("skus", megaJackpotSkus))
                put("firstTimeReward", JSONObject().put("skus", megaJackpotSkus))
            })
            put("featureId", "levelRewards")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok1 = writeSettingJson("womfRewards.json", womfRoot.toString(), !hotSwapMode, false, log)
        val ok2 = writeSettingJson("battlePassConfig.json", bpRoot.toString(), false, false, log)
        val ok3 = writeSettingJson("levelRewards.json", lvlRewardRoot.toString(), false, autoLock, log)
        if (ok1 && ok2 && ok3) {
            log("🎡 Джекпоты Колеса Фортуны (10 000🪙 + 500🔑), Боевой Пропуск (по 1 очку) и Награды за уровни установлены!")
        }
        return ok1 && ok2 && ok3
    }

    // Полное отключение рекламы (ads.json + opportunisticAdPopup.json)
    fun disableAllAdsInGame(
        hotSwapMode: Boolean,
        autoLock: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val adsRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().apply {
                put("adsEnabled", false)
                put("interstitialAdsEnabled", false)
                put("rewardedAdsEnabled", false)
                put("minLevelForAds", 99999)
                put("cooldownSeconds", 9999999)
            })
            put("featureId", "ads")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val popupRoot = JSONArray().put(JSONObject().apply {
            put("configHash", JSONObject.NULL)
            put("configuration", JSONObject().put("isEnabled", false).put("minLevel", 99999))
            put("featureId", "opportunisticAdPopup")
            put("startTimeSec", JSONObject.NULL)
            put("endTimeSec", JSONObject.NULL)
            put("segmentId", JSONObject.NULL)
        })

        val ok1 = writeSettingJson("ads.json", adsRoot.toString(), !hotSwapMode, false, log)
        val ok2 = writeSettingJson("opportunisticAdPopup.json", popupRoot.toString(), false, autoLock, log)
        if (ok1 && ok2) log("🚫 Вся реклама в Best Fiends отключена (ads.json + opportunisticAdPopup.json)!")
        return ok1 && ok2
    }

    // ==================== 6. ПАТЧЕР И СКАНЕР GLOBAL-METADATA.DAT (IL2CPP) ====================

    // Подменяет в global-metadata.dat "https://" на "http://0" (ровно 8 байт в 8 байт без смещения таблицы оффсетов!),
    // чтобы игра не могла скачать с сервера свежие хеши конфигов и перезаписать папку GameSettings!
    fun patchGlobalMetadataKillConfigSync(log: (String) -> Unit): Boolean {
        CryptoAndSyncManager.stopGameForSync(BestFiendsDatabase.PKG_BEST_FIENDS, true, log)
        CryptoAndSyncManager.execSh("chmod 666 '$METADATA_PATH' 2>/dev/null")
        val raw = CryptoAndSyncManager.readFileViaShizuku(METADATA_PATH, log) ?: run {
            log("❌ Файл global-metadata.dat не найден по пути: $METADATA_PATH")
            return false
        }

        val target = "https://".toByteArray(Charsets.US_ASCII)
        val replacement = "http://0".toByteArray(Charsets.US_ASCII) // Строго 8 байт!
        var patchedCount = 0

        var i = 0
        while (i <= raw.size - target.size) {
            var match = true
            for (j in target.indices) {
                if (raw[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                for (j in replacement.indices) {
                    raw[i + j] = replacement[j]
                }
                patchedCount++
                i += target.size
            } else {
                i++
            }
        }

        if (patchedCount == 0) {
            log("ℹ️ В global-metadata.dat уже заблокированы все https:// ссылки.")
            return true
        }

        val ok = CryptoAndSyncManager.writeFileViaShizuku(METADATA_PATH, raw, log)
        if (ok) {
            CryptoAndSyncManager.execSh("chmod 444 '$METADATA_PATH' 2>/dev/null")
            log("🛡️ global-metadata.dat пропатчен (заблокировано $patchedCount https-ссылок синхронизации хешей)!")
        }
        return ok
    }

    // Авто-сканер новых букашек внутри global-metadata.dat
    fun scanGlobalMetadataForFiendIds(log: (String) -> Unit): List<String> {
        val raw = CryptoAndSyncManager.readFileViaShizuku(METADATA_PATH, log) ?: return emptyList()
        val text = String(raw, Charsets.ISO_8859_1)
        val regex = Regex("\\b(Green|Yellow|Blue|Purple|Red)([1-9][0-9]?)\\b")
        val found = regex.findAll(text).map { it.value }.distinct().sorted().toList()
        log("🔍 В global-metadata.dat найдено ${found.size} уникальных ID букашек: ${found.joinToString(", ")}")
        return found
    }
}
