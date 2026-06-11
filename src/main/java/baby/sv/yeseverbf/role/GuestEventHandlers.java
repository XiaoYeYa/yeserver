package baby.sv.yeseverbf.role;

import baby.sv.yeseverbf.tpwarp.TeleportMenu;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * 注册所有与游客/Staff 身份相关的服务端事件。
 */
public final class GuestEventHandlers {
    private static int tickCounter = 0;

    private GuestEventHandlers() {
    }

    public static void register() {
        // 进服时应用身份状态（默认游客）
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                RoleManager.apply(handler.player));

        // 聊天带称号
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            sender.getServer().getPlayerManager()
                    .broadcast(RoleManager.formatChat(sender, message.getContent()), false);
            return false;
        });

        // TAB 装饰 + 周期性维护（约每 2 秒）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter >= 40) {
                tickCounter = 0;
                RoleManager.tickMaintenance(server);
            }
        });

        // 游客不能破坏方块
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !(player instanceof ServerPlayerEntity sp && RoleManager.isGuest(sp)));

        // 右键方块：游客专属物品触发功能，其余交互一律禁止
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            String marker = SpecialItems.markerOf(player.getStackInHand(hand));
            if (marker != null) {
                handleSpecial(sp, marker);
                return ActionResult.SUCCESS;
            }
            return RoleManager.isGuest(sp) ? ActionResult.FAIL : ActionResult.PASS;
        });

        // 右键空气：游客专属物品触发功能
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            ItemStack stack = player.getStackInHand(hand);
            String marker = SpecialItems.markerOf(stack);
            if (marker != null) {
                handleSpecial(sp, marker);
                return ActionResult.SUCCESS;
            }
            return RoleManager.isGuest(sp) ? ActionResult.FAIL : ActionResult.PASS;
        });

        // 游客不能攻击实体
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity sp && RoleManager.isGuest(sp)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // 游客不能与实体交互
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity sp && RoleManager.isGuest(sp)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    private static void handleSpecial(ServerPlayerEntity player, String marker) {
        if (SpecialItems.TP_MENU.equals(marker)) {
            TeleportMenu.open(player);
        } else if (SpecialItems.FLY_CTRL.equals(marker)) {
            FlightController.cycle(player);
        }
    }
}
