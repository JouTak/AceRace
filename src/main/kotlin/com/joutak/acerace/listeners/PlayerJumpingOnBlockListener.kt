package com.joutak.acerace.listeners

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerJumpingOnBlockListener : Listener {
    @EventHandler
    fun playerJumpingOnBlockEvent(event: PlayerJumpEvent) {
        val player = event.player

        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.LIME_CONCRETE) {
            player.velocity = player.velocity.setY(1.1)

        }
        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.YELLOW_CONCRETE) {
            player.velocity = player.location.direction.multiply(1.07)
            player.velocity = player.velocity.setY(2.1)

        }
        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.RED_CONCRETE) {
            player.velocity = player.location.direction.multiply(4)
            player.velocity = player.velocity.setY(3.5)

        }
    }
}
