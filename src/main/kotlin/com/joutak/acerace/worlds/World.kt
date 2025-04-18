package com.joutak.acerace.worlds

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.utils.PluginManager
import org.bukkit.*

data class World(
    val worldName : String
) {
    private var state = WorldState.READY

    init {
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
        val world = Bukkit.getWorld(worldName)!!
        val mvWorld = PluginManager.multiverseCore.mvWorldManager.getMVWorld(worldName)

        mvWorld.setTime("midnight")
        mvWorld.setEnableWeather(false)
        mvWorld.setDifficulty(Difficulty.PEACEFUL)
        mvWorld.setGameMode(GameMode.ADVENTURE)
        mvWorld.setPVPMode(false)
        mvWorld.hunger = false

        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
        world.setGameRule(GameRule.FALL_DAMAGE, false)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)

        setState(WorldState.READY)
    }

    fun serialize(): Map<String, Any> {
        return mapOf(
            "worldName" to this.worldName
        )
    }
}