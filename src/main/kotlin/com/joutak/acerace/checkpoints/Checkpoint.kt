package com.joutak.acerace.checkpoints

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.players.PlayerData
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.text.format.NamedTextColor
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

        val playerData = PlayerData.get(player.uniqueId)

        val lastName = playerData.getLastCheck()
        val lastCheck = floor(playerData.getLastCheck().toDouble()).toInt()

        val checks = CheckpointManager.getCheckpoints().values.map { floor(it.name.toDouble()).toInt() }
        val maxcheck = checks.maxOrNull() ?: 0

        val curcheckpoint = floor(name.toDouble()).toInt()

        if ((lastName != name) && (floor(name.toDouble()).toInt() == floor(lastName.toDouble()).toInt())){
            playerData.setLastCheck(name)
            player.sendMessage("Вы успешно сменили чекпоинт!")
        }

        if ((lastCheck == 0) and (GameManager.isPlaying(player.uniqueId))) {
            playerData.setLapse(1)
            playerData.setLastCheck(name)
            Audience.audience(player).showTitle(Title.title(LinearComponents.linear(Component.text("1-й круг!")), LinearComponents.linear()))
        }
        else {
            if (lastCheck == curcheckpoint - 1){
                playerData.setLastCheck(name)
                player.sendMessage("Вы достигли чекпоинта №" + (curcheckpoint - 1) + "!" + " (" + playerData.getLapse().toString() + "-й круг)")
            }
            else if ((curcheckpoint == 1) and ((lastCheck == maxcheck) or (lastCheck == 0))) {
                if (playerData.getLapse() < Config.get(ConfigKeys.LAPSES_TO_FINISH)) {
                    playerData.setLapse(
                        playerData.getLapse() + 1
                    )
                    Audience.audience(player).showTitle(
                        Title.title(
                            LinearComponents.linear(
                                Component.text(
                                    playerData.getLapse().toString() + "-й круг!"
                                )
                            ), LinearComponents.linear(),
                        )
                    )
                    playerData.setLastCheck(name)
                }
                else {
                    PlayerData.get(player.uniqueId).setFinished(true)
                }
            }
            else if (floor(lastName.toDouble()).toInt() != curcheckpoint){
                Audience.audience(player).showTitle(
                Title.title(
                    LinearComponents.linear(
                        Component.text(
                            "Вы пропустили чекпоинт!", NamedTextColor.RED
                        )
                    ), LinearComponents.linear()
                )
            )
            }
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
