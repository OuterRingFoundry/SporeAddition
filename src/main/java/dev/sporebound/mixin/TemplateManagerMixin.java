package dev.sporebound.mixin;
import java.util.Optional;
import dev.sporebound.TemplateIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(StructureTemplateManager.class)
public abstract class TemplateManagerMixin {
    @Inject(method="get",at=@At("RETURN"))
    private void sporebound$identify(ResourceLocation id,CallbackInfoReturnable<Optional<StructureTemplate>> cir) {
        cir.getReturnValue().ifPresent(template -> ((TemplateIdentity)template).sporebound$markSpore(id.getNamespace().equals("spore")));
    }
}
