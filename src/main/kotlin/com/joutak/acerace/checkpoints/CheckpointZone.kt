package com.joutak.acerace.checkpoints

import org.bukkit.Location
import java.util.UUID

data class CheckpointZone(
    val checkpointIndex: Int,
    val min: Location,
    val max: Location
) {
    val id: String = UUID.randomUUID().toString()

    fun contains(location: Location): Boolean {
        if (location.world?.name != min.world?.name) return false
        val buffer = if (location.world?.players?.any { it.isGliding } == true) {
            2.0
        } else {
            0.0
        }


        val minX = minOf(min.x, max.x) - buffer
        val maxX = maxOf(min.x, max.x) + buffer
        val minY = minOf(min.y, max.y) - buffer
        val maxY = maxOf(min.y, max.y) + buffer
        val minZ = minOf(min.z, max.z) - buffer
        val maxZ = maxOf(min.z, max.z) + buffer

        val EPSILON = 0.001
        return location.x >= minX - EPSILON && location.x <= maxX + EPSILON &&
                location.y >= minY - EPSILON && location.y <= maxY + EPSILON &&
                location.z >= minZ - EPSILON && location.z <= maxZ + EPSILON
    }
}