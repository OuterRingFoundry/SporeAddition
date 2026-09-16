package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/** A deterministic local pressure modifier over the independently saved dimension index. */
public final class RegionalCorruption {
    private RegionalCorruption() {}
    public static double offset(Holder<Biome> biome) {
        if(biome.is(Sporebound.id("remnant_grove")))return -4;
        if(biome.is(Sporebound.id("drowned_hollows")))return 1;
        if(biome.is(Sporebound.id("ribbed_highlands")))return 2;
        return 0;
    }
    public static double at(ServerLevel level,BlockPos pos) {
        double index=CorruptionData.get(level).index();
        return level.dimension().equals(Sporebound.BLIGHT)?CorruptionMath.regional(index,offset(level.getBiome(pos))):index;
    }
    public static String name(ServerLevel level,BlockPos pos) {
        if(!level.dimension().equals(Sporebound.BLIGHT))return "Dimension-wide";
        var biome=level.getBiome(pos);
        if(biome.is(Sporebound.id("remnant_grove")))return "Remnant Grove";
        if(biome.is(Sporebound.id("drowned_hollows")))return "Drowned Hollows";
        if(biome.is(Sporebound.id("ribbed_highlands")))return "Ribbed Highlands";
        return "Blighted Wilds";
    }
}
