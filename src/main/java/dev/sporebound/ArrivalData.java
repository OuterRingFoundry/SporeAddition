package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.Heightmap;

/** One shared arrival cairn per world, fixed after first construction. */
public final class ArrivalData extends SavedData {
    private BlockPos center;
    public static ArrivalData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ArrivalData::new,(tag,registries)->{
            var data=new ArrivalData();if(tag.contains("center"))data.center=BlockPos.of(tag.getLong("center"));return data;
        }),"sporebound_arrival");
    }
    public BlockPos center(ServerLevel level) {
        if(center==null) {
            int y=level.getMinBuildHeight()+1;
            // Build above the whole footprint, without cutting through terrain or player blocks.
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++) {
                level.getChunk(x>>4,z>>4);
                y=Math.max(y,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z));
            }
            if(y>level.getMaxBuildHeight()-5)throw new IllegalStateException("No space for the arrival cairn");
            center=new BlockPos(0,y,0);RiftCairn.build(level,center);setDirty();
        }
        level.getChunkAt(center);return center;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        if(center!=null)tag.putLong("center",center.asLong());return tag;
    }
}
