package com.joutak.acerace.zones

import com.joutak.acerace.AceRacePlugin
import org.bukkit.configuration.file.YamlConfiguration
import java.io.IOException
import java.io.File

object ZoneManager {
    private val zones = mutableMapOf<String, Zone>()
    private var zonesFile = YamlConfiguration()

    fun add(zone: Zone) {
        if (zones.containsKey(zone.name))
            throw IllegalArgumentException("Зона с таким именем уже существует.")

        zones[zone.name] = zone
    }

    fun get(name: String): Zone {
        if (!zones.containsKey(name))
            throw IllegalArgumentException("Зоны с таким именем не существует.")

        return zones[name]!!
    }

    fun getZones() : Map<String, Zone> {
        return zones
    }

    fun remove(name: String) {
        if (!zones.containsKey(name))
            throw IllegalArgumentException("Зоны с таким именем не существует.")

        zones.remove(name)
    }

    fun clear() {
        zones.clear()
    }

    fun loadZones() {
        val fx = File(AceRacePlugin.instance.dataFolder, "zones.yml")
        if (!fx.exists()) {
            AceRacePlugin.instance.saveResource("zones.yml", true)
        }

        zonesFile = YamlConfiguration.loadConfiguration(fx)
        val zonesList = zonesFile.getMapList("zones")

        clear()

        for (value in zonesList) {
            try {
                val normalizedValue = value.entries.associate { (key, entryValue) ->
                    key.toString() to (entryValue ?: throw IllegalArgumentException("Пустое значение в zones.yml"))
                }
                add(Zone.deserialize(normalizedValue)!!)
            } catch (e: Exception) {
                AceRacePlugin.instance.logger.severe("Ошибка при загрузке зон: ${e.message}")
                break
            }
        }

        AceRacePlugin.instance.logger.info("Загружено зон: ${zones.size}")
    }

    fun saveZones() {
        val fx = File(AceRacePlugin.instance.dataFolder, "zones.yml")

        zonesFile.set("zones", zones.values.map {
                value -> value.serialize()
        })

        try {
            zonesFile.save(fx)
        } catch (e: IOException) {
            AceRacePlugin.instance.logger.severe("Ошибка при сохранении зон: ${e.message}")
        }
    }
}
