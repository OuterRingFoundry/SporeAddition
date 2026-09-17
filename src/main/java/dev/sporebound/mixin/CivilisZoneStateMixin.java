package dev.sporebound.mixin;

import dev.sporebound.client.CivilisBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client HUD semantic state only; civilization scores and spawn rules stay server-owned. */
@Pseudo
@Mixin(targets="civil.civilization.ZoneTransitionPayload",remap=false)
public abstract class CivilisZoneStateMixin {
    @Inject(method="state",at=@At("RETURN"),cancellable=true,require=0)
    private void sporebound$caution(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(CivilisBridge.cautionState(cir.getReturnValue()));
    }
}
