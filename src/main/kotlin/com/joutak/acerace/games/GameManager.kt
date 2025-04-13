package com.joutak.acerace.games

import com.joutak.acerace.Config
import com.joutak.acerace.utils.LobbyManager
import com.joutak.acerace.worlds.WorldManager
import java.util.*
import kotlin.math.min

object GameManager {
    private val games = mutableMapOf<UUID, Game>()

    fun createNewGame(): Game {
        val world = WorldManager.getReadyWorld()!!
        val players = LobbyManager.getReadyPlayers()
            .slice(0..<min(LobbyManager.getReadyPlayers().size, Config.MAX_PLAYERS_IN_GAME))
            .toMutableList()
        val game = Game(world, players)

        games[game.uuid] = game

        return game
    }

    fun get(gameUuid: UUID?): Game? {
        return games[gameUuid]
    }

    fun remove(gameUuid: UUID) {
        games.remove(gameUuid)
    }
}