package baby.sv.yeseverbf.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class Forest1Command {
    public static final RegistryKey<World> FOREST1_WORLD_KEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("yeseverbf", "forest1"));

    private static final Map<UUID, ReturnPosition> RETURN_POSITIONS = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("forest1")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("join")
                                .executes(ctx -> joinForest1(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayer())))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .executes(ctx -> joinForest1(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "target")))
                                )
                        )
                        .then(CommandManager.literal("back")
                                .executes(ctx -> leaveForest1(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayer())))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .executes(ctx -> leaveForest1(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "target")))
                                )
                        )
                        .then(CommandManager.literal("info")
                                .executes(ctx -> showInfo(ctx.getSource()))
                        )
        );
    }

    private static int joinForest1(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        MinecraftServer server = source.getServer();
        ServerWorld forest1World = server.getWorld(FOREST1_WORLD_KEY);

        if (forest1World == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §cforest1 世界未加载，请检查 forest1 文件夹是否存在"));
            return 0;
        }

        int count = 0;
        for (ServerPlayerEntity player : targets) {
            if (player == null) continue;

            if (player.getWorld().getRegistryKey() == FOREST1_WORLD_KEY) {
                if (targets.size() == 1) {
                    source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e" + player.getName().getString() + " 已经在 forest1 世界中了"), false);
                }
                continue;
            }

            RETURN_POSITIONS.put(player.getUuid(), new ReturnPosition(
                    player.getWorld().getRegistryKey(),
                    player.getBlockPos(),
                    player.getYaw(),
                    player.getPitch()
            ));

            player.teleport(forest1World, 1233.12, 42.00, 822.26, Set.of(), player.getYaw(), player.getPitch(), false);
            player.sendMessage(Text.literal("§d[!][夜喵喵] §a已传送到 forest1 世界！§7使用 /forest1 back 返回"), false);
            count++;
        }

        if (count > 0 && source.getEntity() != null) {
            source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已将 " + targets.size() + " 名玩家传送到 forest1 世界"), true);
        }
        return count;
    }

    private static int leaveForest1(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        MinecraftServer server = source.getServer();
        int count = 0;

        for (ServerPlayerEntity player : targets) {
            if (player == null) continue;

            ReturnPosition returnPos = RETURN_POSITIONS.remove(player.getUuid());

            if (returnPos != null) {
                ServerWorld returnWorld = server.getWorld(returnPos.world());
                if (returnWorld != null) {
                    player.teleport(returnWorld, returnPos.pos().getX() + 0.5, returnPos.pos().getY() + 0.0, returnPos.pos().getZ() + 0.5, Set.of(), returnPos.yaw(), returnPos.pitch(), false);
                    player.sendMessage(Text.literal("§d[!][夜喵喵] §a已返回原来的世界"), false);
                    count++;
                    continue;
                }
            }

            ServerWorld overworld = server.getOverworld();
            BlockPos spawnPos = overworld.getSpawnPos();
            player.teleport(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch(), false);
            player.sendMessage(Text.literal("§d[!][夜喵喵] §a已返回主世界出生点"), false);
            count++;
        }

        if (count > 0 && source.getEntity() != null) {
            source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已将 " + targets.size() + " 名玩家送回主世界"), true);
        }
        return count;
    }

    private static int showInfo(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        ServerWorld forest1World = server.getWorld(FOREST1_WORLD_KEY);

        if (forest1World == null) {
            source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §cforest1 世界未加载"), false);
            return 0;
        }

        int playerCount = forest1World.getPlayers().size();
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[forest1] §f状态: §a已加载 §7| §f在线: §b" + playerCount + " 人"), false);
        return 1;
    }

    public static void onPlayerDisconnect(UUID uuid) {
        RETURN_POSITIONS.remove(uuid);
    }

    private record ReturnPosition(RegistryKey<World> world, BlockPos pos, float yaw, float pitch) {}
}
