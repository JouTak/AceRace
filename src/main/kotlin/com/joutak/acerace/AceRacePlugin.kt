package com.joutak.acerace

import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.commands.AceRaceCommandExecutor
import com.joutak.acerace.games.SpartakiadaManager
import com.joutak.acerace.listeners.*
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyReadyBossBar
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

        loadData()
        loadConfig()
        SpartakiadaManager.watchParticipantsChanges()
        LobbyReadyBossBar.removeAllBossBars()
        LobbyReadyBossBar.checkLobby()
        registerEvents()
        registerCommands()

        logger.info("AceRace plugin version ${pluginMeta.version} enabled!")

    }

    private fun loadData() {
        PlayerData.reloadDatas()
        WorldManager.loadWorlds()
        ZoneManager.loadZones()
        CheckpointManager.loadCheckpoints()
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
        Bukkit.getPluginManager().registerEvents(PlayerGettingDamageListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerLoginListener, this)
    }

    private fun registerCommands() {
        getCommand("ar")?.setExecutor(AceRaceCommandExecutor)
    }

    override fun onDisable() {
        // Plugin shutdown logic\
        SpartakiadaManager.stopWatching()
        for (player in Bukkit.getOnlinePlayers()) {
            PlayerData.get(player.uniqueId).save()
        }
        saveConfig()
        WorldManager.saveWorlds()
        ZoneManager.saveZones()
        CheckpointManager.saveCheckpoints()
        logger.info("AceRace plugin version ${pluginMeta.version} disabled!")
    }
}
