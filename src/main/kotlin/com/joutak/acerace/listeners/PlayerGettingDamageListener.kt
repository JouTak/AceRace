package com.joutak.acerace.listeners
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

object PlayerGettingDamageListener : Listener {
    @EventHandler
    fun onDamage(event: EntityDamageEvent){
        event.isCancelled = true
    }
}