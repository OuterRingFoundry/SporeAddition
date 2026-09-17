package dev.sporebound;

import com.Harbinger.Spore.core.Sblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import java.util.EnumSet;

/** Searches loaded nearby terrain only; native remains never produce a second loot drop. */
public final class BiomassScavengingGoal extends Goal {
    private final InfectedBiomass biomass;
    private BlockPos remains;
    private com.Harbinger.Spore.Sentities.Utility.CorpseEntity carcass;
    private int pursuing;
    public BiomassScavengingGoal(InfectedBiomass biomass) { this.biomass=biomass;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    public static boolean edible(BlockState state) {
        return state.is(Sblocks.REMAINS.get()) || state.is(Sblocks.WALL_REMAINS.get())
            || state.is(Sblocks.FROZEN_REMAINS.get()) || state.is(Sblocks.BIOMASS_BULB.get())
            || state.is(Sblocks.DROWNED_LUMP.get());
    }
    public static boolean consume(InfectedBiomass biomass,BlockPos pos) {
        if(!(biomass.level() instanceof ServerLevel level) || !biomass.isHungry() || biomass.busy()
                || !biomass.isAlive() || !level.hasChunkAt(pos) || !pos.closerToCenterThan(biomass.position(),2.5)
                || Protection.sterile(level,pos) || Protection.sterile(level,biomass.blockPosition())
                || !EventHooks.canEntityGrief(level,biomass) || !edible(level.getBlockState(pos)))return false;
        var hit=level.clip(new ClipContext(biomass.getEyePosition(),Vec3.atCenterOf(pos),
            ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,biomass));
        if(hit.getType()!=HitResult.Type.MISS && !hit.getBlockPos().equals(pos))return false;
        if(!level.destroyBlock(pos,false,biomass))return false;
        biomass.nourish(1);return true;
    }
    public static boolean consumeCorpse(InfectedBiomass biomass,LivingEntity corpse) {
        if(!(biomass.level() instanceof ServerLevel level) || corpse.level()!=level || !biomass.isAlive()
                || !biomass.isHungry() || biomass.busy() || corpse.isAlive() || corpse.isRemoved()
                || !Protection.spore(corpse) || corpse instanceof InfectedBiomass || biomass.distanceToSqr(corpse)>6.25
                || !biomass.hasLineOfSight(corpse) || Protection.sterile(level,corpse.blockPosition())
                || Protection.sterile(level,biomass.blockPosition()) || !EventHooks.canEntityGrief(level,biomass))return false;
        corpse.discard();biomass.nourish(1);return true;
    }
    public static boolean consumeCarcass(InfectedBiomass biomass,com.Harbinger.Spore.Sentities.Utility.CorpseEntity corpse) {
        if(!(biomass.level() instanceof ServerLevel level) || corpse.level()!=level || !biomass.isAlive()
                || !biomass.isHungry() || biomass.busy() || corpse.isRemoved() || biomass.distanceToSqr(corpse)>6.25
                || !biomass.hasLineOfSight(corpse) || Protection.sterile(level,corpse.blockPosition())
                || Protection.sterile(level,biomass.blockPosition()) || !EventHooks.canEntityGrief(level,biomass))return false;
        // Contents remain ordinary dropped items, eligible for later digestion; no duplicate inventory.
        net.minecraft.world.Containers.dropContents(level,corpse.blockPosition(),corpse.getInventory());
        corpse.getInventory().clearContent();corpse.discard();biomass.nourish(1);return true;
    }
    @Override public boolean canUse() {
        if(!biomass.isHungry() || biomass.busy() || biomass.tickCount%20!=0
                || !(biomass.level() instanceof ServerLevel level) || Protection.sterile(level,biomass.blockPosition())
                || !EventHooks.canEntityGrief(level,biomass))return false;
        for(var corpse:level.getEntitiesOfClass(LivingEntity.class,biomass.getBoundingBox().inflate(2.5)))
            if(consumeCorpse(biomass,corpse))return false;
        carcass=level.getEntitiesOfClass(com.Harbinger.Spore.Sentities.Utility.CorpseEntity.class,
            biomass.getBoundingBox().inflate(8),c -> !c.isRemoved() && biomass.hasLineOfSight(c)
                && !Protection.sterile(level,c.blockPosition())).stream()
            .sorted(java.util.Comparator.comparingDouble(biomass::distanceToSqr))
            .filter(c -> {var path=biomass.getNavigation().createPath(c,0);return path!=null&&path.canReach();})
            .findFirst().orElse(null);
        if(carcass!=null)return true;
        remains=null;
        double nearest=Double.MAX_VALUE;
        for(var pos:BlockPos.betweenClosed(biomass.blockPosition().offset(-6,-2,-6),biomass.blockPosition().offset(6,2,6))) {
            if(!level.hasChunkAt(pos) || !edible(level.getBlockState(pos)) || Protection.sterile(level,pos))continue;
            double distance=pos.distToCenterSqr(biomass.position());
            if(distance>=nearest)continue;
            var path=biomass.getNavigation().createPath(pos,1);
            if(path!=null && path.canReach()){remains=pos.immutable();nearest=distance;}
        }
        return remains!=null;
    }
    @Override public void start(){pursuing=0;}
    @Override public boolean canContinueToUse(){
        return biomass.isHungry() && !biomass.busy() && pursuing<200
            && (carcass!=null ? !carcass.isRemoved() && biomass.distanceToSqr(carcass)<144
                : remains!=null && biomass.level().hasChunkAt(remains) && edible(biomass.level().getBlockState(remains)));
    }
    @Override public void tick(){
        pursuing++;
        if(carcass!=null) {
            if(biomass.distanceToSqr(carcass)<=6.25){biomass.getNavigation().stop();consumeCarcass(biomass,carcass);}
            else if(biomass.tickCount%10==0)biomass.getNavigation().moveTo(carcass,1);
            return;
        }
        if(remains.closerToCenterThan(biomass.position(),2.5)){biomass.getNavigation().stop();consume(biomass,remains);}
        else if(biomass.tickCount%10==0)biomass.getNavigation().moveTo(remains.getX()+0.5,remains.getY(),remains.getZ()+0.5,1);
    }
    @Override public void stop(){carcass=null;remains=null;biomass.getNavigation().stop();}
}
