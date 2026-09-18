package dev.sporebound.mixin;
import com.Harbinger.Spore.Sevents.Infection;
import dev.sporebound.Protection;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Infection.class,remap=false)
public abstract class SporeInfectionMixin {
    @Inject(method="onEntityDeath",at=@At("HEAD"),cancellable=true)
    private static void sporebound$conversion(LivingDeathEvent event,CallbackInfo ci) {
        if(event.getEntity().level() instanceof ServerLevel level && Protection.sterile(level,event.getEntity().blockPosition())) { ci.cancel(); return; }
        dev.sporebound.FungalEcology.prepareConversion(event);
        if(dev.sporebound.SurvivorConversion.convert(event))ci.cancel();
    }
    @Inject(method="onEntityDeath",at=@At("TAIL"))
    private static void sporebound$biomass(LivingDeathEvent event,CallbackInfo ci) {
        dev.sporebound.FungalEcology.convertUnmatched(event);
    }
}
