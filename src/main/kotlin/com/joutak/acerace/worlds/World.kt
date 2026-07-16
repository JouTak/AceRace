package com.joutak.acerace.worlds

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.utils.PluginManager
import org.bukkit.*
import org.mvplugins.multiverse.core.MultiverseCore
import org.mvplugins.multiverse.core.MultiverseCoreApi

data class World(
    val worldName : String
) {
    private var state = WorldState.READY
    private lateinit var multiverseCore: MultiverseCore

    init {
        val mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core") as? MultiverseCore
        if (mv == null) {
            AceRacePlugin.instance.logger.warning("Multiverse-Core не найден! Некоторые функции мира могут не работать.")
        } else {
            multiverseCore = mv
        }
        this.reset()
    }

    companion object {
        fun deserialize(values: Map<String, Any>): World {
            AceRacePlugin.instance.logger.info("Десериализация информации о мире ${values["worldName"]}")
            return World(
                values["worldName"] as String,
            )
        }
    }

    fun getState(): WorldState {
        return state
    }

    fun setState(state: WorldState) {
        this.state = state
    }

    fun reset() {
        val world = Bukkit.getWorld(worldName) ?: run {
            AceRacePlugin.instance.logger.warning("Мир $worldName не найден!")
            return
        }
        if (!::multiverseCore.isInitialized) {
            AceRacePlugin.instance.logger.warning("Multiverse-Core не инициализирован, пропускаем настройку MVWorld")
            // Все равно настраиваем Bukkit мир
            configureBukkitWorld(world)
            setState(WorldState.READY)
            return
        }

        val coreApi = MultiverseCoreApi.get();
        val worldManager = coreApi.worldManager
        val mvWorldOption = worldManager.getWorld(worldName)

        if (mvWorldOption.isDefined) {
            val mvWorld = mvWorldOption.get()

            // MV 5.7.0 использует методы-сеттеры
            mvWorld.setAllowWeather(false)
            mvWorld.setDifficulty(Difficulty.PEACEFUL)
            mvWorld.setGameMode(GameMode.ADVENTURE)
            mvWorld.setPvp(false)
            mvWorld.setHunger(false)

            // Устанавливаем время (midnight = 18000)
            try {
                val setTimeMethod = mvWorld.javaClass.getMethod("setTime", Long::class.java)
                setTimeMethod.invoke(mvWorld, 18000L)
            } catch (_: Exception) {
                // Игнорируем, если метод недоступен
            }
        } else {
            AceRacePlugin.instance.logger.warning("Мир $worldName не зарегистрирован в Multiverse")
        }

        configureBukkitWorld(world)
        setState(WorldState.READY)
    }

    private fun configureBukkitWorld(world: org.bukkit.World) {
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
        world.setGameRule(GameRule.FALL_DAMAGE, false)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
    }

    fun serialize(): Map<String, Any> {
        return mapOf(
            "worldName" to this.worldName
        )
    }
}