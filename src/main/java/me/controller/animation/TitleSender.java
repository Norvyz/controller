package me.controller.animation;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.logging.Level;

/**
 * Envia titulos con componentes JSON (necesario para usar una fuente propia).
 * Spigot puro solo acepta texto plano en sendTitle, asi que:
 *   1) En Paper usa Adventure (limpio, sin mensajes en consola).
 *   2) En Spigot usa el comando /title ejecutado por la consola (funciona, pero la consola
 *      imprime "Showing new title..." por cada cuadro; se recomienda Paper).
 */
public final class TitleSender {

    private static boolean tried;
    private static boolean adventure;
    private static Object gson;
    private static Method deserialize;
    private static Method newTimes;
    private static Method newTitle;
    private static Method showTitle;

    private TitleSender() {}

    public static boolean usingAdventure() {
        init();
        return adventure;
    }

    private static synchronized void init() {
        if (tried) return;
        tried = true;
        try {
            Class<?> compC = Class.forName("net.kyori.adventure.text.Component");
            Class<?> gsonC = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
            Class<?> titleC = Class.forName("net.kyori.adventure.title.Title");
            Class<?> timesC = Class.forName("net.kyori.adventure.title.Title$Times");
            Class<?> audC = Class.forName("net.kyori.adventure.audience.Audience");

            gson = gsonC.getMethod("gson").invoke(null);
            for (Method m : gson.getClass().getMethods()) {
                if (m.getName().equals("deserialize") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(String.class)
                        && compC.isAssignableFrom(m.getReturnType())) {
                    deserialize = m;
                    break;
                }
            }
            if (deserialize == null) {
                deserialize = gson.getClass().getMethod("deserialize", Object.class);
            }
            newTimes = timesC.getMethod("times", Duration.class, Duration.class, Duration.class);
            newTitle = titleC.getMethod("title", compC, compC, timesC);
            showTitle = audC.getMethod("showTitle", titleC);
            adventure = true;
        } catch (Throwable t) {
            adventure = false;
        }
    }

    /** titleJson / subtitleJson: componentes de texto en JSON. Tiempos en ticks. */
    public static void send(Plugin plugin, Player p, String titleJson, String subtitleJson,
                            int fadeIn, int stay, int fadeOut) {
        init();
        if (adventure) {
            try {
                Object times = newTimes.invoke(null, Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L));
                Object t = deserialize.invoke(gson, titleJson);
                Object s = deserialize.invoke(gson, subtitleJson);
                showTitle.invoke(p, newTitle.invoke(null, t, s, times));
                return;
            } catch (Throwable ex) {
                adventure = false; // no insistir: pasar al metodo por comandos
                plugin.getLogger().log(Level.WARNING, "Adventure fallo, usando /title: " + ex);
            }
        }
        CommandSender console = Bukkit.getConsoleSender();
        String who = p.getUniqueId().toString();
        Bukkit.dispatchCommand(console, "title " + who + " times " + fadeIn + " " + stay + " " + fadeOut);
        Bukkit.dispatchCommand(console, "title " + who + " subtitle " + subtitleJson);
        Bukkit.dispatchCommand(console, "title " + who + " title " + titleJson);
    }
}
