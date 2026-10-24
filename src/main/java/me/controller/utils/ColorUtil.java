package me.controller.utils;

import org.bukkit.ChatColor;

import java.awt.Color;

public class ColorUtil {

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** Texto sin ningun codigo de color. */
    public static String strip(String text) {
        return ChatColor.stripColor(colorize(text));
    }

    public static Color parseHex(String hex, Color fallback) {
        try {
            if (hex == null) return fallback;
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

    public static String hex(Color c) {
        return net.md_5.bungee.api.ChatColor.of(c).toString();
    }

    /**
     * Degradado ondulante entre dos colores.
     *
     * @param phase      desplazamiento de la onda (0..1, anima el degradado)
     * @param shinePos   indice de caracter donde brilla un destello blanco (-1 = sin destello)
     * @param shineWidth ancho del destello en caracteres
     */
    public static String gradient(String text, Color a, Color b, boolean bold,
                                  double phase, double shinePos, double shineWidth) {
        StringBuilder sb = new StringBuilder();
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                sb.append(ch);
                continue;
            }
            double pos = n <= 1 ? 0 : (double) i / (n - 1);
            double w = 0.5 + 0.5 * Math.sin(2 * Math.PI * (pos + phase));
            Color c = lerp(a, b, w);
            if (shinePos >= 0 && shineWidth > 0) {
                double d = Math.abs(i - shinePos);
                if (d < shineWidth) {
                    c = lerp(c, Color.WHITE, 1 - d / shineWidth);
                }
            }
            sb.append(hex(c));
            if (bold) sb.append(ChatColor.BOLD);
            sb.append(ch);
        }
        return sb.toString();
    }

    /** Muestra solo los primeros N caracteres visibles de un texto ya coloreado (conserva los codigos). */
    public static String reveal(String colored, int visibleChars) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < colored.length(); i++) {
            char ch = colored.charAt(i);
            if (ch == ChatColor.COLOR_CHAR && i + 1 < colored.length()) {
                sb.append(ch).append(colored.charAt(++i));
                continue;
            }
            if (shown >= visibleChars) break;
            sb.append(ch);
            shown++;
        }
        return sb.toString();
    }
}
