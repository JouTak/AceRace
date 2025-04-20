package com.joutak.acerace.listeners

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.utils.PluginManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerQuitListener : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerData = PlayerData.get(player.uniqueId)
        val lastGame = GameManager.getByPlayer(player)

        Bukkit.getScheduler().runTaskLater(
            PluginManager.acerace,
            Runnable {
                if (lastGame != null && lastGame.getPhase() != GamePhase.END) {
                    PlayerData.resetPlayer(player.uniqueId)
                    lastGame.checkPlayers()
                }
                LobbyManager.removeFromReadyPlayers(player)
                playerData.save()
                LobbyManager.check()
            },
            5L,
        )
    }
}