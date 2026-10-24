package me.controller.animation;

import me.controller.ControllerPlugin;
import me.controller.pack.PackManager;
import me.controller.utils.ColorUtil;
import me.controller.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.logging.Level;

/** Orquesta todas las animaciones (bienvenida y PvP) y limpia tareas/entidades al apagar. */
public class AnimationManager {

    private final ControllerPlugin plugin;
    private final Set<UUID> pendingDeath = new HashSet<>();
    private final Random random = new Random();
    private final Set<BukkitRunnable> tasks = new HashSet<>();
    private final Set<Entity> entities = new HashSet<>();

    public AnimationManager(ControllerPlugin plugin) {
        this.plugin = plugin;
    }

    public ControllerPlugin plugin() {
        return plugin;
    }

    private PackManager packs() {
        return plugin.getPacks();
    }

    // ------------------------------------------------------------------ infraestructura

    /** Ejecuta step(0), step(1)... cada 'period' ticks hasta que devuelva false. */
    public BukkitRunnable repeat(long delay, long period, IntPredicate step) {
        final int[] n = {0};
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                boolean keep;
                try {
                    keep = step.test(n[0]++);
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.WARNING, "Error en una animacion", t);
                    keep = false;
                }
                if (!keep) {
                    cancel();
                    tasks.remove(this);
                }
            }
        };
        tasks.add(r);
        r.runTaskTimer(plugin, delay, period);
        return r;
    }

    public void trackEntity(Entity e) {
        entities.add(e);
    }

    public void removeEntity(Entity e) {
        if (e == null) return;
        entities.remove(e);
        if (e.isValid()) e.remove();
    }

    /** Cancela todo y borra las entidades temporales. Se llama al apagar el plugin. */
    public void shutdown() {
        for (BukkitRunnable r : new HashSet<>(tasks)) {
            try {
                r.cancel();
            } catch (IllegalStateException ignored) {
            }
        }
        tasks.clear();
        for (Entity e : new HashSet<>(entities)) {
            if (e.isValid()) e.remove();
        }
        entities.clear();
    }

    private Color[] gradient(String path, String defA, String defB) {
        List<String> l = plugin.getConfig().getStringList(path);
        Color a = ColorUtil.parseHex(l.size() > 0 ? l.get(0) : defA, ColorUtil.parseHex(defA, Color.RED));
        Color b = ColorUtil.parseHex(l.size() > 1 ? l.get(1) : defB, ColorUtil.parseHex(defB, Color.ORANGE));
        return new Color[]{a, b};
    }

    // ------------------------------------------------------------------ resource pack

    /**
     * Ejecuta 'image' cuando el jugador ya cargo el resource pack, o 'fallback' (solo texto) si no hay pack,
     * lo rechazo, fallo, o no cargo a tiempo.
     */
    private void whenImageReady(Player p, boolean useImage, Runnable image, Runnable fallback) {
        if (!useImage || !packs().isAvailable()) {
            fallback.run();
            return;
        }
        packs().send(p);
        if (packs().isLoaded(p)) {
            image.run();
            return;
        }
        final int wait = Math.max(20, plugin.getConfig().getInt("resource-pack.wait-ticks", 200));
        repeat(0, 5, i -> {
            if (!p.isOnline()) return false;
            if (packs().isLoaded(p)) {
                image.run();
                return false;
            }
            if (packs().hasFailed(p) || i * 5 >= wait) {
                fallback.run();
                return false;
            }
            return true;
        });
    }

    // ------------------------------------------------------------------ bienvenida

    public void playWelcome(Player p, boolean immediate) {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("join-message.enabled", true)) return;

        String raw = c.getString("join-message.text", "");
        if (raw == null || raw.isEmpty()) return;
        raw = raw.replace("%player%", p.getName()).replace("\\n", "\n");
        String[] split = ColorUtil.colorize(raw).split("\n", 2);
        final String title = split[0];
        final String subtitle = split.length > 1 ? split[1] : "";
        final String enter = c.getString("join-message.sounds.enter", "UI_TOAST_CHALLENGE_COMPLETE");

        boolean animated = c.getBoolean("join-message.animation.enabled", true) && plugin.isEnabled();
        if (!animated) {
            p.sendTitle(title, subtitle, 10, 70, 20);
            Fx.sound(p, enter, 1.0f, 1.0f);
            return;
        }

        long delay = immediate ? 0 : Math.max(0, c.getLong("join-message.animation.start-delay-ticks", 20));
        final Color[] g = gradient("join-message.animation.gradient", "#FFD700", "#FF7A00");
        final String plainTitle = ColorUtil.strip(title);
        final boolean useImage = c.getBoolean("join-message.animation.show-logo", true);

        repeat(delay, 1, t -> {
            if (!p.isOnline()) return false;
            whenImageReady(p, useImage,
                    () -> {
                        Fx.sound(p, enter, 1.0f, 1.0f);
                        ImageTitle.welcome(this, p, title, subtitle, g[0], g[1]);
                    },
                    () -> {
                        Fx.sound(p, enter, 1.0f, 1.0f);
                        TitleAnimation.play(this, p, new TitleAnimation.Spec(plainTitle, subtitle, g[0], g[1],
                                "", "", TitleAnimation.Mode.TYPE, 50, 20));
                    });
            return false;
        });
    }

    // ------------------------------------------------------------------ muerte

    private String pick(String path, String fallback, Player p) {
        List<String> l = plugin.getConfig().getStringList(path);
        String s = l.isEmpty() ? fallback : l.get(random.nextInt(l.size()));
        return ColorUtil.colorize(s.replace("%player%", p.getName()));
    }

    /** Al morir: burla en el chat para todos. La imagen se muestra cuando el jugador reaparece. */
    public void onPlayerDeath(Player p, PlayerDeathEvent e) {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("death.enabled", true)) return;
        if (c.getBoolean("death.replace-vanilla-message", false)) {
            e.setDeathMessage(null);
        }
        if (c.getBoolean("death.broadcast", true)) {
            Bukkit.broadcastMessage(pick("death.messages", "&c%player% &7se murió, jajaja", p));
        }
        pendingDeath.add(p.getUniqueId());
    }

    /** Al reaparecer: titulo burlon + calavera paisa riendose + carcajadas. */
    public void onPlayerRespawn(Player p) {
        if (!pendingDeath.remove(p.getUniqueId())) return;
        long delay = Math.max(1, plugin.getConfig().getLong("death.start-delay-ticks", 15));
        repeat(delay, 1, t -> {
            if (p.isOnline()) playDeath(p);
            return false;
        });
    }

    public void playDeath(Player p) {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("death.enabled", true) || !plugin.isEnabled()) return;
        final String taunt = pick("death.subtitles", "&7Que oso, parce!", p);
        final String headline = ColorUtil.strip(c.getString("death.title", "¡JAJAJAJA!"));
        final Color[] g = gradient("death.gradient", "#FF2A2A", "#8A0000");

        whenImageReady(p, c.getBoolean("death.show-image", true),
                () -> {
                    ImageTitle.death(this, p, headline, taunt, g[0], g[1]);
                    laugh(p);
                },
                () -> {
                    TitleAnimation.play(this, p, new TitleAnimation.Spec(headline, taunt, g[0], g[1],
                            "", "", TitleAnimation.Mode.CONVERGE, 50, 20));
                    laugh(p);
                });
    }

    /** Carcajadas: el mismo sonido repetido con tonos distintos. */
    private void laugh(Player p) {
        FileConfiguration c = plugin.getConfig();
        String laugh = c.getString("death.laugh-sound", "entity.witch.celebrate");
        float vol = (float) c.getDouble("death.laugh-volume", 1.0);
        float[] pitches = {1.0f, 0.8f, 1.15f, 0.9f, 1.25f, 0.85f};
        int count = Math.max(1, Math.min(pitches.length, c.getInt("death.laugh-count", 5)));
        repeat(0, 9, i -> {
            if (!p.isOnline() || i >= count) return false;
            Fx.sound(p, laugh, vol, pitches[i]);
            return true;
        });
    }

    // ------------------------------------------------------------------ PvP

    /** Titulo con espadas que convergen + espadas 3D que chocan + sonido de inicio. */
    public void playPvpStart(Player p, long seconds) {
        FileConfiguration c = plugin.getConfig();
        String sound = c.getString("pvp.sounds.start", "ENTITY_ENDER_DRAGON_GROWL");
        String subtitle = ColorUtil.colorize("&fPor &e" + TimeUtil.formatTime(seconds));

        if (!c.getBoolean("pvp.animation.enabled", true) || !plugin.isEnabled()) {
            p.sendTitle(ColorUtil.colorize("&c&l¡PVP ACTIVADO!"), subtitle,
                    c.getInt("pvp.title-fade-in", 10), c.getInt("pvp.title-stay", 70), c.getInt("pvp.title-fade-out", 20));
            Fx.sound(p, sound, 1.0f, 1.0f);
            return;
        }

        Fx.sound(p, sound, 1.0f, 1.0f);
        Color[] g = gradient("pvp.animation.gradient", "#FF2A2A", "#FFB300");
        TitleAnimation.play(this, p, new TitleAnimation.Spec("¡PVP ACTIVADO!", subtitle, g[0], g[1],
                c.getString("pvp.animation.symbol-left", ">>"), c.getString("pvp.animation.symbol-right", "<<"),
                TitleAnimation.Mode.CONVERGE,
                c.getInt("pvp.title-stay", 70), c.getInt("pvp.title-fade-out", 20)));
        if (c.getBoolean("pvp.animation.swords", true)) {
            SwordsAnimation.play(this, p);
        }
    }

    public void playPvpWarn(Player p, long remaining) {
        FileConfiguration c = plugin.getConfig();
        String sound = c.getString("pvp.sounds.warn", "BLOCK_NOTE_BLOCK_PLING");
        String subtitle = ColorUtil.colorize("&fPVP se desactiva en &c" + TimeUtil.formatTime(remaining));
        Fx.sound(p, sound, 1.0f, 1.0f);

        if (!c.getBoolean("pvp.animation.enabled", true) || !plugin.isEnabled()) {
            p.sendTitle(ColorUtil.colorize("&e&l¡ATENCIÓN!"), subtitle, 10, 40, 10);
            return;
        }
        Color[] g = gradient("pvp.animation.warn-gradient", "#FFE600", "#FF7A00");
        TitleAnimation.play(this, p, new TitleAnimation.Spec("¡ATENCIÓN!", subtitle, g[0], g[1],
                "!!", "!!", TitleAnimation.Mode.CONVERGE, 30, 10));
        p.spawnParticle(Particle.ENCHANTED_HIT, p.getEyeLocation().add(Fx.horizontalDir(p).multiply(2.5)), 25, 0.6, 0.5, 0.6, 0.2);
        Fx.sound(p, "block.note_block.bell", 0.8f, 1.2f);
    }

    public void playPvpEnd(Player p) {
        FileConfiguration c = plugin.getConfig();
        String sound = c.getString("pvp.sounds.end", "ENTITY_WITHER_DEATH");
        String subtitle = ColorUtil.colorize("&fYa puedes volver a la tranquilidad");
        Fx.sound(p, sound, 1.0f, 1.0f);

        if (!c.getBoolean("pvp.animation.enabled", true) || !plugin.isEnabled()) {
            p.sendTitle(ColorUtil.colorize("&a&l¡PVP DESACTIVADO!"), subtitle, 10, 70, 20);
            return;
        }
        Color[] g = gradient("pvp.animation.end-gradient", "#2BFF6B", "#00B7FF");
        TitleAnimation.play(this, p, new TitleAnimation.Spec("¡PVP DESACTIVADO!", subtitle, g[0], g[1],
                "**", "**", TitleAnimation.Mode.CONVERGE, 60, 20));
        p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getEyeLocation().add(Fx.horizontalDir(p).multiply(2.5)), 40, 0.7, 0.6, 0.7, 0.3);
        Fx.soundAt(p, p.getLocation(), "item.totem.use", 0.5f, 1.4f);
    }

    /** Texto animado para la barra de accion / bossbar mientras el PvP esta activo. */
    public String pvpBarText(long remaining, int frame) {
        FileConfiguration c = plugin.getConfig();
        Color[] g = gradient("pvp.animation.gradient", "#FF2A2A", "#FFB300");
        String symL = c.getString("pvp.animation.symbol-left", ">>");
        String symR = c.getString("pvp.animation.symbol-right", "<<");
        boolean urgent = remaining <= 30;
        Color a = g[0], b = g[1];
        if (urgent && frame % 2 == 0) {
            a = Color.WHITE;
            b = g[0];
        }
        String core = ColorUtil.gradient("PVP ACTIVO", a, b, true, frame * 0.07, -1, 0);
        String edge = ColorUtil.hex(urgent && frame % 2 == 0 ? Color.WHITE : g[1]);
        return edge + symL + " " + core + " " + edge + symR
                + ColorUtil.colorize("  &8- &f" + TimeUtil.formatTime(remaining));
    }

    public void sendActionBar(Player p, String legacyText) {
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(legacyText));
    }
}
