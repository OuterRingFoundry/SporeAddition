package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Validate an entire assembled ruin before any chunk of it can be placed. */
public final class StructureTerrain {
    private StructureTerrain() {}
    public static boolean suitable(StructureStart start, ChunkGenerator generator,
            LevelHeightAccessor height, RandomState random) {
        if(!start.isValid())return false;
        int lowest=Integer.MAX_VALUE, highest=Integer.MIN_VALUE;
        for(var piece:start.getPieces()) {
            var box=piece.getBoundingBox();
            if(box.minY()<height.getMinBuildHeight()+5||box.maxY()>=height.getMaxBuildHeight()-1)return false;
            // Include both far edges even when the dimensions are not multiples of four.
            for(int x=box.minX();;x=Math.min(x+4,box.maxX())) {
                for(int z=box.minZ();;z=Math.min(z+4,box.maxZ())) {
                    int floor=generator.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,height,random);
                    int surface=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,height,random);
                    if(surface>floor||floor<generator.getSeaLevel())return false;
                    var column=generator.getBaseColumn(x,z,height,random);
                    var ground=column.getBlock(floor-1);
                    if(!ground.isSolid()||!ground.getFluidState().isEmpty())return false;
                    lowest=Math.min(lowest,floor);highest=Math.max(highest,floor);
                    if(highest-lowest>6)return false;
                    if(z==box.maxZ())break;
                }
                if(x==box.maxX())break;
            }
        }
        return lowest!=Integer.MAX_VALUE;
    }
}
