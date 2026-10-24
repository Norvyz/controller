package me.controller.animation;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** Utilidades chicas de sonido y posicion para las animaciones. */
public final class Fx {

    private Fx() {}

    /** Sonido por nombre de config (ENTITY_ENDER_DRAGON_GROWL) o por clave (entity.player.levelup). */
    public static void sound(Player p, String name, float volume, float pitch) {
        if (p == null || name == null || name.isEmpty()) return;
        try {
            if (name.contains(".") || name.contains(":")) {
                p.playSound(p.getLocation(), name, volume, pitch);
            } else {
                Sound s = Sound.valueOf(name.toUpperCase());
                p.playSound(p.getLocation(), s, volume, pitch);
            }
        } catch (Exception ignored) {
            // sonido inexistente: se ignora
        }
    }

    /** Sonido con clave namespaced en una posicion concreta. */
    public static void soundAt(Player p, Location loc, String key, float volume, float pitch) {
        try {
            p.playSound(loc, key, volume, pitch);
        } catch (Exception ignored) {
        }
    }

    /** Direccion horizontal hacia donde mira el jugador. */
    public static Vector horizontalDir(Player p) {
        Vector dir = p.getEyeLocation().getDirection();
        dir.setY(0);
        if (dir.lengthSquared() < 1.0E-4) {
            dir = new Vector(0, 0, 1);
        }
        return dir.normalize();
    }

    /** Distancia libre (sin bloques) delante del jugador, hasta maxDistance. */
    public static double clearDistance(Player p, Vector dir, double maxDistance) {
        Location eye = p.getEyeLocation();
        RayTraceResult hit = p.getWorld().rayTraceBlocks(eye, dir, maxDistance, FluidCollisionMode.NEVER, true);
        if (hit == null) return maxDistance;
        double d = hit.getHitPosition().distance(eye.toVector()) - 0.6;
        return Math.max(1.5, Math.min(maxDistance, d));
    }
}
