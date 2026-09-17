package dev.sporebound.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/**
 * During shutdown, vanilla supplies an unlimited unload budget. A completed save future
 * can reschedule itself while generation still holds the chunk, starving the server task
 * queue that releases that reference. Yield between batches so stopServer can drain tasks.
 * Every unload/save stays queued; ordinary running-world chunk scheduling is unchanged.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkUnloadBudgetMixin {
    @Shadow @Final private ServerLevel level;
    @ModifyVariable(method="processUnloads",at=@At("HEAD"),argsOnly=true)
    private BooleanSupplier sporebound$shutdownBudget(BooleanSupplier original){
        if(level.getServer().isRunning())return original;
        int[] checks={0};
        return ()->++checks[0]<=512&&original.getAsBoolean();
    }
}
