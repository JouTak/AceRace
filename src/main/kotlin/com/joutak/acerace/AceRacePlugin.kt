package com.joutak.acerace

import com.joutak.acerace.commands.AceRaceCommandExecutor
import com.joutak.acerace.listeners.*
import com.joutak.acerace.worlds.World
import com.joutak.acerace.worlds.WorldManager
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class AceRacePlugin : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: AceRacePlugin
    }

    private var customConfig = YamlConfiguration()
    private fun loadConfig() {
        val fx = File(dataFolder, "config.yml")
        if (!fx.exists()) {
            saveResource("config.yml", true)
        }
    }

    override fun onEnable() {
        // Plugin startup logic
        instance = this

        loadConfig()
        WorldManager.loadWorlds()
        ZoneManager.loadZones()
        registerEvents()
        registerCommands()

        logger.info("AceRace plugin version ${pluginMeta.version} enabled!")

    }


    private fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(PlayerJumpingOnBlockListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerRunningOnBlockListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerMoveListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerJoinListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerQuitListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerChangeWorldListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerDropItemListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerInteractWithInventoryListener, this)
    }

    private fun registerCommands() {
        getCommand("ar")?.setExecutor(AceRaceCommandExecutor)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        saveConfig()
        WorldManager.saveWorlds()
        ZoneManager.saveZones()
        logger.info("AceRace plugin version ${pluginMeta.version} disabled!")
    }
}
