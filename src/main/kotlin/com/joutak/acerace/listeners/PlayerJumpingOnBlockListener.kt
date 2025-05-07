package com.joutak.acerace.listeners

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerJumpingOnBlockListener : Listener {
    @EventHandler
    fun playerJumpingOnBlockEvent(event: PlayerJumpEvent) {
        val player = event.player

        for (x in -3..3){
            for (z in -3..3){
                if (Location(player.world, player.location.x + x.toDouble()/10, player.location.y, player.location.z + z.toDouble()/10).block.getRelative(BlockFace.DOWN).type == Material.LIME_CONCRETE) {
                    player.velocity = player.velocity.setY(Config.get(ConfigKeys.SET_Y_SMALL))
                }
                if (Location(player.world, player.location.x + x.toDouble()/10, player.location.y, player.location.z + z.toDouble()/10).block.getRelative(BlockFace.DOWN).type == Material.YELLOW_CONCRETE) {
                    player.velocity = player.location.direction.multiply(Config.get(ConfigKeys.DIR_MP_MID))
                    player.velocity = player.velocity.setY(Config.get(ConfigKeys.SET_Y_MID))

                }
                if (Location(player.world, player.location.x + x.toDouble()/10, player.location.y, player.location.z + z.toDouble()/10).block.getRelative(BlockFace.DOWN).type == Material.RED_CONCRETE) {
                    player.velocity = player.location.direction.multiply(Config.get(ConfigKeys.DIR_MP_BIG))
                    player.velocity = player.velocity.setY(Config.get(ConfigKeys.SET_Y_BIG))

                }
            }
        }
    }
}
