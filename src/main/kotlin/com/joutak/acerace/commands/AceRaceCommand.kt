package com.joutak.acerace.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

abstract class AceRaceCommand(val name: String, val args: List<String>) {
    abstract fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean
    abstract fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>
}