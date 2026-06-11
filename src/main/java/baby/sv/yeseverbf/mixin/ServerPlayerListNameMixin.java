package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.role.RoleManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让 TAB 列表显示带称号的名字：[Staff]/[Guest] + 玩家名。
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerListNameMixin {

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void yeseverbf$listName(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(RoleManager.formatTabName((ServerPlayerEntity) (Object) this));
    }
}
