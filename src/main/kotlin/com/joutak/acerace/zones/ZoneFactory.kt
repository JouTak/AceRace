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
}