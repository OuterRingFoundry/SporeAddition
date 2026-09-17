package dev.sporebound.mixin;
import dev.sporebound.Hivebound;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Equipable.class)
public interface HiveboundEquipMixin {
    @Inject(method="swapWithEquipmentSlot",at=@At("HEAD"),cancellable=true)
    private void sporebound$swap(Item item,Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){
        if(Hivebound.locked(player,player.getItemBySlot(((Equipable)(Object)this).getEquipmentSlot())))
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
    }
}
