package com.joutak.acerace.commands

import com.joutak.acerace.zones.ZoneManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceZoneInfoCommand : AceRaceCommand("zoneInfo", listOf("name"), "acerace.admin") {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }

        try {
            val zone = ZoneManager.get(args[0])

            sender.sendMessage("Информация о зоне ${zone.name}:")
            sender.sendMessage("Вид: ${zone.type}")
            sender.sendMessage("Координаты: (${zone.x1}, ${zone.y1}, ${zone.z1} ; ${zone.x2}, ${zone.y2}, ${zone.z2})")
        }
        catch (e: IllegalArgumentException) {
            sender.sendMessage("${e.message}")
        }

        return true
    }

    override fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.isOp) return emptyList()

        return when (args.size) {
            1 -> ZoneManager.getZones().keys.filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}