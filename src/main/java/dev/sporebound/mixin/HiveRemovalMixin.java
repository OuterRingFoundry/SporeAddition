package dev.sporebound.mixin;
import dev.sporebound.HivePopulation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class HiveRemovalMixin {
    @Inject(method="setRemoved",at=@At("TAIL"))
    private void sporebound$release(Entity.RemovalReason reason,CallbackInfo ci) {
        HivePopulation.removed((Entity)(Object)this,reason);
    }
}
