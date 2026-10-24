package me.controller.listeners;

import me.controller.ControllerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class PackListener implements Listener {

    private final ControllerPlugin plugin;

    public PackListener(ControllerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        Player p = event.getPlayer();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> plugin.getPacks().markLoaded(p);
            case DECLINED, FAILED_DOWNLOAD, FAILED_RELOAD, INVALID_URL, DISCARDED -> plugin.getPacks().markFailed(p);
            default -> { /* ACCEPTED / DOWNLOADED: todavia cargando */ }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPacks().forget(event.getPlayer());
    }
}
