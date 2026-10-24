package me.controller.animation;

import me.controller.utils.ColorUtil;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.entity.Player;

import java.awt.Color;

/**
 * Pone una imagen del resource pack en pantalla usando el TITULO (la imagen es un caracter de
 * una fuente propia) y el texto animado debajo (subtitulo + barra de accion).
 */
public final class ImageTitle {

    private static final String FONT = "sociolandia:image";
    private static final String EMPTY = "{\"text\":\"\"}";

    // Cuadros de entrada (grande -> tamano final) y respiro. Definidos en tools/generate_font.py
    private static final char[] LOGO = {'', '', '', '', '', '', ''};
    private static final char[] SKULL = {'', '', '', '', '', '', ''};
    private static final char SKULL_LOW = '';
    private static final char SKULL_LOWER = '';
    private static final char SKULL_HIGH = '';
    private static final char SHIFT_RIGHT = '';
    private static final char SHIFT_LEFT = '';
    private static final int FINAL = 5;
    private static final int BREATH = 6;

    private ImageTitle() {}

    private static String glyph(String chars) {
        TextComponent c = new TextComponent(chars);
        c.setFont(FONT);
        c.setColor(net.md_5.bungee.api.ChatColor.WHITE); // blanco = la imagen sale con sus colores originales
        return ComponentSerializer.toString(c);
    }

    private static String text(String legacy) {
        if (legacy == null || legacy.isEmpty()) return EMPTY;
        return ComponentSerializer.toString(TextComponent.fromLegacyText(legacy));
    }

    private static void send(AnimationManager m, Player p, String chars, String subLegacy,
                             int fadeIn, int stay, int fadeOut) {
        TitleSender.send(m.plugin(), p, glyph(chars), text(subLegacy), fadeIn, stay, fadeOut);
    }

    /** Bienvenida: el logo entra de golpe, respira, y debajo se escribe el texto con destello. */
    public static void welcome(AnimationManager m, Player p, String line1, String line2, Color a, Color b) {
        final String plain = ColorUtil.strip(line1);
        final String second = line2 == null ? "" : line2;
        final int n = plain.length();
        final int slam = 6;
        final int reveal = Math.max(8, Math.min(n, 14));
        final int shine = 10;
        final int total = slam + reveal + shine;

        m.repeat(0, 2, f -> {
            if (!p.isOnline()) return false;

            if (f < slam) {
                send(m, p, String.valueOf(LOGO[f]), "", f == 0 ? 3 : 0, 8, 0);
                if (f == 0) Fx.sound(p, "entity.player.levelup", 0.6f, 1.2f);
                if (f == slam - 1) Fx.sound(p, "entity.generic.explode", 0.35f, 1.6f);
                return true;
            }
            if (f < slam + reveal) {
                int k = f - slam;
                int shown = Math.max(1, (int) Math.ceil(n * (k + 1) / (double) reveal));
                String sub = n == 0 ? "" : ColorUtil.gradient(plain.substring(0, shown), a, b, true, k * 0.03, shown - 1, 2.5);
                send(m, p, String.valueOf(LOGO[k % 4 == 3 ? BREATH : FINAL]), sub, 0, 8, 0);
                Fx.sound(p, "block.note_block.hat", 0.35f, 1.3f + 0.04f * k);
                if (k >= reveal / 2 && !second.isEmpty()) {
                    int vis = (int) Math.ceil(ColorUtil.strip(second).length() * (k - reveal / 2 + 1) / (double) (reveal - reveal / 2));
                    m.sendActionBar(p, ColorUtil.reveal(second, vis));
                }
                return true;
            }
            if (f < total) {
                int k = f - slam - reveal;
                double sh = (k / (shine - 1.0)) * (n + 6) - 3;
                String sub = n == 0 ? "" : ColorUtil.gradient(plain, a, b, true, 0.15 + k * 0.01, sh, 3);
                send(m, p, String.valueOf(LOGO[k % 5 == 4 ? BREATH : FINAL]), sub, 0, 8, 0);
                if (!second.isEmpty()) m.sendActionBar(p, second);
                return true;
            }
            String sub = n == 0 ? "" : ColorUtil.gradient(plain, a, b, true, 0.15, -1, 0);
            send(m, p, String.valueOf(LOGO[FINAL]), sub, 0, 50, 20);
            if (!second.isEmpty()) m.sendActionBar(p, second);
            return false;
        });
    }

    /** Muerte: la calavera cae de golpe y se rie (tiembla y rebota) con "JAJAJA" y una burla debajo. */
    public static void death(AnimationManager m, Player p, String headline, String taunt, Color a, Color b) {
        final int slam = 6;
        final int shake = 26;
        final int total = slam + shake;
        final int n = headline.length();

        m.repeat(0, 2, f -> {
            if (!p.isOnline()) return false;

            if (f < slam) {
                send(m, p, String.valueOf(SKULL[f]), "", f == 0 ? 2 : 0, 8, 0);
                return true;
            }
            if (f < total) {
                int k = f - slam;
                String chars;
                switch (k % 4) {
                    case 0 -> chars = String.valueOf(SHIFT_RIGHT) + SKULL_LOW;
                    case 1 -> chars = String.valueOf(SHIFT_LEFT) + SKULL_HIGH;
                    case 2 -> chars = String.valueOf(SHIFT_RIGHT) + SKULL[FINAL];
                    default -> chars = String.valueOf(SHIFT_LEFT) + SKULL_LOWER;
                }
                String sub = ColorUtil.gradient(headline, a, b, true, k * 0.09, -1, 0);
                send(m, p, chars, sub, 0, 8, 0);
                if (!taunt.isEmpty()) m.sendActionBar(p, taunt);
                return true;
            }
            String sub = ColorUtil.gradient(headline, a, b, true, 0.0, -1, 0);
            send(m, p, String.valueOf(SKULL[FINAL]), sub, 0, 50, 20);
            if (!taunt.isEmpty()) m.sendActionBar(p, taunt);
            return false;
        });
    }
}
