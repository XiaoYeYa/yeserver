package baby.sv.yeseverbf.command;

import baby.sv.yeseverbf.tpwarp.TeleportMenu;
import baby.sv.yeseverbf.tpwarp.TeleportPoint;
import baby.sv.yeseverbf.tpwarp.WarpManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /warp add|settitle|setdesc|remove|list|tp|menu —— 管理传送点。
 */
public final class WarpCommand {

    private WarpCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("warp")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> add(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(CommandManager.literal("settitle")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .then(CommandManager.argument("title", StringArgumentType.greedyString())
                                                .executes(ctx -> setTitle(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "title"))))))
                        .then(CommandManager.literal("setdesc")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .then(CommandManager.argument("description", StringArgumentType.greedyString())
                                                .executes(ctx -> setDesc(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "description"))))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> remove(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(CommandManager.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(CommandManager.literal("tp")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> tp(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(CommandManager.literal("menu")
                                .executes(ctx -> menu(ctx.getSource())))
        );
    }

    private static int add(ServerCommandSource source, String id) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        TeleportPoint point = new TeleportPoint(
                id, id, "", dimension,
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch());
        if (!WarpManager.get().add(point)) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c传送点已存在: " + id));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已添加传送点 §b" + id
                + " §7(" + dimension + " " + (int) point.x + "," + (int) point.y + "," + (int) point.z + ")"
                + "§7，可用 /warp settitle 和 /warp setdesc 设置标题与描述"), true);
        return 1;
    }

    private static int setTitle(ServerCommandSource source, String id, String title) {
        TeleportPoint point = WarpManager.get().get(id);
        if (point == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c不存在的传送点: " + id));
            return 0;
        }
        point.title = title;
        WarpManager.save();
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已设置 §b" + id + " §a的标题为: " + title), true);
        return 1;
    }

    private static int setDesc(ServerCommandSource source, String id, String description) {
        TeleportPoint point = WarpManager.get().get(id);
        if (point == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c不存在的传送点: " + id));
            return 0;
        }
        point.description = description;
        WarpManager.save();
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已设置 §b" + id + " §a的描述"), true);
        return 1;
    }

    private static int remove(ServerCommandSource source, String id) {
        if (!WarpManager.get().remove(id)) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c不存在的传送点: " + id));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §a已删除传送点 §b" + id), true);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        var all = WarpManager.get().all();
        if (all.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §7暂无传送点"), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e传送点列表 (" + all.size() + "):"), false);
        for (TeleportPoint point : all) {
            source.sendFeedback(() -> Text.literal("§7- §b" + point.id + " §f" + point.displayTitle()
                    + " §7@ " + point.dimension + " "
                    + (int) point.x + "," + (int) point.y + "," + (int) point.z), false);
        }
        return 1;
    }

    private static int tp(ServerCommandSource source, String id) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        TeleportPoint point = WarpManager.get().get(id);
        if (point == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c不存在的传送点: " + id));
            return 0;
        }
        WarpManager.get().teleport(player, point);
        return 1;
    }

    private static int menu(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        TeleportMenu.open(player);
        return 1;
    }
}
