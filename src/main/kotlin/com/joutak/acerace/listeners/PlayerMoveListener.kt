package com.joutak.acerace.listeners

import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.PluginManager
import com.joutak.acerace.worlds.World
import com.joutak.acerace.worlds.WorldManager
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import org.bukkit.scoreboard.Team

class PlayerMoveListener : Listener {
    @EventHandler
    fun playerMoveEvent(event : PlayerMoveEvent) {
        val player = event.player
        val location = event.player.location

        if (player.gameMode != GameMode.ADVENTURE) return

        if (location.world.name.startsWith("AceRaceMap") || location.world.name.startsWith("lobby") ){

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
                val playerData = PlayerData.get(player.uniqueId)
                val x1 = CheckpointManager.get(playerData.getLastCheck()).x1
                val x2 = CheckpointManager.get(playerData.getLastCheck()).x2
                val y1 = CheckpointManager.get(playerData.getLastCheck()).y1
                val y2 = CheckpointManager.get(playerData.getLastCheck()).y2
                val z1 = CheckpointManager.get(playerData.getLastCheck()).z1
                val z2 = CheckpointManager.get(playerData.getLastCheck()).z2
                val yaw = CheckpointManager.get(playerData.getLastCheck()).yaw
                val pitch = CheckpointManager.get(playerData.getLastCheck()).pitch

                val loc = Location(location.world, (x1+x2)/2, (y1+y2)/2, (z1+z2)/2, yaw, pitch)
                player.teleport(loc)
            }
            if (!player.isGliding) player.inventory.chestplate = null;
        }
    }
}
