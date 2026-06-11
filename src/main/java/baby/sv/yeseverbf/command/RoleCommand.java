package baby.sv.yeseverbf.command;

import baby.sv.yeseverbf.role.RoleManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /role guest|staff|status —— 管理员可把自己在 Staff / 游客之间切换。
 */
public final class RoleCommand {

    private RoleCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("role")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("guest")
                                .executes(ctx -> switchMode(ctx.getSource(), true)))
                        .then(CommandManager.literal("staff")
                                .executes(ctx -> switchMode(ctx.getSource(), false)))
                        .then(CommandManager.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
        );
    }

    private static int switchMode(ServerCommandSource source, boolean guest) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        RoleManager.setForcedGuest(player, guest);
        if (guest) {
            player.sendMessage(Text.literal("§d[!][夜喵喵] §a你已切换到 §2[Guest] §a游客模式"), false);
        } else {
            player.sendMessage(Text.literal("§d[!][夜喵喵] §a你已切换回 §c[Staff] §a管理模式"), false);
        }
        return 1;
    }

    private static int status(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        String role = RoleManager.isStaff(player) ? "§c[Staff]" : "§2[Guest]";
        player.sendMessage(Text.literal("§d[!][夜喵喵] §f当前身份: " + role), false);
        return 1;
    }
}
