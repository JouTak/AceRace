package com.joutak.acerace.arenas

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.config.Config
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.utils.PluginManager
import com.joutak.acerace.zones.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.World
import org.bukkit.WorldCreator
import org.mvplugins.multiverse.core.MultiverseCore
import org.mvplugins.multiverse.core.MultiverseCoreApi
import org.mvplugins.multiverse.core.world.options.CloneWorldOptions
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions
import org.mvplugins.multiverse.core.world.options.LoadWorldOptions
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ArenaManager {
    private val arenas = mutableMapOf<String, Arena>()
    private var nextArenaId = 1
    private var template: World? = null
    private lateinit var checkpointManager: CheckpointManager
    private lateinit var multiverseCore: MultiverseCore

    fun init(checkpointMgr: CheckpointManager) {
        val mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core") as? MultiverseCore
        if (mv == null) {
            PluginManager.getLogger().severe("Multiverse-Core не найден! AceRace не сможет создавать арены.")
            return
        }
        multiverseCore = mv

        this.checkpointManager = checkpointMgr
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
        checkpointManager.clearZonesForArena(arena.worldName)
        recreateWorld(arena.worldName)
        arena.setState(ArenaState.READY)
    }

    fun shutdown() {
        clearRuntimeArenas()
        template = null
    }

    private fun loadTemplate() {
        val templateName = Config.get(ConfigKeys.ARENA_TEMPLATE_WORLD_NAME)

        val coreApi = MultiverseCoreApi.get()
        val worldManager = coreApi.worldManager
        var mvWorldOption = worldManager.getWorld(templateName)

        if (mvWorldOption.isDefined) {
            // Мир зарегистрирован в Multiverse - пробуем загрузить
            val mvWorld = mvWorldOption.get()
            val loadedWorld = Bukkit.getWorld(templateName)

            if (loadedWorld != null) {
                template = loadedWorld
                configureWorldRules(loadedWorld)
                PluginManager.getLogger().info("Шаблонный мир '$templateName' уже загружен.")
                return
            } else {
                // Мир зарегистрирован, но не загружен - загружаем через Multiverse
                PluginManager.getLogger().info("Загружаем шаблонный мир '$templateName' через Multiverse...")
                val loadLatch = CountDownLatch(1)
                var loadSuccess = false

                worldManager.loadWorld(LoadWorldOptions.world(mvWorld))
                    .onSuccess { loadedWorld ->
                        template = Bukkit.getWorld(loadedWorld.name)
                        if (template != null) {
                            configureWorldRules(template!!)
                            loadSuccess = true
                            PluginManager.getLogger().info("Шаблонный мир '$templateName' загружен.")
                        }
                        loadLatch.countDown()
                    }
                    .onFailure { failure ->
                        PluginManager.getLogger().severe("Не удалось загрузить шаблонный мир: ${failure.failureMessage}")
                        loadLatch.countDown()
                    }

                try {
                    if (!loadLatch.await(10, TimeUnit.SECONDS) || !loadSuccess) {
                        PluginManager.getLogger().warning("Не удалось загрузить мир через Multiverse, пробуем создать заново...")
                        // Если не удалось загрузить, удаляем и создаем заново
                        deleteWorld(templateName)
                        createNewTemplateWorld(templateName, worldManager)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    createNewTemplateWorld(templateName, worldManager)
                }
                return
            }
        }

        // Мир не зарегистрирован в Multiverse
        val worldFolder = File(Bukkit.getWorldContainer(), templateName)

        // Проверяем, существует ли папка мира
        if (worldFolder.exists() && worldFolder.isDirectory) {
            PluginManager.getLogger().info("Найдена папка шаблонного мира '$templateName'. Импортируем в Multiverse...")

            // Импортируем мир в Multiverse
            val importOptions = ImportWorldOptions.worldName(templateName)
                .environment(World.Environment.NORMAL)

            val importLatch = CountDownLatch(1)
            var importSuccess = false

            worldManager.importWorld(importOptions)
                .onSuccess { importedWorld ->
                    importSuccess = true
                    template = Bukkit.getWorld(importedWorld.name)
                    if (template != null) {
                        configureWorldRules(template!!)
                        PluginManager.getLogger().info("Шаблонный мир '$templateName' импортирован.")
                    }
                    importLatch.countDown()
                }
                .onFailure { failure ->
                    PluginManager.getLogger().severe("Не удалось импортировать шаблонный мир: ${failure.failureMessage}")
                    importLatch.countDown()
                }

            try {
                if (!importLatch.await(10, TimeUnit.SECONDS) || !importSuccess) {
                    PluginManager.getLogger().warning("Не удалось импортировать мир, создаем новый...")
                    // Если не удалось импортировать, удаляем папку и создаем заново
                    deleteWorld(templateName)
                    createNewTemplateWorld(templateName, worldManager)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                createNewTemplateWorld(templateName, worldManager)
            }
        } else {
            // Нет ни папки, ни регистрации - создаем новый мир
            createNewTemplateWorld(templateName, worldManager)
        }
    }

    private fun createNewTemplateWorld(templateName: String, worldManager: org.mvplugins.multiverse.core.world.WorldManager) {
        PluginManager.getLogger().info("Создаем новый шаблонный мир '$templateName'...")

        // Удаляем старую папку если есть
        val worldFolder = File(Bukkit.getWorldContainer(), templateName)
        if (worldFolder.exists()) {
            worldFolder.deleteRecursively()
        }

        // Создаем мир через Bukkit
        val newWorld = Bukkit.createWorld(WorldCreator(templateName))
        if (newWorld == null) {
            throw IllegalStateException("Не удалось создать шаблонный мир $templateName!")
        }

        // Импортируем в Multiverse
        val importOptions = ImportWorldOptions.worldName(templateName)
            .environment(newWorld.environment)

        val importLatch = CountDownLatch(1)
        var importSuccess = false

        worldManager.importWorld(importOptions)
            .onSuccess { importedWorld ->
                importSuccess = true
                template = Bukkit.getWorld(importedWorld.name)
                if (template != null) {
                    configureWorldRules(template!!)
                    PluginManager.getLogger().info("Шаблонный мир '$templateName' создан и импортирован.")
                }
                importLatch.countDown()
            }
            .onFailure { failure ->
                PluginManager.getLogger().severe("Не удалось импортировать созданный мир: ${failure.failureMessage}")
                importLatch.countDown()
            }

        try {
            if (!importLatch.await(10, TimeUnit.SECONDS) || !importSuccess) {
                // Если не удалось импортировать, используем мир как есть
                template = newWorld
                configureWorldRules(newWorld)
                PluginManager.getLogger().warning("Мир создан, но не зарегистрирован в Multiverse. Возможны проблемы.")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            template = newWorld
            configureWorldRules(newWorld)
        }
    }

    private fun registerInMultiverse(worldName: String) {
        val coreApi = MultiverseCoreApi.get()
        val worldManager = coreApi.worldManager
        val mvWorldOption = worldManager.getWorld(worldName)

        if (mvWorldOption.isEmpty) {
            PluginManager.getLogger().info("Импортируем шаблонный мир '$worldName' в Multiverse...")
            val importOptions = ImportWorldOptions.worldName(worldName)
                .environment(template?.environment ?: World.Environment.NORMAL)

            val latch = CountDownLatch(1)
            var success = false

            worldManager.importWorld(importOptions)
                .onSuccess { importedWorld ->
                    success = true
                    PluginManager.getLogger().info("Шаблонный мир '$worldName' зарегистрирован в Multiverse.")
                    latch.countDown()
                }
                .onFailure { failure ->
                    PluginManager.getLogger().severe("Не удалось импортировать шаблонный мир: ${failure.failureMessage}")
                    latch.countDown()
                }

            try {
                if (!latch.await(10, TimeUnit.SECONDS) || !success) {
                    PluginManager.getLogger().severe("Таймаут импорта шаблонного мира $worldName")
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        } else {
            PluginManager.getLogger().info("Шаблонный мир '$worldName' уже зарегистрирован в Multiverse.")
        }
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
        val templateWorld = template
            ?: throw IllegalStateException("Шаблонный мир не загружен.")
        templateWorld.save()

        checkpointManager.clearZonesForArena(worldName)
        ZoneManager.clearZonesForArena(worldName)

        deleteWorld(worldName)

        val coreApi = MultiverseCoreApi.get();
        val worldManager = coreApi.worldManager

        var mvTemplateOption = worldManager.getWorld(templateWorld.name)
        if (mvTemplateOption.isEmpty) {
            // Если мир не зарегистрирован, импортируем его
            PluginManager.getLogger().info("Импортируем шаблонный мир перед клонированием...")
            val importOptions = ImportWorldOptions.worldName(templateWorld.name)
                .environment(templateWorld.environment)

            val importLatch = CountDownLatch(1)
            var importSuccess = false

            worldManager.importWorld(importOptions)
                .onSuccess { importedWorld ->
                    importSuccess = true
                    importLatch.countDown()
                }
                .onFailure { failure ->
                    PluginManager.getLogger().severe("Не удалось импортировать шаблонный мир: ${failure.failureMessage}")
                    importLatch.countDown()
                }

            try {
                if (!importLatch.await(5, TimeUnit.SECONDS) || !importSuccess) {
                    throw IllegalStateException("Не удалось импортировать шаблонный мир")
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("Импорт прерван", e)
            }

            mvTemplateOption = worldManager.getWorld(templateWorld.name)
        }

        val mvTemplate = mvTemplateOption.getOrNull()
            ?: throw IllegalStateException("Шаблонный мир не зарегистрирован в Multiverse")

        val cloneOptions = CloneWorldOptions.fromTo(mvTemplate, worldName)
            .keepWorldConfig(true)
            .keepGameRule(false)
            .keepWorldBorder(true)
            .saveBukkitWorld(true)

        val cloneLatch = CountDownLatch(1)
        var cloneSuccess = false

        worldManager.cloneWorld(cloneOptions)
            .onSuccess { newWorld ->
                cloneSuccess = true
                PluginManager.getLogger().info("Мир $worldName успешно склонирован")
                cloneLatch.countDown()
            }
            .onFailure { failure ->
                PluginManager.getLogger().severe("Не удалось клонировать мир: ${failure.failureMessage}")
                cloneLatch.countDown()
            }

        try {
            if (!cloneLatch.await(10, TimeUnit.SECONDS)) {
                throw IllegalStateException("Таймаут клонирования мира $worldName")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Клонирование прервано", e)
        }

        if (!cloneSuccess) {
            throw IllegalStateException("Не удалось клонировать мир")
        }

        var recreatedWorld: World? = Bukkit.getWorld(worldName)
        if (recreatedWorld == null) {
            // Пробуем загрузить мир
            val mvNewWorldOption = worldManager.getWorld(worldName)
            if (mvNewWorldOption.isDefined) {
                val loadLatch = CountDownLatch(1)
                worldManager.loadWorld(LoadWorldOptions.world(mvNewWorldOption.get()))
                    .onSuccess { loadedWorld ->
                        recreatedWorld = Bukkit.getWorld(loadedWorld.name)
                        loadLatch.countDown()
                    }
                    .onFailure { failure ->
                        PluginManager.getLogger().severe("Не удалось загрузить мир: ${failure.failureMessage}")
                        loadLatch.countDown()
                    }

                try {
                    if (!loadLatch.await(5, TimeUnit.SECONDS)) {
                        throw IllegalStateException("Таймаут загрузки мира $worldName")
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IllegalStateException("Загрузка прервана", e)
                }
            }
        }

        val finalWorld = recreatedWorld ?: throw IllegalStateException("Не удалось создать арену $worldName.")

        finalWorld.setAutoSave(false)
        configureWorldRules(finalWorld)

        val templateZones = checkpointManager.getZones()
        checkpointManager.loadZonesForArena(worldName, templateZones)

        ZoneManager.loadZonesForArena(worldName)

        println("Арена $worldName: чекпоинтов=${templateZones.size}, зон=${ZoneManager.getZonesForArena(worldName).size}")
    }

    private fun deleteWorld(worldName: String) {
        val bukkitWorld = Bukkit.getWorld(worldName)
        if (bukkitWorld != null) {
            Bukkit.unloadWorld(bukkitWorld, false)
        }

        val coreApi = MultiverseCoreApi.get();
        val worldManager = coreApi.worldManager
        val mvWorldOption = worldManager.getWorld(worldName)

        if (mvWorldOption.isDefined) {
            try {
                val deleteOptions = DeleteWorldOptions.world(mvWorldOption.get())
                worldManager.deleteWorld(deleteOptions)
            } catch (e: Exception) {
                PluginManager.getLogger().warning("Не удалось удалить мир $worldName через Multiverse: ${e.message}")
            }
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


        val coreApi = MultiverseCoreApi.get();
        val worldManager = coreApi.worldManager
        val mvWorldOption = worldManager.getWorld(world.name)
        if (mvWorldOption.isDefined) {
            val mvWorld = mvWorldOption.get()
            mvWorld.setAllowWeather(false)
            mvWorld.setDifficulty(Difficulty.PEACEFUL)
            mvWorld.setPvp(false)

            try {
                val setTimeMethod = mvWorld.javaClass.getMethod("setTime", Long::class.java)
                setTimeMethod.invoke(mvWorld, 18000L)
            } catch (_: Exception) {
            }
        }
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
