package baby.sv.yeseverbf.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class LhcWorldLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger("yeseverbf-lhc");

    public static void linkLhc(Path worldSaveDir) {
        Path lhcSource = Path.of("lhc").toAbsolutePath().normalize();
        Path dimensionTarget = worldSaveDir.resolve("dimensions").resolve("yeseverbf").resolve("lhc").toAbsolutePath().normalize();

        if (!Files.isDirectory(lhcSource)) {
            LOGGER.warn("lhc folder not found at server root: {}", lhcSource);
            return;
        }

        if (Files.isDirectory(dimensionTarget.resolve("region"))) {
            LOGGER.info("lhc dimension data already exists, skipping link");
            return;
        }

        LOGGER.info("Copying lhc world data from {} to {}", lhcSource, dimensionTarget);

        try {
            Files.createDirectories(dimensionTarget);
            String[] subDirs = {"region", "entities", "poi", "data"};
            for (String sub : subDirs) {
                Path src = lhcSource.resolve(sub);
                Path dst = dimensionTarget.resolve(sub);
                if (Files.isSymbolicLink(dst)) {
                    Files.delete(dst);
                    LOGGER.info("Removed old symlink: {}", dst);
                }
                if (Files.isDirectory(src) && !Files.isDirectory(dst)) {
                    copyDirectory(src, dst);
                    LOGGER.info("Copied directory: {} -> {}", src, dst);
                }
            }
            LOGGER.info("lhc world data copied successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to copy lhc world data", e);
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}