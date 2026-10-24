package me.controller.animation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class SwordsAnimation {

    private static final int FLY_IN_START = 0;
    private static final int FLY_IN_TICKS = 15;
    private static final int CLASH = 20;

    public static void play(AnimationManager m, Player p) {
        FileConfiguration c = m.plugin().getConfig();
        Material left = material(c.getString("pvp.animation.sword-left", "DIAMOND_SWORD"), Material.DIAMOND_SWORD);
        Material right = material(c.getString("pvp.animation.sword-right", "NETHERITE_SWORD"), Material.NETHERITE_SWORD);
        int hold = Math.max(10, c.getInt("pvp.animation.hold-ticks", 45));
        float base = (float) c.getDouble("pvp.animation.sword-scale", 1.7);
        Vector dir = Fx.horizontalDir(p);
        double dist = Fx.clearDistance(p, dir, 4.0);
        
        // AJUSTE: Subimos las espadas a Y = 2.0 para que queden ARRIBA del texto del título
        Location anchor = p.getEyeLocation().add(dir.clone().multiply(dist)).add(0, 2.0, 0);

        final ItemDisplay a = spawn(m, p, anchor, left);
        final ItemDisplay b = spawn(m, p, anchor, right);

        if (a == null || b == null) {
            if (a != null) m.removeEntity(a);
            if (b != null) m.removeEntity(b);
            return;
        }

        // Estado inicial: diminutas y fuera de escena
        set(a, 0, -3.0f, -0.4f, 0.01f, Math.toRadians(-45));
        set(b, 0, 3.0f, -0.4f, 0.01f, Math.toRadians(135));

        final int leaveAt = CLASH + 5 + hold;

        m.repeat(0, 1, t -> {
            if (!p.isOnline() || !a.isValid() || !b.isValid()) {
                m.removeEntity(a);
                m.removeEntity(b);
                return false;
            }

            if (t == FLY_IN_START) {
                set(a, FLY_IN_TICKS, -0.06f, 0f, base, 0);
                set(b, FLY_IN_TICKS, 0.06f, 0f, base, Math.toRadians(90));
                Fx.soundAt(p, anchor, "item.armor.equip_iron", 1.0f, 1.4f);
            } else if (t == CLASH) { // CORREGIDO: 'else if' en lugar de 'elseif'
                clash(p, anchor);
                set(a, 2, -0.06f, 0f, base * 1.45f, Math.toRadians(-8));
                set(b, 2, 0.06f, 0f, base * 1.45f, Math.toRadians(98));
            } else if (t == CLASH + 2) {
                set(a, 3, -0.06f, 0f, base, 0);
                set(b, 3, 0.06f, 0f, base, Math.toRadians(90));
            } else if (t > CLASH + 5 && t < leaveAt && (t - CLASH - 5) % 10 == 0) {
                // "respiracion": pulso suave mientras se mantienen cruzadas
                boolean up = ((t - CLASH - 5) / 10) % 2 == 0;
                float s = up ? base * 1.08f : base;
                double wob = Math.toRadians(up ? 4 : -4);
                set(a, 10, -0.06f, 0f, s, wob);
                set(b, 10, 0.06f, 0f, s, Math.toRadians(90) + wob);
                p.spawnParticle(Particle.ELECTRIC_SPARK, anchor, 8, 0.5, 0.5, 0.2, 0.1);
            } else if (t == leaveAt) {
                set(a, 8, -3.0f, 0.9f, 0.01f, Math.toRadians(-45));
                set(b, 8, 3.0f, 0.9f, 0.01f, Math.toRadians(135));
                Fx.soundAt(p, anchor, "item.armor.equip_chain", 1.0f, 1.2f);
            } else if (t >= leaveAt + 9) {
                m.removeEntity(a);
                m.removeEntity(b);
                return false;
            }

            return true;
        });
    }

    private static ItemDisplay spawn(AnimationManager m, Player p, Location loc, Material mat) {
        ItemDisplay display = p.getWorld().spawn(loc, ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(mat));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setPersistent(false);
        });
        m.trackEntity(display);
        return display;
    }

    private static void clash(Player p, Location loc) {
        p.spawnParticle(Particle.CRIT, loc, 30, 0.3, 0.3, 0.3, 0.1);
        Fx.soundAt(p, loc, "entity.player.attack.crit", 1.0f, 0.8f);
        Fx.soundAt(p, loc, "block.anvil.land", 0.8f, 1.2f);
    }

    private static void set(ItemDisplay d, int ticks, float tx, float ty, float scale, double roll) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(ticks);
        d.setTransformation(new Transformation(
            new Vector3f(tx, ty, 0f),
            new Quaternionf().rotateZ((float) roll),
            new Vector3f(scale, scale, scale),
            new Quaternionf()));
    }

    private static Material material(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}