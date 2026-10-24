package me.controller.managers;

import me.controller.ControllerPlugin;
import me.controller.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PvpManager {

    private final ControllerPlugin plugin;
    private boolean pvpActive = false;
    private long endTime = 0;
    private long totalSeconds = 1;
    private BukkitTask timerTask;
    private BukkitRunnable hudTask;
    private BossBar bossBar;
    private Set<String> affectedWorlds = new HashSet<>();

    public PvpManager(ControllerPlugin plugin) {
        this.plugin = plugin;
    }

    public void activatePvp(long seconds) {
        if (pvpActive) {
            disablePvp(false);
        }

        affectedWorlds.clear();
        boolean allWorlds = plugin.getConfig().getBoolean("pvp.all-worlds", true);

        if (allWorlds) {
            for (World world : Bukkit.getWorlds()) {
                world.setPVP(true);
                affectedWorlds.add(world.getName());
            }
        } else {
            List<String> worldsList = plugin.getConfig().getStringList("pvp.worlds");
            for (String worldName : worldsList) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    world.setPVP(true);
                    affectedWorlds.add(world.getName());
                }
            }
        }

        pvpActive = true;
        totalSeconds = Math.max(1, seconds);
        endTime = System.currentTimeMillis() + (seconds * 1000);

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getAnimations().playPvpStart(player, seconds);
        }

        if (plugin.getConfig().getBoolean("pvp.use-bossbar", false)) {
            createBossBar();
        }

        startTimer();
        startHud();
    }

    private void startTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        List<Integer> preWarns = plugin.getConfig().getIntegerList("pvp.pre-warn-seconds");

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!pvpActive) {
                    cancel();
                    return;
                }

                long remaining = (endTime - System.currentTimeMillis()) / 1000;

                if (remaining <= 0) {
                    disablePvp();
                    cancel();
                    return;
                }

                if (preWarns.contains((int) remaining)) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getAnimations().playPvpWarn(player, remaining);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /** Barra de accion (y bossbar) animada mientras el PvP esta activo. */
    private void startHud() {
        if (hudTask != null) {
            hudTask.cancel();
            hudTask = null;
        }
        boolean actionBar = plugin.getConfig().getBoolean("pvp.actionbar", true);
        if (!actionBar && bossBar == null) {
            return;
        }

        hudTask = plugin.getAnimations().repeat(0L, 5L, frame -> {
            if (!pvpActive) return false;
            long remaining = Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
            String text = plugin.getAnimations().pvpBarText(remaining, frame);

            if (actionBar) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    plugin.getAnimations().sendActionBar(player, text);
                }
            }
            if (bossBar != null) {
                bossBar.setTitle(text);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, remaining / (double) totalSeconds)));
                bossBar.setColor(remaining <= 30 && frame % 2 == 0 ? BarColor.WHITE : BarColor.RED);
            }
            return true;
        });
    }

    public void disablePvp() {
        disablePvp(true);
    }

    /** @param announce false = apagado silencioso (reinicio del plugin / servidor). */
    public void disablePvp(boolean announce) {
        if (!pvpActive && affectedWorlds.isEmpty()) {
            return;
        }

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (hudTask != null) {
            try {
                hudTask.cancel();
            } catch (IllegalStateException ignored) {
            }
            hudTask = null;
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        for (String worldName : affectedWorlds) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.setPVP(false);
            }
        }
        affectedWorlds.clear();

        pvpActive = false;
        endTime = 0;

        if (!announce) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getAnimations().playPvpEnd(player);
            // Limpia la barra de accion
            plugin.getAnimations().sendActionBar(player, "");
        }
    }

    /** Un jugador entra mientras el PvP esta activo: se une a la bossbar. */
    public void onPlayerJoin(Player player) {
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    private void createBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(ColorUtil.colorize("&c&lPVP ACTIVO"), BarColor.RED, BarStyle.SOLID);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    public boolean isPvpActive() {
        return pvpActive;
    }

    public long getRemainingSeconds() {
        return pvpActive ? Math.max(0, (endTime - System.currentTimeMillis()) / 1000) : 0;
    }
}
