package com.joutak.acerace.checkpoints

import com.joutak.acerace.utils.PluginManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object CheckpointConfig {
    private val file: File by lazy {
        File(PluginManager.acerace.dataFolder, "checkpoints.yml")
            .apply { if (!exists()) createNewFile() }
    }

    private var config: YamlConfiguration =
        YamlConfiguration.loadConfiguration(file)

    fun saveZone(zone: CheckpointZone) {
        val basePath = "checkpoints.${zone.checkpointIndex}.zones"
        val existing = config.getConfigurationSection(basePath)
        val nextIndex = existing?.getKeys(false)?.size ?: 0

        val zonePath = "$basePath.$nextIndex"
        saveLocation("$zonePath.min", zone.min)
        saveLocation("$zonePath.max", zone.max)

        config.save(file)
    }

    fun removeZonesByIndex(checkpointIndex: Int) {
        config.set("checkpoints.$checkpointIndex", null)
        config.save(file)
    }

    fun clearAll() {
        config.set("checkpoints", null)
        config.save(file)
    }

    fun loadAll(): List<CheckpointZone> {
        config = YamlConfiguration.loadConfiguration(file)
        val result = mutableListOf<CheckpointZone>()

        val checkpointsSection =
            config.getConfigurationSection("checkpoints") ?: return emptyList()

        for (cpKey in checkpointsSection.getKeys(false)) {
            val cpIndex = cpKey.toIntOrNull() ?: continue
            val zonesSection = checkpointsSection
                .getConfigurationSection("$cpKey.zones") ?: continue

            for (zoneKey in zonesSection.getKeys(false)) {
                val zonePath = "$cpKey.zones.$zoneKey"
                val min = loadLocation("checkpoints.$zonePath.min") ?: continue
                val max = loadLocation("checkpoints.$zonePath.max") ?: continue
                result.add(CheckpointZone(cpIndex, min, max))
            }
        }

        return result
    }

    private fun saveLocation(path: String, loc: Location) {
        config.set("$path.world", loc.world?.name)
        config.set("$path.x", loc.x)
        config.set("$path.y", loc.y)
        config.set("$path.z", loc.z)
    }

    private fun loadLocation(path: String): Location? {
        val worldName = config.getString("$path.world") ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null
        val x = config.getDouble("$path.x")
        val y = config.getDouble("$path.y")
        val z = config.getDouble("$path.z")
        return Location(world, x, y, z)
    }
}