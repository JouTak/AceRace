package com.joutak.acerace.zones

import com.joutak.acerace.Config
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.players.PlayerState
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
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

    override fun execute(player: Player){
        val checks = ZoneManager.getZoneCheckpoint().map { it.name.toInt() }
        val maxcheck = checks.maxOrNull() ?: 0
        if (player.gameMode != GameMode.ADVENTURE) return
        var lastCheck = PlayerData.getLastCheck(playerUuid = player.uniqueId).toInt()
        if (lastCheck == 0) {
            lastCheck = name.toInt()
            PlayerData.setLastCheck(playerUuid = player.uniqueId, lastCheck.toString())}
        else {
            if (lastCheck == name.toInt() - 1){
                lastCheck = name.toInt()
                PlayerData.setLastCheck(playerUuid = player.uniqueId, lastCheck.toString())
            }
            if ((name.toInt() == 1) and (lastCheck == maxcheck)){
                if (PlayerData.getLapse(playerUuid = player.uniqueId) < Config.LAPSES_TO_FINISH){
                    player.sendMessage("WOW")
                    PlayerData.setLapse(playerUuid = player.uniqueId, PlayerData.getLapse(playerUuid = player.uniqueId) + 1)
                }
                else {
                    PlayerData.setState(playerUuid = player.uniqueId, PlayerState.FINISHED)
                    PlayerData.setLapse(playerUuid = player.uniqueId, 1)
                    PlayerData.setLastCheck(playerUuid = player.uniqueId, "0")
                }
                lastCheck = name.toInt()
                PlayerData.setLastCheck(playerUuid = player.uniqueId, lastCheck.toString()) }
        }
    }
}
