package dev.sporebound.mixin;
import dev.sporebound.HivePopulation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PersistentEntitySectionManager.class)
public abstract class HiveAdmissionMixin {
    @Inject(method="addEntityWithoutEvent",at=@At("HEAD"),cancellable=true)
    private void sporebound$limit(EntityAccess access,boolean disk,CallbackInfoReturnable<Boolean> cir) {
        if(access instanceof Entity entity&&!HivePopulation.admit(entity))cir.setReturnValue(false);
    }
    @Inject(method="addEntityWithoutEvent",at=@At("RETURN"))
    private void sporebound$count(EntityAccess access,boolean disk,CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()&&access instanceof Entity entity)HivePopulation.added(entity);
    }
}
