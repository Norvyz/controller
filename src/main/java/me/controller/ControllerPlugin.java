package me.controller;

import me.controller.animation.AnimationManager;
import me.controller.commands.MensajeCommand;
import me.controller.commands.PvpCommand;
import me.controller.listeners.DeathListener;
import me.controller.listeners.JoinListener;
import me.controller.listeners.PackListener;
import me.controller.listeners.SilentCommandListener;
import me.controller.pack.PackManager;
import me.controller.managers.PvpManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ControllerPlugin extends JavaPlugin {
    
    private static ControllerPlugin instance;
    private PvpManager pvpManager;
    private AnimationManager animations;
    private PackManager packs;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        reloadConfig();
        
        this.packs = new PackManager(this);
        this.packs.start();
        this.animations = new AnimationManager(this);
        this.pvpManager = new PvpManager(this);
        
        if (getCommand("pvp") != null) {
            getCommand("pvp").setExecutor(new PvpCommand(this));
        }
        if (getCommand("mensaje") != null) {
            getCommand("mensaje").setExecutor(new MensajeCommand(this));
        }
        
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PackListener(this), this);
        getServer().getPluginManager().registerEvents(new SilentCommandListener(this), this);
        
        getLogger().info("Controller ha sido activado correctamente!");
    }
    
    @Override
    public void onDisable() {
        if (pvpManager != null) {
            pvpManager.disablePvp(false);
        }
        if (animations != null) {
            animations.shutdown();
        }
        if (packs != null) {
            packs.stop();
        }
        getLogger().info("Controller ha sido desactivado.");
    }
    
    public static ControllerPlugin getInstance() {
        return instance;
    }
    
    public PvpManager getPvpManager() {
        return pvpManager;
    }

    public PackManager getPacks() {
        return packs;
    }

    public AnimationManager getAnimations() {
        return animations;
    }
}
