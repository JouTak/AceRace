package com.joutak.acerace.zones

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class ZoneCheckpoint(
    name: String,
    worldName: String,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
) : Zone(ZoneType.CHECKPOINT, name, worldName, x1, y1, z1, x2, y2, z2) {

    override fun execute(player: Player){
        player.bedSpawnLocation = Location(Bukkit.getWorld(worldName), (x1+x2)/2, (y1+y2)/2, (z1+z2)/2)
    }
}
