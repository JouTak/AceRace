package com.joutak.acerace.listeners

import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
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
        val location = event.player.location

        if (player.gameMode != GameMode.ADVENTURE) return
        
        for (zone in ZoneManager.getZones().values) {
            if (zone.isInside(location)) {
                zone.execute(player)
            }
        }

        for (checkpoint in CheckpointManager.getCheckpoints().values){
            if (checkpoint.isInside(location)){
                checkpoint.execute(player)
            }
        }

        if (player.y <= Config.get(ConfigKeys.Y_DEATH)) {
            player.teleport(CheckpointManager.get(PlayerData.getLastCheck(playerUuid = player.uniqueId)).location)
        }
        if (!player.isGliding) player.inventory.chestplate = null;

    }
}
