package baby.sv.yeseverbf.role;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 飞行控制器：右键循环切换飞行速度。
 */
public final class FlightController {
    private static final float BASE = 0.05f;
    private static final float[] SPEEDS = {0.05f, 0.1f, 0.2f, 0.4f};
    private static final String[] LABELS = {"正常", "快速", "极速", "狂飙"};

    private FlightController() {
    }

    public static void cycle(ServerPlayerEntity player) {
        float current = player.getAbilities().getFlySpeed();
        int index = nearestIndex(current);
        int next = (index + 1) % SPEEDS.length;
        player.getAbilities().setFlySpeed(SPEEDS[next]);
        player.getAbilities().allowFlying = true;
        player.sendAbilitiesUpdate();
        int multiplier = Math.round(SPEEDS[next] / BASE);
        player.sendMessage(
                Text.literal("§d[!][夜喵喵] §a飞行速度: §b" + LABELS[next] + " §7(" + multiplier + "x)"),
                true);
    }

    private static int nearestIndex(float speed) {
        int best = 0;
        float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < SPEEDS.length; i++) {
            float diff = Math.abs(SPEEDS[i] - speed);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return best;
    }
}
