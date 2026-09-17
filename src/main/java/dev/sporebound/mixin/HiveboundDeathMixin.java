package dev.sporebound.mixin;
import dev.sporebound.Hivebound;
import net.minecraft.world.entity.player.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Inventory.class)
public abstract class HiveboundDeathMixin {
    @Shadow @Final public Player player;
    @Inject(method="dropAll",at=@At("HEAD"))
    private void sporebound$retain(CallbackInfo ci){Hivebound.retainOnDeath(player);}
}
