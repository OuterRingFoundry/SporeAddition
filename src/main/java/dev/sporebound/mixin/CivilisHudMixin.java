package dev.sporebound.mixin;
import dev.sporebound.client.CivilisBridge;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets="civil.civilization.ZoneTransitionHud",remap=false)
public abstract class CivilisHudMixin {
    @Inject(method="onPayload",at=@At("HEAD"),require=0)
    private static void sporebound$observe(@Coerce Object payload,CallbackInfo ci){CivilisBridge.observe(payload);}
    @Inject(method="labelForPayload",at=@At("RETURN"),cancellable=true,require=0)
    private static void sporebound$territory(CallbackInfoReturnable<Component> cir){cir.setReturnValue(CivilisBridge.decorate(cir.getReturnValue()));}
}
