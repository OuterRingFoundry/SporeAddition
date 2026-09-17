package dev.sporebound;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.Harbinger.Spore.core.Sentities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class FoundingHives {
    public static final int[][] SITES = {{512, 0}};
    private FoundingHives() {}
    public static void seed(ServerLevel level) {
        if (level == null || !level.dimension().equals(Sporebound.BLIGHT) || level.getDifficulty() == Difficulty.PEACEFUL) return;
        var data = CorruptionData.get(level);
        if (!CorruptionMath.allowsBoss(data.index())) return;
        for (int slot = 0; slot < SITES.length; slot++) {
            if (data.seeded(slot)) continue;
            int x = SITES[slot][0], z = SITES[slot][1];
            level.getChunk(x >> 4, z >> 4);
            UUID id = UUID.nameUUIDFromBytes(("sporebound:founder:" + level.getSeed() + ":" + slot).getBytes(StandardCharsets.UTF_8));
            if (level.getEntity(id) != null) { data.markSeeded(slot); continue; }
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse("spore:proto"));
            if (!(type.create(level) instanceof Mob hive)) throw new IllegalStateException("Spore Hive Mind spore:proto is unavailable");
            boolean found=false;
            // Search natural corrupted terrain; never create a platform or overwrite the landscape.
            for(int radius=0;radius<=256&&!found;radius+=16)for(int dx=-radius;dx<=radius&&!found;dx+=16)
                for(int dz=-radius;dz<=radius&&!found;dz+=16){
                    if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                    int px=x+dx,pz=z+dz;level.getChunk(px>>4,pz>>4);
                    int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,px,pz);
                    var pos=new BlockPos(px,y,pz);
                    if(RegionalCorruption.at(level,pos)<5||RegionalCorruption.biomeAt(level,pos).is(Sporebound.id("remnant_grove"))
                        ||!level.getFluidState(pos.below()).isEmpty()||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))continue;
                    hive.moveTo(px+0.5,y,pz+0.5,0,0);
                    if(level.noCollision(hive)&&!level.containsAnyLiquid(hive.getBoundingBox()))found=true;
                }
            if(!found){hive.discard();continue;}
            hive.setUUID(id);
            hive.finalizeSpawn(level, level.getCurrentDifficultyAt(hive.blockPosition()), MobSpawnType.STRUCTURE, null);
            hive.setPersistenceRequired();
            if (level.addFreshEntity(hive)) data.markSeeded(slot);
        }
    }
}
