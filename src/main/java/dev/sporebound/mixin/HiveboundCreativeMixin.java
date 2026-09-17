package dev.sporebound.mixin;
import dev.sporebound.Hivebound;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HiveboundCreativeMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleSetCreativeModeSlot",at=@At("HEAD"),cancellable=true)
    private void sporebound$creative(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci){
        // The packet can arrive on the network thread; enforce on the server thread after vanilla reschedules it.
        if(!player.server.isSameThread())return;
        int slot=packet.slotNum();
        if(slot>=5&&slot<=8&&Hivebound.locked(player,player.inventoryMenu.getSlot(slot).getItem())){
            player.inventoryMenu.sendAllDataToRemote();ci.cancel();
        }
    }
}
