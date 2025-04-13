package com.joutak.acerace.games

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.Config
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.players.PlayerState
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.utils.PluginManager
import com.joutak.acerace.worlds.World
import com.joutak.acerace.worlds.WorldState
import org.bukkit.Bukkit
import org.bukkit.GameMode
import java.util.*

class Game(private val world: World, private val players: MutableList<UUID>) : Runnable {
    val uuid: UUID = UUID.randomUUID()
    private val scoreboard: GameScoreboard = GameScoreboard()
    private var phase = GamePhase.START
    private var timeLeft = 1
    private var totalTime = 1
    private val onlinePlayers = mutableSetOf<UUID>()
    private val logger: GameLogger = GameLogger(this)
    private val winners = mutableSetOf<UUID>()
    private var taskId: Int = -1

    companion object {
        val checkRemainingPlayers = {playerUuid : UUID -> if (Bukkit.getPlayer(playerUuid) == null) false
        else PlayerData.get(playerUuid).isInGame() && Bukkit.getPlayer(playerUuid)!!.gameMode == GameMode.ADVENTURE
        }
    }

    fun start() {
        world.reset()
        world.setState(WorldState.INGAME)
        phase = GamePhase.START
        for (playerUuid in players) {
            val playerData = PlayerData.get(playerUuid)
            playerData.games.add(this.uuid)
            playerData.currentWorld= this.world
            playerData.state = PlayerState.INGAME
            onlinePlayers.add(playerUuid)
            Bukkit.getPlayer(playerUuid)?.let {
                PluginManager.multiverseCore.teleportPlayer(Bukkit.getConsoleSender(), it, Bukkit.getWorld(world.worldName)!!.spawnLocation)
                LobbyManager.removePlayer(it)
                scoreboard.setFor(it)
            }
        }

        setTime(Config.TIME_TO_START_GAME)
        logger.info("Игра началась в составе из ${players.size} игроков:\n${players.joinToString("\n")}")

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            AceRacePlugin.instance,
            this,
            0L,
            20L
        )
    }


    override fun run() {
        scoreboard.update(getPlayers(checkRemainingPlayers).size)
        scoreboard.setBossBarTimer(onlinePlayers, phase, timeLeft, totalTime)

        when (phase){
            GamePhase.START -> startCountdown()
            GamePhase.RACING -> timer()
            GamePhase.END -> finish()
        }

    }

    private fun startCountdown() {
        logger.info("$timeLeft секунд до начала игры!")

        if (timeLeft > 0) {
            timeLeft--
            return
        }
        setTime(Config.TIME_TO_FINISH)
        phase = GamePhase.RACING
    }

    private fun timer() {

        if (timeLeft > 0) {
            checkPlayers()
            timeLeft--
            return
        }

        setTime(Config.TIME_TO_END_GAME)
        phase = GamePhase.END
    }

    private fun finish(){
        for (playerUuid in onlinePlayers){
            Bukkit.getPlayer(playerUuid)!!.gameMode = GameMode.SPECTATOR
        }

        if (timeLeft > 0) {
            timeLeft--
            return
        }
        Bukkit.getScheduler().cancelTask(taskId)
        logger.saveGameResults()

        for (playerUuid in onlinePlayers) {
            PlayerData.resetGame(playerUuid)

            Bukkit.getPlayer(playerUuid)?.let {
                scoreboard.removeFor(it)
                LobbyManager.addPlayer(it)
            }
        }

        logger.info("Игра завершилась")

        world.reset()
        GameManager.remove(uuid)
    }

    private fun getPlayers(checker: (UUID) -> Boolean): List<UUID> {
        return onlinePlayers.filter { playerUuid -> checker(playerUuid) } // .also { players -> PluginManager.getLogger().info(players.toString()) }
    }

    private fun setTime(time: Int) {
        totalTime = time
        timeLeft = time - 1
    }

    fun getPhase(): GamePhase {
        return this.phase
    }

    fun checkPlayers() {
        for (playerUuid in onlinePlayers.filter { !PlayerData.get(it).isInGame() }) {
            logger.info("Игрок $playerUuid вышел из игры!")
            Bukkit.getPlayer(playerUuid)?.let {
                scoreboard.removeFor(it)
            }
            onlinePlayers.remove(playerUuid)
        }

        for (playerUuid in onlinePlayers.filter { PlayerData.get(it).isInGame() }){
            if (PlayerData.getState(playerUuid) == PlayerState.FINISHED){
                Bukkit.getPlayer(playerUuid)!!.gameMode = GameMode.SPECTATOR
            }
        }

        phase = GamePhase.RACING
    }

    fun serialize(): Map<String, Any> {
        return mapOf(
            "gameUuid" to this.uuid.toString(),
            "players" to this.players.map { it.toString() },
            "winners" to this.winners.map { it.toString() }
        )
    }
}