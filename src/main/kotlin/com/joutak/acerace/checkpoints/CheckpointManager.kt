package com.joutak.acerace.checkpoints

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.players.PlayerData
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp

class CheckpointManager {
    private val zones: MutableList<CheckpointZone> = mutableListOf()
    private val arenaZones: MutableMap<String, MutableList<CheckpointZone>> = mutableMapOf()
    private val startTimes: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()

    private val lastLocations = ConcurrentHashMap<UUID, Location>()

    var maxCheckpointIndex: Int = 0
        private set

    fun loadZonesForArena(arenaWorldName: String, templateZones: List<CheckpointZone>) {
        val world = org.bukkit.Bukkit.getWorld(arenaWorldName) ?: run {
            println("Мир $arenaWorldName не найден при загрузке зон!")
            return
        }

        val clonedZones = templateZones.map { zone ->
            CheckpointZone(
                checkpointIndex = zone.checkpointIndex,
                min = Location(
                    world,
                    zone.min.x,
                    zone.min.y,
                    zone.min.z
                ),
                max = Location(
                    world,
                    zone.max.x,
                    zone.max.y,
                    zone.max.z
                )
            )
        }

        arenaZones[arenaWorldName] = clonedZones.toMutableList()
        println("Загружено ${clonedZones.size} зон для арены $arenaWorldName")
        clonedZones.forEachIndexed { i, zone ->
            println("  Зона $i: CP${zone.checkpointIndex}, мир: ${zone.min.world?.name}")
        }
    }

    fun getZonesForArena(arenaWorldName: String): List<CheckpointZone> {
        return arenaZones[arenaWorldName] ?: emptyList()
    }

    fun clearZonesForArena(arenaWorldName: String) {
        arenaZones.remove(arenaWorldName)
    }

    fun addZone(checkpointIndex: Int, min: Location, max: Location) {
        zones.add(CheckpointZone(checkpointIndex, min, max))
        if (checkpointIndex > maxCheckpointIndex) {
            maxCheckpointIndex = checkpointIndex
        }
    }

    fun removeZonesByIndex(checkpointIndex: Int) {
        zones.removeAll { it.checkpointIndex == checkpointIndex }
        arenaZones.values.forEach { it.removeAll { zone -> zone.checkpointIndex == checkpointIndex } }

        recalculateMax()
    }

    fun clearZones() {
        zones.clear()
        arenaZones.clear()
        maxCheckpointIndex = 0
    }

    fun getZones(): List<CheckpointZone> = zones.toList()

    private fun recalculateMax() {
        maxCheckpointIndex = zones.maxOfOrNull { it.checkpointIndex } ?: 0
    }

    fun registerPlayer(player: Player) {
        val data = PlayerData.get(player.uniqueId)

        data.setLastCheck("0")
        data.setLapse(0)
        data.setFinished(false)
        data.setMissedCheck(false)
        data.setReady(true)

        startTimes[player.uniqueId] = System.currentTimeMillis()
        lastLocations[player.uniqueId] = player.location.clone()
    }

    fun unregisterPlayer(player: Player) {
        val data = PlayerData.get(player.uniqueId)
        data.setReady(false)
        startTimes.remove(player.uniqueId)
        lastLocations.remove(player.uniqueId)
    }

    fun isParticipating(player: Player): Boolean {
        val data = PlayerData.get(player.uniqueId)
        return data.isReady() && !data.isFinished()
    }

    fun getElapsedTime(player: Player): Long {
        val start = startTimes[player.uniqueId] ?: return 0L
        return System.currentTimeMillis() - start
    }

