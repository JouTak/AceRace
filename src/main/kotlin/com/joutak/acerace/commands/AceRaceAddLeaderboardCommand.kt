package com.joutak.acerace.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object AceRaceAddLeaderboardCommand : AceRaceCommand("addleaderboard", emptyList(), "acerace.admin") {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {

        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (sender !is Player) {
            sender.sendMessage("Данную команду можно использовать только в игре.")
            return true
        }

        Bukkit.dispatchCommand(sender, "summon text_display ~ ~ ~ {transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[3f,3f,3f]},text:'{\"bold\":true,\"color\":\"gold\"," +
                "\"text\":\"LEADERBOARD\"}'}")

        return true
    }

    override fun getTabHint(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return emptyList()
    }
}