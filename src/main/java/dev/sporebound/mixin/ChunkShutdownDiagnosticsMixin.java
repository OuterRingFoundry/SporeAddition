package dev.sporebound.mixin;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Read-only diagnostics for the explicitly enabled disposable client fixture. */
@Mixin(ServerChunkCache.class)
public abstract class ChunkShutdownDiagnosticsMixin {
    @Shadow @Final private ServerLevel level;
    @Unique private int sporebound$shutdownRequests;
    @Inject(method="getChunk",at=@At("HEAD"))
    private void sporebound$traceShutdown(int x,int z,ChunkStatus status,boolean create,CallbackInfoReturnable<ChunkAccess> cir) {
        if(Boolean.getBoolean("sporebound.clientValidation") && create && level.getServer().getTickCount()>0
                && !level.getServer().isRunning() && sporebound$shutdownRequests++<32) {
            new Exception("SPOREBOUND SHUTDOWN CHUNK REQUEST "+level.dimension().location()+" "+x+","+z+" "+status).printStackTrace(System.out);
        }
    }
}
