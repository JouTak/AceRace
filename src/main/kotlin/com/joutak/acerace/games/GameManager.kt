package com.joutak.acerace.games

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.worlds.World
import com.joutak.acerace.worlds.WorldManager
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.min

object GameManager {
    private val games = mutableMapOf<UUID, Game>()

    fun createNewGame(): Game {
        val world = WorldManager.getReadyWorld()!!
        val players = LobbyManager.getReadyPlayers()
            .slice(0..<min(LobbyManager.getReadyPlayers().size, Config.get(ConfigKeys.MAX_PLAYERS_IN_GAME)))
            .toMutableList()
        val game = Game(world, players)

        games[game.uuid] = game

        return game
    }

    fun get(gameUuid: UUID?): Game? {
        return games[gameUuid]
    }

    fun getByPlayer(player: Player): Game? {
        for (game in games.values) {
            if (game.hasPlayer(player)) {
                return game
            }
        }
        return null
    }

    fun getByWorld(world: World): Game? {
        for (game in games.values) {
            if (game.world == world) {
                return game
            }
        }
        return null
    }

    fun getBySpectator(spectator: Player): Iterable<Game> {
        val result = mutableListOf<Game>()
        for (game in games.values) {
            if (game.hasSpectator(spectator)) {
                result.add(game)
            }
        }
        return result
    }

    fun isPlaying(playerUuid: UUID): Boolean {
        for (game in games.values) {
            if (game.hasPlayer(playerUuid)) {
                return true
            }
        }
        return false
    }


    fun remove(gameUuid: UUID) {
        games.remove(gameUuid)
    }
}