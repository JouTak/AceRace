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
        val zonesList = zonesFile.getMapList("zones")

        templateZones.clear()

        for (value in zonesList) {
            try {
                val normalizedValue = value.entries.associate { (key, entryValue) ->
                    key.toString() to (entryValue ?: throw IllegalArgumentException("Пустое значение в zones.yml"))
                }
                val zone = Zone.deserialize(normalizedValue)
                if (zone != null) {
                    templateZones[zone.name] = zone
                }
            } catch (e: Exception) {
                AceRacePlugin.instance.logger.severe("Ошибка при загрузке зон: ${e.message}")
                break
            }
        }
    }


    fun saveZones() {
        val fx = File(AceRacePlugin.instance.dataFolder, "zones.yml")
        zonesFile.set("zones", templateZones.values.map { it.serialize() })
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
            when (templateZone) {
                is ZoneBarrier -> ZoneBarrier(
                    name = templateZone.name,
                    x1 = templateZone.x1,
                    y1 = templateZone.y1,
                    z1 = templateZone.z1,
                    x2 = templateZone.x2,
                    y2 = templateZone.y2,
                    z2 = templateZone.z2
                )
                is ZoneGiveElytra -> ZoneGiveElytra(
                    name = templateZone.name,
                    x1 = templateZone.x1,
                    y1 = templateZone.y1,
                    z1 = templateZone.z1,
                    x2 = templateZone.x2,
                    y2 = templateZone.y2,
                    z2 = templateZone.z2
                )
                is ZoneUnderwaterBoost -> ZoneUnderwaterBoost(
                    name = templateZone.name,
                    x1 = templateZone.x1,
                    y1 = templateZone.y1,
                    z1 = templateZone.z1,
                    x2 = templateZone.x2,
                    y2 = templateZone.y2,
                    z2 = templateZone.z2
                )
                else -> templateZone
            }
        }.toMutableList()

        arenaZones[arenaWorldName] = clonedZones

        val barrierCount = clonedZones.filterIsInstance<ZoneBarrier>().size
        AceRacePlugin.instance.logger.info("✅ Клонировано ${clonedZones.size} зон для арены $arenaWorldName (BARRIER: $barrierCount)")
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
}
