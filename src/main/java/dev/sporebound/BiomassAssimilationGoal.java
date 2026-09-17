package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.BaseEntities.EvolvedInfected;
import com.Harbinger.Spore.Sentities.BaseEntities.Hyper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.Comparator;
import java.util.EnumSet;

/** Surplus idle basic infected occasionally volunteer for a stronger combined organism. */
public final class BiomassAssimilationGoal extends Goal {
    public static final String IDLE = "SporeboundIdleTicks";
    public static final int CROWD = 6;
    private final Infected donor;
    private InfectedBiomass receiver;
    private int nextAttempt;
    public BiomassAssimilationGoal(Infected donor) { this.donor=donor; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    public static boolean basic(Infected infected) {
        return !(infected instanceof EvolvedInfected) && !(infected instanceof Hyper);
    }
    public static boolean eligible(Infected infected) {
        return basic(infected) && infected.isAlive() && !infected.isNoAi() && !infected.isPassenger()
            && !infected.isVehicle() && infected.getTarget()==null && !infected.isStarving()
            && infected.hurtTime==0 && (infected.getLastHurtByMob()==null
                || infected.tickCount-infected.getLastHurtByMobTimestamp()>=200)
            && infected.getPersistentData().getInt(IDLE)>=BiomassMath.IDLE_TICKS
            && infected.level() instanceof ServerLevel level && !Protection.sterile(level,infected.blockPosition());
    }
    public static boolean crowded(Infected infected) {
        return infected.level().getEntitiesOfClass(Infected.class,infected.getBoundingBox().inflate(12),
            BiomassAssimilationGoal::eligible).size()>=CROWD;
    }
    public static void age(Infected infected) {
        if (!basic(infected)) return;
        boolean idle=infected.getTarget()==null && infected.hurtTime==0;
        var data=infected.getPersistentData();
        data.putInt(IDLE,idle?Math.min(BiomassMath.IDLE_TICKS,data.getInt(IDLE)+20):0);
    }
    @Override public boolean canUse() {
        if(donor.tickCount<nextAttempt)return false;
        nextAttempt=donor.tickCount+200;
        if(!eligible(donor) || donor.getRandom().nextInt(8)!=0 || !crowded(donor))return false;
        receiver=donor.level().getEntitiesOfClass(InfectedBiomass.class,donor.getBoundingBox().inflate(12),
            b->b.canAssimilate(donor)).stream().min(Comparator.comparingDouble(donor::distanceToSqr)).orElse(null);
        return receiver!=null;
    }
    @Override public boolean canContinueToUse() {
        return receiver!=null && receiver.canAssimilate(donor) && eligible(donor)
            && donor.distanceToSqr(receiver)<256 && crowded(donor);
    }
    @Override public void tick() {
        donor.getLookControl().setLookAt(receiver,30,30);
        if(donor.distanceToSqr(receiver)<=2.25) {
            donor.getNavigation().stop();receiver.beginAssimilation(donor);
        } else donor.getNavigation().moveTo(receiver,1);
    }
    @Override public void stop() { receiver=null;donor.getNavigation().stop(); }
}
