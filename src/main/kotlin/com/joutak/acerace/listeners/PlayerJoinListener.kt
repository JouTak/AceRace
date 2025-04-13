package com.joutak.acerace.listeners

import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object PlayerJoinListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        PlayerData.resetGame(player.uniqueId)
        LobbyManager.addPlayer(player)
    }
}