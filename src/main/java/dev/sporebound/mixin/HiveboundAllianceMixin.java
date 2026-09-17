package dev.sporebound.mixin;
import dev.sporebound.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class HiveboundAllianceMixin {
    @Inject(method="isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void sporebound$alliance(Entity other,CallbackInfoReturnable<Boolean> cir){
        Entity self=(Entity)(Object)this;
        if(other!=null&&((Hivebound.member(self)&&Protection.spore(other))||(Protection.spore(self)&&Hivebound.member(other))))cir.setReturnValue(true);
    }
}
