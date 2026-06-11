package baby.sv.yeseverbf.util;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 玩家传送时的视听效果：起点 / 终点的原版音效 + 粒子，外加目标玩家的大标题提示。
 */
public final class TeleportEffects {

    private TeleportEffects() {
    }

    /** 在传送起点播放消失粒子与音效（应在玩家移动前调用）。 */
    public static void source(ServerWorld world, double x, double y, double z) {
        world.spawnParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 50, 0.4, 0.8, 0.4, 0.2);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /** 在传送终点播放出现粒子与音效，并给玩家显示大标题。 */
    public static void arrive(ServerPlayerEntity player, String title) {
        ServerWorld world = player.getServerWorld();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 50, 0.4, 0.8, 0.4, 0.2);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);

        player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 35, 10));
        player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.literal("✦ 已传送 ✦").formatted(Formatting.AQUA, Formatting.BOLD)));
        if (title != null && !title.isEmpty()) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal(title).formatted(Formatting.GRAY)));
        }
    }
}
