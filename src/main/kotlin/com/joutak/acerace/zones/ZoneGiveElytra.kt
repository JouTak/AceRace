package com.joutak.acerace.zones

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
        player.velocity = player.location.direction.multiply(0.76)
        player.velocity = player.velocity.setY(0.2)
    }
}