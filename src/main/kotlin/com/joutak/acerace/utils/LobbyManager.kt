package com.joutak.acerace.utils

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.Config
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.players.PlayerState
import com.joutak.acerace.worlds.WorldManager
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.min

object LobbyManager {
    private val world: World
    private val readyPlayers = LinkedHashSet<UUID>()
    private var gameStartTask: Int? = null

    init {
        if (Bukkit.getWorld("lobby") == null) {
            world = Bukkit.getWorlds()[0]
            PluginManager.getLogger().warning("Отсутствует мир lobby! В качестве лобби используется мир ${world.name}.")
        }
        else
            world = Bukkit.getWorld("lobby")!!

        val worldManager = PluginManager.multiverseCore.mvWorldManager
        worldManager.setFirstSpawnWorld(world.name)
        val mvWorld = worldManager.getMVWorld(world)

        mvWorld.setTime("day")
        mvWorld.setEnableWeather(false)
        mvWorld.setDifficulty(Difficulty.PEACEFUL)
        mvWorld.setGameMode(GameMode.ADVENTURE)
        mvWorld.setPVPMode(false)
        mvWorld.hunger = false

        world.setGameRule(GameRule.FALL_DAMAGE, false)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
    }

    fun addPlayer(player: Player) {
        PluginManager.multiverseCore.teleportPlayer(Bukkit.getConsoleSender(), player, world.spawnLocation)
        Audience.audience(player).sendMessage(
            LinearComponents.linear(
                Component.text("Для игры в AceRace"),
                Component.text(" введите команду "),
                Component.text("/ar ready", NamedTextColor.RED, TextDecoration.BOLD)
            )
        )
    }

    fun removePlayer(player: Player) {
        readyPlayers.remove(player.uniqueId)
    }

    fun getReadyPlayers(): List<UUID> {
        return readyPlayers.toList()
    }

    fun resetTask() {
        if (gameStartTask != null)
            Bukkit.getScheduler().cancelTask(gameStartTask!!)

        gameStartTask = null
    }

    fun check() {
        for (player in world.players)
            if (PlayerData.get(player.uniqueId).state == PlayerState.READY)
                readyPlayers.add(player.uniqueId)
            else readyPlayers.remove(player.uniqueId)

        val readyPlayersAudience = Audience.audience(readyPlayers.mapNotNull { Bukkit.getPlayer(it) }
            .slice(0..<min(readyPlayers.size, Config.MAX_PLAYERS_IN_GAME)))

        if (readyPlayers.count() >= Config.PLAYERS_TO_START && gameStartTask == null) {
            if (WorldManager.hasReadyWorld()) {
                var timeLeft = Config.TIME_TO_START_GAME_LOBBY
                gameStartTask = Bukkit.getScheduler().runTaskTimer(AceRacePlugin.instance, Runnable {
                    if (timeLeft > 0) {
                        readyPlayersAudience.sendMessage(
                            LinearComponents.linear(
                                Component.text("Ваша игра начнется через "),
                                Component.text("$timeLeft", NamedTextColor.RED),
                                Component.text(" секунд!")
                            )
                        )
                        timeLeft--
                    } else {
                        GameManager.createNewGame().start()
                        resetTask()
                    }
                }, 0L, 20L).taskId
            } else {
                readyPlayersAudience.sendMessage(
                    LinearComponents.linear(
                        Component.text("Отсутствует свободный мир, пожалуйста, подождите...")
                    )
                )
            }
        } else if (readyPlayers.count() < Config.PLAYERS_TO_START) {
            if (gameStartTask != null) {
                readyPlayersAudience.sendMessage(LinearComponents.linear(Component.text("Недостаточно игроков для начала игры!")))
                resetTask()
            }

            Audience.audience(world.players).sendMessage(
                LinearComponents.linear(
                    Component.text("Ожидание "),
                    Component.text("${Config.PLAYERS_TO_START - readyPlayers.count()}", NamedTextColor.GOLD),
                    Component.text(" игроков для начала игры.")
                )
            )
        }
    }
}