package dev.sporebound.mixin;
import dev.sporebound.Hivebound;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;
@Mixin(value=Proto.class,remap=false)
public abstract class HiveboundRaidMixin {
    @ModifyVariable(method={"scanForHosts","giveMadness"},at=@At("STORE"),ordinal=0)
    private List<Entity> sporebound$hosts(List<Entity> entities){return entities.stream().filter(e->!Hivebound.member(e)).toList();}
}
