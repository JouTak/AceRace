package com.joutak.acerace.listeners

import com.joutak.acerace.players.PlayerData
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent

object PlayerDropItemListener : Listener {
    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player

        if (PlayerData.get(player.uniqueId).isInGame()) {
            event.isCancelled = true
        }
    }
}