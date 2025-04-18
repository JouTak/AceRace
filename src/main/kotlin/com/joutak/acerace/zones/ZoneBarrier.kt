package com.joutak.acerace.zones

import org.bukkit.entity.Player

class ZoneBarrier(
    name: String,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
) : Zone(ZoneType.BARRIER, name, x1, y1, z1, x2, y2, z2) {

    override fun execute(player: Player){
    }
}