package com.joutak.acerace.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import ru.joutak.blockparty.commands.AceRaceChangeConfigCommand

object AceRaceCommandExecutor : CommandExecutor, TabExecutor {
    private val commands = mutableMapOf<String, AceRaceCommand>()

    init {
        registerCommand(AceRaceAddZoneCommand)
        registerCommand(AceRaceZoneListCommand)
        registerCommand(AceRaceZoneInfoCommand)
        registerCommand(AceRaceRemoveZoneCommand)
        registerCommand(AceRaceChangeConfigCommand)
        registerCommand(AceRaceReadyCommand)
        registerCommand(AceRaceAddWorldCommand)
        registerCommand(AceRaceAddCheckpointCommand)
        registerCommand(AceRaceRemoveCheckpointCommand)
        registerCommand(AceRaceCheckpointListCommand)
        registerCommand(AceRaceCheckpointInfoCommand)
        registerCommand(AceRaceWorldListCommand)
        registerCommand(AceRaceSpectateCommand)
    }

    private fun registerCommand(command : AceRaceCommand) {
        commands[command.name] = command
    }

    private fun getUsageMessage(sender: CommandSender): Component {
        if (sender is Player && !sender.isOp) {
            return LinearComponents.linear(
                Component.text("/ar ready", NamedTextColor.GOLD),
                Component.text(" - "),
                Component.text("Встать", NamedTextColor.GREEN),
                Component.text(" в очередь/"),
                Component.text("выйти", NamedTextColor.RED),
                Component.text(" из очереди на AceRace"),
            )
        } else {
            return Component.text(
                ""
            )
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        string: String,
        args: Array<out String>?,
    ): Boolean {
        if (args?.getOrNull(0) in commands.keys &&
            commands[args!![0]]!!.execute(
                sender,
                command,
                string,
                if (args.size > 1) args.sliceArray(1 until args.size) else emptyArray(),
            )
        ) {
            return true
        }

        sender.sendMessage(getUsageMessage(sender))
        return true
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
