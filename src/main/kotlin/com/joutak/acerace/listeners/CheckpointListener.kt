package com.joutak.acerace.listeners

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.checkpoints.CheckpointResult
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.GamePhase
import com.joutak.acerace.players.PlayerData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CheckpointListener (private val checkpointManager: CheckpointManager) : Listener {
    private val lastMessageTime = ConcurrentHashMap<UUID, Long>()
    private val lastMessageType = ConcurrentHashMap<UUID, String>()
    private val lastProcessed = ConcurrentHashMap<UUID, Pair<Int, Long>>()
    private val COOLDOWN = 2000L
    private val MESSAGE_COOLDOWN = 2000L

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX &&
            from.blockY == to.blockY &&
            from.blockZ == to.blockZ
        ) return

        val player = event.player

        if (player.isGliding) {
            println("🪶 [${player.name}] Летит на элитрах! Позиция: ${to.blockX}, ${to.blockY}, ${to.blockZ}")
        }

        val game = GameManager.getByPlayer(player)
        if (game == null) {
            return
        }

        if (game.getPhase() != GamePhase.RACING) {
            return
        }

        val worldName = to.world?.name ?: return
        val zones = AceRacePlugin.checkpointManager.getZonesForArena(worldName)

        val isParticipating = AceRacePlugin.checkpointManager.isParticipating(player)
        if (!isParticipating) {
            return
        }

        val now = System.currentTimeMillis()
        val lastData = lastProcessed[player.uniqueId]

        when (val result = checkpointManager.handleMove(player, to)) {
            is CheckpointResult.CheckpointPassed -> {
                if (lastData != null &&
                    lastData.first == result.checkpointIndex &&
                    now - lastData.second < COOLDOWN) {
                    return
                }
                player.sendMessage(
                    Component.text( "Чекпоинт ${result.checkpointIndex} пройден!", NamedTextColor.GREEN)
                )

                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f)
                lastMessageTime.remove(player.uniqueId)

                lastProcessed[player.uniqueId] = Pair(result.checkpointIndex, now)
                lastMessageTime.remove(player.uniqueId)
                lastMessageType.remove(player.uniqueId)
            }
            is CheckpointResult.LapCompleted -> {
                if (lastData != null && now - lastData.second < COOLDOWN) {
                    return
                }
                player.sendMessage(
                    Component.text("Круг ${result.lapDone}/${result.lapsTotal} завершён!", NamedTextColor.GREEN)
                )

                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f)
                lastMessageTime.remove(player.uniqueId)

                lastProcessed[player.uniqueId] = Pair(-1, now)
                lastMessageTime.remove(player.uniqueId)
                lastMessageType.remove(player.uniqueId)
            }

            is CheckpointResult.RaceFinished -> {
                player.sendMessage(Component.text("ГОНКА ЗАВЕРШЕНА!", NamedTextColor.GOLD))
                player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                lastMessageTime.remove(player.uniqueId)
                lastProcessed.remove(player.uniqueId)
                lastMessageTime.remove(player.uniqueId)
                lastMessageType.remove(player.uniqueId)
            }

            is CheckpointResult.WrongOrder -> {
                if (lastData != null && now - lastData.second < COOLDOWN) {
                    return
                }
                val now = System.currentTimeMillis()
                val lastTime = lastMessageTime[player.uniqueId] ?: 0
                val lastType = lastMessageType[player.uniqueId]

                if (now - lastTime > MESSAGE_COOLDOWN || lastType != "wrong") {
                    player.sendMessage(
                        Component.text( "Не туда! Пропущен ${result.required} чекпоинт", NamedTextColor.RED)
                    )
                    player.playSound(
                        player.location,
                        Sound.ENTITY_VILLAGER_NO,
                        1f, 1f
                    )
                }

                lastMessageTime[player.uniqueId] = now
                lastMessageType[player.uniqueId] = "wrong"
            }

            is CheckpointResult.NotParticipating,
            is CheckpointResult.Nothing -> Unit
        }


    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        checkpointManager.unregisterPlayer(event.player)
        lastMessageTime.remove(event.player.uniqueId)
        lastMessageType.remove(event.player.uniqueId)
    }

    private fun formatTime(ms: Long): String {
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return "%d:%02d.%03d".format(minutes, seconds, millis)
    }
}