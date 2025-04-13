package com.joutak.acerace.zones

import com.joutak.acerace.Config
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ZoneGiveElytra(
    name: String,
    worldName: String,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
) : Zone(ZoneType.ELYTRA, name, worldName, x1, y1, z1, x2, y2, z2) {

    override fun execute(player: Player){

        player.getInventory().setChestplate(ItemStack(Material.ELYTRA));
        player.isGliding = true
        player.velocity = player.location.direction.multiply(Config.DIR_MP_ELYTRA)
        player.velocity = player.velocity.setY(Config.SET_Y_ELYTRA)
    }
}