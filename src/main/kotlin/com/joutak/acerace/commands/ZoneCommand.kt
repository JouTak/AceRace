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
 * /zone setpos1 <name>                      — установить первую точку
 * /zone setpos2 <name>                      — установить вторую точку
 * /zone remove <name>                       — удалить зону
 * /zone list — список всех зон
 * /zone save - сохранить зоны
 * /zone load - загрузить для текущего мира
 * /zone reload                              — перезагрузить зоны из файла
 * /zone clear                               — удалить все зоны
 * /zone info <name>                         — информация о зоне
 * /zone test <name>                         — проверить, внутри ли игрок
 */
class ZoneCommand : CommandExecutor, TabCompleter {

    private val pendingPos1 = mutableMapOf<UUID, Triple<String, ZoneType, Location>>()

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

            "setpos1" -> {
                val name = args.getOrNull(1)
                val typeStr = args.getOrNull(2)?.uppercase()

                if (name == null || typeStr == null) {
                    sender.err("Использование: /zone setpos1 <name> <type> (BARRIER, ELYTRA, UNDERWATER)")
                    sender.err("Пример: /zone setpos1 myZone ELYTRA")
                    return true
                }

                val type = try {
                    ZoneType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    sender.err("Неверный тип зоны! Доступные: BARRIER, ELYTRA, UNDERWATER")
                    return true
                }

                pendingPos1[sender.uniqueId] = Triple(name, type, sender.location.clone())
                sender.ok(
                    "Pos1 для зоны '$name' типа $type: " +
                            "${sender.location.blockX}, " +
                            "${sender.location.blockY}, " +
                            "${sender.location.blockZ}"
                )
            }

            "setpos2" -> {
                val name = args.getOrNull(1)
                val typeStr = args.getOrNull(2)?.uppercase()

                if (name == null || typeStr == null) {
                    sender.err("Использование: /zone setpos2 <name> <type>")
                    return true
                }

                val type = try {
                    ZoneType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    sender.err("Неверный тип зоны! Доступные: BARRIER, ELYTRA, UNDERWATER")
                    return true
                }

                val pos1Data = pendingPos1[sender.uniqueId]
                if (pos1Data == null || pos1Data.first != name || pos1Data.second != type) {
                    sender.err("Сначала установи /zone setpos1 $name $typeStr")
                    return true
                }

                val pos1 = pos1Data.third
                val pos2 = sender.location.clone()

                if (pos1.world?.name != pos2.world?.name) {
                    sender.err("Нельзя использовать точки из разных миров!")
                    pendingPos1.remove(sender.uniqueId)
                    return true
                }

                val worldName = sender.world.name

                try {
                    val existingZone = ZoneManager.getZone(name)
                    ZoneManager.removeZone(name)
                } catch (e: IllegalArgumentException) {
                }

                val zone = ZoneFactory.createZone(
                    type,
                    name,
                    worldName,
                    pos1,
                    pos2
                )
                ZoneManager.addZone(zone)
                pendingPos1.remove(sender.uniqueId)

                sender.ok(
                    "Зона '$name' типа $type сохранена в мире $worldName! " +
                            "Координаты: (${pos1.blockX},${pos1.blockY},${pos1.blockZ}) -> " +
                            "(${pos2.blockX},${pos2.blockY},${pos2.blockZ})"
                )
            }


            "remove" -> {
                val name = args.getOrNull(1)
                if (name == null) {
                    sender.err("Использование: /zone remove <name>")
                    return true
                }

                try {
                    ZoneManager.removeZone(name)
                    pendingPos1.remove(sender.uniqueId)
                    sender.ok("Зона '$name' удалена!")
                } catch (e: IllegalArgumentException) {
                    sender.err(e.message ?: "Зона не найдена!")
                }
            }

            "list" -> {
                val zones = ZoneManager.getZones()
                if (zones.isEmpty()) {
                    sender.sendMessage(Component.text("Зон нет!", NamedTextColor.GRAY))
                    return true
                }

                sender.sendMessage(
                    Component.text("=== Зоны ===", NamedTextColor.AQUA)
                )

                zones.values.groupBy { it.worldName }.forEach { (worldName, worldZones) ->
                    sender.sendMessage(
                        Component.text("Мир: $worldName", NamedTextColor.GOLD)
                    )
                    worldZones.forEach { zone ->
                        sender.sendMessage(
                            Component.text(
                                "  ${zone.name} (${zone.type}) - " +
                                        "(${zone.x1.toInt()},${zone.y1.toInt()},${zone.z1.toInt()}) -> " +
                                        "(${zone.x2.toInt()},${zone.y2.toInt()},${zone.z2.toInt()})",
                                NamedTextColor.WHITE
                            )
                        )
                    }
                }
            }

            "save" -> {
                try {
                    ZoneManager.saveZones()
                    sender.ok("Зоны сохранены!")
                } catch (e: Exception) {
                    sender.err("Ошибка при сохранении: ${e.message}")
                }
            }

            "load" -> {
                try {
                    ZoneManager.loadZones()
                    sender.ok("Зоны загружены!")
                } catch (e: Exception) {
                    sender.err("Ошибка при загрузке: ${e.message}")
                }
            }

            "reload" -> {
                try {
                    ZoneManager.loadZones()
                    val worldName = sender.world.name
                    ZoneManager.loadZonesForArena(worldName)
                    sender.ok("Зоны перезагружены для мира $worldName!")
                } catch (e: Exception) {
                    sender.err("Ошибка при перезагрузке: ${e.message}")
                }
            }

            "worlds" -> {
                val worlds = ZoneManager.getAllWorlds()
                if (worlds.isEmpty()) {
                    sender.sendMessage(
                        Component.text("Нет миров с зонами!", NamedTextColor.GRAY)
                    )
                    return true
                }

                sender.sendMessage(
                    Component.text("=== Миры с зонами ===", NamedTextColor.AQUA)
                )
                worlds.forEach { worldName ->
                    val zones = ZoneManager.getZones().values.filter { it.worldName == worldName }
                    sender.sendMessage(
                        Component.text(
                            "  $worldName — ${zones.size} зон(ы)",
                            NamedTextColor.WHITE
                        )
                    )
                }
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
        1 -> listOf("setpos1", "setpos2", "remove", "list", "reload", "clear", "info", "test", "sync")
            .filter { it.startsWith(args[0].lowercase()) }
        2 -> when (args[0].lowercase()) {
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
