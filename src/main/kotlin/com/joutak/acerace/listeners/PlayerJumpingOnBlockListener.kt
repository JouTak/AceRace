package com.joutak.acerace.listeners

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.Config
import com.joutak.acerace.commands.AceRaceAddWorldCommand
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerJumpingOnBlockListener : Listener {
    @EventHandler
    fun playerJumpingOnBlockEvent(event: PlayerJumpEvent) {
        val player = event.player

        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.LIME_CONCRETE) {
            player.velocity = player.velocity.setY(Config.SET_Y_SMALL)

        }
        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.YELLOW_CONCRETE) {
            player.velocity = player.location.direction.multiply(Config.DIR_MP_MID)
            player.velocity = player.velocity.setY(Config.SET_Y_MID)

        }
        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.RED_CONCRETE) {
            player.velocity = player.location.direction.multiply(Config.DIR_MP_BIG)
            player.velocity = player.velocity.setY(Config.SET_Y_BIG)

        }
    }
}
