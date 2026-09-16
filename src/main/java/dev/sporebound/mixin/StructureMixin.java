package dev.sporebound.mixin;
import dev.sporebound.Sporebound;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChunkGenerator.class)
public abstract class StructureMixin {
    @Inject(method="tryGenerateStructure",at=@At("HEAD"),cancellable=true)
    private void sporebound$dimension(StructureSet.StructureSelectionEntry entry,StructureManager manager,
        RegistryAccess registries,RandomState random,StructureTemplateManager templates,long seed,
        ChunkAccess chunk,ChunkPos pos,SectionPos section,CallbackInfoReturnable<Boolean> cir) {
        var id=registries.registryOrThrow(Registries.STRUCTURE).getKey(entry.structure().value());
        if(id==null||!id.getNamespace().equals("spore"))return;
        var access=((StructureManagerAccessor)manager).sporebound$level();
        ServerLevel level=access instanceof ServerLevel s?s:access instanceof WorldGenRegion r?r.getLevel():null;
        if(level==null||!level.dimension().equals(Sporebound.BLIGHT))cir.setReturnValue(false);
    }
}
