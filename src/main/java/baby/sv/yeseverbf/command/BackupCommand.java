package baby.sv.yeseverbf.command;

import baby.sv.yeseverbf.backup.BackupManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

public class BackupCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, Supplier<BackupManager> backupManagerSupplier) {
        dispatcher.register(
                CommandManager.literal("backup")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("start")
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    BackupManager backupManager = backupManagerSupplier.get();
                                    String trigger = source.getName();
                                    backupManager.performBackup(trigger).thenAccept(result -> {
                                        if (!result.success()) {
                                            source.sendError(Text.literal("§d[!][夜喵喵] §c备份失败: " + result.message()));
                                        }
                                    });
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("list")
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    List<BackupManager.BackupInfo> backups = backupManagerSupplier.get().listBackups();
                                    if (backups.isEmpty()) {
                                        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[备份] §f没有找到备份"), false);
                                        return 1;
                                    }
                                    source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[备份] §f备份列表 (" + backups.size() + " 个):"), false);
                                    for (BackupManager.BackupInfo info : backups) {
                                        long mb = info.sizeBytes() / (1024 * 1024);
                                        source.sendFeedback(() -> Text.literal("  §d[!][夜喵喵] §7- §f" + info.name() + " §7(" + mb + "MB)"), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            boolean ok = backupManagerSupplier.get().deleteBackup(name);
                                            ServerCommandSource source = ctx.getSource();
                                            if (ok) {
                                                source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[备份] §a已删除备份: " + name), true);
                                            } else {
                                                source.sendError(Text.literal("§d[!][夜喵喵] §c删除失败或备份不存在: " + name));
                                            }
                                            return ok ? 1 : 0;
                                        })
                                )
                        )
                        .then(CommandManager.literal("status")
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    boolean running = backupManagerSupplier.get().isBackingUp();
                                    source.sendFeedback(() -> Text.literal(
                                            running ? "§d[!][夜喵喵] §e[备份] §f正在备份中..." : "§d[!][夜喵喵] §e[备份] §f当前没有进行中的备份"
                                    ), false);
                                    return 1;
                                })
                        )
        );
    }
}
