package baby.sv.yeseverbf.mixin;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * Allows all players to use the vanilla {@code /teleport} and {@code /tp} commands.
 */
@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
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
