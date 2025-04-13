package com.joutak.acerace.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

object AceRaceCommandExecutor : CommandExecutor, TabExecutor {
    private val commands = mutableMapOf<String, AceRaceCommand>()

    init {
        registerCommand(AceRaceCreateCommand)
        registerCommand(AceRaceListCommand)
        registerCommand(AceRaceInfoCommand)
        registerCommand(AceRaceRemoveCommand)
        registerCommand(AceRaceChangeConfigCommand)
        registerCommand(AceRaceReadyCommand)
        registerCommand(AceRaceAddWorldCommand)
    }

    private fun registerCommand(command : AceRaceCommand) {
        commands[command.name] = command
    }

    override fun onCommand(sender: CommandSender, command: Command, string: String, args: Array<out String>?): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args?.getOrNull(0) in commands.keys)
            return commands[args!![0]]!!.execute(
                sender,
                command,
                string,
                if (args.size > 1) args.sliceArray(1 until args.size) else emptyArray()
            )

        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> commands.keys.toList()
            else -> {
                if (args[0] !in commands.keys)
                    emptyList<String>()
                else
                    commands[args[0]]!!.getTabHint(
                        sender,
                        command,
                        alias,
                        args.sliceArray(1 until args.size)
                    )
            }
        }
    }
}
