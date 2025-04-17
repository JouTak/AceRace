package com.joutak.acerace.games

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.utils.PluginManager
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class GameLogger(val game: Game) {
    companion object {
        private val dataFolder = File(AceRacePlugin.instance.dataFolder.path, "games")
        private val winnersFile = File(AceRacePlugin.instance.dataFolder.path, "winners.yml")
    }

    private val logger = Logger.getLogger("GAME/${game.uuid}")

    private val gameFolder: File = File(dataFolder, game.uuid.toString())
    private val resultFile = File(gameFolder, "${game.uuid}.yml")
    private val logFile = File(gameFolder, "${game.uuid}.log")

    init {
        gameFolder.mkdirs()
        winnersFile.createNewFile()
        resultFile.createNewFile()
        logFile.createNewFile()

        val fileHandler = FileHandler(logFile.absolutePath, true)
        fileHandler.formatter = object : SimpleFormatter() {
            override fun format(record: LogRecord): String {
                val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(record.millis)
                return "[$timestamp ${record.level}] [${AceRacePlugin.instance.pluginMeta.name}] [GAME/${game.uuid}] ${record.message}\n"
            }
        }

        logger.setParent(PluginManager.getLogger())
        logger.addHandler(fileHandler)
    }

    fun info(msg: String) {
        logger.useParentHandlers = Config.get(ConfigKeys.LOG_INFO_TO_CONSOLE)
        logger.info(msg)
    }

    fun warning(msg: String) {
        logger.useParentHandlers = true
        logger.warning(msg)
    }

    fun severe(msg: String) {
        logger.useParentHandlers = true
        logger.severe(msg)
    }

    fun saveGameResults() {
        val gameData = YamlConfiguration()

        for ((path, value) in game.serialize()) {
            gameData.set(path, value)
        }

        try {
            gameData.save(resultFile)
        } catch (e: IOException) {
            PluginManager.getLogger().severe("Ошибка при сохранении информации об игре: ${e.message}")
        }


    }

    fun addWinners(winners: Iterable<UUID>) {
        winnersFile.appendText(winners.joinToString("\n"))
    }
}