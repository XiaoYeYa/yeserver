package baby.sv.yeseverbf.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "yeseverbf.json");
    private static ModConfig INSTANCE;

    public String backupDirectory = "backups";
    public int maxBackups = 20;
    public int autoBackupIntervalMinutes = 120;
    public int httpApiPort = 7655;
    public String httpApiToken = "change-me-to-a-secret-token";

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        INSTANCE = new ModConfig();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
