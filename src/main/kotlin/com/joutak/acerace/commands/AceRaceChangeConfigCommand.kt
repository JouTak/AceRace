package com.joutak.acerace.commands

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.Config
import com.joutak.acerace.utils.ConfigValType
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceChangeConfigCommand : AceRaceCommand("changecon", listOf<String>("type", "mod")) {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }
        if (args.size != this.args.size) {
            return false
        }
        try {
            when (args[0]) {
                "SET_Y_SMALL" -> Config.SET_Y_SMALL = args[1].toDouble()
                "SET_Y_MID" -> Config.SET_Y_MID = args[1].toDouble()
                "SET_Y_BIG" -> Config.SET_Y_BIG = args[1].toDouble()
                "DIR_MP_MID" -> Config.DIR_MP_MID = args[1].toDouble()
                "DIR_MP_BIG" -> Config.DIR_MP_BIG = args[1].toDouble()
                "MAX_PLAYERS_IN_GAME" -> Config.MAX_PLAYERS_IN_GAME = args[1].toInt()
                "PLAYERS_TO_END" -> Config.PLAYERS_TO_END = args[1].toInt()
                "PLAYERS_TO_START" -> Config.PLAYERS_TO_START = args[1].toInt()
                "TIME_TO_START_GAME_LOBBY" -> Config.TIME_TO_START_GAME_LOBBY = args[1].toInt()
                "SPEED_AMP" -> Config.SPEED_AMP = args[1].toInt()
                "SPEED_DURATION" -> Config.SPEED_DURATION = args[1].toInt()
                "DIR_MP_ELYTRA" -> Config.DIR_MP_ELYTRA = args[1].toDouble()
                "DIR_MP_WATER" -> Config.DIR_MP_WATER = args[1].toDouble()
                "SET_Y_ELYTRA" -> Config.SET_Y_ELYTRA = args[1].toDouble()
                "TIME_TO_END_GAME" -> Config.TIME_TO_END_GAME = args[1].toInt()
                "Y_DEATH" -> Config.Y_DEATH = args[1].toInt()
            }
        } catch (e: NumberFormatException) {
            sender.sendMessage("Значение должно быть числом.")
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("${e.message}")
        }

        return true
    }
    override fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.isOp)
            return emptyList()


        return when (args.size) {
            1 -> ConfigValType.values().map{it.toString()}
            else -> emptyList()
        }
    }
}