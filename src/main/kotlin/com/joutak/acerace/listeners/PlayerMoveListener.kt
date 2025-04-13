package com.joutak.acerace.listeners

import com.joutak.acerace.Config
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.worlds.World
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerMoveListener : Listener {
    @EventHandler
    fun playerMoveEvent(event : PlayerMoveEvent) {
        val player = event.player
        val location = event.player.location
        for (zone in ZoneManager.getZones().values) {
            if (zone.isInside(location)) {
                zone.execute(player)
            }
        }

        if (player.gameMode == GameMode.ADVENTURE) {
            if (player.y < Config.Y_DEATH) player.teleport(ZoneManager.get(PlayerData.getLastCheck(playerUuid = player.uniqueId)).location)
            if (!player.isGliding) player.getInventory().setChestplate(null);
        }
    }
}