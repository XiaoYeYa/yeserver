package baby.sv.yeseverbf.api;

import baby.sv.yeseverbf.backup.BackupManager;
import baby.sv.yeseverbf.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class HttpApiServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("yeseverbf-api");
    private static final Gson GSON = new Gson();

    private HttpServer httpServer;
    private final MinecraftServer mcServer;
    private final BackupManager backupManager;

    public HttpApiServer(MinecraftServer mcServer, BackupManager backupManager) {
        this.mcServer = mcServer;
        this.backupManager = backupManager;
    }

    public void start() {
        int port = ModConfig.get().httpApiPort;
        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            httpServer.setExecutor(Executors.newFixedThreadPool(2));

            httpServer.createContext("/api/backup/start", this::handleBackupStart);
            httpServer.createContext("/api/backup/list", this::handleBackupList);
            httpServer.createContext("/api/backup/delete", this::handleBackupDelete);
            httpServer.createContext("/api/backup/status", this::handleBackupStatus);
            httpServer.createContext("/api/server/status", this::handleServerStatus);
            httpServer.createContext("/api/server/players", this::handlePlayerList);

            httpServer.start();
            LOGGER.info("HTTP API server started on 127.0.0.1:{}", port);
        } catch (IOException e) {
            LOGGER.error("Failed to start HTTP API server", e);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(1);
            LOGGER.info("HTTP API server stopped");
        }
    }

    private boolean authenticate(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (token == null || !token.equals("Bearer " + ModConfig.get().httpApiToken)) {
            sendJson(exchange, 401, Map.of("error", "Unauthorized"));
            return false;
        }
        return true;
    }

    private void handleBackupStart(HttpExchange exchange) {
        if (!authenticate(exchange)) return;
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        if (backupManager.isBackingUp()) {
            sendJson(exchange, 409, Map.of("success", false, "message", "已有备份正在进行中"));
            return;
        }
        backupManager.performBackup("HTTP API").thenAccept(result -> {
            // result already logged and broadcast
        });
        sendJson(exchange, 200, Map.of("success", true, "message", "备份已启动"));
    }

    private void handleBackupList(HttpExchange exchange) {
        if (!authenticate(exchange)) return;
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        List<BackupManager.BackupInfo> backups = backupManager.listBackups();
        List<Map<String, Object>> list = new ArrayList<>();
        for (BackupManager.BackupInfo info : backups) {
            list.add(Map.of("name", info.name(), "sizeMB", info.sizeBytes() / (1024 * 1024)));
        }
        sendJson(exchange, 200, Map.of("success", true, "backups", list));
    }

    private void handleBackupDelete(HttpExchange exchange) {
        if (!authenticate(exchange)) return;
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        try {
            String body = readBody(exchange);
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            String name = json.get("name").getAsString();
            boolean ok = backupManager.deleteBackup(name);
            sendJson(exchange, ok ? 200 : 404, Map.of("success", ok, "message", ok ? "已删除" : "备份不存在"));
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "Invalid request body"));
        }
    }

    private void handleBackupStatus(HttpExchange exchange) {
        if (!authenticate(exchange)) return;
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        sendJson(exchange, 200, Map.of(
                "success", true,
                "backing_up", backupManager.isBackingUp(),
                "total_backups", backupManager.listBackups().size()
        ));
    }

    private void handleServerStatus(HttpExchange exchange) {
        if (!authenticate(exchange)) return;
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        sendJson(exchange, 200, Map.of(
                "success", true,
                "online_players", mcServer.getCurrentPlayerCount(),
                "max_players", mcServer.getMaxPlayerCount(),
                "tps", Math.round(mcServer.getTickManager().getMillisPerTick() > 0 ? 1000.0 / mcServer.getTickManager().getMillisPerTick() : 20),
                "motd", mcServer.getServerMotd()
        ));
    }

    private void handlePlayerList(HttpExchange exchange) {
        if (!authenticate(exchange)) return;
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        List<String> players = new ArrayList<>();
        for (ServerPlayerEntity player : mcServer.getPlayerManager().getPlayerList()) {
            players.add(player.getName().getString());
        }
        sendJson(exchange, 200, Map.of("success", true, "players", players));
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) {
        try {
            String json = GSON.toJson(data);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to send HTTP response", e);
        }
    }
}
