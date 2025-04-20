package com.joutak.acerace.commands
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.worlds.WorldManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object AceRaceSpectateCommand : AceRaceCommand("spectate", listOf("name"), "acerace.spectator") {
    override fun execute(
        sender: CommandSender,
        command: Command,
        string: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Данную команду можно использовать только в игре.")
            return true
        }

        var startSpectating = false
        var endSpectating = false

        if (args.size == this.args.size) {
            startSpectating = true
        } else if (args.size == this.args.size - 1) {
            endSpectating = true
        }

        if (!startSpectating && !endSpectating) {
            return false
        }

        if (endSpectating) {
            GameManager.getBySpectator(sender).forEach { it.removeSpectator(sender) }
            LobbyManager.teleportToLobby(sender)
            return true
        }

        // else if (startSpectating)
        val world = WorldManager.get(args[0])
        if (world == null) {
            sender.sendMessage("Мира с таким именем не существует.")
            return true
        }
        val game = GameManager.getByWorld(world)
        if (game == null) {
            sender.sendMessage("В данный момент на арене не идет игра.")
            return true
        }

        GameManager.getBySpectator(sender).forEach { it.removeSpectator(sender) }
        game.addSpectator(sender)
        return true
    }

    override fun getTabHint(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> =
        when (args.size) {
            1 -> WorldManager.getWorlds().keys.filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
}