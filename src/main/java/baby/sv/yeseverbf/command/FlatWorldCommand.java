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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import java.util.Collections;

public class FlatWorldCommand {
    public static final RegistryKey<World> FLAT_WORLD_KEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("yeseverbf", "flat_world"));

    private static final Map<UUID, ReturnPosition> RETURN_POSITIONS = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("flatworld")
                        .requires(source -> source.hasPermissionLevel(2)) // 允许命令方块执行
                        .then(CommandManager.literal("join")
                                .executes(ctx -> joinFlatWorld(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayer())))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .executes(ctx -> joinFlatWorld(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "target")))
                                )
                        )
                        .then(CommandManager.literal("back")
                                .executes(ctx -> leaveFlatWorld(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayer())))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .executes(ctx -> leaveFlatWorld(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "target")))
                                )
                        )
                        .then(CommandManager.literal("info")
                                .executes(ctx -> showInfo(ctx.getSource()))
                        )
        );
    }

    private static int joinFlatWorld(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        MinecraftServer server = source.getServer();
        ServerWorld flatWorld = server.getWorld(FLAT_WORLD_KEY);

        if (flatWorld == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c超平坦世界未加载，请检查模组配置"));
            return 0;
        }

        int count = 0;
        for (ServerPlayerEntity player : targets) {
            if (player == null) continue;

            if (player.getWorld().getRegistryKey() == FLAT_WORLD_KEY) {
                if (targets.size() == 1) {
                    source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e" + player.getName().getString() + " 已经在超平坦世界中了"), false);
                }
                continue;
            }

            RETURN_POSITIONS.put(player.getUuid(), new ReturnPosition(
                    player.getWorld().getRegistryKey(),
                    player.getBlockPos(),
                    player.getYaw(),
                    player.getPitch()
            ));

            BlockPos spawnPos = flatWorld.getSpawnPos();
            player.teleport(flatWorld, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch(), false);
            player.sendMessage(Text.literal("§d[!][夜喵喵] §a已传送到超平坦世界！§7使用 /flatworld back 返回"), false);
            count++;
        }
        
        if (count > 0 && source.getEntity() != null) {
            source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已将 " + targets.size() + " 名玩家传送到超平坦世界"), true);
        }
        return count;
    }

    private static int leaveFlatWorld(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
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
        ServerWorld flatWorld = server.getWorld(FLAT_WORLD_KEY);

        if (flatWorld == null) {
            source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §c超平坦世界未加载"), false);
            return 0;
        }

        int playerCount = flatWorld.getPlayers().size();
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[超平坦世界] §f状态: §a已加载 §7| §f在线: §b" + playerCount + " 人"), false);
        return 1;
    }

    public static void onPlayerDisconnect(UUID uuid) {
        RETURN_POSITIONS.remove(uuid);
    }

    private record ReturnPosition(RegistryKey<World> world, BlockPos pos, float yaw, float pitch) {}
}
