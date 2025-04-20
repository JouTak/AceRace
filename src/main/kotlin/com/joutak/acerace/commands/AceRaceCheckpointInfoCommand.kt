package com.joutak.acerace.commands

import com.joutak.acerace.checkpoints.CheckpointManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceCheckpointInfoCommand : AceRaceCommand("checkpointInfo", listOf("name"), "acerace.admin") {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }

        try {
            val checkpoint = CheckpointManager.get(args[0])

            sender.sendMessage("Информация о зоне ${checkpoint.name}:")
            sender.sendMessage("Координаты: (${checkpoint.x1}, ${checkpoint.y1}, ${checkpoint.z1} ; ${checkpoint.x2}, ${checkpoint.y2}, ${checkpoint.z2})")
            sender.sendMessage("Yaw: ${checkpoint.yaw}")
            sender.sendMessage("Pitch: ${checkpoint.pitch}")
        }
        catch (e: IllegalArgumentException) {
            sender.sendMessage("${e.message}")
        }

        return true
    }

    override fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.isOp) return emptyList()

        return when (args.size) {
            1 -> CheckpointManager.getCheckpoints().keys.filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}