package com.joutak.acerace.listeners

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent


object PlayerChangeWorldListener : Listener {
    @EventHandler
    fun playerChangeWorldEvent(event: PlayerInteractEvent){
        val player = event.player

        if (player.gameMode == GameMode.ADVENTURE){
            event.setUseInteractedBlock(Event.Result.DENY)
        }
    }
}