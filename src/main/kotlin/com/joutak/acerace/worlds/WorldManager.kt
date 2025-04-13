package com.joutak.acerace.worlds

import com.joutak.acerace.AceRacePlugin
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

object WorldManager {
    private val worlds = mutableMapOf<String, World>()
    private var worldsFile = YamlConfiguration()

    fun add(worldName: String) {
        if (worlds.containsKey(worldName))
            throw IllegalArgumentException("Мир с таким именем уже существует.")

        worlds[worldName] = World(worldName)
    }

    fun add(world: World){
        if (worlds.containsKey(world.worldName))
            throw IllegalArgumentException("Мир с таким именем уже существует.")

        worlds[world.worldName] = world
    }

    fun get(name: String): World {
        if (!worlds.containsKey(name))
            throw IllegalArgumentException("Мира с таким именем не существует.")

        return worlds[name]!!
    }

    fun getWorlds() : Map<String, World> {
        return worlds
    }

    fun getReadyWorld() : World? {
        for (world in worlds.values) {
            if (world.getState() == WorldState.READY)
                return world
        }
        return null
    }

    fun remove(name: String) {
        if (!worlds.containsKey(name))
            throw IllegalArgumentException("Мира с таким именем не существует.")

        worlds.remove(name)
    }

    fun clear() {
        worlds.clear()
    }

    fun hasReadyWorld(): Boolean {
        for (world in worlds.values) {
            if (world.getState() == WorldState.READY)
                return true
        }
        return false
    }

    fun loadWorlds() {
        val fx = File(AceRacePlugin.instance.dataFolder, "worlds.yml")
        if (!fx.exists()) {
            AceRacePlugin.instance.saveResource("worlds.yml", true)
        }

        worldsFile = YamlConfiguration.loadConfiguration(fx)
        val worldsList = worldsFile.getList("worlds") as? List<Map<String, Any>> ?: return

        clear()

        for (value in worldsList) {
            try {
                add(World.deserialize(value))
            } catch (e: Exception) {
                AceRacePlugin.instance.getLogger().severe("Ошибка при загрузке миров: ${e.message}")
                break
            }
        }
    }

    fun saveWorlds() {
        val fx = File(AceRacePlugin.instance.dataFolder, "worlds.yml")

        worldsFile.set("worlds", worlds.values.map {
                value -> value.serialize()
        })

        try {
            worldsFile.save(fx)
        } catch (e: IOException) {
            AceRacePlugin.instance.getLogger().severe("Ошибка при сохранении зон: ${e.message}")
        }
    }
}