package com.joutak.acerace.utils

import com.joutak.acerace.zones.ZoneBarrier
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

object Barriers {
    val barrierZones = ZoneManager.getZones().values.filterIsInstance<ZoneBarrier>()

    fun setBarriers(){
        for (zone in barrierZones){
            for (x in zone.x1.toInt()..<zone.x2.toInt()){
                for (y in zone.y1.toInt()..<zone.y2.toInt()){
                    for (z in zone.z1.toInt()..<zone.z2.toInt()){
                        val newLoc = Location(Bukkit.getWorld(zone.worldName),  x.toDouble(), y.toDouble(),  z.toDouble())
                        newLoc.block.type = Material.BARRIER
                    }
                }
            }
        }
    }

    fun deleteBarriers() {
        for (zone in barrierZones) {
            for (x in zone.x1.toInt()..<zone.x2.toInt()) {
                for (y in zone.y1.toInt()..<zone.y2.toInt()) {
                    for (z in zone.z1.toInt()..<zone.z2.toInt()) {
                        val newLoc = Location(Bukkit.getWorld(zone.worldName), x.toDouble(), y.toDouble(), z.toDouble())
                        newLoc.block.type = Material.AIR
                    }
                }
            }
        }
    }
}