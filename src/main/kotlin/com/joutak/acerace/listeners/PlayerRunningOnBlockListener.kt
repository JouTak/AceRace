package com.joutak.acerace.listeners

import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class PlayerRunningOnBlockListener : Listener{
    @EventHandler
    fun playerRunningOnBlockEvent(event : PlayerMoveEvent){
        val player = event.player

        if (!player.isSprinting) return
        if (player.hasPotionEffect(PotionEffectType.SPEED)) return

        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.LIGHT_BLUE_CONCRETE){
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 40, 2))
        }
    }
}
