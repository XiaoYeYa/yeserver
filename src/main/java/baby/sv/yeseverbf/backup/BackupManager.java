package baby.sv.yeseverbf.backup;

import baby.sv.yeseverbf.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("yeseverbf-backup");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final AtomicBoolean BACKING_UP = new AtomicBoolean(false);

    private final MinecraftServer server;

    public BackupManager(MinecraftServer server) {
        this.server = server;
    }

    public CompletableFuture<BackupResult> performBackup(String trigger) {
        if (!BACKING_UP.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(new BackupResult(false, "已有备份正在进行中"));
        }

        broadcast("§d[!][夜喵喵] §e[备份] §f正在保存世界...(由 " + trigger + " 触发)");

        return CompletableFuture.supplyAsync(() -> {
            try {
                saveAllWorlds();
                Thread.sleep(500);

                String timestamp = LocalDateTime.now().format(FORMATTER);
                String backupName = "backup_" + timestamp;
                Path backupDir = Path.of(ModConfig.get().backupDirectory);
                Files.createDirectories(backupDir);

                Path worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toAbsolutePath().normalize();
                Path targetDir = backupDir.resolve(backupName).toAbsolutePath();

                broadcast("§d[!][夜喵喵] §e[备份] §f正在复制存档文件...");
                long startTime = System.currentTimeMillis();

                copyDirectory(worldDir, targetDir);

                long elapsed = System.currentTimeMillis() - startTime;
                long sizeMB = getDirectorySize(targetDir) / (1024 * 1024);

                cleanOldBackups(backupDir);

                String msg = String.format("§d[!][夜喵喵] §e[备份] §a备份完成! §f%s §7(%.1fs, %dMB)", backupName, elapsed / 1000.0, sizeMB);
                broadcast(msg);
                LOGGER.info("Backup completed: {} ({}ms, {}MB)", backupName, elapsed, sizeMB);

                return new BackupResult(true, backupName);
            } catch (Exception e) {
                LOGGER.error("Backup failed", e);
                broadcast("§d[!][夜喵喵] §e[备份] §c备份失败: " + e.getMessage());
                return new BackupResult(false, e.getMessage());
            } finally {
                BACKING_UP.set(false);
            }
        });
    }

    private void saveAllWorlds() {
        server.execute(() -> {
            for (ServerWorld world : server.getWorlds()) {
                try {
                    world.savingDisabled = false;
                    world.save(null, true, false);
                } catch (Exception e) {
                    LOGGER.error("Failed to save world: {}", world.getRegistryKey().getValue(), e);
                }
            }
            server.getPlayerManager().saveAllPlayerData();
        });
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Path backupDir = Path.of(ModConfig.get().backupDirectory).toAbsolutePath().normalize();
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path dirAbsolute = dir.toAbsolutePath().normalize();
                if (dirAbsolute.startsWith(backupDir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path relative = source.relativize(dir);
                Path targetPath = target.resolve(relative);
                Files.createDirectories(targetPath);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Path targetFile = target.resolve(relative);
                if (file.toString().endsWith(".lock")) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Failed to copy file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private long getDirectorySize(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private void cleanOldBackups(Path backupDir) throws IOException {
        int max = ModConfig.get().maxBackups;
        try (Stream<Path> dirs = Files.list(backupDir)) {
            List<Path> backups = dirs
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());

            while (backups.size() > max) {
                Path oldest = backups.remove(0);
                deleteDirectory(oldest);
                LOGGER.info("Deleted old backup: {}", oldest.getFileName());
            }
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public List<BackupInfo> listBackups() {
        Path backupDir = Path.of(ModConfig.get().backupDirectory);
        if (!Files.exists(backupDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> dirs = Files.list(backupDir)) {
            return dirs
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString(), Comparator.reverseOrder()))
                    .map(p -> {
                        long size = getDirectorySize(p);
                        return new BackupInfo(p.getFileName().toString(), size);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Failed to list backups", e);
            return Collections.emptyList();
        }
    }

    public boolean deleteBackup(String name) {
        Path backupDir = Path.of(ModConfig.get().backupDirectory).resolve(name);
        if (!Files.exists(backupDir) || !name.startsWith("backup_")) {
            return false;
        }
        try {
            deleteDirectory(backupDir);
            LOGGER.info("Deleted backup: {}", name);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to delete backup: {}", name, e);
            return false;
        }
    }

    public boolean isBackingUp() {
        return BACKING_UP.get();
    }

    private void broadcast(String message) {
        server.execute(() -> {
            server.getPlayerManager().broadcast(Text.literal(message), false);
        });
    }

    public record BackupResult(boolean success, String message) {}
    public record BackupInfo(String name, long sizeBytes) {}
}
