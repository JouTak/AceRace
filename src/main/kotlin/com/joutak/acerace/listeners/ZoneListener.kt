package com.joutak.acerace.listeners

import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class ZoneListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to

        if (from.blockX == to.blockX &&
            from.blockY == to.blockY &&
            from.blockZ == to.blockZ
        ) return

        val player = event.player

        val worldName = to.world?.name ?: return
        if (!worldName.startsWith("AceRaceMap_")) return

        val game = GameManager.getByPlayer(player)
        if (game == null || game.getPhase() != GamePhase.RACING) return

        ZoneManager.checkPlayerZones(player)
    }
}