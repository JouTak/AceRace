package com.joutak.acerace.zones

import com.joutak.acerace.AceRacePlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

abstract class Zone(
    val type: ZoneType,
    val name: String,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
) {
    val x1: Double
    val y1: Double
    val z1: Double
    val x2: Double
    val y2: Double
    val z2: Double

    init {
        this.x1 = minOf(x1, x2)
        this.x2 = maxOf(x1, x2)
        this.y1 = minOf(y1, y2)
        this.y2 = maxOf(y1, y2)
        this.z1 = minOf(z1, z2)
        this.z2 = maxOf(z1, z2)
    }

    companion object {
        fun deserialize(values: Map<String, Any>): Zone? {
            AceRacePlugin.instance.logger.info("Десериализация информации о зоне ${values["name"]}")

            return ZoneFactory.createZone(
                ZoneType.valueOf(values["type"] as String),
                values["name"] as String,
                values["x1"] as Double,
                values["y1"] as Double,
                values["z1"] as Double,
                values["x2"] as Double,
                values["y2"] as Double,
                values["z2"] as Double
            )
        }
    }

    abstract fun execute(player: Player)

    fun isInside(playerLoc: Location): Boolean {
        return playerLoc.x in this.x1..this.x2&&
                playerLoc.y in this.y1 ..this.y2 &&
                playerLoc.z in this.z1 ..this.z2
    }

    fun serialize(): Map<String, Any> {
        return mapOf(
            "type" to this.type.toString(),
            "name" to this.name,
            "x1" to this.x1,
            "y1" to this.y1,
            "z1" to this.z1,
            "x2" to this.x2,
            "y2" to this.y2,
            "z2" to this.z2
        )
    }
}
