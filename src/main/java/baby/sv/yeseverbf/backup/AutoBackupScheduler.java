package baby.sv.yeseverbf.backup;

import baby.sv.yeseverbf.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoBackupScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("yeseverbf-autobackup");
    private ScheduledExecutorService scheduler;
    private final MinecraftServer server;
    private final BackupManager backupManager;

    public AutoBackupScheduler(MinecraftServer server, BackupManager backupManager) {
        this.server = server;
        this.backupManager = backupManager;
    }

    public void start() {
        int intervalMinutes = ModConfig.get().autoBackupIntervalMinutes;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "yeseverbf-autobackup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::tick, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
        LOGGER.info("Auto backup scheduler started (interval: {} minutes)", intervalMinutes);
    }

    private void tick() {
        try {
            if (server.getCurrentPlayerCount() > 0) {
                LOGGER.info("Auto backup triggered ({} players online)", server.getCurrentPlayerCount());
                backupManager.performBackup("自动备份");
            } else {
                LOGGER.debug("Skipping auto backup: no players online");
            }
        } catch (Exception e) {
            LOGGER.error("Auto backup tick failed", e);
        }
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            LOGGER.info("Auto backup scheduler stopped");
        }
    }
}
