package me.controller.animation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dos espadas (ItemDisplay) que vuelan una contra la otra, chocan con chispas y sonido,
 * quedan cruzadas "respirando" y al final se separan y desaparecen.
 * Las entidades solo las ve el jugador al que se le muestra la animacion.
 */
public final class SwordsAnimation {

    private static final float FLY_X = 3.2f;
    private static final int FLY_IN_START = 2;
    private static final int FLY_IN_TICKS = 8;
    private static final int CLASH = FLY_IN_START + FLY_IN_TICKS;

    private SwordsAnimation() {}

    public static void play(AnimationManager m, Player p) {
        FileConfiguration c = m.plugin().getConfig();
        Material left = material(c.getString("pvp.animation.sword-left", "DIAMOND_SWORD"), Material.DIAMOND_SWORD);
        Material right = material(c.getString("pvp.animation.sword-right", "NETHERITE_SWORD"), Material.NETHERITE_SWORD);
        int hold = Math.max(10, c.getInt("pvp.animation.hold-ticks", 45));
        float base = (float) c.getDouble("pvp.animation.sword-scale", 1.7);

        Vector dir = Fx.horizontalDir(p);
        double dist = Fx.clearDistance(p, dir, 4.0);
        Location anchor = p.getEyeLocation().add(dir.clone().multiply(dist)).add(0, 0.2, 0);

        final ItemDisplay a = spawn(m, p, anchor, left);
        final ItemDisplay b = spawn(m, p, anchor, right);
        if (a == null || b == null) {
            if (a != null) m.removeEntity(a);
            if (b != null) m.removeEntity(b);
            return;
        }
        // Estado inicial: diminutas y fuera de escena
        set(a, 0, -FLY_X, -0.4f, 0.01f, Math.toRadians(-45));
        set(b, 0, FLY_X, -0.4f, 0.01f, Math.toRadians(135));

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
            } else if (t == CLASH) {
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
                set(a, 8, -FLY_X, 0.9f, 0.01f, Math.toRadians(-45));
                set(b, 8, FLY_X, 0.9f, 0.01f, Math.toRadians(135));
                Fx.soundAt(p, anchor, "item.armor.equip_chain", 1.0f, 1.2f);
            } else if (t >= leaveAt + 9) {
                m.removeEntity(a);
                m.removeEntity(b);
                return false;
            }
            return true;
        });
    }

    /** Chispas, onda de choque y sonido del choque de espadas. */
    private static void clash(Player p, Location at) {
        p.spawnParticle(Particle.SWEEP_ATTACK, at, 3, 0.35, 0.35, 0.1, 0);
        p.spawnParticle(Particle.CRIT, at, 70, 0.2, 0.2, 0.2, 0.9);
        p.spawnParticle(Particle.ELECTRIC_SPARK, at, 45, 0.15, 0.15, 0.15, 0.7);
        p.spawnParticle(Particle.FLAME, at, 25, 0.1, 0.1, 0.1, 0.18);
        // Onda de choque: anillo de chispas en el plano de las espadas
        Vector dir = at.toVector().subtract(p.getEyeLocation().toVector()).setY(0);
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 0, 1);
        dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        for (int i = 0; i < 28; i++) {
            double ang = (Math.PI * 2 * i) / 28.0;
            double rx = Math.cos(ang), ry = Math.sin(ang);
            p.spawnParticle(Particle.ENCHANTED_HIT,
                    at.getX() + right.getX() * rx * 0.3, at.getY() + ry * 0.3, at.getZ() + right.getZ() * rx * 0.3,
                    0, right.getX() * rx, ry, right.getZ() * rx, 0.6);
        }
        Fx.soundAt(p, at, "entity.player.attack.sweep", 1.0f, 1.0f);
        Fx.soundAt(p, at, "entity.player.attack.crit", 1.0f, 0.9f);
        Fx.soundAt(p, at, "block.anvil.land", 0.5f, 1.8f);
        Fx.soundAt(p, at, "item.trident.hit_ground", 0.8f, 1.6f);
    }

    private static ItemDisplay spawn(AnimationManager m, Player p, Location at, Material mat) {
        try {
            ItemDisplay d = at.getWorld().spawn(at, ItemDisplay.class, e -> {
                e.setVisibleByDefault(false); // nadie la ve hasta que se la mostramos al jugador
                e.setPersistent(false);       // si el server se cae, no queda guardada en el mundo
                e.setItemStack(new ItemStack(mat));
                e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                e.setBillboard(Display.Billboard.CENTER); // siempre de frente a la camara
                e.setBrightness(new Display.Brightness(15, 15));
                e.setInterpolationDuration(0);
            });
            p.showEntity(m.plugin(), d);
            m.trackEntity(d);
            return d;
        } catch (Throwable ex) {
            m.plugin().getLogger().warning("No se pudo crear la animacion de espadas: " + ex.getMessage());
            return null;
        }
    }

    /** Cambia la transformacion; el cliente interpola suavemente durante 'ticks' ticks. */
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
            Material mat = Material.matchMaterial(name == null ? "" : name);
            return mat != null ? mat : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
