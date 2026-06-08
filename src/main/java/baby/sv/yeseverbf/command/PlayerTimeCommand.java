package baby.sv.yeseverbf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTimeCommand {
    private static final Map<UUID, Long> PLAYER_TIME_OVERRIDES = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("ptime")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("day")
                                        .executes(ctx -> setTime(ctx.getSource(), 1000, null))
                                )
                                .then(CommandManager.literal("noon")
                                        .executes(ctx -> setTime(ctx.getSource(), 6000, null))
                                )
                                .then(CommandManager.literal("sunset")
                                        .executes(ctx -> setTime(ctx.getSource(), 12000, null))
                                )
                                .then(CommandManager.literal("night")
                                        .executes(ctx -> setTime(ctx.getSource(), 13000, null))
                                )
                                .then(CommandManager.literal("midnight")
                                        .executes(ctx -> setTime(ctx.getSource(), 18000, null))
                                )
                                .then(CommandManager.literal("sunrise")
                                        .executes(ctx -> setTime(ctx.getSource(), 23000, null))
                                )
                                .then(CommandManager.argument("ticks", LongArgumentType.longArg(0, 24000))
                                        .executes(ctx -> setTime(ctx.getSource(), LongArgumentType.getLong(ctx, "ticks"), null))
                                )
                        )
                        .then(CommandManager.literal("world")
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.literal("day")
                                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setTime(ctx.getSource(), 1000, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                                )
                                                .executes(ctx -> setTime(ctx.getSource(), 1000, null))
                                        )
                                        .then(CommandManager.literal("noon")
                                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setTime(ctx.getSource(), 6000, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                                )
                                                .executes(ctx -> setTime(ctx.getSource(), 6000, null))
                                        )
                                        .then(CommandManager.literal("sunset")
                                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setTime(ctx.getSource(), 12000, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                                )
                                                .executes(ctx -> setTime(ctx.getSource(), 12000, null))
                                        )
                                        .then(CommandManager.literal("night")
                                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setTime(ctx.getSource(), 13000, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                                )
                                                .executes(ctx -> setTime(ctx.getSource(), 13000, null))
                                        )
                                        .then(CommandManager.literal("midnight")
                                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setTime(ctx.getSource(), 18000, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                                )
                                                .executes(ctx -> setTime(ctx.getSource(), 18000, null))
                                        )
                                        .then(CommandManager.literal("sunrise")
                                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setTime(ctx.getSource(), 23000, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                                )
                                                .executes(ctx -> setTime(ctx.getSource(), 23000, null))
                                        )
                                        .then(CommandManager.argument("ticks", LongArgumentType.longArg(0, 24000))
                                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setTime(ctx.getSource(), LongArgumentType.getLong(ctx, "ticks"), IdentifierArgumentType.getIdentifier(ctx, "world")))
                                                )
                                                .executes(ctx -> setTime(ctx.getSource(), LongArgumentType.getLong(ctx, "ticks"), null))
                                        )
                                )
                                .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                                .executes(ctx -> resetTime(ctx.getSource(), IdentifierArgumentType.getIdentifier(ctx, "world")))
                                        )
                                        .executes(ctx -> resetTime(ctx.getSource(), null))
                                )
                        )
        );
    }

    private static int setTime(ServerCommandSource source, long time, Identifier worldId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }

        ServerWorld targetWorld = null;
        String worldName;

        if (worldId != null) {
            targetWorld = findWorld(source.getServer(), worldId);
            if (targetWorld == null) {
                source.sendError(Text.literal("§d[!][夜喵喵] §c找不到世界: " + worldId));
                return 0;
            }
            worldName = getWorldDisplayName(targetWorld);
        } else {
            targetWorld = player.getServerWorld();
            worldName = getWorldDisplayName(targetWorld);
        }

        PLAYER_TIME_OVERRIDES.put(player.getUuid(), time);

        long worldAge = targetWorld.getTime();
        player.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(worldAge, -time, false));

        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[个人时间] §f已将 §b" + worldName + " §f的时间设置为 §6" + time + " ticks"), false);
        return 1;
    }

    private static int resetTime(ServerCommandSource source, Identifier worldId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }

        ServerWorld targetWorld = null;
        String worldName;

        if (worldId != null) {
            targetWorld = findWorld(source.getServer(), worldId);
            if (targetWorld == null) {
                source.sendError(Text.literal("§d[!][夜喵喵] §c找不到世界: " + worldId));
                return 0;
            }
            worldName = getWorldDisplayName(targetWorld);
        } else {
            targetWorld = player.getServerWorld();
            worldName = getWorldDisplayName(targetWorld);
        }

        PLAYER_TIME_OVERRIDES.remove(player.getUuid());

        long worldAge = targetWorld.getTime();
        long timeOfDay = targetWorld.getTimeOfDay();
        player.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(worldAge, timeOfDay, false));

        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[个人时间] §f已将 §b" + worldName + " §f重置为服务器时间"), false);
        return 1;
    }

    private static ServerWorld findWorld(net.minecraft.server.MinecraftServer server, Identifier id) {
        if (id.equals(World.OVERWORLD.getValue())) return server.getOverworld();
        if (id.equals(World.NETHER.getValue())) return server.getWorld(World.NETHER);
        if (id.equals(World.END.getValue())) return server.getWorld(World.END);
        return server.getWorld(RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, id));
    }

    private static String getWorldDisplayName(ServerWorld world) {
        RegistryKey<World> key = world.getRegistryKey();
        if (key == World.OVERWORLD) return "主世界";
        if (key == World.NETHER) return "下界";
        if (key == World.END) return "末地";
        return key.getValue().getPath();
    }

    public static boolean hasTimeOverride(UUID uuid) {
        return PLAYER_TIME_OVERRIDES.containsKey(uuid);
    }

    public static long getTimeOverride(UUID uuid) {
        return PLAYER_TIME_OVERRIDES.getOrDefault(uuid, -1L);
    }

    public static void removePlayer(UUID uuid) {
        PLAYER_TIME_OVERRIDES.remove(uuid);
    }
}