package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.tpa.TpaManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.function.Predicate;

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

    /**
     * 下放 /teleport 与 /tp 的使用权限到普通玩家（原版要求权限等级 2）。
     * 由于"传送到其他玩家"已改为需对方同意，这里允许所有玩家使用该命令。
     */
    @ModifyArg(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"
            ),
            index = 0
    )
    private static Predicate<ServerCommandSource> yeseverbf$lowerTpPermission(Predicate<ServerCommandSource> original) {
        return source -> true;
    }
}
