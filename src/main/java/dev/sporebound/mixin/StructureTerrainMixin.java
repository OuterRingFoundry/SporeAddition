package dev.sporebound.mixin;

import dev.sporebound.StructureTerrain;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Structure.class)
public abstract class StructureTerrainMixin {
    @Inject(method="generate",at=@At("RETURN"),cancellable=true)
    private void sporebound$wholeSite(RegistryAccess registries, ChunkGenerator generator,
            BiomeSource biomes, RandomState random, StructureTemplateManager templates,
            long seed, ChunkPos chunk, int references, LevelHeightAccessor height,
            Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        var id=registries.registryOrThrow(Registries.STRUCTURE).getKey((Structure)(Object)this);
        if(id==null||!id.getNamespace().equals("spore"))return;
        var start=cir.getReturnValue();
        if(start.isValid()&&!StructureTerrain.suitable(start,generator,height,random))
            cir.setReturnValue(StructureStart.INVALID_START);
    }
}
