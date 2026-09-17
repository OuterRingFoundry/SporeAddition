package dev.sporebound;

import com.Harbinger.Spore.core.Sblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Mature Spore stalk colonies; no vanilla mushroom blocks or copied Spore assets. */
public final class SporeColonyFeature extends Feature<NoneFeatureConfiguration> {
    public SporeColonyFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        if (!level.getLevel().dimension().equals(Sporebound.BLIGHT)) return false;
        boolean placed = false;
        for (int stem = 0; stem < 5; stem++) {
            BlockPos base = context.origin().offset(context.random().nextInt(5) - 2, 0, context.random().nextInt(5) - 2);
            var floor = level.getBlockState(base.below());
            if (!(floor.is(BlockTags.DIRT) || floor.is(Sblocks.INFESTED_DIRT.get()) || floor.is(FungalContent.CRUST.get()))) continue;
            int height = 3 + context.random().nextInt(5);
            boolean clear = true;
            for (int y = 0; y <= height; y++) if (!level.getBlockState(base.above(y)).isAir()) { clear = false; break; }
            if (!clear) continue;
            level.setBlock(base.below(), Sblocks.ROOTED_MYCELIUM.get().defaultBlockState(), 2);
            for (int y = 0; y < height; y++) level.setBlock(base.above(y), Sblocks.FUNGAL_STEM.get().defaultBlockState(), 2);
            level.setBlock(base.above(height), Sblocks.FUNGAL_STEM_TOP.get().defaultBlockState(), 2);
            placed = true;
        }
        return placed;
    }
}
