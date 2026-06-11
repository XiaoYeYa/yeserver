package baby.sv.yeseverbf.command;

import baby.sv.yeseverbf.worldspawn.WorldSpawnManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /worldspawn set|tp —— 设置 / 传送到每个世界的出生点。
 */
public final class WorldSpawnCommand {

    private WorldSpawnCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("worldspawn")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("set")
                                .executes(ctx -> set(ctx.getSource())))
                        .then(CommandManager.literal("tp")
                                .executes(ctx -> tpCurrent(ctx.getSource()))
                                .then(CommandManager.argument("world", StringArgumentType.greedyString())
                                        .executes(ctx -> tp(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "world")))))
                        .then(CommandManager.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
        );
    }

    private static int set(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        String worldId = player.getWorld().getRegistryKey().getValue().toString();
        WorldSpawnManager.get().set(worldId, new WorldSpawnManager.SpawnPoint(
                player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已设置世界 §b" + worldId
                + " §a的出生点为当前位置 §7(" + (int) player.getX() + ","
                + (int) player.getY() + "," + (int) player.getZ() + ")"), true);
        return 1;
    }

    private static int tpCurrent(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        return tp(source, player.getWorld().getRegistryKey().getValue().toString());
    }

    private static int tp(ServerCommandSource source, String worldId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        if (!WorldSpawnManager.get().teleport(player, worldId)) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c世界 " + worldId + " 还没有设置出生点"));
            return 0;
        }
        player.sendMessage(Text.literal("§d[!][夜喵喵] §a已传送到 §b" + worldId + " §a的出生点"), false);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        var spawns = WorldSpawnManager.get().spawns;
        if (spawns.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §7暂无已设置的世界出生点"), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e世界出生点列表 (" + spawns.size() + "):"), false);
        spawns.forEach((world, point) -> source.sendFeedback(() -> Text.literal(
                "§7- §b" + world + " §7@ "
                        + (int) point.x + "," + (int) point.y + "," + (int) point.z), false));
        return 1;
    }
}
