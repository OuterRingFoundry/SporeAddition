package dev.sporebound.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class HiveVisionMixin {
    @Inject(method="shouldEntityAppearGlowing",at=@At("HEAD"),cancellable=true)
    private void sporebound$vision(Entity entity,CallbackInfoReturnable<Boolean> cir){
        if(dev.sporebound.client.HiveVision.marked(entity))cir.setReturnValue(true);
    }
}
