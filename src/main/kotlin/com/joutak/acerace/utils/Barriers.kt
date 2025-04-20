package com.joutak.acerace.utils

import com.joutak.acerace.worlds.World
import com.joutak.acerace.worlds.WorldManager
import com.joutak.acerace.zones.ZoneBarrier
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

object Barriers  {
    private val barrierZones = ZoneManager.getZones().values.filterIsInstance<ZoneBarrier>()

    fun setBarriers(world: World){
            for (zone in barrierZones) {
                for (x in zone.x1.toInt()..<zone.x2.toInt()) {
                    for (y in zone.y1.toInt()..<zone.y2.toInt()) {
                        for (z in zone.z1.toInt()..<zone.z2.toInt()) {
                            val newLoc =
                                Location(Bukkit.getWorld(world.worldName), x.toDouble(), y.toDouble(), z.toDouble())
                            newLoc.block.type = Material.BARRIER
                        }
                    }

            }
        }
    }

    fun deleteBarriers(world: World) {
            for (zone in barrierZones) {
                for (x in zone.x1.toInt()..<zone.x2.toInt()) {
                    for (y in zone.y1.toInt()..<zone.y2.toInt()) {
                        for (z in zone.z1.toInt()..<zone.z2.toInt()) {
                            val newLoc = Location(Bukkit.getWorld(world.worldName), x.toDouble(), y.toDouble(), z.toDouble())
                            newLoc.block.type = Material.AIR
                        }
                    }
                }
            }

    }
}