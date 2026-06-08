package baby.sv.yeseverbf.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Forest1WorldLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger("yeseverbf-forest1");

    public static void linkForest1(Path worldSaveDir) {
        Path forest1Source = Path.of("forest1").toAbsolutePath().normalize();
        Path dimensionTarget = worldSaveDir.resolve("dimensions").resolve("yeseverbf").resolve("forest1").toAbsolutePath().normalize();

        if (!Files.isDirectory(forest1Source)) {
            LOGGER.warn("forest1 folder not found at server root: {}", forest1Source);
            return;
        }

        if (Files.isDirectory(dimensionTarget.resolve("region"))) {
            LOGGER.info("forest1 dimension data already exists, skipping link");
            return;
        }

        LOGGER.info("Copying forest1 world data from {} to {}", forest1Source, dimensionTarget);

        try {
            Files.createDirectories(dimensionTarget);
            String[] subDirs = {"region", "entities", "poi", "data"};
            for (String sub : subDirs) {
                Path src = forest1Source.resolve(sub);
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
            LOGGER.info("forest1 world data copied successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to copy forest1 world data", e);
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
