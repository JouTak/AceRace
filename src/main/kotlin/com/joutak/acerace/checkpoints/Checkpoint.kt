package com.joutak.acerace.checkpoints

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.players.PlayerState
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.math.floor

class Checkpoint(
    val name: String,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double,
    val yaw: Float,
    val pitch: Float
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
        fun deserialize(values: Map<String, Any>): Checkpoint {
            AceRacePlugin.instance.logger.info("Десериализация информации о чекпоинте ${values["name"]}")

            return Checkpoint(
                values["name"] as String,
                values["x1"] as Double,
                values["y1"] as Double,
                values["z1"] as Double,
                values["x2"] as Double,
                values["y2"] as Double,
                values["z2"] as Double,
                (values["yaw"] as Double).toFloat(),
                (values["pitch"] as Double).toFloat()
            )
        }
    }

    fun execute(player: Player) {
        if (player.gameMode != GameMode.ADVENTURE) return

        val lastName = PlayerData.getLastCheck(playerUuid = player.uniqueId)

        val checks = CheckpointManager.getCheckpoints().values.map { floor(it.name.toDouble()).toInt() }
        val maxcheck = checks.maxOrNull() ?: 0
        val lastCheck = floor(PlayerData.getLastCheck(playerUuid = player.uniqueId).toDouble()).toInt()
        val checkpoint = floor(name.toDouble()).toInt()

        if ((lastName != name) && (floor(name.toDouble()).toInt() == floor(lastName.toDouble()).toInt())){
            PlayerData.setLastCheck(playerUuid = player.uniqueId, name)
        }

        if ((lastCheck == 0) and (PlayerData.getState(playerUuid = player.uniqueId) == PlayerState.INGAME)) {
            PlayerData.setLapse(playerUuid = player.uniqueId, 1)
            PlayerData.setLastCheck(playerUuid = player.uniqueId, name)
            Audience.audience(player).showTitle(Title.title(LinearComponents.linear(Component.text("1-й круг!")), LinearComponents.linear()))
        }
        else {
            if (lastCheck == checkpoint - 1){
                PlayerData.setLastCheck(playerUuid = player.uniqueId, name)
                player.sendMessage("Вы достигли чекпоинта №" + (checkpoint - 1) + "!" + " (" + PlayerData.getLapse(playerUuid = player.uniqueId).toString() + "-й круг)")
            }
            else if ((checkpoint == 1) and ((lastCheck == maxcheck) or (lastCheck == 0))) {
                if (PlayerData.getLapse(playerUuid = player.uniqueId) < Config.get(ConfigKeys.LAPSES_TO_FINISH)) {
                    PlayerData.setLapse(
                        playerUuid = player.uniqueId,
                        PlayerData.getLapse(playerUuid = player.uniqueId) + 1
                    )
                    Audience.audience(player).showTitle(
                        Title.title(
                            LinearComponents.linear(
                                Component.text(
                                    PlayerData.getLapse(playerUuid = player.uniqueId).toString() + "-й круг!"
                                )
                            ), LinearComponents.linear()
                        )
                    )
                    PlayerData.setLastCheck(playerUuid = player.uniqueId, name)
                }
                else {
                    PlayerData.setState(playerUuid = player.uniqueId, PlayerState.FINISHED)
                }
            }
//            else if (floor(lastName.toDouble()).toInt() != checkpoint){
//                player.sendMessage("Вы пропустили чекпоинт!")
//            }
        }
    }

    fun isInside(playerLoc: Location): Boolean {
        return playerLoc.x in this.x1..this.x2&&
                playerLoc.y in this.y1 ..this.y2 &&
                playerLoc.z in this.z1 ..this.z2
    }

    fun serialize(): Map<String, Any> {
        return mapOf(
            "name" to this.name,
            "x1" to this.x1,
            "y1" to this.y1,
            "z1" to this.z1,
            "x2" to this.x2,
            "y2" to this.y2,
            "z2" to this.z2,
            "yaw" to this.yaw,
            "pitch" to this.pitch
        )
    }
}
