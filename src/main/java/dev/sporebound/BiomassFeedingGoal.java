package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.Comparator;
import java.util.EnumSet;

/** Hunger triggers cooperative assimilation; combat never targets the biomass. */
public final class BiomassFeedingGoal extends Goal {
    private final Infected infected;
    private InfectedBiomass food;
    private int cooldown;
    public BiomassFeedingGoal(Infected infected) { this.infected = infected; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() {
        if (!infected.isStarving() || infected.getTarget() != null || --cooldown > 0) return false;
        cooldown = 20;
        food = infected.level().getEntitiesOfClass(InfectedBiomass.class, infected.getBoundingBox().inflate(12),
            b -> b.isAlive() && !b.absorbing()).stream().min(Comparator.comparingDouble(infected::distanceToSqr)).orElse(null);
        return food != null;
    }
    @Override public boolean canContinueToUse() {
        return food != null && food.isAlive() && infected.isStarving() && infected.getTarget() == null
            && infected.distanceToSqr(food) < 256;
    }
    @Override public void tick() {
        infected.getLookControl().setLookAt(food, 30, 30);
        if (infected.distanceToSqr(food) <= 2.25) { infected.getNavigation().stop(); food.beginAbsorption(infected); }
        else infected.getNavigation().moveTo(food, 1);
    }
    @Override public void stop() { food = null; infected.getNavigation().stop(); }
}
