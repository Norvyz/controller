package me.controller.animation;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * Convierte el logo del servidor (plugins/Controller/logo.png) en una cuadricula de pixeles
 * y lo dibuja con particulas de color flotando frente al jugador. No necesita resource pack.
 */
public class LogoRenderer {

    /** Un "pixel" del logo: posicion normalizada (u,v), color y un rango aleatorio para disolverlo. */
    record Pt(double u, double v, int r, int g, int b, double rank) {}

    private final Plugin plugin;
    private final String resource;   // archivo dentro del jar / carpeta del plugin (logo.png, calavera.png)
    private final String prefix;     // seccion de config (logo, death.image)
    private final boolean mock;      // true = estilo burla: tiembla, rebota y suelta llamas de alma
    private final List<Pt> points = new ArrayList<>();
    private double aspect = 1.0; // alto / ancho
    private boolean ready = false;

    public LogoRenderer(Plugin plugin, String resource, String prefix, boolean mock) {
        this.plugin = plugin;
        this.resource = resource;
        this.prefix = prefix;
        this.mock = mock;
    }

    public boolean isReady() {
        return ready;
    }

    public int pointCount() {
        return points.size();
    }

    public double aspect() {
        return aspect;
    }

    /** (Re)carga el logo. Si no existe en la carpeta del plugin, copia el que viene dentro del .jar. */
    public void load() {
        ready = false;
        points.clear();
        try {
            File file = new File(plugin.getDataFolder(), resource);
            if (!file.exists()) {
                plugin.saveResource(resource, false);
            }
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                plugin.getLogger().warning("" + resource + " no es una imagen valida; esa animacion queda desactivada.");
                return;
            }
            int res = Math.max(8, Math.min(96, plugin.getConfig().getInt(prefix + ".resolution", 34)));
            build(img, res);
            ready = !points.isEmpty();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo cargar " + resource, e);
        }
    }

    /** Reduce la imagen a res columnas promediando bloques de pixeles (ponderado por transparencia). */
    void build(BufferedImage img, int res) {
        int w = img.getWidth();
        int h = img.getHeight();
        int gw = res;
        int gh = Math.max(1, (int) Math.round(res * (h / (double) w)));
        aspect = gh / (double) gw;
        Random rnd = new Random(7);

        for (int gy = 0; gy < gh; gy++) {
            for (int gx = 0; gx < gw; gx++) {
                int x0 = gx * w / gw, x1 = Math.max(x0 + 1, (gx + 1) * w / gw);
                int y0 = gy * h / gh, y1 = Math.max(y0 + 1, (gy + 1) * h / gh);
                double sa = 0, sr = 0, sg = 0, sb = 0;
                int cnt = 0;
                for (int y = y0; y < y1 && y < h; y++) {
                    for (int x = x0; x < x1 && x < w; x++) {
                        int argb = img.getRGB(x, y);
                        double a = ((argb >>> 24) & 0xFF) / 255.0;
                        sa += a;
                        sr += ((argb >> 16) & 0xFF) * a;
                        sg += ((argb >> 8) & 0xFF) * a;
                        sb += (argb & 0xFF) * a;
                        cnt++;
                    }
                }
                if (cnt == 0 || sa / cnt < 0.4 || sa == 0) continue;
                points.add(new Pt((gx + 0.5) / gw, (gy + 0.5) / gh,
                        (int) Math.round(sr / sa), (int) Math.round(sg / sa), (int) Math.round(sb / sa),
                        rnd.nextDouble()));
            }
        }
    }

    /** Muestra el logo flotando frente al jugador: aparece creciendo, brilla y se disuelve. */
    public void show(AnimationManager m, Player p) {
        if (!ready) return;
        FileConfiguration c = plugin.getConfig();
        double size = c.getDouble(prefix + ".size", 5.5);
        double dist = c.getDouble(prefix + ".distance", 6.0);
        int duration = Math.max(40, c.getInt(prefix + ".duration-ticks", 100));
        int refresh = Math.max(2, c.getInt(prefix + ".refresh-ticks", 4));
        float dust = (float) c.getDouble(prefix + ".particle-size", 1.2);

        Vector dir = Fx.horizontalDir(p);
        double clear = Fx.clearDistance(p, dir, dist);
        size *= clear / dist; // si hay una pared cerca, el logo se achica en vez de quedar dentro del bloque
        final double fsize = size;
        final double height = fsize * aspect;

        Location anchor = p.getEyeLocation().add(dir.clone().multiply(clear));
        anchor.add(0, 0.5 + Math.max(0, (height - 3.0) * 0.1), 0);
        final Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        final int fadeTicks = 18;

        m.repeat(0, refresh, i -> {
            if (!p.isOnline() || !p.getWorld().equals(anchor.getWorld())) return false;
            int t = i * refresh;
            if (t > duration) return false;

            double grow = Math.min(1.0, t / 14.0);
            double s = easeOutBack(grow);
            double fade = t > duration - fadeTicks ? (t - (duration - fadeTicks)) / (double) fadeTicks : 0;
            double bob = (mock ? 0.14 : 0.06) * Math.sin(t * (mock ? 0.5 : 0.12));
            double shake = (mock && t >= 14 && fade == 0) ? 0.07 * Math.sin(t * 2.3) : 0;
            double shineCenter = ((t * 0.035) % 1.8) - 0.4;

            if (t >= 12 && t < 12 + refresh) {
                if (mock) {
                    p.spawnParticle(Particle.SOUL_FIRE_FLAME, anchor, 80, fsize * 0.4, height * 0.45, 0.3, 0.03);
                } else {
                    p.spawnParticle(Particle.FIREWORK, anchor, 70, fsize * 0.45, height * 0.45, 0.3, 0.06);
                    Fx.soundAt(p, anchor, "entity.firework_rocket.blast", 0.8f, 1.1f);
                    Fx.soundAt(p, anchor, "entity.firework_rocket.twinkle", 0.8f, 1.0f);
                }
            }

            for (Pt pt : points) {
                if (fade > 0 && pt.rank() < fade) continue;
                double lx = (pt.u() - 0.5) * fsize * s + shake;
                double ly = (0.5 - pt.v()) * height * s + bob + fade * pt.rank() * 0.9;
                double x = anchor.getX() + right.getX() * lx;
                double y = anchor.getY() + ly;
                double z = anchor.getZ() + right.getZ() * lx;

                int r = pt.r(), g = pt.g(), b = pt.b();
                double d = Math.abs((pt.u() + pt.v()) / 2 - shineCenter);
                if (d < 0.07) {
                    double k = (1 - d / 0.07) * 0.8;
                    r += (int) ((255 - r) * k);
                    g += (int) ((255 - g) * k);
                    b += (int) ((255 - b) * k);
                }
                p.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(r, g, b), dust));
            }
            return true;
        });
    }

    private static double easeOutBack(double x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        double v = x - 1;
        return 1 + c3 * v * v * v + c1 * v * v;
    }
}
