package com.joutak.acerace.listeners

import com.joutak.acerace.Config
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerMoveListener : Listener {
    @EventHandler
    fun playerMoveEvent(event : PlayerMoveEvent) {
        val player = event.player
        val playerData = PlayerData.get(player.uniqueId)
        val location = event.player.location
        for (zone in ZoneManager.getZones().values) {
            if (zone.isInside(location)) {
                zone.execute(player)
            }
        }

        if (player.gameMode == GameMode.ADVENTURE && playerData.isInGame()) {
            if (player.y <= Config.Y_DEATH) {
                player.teleport(ZoneManager.get(PlayerData.getLastCheck(playerUuid = player.uniqueId)).location)
            }
            if (!player.isGliding) player.inventory.chestplate = null;
            if (GameManager.get(playerData.games.lastOrNull())!!.getPhase() == GamePhase.START) event.isCancelled = true
        }
    }
}
