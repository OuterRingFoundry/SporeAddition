package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.Seffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.*;

/** Idle crowded organisms transfer into the strongest peer, using the native evolution counter. */
public final class SporeConsolidationGoal extends Goal {
    public static final String IDLE="sporebound:consolidation_idle";
    private final Infected receiver;
    private Infected donor;
    private int progress,pursuing;
    public SporeConsolidationGoal(Infected receiver){this.receiver=receiver;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public static boolean idle(Infected mob){return mob.isAlive()&&!mob.isNoAi()&&!mob.isPassenger()&&!mob.isVehicle()
        &&mob.getTarget()==null&&mob.hurtTime==0&&(mob.getLastHurtByMob()==null||mob.tickCount-mob.getLastHurtByMobTimestamp()>200)
        &&mob.level() instanceof ServerLevel l&&!Protection.sterile(l,mob.blockPosition());}
    public static void age(Infected mob){var tag=mob.getPersistentData();tag.putInt(IDLE,idle(mob)?Math.min(HiveTactics.IDLE_SECONDS,tag.getInt(IDLE)+1):0);}
    private boolean stronger(Infected a,Infected b){int x=HiveboundEvolution.power(a),y=HiveboundEvolution.power(b);return x>y||(x==y&&a.getUUID().compareTo(b.getUUID())<0);}
    private List<Infected> crowd(){return receiver.level().getEntitiesOfClass(Infected.class,receiver.getBoundingBox().inflate(12),
        mob->idle(mob)&&mob.getPersistentData().getInt(IDLE)>=HiveTactics.IDLE_SECONDS);}
    @Override public boolean canUse(){
        if(receiver.tickCount%100!=0||!idle(receiver)||receiver.getPersistentData().getInt(IDLE)<HiveTactics.IDLE_SECONDS)return false;
        var group=crowd();if(!HiveTactics.canMerge(receiver.getPersistentData().getInt(IDLE),group.size())||group.stream().anyMatch(m->m!=receiver&&stronger(m,receiver)))return false;
        donor=group.stream().filter(m->m!=receiver&&stronger(receiver,m)&&receiver.hasLineOfSight(m))
            .min(Comparator.comparingDouble(receiver::distanceToSqr)).orElse(null);return donor!=null;
    }
    @Override public void start(){progress=0;pursuing=0;}
    @Override public boolean canContinueToUse(){return donor!=null&&idle(receiver)&&idle(donor)&&stronger(receiver,donor)
        &&receiver.distanceToSqr(donor)<256&&pursuing<200&&crowd().size()>=HiveTactics.IDLE_CROWD;}
    public static boolean transfer(Infected receiver,Infected donor){
        if(receiver==donor||receiver.level()!=donor.level()||!idle(receiver)||!idle(donor)
            ||receiver.distanceToSqr(donor)>6.25||!receiver.hasLineOfSight(donor))return false;
        int gained=Math.max(1,HiveboundEvolution.power(donor)+1),kills=Math.max(0,donor.getKills());
        donor.setEvoPoints(0);donor.setKills(0);donor.discard();
        receiver.setEvoPoints(EvolutionMath.add(receiver.getEvoPoints(),gained));receiver.setKills(EvolutionMath.add(receiver.getKills(),kills));
        receiver.setHunger(0);receiver.removeEffect(Seffects.STARVATION);receiver.heal(Math.max(4,donor.getMaxHealth()/2));
        receiver.getPersistentData().putInt(IDLE,0);receiver.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT,1,0.6f);return true;
    }
    @Override public void tick(){pursuing++;receiver.getLookControl().setLookAt(donor,30,30);
        if(receiver.distanceToSqr(donor)<=6.25&&receiver.hasLineOfSight(donor)){
            receiver.getNavigation().stop();donor.getNavigation().stop();
            if(++progress>=40){transfer(receiver,donor);donor=null;}
        }else{progress=0;if(pursuing%10==0)receiver.getNavigation().moveTo(donor,1);}}
    @Override public void stop(){donor=null;progress=0;receiver.getNavigation().stop();}
}
