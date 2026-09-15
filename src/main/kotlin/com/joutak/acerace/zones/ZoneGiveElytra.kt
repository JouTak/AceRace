package com.joutak.acerace.zones

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKey
import com.joutak.acerace.config.ConfigKeys
import org.bukkit.Bukkit
import org.bukkit.GameMode
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

    override fun execute(player: Player) {
        if (player.gameMode != GameMode.ADVENTURE) return

        if (player.inventory.chestplate != ItemStack(Material.ELYTRA)) player.inventory.chestplate = ItemStack(Material.ELYTRA)

        player.isGliding = true

        val direction = player.location.direction
        val speed = Config.get(ConfigKeys.DIR_MP_ELYTRA)
        val ySpeed = Config.get(ConfigKeys.SET_Y_ELYTRA)

        player.velocity = direction.multiply(speed).setY(ySpeed)
    }

    override fun clone(newWorldName: String): Zone {
        return ZoneGiveElytra(
            name = this.name,
            worldName = newWorldName,
            x1 = this.x1,
            y1 = this.y1,
            z1 = this.z1,
            x2 = this.x2,
            y2 = this.y2,
            z2 = this.z2
        )
    }
}
