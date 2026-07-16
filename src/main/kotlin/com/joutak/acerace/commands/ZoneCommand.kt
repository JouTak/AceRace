package com.joutak.acerace.commands

import com.joutak.acerace.arenas.ArenaManager
import com.joutak.acerace.zones.Zone
import com.joutak.acerace.zones.ZoneManager
import com.joutak.acerace.zones.ZoneType
import com.joutak.acerace.zones.ZoneFactory
import com.joutak.acerace.utils.PluginManager
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
 * Команды для управления зонами:
 * /zone create <name> <type> [pos1] [pos2]  — создать зону
 * /zone setpos1 <name>                      — установить первую точку
 * /zone setpos2 <name>                      — установить вторую точку
 * /zone remove <name>                       — удалить зону
 * /zone list                                — список всех зон
 * /zone reload                              — перезагрузить зоны из файла
 * /zone clear                               — удалить все зоны
 * /zone info <name>                         — информация о зоне
 * /zone test <name>                         — проверить, внутри ли игрок
 */
class ZoneCommand : CommandExecutor, TabCompleter {

    private val pendingPos1 = mutableMapOf<UUID, Pair<String, org.bukkit.Location>>()

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

            "create" -> {
                val name = args.getOrNull(1)
                val typeStr = args.getOrNull(2)?.uppercase()

                if (name == null || typeStr == null) {
                    sender.err("Использование: /zone create <name> <type>")
                    sender.sendMessage(Component.text(
                        "Доступные типы: ${ZoneType.values().joinToString(", ")}",
                        NamedTextColor.GRAY
                    ))
                    return true
                }

                val type = try {
                    ZoneType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    sender.err("Неизвестный тип: $typeStr")
                    sender.sendMessage(Component.text(
                        "Доступные типы: ${ZoneType.values().joinToString(", ")}",
                        NamedTextColor.GRAY
                    ))
                    return true
                }

                try {
                    ZoneManager.getZone(name)
                    sender.err("Зона с именем '$name' уже существует!")
                    return true
                } catch (e: IllegalArgumentException) {
                }

                pendingPos1[sender.uniqueId] = Pair(name, sender.location.clone())
                sender.ok("Зона '$name' ($type) создается. Используй /zone setpos2 $name")
                sender.sendMessage(Component.text(
                    "Pos1: ${sender.location.blockX}, ${sender.location.blockY}, ${sender.location.blockZ}",
                    NamedTextColor.GRAY
                ))
            }

            "setpos1" -> {
                val name = args.getOrNull(1)
                if (name == null) {
                    sender.err("Использование: /zone setpos1 <name>")
                    return true
                }

                pendingPos1[sender.uniqueId] = Pair(name, sender.location.clone())
                sender.ok("Pos1 для зоны '$name' установлена")
            }

            "setpos2" -> {
                val name = args.getOrNull(1)
                if (name == null) {
                    sender.err("Использование: /zone setpos2 <name>")
                    return true
                }

                val pos1Data = pendingPos1[sender.uniqueId]
                if (pos1Data == null || pos1Data.first != name) {
                    sender.err("Сначала установи /zone setpos1 $name")
                    return true
                }

                try {
                    ZoneManager.getZone(name)
                    sender.err("Зона '$name' уже существует! Используй /zone remove $name")
                    return true
                } catch (e: IllegalArgumentException) {
                    // OK
                }

                val typeStr = args.getOrNull(2)?.uppercase() ?: "BARRIER"
                val type = try {
                    ZoneType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    sender.err("Неизвестный тип: $typeStr")
                    return true
                }

                var pos1 = pos1Data.second
                var pos2 = sender.location.clone()

                val expanded = expandZone(pos1, pos2)
                pos1 = expanded.first
                pos2 = expanded.second

                val zone = ZoneFactory.createZone(
                    type,
                    name,
                    pos1.x, pos1.y, pos1.z,
                    pos2.x, pos2.y, pos2.z
                )

                ZoneManager.addZone(zone)

                pendingPos1.remove(sender.uniqueId)

                sender.ok("Зона '$name' ($type) сохранена в шаблоне!")
                sender.sendMessage(Component.text(
                    "(${zone.x1.toInt()}, ${zone.y1.toInt()}, ${zone.z1.toInt()}) -> " +
                            "(${zone.x2.toInt()}, ${zone.y2.toInt()}, ${zone.z2.toInt()})",
                    NamedTextColor.GRAY
                ))

                sender.sendMessage(Component.text(
                    "Используй /zone sync, чтобы применить изменения ко всем аренам",
                    NamedTextColor.YELLOW
                ))
            }


            "remove" -> {
                val name = args.getOrNull(1)
                if (name == null) {
                    sender.err("Использование: /zone remove <name>")
                    return true
                }

                try {
                    ZoneManager.removeZone(name)
                    sender.ok("Зона '$name' удалена из шаблона!")
                } catch (e: IllegalArgumentException) {
                    sender.err("Зона '$name' не найдена!")
                }
            }

            "list" -> {
                val zones = ZoneManager.getZones()
                if (zones.isEmpty()) {
                    sender.sendMessage(Component.text("Шаблонных зон нет!", NamedTextColor.GRAY))
                    return true
                }

                sender.sendMessage(
                    Component.text("=== Шаблонные зоны (${zones.size}) ===", NamedTextColor.AQUA)
                )

                zones.values.sortedBy { it.name }.forEach { zone ->
                    sender.sendMessage(Component.text(
                        "  ${zone.type} - ${zone.name}",
                        NamedTextColor.WHITE
                    ))
                }
            }

