package com.joutak.acerace.arenas

import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.utils.PluginManager
import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.World
import org.bukkit.WorldCreator
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

object ArenaManager {
    private val arenas = mutableMapOf<String, Arena>()
    private var nextArenaId = 1
    private var template: World? = null

    fun init() {
        loadTemplate()
        bootstrap()
    }

    fun get(name: String): Arena {
        return arenas[name] ?: throw IllegalArgumentException("Арены с таким именем не существует.")
    }

    fun getArenas(): Map<String, Arena> {
        return arenas
    }

    fun hasReadyArena(): Boolean {
        return arenas.values.any { it.getState() == ArenaState.READY }
    }

    fun acquireReadyArena(): Arena? {
        val arena = arenas.values.firstOrNull { it.getState() == ArenaState.READY } ?: return null
        arena.setState(ArenaState.RESERVED)
        return arena
    }

    fun recycle(arena: Arena) {
        arena.setState(ArenaState.RESETTING)
        recreateWorld(arena.worldName)
        arena.setState(ArenaState.READY)
    }

    fun shutdown() {
        clearRuntimeArenas()
        template = null
    }

    private fun loadTemplate() {
        val templateName = Config.get(ConfigKeys.ARENA_TEMPLATE_WORLD_NAME)
        template = Bukkit.getWorld(templateName) ?: Bukkit.createWorld(WorldCreator(templateName))
            ?: throw IllegalStateException("Шаблонный мир $templateName не найден! Проверь config.yml.")
        configureWorldRules(template!!)
        PluginManager.getLogger().info("Шаблонный мир '$templateName' успешно найден.")
    }

    private fun bootstrap() {
        clearRuntimeArenas()
        nextArenaId = 1

        repeat(Config.get(ConfigKeys.ARENA_POOL_SIZE)) {
            val arenaName = buildArenaName()
            recreateWorld(arenaName)
            arenas[arenaName] = Arena(arenaName)
        }
    }

    private fun buildArenaName(): String {
        val prefix = Config.get(ConfigKeys.ARENA_RUNTIME_PREFIX)
        return "${prefix}_${nextArenaId++}"
    }

    private fun clearRuntimeArenas() {
        val arenaNames = arenas.keys.toList()
        for (arenaName in arenaNames) {
            deleteWorld(arenaName)
        }
        arenas.clear()
    }

    private fun recreateWorld(worldName: String) {
        val templateWorld = template ?: throw IllegalStateException("Шаблонный мир не загружен.")
        templateWorld.save()

        deleteWorld(worldName)

        val targetFolder = getWorldFolder(worldName)
        copyWorldFolder(templateWorld.worldFolder.toPath(), targetFolder.toPath())

        val recreatedWorld = Bukkit.createWorld(WorldCreator(worldName))
            ?: throw IllegalStateException("Не удалось создать арену $worldName.")
        recreatedWorld.setAutoSave(false)
        configureWorldRules(recreatedWorld)
    }

    private fun deleteWorld(worldName: String) {
        val bukkitWorld = Bukkit.getWorld(worldName)
        if (bukkitWorld != null) {
            Bukkit.unloadWorld(bukkitWorld, false)
        }

        val worldFolder = getWorldFolder(worldName)
        if (worldFolder.exists()) {
            deleteFolder(worldFolder.toPath())
        }
    }

    private fun getWorldFolder(worldName: String): File {
        return File(Bukkit.getWorldContainer(), worldName)
    }

    private fun configureWorldRules(world: World) {
        world.difficulty = Difficulty.PEACEFUL
        world.pvp = false
        world.time = 18000
        world.setStorm(false)
        world.setThundering(false)
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
        world.setGameRule(GameRule.FALL_DAMAGE, false)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
    }

    private fun copyWorldFolder(source: Path, target: Path) {
        Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(
                dir: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                val relative = source.relativize(dir)
                Files.createDirectories(target.resolve(relative))
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(
                file: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                val fileName = file.fileName.toString()
                if (fileName == "uid.dat" || fileName == "session.lock") {
                    return FileVisitResult.CONTINUE
                }

                val relative = source.relativize(file)
                Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun deleteFolder(path: Path) {
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(
                file: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                Files.deleteIfExists(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(
                dir: Path,
                exc: IOException?,
            ): FileVisitResult {
                Files.deleteIfExists(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }
}
