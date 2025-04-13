package com.joutak.acerace

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object Config {
    val LOG_INFO_TO_CONSOLE: Boolean
    var TIME_TO_START_GAME: Int
    var MAX_PLAYERS_IN_GAME: Int
    var PLAYERS_TO_START: Int
    var PLAYERS_TO_END: Int
    var TIME_TO_START_GAME_LOBBY: Int
    var TIME_TO_FINISH: Int
    var SET_Y_SMALL: Double
    var SET_Y_MID: Double
    var SET_Y_BIG: Double
    var DIR_MP_MID: Double
    var DIR_MP_BIG: Double
    var DIR_MP_ELYTRA: Double
    var DIR_MP_WATER: Double
    var SET_Y_ELYTRA: Double
    var SPEED_AMP: Int
    var SPEED_DURATION: Int
    var TIME_TO_END_GAME: Int
    var Y_DEATH: Int
    var LAPSES_TO_FINISH: Int

    init {
        val configFile = File(AceRacePlugin.instance.dataFolder, "config.yml")
        if (!configFile.exists()) {
            AceRacePlugin.instance.saveResource("config.yml", true)
        }
        val config = YamlConfiguration.loadConfiguration(configFile)

        LOG_INFO_TO_CONSOLE = config.getBoolean("LOG_INFO_TO_CONSOLE")
        TIME_TO_START_GAME = config.getInt("TIME_TO_START_GAME")
        MAX_PLAYERS_IN_GAME = config.getInt("MAX_PLAYERS_IN_GAME")
        PLAYERS_TO_START = config.getInt("PLAYERS_TO_START")
        PLAYERS_TO_END = config.getInt("PLAYERS_TO_END")
        TIME_TO_START_GAME_LOBBY = config.getInt("TIME_TO_START_GAME_LOBBY")
        SET_Y_SMALL = config.getDouble("SET_Y_SMALL")
        SET_Y_MID = config.getDouble("SET_Y_MID")
        SET_Y_BIG = config.getDouble("SET_Y_BIG")
        DIR_MP_MID = config.getDouble("DIR_MP_MID")
        DIR_MP_BIG = config.getDouble("DIR_MP_BIG")
        DIR_MP_WATER = config.getDouble("DIR_MP_WATER")
        DIR_MP_ELYTRA = config.getDouble("DIR_MP_ELYTRA")
        SET_Y_ELYTRA = config.getDouble("SET_Y_ELYTRA")
        SPEED_DURATION = config.getInt("SPEED_DURATION")
        SPEED_AMP = config.getInt("SPEED_AMP")
        TIME_TO_END_GAME = config.getInt("TIME_TO_END_GAME")
        Y_DEATH = config.getInt("Y_DEATH")
        TIME_TO_FINISH = config.getInt("TIME_TO_FINISH")
        LAPSES_TO_FINISH = config.getInt("LAPSES_TO_FINISH")
    }
}