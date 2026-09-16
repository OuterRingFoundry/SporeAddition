package dev.sporebound.mixin;
import dev.sporebound.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
    @Inject(method="setBlock",at=@At("HEAD"),cancellable=true)
    private void sporebound$contain(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> cir) {
        // World generation runs off-thread. Never query/load ServerLevel chunks or saved data here.
        var region=(WorldGenRegion)(Object)this;
        if(Protection.spore(state) && (!region.getLevel().dimension().equals(Sporebound.BLIGHT) || CorruptionData.peek(region.getLevel())<0))cir.setReturnValue(false);
    }
}
