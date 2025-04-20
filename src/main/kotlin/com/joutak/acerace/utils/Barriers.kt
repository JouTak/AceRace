package com.joutak.acerace.utils

import com.joutak.acerace.worlds.WorldManager
import com.joutak.acerace.zones.ZoneBarrier
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

object Barriers {
    private val barrierZones = ZoneManager.getZones().values.filterIsInstance<ZoneBarrier>()

    fun setBarriers(){
        for (world in WorldManager.getWorlds()) {
            if ((world.value.worldName.take(10) != "AceRaceMap") && (world.value.worldName != "lobby")) return
            for (zone in barrierZones) {
                for (x in zone.x1.toInt()..<zone.x2.toInt()) {
                    for (y in zone.y1.toInt()..<zone.y2.toInt()) {
                        for (z in zone.z1.toInt()..<zone.z2.toInt()) {
                            val newLoc =
                                Location(Bukkit.getWorld(world.value.worldName), x.toDouble(), y.toDouble(), z.toDouble())
                            newLoc.block.type = Material.BARRIER
                        }
                    }
                }
            }
        }
    }

    fun deleteBarriers() {
        for (world in WorldManager.getWorlds()){
            if ((world.value.worldName.take(10) != "AceRaceMap") && (world.value.worldName != "lobby")) return
            for (zone in barrierZones) {
                for (x in zone.x1.toInt()..<zone.x2.toInt()) {
                    for (y in zone.y1.toInt()..<zone.y2.toInt()) {
                        for (z in zone.z1.toInt()..<zone.z2.toInt()) {
                            val newLoc = Location(Bukkit.getWorld(world.value.worldName), x.toDouble(), y.toDouble(), z.toDouble())
                            newLoc.block.type = Material.AIR
                        }
                    }
                }
            }
        }
    }
}