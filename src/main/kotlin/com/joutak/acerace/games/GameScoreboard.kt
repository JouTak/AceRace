package com.joutak.acerace.games

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.LinearComponents
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import java.util.*

class GameScoreboard {
    private val manager: ScoreboardManager = Bukkit.getScoreboardManager()
    private val scoreboard: Scoreboard = manager.newScoreboard
    private var bossBar: BossBar? = null
    private val objective = scoreboard.registerNewObjective("game", Criteria.DUMMY, "AceRace")

    init {
        objective.displaySlot = DisplaySlot.SIDEBAR
    }

    fun update(playersLeft: Int) {
        scoreboard.entries.forEach { scoreboard.resetScores(it) } // Очищаем старые данные

        objective.getScore("Оставшиеся игроки:").score = playersLeft
    }

    fun setBossBarTimer(playersUuids: Iterable<UUID>, phase: GamePhase, timeLeft: Int, totalTime: Int) {
        val newBossBar: BossBar? = when (phase) {
            GamePhase.GIVEITEMS -> null

            GamePhase.START,
            GamePhase.RACING,
            GamePhase.END -> BossBar.bossBar(
                LinearComponents.linear(
                    Component.text(phase.toString()),
                    Component.text(": $timeLeft сек.")
                ),
                timeLeft.toFloat() / totalTime.toFloat(),
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
            )
        }

        for (playerUuid in playersUuids) {
            val player = Bukkit.getPlayer(playerUuid) ?: continue
            if (bossBar != null)
                player.hideBossBar(bossBar!!)

            if (newBossBar != null)
                player.showBossBar(newBossBar)
        }
        bossBar = newBossBar
    }

    fun setFor(player: Player) {
        player.scoreboard = scoreboard
    }


    fun removeFor(player: Player) {
        player.scoreboard = manager.newScoreboard
        player.hideBossBar(bossBar ?: return)
    }
}
