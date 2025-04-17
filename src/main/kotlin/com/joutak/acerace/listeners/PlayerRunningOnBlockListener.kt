package com.joutak.acerace.listeners

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class PlayerRunningOnBlockListener : Listener{
    @EventHandler
    fun playerMovingOnBlockEvent(event : PlayerMoveEvent){
        val player = event.player

        if (player.hasPotionEffect(PotionEffectType.SPEED)) return

        if (player.location.block.getRelative(BlockFace.DOWN).getType() == Material.LIGHT_BLUE_CONCRETE){
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, Config.get(ConfigKeys.SPEED_DURATION), Config.get(ConfigKeys.SPEED_AMP)))
        }
    }
}
