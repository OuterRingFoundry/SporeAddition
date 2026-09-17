package dev.sporebound.mixin;
import dev.sporebound.Hivebound;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ArmorSlot.class)
public abstract class HiveboundSlotMixin {
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void sporebound$binding(Player player,CallbackInfoReturnable<Boolean> cir){
        var stack=((Slot)(Object)this).getItem();
        if(stack.getItem() instanceof dev.sporebound.HiveboundArmor)cir.setReturnValue(!Hivebound.locked(player,stack));
    }
}
