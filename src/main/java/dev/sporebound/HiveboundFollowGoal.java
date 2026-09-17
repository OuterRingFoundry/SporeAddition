package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.Comparator;
import java.util.EnumSet;

/** Weaker infected follow a nearby dominant symbiont; combat and feeding keep priority. */
public final class HiveboundFollowGoal extends Goal {
    private final Infected mob;
    private Player leader;
    private int nextSearch;
    public HiveboundFollowGoal(Infected mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){
        if(mob.isNoAi()||mob.getTarget()!=null||mob.tickCount<nextSearch)return false;
        nextSearch=mob.tickCount+40;
        leader=mob.level().getEntitiesOfClass(Player.class,mob.getBoundingBox().inflate(24),p->HiveboundEvolution.dominates(p,mob))
            .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        return leader!=null&&mob.distanceToSqr(leader)>9;
    }
    @Override public boolean canContinueToUse(){return leader!=null&&mob.getTarget()==null&&HiveboundEvolution.dominates(leader,mob)
        &&mob.distanceToSqr(leader)<32*32&&mob.distanceToSqr(leader)>6.25;}
    @Override public void tick(){
        mob.getLookControl().setLookAt(leader,30,30);
        if(mob.tickCount%10==0)mob.getNavigation().moveTo(leader,1.05);
    }
    @Override public void stop(){leader=null;mob.getNavigation().stop();}
}
