package com.joutak.acerace.games

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.arenas.Arena
import com.joutak.acerace.arenas.ArenaManager
import com.joutak.acerace.arenas.ArenaState
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.Barriers
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.utils.LobbyReadyBossBar
import com.joutak.acerace.utils.PluginManager
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID

class Game(
    val arena: Arena,
    private val players: MutableList<UUID>,
) : Runnable {
    val uuid: UUID = UUID.randomUUID()
    private val scoreboard: GameScoreboard = GameScoreboard()
    private var phase = GamePhase.PREP
    private val spectators = mutableSetOf<UUID>()
    private var timeLeft = 1
    private var totalTime = 1
    private val onlinePlayers = mutableSetOf<UUID>()
    private val logger: GameLogger = GameLogger(this)
    private val winners = mutableSetOf<UUID>()
    private var taskId: Int = -1
    private var startTime: Long = System.currentTimeMillis()

    companion object {
        val checkRemainingPlayers = { playerUuid: UUID ->
            if (Bukkit.getPlayer(playerUuid) == null) {
                false
            } else {
                GameManager.isPlaying(playerUuid) && Bukkit.getPlayer(playerUuid)!!.gameMode == GameMode.ADVENTURE
            }
        }
    }

    fun start() {
        arena.setState(ArenaState.INGAME)
        LobbyReadyBossBar.removeAllBossBars()

        for (playerUuid in players) {
            val playerData = PlayerData.get(playerUuid)
            playerData.addGame(this.uuid)
            onlinePlayers.add(playerUuid)
            Bukkit.getPlayer(playerUuid)?.let {
                PluginManager.multiverseCore.teleportPlayer(
                    Bukkit.getConsoleSender(),
                    it,
                    Bukkit.getWorld(arena.worldName)!!.spawnLocation
                )
                LobbyManager.removeFromReadyPlayers(it)
                it.gameMode = GameMode.ADVENTURE
                scoreboard.setFor(it)
            }
        }

        LobbyReadyBossBar.checkLobby()
        setTime(Config.get(ConfigKeys.TIME_TO_START_GAME))
        logger.info("Игра началась в составе из ${players.size} игроков:\n${players.joinToString("\n")}")

        taskId =
            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AceRacePlugin.instance,
                this,
                0L,
                20L,
            )
    }

    override fun run() {
        scoreboard.update(getRemainingPlayers().count())
        scoreboard.setBossBarTimer(getAvailablePlayers(), phase, timeLeft, totalTime)

        when (phase) {
            GamePhase.PREP -> prep()
            GamePhase.START -> startCountdown()
            GamePhase.RACING -> timer()
            GamePhase.END -> finish()
        }
    }

    private fun prep() {
        for (playerUuid in onlinePlayers) {
            val playerData = PlayerData.get(playerUuid)

            playerData.setLapse(1)
            Bukkit.getPlayer(playerUuid)!!.inventory.clear()

            val item = ItemStack(Material.LEATHER_BOOTS, 1)
            val metaBoots: ItemMeta = item.itemMeta
            metaBoots.isUnbreakable = true
            item.setItemMeta(metaBoots)
            Bukkit.getPlayer(playerUuid)!!.inventory.boots = item

            val trident = ItemStack(Material.TRIDENT)
            trident.addEnchantment(Enchantment.RIPTIDE, 3)
            val metaTrident: ItemMeta = trident.itemMeta
            metaTrident.isUnbreakable = true
            trident.setItemMeta(metaTrident)
            Bukkit.getPlayer(playerUuid)!!.inventory.addItem(trident)
        }

        Barriers.setBarriers(arena)
        phase = GamePhase.START
    }

    private fun startCountdown() {
        logger.info("$timeLeft секунд до начала игры!")

        Audience.audience(onlinePlayers.mapNotNull { Bukkit.getPlayer(it) }).showTitle(
            when (timeLeft) {
                2 ->
                    Title.title(
                        LinearComponents.linear(
                            Component.text("На старт!", NamedTextColor.RED),
                        ),
                        LinearComponents.linear(),
                    )

                1 ->
                    Title.title(
                        LinearComponents.linear(
                            Component.text("Внимание!", NamedTextColor.RED),
                        ),
                        LinearComponents.linear(),
                    )

                0 ->
                    Title.title(
                        LinearComponents.linear(
                            Component.text("Побежали!", NamedTextColor.RED),
                        ),
                        LinearComponents.linear(),
                    )

                else ->
                    Title.title(LinearComponents.linear(), LinearComponents.linear())
            },
        )

        if (timeLeft > 0) {
            timeLeft--
            return
        }

        Barriers.deleteBarriers(arena)
        setTime(Config.get(ConfigKeys.TIME_TO_FINISH))
        phase = GamePhase.RACING

        startTime = System.currentTimeMillis()
    }

    private fun timer() {
        if (timeLeft > 0) {
            checkPlayers()
            timeLeft--
            return
        }

        setTime(Config.get(ConfigKeys.TIME_TO_END_GAME))
        phase = GamePhase.END

        for (playerUuid in onlinePlayers) {
            Bukkit.getPlayer(playerUuid)?.gameMode = GameMode.SPECTATOR
            if (!PlayerData.get(playerUuid).isFinished()) {
                Bukkit.getPlayer(playerUuid)?.let {
                    Audience.audience(it).showTitle(
                        Title.title(
                            LinearComponents.linear(
                                Component.text(
                                    "Ты не успел финишировать :(",
                                    NamedTextColor.RED,
                                ),
                            ),
                            LinearComponents.linear(),
                        ),
                    )
                }
            }
        }
    }

    private fun finish() {
        if (timeLeft > 0) {
            checkPlayers()
            timeLeft--
            return
        }

        stopGame()
    }

    fun checkPlayers() {
        for (playerUuid in onlinePlayers.filter {
            !Bukkit
                .getPlayer(it)
                ?.world
                ?.name
                .equals(arena.worldName)
        }) {
            logger.info("Игрок $playerUuid вышел из игры!")
            Bukkit.getPlayer(playerUuid)?.let {
                scoreboard.removeFor(it)
            }
            onlinePlayers.remove(playerUuid)
        }

        if (onlinePlayers.isEmpty() || getPlayers(checkRemainingPlayers).isEmpty()) {
            stopGame()
            return
        }

        for (playerUuid in onlinePlayers) {
            val playerData = PlayerData.get(playerUuid)

            if (playerData.getLapse() == 1) {
                val item = ItemStack(Material.LEATHER_BOOTS, 1)
                val metaBoots: ItemMeta = item.itemMeta
                metaBoots.isUnbreakable = true
                item.setItemMeta(metaBoots)
                Bukkit.getPlayer(playerUuid)!!.inventory.boots = item
            } else if (playerData.getLapse() == 2) {
                val item = ItemStack(Material.IRON_BOOTS, 1)
                val metaBoots: ItemMeta = item.itemMeta
                metaBoots.isUnbreakable = true
                item.setItemMeta(metaBoots)
                Bukkit.getPlayer(playerUuid)!!.inventory.boots = item
            } else if (playerData.getLapse() == 3) {
                val item = ItemStack(Material.DIAMOND_BOOTS, 1)
                val metaBoots: ItemMeta = item.itemMeta
                metaBoots.isUnbreakable = true
                item.setItemMeta(metaBoots)
                Bukkit.getPlayer(playerUuid)!!.inventory.boots = item
            }
            if (PlayerData.get(playerUuid)
                    .isFinished() && (playerData.getLapse() == Config.get(ConfigKeys.LAPSES_TO_FINISH))
            ) {
                val time = (System.currentTimeMillis() - startTime) / 1000
                playerData.setBestTime(System.currentTimeMillis() - startTime)
                logger.info((System.currentTimeMillis() - startTime).toString())
                Bukkit.getPlayer(playerUuid)!!.gameMode = GameMode.SPECTATOR
                playerData.setLapse(0)
                playerData.setLastCheck("0")
                Bukkit.getPlayer(playerUuid)?.let {
                    Audience.audience(it).showTitle(
                        Title.title(
                            LinearComponents.linear(
                                Component.text(
                                    "Ты финишировал за $time секунд(ы)!",
                                    NamedTextColor.GREEN,
                                ),
                            ),
                            LinearComponents.linear(
                                Component.text("Твое лучшее время: " + (playerData.getBestTime() / 1000).toString() + " секунд(ы)"),
                            ),
                        ),
                    )
                }
                Audience
                    .audience(
                        Bukkit.getServer().onlinePlayers,
                    )
                    .sendMessage(Component.text(Bukkit.getPlayer(playerUuid)!!.name + " финишировал за $time секунд(ы)!"))
            }
        }
    }

    private fun stopGame() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }

        logger.saveGameResults()

        for (playerUuid in getAvailablePlayers()) {
            if (playerUuid in onlinePlayers) {
                PlayerData.resetPlayer(playerUuid)
            }

            Bukkit.getPlayer(playerUuid)?.let {
                scoreboard.removeFor(it)

                if (playerUuid in onlinePlayers) {
                    it.gameMode = GameMode.ADVENTURE
                }
                LobbyManager.teleportToLobby(it)
            }
        }

        logger.info("Игра завершилась")

        arena.setState(ArenaState.RESETTING)
        GameManager.remove(uuid)
        logger.close()
        ArenaManager.recycle(arena)
        LobbyManager.check()
    }

    private fun getPlayers(checker: (UUID) -> Boolean): List<UUID> {
        return onlinePlayers.filter { playerUuid -> checker(playerUuid) }
    }

    private fun setTime(time: Int) {
        totalTime = time
        timeLeft = time - 1
    }

    fun getPhase(): GamePhase = this.phase

    fun addSpectator(player: Player) {
        spectators.add(player.uniqueId)
        PluginManager.multiverseCore.teleportPlayer(
            Bukkit.getConsoleSender(),
            player,
            Bukkit.getWorld(arena.worldName)!!.spawnLocation
        )
        player.gameMode = GameMode.SPECTATOR
        LobbyReadyBossBar.removeFor(player)

        player.sendMessage("Вы наблюдаете за игрой в мире ${arena.worldName}.")
    }

    fun removeSpectator(player: Player) {
        LobbyManager.teleportToLobby(player)
        spectators.remove(player.uniqueId)
        scoreboard.removeFor(player)
    }

    private fun getRemainingPlayers(): Iterable<UUID> =
        onlinePlayers.filter {
            if (Bukkit.getPlayer(it) == null) {
                false
            } else {
                Bukkit.getPlayer(it)!!.gameMode == GameMode.ADVENTURE
            }
        }

    private fun getAvailablePlayers(): Iterable<UUID> = (onlinePlayers + spectators).toSet()

    fun hasSpectator(player: Player): Boolean = spectators.contains(player.uniqueId)

    fun hasPlayer(player: Player): Boolean = hasPlayer(player.uniqueId)

    fun hasPlayer(playerUuid: UUID): Boolean = onlinePlayers.contains(playerUuid)

    fun serialize(): Map<String, Any> =
        mapOf(
            "gameUuid" to this.uuid.toString(),
            "players" to this.players.map { it.toString() },
            "winners" to this.winners.map { it.toString() },
        )
}
