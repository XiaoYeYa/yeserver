package baby.sv.yeseverbf.command;

import baby.sv.yeseverbf.tpa.TpaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Registers the {@code /tpaccept} and {@code /tpdeny} commands used by the clickable chat
 * buttons to respond to a pending teleport request.
 */
public class TpaCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpaccept")
                .then(CommandManager.argument("requester", StringArgumentType.word())
                        .executes(ctx -> respond(ctx.getSource(),
                                StringArgumentType.getString(ctx, "requester"), true))));

        dispatcher.register(CommandManager.literal("tpdeny")
                .then(CommandManager.argument("requester", StringArgumentType.word())
                        .executes(ctx -> respond(ctx.getSource(),
                                StringArgumentType.getString(ctx, "requester"), false))));
    }

    private static int respond(ServerCommandSource source, String requesterName, boolean accept) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("只有玩家可以使用该命令。").formatted(Formatting.RED));
            return 0;
        }

        if (accept) {
            TpaManager.getInstance().accept(player, requesterName);
        } else {
            TpaManager.getInstance().deny(player, requesterName);
        }
        return 1;
    }
}
