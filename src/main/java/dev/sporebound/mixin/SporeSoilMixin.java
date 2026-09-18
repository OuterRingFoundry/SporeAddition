package dev.sporebound.mixin;
import dev.sporebound.SporeMyceliumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Level.class)
public abstract class SporeSoilMixin {
    @ModifyVariable(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at=@At("HEAD"),argsOnly=true)
    private BlockState sporebound$soil(BlockState state,BlockPos pos,BlockState original,int flags,int depth) {
        return SporeMyceliumBlock.infectionResult((Level)(Object)this,pos,state);
    }
}
