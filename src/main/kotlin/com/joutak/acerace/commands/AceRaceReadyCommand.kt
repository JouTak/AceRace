package com.joutak.acerace.commands

import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.players.PlayerState
import com.joutak.acerace.utils.LobbyManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object AceRaceReadyCommand : AceRaceCommand("ready", emptyList()) {
    override fun execute(sender: CommandSender, command: Command, string: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Данную команду можно использовать только в игре.")
            return true
        }

        if (args.size != this.args.size) {
            return false
        }

        val playerData = PlayerData.get(sender.uniqueId)

        when (playerData.state) {
            PlayerState.LOBBY -> {
                sender.sendMessage(
                    LinearComponents.linear(
                        Component.text("Вы "),
                        Component.text("встали", NamedTextColor.GREEN),
                        Component.text(" в очередь на AceRace!")
                    )
                )
                playerData.state = PlayerState.READY
                LobbyManager.check()
            }

            PlayerState.READY -> {
                sender.sendMessage(
                    LinearComponents.linear(
                        Component.text("Вы "),
                        Component.text("вышли", NamedTextColor.RED),
                        Component.text(" из очереди на AceRace!")
                    )
                )
                playerData.state = PlayerState.LOBBY
                LobbyManager.check()
            }

            else -> sender.sendMessage("Данную команду можно использовать только в лобби.")
        }

        return true
    }

    override fun getTabHint(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return emptyList()
    }

}