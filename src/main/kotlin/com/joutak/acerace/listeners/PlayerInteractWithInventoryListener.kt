package com.joutak.acerace.listeners

import com.joutak.acerace.games.GameManager
import com.joutak.acerace.players.PlayerData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent


object PlayerInteractWithInventoryListener : Listener {
    @EventHandler
    fun onInteraction(event: InventoryClickEvent) {
        val player = event.whoClicked as Player

        if (GameManager.isPlaying(player.uniqueId)) {
            event.isCancelled = true
        }
    }
}