package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.TrueCalamity;
import com.Harbinger.Spore.Sentities.BaseEntities.Hyper;
import com.Harbinger.Spore.Sentities.BaseEntities.EvolvedInfected;

public final class Protection {
    private Protection() {}
    public static boolean spore(Entity entity) {
        return entity instanceof InfectedBiomass || BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("spore");
    }
    public static boolean spore(BlockState state) {
        return state.is(FungalContent.CRUST.get()) || BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("spore");
    }
    public static boolean boss(Entity entity) { return entity instanceof Proto || entity instanceof Calamity || entity instanceof TrueCalamity; }
    public static boolean mushroom(Level level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        // Protect the entire mushroom-island column, including its caves and buildings.
        return level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS)
            || level.getBiome(new BlockPos(pos.getX(), 64, pos.getZ())).is(Biomes.MUSHROOM_FIELDS);
    }
    public static boolean sterile(ServerLevel level, BlockPos pos) {
        return CorruptionData.get(level).index() < 0 || mushroom(level, pos);
    }
    public static boolean denyBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (spore(state)) return sterile(level, pos);
        // A Spore actor can also place vanilla blocks or air (terrain destruction).
        // Inspect provenance only in protected mushroom columns, not ordinary terrain.
        return mushroom(level, pos) && StackWalker.getInstance().walk(frames ->
            frames.anyMatch(frame -> frame.getClassName().startsWith("com.Harbinger.Spore.")));
    }
    public static boolean denyEntity(ServerLevel level, Entity entity, boolean joining) {
        if (!spore(entity)) return false;
        double index = CorruptionData.get(level).index();
        if (mushroom(level, entity.blockPosition()) || index == -2) return true;
        if (entity instanceof Proto && HivePopulation.get(level).retired(entity.getUUID()))return true;
        if (boss(entity) && !CorruptionMath.allowsBoss(index)) return true;
        return joining && index < 0;
    }
    public static double weight(Entity entity) {
        if (!(entity instanceof Mob) || !spore(entity)) return 0;
        if (boss(entity)) return 20;
        if (entity instanceof Hyper) return 8;
        if (entity instanceof EvolvedInfected) return 4;
        return 1;
    }
}
