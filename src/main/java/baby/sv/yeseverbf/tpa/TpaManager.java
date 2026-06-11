package baby.sv.yeseverbf.tpa;

import baby.sv.yeseverbf.util.TeleportEffects;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending teleport requests created when a player uses {@code /tp} to teleport
 * to another player. The destination player must explicitly accept before the teleport
 * happens.
 */
public final class TpaManager {
    /** How long a request stays valid before it expires. */
    public static final long TIMEOUT_MS = 60_000L;

    private static final TpaManager INSTANCE = new TpaManager();

    public static TpaManager getInstance() {
        return INSTANCE;
    }

    private TpaManager() {
    }

    /** A pending request keyed by the requester's UUID. */
    public record Request(UUID requester, String requesterName, UUID target, long deadline) {
        public boolean isExpired() {
            return System.currentTimeMillis() > deadline;
        }
    }

    private final Map<UUID, Request> pending = new ConcurrentHashMap<>();

    /**
     * Records a new request and notifies both players. The destination player receives a
     * clickable chat message to accept or deny.
     */
    public void createRequest(ServerPlayerEntity requester, ServerPlayerEntity target) {
        String requesterName = requester.getName().getString();
        pending.put(requester.getUuid(),
                new Request(requester.getUuid(), requesterName, target.getUuid(), System.currentTimeMillis() + TIMEOUT_MS));

        long seconds = TIMEOUT_MS / 1000L;

        requester.sendMessage(Text.literal("已向 ").formatted(Formatting.GRAY)
                .append(Text.literal(target.getName().getString()).formatted(Formatting.AQUA))
                .append(Text.literal(" 发送传送请求，等待对方同意…（" + seconds + "秒内有效）").formatted(Formatting.GRAY)));

        MutableText accept = Text.literal("[同意]").styled(s -> s
                .withColor(Formatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + requesterName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("点击同意 " + requesterName + " 的传送请求"))));
        MutableText deny = Text.literal("[拒绝]").styled(s -> s
                .withColor(Formatting.RED)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + requesterName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("点击拒绝 " + requesterName + " 的传送请求"))));

        target.sendMessage(Text.literal(requesterName).formatted(Formatting.AQUA)
                .append(Text.literal(" 想要传送到你这里。 ").formatted(Formatting.YELLOW))
                .append(accept)
                .append(Text.literal(" "))
                .append(deny));
    }

    /**
     * Accepts the request the given requester sent to {@code accepter} and performs the teleport.
     */
    public void accept(ServerPlayerEntity accepter, String requesterName) {
        ServerPlayerEntity requester = accepter.getServer().getPlayerManager().getPlayer(requesterName);
        if (requester == null) {
            accepter.sendMessage(Text.literal("玩家 " + requesterName + " 不在线，无法传送。").formatted(Formatting.RED));
            return;
        }

        Request request = pending.get(requester.getUuid());
        if (request == null || !request.target().equals(accepter.getUuid())) {
            accepter.sendMessage(Text.literal("没有来自 " + requesterName + " 的传送请求。").formatted(Formatting.RED));
            return;
        }

        pending.remove(requester.getUuid());

        if (request.isExpired()) {
            accepter.sendMessage(Text.literal(requesterName + " 的传送请求已过期。").formatted(Formatting.RED));
            requester.sendMessage(Text.literal("你向 " + accepter.getName().getString() + " 发送的传送请求已过期。").formatted(Formatting.RED));
            return;
        }

        ServerWorld world = (ServerWorld) accepter.getWorld();
        TeleportEffects.source((ServerWorld) requester.getWorld(), requester.getX(), requester.getY(), requester.getZ());
        requester.teleport(world, accepter.getX(), accepter.getY(), accepter.getZ(),
                EnumSet.noneOf(PositionFlag.class), accepter.getYaw(), accepter.getPitch(), true);
        TeleportEffects.arrive(requester, "传送至 " + accepter.getName().getString());

        requester.sendMessage(Text.literal(accepter.getName().getString() + " 已同意，正在传送…").formatted(Formatting.GREEN));
        accepter.sendMessage(Text.literal("已同意 " + requesterName + " 的传送请求。").formatted(Formatting.GREEN));
    }

    /**
     * Rejects the request the given requester sent to {@code denier}.
     */
    public void deny(ServerPlayerEntity denier, String requesterName) {
        Request request = findRequest(requesterName, denier.getUuid());
        if (request == null) {
            denier.sendMessage(Text.literal("没有来自 " + requesterName + " 的传送请求。").formatted(Formatting.RED));
            return;
        }

        pending.remove(request.requester());
        denier.sendMessage(Text.literal("已拒绝 " + requesterName + " 的传送请求。").formatted(Formatting.YELLOW));

        ServerPlayerEntity requester = denier.getServer().getPlayerManager().getPlayer(request.requester());
        if (requester != null) {
            requester.sendMessage(Text.literal(denier.getName().getString() + " 拒绝了你的传送请求。").formatted(Formatting.RED));
        }
    }

    private Request findRequest(String requesterName, UUID target) {
        for (Request request : pending.values()) {
            if (request.target().equals(target) && request.requesterName().equalsIgnoreCase(requesterName)) {
                return request;
            }
        }
        return null;
    }
}
