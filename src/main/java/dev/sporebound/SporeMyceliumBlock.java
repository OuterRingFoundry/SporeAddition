package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Living Spore soil, separate from the pale remnant building material. */
public final class SporeMyceliumBlock extends Block {
    public SporeMyceliumBlock(Properties properties) { super(properties); }
    public static boolean exposed(Level level, BlockPos pos) {
        var above = level.getBlockState(pos.above());
        return above.getFluidState().isEmpty() && !above.isSolidRender(level, pos.above());
    }
    public static BlockState infectionResult(Level level, BlockPos pos, BlockState proposed) {
        if (!(level instanceof ServerLevel)) return proposed;
        if (proposed.is(com.Harbinger.Spore.core.Sblocks.INFESTED_DIRT.get()) && exposed(level,pos))
            return FungalContent.CRUST.get().defaultBlockState();
        // Only native Spore calls are redirected; player-placed vanilla turf remains vanilla.
        if (proposed.is(Blocks.MYCELIUM) && StackWalker.getInstance().walk(frames ->
                frames.anyMatch(f -> f.getClassName().startsWith("com.Harbinger.Spore."))))
            return FungalContent.CRUST.get().defaultBlockState();
        return proposed;
    }
    public static boolean spreadTo(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || Protection.sterile(level,pos) || !exposed(level,pos)) return false;
        var state=level.getBlockState(pos);
        if (!(state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.COARSE_DIRT)
                ||state.is(Blocks.PODZOL)||state.is(com.Harbinger.Spore.core.Sblocks.INFESTED_DIRT.get()))) return false;
        return level.setBlockAndUpdate(pos,FungalContent.CRUST.get().defaultBlockState());
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Protection.sterile(level,pos) || !exposed(level,pos)) return;
        for(int i=0;i<4;i++) spreadTo(level,pos.offset(random.nextInt(3)-1,random.nextInt(3)-1,random.nextInt(3)-1));
    }
}
