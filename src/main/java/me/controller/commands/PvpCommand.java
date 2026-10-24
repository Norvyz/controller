package me.controller.commands;

import me.controller.ControllerPlugin;
import me.controller.utils.ColorUtil;
import me.controller.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpCommand implements CommandExecutor {
    
    private final ControllerPlugin plugin;
    
    public PvpCommand(ControllerPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("controller.pvp"))) {
            sender.sendMessage(ColorUtil.colorize("&cNo tienes permiso para usar este comando."));
            return true;
        }
        
        if (args.length >= 1 && args[0].equalsIgnoreCase("off")) {
            if (!plugin.getPvpManager().isPvpActive()) {
                sender.sendMessage(ColorUtil.colorize("&cEl PvP no esta activo."));
                return true;
            }
            plugin.getPvpManager().disablePvp();
            sender.sendMessage(ColorUtil.colorize("&aPvP desactivado."));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("preview")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtil.colorize("&cSolo un jugador puede ver la vista previa."));
                return true;
            }
            plugin.getAnimations().playPvpStart(player, 1800);
            sender.sendMessage(ColorUtil.colorize("&7Vista previa de la animacion de PvP (solo la ves tu)."));
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("on")) {
            sender.sendMessage(ColorUtil.colorize("&cUso: /pvp on <tiempo> (ej: 30m, 1h, 90s) | /pvp off | /pvp preview"));
            return true;
        }

        long seconds = TimeUtil.parseTime(args[1]);
        if (seconds <= 0) {
            sender.sendMessage(ColorUtil.colorize("&cTiempo inválido. Usa s, m o h. Ej: 30m"));
            return true;
        }
        
        plugin.getPvpManager().activatePvp(seconds);
        sender.sendMessage(ColorUtil.colorize("&aPvP activado por &e" + TimeUtil.formatTime(seconds) + "&a."));
        
        return true;
    }
}
