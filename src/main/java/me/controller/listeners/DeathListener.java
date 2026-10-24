package me.controller.listeners;

import me.controller.ControllerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathListener implements Listener {

    private final ControllerPlugin plugin;

    public DeathListener(ControllerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getAnimations().onPlayerDeath(event.getEntity(), event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getAnimations().onPlayerRespawn(event.getPlayer());
    }
}
