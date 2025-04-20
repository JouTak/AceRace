package com.joutak.acerace.listeners

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.games.SpartakiadaManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent

object PlayerLoginListener : Listener {
    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        if (!Config.get(ConfigKeys.SPARTAKIADA_MODE)) {
            return
        }

        val player = event.player

        if (SpartakiadaManager.canBypass(player)) return

        if (!SpartakiadaManager.hasAttempts(player)) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_WHITELIST,
                SpartakiadaManager.KICK_NO_ATTEMPTS_MESSAGE,
            )
            return
        }

        if (!SpartakiadaManager.isParticipant(player)) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_WHITELIST,
                SpartakiadaManager.KICK_NON_PARTICIPANT_MESSAGE,
            )
            return
        }
    }
}