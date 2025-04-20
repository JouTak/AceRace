package com.joutak.acerace.checkpoints

import com.joutak.acerace.AceRacePlugin
import org.bukkit.configuration.file.YamlConfiguration
import java.io.IOException
import java.io.File

object CheckpointManager {
    private val checkpoints = mutableMapOf<String, Checkpoint>()
    private var checkpointsFile = YamlConfiguration()

    fun add(checkpoint: Checkpoint) {
        if (checkpoints.containsKey(checkpoint.name))
            throw IllegalArgumentException("Чекпоинт с таким именем уже существует.")

        checkpoints[checkpoint.name] = checkpoint
    }

    fun get(name: String): Checkpoint {
        if (!checkpoints.containsKey(name))
            throw IllegalArgumentException("Чекпоинта с таким именем не существует.")

        return checkpoints[name]!!
    }

    fun getCheckpoints() : Map<String, Checkpoint> {
        return checkpoints
    }

    fun remove(name: String) {
        if (!checkpoints.containsKey(name))
            throw IllegalArgumentException("Чекпоинта с таким именем не существует.")

        checkpoints.remove(name)
    }

    private fun clear() {
        checkpoints.clear()
    }

    fun loadCheckpoints() {
        val fx = File(AceRacePlugin.instance.dataFolder, "checkpoints.yml")

        AceRacePlugin.instance.logger.info("LOADING")

        if (!fx.exists()) {
            AceRacePlugin.instance.saveResource("checkpoints.yml", true)
        }

        checkpointsFile = YamlConfiguration.loadConfiguration(fx)
        val checkpointsList = checkpointsFile.getList("checkpoints") as? List<Map<String, Any>> ?: return

        clear()

        for (value in checkpointsList) {
            try {
                add(Checkpoint.deserialize(value))
            } catch (e: Exception) {
                AceRacePlugin.instance.logger.severe("Ошибка при загрузке чекпоинтов: ${e.message}")
                break
            }
        }
    }

    fun saveCheckpoints() {
        val fx = File(AceRacePlugin.instance.dataFolder, "checkpoints.yml")

        checkpointsFile.set("checkpoints", checkpoints.values.map {
                value -> value.serialize()
        })

        try {
            checkpointsFile.save(fx)
        } catch (e: IOException) {
            AceRacePlugin.instance.logger.severe("Ошибка при сохранении чекпоинтов: ${e.message}")
        }
    }
}
