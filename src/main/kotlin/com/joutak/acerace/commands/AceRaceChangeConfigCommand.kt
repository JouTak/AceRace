package ru.joutak.blockparty.commands

import com.joutak.acerace.commands.AceRaceCommand
import com.joutak.acerace.config.ConfigKey
import com.joutak.acerace.config.ConfigKeys
import com.joutak.acerace.games.SpartakiadaManager
import com.joutak.acerace.players.PlayerData
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object AceRaceChangeConfigCommand : AceRaceCommand("config", listOf("key", "value"), "acerace.admin") {
    override fun execute(
        sender: CommandSender,
        command: Command,
        string: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("Недостаточно прав для использования данной команды.")
            return true
        }

        var set = false
        var get = false

        if (args.size == this.args.size) {
            set = true
        } else if (args.size == this.args.size - 1) {
            get = true
        }

        if (!get && !set) {
            return false
        }

        val key = ConfigKeys.all.find { it.path.equals(args[0], ignoreCase = true) }

        if (key == null) {
            sender.sendMessage("${args[0]} не найден.")
            return true
        }

        if (get) {
            sender.sendMessage("Текущее значение ${key.path}: ${com.joutak.acerace.config.Config.get(key)}")
        } else if (set) {
            val value = key.parse(args[1])

            if (value == null) {
                sender.sendMessage("Не удалось преобразовать ${args[1]}.")
                return true
            }

            @Suppress("UNCHECKED_CAST")
            com.joutak.acerace.config.Config.set(key as ConfigKey<Any>, value)
            sender.sendMessage("Значение ${key.path} обновлено на $value.")
        }
        
        if (key.path.equals(ConfigKeys.SPARTAKIADA_MODE.path)) {
            PlayerData.reloadDatas()
            SpartakiadaManager.reload()
        }

        return true
    }

    override fun getTabHint(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (!sender.isOp) return emptyList()

        return when (args.size) {
            1 -> ConfigKeys.all.map { it.path }.filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
