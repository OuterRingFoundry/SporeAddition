package dev.sporebound;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.core.Sblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.ArrayList;

/** A mature Hive Mind grows a persistent underground root network without creating extra bosses. */
public final class HiveBurrowing {
    public static final int MATURITY_TICKS = 6000, BIOMASS_THRESHOLD = 100, MAX_NODES = 256;
    public static final String DATA = "SporeboundBurrow";
    private static final Direction[] DIRECTIONS = {Direction.DOWN, Direction.DOWN, Direction.NORTH,
        Direction.SOUTH, Direction.EAST, Direction.WEST};

    @SubscribeEvent public void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Proto hive) || !(hive.level() instanceof ServerLevel level)
                || hive.isNoAi() || !hive.isAlive() || !level.dimension().equals(Sporebound.BLIGHT)
                || Protection.sterile(level, hive.blockPosition()) || hive.tickCount % 20 != 0) return;
        var data = data(hive);
        data.putInt("Age", Math.min(MATURITY_TICKS, data.getInt("Age") + 20));
        if (hive.tickCount % 200 == 0) { HiveResources.develop(hive); grow(hive); }
    }
    static CompoundTag data(Proto hive) {
        var persistent = hive.getPersistentData();
        if (!persistent.contains(DATA)) persistent.put(DATA, new CompoundTag());
        return persistent.getCompound(DATA);
    }
    public static boolean mature(Proto hive) {
        return hive.isAlive() && hive.level() instanceof ServerLevel level
            && level.dimension().equals(Sporebound.BLIGHT) && !Protection.sterile(level, hive.blockPosition())
            && RegionalCorruption.at(level, hive.blockPosition()) >= 7
            && data(hive).getInt("Age") >= MATURITY_TICKS && hive.getBiomass() >= BIOMASS_THRESHOLD;
    }
    /** At most four paid placements per ten seconds, with a fixed search budget. */
    public static int grow(Proto hive) {
        if (!mature(hive) || !(hive.level() instanceof ServerLevel level) || !EventHooks.canEntityGrief(level, hive)) return 0;
        var data = data(hive);
        BlockPos origin = hive.blockPosition().below();
        if (data.contains("Origin") && !BlockPos.of(data.getLong("Origin")).equals(origin)) {
            // Teleported hives start a new local network; old roots never become remote growth anchors.
            data.remove("Roots");
        }
        data.putLong("Origin", origin.asLong());
        var roots = new ArrayList<BlockPos>();
        for (long node : data.getLongArray("Roots")) {
            BlockPos pos = BlockPos.of(node);
            if (within(origin, pos) && roots.size() < MAX_NODES && !roots.contains(pos)) roots.add(pos);
        }
        int placed = HiveConnections.grow(hive, 4);
        if (roots.isEmpty() && level.hasChunkAt(origin) && level.getBlockState(origin).is(Sblocks.ROOTED_BIOMASS.get()))
            roots.add(origin);
        else if (placed < 4 && roots.isEmpty() && place(level, hive, origin)) { roots.add(origin); placed++; }
        for (int attempt = 0; attempt < 32 && placed < 4 && !roots.isEmpty() && roots.size() < MAX_NODES; attempt++) {
            BlockPos parent = roots.get(hive.getRandom().nextInt(roots.size()));
            if (!level.hasChunkAt(parent) || !level.getBlockState(parent).is(Sblocks.ROOTED_BIOMASS.get())) continue;
            BlockPos next = parent.relative(DIRECTIONS[hive.getRandom().nextInt(DIRECTIONS.length)]);
            if (!within(origin, next) || roots.contains(next)) continue;
            if (place(level, hive, next)) { roots.add(next); placed++; }
        }
        data.putLongArray("Roots", roots.stream().mapToLong(BlockPos::asLong).toArray());
        return placed;
    }
    private static boolean within(BlockPos origin, BlockPos pos) {
        return pos.getY() <= origin.getY() && pos.getY() >= origin.getY() - 32
            && Math.abs(pos.getX() - origin.getX()) <= 24 && Math.abs(pos.getZ() - origin.getZ()) <= 24;
    }
    static boolean naturalSubstrate(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
            || state.is(Blocks.TUFF) || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE)
            || state.is(Blocks.ANDESITE) || state.is(Sblocks.INFESTED_DIRT.get())
            || state.is(Sblocks.INFESTED_STONE.get()) || state.is(Sblocks.INFESTED_DEEPSLATE.get())
            || state.is(Sblocks.BIOMASS_BLOCK.get()) || state.is(Sblocks.ROOTED_MYCELIUM.get())
            || state.is(FungalContent.CRUST.get());
    }
    private static boolean place(ServerLevel level, Proto hive, BlockPos pos) {
        if (hive.getBiomass() < BIOMASS_THRESHOLD || !level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)
                || Protection.sterile(level, pos)) return false;
        var state = level.getBlockState(pos);
        if (state.hasBlockEntity() || !state.getFluidState().isEmpty()) return false;
        if (!naturalSubstrate(state)) return false;
        if (!level.setBlockAndUpdate(pos, Sblocks.ROOTED_BIOMASS.get().defaultBlockState())) return false;
        hive.eatBiomass(1);
        BlockPos below = pos.below();
        if (level.hasChunkAt(below) && !level.isOutsideBuildHeight(below) && !Protection.sterile(level, below)
                && level.getBlockState(below).isAir())
            level.setBlockAndUpdate(below, Sblocks.FUNGAL_ROOTS.get().defaultBlockState());
        return true;
    }
}
