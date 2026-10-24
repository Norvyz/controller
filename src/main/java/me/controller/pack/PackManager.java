package me.controller.pack;

import com.sun.net.httpserver.HttpServer;
import me.controller.ControllerPlugin;
import me.controller.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Construye el resource pack (fuente con el logo y la calavera) a partir de los archivos del .jar,
 * calcula su SHA-1, se lo envia a los jugadores y lleva la cuenta de quien ya lo cargo.
 * Opcionalmente sirve el .zip por HTTP desde el mismo servidor.
 */
public class PackManager {

    /** Id fijo: asi nuestro pack convive con otros packs del servidor y se reemplaza solo al cambiar. */
    public static final UUID PACK_ID = UUID.fromString("5f0c1a52-7a53-4b5e-9a0e-50c10a4d0001");

    private static final String[] FILES = {
            "pack.mcmeta",
            "assets/sociolandia/font/image.json",
            "assets/sociolandia/textures/font/logo.png",
            "assets/sociolandia/textures/font/calavera.png"
    };

    private final ControllerPlugin plugin;
    private File zip;
    private byte[] sha1;
    private String url;
    private HttpServer http;
    private ExecutorService httpPool;

    private final Set<UUID> requested = new HashSet<>();
    private final Set<UUID> loaded = new HashSet<>();
    private final Set<UUID> failed = new HashSet<>();

    public PackManager(ControllerPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ arranque

    public void start() {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("resource-pack.enabled", true)) {
            return;
        }
        try {
            build();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo construir el resource pack", e);
            return;
        }

        if (c.getBoolean("resource-pack.host.enabled", false)) {
            startHttp(c.getInt("resource-pack.host.port", 8123));
            String addr = c.getString("resource-pack.host.address", "");
            if (addr == null || addr.isBlank()) {
                plugin.getLogger().warning("resource-pack.host.address esta vacio: pon la IP o dominio publico del servidor.");
            } else if (http != null) {
                url = "http://" + addr.trim() + ":" + c.getInt("resource-pack.host.port", 8123) + "/pack.zip";
            }
        } else {
            String u = c.getString("resource-pack.url", "");
            if (u != null && !u.isBlank()) {
                url = u.trim();
            }
        }

        if (url == null) {
            plugin.getLogger().warning("Resource pack construido en " + zip.getPath() + " pero no hay URL. "
                    + "Sube ese .zip a un hosting y pon el enlace en resource-pack.url (o activa resource-pack.host). "
                    + "Mientras tanto las animaciones usan solo texto.");
        } else {
            plugin.getLogger().info("Resource pack listo: " + url + " (sha1 " + hex(sha1) + ")");
        }
    }

    public void stop() {
        if (http != null) {
            http.stop(0);
            http = null;
        }
        if (httpPool != null) {
            httpPool.shutdownNow();
            httpPool = null;
        }
    }

    /** Reconstruye el .zip solo si no existe o si cambio el contenido (asi el hash no cambia sin motivo). */
    private void build() throws Exception {
        File dir = plugin.getDataFolder();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("No se pudo crear " + dir);
        }
        zip = new File(dir, "SocioLandia-pack.zip");
        File marker = new File(dir, "pack.contentsha");

        MessageDigest contents = MessageDigest.getInstance("SHA-1");
        byte[][] data = new byte[FILES.length][];
        for (int i = 0; i < FILES.length; i++) {
            try (InputStream in = plugin.getResource("pack/" + FILES[i])) {
                if (in == null) throw new IllegalStateException("Falta pack/" + FILES[i] + " dentro del .jar");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                in.transferTo(bos);
                data[i] = bos.toByteArray();
            }
            contents.update(FILES[i].getBytes(StandardCharsets.UTF_8));
            contents.update(data[i]);
        }
        String contentHash = hex(contents.digest());

        boolean fresh = zip.exists() && marker.exists()
                && contentHash.equals(Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim());
        if (!fresh) {
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
                for (int i = 0; i < FILES.length; i++) {
                    ZipEntry e = new ZipEntry(FILES[i]);
                    e.setTimeLocal(LocalDateTime.of(2026, 1, 1, 0, 0)); // fecha fija
                    out.putNextEntry(e);
                    out.write(data[i]);
                    out.closeEntry();
                }
            }
            Files.writeString(marker.toPath(), contentHash, StandardCharsets.UTF_8);
            plugin.getLogger().info("Resource pack reconstruido: " + zip.getName());
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        sha1 = md.digest(Files.readAllBytes(zip.toPath()));
    }

    private void startHttp(int port) {
        try {
            final byte[] bytes = Files.readAllBytes(zip.toPath());
            http = HttpServer.create(new InetSocketAddress(port), 0);
            http.createContext("/pack.zip", ex -> {
                ex.getResponseHeaders().add("Content-Type", "application/zip");
                ex.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(bytes);
                }
            });
            httpPool = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "Controller-PackHost");
                t.setDaemon(true);
                return t;
            });
            http.setExecutor(httpPool);
            http.start();
            plugin.getLogger().info("Sirviendo el resource pack en el puerto " + port + " (abre ese puerto TCP en tu hosting).");
        } catch (Throwable t) {
            http = null;
            plugin.getLogger().log(Level.WARNING, "No se pudo iniciar el servidor HTTP del pack en el puerto " + port, t);
        }
    }

    // ------------------------------------------------------------------ jugadores

    public boolean isAvailable() {
        return url != null && sha1 != null;
    }

    /** Envia el pack una sola vez por sesion. */
    public void send(Player p) {
        if (!isAvailable() || !requested.add(p.getUniqueId())) return;
        FileConfiguration c = plugin.getConfig();
        String prompt = ColorUtil.colorize(c.getString("resource-pack.prompt", "Texturas de SocioLandia (logo y animaciones)"));
        p.setResourcePack(PACK_ID, url, sha1, prompt, c.getBoolean("resource-pack.force", false));
    }

    public boolean isLoaded(Player p) {
        return loaded.contains(p.getUniqueId());
    }

    public boolean hasFailed(Player p) {
        return failed.contains(p.getUniqueId());
    }

    public void markLoaded(Player p) {
        loaded.add(p.getUniqueId());
        failed.remove(p.getUniqueId());
    }

    public void markFailed(Player p) {
        failed.add(p.getUniqueId());
        loaded.remove(p.getUniqueId());
    }

    public void forget(Player p) {
        UUID id = p.getUniqueId();
        requested.remove(id);
        loaded.remove(id);
        failed.remove(id);
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
