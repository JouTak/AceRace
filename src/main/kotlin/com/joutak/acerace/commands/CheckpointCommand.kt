package com.joutak.acerace.commands

import com.joutak.acerace.checkpoints.CheckpointConfig
import com.joutak.acerace.checkpoints.CheckpointManager
import com.joutak.acerace.checkpoints.CheckpointZone
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

/**
 * /cp setpos1 <index>  — встать в первый угол зоны
 * /cp setpos2 <index>  — встать во второй угол, зона сохраняется
 * /cp remove  <index>  — удалить все зоны чекпоинта
 * /cp list             — список всех зон
 * /cp save - сохранить чекпоинт зоны для этого мира
 * /cp reload           — перезагрузить зоны из файла
 * /cp clear            — удалить все зоны
 * /cp info             — показать maxCheckpointIndex и кол-во зон
 * /cp worlds           показать все миры с чекпоинт зонами
 */
class CheckpointCommand(
    private val checkpointManager: CheckpointManager
) : CommandExecutor, TabCompleter {

    // UUID игрока -> Pair(cpIndex, первый угол)
    private val pendingPos1 = mutableMapOf<UUID, Pair<Int, org.bukkit.Location>>()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Только для игроков!")
            return true
        }

        if (!sender.hasPermission("acerace.admin")) {
            sender.sendMessage(Component.text("Нет прав!", NamedTextColor.RED))
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {

            // ------------------------------------------------------------------
            "setpos1" -> {
                val index = args.getOrNull(1)?.toIntOrNull()
                if (index == null) {
                    sender.err("Использование: /cp setpos1 <index>")
                    return true
                }
                pendingPos1[sender.uniqueId] = Pair(index, sender.location.clone())
                sender.ok(
                    "Pos1 для CP $index: " +
                            "${sender.location.blockX}, " +
                            "${sender.location.blockY}, " +
                            "${sender.location.blockZ}"
                )
            }

            // ------------------------------------------------------------------
            "setpos2" -> {
                val index = args.getOrNull(1)?.toIntOrNull()
                if (index == null) {
                    sender.err("Использование: /cp setpos2 <index>")
                    return true
                }

                val pos1Data = pendingPos1[sender.uniqueId]
                if (pos1Data == null || pos1Data.first != index) {
                    sender.err("Сначала установи /cp setpos1 $index")
                    return true
                }

                val min = pos1Data.second
                val max = sender.location.clone()

                if (min.world?.name != max.world?.name) {
                    sender.err("Нельзя использовать точки из разных миров!")
                    pendingPos1.remove(sender.uniqueId)
                    return true
                }

                val zone = CheckpointZone(index, min, max)
                val worldName = sender.world.name

                checkpointManager.addZone(index, min, max)
                CheckpointConfig.saveZone(zone, worldName)
                pendingPos1.remove(sender.uniqueId)

                val count = checkpointManager.getZones()
                    .count { it.checkpointIndex == index }

                sender.ok(
                    "Зона CP $index сохранена в мире $worldName! " +
                            "Зон для этого CP: $count. " +
                            "Финиш = CP ${checkpointManager.maxCheckpointIndex}"
                )
            }

            // ------------------------------------------------------------------

            "save" -> {
                val worldName = sender.world.name
                val zones = checkpointManager.getZones()
                if (zones.isEmpty()) {
                    sender.err("Нет зон для сохранения!")
                    return true
                }

                // Сначала очищаем старые зоны для этого мира
                checkpointManager.clearZonesForArena(worldName)
                CheckpointConfig.clearAll(worldName)

                // Сохраняем все зоны
                var savedCount = 0
                zones.forEach { zone ->
                    CheckpointConfig.saveZone(zone, worldName)
                    savedCount++
                }

                sender.ok("Сохранено $savedCount зон для мира $worldName!")
            }

            // ------------------------------------------------------------------

            "load" -> {
                val worldName = sender.world.name
                checkpointManager.clearZones()
                val loaded = CheckpointConfig.loadAll(worldName)
                loaded.forEach {
                    checkpointManager.addZone(it.checkpointIndex, it.min, it.max)
                }
                sender.ok(
                    "Загружено ${loaded.size} зон из мира $worldName. " +
                            "Финиш = CP ${checkpointManager.maxCheckpointIndex}"
                )
            }

            // ------------------------------------------------------------------
            "remove" -> {
                val index = args.getOrNull(1)?.toIntOrNull()
                if (index == null) {
                    sender.err("Использование: /cp remove <index>")
                    return true
                }
                val worldName = sender.world.name
                checkpointManager.removeZonesByIndex(index)
                CheckpointConfig.removeZonesByIndex(index, worldName)
                sender.sendMessage(
                    Component.text(
                        "Все зоны CP $index удалены из мира $worldName. " +
                                "Финиш теперь = CP ${checkpointManager.maxCheckpointIndex}",
                        NamedTextColor.YELLOW
                    )
                )
            }

            // ------------------------------------------------------------------
            "list" -> {
                val worldName = sender.world.name
                val zones = checkpointManager.getZones()
                if (zones.isEmpty()) {
                    sender.sendMessage(
                        Component.text("Зон для мира $worldName нет!", NamedTextColor.GRAY)
                    )
                    return true
                }

                sender.sendMessage(
                    Component.text(
                        "=== Чекпоинты для мира $worldName (финиш = CP ${checkpointManager.maxCheckpointIndex}) ===",
                        NamedTextColor.AQUA
                    )
                )

                zones.groupBy { it.checkpointIndex }
                    .toSortedMap()
                    .forEach { (idx, zoneList) ->
                        val label = when (idx) {
                            0 -> "СТАРТ"
                            checkpointManager.maxCheckpointIndex -> "ФИНИШ"
                            else -> "CP $idx"
                        }
                        sender.sendMessage(
                            Component.text(
                                "  $label — ${zoneList.size} зон(а)",
                                NamedTextColor.WHITE
                            )
                        )
                        // Показываем координаты каждой зоны
                        zoneList.forEachIndexed { i, zone ->
                            sender.sendMessage(
                                Component.text(
                                    "    [$i] " +
                                            "min(${zone.min.blockX},${zone.min.blockY},${zone.min.blockZ}) " +
                                            "max(${zone.max.blockX},${zone.max.blockY},${zone.max.blockZ})",
                                    NamedTextColor.GRAY
                                )
                            )
                        }
                    }
            }

            // ------------------------------------------------------------------
            "reload" -> {
                val worldName = sender.world.name
                checkpointManager.clearZones()
                checkpointManager.clearZonesForArena(worldName)
                val loaded = CheckpointConfig.loadAll(worldName)
                loaded.forEach {
                    checkpointManager.addZone(it.checkpointIndex, it.min, it.max)
                    val zones = checkpointManager.getZones()
                    checkpointManager.loadZonesForArena(worldName, zones)
                }
                sender.ok(
                    "Загружено ${loaded.size} зон из мира $worldName. " +
                            "Финиш = CP ${checkpointManager.maxCheckpointIndex}"
                )
            }

            // ------------------------------------------------------------------
            "clear" -> {
                val worldName = sender.world.name
                checkpointManager.clearZones()
                checkpointManager.clearZonesForArena(worldName)
                CheckpointConfig.clearAll(worldName)
                sender.sendMessage(
                    Component.text("Все зоны для мира $worldName удалены!", NamedTextColor.RED)
                )
            }

            // ------------------------------------------------------------------
            "info" -> {
                val worldName = sender.world.name
                val zones = checkpointManager.getZones()
                val grouped = zones.groupBy { it.checkpointIndex }
                sender.sendMessage(
                    Component.text("=== Информация для мира $worldName ===", NamedTextColor.AQUA)
                )
                sender.sendMessage(
                    Component.text(
                        "Всего зон: ${zones.size}",
                        NamedTextColor.WHITE
                    )
                )
                sender.sendMessage(
                    Component.text(
                        "Уникальных CP: ${grouped.size}",
                        NamedTextColor.WHITE
                    )
                )
                sender.sendMessage(
                    Component.text(
                        "Финиш = CP ${checkpointManager.maxCheckpointIndex}",
                        NamedTextColor.WHITE
                    )
                )
            }
            // ------------------------------------------------------------------

            "worlds" -> {
                val worlds = CheckpointConfig.getAllWorlds()
                if (worlds.isEmpty()) {
                    sender.sendMessage(
                        Component.text("Нет миров с сохраненными зонами!", NamedTextColor.GRAY)
                    )
                    return true
                }

                sender.sendMessage(
                    Component.text("=== Миры с чекпоинтами ===", NamedTextColor.AQUA)
                )
                worlds.forEach { worldName ->
                    val zones = CheckpointConfig.loadAll(worldName)
                    sender.sendMessage(
                        Component.text(
                            "  $worldName — ${zones.size} зон(ы)",
                            NamedTextColor.WHITE
                        )
                    )
                }
            }

            // ------------------------------------------------------------------

            "test" -> {
                val player = sender as Player
                val world = player.world
                val location = player.location

                sender.sendMessage(Component.text("=== Тест чекпоинтов ===", NamedTextColor.AQUA))
                sender.sendMessage(Component.text("Мир: ${world.name}", NamedTextColor.WHITE))
                sender.sendMessage(Component.text("Позиция: ${location.blockX}, ${location.blockY}, ${location.blockZ}", NamedTextColor.WHITE))

                val zones = checkpointManager.getZonesForArena(world.name)
                sender.sendMessage(Component.text("Зон в мире: ${zones.size}", NamedTextColor.WHITE))

                if (zones.isNotEmpty()) {
                    zones.forEachIndexed { i, zone ->
                        val contains = zone.contains(location)
                        val status = if (contains) "ВНУТРИ" else "СНАРУЖИ"
                        sender.sendMessage(Component.text(
                            "  [$i] CP${zone.checkpointIndex}: $status | " +
                                    "(${zone.min.blockX},${zone.min.blockY},${zone.min.blockZ}) -> " +
                                    "(${zone.max.blockX},${zone.max.blockY},${zone.max.blockZ})",
                            if (contains) NamedTextColor.GREEN else NamedTextColor.GRAY
                        ))
                    }
                } else {
                    sender.sendMessage(Component.text("Нет зон для этого мира!", NamedTextColor.RED))
                    sender.sendMessage(Component.text("Шаблонных зон: ${checkpointManager.getZones().size}", NamedTextColor.YELLOW))
                }
            }

            // ------------------------------------------------------------------
            else -> {
                sender.sendMessage(
                    Component.text(
                        "/cp <setpos1|setpos2|remove|list|reload|clear|info> [index]",
                        NamedTextColor.GRAY
                    )
                )
            }
        }
        return true
    }



    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> = when (args.size) {
        1 -> listOf("setpos1", "setpos2", "remove", "list", "save", "reload", "clear", "info", "worlds", "test")
            .filter { it.startsWith(args[0].lowercase()) }
        2 -> when (args[0].lowercase()) {
            "setpos1", "setpos2", "remove" ->
                (0..20).map { it.toString() }.filter { it.startsWith(args[1]) }
            else -> emptyList()
        }
        else -> emptyList()
    }

    private fun Player.ok(msg: String) =
        sendMessage(Component.text(msg, NamedTextColor.GREEN))

    private fun Player.err(msg: String) =
        sendMessage(Component.text(msg, NamedTextColor.RED))
}