package com.joutak.acerace.zones

object ZoneFactory {
    fun createZone(type: ZoneType,
                   name: String,
                   x1: Double,
                   y1: Double,
                   z1: Double,
                   x2: Double,
                   y2: Double,
                   z2: Double) : Zone {
        return when(type){
            ZoneType.BARRIER -> ZoneBarrier(name, x1, y1, z1, x2, y2, z2)
            ZoneType.ELYTRA -> ZoneGiveElytra(name, x1, y1, z1, x2, y2, z2)
            ZoneType.UNDERWATER -> ZoneUnderwaterBoost(name, x1, y1, z1, x2, y2, z2)
        }
    }

    fun createZone(
        type: ZoneType,
        name: String,
        pos1: org.bukkit.Location,
        pos2: org.bukkit.Location
    ): Zone {
        return createZone(
            type,
            name,
            pos1.x, pos1.y, pos1.z,
            pos2.x, pos2.y, pos2.z
        )
    }
}