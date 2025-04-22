package com.joutak.acerace.players

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.games.SpartakiadaManager
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.utils.PluginManager
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class PlayerData(
    val playerUuid: UUID,
    private val games: MutableList<UUID> = mutableListOf(),
) {
    private val dataFolder: File by lazy {
        val root =
            if (Config.get(ConfigKeys.SPARTAKIADA_MODE)) {
                SpartakiadaManager.spartakiadaFolder
            } else {
                PluginManager.acerace.dataFolder
            }

        File(root, "players").apply { mkdirs() }
    }

    companion object {
        private val cache = ConcurrentHashMap<UUID, PlayerData>()

        fun get(uuid: UUID) = cache.getOrPut(uuid) { PlayerData(uuid) }

        fun reloadDatas() = cache.clear()

        fun resetPlayer(playerUuid: UUID) {
            val player = Bukkit.getPlayer(playerUuid) ?: return
            player.health = 20.0
            player.foodLevel = 20
            player.inventory.clear()
            player.level = 0
            player.exp = 0.0f

            val playerData = get(playerUuid)
            playerData.setLapse(0)
            playerData.setLastCheck("0")

        }

    }

    private var bestTime: Long = Long.MAX_VALUE
    private var lapse: Int = 0
    private var lastCheck: String = "0"
    private var isReady: Boolean = false
    private var isFinished: Boolean = false
    private val file = File(dataFolder, "${this.playerUuid}.yml")
    private val playerData: YamlConfiguration


    init {
        if (file.createNewFile()) {
            playerData = YamlConfiguration()
            playerData.set("nickname", Bukkit.getOfflinePlayer(playerUuid).name)
            playerData.set("playerUuid", playerUuid.toString())
            playerData.set("games", emptyList<String>())
            playerData.set("bestTime", Long.MAX_VALUE)
            playerData.save(file)
        } else {
            playerData = YamlConfiguration.loadConfiguration(file)
            for (game in playerData.get("games") as List<String>) {
                games.add(UUID.fromString(game))
            }
            bestTime = playerData.getLong("bestTime", Long.MAX_VALUE)
        }
    }

    fun isInLobby(): Boolean =
        Bukkit
            .getPlayer(playerUuid)
            ?.world
            ?.name
            .equals(LobbyManager.world.name)

    fun getBestTime() : Long {
        return bestTime
    }

    fun setBestTime(value: Long){
        if (getBestTime().toInt() == 0) playerData.set("bestTime", value)
        bestTime = minOf(value, getBestTime())
        playerData.set("bestTime", bestTime)
        playerData.save(file)
    }

    fun getLastCheck() : String {
        return lastCheck
    }

    fun getLapse() : Int {
        return lapse
    }

    fun setLastCheck(value: String){
        lastCheck = value
    }

    fun setLapse( value: Int){
        lapse = value
    }

    fun isReady(): Boolean = isReady

    fun isFinished(): Boolean = isFinished

    fun setFinished(finished: Boolean){
        isFinished = finished
    }

    fun setReady(ready: Boolean) {
        isReady = ready
    }


    fun addGame(gameUuid: UUID) {
        games.add(gameUuid)
        playerData.set("games", games.map { it.toString() })
        playerData.save(file)
    }

    fun getGames(): List<UUID> = games

    fun save() {
        try {
            playerData.save(file)
        } catch (e: IOException) {
            PluginManager.getLogger().severe("Ошибка при сохранении информации о игроке: ${e.message}")
        } finally {
            cache.remove(playerUuid)
        }
    }
}