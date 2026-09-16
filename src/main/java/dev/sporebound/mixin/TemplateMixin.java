package dev.sporebound.mixin;
import dev.sporebound.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(StructureTemplate.class)
public abstract class TemplateMixin implements TemplateIdentity {
    @Unique private boolean sporebound$spore;
    public void sporebound$markSpore(boolean value) { sporebound$spore=value; }
    @Inject(method="placeInWorld",at=@At("HEAD"),cancellable=true)
    private void sporebound$contain(ServerLevelAccessor access,BlockPos pos,BlockPos origin,StructurePlaceSettings settings,RandomSource random,int flags,CallbackInfoReturnable<Boolean> cir) {
        if(sporebound$spore && (!access.getLevel().dimension().equals(Sporebound.BLIGHT) || CorruptionData.peek(access.getLevel())<0))cir.setReturnValue(false);
    }
}
