package dev.sporebound.mixin;
import dev.sporebound.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow @Final Level level;
    @Inject(method="setBlockState",at=@At("HEAD"),cancellable=true)
    private void sporebound$contain(BlockPos pos,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> cir) {
        if(level instanceof ServerLevel server && Protection.denyBlock(server,pos,state))cir.setReturnValue(null);
    }
}