    fun handleMove(player: Player, to: Location): CheckpointResult {
        val data = PlayerData.get(player.uniqueId)

        if (!data.isReady() || data.isFinished()) {
            return CheckpointResult.NotParticipating
        }

        val worldName = to.world?.name ?: return CheckpointResult.Nothing
        val arenaZonesForWorld = arenaZones[worldName] ?: return CheckpointResult.Nothing

        if (arenaZonesForWorld.isEmpty()) {
            return CheckpointResult.Nothing
        }

        val from = lastLocations[player.uniqueId] ?: to.clone()
        lastLocations[player.uniqueId] = to.clone()

        val currentMatchingZones = arenaZonesForWorld.filter { it.contains(to) }
        if (currentMatchingZones.isNotEmpty()) {
            val result = processZones(player, currentMatchingZones)
            if (result !is CheckpointResult.Nothing) {
                return result
            }
        }

        val rayResult = checkRayTrace(player, from, to, arenaZonesForWorld)
        if (rayResult !is CheckpointResult.Nothing) {
            return rayResult
        }

        return CheckpointResult.Nothing
    }

    private fun checkRayTrace(
        player: Player,
        from: Location,
        to: Location,
        zones: List<CheckpointZone>
    ): CheckpointResult {
        val data = PlayerData.get(player.uniqueId)

        val direction = to.toVector().subtract(from.toVector())
        val distance = direction.length()

        if (distance < 0.1) return CheckpointResult.Nothing

        val isGliding = player.isGliding
        val steps = if (isGliding) {
            (distance * 10).toInt().coerceIn(20, 100)
        } else {
            (distance * 3).toInt().coerceIn(5, 30)
        }

        val normalizedDirection = direction.clone().normalize()

        val stepSize = distance / steps

        for (i in 1..steps) {
            val progress = i.toDouble() / steps
            val checkPoint = from.clone().add(
                direction.x * progress,
                direction.y * progress,
                direction.z * progress
            )

            val matchingZones = zones.filter { it.contains(checkPoint) }
            if (matchingZones.isNotEmpty()) {
                val result = processZones(player, matchingZones)
                if (result !is CheckpointResult.Nothing) {
                    return result
                }
            }
        }

        return CheckpointResult.Nothing
    }

    private fun processZones(player: Player, matchingZones: List<CheckpointZone>): CheckpointResult {
        val data = PlayerData.get(player.uniqueId)

        val lastPassed = data.getLastCheck().toIntOrNull() ?: -1

        val sortedZones = matchingZones.sortedBy { it.checkpointIndex }

        for (zone in sortedZones) {
            if (zone.checkpointIndex <= lastPassed) {
                continue
            }

            if (lastPassed == -1) {
                if (zone.checkpointIndex == 0) {
                    data.setLastCheck("0")
                    data.setMissedCheck(false)
                    return CheckpointResult.CheckpointPassed(
                        checkpointIndex = 0,
                        nextRequired = 1
                    )
                } else {
                    data.setMissedCheck(true)
                    return CheckpointResult.WrongOrder(
                        attempted = zone.checkpointIndex,
                        required = 0
                    )
                }
            }

            val expected = lastPassed + 1

            if (zone.checkpointIndex > expected) {
                data.setMissedCheck(true)
                return CheckpointResult.WrongOrder(
                    attempted = zone.checkpointIndex,
                    required = expected
                )
            }

            if (zone.checkpointIndex == expected) {
                data.setLastCheckpointZoneId(zone.id)
                data.setLastCheck(zone.checkpointIndex.toString())

                if (zone.checkpointIndex == maxCheckpointIndex) {
                    val currentLapse = data.getLapse()
                    val lapseNeeded = Config.get(ConfigKeys.LAPSES_TO_FINISH)

                    val newLapse = currentLapse + 1
                    data.setLapse(newLapse)

                    if (newLapse >= lapseNeeded) {
                        data.setFinished(true)
                        data.setReady(false)
                        return CheckpointResult.RaceFinished
                    } else {
                        data.setLastCheck("0")
                        return CheckpointResult.LapCompleted(
                            lapDone = newLapse,
                            lapsTotal = lapseNeeded
                        )
                    }
                } else {
                    data.setLastCheck(zone.checkpointIndex.toString())
                    data.setMissedCheck(false)
                    return CheckpointResult.CheckpointPassed(
                        checkpointIndex = zone.checkpointIndex,
                        nextRequired = zone.checkpointIndex + 1
                    )
                }
            }
        }

        return CheckpointResult.Nothing
    }
}