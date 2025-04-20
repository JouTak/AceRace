package com.joutak.acerace.utils

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.games.GameManager
import com.joutak.acerace.games.SpartakiadaManager
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.worlds.WorldManager
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*
import kotlin.math.min

object LobbyManager {
    val world: World
    private val readyPlayers = LinkedHashSet<UUID>()
    private var gameStartTask: Int? = null
    private var timeLeft: Int =
        Config.get(
            ConfigKeys.TIME_TO_START_GAME_LOBBY,
        )

    init {
        if (Bukkit.getWorld(Config.get(ConfigKeys.LOBBY_WORLD_NAME)) == null) {
            world = Bukkit.getWorlds()[0]
            PluginManager.getLogger().warning(
                "Отсутствует мир ${Config.get(ConfigKeys.LOBBY_WORLD_NAME)}! В качестве лобби используется мир ${world.name}.",
            )
        } else {
            world = Bukkit.getWorld(Config.get(ConfigKeys.LOBBY_WORLD_NAME))!!
        }

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

    fun teleportToLobby(player: Player) {
        PluginManager.multiverseCore.teleportPlayer(
            Bukkit.getConsoleSender(),
            player,
            PluginManager.multiverseCore.mvWorldManager
                .getMVWorld(world)
                .spawnLocation,
        )
        LobbyReadyBossBar.setFor(player)
        val audience = Audience.audience(player)

        val boots = ItemStack(Material.DIAMOND_BOOTS, 1)
        val metaBoots: ItemMeta = boots.itemMeta
        metaBoots.isUnbreakable = true
        boots.setItemMeta(metaBoots)
        player.inventory.boots = boots

        val trident = ItemStack(Material.TRIDENT)
        trident.addEnchantment(Enchantment.RIPTIDE, 3)
        val metaTrident: ItemMeta = trident.itemMeta
        metaTrident.isUnbreakable = true
        trident.setItemMeta(metaTrident)
        player.inventory.addItem(trident)

        if (Config.get(ConfigKeys.SPARTAKIADA_MODE)) {
            val attempts = SpartakiadaManager.getRemainingAttempts(player)
            audience.showTitle(
                Title.title(
                    LinearComponents.linear(
                        Component.text("$attempts ", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text(if (attempts > 1) "попытки" else "попытка"),
                    ),
                    LinearComponents.linear(Component.text("Чтобы показать свое мастерство")),
                ),
            )
        }

        audience.sendMessage(
            LinearComponents.linear(
                Component.text("Для игры в AceRace введите команду "),
                Component.text("/ar ready", NamedTextColor.RED, TextDecoration.BOLD),
            ),
        )
    }

    fun removeFromReadyPlayers(player: Player) {
        readyPlayers.remove(player.uniqueId)
        PlayerData.get(player.uniqueId).setReady(false)
    }

    fun getPlayers(): List<Player> = world.players

    fun getReadyPlayers(): List<UUID> = readyPlayers.toList()

    fun getReadyPlayersAudience(): Audience =
        Audience.audience(
            readyPlayers
                .mapNotNull { Bukkit.getPlayer(it) }
                .slice(0..<min(readyPlayers.size, Config.get(ConfigKeys.MAX_PLAYERS_IN_GAME))),
        )

    fun getPlayersExceptReady(): List<Player> = world.players - readyPlayers.mapNotNull { Bukkit.getPlayer(it) }.toSet()

    fun check() {
        for (player in world.players) {
            if (PlayerData.get(player.uniqueId).isReady()) {
                readyPlayers.add(player.uniqueId)
            } else {
                readyPlayers.remove(player.uniqueId)
            }
        }

        if (readyPlayers.count() >= Config.get(ConfigKeys.PLAYERS_TO_START) && gameStartTask == null) {
            if (WorldManager.hasReadyWorld()) {
                startLobbyCountdown()
            } else {
                getReadyPlayersAudience().sendMessage(
                    LinearComponents.linear(
                        Component.text("Отсутствует свободная арена, пожалуйста, подождите..."),
                    ),
                )
            }

            return
        }

        if (readyPlayers.count() < Config.get(ConfigKeys.PLAYERS_TO_START)) {
            if (gameStartTask != null) {
                getReadyPlayersAudience().sendMessage(
                    LinearComponents.linear(
                        Component.text("Недостаточно игроков для начала игры!"),
                    ),
                )
                resetTask()
            }

            Audience.audience(world.players).sendMessage(
                LinearComponents.linear(
                    Component.text("Ожидание "),
                    Component.text(
                        "${Config.get(ConfigKeys.PLAYERS_TO_START) - readyPlayers.count()}",
                        NamedTextColor.GOLD,
                    ),
                    Component.text(" игроков для начала игры."),
                ),
            )
        }
    }

    private fun startLobbyCountdown() {
        timeLeft = Config.get(ConfigKeys.TIME_TO_START_GAME_LOBBY)

        gameStartTask =
            Bukkit
                .getScheduler()
                .runTaskTimer(
                    PluginManager.acerace,
                    Runnable {
                        if (timeLeft > 0) {
                            if (timeLeft % 5 == 0 || timeLeft <= 3) {
                                getReadyPlayersAudience().sendMessage(
                                    LinearComponents.linear(
                                        Component.text("Ваша игра начнется через "),
                                        Component.text("$timeLeft", NamedTextColor.RED),
                                        Component.text(" секунд!"),
                                    ),
                                )

                                Audience.audience(getPlayersExceptReady()).sendMessage(
                                    LinearComponents.linear(
                                        Component.text("Следующая игра начнется через "),
                                        Component.text("$timeLeft", NamedTextColor.RED),
                                        Component.text(" секунд, успейте присоединиться!"),
                                    ),
                                )
                            }

                            timeLeft--
                        } else {
                            GameManager.createNewGame().start()
                            resetTask()
                        }
                    },
                    0L,
                    20L,
                ).taskId
    }

    fun resetTask() {
        if (gameStartTask != null) {
            Bukkit.getScheduler().cancelTask(gameStartTask!!)
        }

        gameStartTask = null
    }
}