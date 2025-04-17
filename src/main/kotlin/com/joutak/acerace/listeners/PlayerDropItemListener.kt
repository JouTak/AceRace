package com.joutak.acerace.listeners

import com.joutak.acerace.games.Game
import com.joutak.acerace.players.PlayerData
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent

object PlayerDropItemListener : Listener {
    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player

        if (player.gameMode == GameMode.ADVENTURE) {
            event.isCancelled = true
        }
    }
}