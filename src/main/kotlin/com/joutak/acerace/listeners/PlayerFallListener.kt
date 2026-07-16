package com.joutak.acerace.listeners

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.checkpoints.CheckpointZone
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.PluginManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerFallListener : Listener {

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

        val yDeath = Config.get(ConfigKeys.Y_DEATH)
        if (to.y <= yDeath) {
            teleportToLastCheckpoint(player)
            return
        }

        if (player.fireTicks > 0) {
            teleportToLastCheckpoint(player)
            return
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val game = GameManager.getByPlayer(player)
        if (game == null || game.getPhase() != GamePhase.RACING) return

        Bukkit.getScheduler().runTaskLater(AceRacePlugin.instance, Runnable {
            teleportToLastCheckpoint(player)
        }, 2L)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val player = entity
        val game = GameManager.getByPlayer(player)
        if (game == null || game.getPhase() != GamePhase.RACING) return

        val data = PlayerData.get(player.uniqueId)
        if (!data.isReady() || data.isFinished()) return

        when (event.cause) {
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.HOT_FLOOR -> {
                event.isCancelled = true
                teleportToLastCheckpoint(player)
            }
            EntityDamageEvent.DamageCause.FALL -> {
                event.isCancelled = true
                player.fallDistance = 0f
            }
            else -> {
                event.isCancelled = true
            }
        }
    }

    private fun teleportToLastCheckpoint(player: Player) {
        val data = PlayerData.get(player.uniqueId)
        val lastCheck = data.getLastCheck().toIntOrNull() ?: 0
        val lastZoneId = data.getLastCheckpointZoneId()

        player.fireTicks = 0

        val worldName = player.world.name
        val zones = AceRacePlugin.checkpointManager.getZonesForArena(worldName)
        var targetZone: CheckpointZone? = null

        if (lastZoneId.isNotEmpty()) {
            targetZone = zones.find { it.id == lastZoneId }
        }

        if (targetZone == null) {
            targetZone = zones.filter { it.checkpointIndex == lastCheck }.firstOrNull()
        }

        if (targetZone == null) {
            player.world.spawnLocation.let { player.teleport(it) }
            return
        }

        val centerX = (targetZone.min.x + targetZone.max.x) / 2
        val centerZ = (targetZone.min.z + targetZone.max.z) / 2
        val y = targetZone.min.y + 1.0

        val targetLocation = Location(
            player.world,
            centerX,
            y,
            centerZ,
            player.location.yaw,
            player.location.pitch
        )

        player.teleport(targetLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)


        player.fallDistance = 0f

        player.setNoDamageTicks(10)
    }
}