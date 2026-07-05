package baby.sv.yeseverbf.command;

import baby.sv.yeseverbf.role.RoleManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /spectate [on|off] —— 游客在旁观者 / 冒险模式之间切换。
 * 无参数时按当前状态切换；on=旁观，off=冒险。所有玩家可用，仅对游客生效。
 */
public final class SpectateCommand {

    private SpectateCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("spectate")
                        .executes(ctx -> toggle(ctx.getSource()))
                        .then(CommandManager.literal("on")
                                .executes(ctx -> set(ctx.getSource(), true)))
                        .then(CommandManager.literal("off")
                                .executes(ctx -> set(ctx.getSource(), false)))
        );
    }

    private static int toggle(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        return set(source, !RoleManager.isSpectatorGuest(player));
    }

    private static int set(ServerCommandSource source, boolean spectate) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }
        if (!RoleManager.setSpectator(player, spectate)) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c只有游客可以使用该命令"));
            return 0;
        }
        if (spectate) {
            player.sendMessage(Text.literal("§d[!][夜喵喵] §a你已切换到 §5旁观者 §a模式"), false);
        } else {
            player.sendMessage(Text.literal("§d[!][夜喵喵] §a你已切换回 §2冒险 §a模式"), false);
        }
        return 1;
    }
}
