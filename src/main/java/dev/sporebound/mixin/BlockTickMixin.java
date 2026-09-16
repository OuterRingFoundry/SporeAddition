package dev.sporebound.mixin;
import dev.sporebound.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockTickMixin {
    @Inject(method={"tick","randomTick"},at=@At("HEAD"),cancellable=true)
    private void sporebound$freeze(ServerLevel level,BlockPos pos,RandomSource random,CallbackInfo ci) {
        if(Protection.denyBlock(level,pos,(BlockState)(Object)this))ci.cancel();
    }
}
