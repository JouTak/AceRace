package com.joutak.acerace.zones

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import org.bukkit.GameMode
import org.bukkit.entity.Player

class ZoneUnderwaterBoost(
    name: String,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
) : Zone(ZoneType.UNDERWATER, name, x1, y1, z1, x2, y2, z2) {

    override fun execute(player: Player){
        if (player.gameMode != GameMode.ADVENTURE) return

        if (player.isInWater) {
            player.velocity = player.location.direction.multiply(Config.get(ConfigKeys.DIR_MP_WATER))
        }
    }

    override fun clone(): Zone {
        return ZoneUnderwaterBoost(
            name = this.name,
            x1 = this.x1,
            y1 = this.y1,
            z1 = this.z1,
            x2 = this.x2,
            y2 = this.y2,
            z2 = this.z2
        )
    }
}