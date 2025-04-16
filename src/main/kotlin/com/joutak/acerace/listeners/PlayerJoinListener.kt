package com.joutak.acerace.listeners

import com.joutak.acerace.players.PlayerData
import com.joutak.acerace.utils.LobbyManager
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta


object PlayerJoinListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        PlayerData.resetGame(player.uniqueId)
        LobbyManager.addPlayer(player)

        val item = ItemStack(Material.DIAMOND_BOOTS, 1)
        val metaBoots: ItemMeta = item.itemMeta
        metaBoots.isUnbreakable = true
        item.setItemMeta(metaBoots)
        player.inventory.boots = item

        val trident = ItemStack(Material.TRIDENT)
        trident.addEnchantment(Enchantment.RIPTIDE, 3)
        val metaTrident: ItemMeta = trident.itemMeta
        metaTrident.isUnbreakable = true
        trident.setItemMeta(metaTrident)
        player.inventory.addItem(trident)
    }
}