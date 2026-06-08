package baby.sv.yeseverbf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class PlayerWeatherCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("pweather")
                        .then(CommandManager.literal("clear")
                                .executes(ctx -> setWeather(ctx.getSource(), WeatherType.CLEAR, null))
                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                        .executes(ctx -> setWeather(ctx.getSource(), WeatherType.CLEAR, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                )
                        )
                        .then(CommandManager.literal("rain")
                                .executes(ctx -> setWeather(ctx.getSource(), WeatherType.RAIN, null))
                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                        .executes(ctx -> setWeather(ctx.getSource(), WeatherType.RAIN, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                )
                        )
                        .then(CommandManager.literal("thunder")
                                .executes(ctx -> setWeather(ctx.getSource(), WeatherType.THUNDER, null))
                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                        .executes(ctx -> setWeather(ctx.getSource(), WeatherType.THUNDER, IdentifierArgumentType.getIdentifier(ctx, "world")))
                                )
                        )
                        .then(CommandManager.literal("reset")
                                .executes(ctx -> resetWeather(ctx.getSource(), null))
                                .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                        .executes(ctx -> resetWeather(ctx.getSource(), IdentifierArgumentType.getIdentifier(ctx, "world")))
                                )
                        )
                        .then(CommandManager.literal("world")
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.literal("clear")
                                                .executes(ctx -> setWeather(ctx.getSource(), WeatherType.CLEAR, null))
                                                .then(CommandManager.argument("targetWorld", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setWeather(ctx.getSource(), WeatherType.CLEAR, IdentifierArgumentType.getIdentifier(ctx, "targetWorld")))
                                                )
                                        )
                                        .then(CommandManager.literal("rain")
                                                .executes(ctx -> setWeather(ctx.getSource(), WeatherType.RAIN, null))
                                                .then(CommandManager.argument("targetWorld", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setWeather(ctx.getSource(), WeatherType.RAIN, IdentifierArgumentType.getIdentifier(ctx, "targetWorld")))
                                                )
                                        )
                                        .then(CommandManager.literal("thunder")
                                                .executes(ctx -> setWeather(ctx.getSource(), WeatherType.THUNDER, null))
                                                .then(CommandManager.argument("targetWorld", IdentifierArgumentType.identifier())
                                                        .executes(ctx -> setWeather(ctx.getSource(), WeatherType.THUNDER, IdentifierArgumentType.getIdentifier(ctx, "targetWorld")))
                                                )
                                        )
                                )
                                .then(CommandManager.literal("reset")
                                        .executes(ctx -> resetWeather(ctx.getSource(), null))
                                        .then(CommandManager.argument("targetWorld", IdentifierArgumentType.identifier())
                                                .executes(ctx -> resetWeather(ctx.getSource(), IdentifierArgumentType.getIdentifier(ctx, "targetWorld")))
                                        )
                                )
                        )
        );
    }

    private enum WeatherType {
        CLEAR, RAIN, THUNDER
    }

    private static int setWeather(ServerCommandSource source, WeatherType type, Identifier worldId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }

        ServerWorld targetWorld = null;
        String worldName;

        if (worldId != null) {
            targetWorld = findWorld(source.getServer(), worldId);
            if (targetWorld == null) {
                source.sendError(Text.literal("§d[!][夜喵喵] §c找不到世界: " + worldId));
                return 0;
            }
            worldName = getWorldDisplayName(targetWorld);
        } else {
            targetWorld = player.getServerWorld();
            worldName = getWorldDisplayName(targetWorld);
        }

        switch (type) {
            case CLEAR -> {
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.RAIN_STOPPED, 0));
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 0));
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0));
                source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[个人天气] §f已将 §b" + worldName + " §f设置为 §b晴天"), false);
            }
            case RAIN -> {
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.RAIN_STARTED, 0));
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1));
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0));
                source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[个人天气] §f已将 §b" + worldName + " §f设置为 §9雨天"), false);
            }
            case THUNDER -> {
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.RAIN_STARTED, 0));
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1));
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 1));
                source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[个人天气] §f已将 §b" + worldName + " §f设置为 §5雷暴"), false);
            }
        }
        return 1;
    }

    private static int resetWeather(ServerCommandSource source, Identifier worldId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§d[!][夜喵喵] §c该命令只能由玩家执行"));
            return 0;
        }

        ServerWorld targetWorld = null;
        String worldName;

        if (worldId != null) {
            targetWorld = findWorld(source.getServer(), worldId);
            if (targetWorld == null) {
                source.sendError(Text.literal("§d[!][夜喵喵] §c找不到世界: " + worldId));
                return 0;
            }
            worldName = getWorldDisplayName(targetWorld);
        } else {
            targetWorld = player.getServerWorld();
            worldName = getWorldDisplayName(targetWorld);
        }

        boolean isRaining = targetWorld.isRaining();
        boolean isThundering = targetWorld.isThundering();

        if (isRaining) {
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.RAIN_STARTED, 0));
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1));
        } else {
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.RAIN_STOPPED, 0));
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 0));
        }
        player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, isThundering ? 1 : 0));

        source.sendFeedback(() -> Text.literal("§d[!][夜喵喵] §e[个人天气] §f已将 §b" + worldName + " §f重置为服务器天气"), false);
        return 1;
    }

    private static ServerWorld findWorld(net.minecraft.server.MinecraftServer server, Identifier id) {
        if (id.equals(World.OVERWORLD.getValue())) return server.getOverworld();
        if (id.equals(World.NETHER.getValue())) return server.getWorld(World.NETHER);
        if (id.equals(World.END.getValue())) return server.getWorld(World.END);
        return server.getWorld(RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, id));
    }

    private static String getWorldDisplayName(ServerWorld world) {
        RegistryKey<World> key = world.getRegistryKey();
        if (key == World.OVERWORLD) return "主世界";
        if (key == World.NETHER) return "下界";
        if (key == World.END) return "末地";
        return key.getValue().getPath();
    }
}