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
 * /cp reload           — перезагрузить зоны из файла
 * /cp clear            — удалить все зоны
 * /cp info             — показать maxCheckpointIndex и кол-во зон
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


                val zone = CheckpointZone(index, min, max)

                checkpointManager.addZone(index, min, max)
                CheckpointConfig.saveZone(zone)
                pendingPos1.remove(sender.uniqueId)

                val count = checkpointManager.getZones()
                    .count { it.checkpointIndex == index }

                sender.ok(
                    "Зона CP $index сохранена! " +
                            "Зон для этого CP: $count. " +
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
                checkpointManager.removeZonesByIndex(index)
                CheckpointConfig.removeZonesByIndex(index)
                sender.sendMessage(
                    Component.text(
                        "Все зоны CP $index удалены. " +
                                "Финиш теперь = CP ${checkpointManager.maxCheckpointIndex}",
                        NamedTextColor.YELLOW
                    )
                )
            }

            // ------------------------------------------------------------------
            "list" -> {
                val zones = checkpointManager.getZones()
                if (zones.isEmpty()) {
                    sender.sendMessage(
                        Component.text("Зон нет!", NamedTextColor.GRAY)
                    )
                    return true
                }

                sender.sendMessage(
                    Component.text(
                        "=== Чекпоинты (финиш = CP ${checkpointManager.maxCheckpointIndex}) ===",
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
                checkpointManager.clearZones()
                val loaded = CheckpointConfig.loadAll()
                loaded.forEach {
                    checkpointManager.addZone(it.checkpointIndex, it.min, it.max)
                }
                sender.ok(
                    "Загружено ${loaded.size} зон. " +
                            "Финиш = CP ${checkpointManager.maxCheckpointIndex}"
                )
            }

            // ------------------------------------------------------------------
            "clear" -> {
                checkpointManager.clearZones()
                CheckpointConfig.clearAll()
                sender.sendMessage(
                    Component.text("Все зоны удалены!", NamedTextColor.RED)
                )
            }

            // ------------------------------------------------------------------
            "info" -> {
                val zones = checkpointManager.getZones()
                val grouped = zones.groupBy { it.checkpointIndex }
                sender.sendMessage(
                    Component.text("=== Информация ===", NamedTextColor.AQUA)
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
        1 -> listOf("setpos1", "setpos2", "remove", "list", "reload", "clear", "info")
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