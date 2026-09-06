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

    fun saveZone(zone: CheckpointZone, worldName: String) {
        val basePath = "worlds.$worldName.checkpoints.${zone.checkpointIndex}.zones"
        val existing = config.getConfigurationSection(basePath)
        val nextIndex = existing?.getKeys(false)?.size ?: 0

        val zonePath = "$basePath.$nextIndex"
        saveLocation("$zonePath.min", zone.min)
        saveLocation("$zonePath.max", zone.max)

        config.save(file)
    }

    fun removeZonesByIndex(checkpointIndex: Int, worldName: String) {
        config.set("worlds.$worldName.checkpoints.$checkpointIndex", null)
        config.save(file)
    }

    fun clearAll(worldName: String) {
        config.set("worlds.$worldName", null)
        config.save(file)
    }

    fun loadAll(worldName: String): List<CheckpointZone> {
        config = YamlConfiguration.loadConfiguration(file)
        val result = mutableListOf<CheckpointZone>()

        val worldSection = config.getConfigurationSection("worlds.$worldName")
            ?: return emptyList()

        val checkpointsSection = worldSection.getConfigurationSection("checkpoints")
            ?: return emptyList()

        val world = Bukkit.getWorld(worldName) ?: return emptyList()

        for (cpKey in checkpointsSection.getKeys(false)) {
            val cpIndex = cpKey.toIntOrNull() ?: continue
            val zonesSection = checkpointsSection
                .getConfigurationSection("$cpKey.zones") ?: continue

            for (zoneKey in zonesSection.getKeys(false)) {
                val zonePath = "$cpKey.zones.$zoneKey"
                val min = loadLocation("worlds.$worldName.checkpoints.$zonePath.min", world) ?: continue
                val max = loadLocation("worlds.$worldName.checkpoints.$zonePath.max", world) ?: continue
                result.add(CheckpointZone(cpIndex, min, max))
            }
        }

        return result
    }

    fun getAllWorlds(): List<String> {
        config = YamlConfiguration.loadConfiguration(file)
        val worldsSection = config.getConfigurationSection("worlds") ?: return emptyList()
        return worldsSection.getKeys(false).toList()
    }

    private fun saveLocation(path: String, loc: Location) {
        config.set("$path.x", loc.x)
        config.set("$path.y", loc.y)
        config.set("$path.z", loc.z)
    }

    private fun loadLocation(path: String, world: org.bukkit.World): Location? {
        val x = config.getDouble("$path.x")
        val y = config.getDouble("$path.y")
        val z = config.getDouble("$path.z")
        return Location(world, x, y, z)
    }
}