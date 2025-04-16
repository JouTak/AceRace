package com.joutak.acerace.games

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.Config
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.players.PlayerState
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.utils.PluginManager
import com.joutak.acerace.worlds.World
import com.joutak.acerace.worlds.WorldState
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import java.util.*

class Game(private val world: World, private val players: MutableList<UUID>) : Runnable {
    val uuid: UUID = UUID.randomUUID()
    private val scoreboard: GameScoreboard = GameScoreboard()
    private var phase = GamePhase.GIVEITEMS
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
        phase = GamePhase.GIVEITEMS
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
            GamePhase.GIVEITEMS -> giveItems()
            GamePhase.START -> startCountdown()
            GamePhase.RACING -> timer()
            GamePhase.END -> finish()
        }

    }

    private fun giveItems(){
        for (playerUuid in onlinePlayers){
            PlayerData.setLapse(playerUuid,1)

            val trident = ItemStack(Material.TRIDENT)
            trident.addEnchantment(Enchantment.RIPTIDE, 3)
            Bukkit.getPlayer(playerUuid)!!.inventory.addItem(trident)

            val item = ItemStack(Material.LEATHER_BOOTS, 1)
            item.addEnchantment(Enchantment.DEPTH_STRIDER, 3)
            Bukkit.getPlayer(playerUuid)!!.inventory.boots = item
        }

        phase = GamePhase.START
    }

    private fun startCountdown() {
        logger.info("$timeLeft секунд до начала игры!")
        Audience.audience(onlinePlayers.mapNotNull { Bukkit.getPlayer(it) }).showTitle(
            when (timeLeft) {
                2 -> Title.title(
                    LinearComponents.linear(
                        Component.text("На старт!", NamedTextColor.RED)
                    ),
                    LinearComponents.linear()
                )
                1 -> Title.title(
                    LinearComponents.linear(
                        Component.text("Внимание!", NamedTextColor.RED)
                    ),
                    LinearComponents.linear()
                )
                0 -> Title.title(
                    LinearComponents.linear(
                        Component.text("Побежали!", NamedTextColor.RED)
                    ),
                    LinearComponents.linear()
                )
                else ->
                    Title.title(LinearComponents.linear(), LinearComponents.linear())
            }
        )


        if (timeLeft > 0) {
            timeLeft--
            return
        }

        setTime(Config.TIME_TO_FINISH)
        phase = GamePhase.RACING
    }

    private fun timer() {
        if (getPlayers(checkRemainingPlayers).size == 0){
            setTime(Config.TIME_TO_END_GAME)
            phase = GamePhase.END
            return
        }

        if (timeLeft > 0) {
            checkPlayers()
            timeLeft--
            return
        }

        setTime(Config.TIME_TO_END_GAME)
        phase = GamePhase.END
    }

    private fun finish(){

        if (timeLeft > 0) {
            timeLeft--
            return
        }

        for (playerUuid in onlinePlayers){
            Bukkit.getPlayer(playerUuid)!!.gameMode = GameMode.SPECTATOR
        }

        Bukkit.getScheduler().cancelTask(taskId)
        logger.saveGameResults()

        for (playerUuid in onlinePlayers) {
            PlayerData.resetGame(playerUuid)
            Bukkit.getPlayer(playerUuid)!!.inventory.clear()

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
            PlayerData.resetGame(playerUuid)
            Bukkit.getPlayer(playerUuid)!!.inventory.clear()

            Bukkit.getPlayer(playerUuid)?.let {
                scoreboard.removeFor(it)
                LobbyManager.addPlayer(it)
            }
        }

        for (playerUuid in onlinePlayers.filter { PlayerData.get(it).isInGame() }){
            if (PlayerData.getLapse(playerUuid) == 1){
                val item = ItemStack(Material.LEATHER_BOOTS, 1)
                item.addEnchantment(Enchantment.DEPTH_STRIDER, 3)
                Bukkit.getPlayer(playerUuid)!!.inventory.boots = item
            }
            else if(PlayerData.getLapse(playerUuid) == 2){
                val item = ItemStack(Material.IRON_BOOTS, 1)
                item.addEnchantment(Enchantment.DEPTH_STRIDER, 3)
                Bukkit.getPlayer(playerUuid)!!.inventory.boots = item
            }
            else if(PlayerData.getLapse(playerUuid) == 3){
                val item = ItemStack(Material.DIAMOND_BOOTS, 1)
                item.addEnchantment(Enchantment.DEPTH_STRIDER, 3)
                Bukkit.getPlayer(playerUuid)!!.inventory.boots = item
            }
            if (PlayerData.getState(playerUuid) == PlayerState.FINISHED && PlayerData.getLapse(playerUuid) != 0){
                Bukkit.getPlayer(playerUuid)!!.gameMode = GameMode.SPECTATOR
                PlayerData.setLapse(playerUuid,0)
                Bukkit.getPlayer(playerUuid)?.let { Audience.audience(it).showTitle(Title.title(LinearComponents.linear(Component.text("Ты финишировал!")), LinearComponents.linear())) }
                Audience.audience(onlinePlayers.mapNotNull { Bukkit.getPlayer(it) }).sendMessage(Component.text(Bukkit.getPlayer(playerUuid)!!.name + " финишировал!"))
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