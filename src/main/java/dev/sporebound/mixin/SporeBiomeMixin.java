package dev.sporebound.mixin;
import com.Harbinger.Spore.ExtremelySusThings.BiomeModification;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BiomeModification.class,remap=false)
public abstract class SporeBiomeMixin {
    @Inject(method="modify",at=@At("HEAD"),cancellable=true)
    private void sporebound$mushrooms(Holder<Biome> biome,BiomeModifier.Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder,CallbackInfo ci) {
        if(biome.is(Biomes.MUSHROOM_FIELDS))ci.cancel();
    }
}
