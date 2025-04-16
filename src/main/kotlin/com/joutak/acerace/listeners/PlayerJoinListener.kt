package com.joutak.acerace.listeners

import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyManager
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

object PlayerJoinListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        PlayerData.resetGame(player.uniqueId)
        LobbyManager.addPlayer(player)

        val item = ItemStack(Material.DIAMOND_BOOTS, 1)
        item.addEnchantment(Enchantment.DEPTH_STRIDER, 3)
        player.inventory.boots = item

        val trident = ItemStack(Material.TRIDENT)
        trident.addEnchantment(Enchantment.RIPTIDE, 3)
        player.inventory.addItem(trident)
    }
}