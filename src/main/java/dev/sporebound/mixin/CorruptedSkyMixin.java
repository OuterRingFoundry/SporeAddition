package dev.sporebound.mixin;

import dev.sporebound.CorruptionMath;
import dev.sporebound.CorruptionPayload;
import dev.sporebound.Sporebound;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve day/night and weather while shifting the infected sky and clouds. */
@Mixin(ClientLevel.class)
public abstract class CorruptedSkyMixin {
    private double sporebound$amount() {
        ClientLevel level = (ClientLevel)(Object)this;
        var data = CorruptionPayload.ClientState.current;
        if (!level.dimension().equals(Sporebound.BLIGHT) || data == null
                || !data.dimension().equals(level.dimension().location()) || data.sanctuary()) return 0;
        return CorruptionMath.fogDensity(data.regionalIndex());
    }
    @Inject(method="getSkyColor",at=@At("RETURN"),cancellable=true)
    private void sporebound$sky(Vec3 pos,float partial,CallbackInfoReturnable<Vec3> ci) {
        double amount = sporebound$amount();
        if (amount <= 0) return;
        Vec3 color = ci.getReturnValue();
        double light = Math.max(0.035, Math.max(color.x, Math.max(color.y, color.z)));
        ci.setReturnValue(color.lerp(new Vec3(light * 0.70, light * 0.47, light * 0.57), amount * 0.85));
    }
    @Inject(method="getCloudColor",at=@At("RETURN"),cancellable=true)
    private void sporebound$clouds(float partial,CallbackInfoReturnable<Vec3> ci) {
        double amount = sporebound$amount();
        Vec3 color = ci.getReturnValue();
        ci.setReturnValue(color.multiply(1 - amount * 0.13, 1 - amount * 0.26, 1 - amount * 0.35));
    }
}
