package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.tpa.TpaManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Intercepts the vanilla {@code /tp <player>} (teleport self to another player) so that it
 * requires the destination player's consent instead of teleporting immediately. All other
 * teleport forms (coordinates, moving other entities, etc.) keep their vanilla behavior.
 */
@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {

    @Inject(
            method = "execute(Lnet/minecraft/server/command/ServerCommandSource;Ljava/util/Collection;Lnet/minecraft/entity/Entity;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void yeseverbf$requireConsent(ServerCommandSource source,
                                                 Collection<? extends Entity> targets,
                                                 Entity destination,
                                                 CallbackInfoReturnable<Integer> cir) {
        if (!(destination instanceof ServerPlayerEntity target)) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayerEntity mover)) {
            return;
        }
        // Only intercept when the player teleports themselves to another player.
        if (targets.size() != 1 || targets.iterator().next() != mover) {
            return;
        }
        if (mover == target) {
            return;
        }

        TpaManager.getInstance().createRequest(mover, target);
        cir.setReturnValue(1);
    }
}
