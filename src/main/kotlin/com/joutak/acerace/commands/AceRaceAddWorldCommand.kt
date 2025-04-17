package com.joutak.acerace.commands

import com.joutak.acerace.worlds.WorldManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceAddWorldCommand : AceRaceCommand("addWorld", listOf<String>("name")){

    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }
        if (Bukkit.getWorld(args[0]) == null) {
            sender.sendMessage("Мира с таким именем не существует!")
            return true
        }
        else {
            WorldManager.add(args[0])
            sender.sendMessage("Добавлен новый мир!")
        }
        return false
    }

    override fun getTabHint(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.isOp)
            return emptyList()

        return when (args.size) {
            1 -> Bukkit.getWorlds().map { it.name }
            else -> emptyList()
        }
    }

}