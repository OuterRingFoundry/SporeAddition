package dev.sporebound.mixin;
import dev.sporebound.HiveClimbing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Vanilla ladder motion handles ascent, crouch holding and fall-distance reset on both sides. */
@Mixin(LivingEntity.class)
public abstract class HiveClimbingMixin {
    @Inject(method="onClimbable",at=@At("HEAD"),cancellable=true)
    private void sporebound$climb(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof Player player&&player.horizontalCollision&&HiveClimbing.eligible(player))cir.setReturnValue(true);
    }
}
