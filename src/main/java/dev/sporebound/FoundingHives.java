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
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse("spore:proto"));
            if (!(type.create(level) instanceof Mob hive)) throw new IllegalStateException("Spore Hive Mind spore:proto is unavailable");
            // Small founding island also supports a hive when the seeded site falls in water.
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++)
                level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.MYCELIUM.defaultBlockState(), 3);
            hive.setUUID(id); hive.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            hive.finalizeSpawn(level, level.getCurrentDifficultyAt(hive.blockPosition()), MobSpawnType.STRUCTURE, null);
            hive.setPersistenceRequired();
            if (level.addFreshEntity(hive)) data.markSeeded(slot);
        }
    }
}
