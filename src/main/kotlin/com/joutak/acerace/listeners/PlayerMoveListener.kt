package com.joutak.acerace.listeners

import com.joutak.acerace.zones.ZoneManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerMoveListener : Listener {
    @EventHandler
    fun playerMoveEvent(event : PlayerMoveEvent){
        val player = event.player
        val location = event.player.location
        for (zone in ZoneManager.getZones().values){
            if (zone.isInside(location)) {
                zone.execute(player)
            }
        }

        if (!player.isGliding) player.getInventory().setChestplate(null);
    }
}