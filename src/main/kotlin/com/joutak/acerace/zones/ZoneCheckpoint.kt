package com.joutak.acerace.zones

import com.joutak.acerace.Config
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.players.PlayerState
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.entity.Player

class ZoneCheckpoint(
    name: String,
    worldName: String,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
) : Zone(ZoneType.CHECKPOINT, name, worldName, x1, y1, z1, x2, y2, z2) {

    private var lastNameInt = 0

    override fun execute(player: Player){
        val checks = ZoneManager.getZoneCheckpoint().map { it.name.toInt() }
        val maxcheck = checks.maxOrNull() ?: 0
        if (player.gameMode != GameMode.ADVENTURE) return
        var lastCheck = PlayerData.getLastCheck(playerUuid = player.uniqueId).toInt()
        if (lastCheck == 0) {
            lastCheck = name.toInt()
            PlayerData.setLastCheck(playerUuid = player.uniqueId, lastCheck.toString())
            Audience.audience(player).showTitle(Title.title(LinearComponents.linear(Component.text("1-й круг!")), LinearComponents.linear()))
        }
        else {
            if (lastCheck == name.toInt() - 1){
                lastCheck = name.toInt()
                PlayerData.setLastCheck(playerUuid = player.uniqueId, lastCheck.toString())
                player.sendMessage("Вы достигли чекпоинта №" + (name.toInt() - 1) + "!" + " (" + PlayerData.getLapse(playerUuid = player.uniqueId).toString() + "-й круг)")
            }
            else if ((name.toInt() == 1) and (lastCheck == maxcheck)){
                if (PlayerData.getLapse(playerUuid = player.uniqueId) < Config.LAPSES_TO_FINISH){
                    PlayerData.setLapse(playerUuid = player.uniqueId, PlayerData.getLapse(playerUuid = player.uniqueId) + 1)
                    Audience.audience(player).showTitle(Title.title(LinearComponents.linear(Component.text(PlayerData.getLapse(playerUuid = player.uniqueId).toString() + "-й круг!")), LinearComponents.linear()))
                }
                else {
                    PlayerData.setState(playerUuid = player.uniqueId, PlayerState.FINISHED)
                    PlayerData.setLapse(playerUuid = player.uniqueId, 1)
                    PlayerData.setLastCheck(playerUuid = player.uniqueId, "0")
                }
                lastCheck = name.toInt()
                PlayerData.setLastCheck(playerUuid = player.uniqueId, lastCheck.toString()) }
            else if (lastNameInt != name.toInt()){
                    player.sendMessage("Вы пропустили чекпоинт!")
            }
        }

        lastNameInt = name.toInt()
    }
}
