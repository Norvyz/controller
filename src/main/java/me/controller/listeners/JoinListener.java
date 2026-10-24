package me.controller.listeners;

import me.controller.ControllerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final ControllerPlugin plugin;

    public JoinListener(ControllerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPvpManager().onPlayerJoin(player);
        plugin.getPacks().send(player);
        plugin.getAnimations().playWelcome(player, false);
    }
}
