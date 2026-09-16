package dev.sporebound.mixin;
import dev.sporebound.Protection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BlockEntityTickMixin {
    @Shadow @Final private BlockEntity blockEntity;
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void sporebound$quarantine(CallbackInfo ci) {
        if(blockEntity.getLevel() instanceof ServerLevel level && Protection.spore(blockEntity.getBlockState())
            && Protection.sterile(level,blockEntity.getBlockPos()))ci.cancel();
    }
}
