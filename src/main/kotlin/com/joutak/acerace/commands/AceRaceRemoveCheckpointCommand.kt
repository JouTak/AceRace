package com.joutak.acerace.commands

import com.joutak.acerace.checkpoints.Checkpoint
import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceRemoveCheckpointCommand : AceRaceCommand("removeCheck", listOf<String>("remove")) {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }

        try {
            CheckpointManager.remove(args[0])
            sender.sendMessage("Чекпоинт ${args[0]} успешно удален!")
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