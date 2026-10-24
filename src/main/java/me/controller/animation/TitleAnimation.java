package me.controller.animation;

import me.controller.utils.ColorUtil;
import org.bukkit.entity.Player;

import java.awt.Color;

/** Titulos animados: letra por letra con destello, o simbolos que convergen hacia el texto. */
public final class TitleAnimation {

    public enum Mode { TYPE, CONVERGE }

    /** left/right: marcas de texto a los lados del titulo (ej. ">>" y "<<"); vacias = sin marcas. */
    public record Spec(String title, String subtitle, Color from, Color to,
                       String left, String right, Mode mode, int stay, int fadeOut) {}

    private TitleAnimation() {}

    public static void play(AnimationManager m, Player p, Spec s) {
        final String title = s.title();
        final int n = title.length();
        if (n == 0) {
            p.sendTitle("", s.subtitle(), 5, s.stay(), s.fadeOut());
            return;
        }

        final boolean converge = s.mode() == Mode.CONVERGE;
        final int revealFrames = converge ? 12 : Math.max(8, Math.min(n, 16));
        final int shineFrames = 14;
        final int total = revealFrames + shineFrames;
        final int subLen = ColorUtil.strip(s.subtitle()).length();

        m.repeat(0, 2, f -> {
            if (!p.isOnline()) return false;

            if (f < revealFrames) {
                String t;
                if (converge) {
                    double prog = f / (revealFrames - 1.0);
                    double eased = 1 - Math.pow(1 - prog, 3);
                    int pad = (int) Math.round(18 * (1 - eased));
                    t = framed(pad, ColorUtil.gradient(title, s.from(), s.to(), true, f * 0.05, -1, 0), s);
                } else {
                    int shown = Math.max(1, (int) Math.ceil(n * (f + 1) / (double) revealFrames));
                    t = ColorUtil.gradient(title.substring(0, shown), s.from(), s.to(), true,
                            f * 0.03, shown - 1, 2.5);
                    Fx.sound(p, "block.note_block.hat", 0.35f, 1.3f + 0.04f * f);
                }
                p.sendTitle(t, "", 0, 8, 0);
                return true;
            }

            if (f < total) {
                int k = f - revealFrames;
                double shine = (k / (shineFrames - 1.0)) * (n + 6) - 3;
                String t = ColorUtil.gradient(title, s.from(), s.to(), true, 0.15 + k * 0.01, shine, 3);
                t = framed(1, t, s);
                int subShown = (int) Math.ceil(subLen * Math.min(1.0, (k + 1) / 7.0));
                p.sendTitle(t, ColorUtil.reveal(s.subtitle(), subShown), 0, 8, 0);
                return true;
            }

            // Cuadro final: queda fijo y se desvanece solo
            String t = framed(1, ColorUtil.gradient(title, s.from(), s.to(), true, 0.15, -1, 0), s);
            p.sendTitle(t, s.subtitle(), 0, s.stay(), s.fadeOut());
            return false;
        });
    }

    private static String framed(int pad, String body, Spec s) {
        String l = s.left() == null ? "" : s.left();
        String r = s.right() == null ? "" : s.right();
        if (l.isEmpty() && r.isEmpty()) return body;
        String spaces = " ".repeat(Math.max(0, pad));
        String c = ColorUtil.hex(s.from());
        return c + l + spaces + " " + body + " " + spaces + c + r;
    }
}
