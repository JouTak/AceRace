package com.joutak.acerace.listeners

import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.players.PlayerData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class PlayerUnderwaterListener : Listener {

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

        if (player.isInWater) {
            giveWaterBreathing(player)
        }
    }

    private fun giveWaterBreathing(player: Player) {
        val existingEffect = player.getPotionEffect(PotionEffectType.WATER_BREATHING)
        if (existingEffect != null && existingEffect.duration > 100) {
            return
        }

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.WATER_BREATHING,
                200,
                0,
                true,
                false
            )
        )
    }
}