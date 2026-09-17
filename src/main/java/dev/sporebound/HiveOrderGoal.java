package dev.sporebound;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
/** Retains remote orders while native combat/navigation executes them. */
public final class HiveOrderGoal extends Goal {
    private final Mob mob;
    private LivingEntity target;
    public HiveOrderGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.TARGET));}
    @Override public boolean canUse(){
        if(!(mob.level() instanceof ServerLevel level)||!HiveNetwork.active(level)||Protection.sterile(level,mob.blockPosition())
            ||!mob.getPersistentData().hasUUID("sporebound:order"))return false;
        var e=level.getEntity(mob.getPersistentData().getUUID("sporebound:order"));
        if(!(e instanceof LivingEntity living)||!FungalEcology.prey(living))return false;
        target=living;return true;
    }
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public void start(){mob.setTarget(target);}
    @Override public void tick(){if(mob.getTarget()!=target)mob.setTarget(target);}
    @Override public void stop(){if(mob.getTarget()==target)mob.setTarget(null);target=null;}
}
