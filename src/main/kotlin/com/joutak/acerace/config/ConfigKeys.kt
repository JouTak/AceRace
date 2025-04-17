package com.joutak.acerace.config

object ConfigKeys {
    val LOG_INFO_TO_CONSOLE = boolean("LOG_INFO_TO_CONSOLE", true)
    val MAX_PLAYERS_IN_GAME = int("MAX_PLAYERS_IN_GAME", 8)
    val PLAYERS_TO_START = int("PLAYERS_TO_START", 4)
    val PLAYERS_TO_END = int("PLAYERS_TO_END", 8)
    val LAPSES_TO_FINISH = int("LAPSES_TO_FINISH",3)
    val TIME_TO_START_GAME_LOBBY = int("TIME_TO_START_GAME_LOBBY", 10)
    val TIME_TO_START_GAME = int("TIME_TO_START_GAME",20)
    val TIME_TO_FINISH = int("TIME_TO_FINISH",360)
    val TIME_TO_END_GAME = int("TIME_TO_END_GAME",20)
    val SET_Y_SMALL = double("SET_Y_SMALL", 1.1)
    val SET_Y_MID = double("SET_Y_MID", 2.1)
    val SET_Y_BIG = double("SET_Y_BIG",3.5)
    val DIR_MP_MID = double("DIR_MP_MID",1.07)
    val DIR_MP_BIG = double("DIR_MP_BIG",4.0)
    val DIR_MP_WATER = double("DIR_MP_WATER",2.5)
    val DIR_MP_ELYTRA = double("DIR_MP_ELYTRA",1.0)
    val SET_Y_ELYTRA = double("SET_Y_ELYTRA", 0.3)
    val SPEED_DURATION = int("SPEED_DURATION",40)
    val SPEED_AMP = int("SPEED_AMP",2)
    val Y_DEATH = int("Y_DEATH",-64)

    val all =
        setOf(
            LOG_INFO_TO_CONSOLE,
            MAX_PLAYERS_IN_GAME,
            PLAYERS_TO_START,
            PLAYERS_TO_END,
            LAPSES_TO_FINISH,
            TIME_TO_START_GAME_LOBBY,
            TIME_TO_START_GAME,
            TIME_TO_FINISH,
            TIME_TO_END_GAME,
            SET_Y_SMALL,
            SET_Y_MID,
            SET_Y_BIG,
            DIR_MP_MID,
            DIR_MP_BIG,
            DIR_MP_WATER,
            DIR_MP_ELYTRA,
            SET_Y_ELYTRA,
            SPEED_DURATION,
            SPEED_AMP,
            Y_DEATH
        )

    private fun int(
        path: String,
        default: Int,
    ) = object : ConfigKey<Int> {
        override val path = path
        override val value = default

        override fun parse(input: String) = input.toIntOrNull()
    }

    private fun boolean(
        path: String,
        default: Boolean,
    ) = object : ConfigKey<Boolean> {
        override val path = path
        override val value = default

        override fun parse(input: String): Boolean? =
            when (input.lowercase()) {
                "true", "yes", "1" -> true
                "false", "no", "0" -> false
                else -> null
            }
    }

    private fun double(
        path: String,
        default: Double,
    ) = object : ConfigKey<Double> {
        override val path = path
        override val value = default

        override fun parse(input: String) = input.toDoubleOrNull()
    }
}
