package dev.sporebound.mixin;
import dev.sporebound.Hivebound;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class HiveboundVanishingMixin {
    @Inject(method="destroyVanishingCursedItems",at=@At("HEAD"))
    private void sporebound$retainBeforeVanishing(CallbackInfo ci){Hivebound.retainOnDeath((Player)(Object)this);}
}