            "reload" -> {
                ZoneManager.loadZones()
                sender.ok("Шаблонные зоны перезагружены! Загружено ${ZoneManager.getZones().size} зон")
                sender.sendMessage(Component.text(
                    "Используй /zone sync для синхронизации с аренами",
                    NamedTextColor.YELLOW
                ))
            }

            "clear" -> {
                ZoneManager.clearZones()
                sender.sendMessage(Component.text("Все зоны удалены!", NamedTextColor.RED))
            }

            "info" -> {
                val name = args.getOrNull(1)
                if (name == null) {
                    sender.err("Использование: /zone info <name>")
                    return true
                }

                try {
                    val zone = ZoneManager.getZone(name)
                    sender.sendMessage(Component.text("=== Зона '$name' ===", NamedTextColor.AQUA))
                    sender.sendMessage(Component.text("Тип: ${zone.type}", NamedTextColor.WHITE))
                    sender.sendMessage(Component.text(
                        "Координаты: (${zone.x1.toInt()}, ${zone.y1.toInt()}, ${zone.z1.toInt()}) -> " +
                                "(${zone.x2.toInt()}, ${zone.y2.toInt()}, ${zone.z2.toInt()})",
                        NamedTextColor.WHITE
                    ))
                } catch (e: IllegalArgumentException) {
                    sender.err("Зона '$name' не найдена!")
                }
            }

            "test" -> {
                val name = args.getOrNull(1)
                if (name == null) {
                    sender.sendMessage(Component.text("Использование: /zone test <name>", NamedTextColor.GRAY))
                    return true
                }

                try {
                    val zone = ZoneManager.getZone(name)
                    val inside = zone.isInside(sender.location)
                    sender.sendMessage(Component.text(
                        "Шаблонная зона '$name': ${if (inside) "ВНУТРИ" else "СНАРУЖИ"}",
                        if (inside) NamedTextColor.GREEN else NamedTextColor.RED
                    ))
                } catch (e: IllegalArgumentException) {
                    sender.err("Зона '$name' не найдена!")
                }
            }

            "sync" -> {
                val templateZonesCount = ZoneManager.getZones().size
                if (templateZonesCount == 0) {
                    sender.err("Нет шаблонных зон для синхронизации!")
                    return true
                }

                val arenas = ArenaManager.getArenas()
                var count = 0
                for (arena in arenas.values) {
                    ZoneManager.loadZonesForArena(arena.worldName)
                    count++
                }

                sender.ok("Зоны синхронизированы для $count арен (${templateZonesCount} зон каждая)")
            }

            else -> {
                sender.sendMessage(
                    Component.text(
                        "/zone <create|setpos1|setpos2|remove|list|reload|clear|info|test|sync>",
                        NamedTextColor.GRAY
                    )
                )
            }
        }
        return true
    }

    private fun expandZone(pos1: Location, pos2: Location): Pair<Location, Location> {
        var newPos1 = pos1.clone()
        var newPos2 = pos2.clone()

        val xDiff = Math.abs(pos2.x - pos1.x)
        val yDiff = Math.abs(pos2.y - pos1.y)
        val zDiff = Math.abs(pos2.z - pos1.z)

        if (xDiff < 1.0) {
            if (pos2.x >= pos1.x) {
                newPos2.x = pos1.x + 1.0
            } else {
                newPos2.x = pos1.x - 1.0
            }
        }

        if (yDiff < 1.0) {
            if (pos2.y >= pos1.y) {
                newPos2.y = pos1.y + 1.0
            } else {
                newPos2.y = pos1.y - 1.0
            }
        }

        if (zDiff < 1.0) {
            if (pos2.z >= pos1.z) {
                newPos2.z = pos1.z + 1.0
            } else {
                newPos2.z = pos1.z - 1.0
            }
        }

        if (newPos1 == pos1 && newPos2 == pos2) {
            newPos2.x = pos1.x + 1.0
            newPos2.y = pos1.y + 1.0
            newPos2.z = pos1.z + 1.0
        }

        return Pair(newPos1, newPos2)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> = when (args.size) {
        1 -> listOf("create", "setpos1", "setpos2", "remove", "list", "reload", "clear", "info", "test", "sync")
            .filter { it.startsWith(args[0].lowercase()) }
        2 -> when (args[0].lowercase()) {
            "create" -> listOf("barrier_zone", "elytra_zone", "underwater_zone")
                .filter { it.startsWith(args[1]) }
            "setpos1", "setpos2", "remove", "info", "test" -> {
                ZoneManager.getZones().keys.filter { it.startsWith(args[1]) }
            }
            else -> emptyList()
        }
        3 -> when (args[0].lowercase()) {
            "create" -> ZoneType.values().map { it.toString() }
                .filter { it.startsWith(args[2].uppercase()) }
            else -> emptyList()
        }
        else -> emptyList()
    }

    private fun Player.ok(msg: String) =
        sendMessage(Component.text("✔ $msg", NamedTextColor.GREEN))

    private fun Player.err(msg: String) =
        sendMessage(Component.text("✗ $msg", NamedTextColor.RED))
}
