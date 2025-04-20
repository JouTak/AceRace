package com.joutak.acerace.config

import com.joutak.acerace.AceRacePlugin
import com.joutak.acerace.utils.PluginManager
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object Config {
    private val configFile = File(PluginManager.getDataFolder(), "config.yml")
    private val config: YamlConfiguration

    init {
        AceRacePlugin.instance.getLogger().info("Загрузка значений из конфига...")
        if (!configFile.exists()) {
            AceRacePlugin.instance.saveResource("config.yml", true)
        }
        config = YamlConfiguration.loadConfiguration(configFile)
        saveDefaults()
    }

    private fun saveDefaults() {
        for (key in ConfigKeys.all) {
            if (!config.contains(key.path)) {
                AceRacePlugin.instance
                    .getLogger()
                    .warning("Не найден ключ ${key.path} в конфиге! Взято стандартное значение: ${key.value}")
                config.set(key.path, key.value)
            }
        }
        config.save(configFile)
    }

    fun <T : Any> get(key: ConfigKey<T>): T {
        val value = config.get(key.path)
        return if (key.value::class.java.isInstance(value)) {
            value as T
        } else {
            key.value
        }
    }

    fun <T : Any> set(
        key: ConfigKey<T>,
        value: T,
    ) {
        config.set(key.path, value)
        config.save(configFile)
    }
}
