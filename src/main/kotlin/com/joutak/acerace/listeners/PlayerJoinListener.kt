package com.joutak.acerace.listeners

import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import org.bukkit.scoreboard.Team


object PlayerJoinListener : Listener {
    val manager: ScoreboardManager = Bukkit.getScoreboardManager()
    val board: Scoreboard = manager.newScoreboard
    val noCollisionTeam: Team = board.registerNewTeam("noCollisionTeam")

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        if (!noCollisionTeam.hasPlayer(player)){
            noCollisionTeam.addPlayer(player)
        }

        if (noCollisionTeam.getOption(Team.Option.COLLISION_RULE) != Team.OptionStatus.NEVER) {
            noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        }

        PlayerData.resetPlayer(player.uniqueId)
        LobbyManager.teleportToLobby(player)
    }
}