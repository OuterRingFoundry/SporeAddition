package dev.sporebound.mixin;
import dev.sporebound.HiveboundEvolution;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class HiveboundKillMixin {
    @Inject(method="awardKillScore",at=@At("HEAD"))
    private void sporebound$kill(Entity victim,int score,DamageSource source,CallbackInfo ci){HiveboundEvolution.award((ServerPlayer)(Object)this,victim);}
}
