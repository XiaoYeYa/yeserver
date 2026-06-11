package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.role.RoleManager;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 锁死游客权限：禁止丢弃物品、禁止移动自己背包里的物品。
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class GuestRestrictionMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void yeseverbf$blockGuestDrop(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (player == null || !RoleManager.isGuest(player)) {
            return;
        }
        PlayerActionC2SPacket.Action action = packet.getAction();
        if (action == PlayerActionC2SPacket.Action.DROP_ITEM
                || action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
            ci.cancel();
        }
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void yeseverbf$lockGuestInventory(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (player == null || !RoleManager.isGuest(player)) {
            return;
        }
        // 只锁玩家自己的背包界面；传送菜单（容器界面）由其自身处理
        if (player.currentScreenHandler instanceof PlayerScreenHandler) {
            player.playerScreenHandler.syncState();
            ci.cancel();
        }
    }
}
