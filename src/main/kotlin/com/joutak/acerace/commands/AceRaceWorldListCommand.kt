package com.joutak.acerace.commands

import com.joutak.acerace.worlds.WorldManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceWorldListCommand : AceRaceCommand("worldList", listOf()) {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }

        if (WorldManager.getWorlds().isEmpty()) {
            sender.sendMessage("Нет свободных миров.")
        } else {
            sender.sendMessage("Список свободных миров:")
            WorldManager.getWorlds().values.forEach {
                sender.sendMessage(it.worldName)
            }
        }

        return true
    }

    override fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return emptyList()
    }
}