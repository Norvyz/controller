package me.controller.commands;

import me.controller.ControllerPlugin;
import me.controller.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MensajeCommand implements CommandExecutor {
    
    private final ControllerPlugin plugin;
    
    public MensajeCommand(ControllerPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("controller.mensaje"))) {
            sender.sendMessage(ColorUtil.colorize("&cNo tienes permiso para usar este comando."));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.colorize("&cUso: /mensaje <texto> | /mensaje clear | /mensaje preview [muerte] | /mensaje reload"));
            return true;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("preview") && args[1].equalsIgnoreCase("muerte")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtil.colorize("&cSolo un jugador puede ver la vista previa."));
                return true;
            }
            plugin.getAnimations().playDeath(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("preview")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtil.colorize("&cSolo un jugador puede ver la vista previa."));
                return true;
            }
            if (!plugin.getPacks().isAvailable()) {
                sender.sendMessage(ColorUtil.colorize("&eNo hay URL del resource pack: veras solo la version de texto. Mira resource-pack en config.yml."));
            }
            plugin.getAnimations().playWelcome(player, true);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ColorUtil.colorize("&aConfiguracion recargada. Si cambiaste el resource pack, reinicia el servidor."));
            return true;
        }

        // Palabras reservadas: nunca se guardan como mensaje por accidente
        String first = args[0].toLowerCase();
        if (first.equals("preview") || first.equals("reload") || first.equals("clear")) {
            sender.sendMessage(ColorUtil.colorize("&cUso: /mensaje <texto> | /mensaje clear | /mensaje preview [muerte] | /mensaje reload"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            plugin.getConfig().set("join-message.text", "");
            plugin.saveConfig();
            plugin.reloadConfig();
            sender.sendMessage(ColorUtil.colorize("&aMensaje al entrar eliminado."));
            return true;
        }
        
        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }
        String text = message.toString().trim();
        
        plugin.getConfig().set("join-message.text", text);
        plugin.saveConfig();
        plugin.reloadConfig();
        
        sender.sendMessage(ColorUtil.colorize("&aMensaje al entrar guardado correctamente."));
        sender.sendMessage(ColorUtil.colorize("&7Vista previa: &r" + ColorUtil.colorize(text.replace("\\n", "\n"))));
        
        return true;
    }
}
