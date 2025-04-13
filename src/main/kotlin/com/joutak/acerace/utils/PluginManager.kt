package com.joutak.acerace.utils

import com.onarandombox.MultiverseCore.MultiverseCore
import com.joutak.acerace.AceRacePlugin
import org.bukkit.Bukkit
import java.util.logging.Logger

object PluginManager {
    val acerace : AceRacePlugin = AceRacePlugin.instance
    val multiverseCore : MultiverseCore = Bukkit.getServer().pluginManager.getPlugin("Multiverse-Core") as MultiverseCore

    fun getLogger(): Logger {
        return acerace.logger
    }
}