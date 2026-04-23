package com.joutak.acerace.listeners

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerMoveListener : Listener {
    @EventHandler
    fun playerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        val location = player.location
        val worldName = location.world.name.lowercase()

        if (player.gameMode != GameMode.ADVENTURE) return

        if (worldName.startsWith("aceracemap")) {
            for (p in Bukkit.getOnlinePlayers()) {
                if (p in player.getNearbyEntities(2.0, 2.0, 2.0)) {
                    if (!player.isInWater) return
                    player.hidePlayer(AceRacePlugin.instance, p)
                } else {
                    player.showPlayer(AceRacePlugin.instance, p)
                }
            }
        }

        if (worldName.startsWith("aceracemap") || worldName.startsWith("lobby")) {
            for (zone in ZoneManager.getZones().values) {
                if (zone.isInside(location)) {
                    AceRacePlugin.instance.logger.info("Игрок ${player.name} вошел в зону ${zone.name} (${zone.type})")
                    zone.execute(player)
                }
            }

            for (checkpoint in CheckpointManager.getCheckpoints().values) {
                if (checkpoint.isInside(location)) {
                    checkpoint.execute(player)
                }
            }

            if (player.y <= Config.get(ConfigKeys.Y_DEATH)) {
                val playerData = PlayerData.get(player.uniqueId)
                val checkpoint = CheckpointManager.get(playerData.getLastCheck())
                val loc = Location(
                    location.world,
                    (checkpoint.x1 + checkpoint.x2) / 2,
                    (checkpoint.y1 + checkpoint.y2) / 2,
                    (checkpoint.z1 + checkpoint.z2) / 2,
                    checkpoint.yaw,
                    checkpoint.pitch,
                )
                player.teleport(loc)
            }
        }
    }
}
