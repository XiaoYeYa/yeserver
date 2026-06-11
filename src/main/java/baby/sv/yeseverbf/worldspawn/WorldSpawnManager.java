package baby.sv.yeseverbf.worldspawn;

import baby.sv.yeseverbf.util.TeleportEffects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 每个世界的出生点（由管理员通过命令设置），持久化到 config/yeseverbf_worldspawns.json。
 */
public final class WorldSpawnManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of("config", "yeseverbf_worldspawns.json");
    private static WorldSpawnManager INSTANCE;

    public static class SpawnPoint {
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;

        public SpawnPoint() {
        }

        public SpawnPoint(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    // key = 世界 id 字符串，例如 "minecraft:overworld"
    public Map<String, SpawnPoint> spawns = new LinkedHashMap<>();

    public static WorldSpawnManager get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                WorldSpawnManager loaded = GSON.fromJson(json, WorldSpawnManager.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    if (INSTANCE.spawns == null) {
                        INSTANCE.spawns = new LinkedHashMap<>();
                    }
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        INSTANCE = new WorldSpawnManager();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(get()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void set(String worldId, SpawnPoint point) {
        spawns.put(worldId, point);
        save();
    }

    public SpawnPoint get(String worldId) {
        return spawns.get(worldId);
    }

    public boolean teleport(ServerPlayerEntity player, String worldId) {
        SpawnPoint point = spawns.get(worldId);
        if (point == null) {
            return false;
        }
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
        ServerWorld world = player.getServer().getWorld(worldKey);
        if (world == null) {
            player.sendMessage(Text.literal("§d[!][夜喵喵] §c该世界未加载: " + worldId), false);
            return false;
        }
        TeleportEffects.source((ServerWorld) player.getWorld(), player.getX(), player.getY(), player.getZ());
        player.teleport(world, point.x, point.y, point.z, Set.of(), point.yaw, point.pitch, false);
        TeleportEffects.arrive(player, "世界出生点 · " + worldId);
        return true;
    }
}
