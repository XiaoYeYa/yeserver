package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.command.FlatWorldCommand;
import baby.sv.yeseverbf.command.Forest1Command;
import baby.sv.yeseverbf.command.LhcCommand;
import baby.sv.yeseverbf.command.PlayerTimeCommand;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class PlayerDisconnectMixin {

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void yeseverbf$onDisconnect(DisconnectionInfo info, CallbackInfo ci) {
        //noinspection ConstantValue
        if ((Object) this instanceof ServerPlayNetworkHandler handler) {
            PlayerTimeCommand.removePlayer(handler.player.getUuid());
            FlatWorldCommand.onPlayerDisconnect(handler.player.getUuid());
            Forest1Command.onPlayerDisconnect(handler.player.getUuid());
            LhcCommand.onPlayerDisconnect(handler.player.getUuid());
        }
    }
}