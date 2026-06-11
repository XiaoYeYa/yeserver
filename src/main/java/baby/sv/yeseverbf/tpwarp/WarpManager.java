package baby.sv.yeseverbf.tpwarp;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 管理所有传送点：增删查 + 持久化到 config/yeseverbf_warps.json。
 */
public final class WarpManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of("config", "yeseverbf_warps.json");
    private static WarpManager INSTANCE;

    // 保持插入顺序
    public Map<String, TeleportPoint> warps = new LinkedHashMap<>();

    public static WarpManager get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                WarpManager loaded = GSON.fromJson(json, WarpManager.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    if (INSTANCE.warps == null) {
                        INSTANCE.warps = new LinkedHashMap<>();
                    }
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        INSTANCE = new WarpManager();
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

    public Collection<TeleportPoint> all() {
        return new ArrayList<>(warps.values());
    }

    public TeleportPoint get(String id) {
        return warps.get(id.toLowerCase());
    }

    public boolean add(TeleportPoint point) {
        String key = point.id.toLowerCase();
        if (warps.containsKey(key)) {
            return false;
        }
        warps.put(key, point);
        save();
        return true;
    }

    public boolean remove(String id) {
        boolean removed = warps.remove(id.toLowerCase()) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public void teleport(ServerPlayerEntity player, TeleportPoint point) {
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(point.dimension));
        ServerWorld world = player.getServer().getWorld(worldKey);
        if (world == null) {
            player.sendMessage(Text.literal("§d[!][夜喵喵] §c传送点所在的世界未加载: " + point.dimension), false);
            return;
        }
        player.teleport(world, point.x, point.y, point.z, Set.of(), point.yaw, point.pitch, false);
        player.sendMessage(Text.literal("§d[!][夜喵喵] §a已传送到 §b" + point.displayTitle()), false);
    }
}
