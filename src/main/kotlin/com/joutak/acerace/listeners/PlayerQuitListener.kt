package com.joutak.acerace.listeners

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerQuitListener : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerData = PlayerData.get(player.uniqueId)
        val lastGame = GameManager.get(playerData.games.lastOrNull())

        if (lastGame != null && lastGame.getPhase() != GamePhase.END) {
            PlayerData.resetGame(player.uniqueId)
        }

        Bukkit.getScheduler().runTaskLater(AceRacePlugin.instance, Runnable {
            LobbyManager.removePlayer(player)
            playerData.saveData()
            LobbyManager.check()
        }, 5L)
    }
}