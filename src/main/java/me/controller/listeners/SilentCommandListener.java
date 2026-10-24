package me.controller.listeners;

import me.controller.ControllerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class SilentCommandListener implements Listener {
    
    private final ControllerPlugin plugin;
    
    public SilentCommandListener(ControllerPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();
        
        if (!player.isOp() && !player.hasPermission("controller.pvp") && !player.hasPermission("controller.mensaje")) {
            return;
        }
        
        if (message.startsWith("/pvp") || message.startsWith("/mensaje")) {
            if (player.isOp() || player.hasPermission("controller.pvp") || player.hasPermission("controller.mensaje")) {
                event.setCancelled(true);
                player.performCommand(event.getMessage().substring(1));
            }
        }
    }
}
