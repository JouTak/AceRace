package com.joutak.acerace

import com.joutak.acerace.arenas.ArenaManager
import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.checkpoints.CheckpointConfig
import com.joutak.acerace.commands.CheckpointCommand
import com.joutak.acerace.commands.AceRaceCommandExecutor
import com.joutak.acerace.commands.ZoneCommand
import com.joutak.acerace.games.SpartakiadaManager
import com.joutak.acerace.listeners.*
import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyReadyBossBar
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class AceRacePlugin : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: AceRacePlugin
        lateinit var checkpointManager: CheckpointManager
            private set
    }

    private var customConfig = YamlConfiguration()

    private fun loadConfig() {
        val fx = File(dataFolder, "config.yml")
        if (!fx.exists()) {
            saveResource("config.yml", true)
        }
    }

    override fun onEnable() {
        instance = this

        checkpointManager = CheckpointManager()

        val worlds = CheckpointConfig.getAllWorlds()
        if (worlds.isNotEmpty()) {
            worlds.forEach { worldName ->
                val loaded = CheckpointConfig.loadAll(worldName)
                loaded.forEach {
                    checkpointManager.addZone(it.checkpointIndex, it.min, it.max)
                }

                checkpointManager.loadZonesForArena(worldName, loaded)
                logger.info("Загружено ${loaded.size} зон чекпоинтов для мира $worldName")
            }
        }

        ZoneManager.loadZones()
        logger.info("Загружено ${ZoneManager.getZones().size} шаблонных зон")

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
        ArenaManager.init(checkpointManager)
    }

    private fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(PlayerJumpingOnBlockListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerRunningOnBlockListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerJoinListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerQuitListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerChangeWorldListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerDropItemListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerInteractWithInventoryListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerGettingDamageListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerLoginListener, this)
        Bukkit.getPluginManager().registerEvents(CheckpointListener(checkpointManager), this)
        Bukkit.getPluginManager().registerEvents(ZoneListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerFallListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerElytraListener(), this)
        Bukkit.getPluginManager().registerEvents(PlayerUnderwaterListener(), this)
    }

    private fun registerCommands() {
        getCommand("ar")?.setExecutor(AceRaceCommandExecutor)
        val cpCommand = CheckpointCommand(checkpointManager)
        getCommand("cp")?.setExecutor(cpCommand)
        getCommand("cp")?.tabCompleter = cpCommand
        val zoneCommand = ZoneCommand()
        getCommand("zone")?.setExecutor(zoneCommand)
        getCommand("zone")?.tabCompleter = zoneCommand
    }

    override fun onDisable() {
        SpartakiadaManager.stopWatching()
        for (player in Bukkit.getOnlinePlayers()) {
            PlayerData.get(player.uniqueId).save()
        }
        saveConfig()
        ArenaManager.shutdown()
        ZoneManager.saveZones()
        logger.info("AceRace plugin version ${pluginMeta.version} disabled!")
    }
}
