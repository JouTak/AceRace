package com.joutak.acerace.zones

import com.joutak.acerace.AceRacePlugin
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.IOException
import java.io.File

object ZoneManager {
    private val templateZones = mutableMapOf<String, Zone>()
    private val arenaZones = mutableMapOf<String, MutableList<Zone>>()
    private var zonesFile = YamlConfiguration()

    fun addZone(zone: Zone) {
        if (templateZones.containsKey(zone.name)) {
            throw IllegalArgumentException("Зона с именем '${zone.name}' уже существует!")
        }
        templateZones[zone.name] = zone
        saveZones()
    }

    fun getZone(name: String): Zone {
        return templateZones[name] ?: throw IllegalArgumentException("Зона '$name' не найдена!")
    }

    fun getZones(): Map<String, Zone> {
        return templateZones
    }

    fun removeZone(name: String) {
        if (!templateZones.containsKey(name)) {
            throw IllegalArgumentException("Зона '$name' не найдена!")
        }
        templateZones.remove(name)
        saveZones()
    }

    fun clearZones() {
        templateZones.clear()
        arenaZones.clear()
        saveZones()
    }

    fun loadZones() {
        val fx = File(AceRacePlugin.instance.dataFolder, "zones.yml")
        if (!fx.exists()) {
            AceRacePlugin.instance.saveResource("zones.yml", true)
        }

        zonesFile = YamlConfiguration.loadConfiguration(fx)

        val worldsSection = zonesFile.getConfigurationSection("worlds")
        if (worldsSection == null) {
            return
        }

        templateZones.clear()

        for (worldName in worldsSection.getKeys(false)) {
            val zonesList = zonesFile.getMapList("worlds.$worldName.zones")

            for (value in zonesList) {
                try {
                    val normalizedValue = value.entries.associate { (key, entryValue) ->
                        key.toString() to (entryValue ?: throw IllegalArgumentException("Пустое значение в zones.yml"))
                    }
                    val finalValue = normalizedValue.toMutableMap().apply {
                        if (!containsKey("worldName")) {
                            put("worldName", worldName)
                        }
                    }
                    val zone = Zone.deserialize(finalValue)
                    if (zone != null) {
                        templateZones[zone.name] = zone
                    }
                } catch (e: Exception) {
                    AceRacePlugin.instance.logger.severe("Ошибка при загрузке зоны из мира $worldName: ${e.message}")
                }
            }
        }

        AceRacePlugin.instance.logger.info("Загружено ${templateZones.size} шаблонных зон")
    }


    fun saveZones() {
        val fx = File(AceRacePlugin.instance.dataFolder, "zones.yml")

        val worlds = templateZones.values.groupBy { it.worldName }
        worlds.forEach { (worldName, zones) ->
            zonesFile.set("worlds.$worldName.zones", zones.map {it.serialize()})
        }

        try {
            zonesFile.save(fx)
        } catch (e: IOException) {
            AceRacePlugin.instance.logger.severe("Ошибка при сохранении зон: ${e.message}")
        }
    }

    fun loadZonesForArena(arenaWorldName: String) {
        val world = Bukkit.getWorld(arenaWorldName)
        if (world == null) {
            AceRacePlugin.instance.logger.warning("Мир $arenaWorldName не найден!")
            return
        }

        val clonedZones = templateZones.values.map { templateZone ->
            templateZone.clone(arenaWorldName)
        }.toMutableList()

        arenaZones[arenaWorldName] = clonedZones

        val barrierCount = clonedZones.filterIsInstance<ZoneBarrier>().size
        AceRacePlugin.instance.logger.info("Клонировано ${clonedZones.size} зон для арены $arenaWorldName (BARRIER: $barrierCount)")
    }

    fun getZonesForArena(arenaWorldName: String): List<Zone> {
        return arenaZones[arenaWorldName] ?: emptyList()
    }

    fun clearZonesForArena(arenaWorldName: String) {
        arenaZones.remove(arenaWorldName)
    }

    fun checkPlayerZones(player: Player) {
        val worldName = player.world.name
        val zonesForWorld = arenaZones[worldName] ?: return

        for (zone in zonesForWorld) {
            if (zone.isInside(player.location)) {
                zone.execute(player)
            }
        }
    }

    fun getAllWorlds(): List<String> {
        zonesFile = YamlConfiguration.loadConfiguration(File(AceRacePlugin.instance.dataFolder, "zones.yml"))
        val worldsSection = zonesFile.getConfigurationSection("worlds") ?: return emptyList()
        return worldsSection.getKeys(false).toList()
    }

}
