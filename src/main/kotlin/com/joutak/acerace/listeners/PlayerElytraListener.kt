package com.joutak.acerace.listeners

import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.players.PlayerData
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleFlightEvent

class PlayerElytraListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val to = event.to ?: return

        val worldName = to.world?.name ?: return
        if (!worldName.startsWith("AceRaceMap_")) return

        val game = GameManager.getByPlayer(player)
        if (game == null || game.getPhase() != GamePhase.RACING) return

        val data = PlayerData.get(player.uniqueId)
        if (!data.isReady() || data.isFinished()) return

        val chestplate = player.inventory.chestplate
        if (chestplate == null || chestplate.type != Material.ELYTRA) return

        if (player.isOnGround && !player.isGliding) {
            val blockBelow = to.clone().subtract(0.0, 0.1, 0.0).block
            if (blockBelow.type.isSolid) {
                removeElytra(player)
            }
        }
    }

    private fun removeElytra(player: Player) {
        val chestplate = player.inventory.chestplate
        if (chestplate != null && chestplate.type == Material.ELYTRA) {
            player.inventory.chestplate = null
            player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f)
        }
    }
}