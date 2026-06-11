package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.role.RoleManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让游客像旁观者一样不会触发"踩踏类"机关——压力板、绊线钩等。
 * 原版压力板与绊线在统计触发实体时会排除 {@code canAvoidTraps()} 为真的实体，
 * 这里把游客玩家也归入该类。
 */
@Mixin(Entity.class)
public abstract class GuestTrapAvoidMixin {

    @Inject(method = "canAvoidTraps", at = @At("HEAD"), cancellable = true)
    private void yeseverbf$guestAvoidTraps(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && RoleManager.isGuest(player)) {
            cir.setReturnValue(true);
        }
    }
}
