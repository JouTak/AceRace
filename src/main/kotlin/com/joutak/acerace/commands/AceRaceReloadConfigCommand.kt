package com.joutak.acerace.commands

import com.joutak.acerace.config.Config
import com.joutak.acerace.games.SpartakiadaManager
import com.joutak.acerace.players.PlayerData
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import com.joutak.acerace.zones.ZoneManager

object AceRaceReloadConfigCommand : AceRaceCommand("reloadConfig", emptyList(), "acerace.admin") {
    override fun execute(
        sender: CommandSender,
        command: Command,
        string: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        if (args.isNotEmpty()) {
            return false
        }

        Config.reload()
        ZoneManager.loadZones()
        PlayerData.reloadDatas()
        SpartakiadaManager.reload()

        sender.sendMessage("Конфиги и данные AceRace перезагружены.")
        return true
    }

    override fun getTabHint(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
