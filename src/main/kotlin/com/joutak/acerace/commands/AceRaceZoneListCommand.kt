package com.joutak.acerace.commands

import com.joutak.acerace.zones.ZoneManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceZoneListCommand : AceRaceCommand("zoneList", listOf<String>()) {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }

        if (ZoneManager.getZones().isEmpty()) {
            sender.sendMessage("Нет активных зон.")
        } else {
            sender.sendMessage("Список зон:")
            ZoneManager.getZones().values.forEach {
                sender.sendMessage(it.name)
            }
        }

        return true
    }

    override fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return emptyList()
    }
}