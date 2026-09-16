package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.Seffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Idle hungry infected digest edible drops, and gain biomass from mature crops. */
public final class FungalForaging {
    @SubscribeEvent public void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Infected mob) || !(mob.level() instanceof ServerLevel level)
                || mob.isNoAi() || !mob.isAlive() || mob.getTarget()!=null || !mob.isStarving()
                || mob.tickCount%20!=0 || Protection.sterile(level,mob.blockPosition())) return;
        for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(5))) {
            if(!edible(item) || Protection.sterile(level,item.blockPosition()) || !mob.hasLineOfSight(item))continue;
            if(mob.distanceToSqr(item)>2.25) {mob.getNavigation().moveTo(item,1);return;}
            if(digest(mob,item))return;
        }
        if(!EventHooks.canEntityGrief(level,mob))return;
        for(BlockPos pos:BlockPos.betweenClosed(mob.blockPosition().offset(-1,0,-1),mob.blockPosition().offset(1,1,1)))
            if(consumeCrop(mob,pos))return;
        BlockPos floor=mob.blockPosition().below();
        if(level.getBlockState(floor).is(Blocks.FARMLAND) && !Protection.sterile(level,floor))
            level.setBlockAndUpdate(floor,Blocks.DIRT.defaultBlockState());
    }
    public static boolean edible(ItemEntity item) {
        return item.isAlive() && !item.hasPickUpDelay() && item.getOwner()==null
            && item.getItem().has(DataComponents.FOOD);
    }
    public static boolean digest(Infected mob,ItemEntity item) {
        if(!(mob.level() instanceof ServerLevel level) || !mob.isStarving() || !edible(item)
                || mob.distanceToSqr(item)>2.25 || !mob.hasLineOfSight(item)
                || Protection.sterile(level,item.blockPosition()) || Protection.sterile(level,mob.blockPosition()))return false;
        ItemStack bite=item.getItem().copyWithCount(1);
        ItemStack remainder=item.getItem().copy();remainder.shrink(1);
        if(remainder.isEmpty())item.discard();else item.setItem(remainder);
        nourish(mob,1);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,bite),mob.getX(),mob.getEyeY(),mob.getZ(),8,0.2,0.1,0.2,0.03);
        mob.playSound(SoundEvents.GENERIC_EAT,0.6F,0.7F);
        return true;
    }
    public static boolean consumeCrop(Infected mob,BlockPos pos) {
        if(!(mob.level() instanceof ServerLevel level) || !mob.isStarving()
                || !EventHooks.canEntityGrief(level,mob) || Protection.sterile(level,pos)
                || Protection.sterile(level,mob.blockPosition()) || !pos.closerToCenterThan(mob.position(),3))return false;
        var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state))return false;
        if(!level.destroyBlock(pos,false,mob))return false;
        if(level.getBlockState(pos.below()).is(Blocks.FARMLAND) && !Protection.sterile(level,pos.below()))
            level.setBlockAndUpdate(pos.below(),Blocks.DIRT.defaultBlockState());
        nourish(mob,1);mob.playSound(SoundEvents.GENERIC_EAT,0.6F,0.7F);return true;
    }
    private static void nourish(Infected mob,int units) {
        mob.setHunger(0);mob.removeEffect(Seffects.STARVATION);mob.setEvoPoints(mob.getEvoPoints()+units);
    }
}
