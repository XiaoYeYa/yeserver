package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.role.RoleManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 游客免摔落伤害。
 */
@Mixin(LivingEntity.class)
public abstract class GuestFallDamageMixin {

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void yeseverbf$guestNoFallDamage(float fallDistance, float damageMultiplier,
                                             DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && RoleManager.isGuest(player)) {
            cir.setReturnValue(false);
        }
    }
}
