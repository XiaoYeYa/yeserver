package baby.sv.yeseverbf.mixin;

import baby.sv.yeseverbf.command.PlayerTimeCommand;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class WorldTimeUpdateMixin {

    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
    private Packet<?> yeseverbf$modifyTimePacket(Packet<?> packet) {
        //noinspection ConstantValue
        if ((Object) this instanceof ServerPlayNetworkHandler handler) {
            if (packet instanceof WorldTimeUpdateS2CPacket timePacket) {
                if (PlayerTimeCommand.hasTimeOverride(handler.player.getUuid())) {
                    long overrideTime = PlayerTimeCommand.getTimeOverride(handler.player.getUuid());
                    return new WorldTimeUpdateS2CPacket(timePacket.time(), -overrideTime, false);
                }
            }
        }
        return packet;
    }
}
