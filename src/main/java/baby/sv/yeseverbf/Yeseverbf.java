package baby.sv.yeseverbf;

import baby.sv.yeseverbf.api.HttpApiServer;
import baby.sv.yeseverbf.backup.AutoBackupScheduler;
import baby.sv.yeseverbf.backup.BackupManager;
import baby.sv.yeseverbf.command.BackupCommand;
import baby.sv.yeseverbf.command.FlatWorldCommand;
import baby.sv.yeseverbf.command.Forest1Command;
import baby.sv.yeseverbf.command.LhcCommand;
import baby.sv.yeseverbf.command.PlayerTimeCommand;
import baby.sv.yeseverbf.command.PlayerWeatherCommand;
import baby.sv.yeseverbf.world.Forest1WorldLinker;
import baby.sv.yeseverbf.world.LhcWorldLinker;
import baby.sv.yeseverbf.config.ModConfig;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Yeseverbf implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("yeseverbf");

    private BackupManager backupManager;
    private AutoBackupScheduler autoBackupScheduler;
    private HttpApiServer httpApiServer;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Yeseverbf loading...");
        ModConfig.load();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Forest1WorldLinker.linkForest1(server.getSavePath(net.minecraft.util.WorldSavePath.ROOT));
            LhcWorldLinker.linkLhc(server.getSavePath(net.minecraft.util.WorldSavePath.ROOT));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            backupManager = new BackupManager(server);
            autoBackupScheduler = new AutoBackupScheduler(server, backupManager);
            autoBackupScheduler.start();
            httpApiServer = new HttpApiServer(server, backupManager);
            httpApiServer.start();
            LOGGER.info("Yeseverbf started!");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (autoBackupScheduler != null) autoBackupScheduler.stop();
            if (httpApiServer != null) httpApiServer.stop();
            LOGGER.info("Yeseverbf stopped!");
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BackupCommand.register(dispatcher, () -> backupManager);
            PlayerTimeCommand.register(dispatcher);
            PlayerWeatherCommand.register(dispatcher);
            FlatWorldCommand.register(dispatcher);
            Forest1Command.register(dispatcher);
            LhcCommand.register(dispatcher);
        });
    }
}