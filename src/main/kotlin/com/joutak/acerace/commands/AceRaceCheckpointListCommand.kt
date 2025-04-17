package com.joutak.acerace.commands

import com.joutak.acerace.checkpoints.CheckpointManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceCheckpointListCommand : AceRaceCommand("checkpointList", listOf<String>()) {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }

        if (CheckpointManager.getCheckpoints().isEmpty()) {
            sender.sendMessage("Нет активных чекпоинтов.")
        } else {
            sender.sendMessage("Список чекпоинтов:")
            CheckpointManager.getCheckpoints().values.forEach {
                sender.sendMessage(it.name)
            }
        }

        return true
    }

    override fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return emptyList()
    }
}