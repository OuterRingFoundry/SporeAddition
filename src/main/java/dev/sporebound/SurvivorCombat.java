package dev.sporebound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Small loaded-area squads, finite supplies, and normal Minecraft projectiles/blocking. */
public final class SurvivorCombat {
    private SurvivorCombat() {}
    public static boolean hostile(LivingEntity target){
        return target!=null&&target.isAlive()&&!(target instanceof Survivor)&&(Protection.spore(target)||target instanceof Enemy);
    }
    public static boolean clearShot(Survivor s,LivingEntity target){
        if(target==null||!target.isAlive()||s.isAlliedTo(target)||s.distanceToSqr(target)>24*24||!s.hasLineOfSight(target))return false;
        Vec3 from=s.getEyePosition(),to=target.getBoundingBox().getCenter();
        for(var ally:s.level().getEntitiesOfClass(Survivor.class,new AABB(from,to).inflate(0.8),a->a!=s&&a.isAlive()))
            if(ally.getBoundingBox().inflate(0.65).clip(from,to).isPresent())return false;
        return true;
    }
    public static boolean shoot(Survivor s,LivingEntity target,float power){
        if(!(s.level() instanceof ServerLevel level)||!s.usingBow()||!clearShot(s,target))return false;
        var arrow=ProjectileUtil.getMobArrow(s,new ItemStack(Items.ARROW),power,s.getMainHandItem());
        double dx=target.getX()-s.getX(),dz=target.getZ()-s.getZ();
        arrow.shoot(dx,target.getY(0.333)-arrow.getY()+Math.sqrt(dx*dx+dz*dz)*0.2,dz,1.6F,4);
        arrow.pickup=AbstractArrow.Pickup.ALLOWED;
        if(!level.addFreshEntity(arrow))return false;
        s.pay(Items.ARROW);s.getMainHandItem().hurtAndBreak(1,s,EquipmentSlot.MAINHAND);
        s.playSound(net.minecraft.sounds.SoundEvents.SKELETON_SHOOT,1,1);return true;
    }
    /** Preserve the bow in the pack while out of arrows; recover it when an ally resupplies us. */
    public static void equipWeapon(Survivor s){
        boolean bow=s.archer()&&s.supplies().countItem(Items.ARROW)>0;
        if(bow&&s.getMainHandItem().is(Items.BOW))return;
        int best=-1,rank=swordRank(s.getMainHandItem());
        for(int i=0;i<s.supplies().getContainerSize();i++){
            var stack=s.supplies().getItem(i);
            if(bow&&stack.is(Items.BOW)){best=i;break;}
            if(!bow&&swordRank(stack)>rank){best=i;rank=swordRank(stack);}
        }
        if(best<0)return;
        s.stopUsingItem();var old=s.getMainHandItem();
        s.setItemSlot(EquipmentSlot.MAINHAND,s.supplies().removeItem(best,1));s.setDropChance(EquipmentSlot.MAINHAND,1);
        var left=s.supplies().addItem(old);if(!left.isEmpty())s.spawnAtLocation(left);
    }
    private static int swordRank(ItemStack s){
        if(s.is(Items.NETHERITE_SWORD))return 5;if(s.is(Items.DIAMOND_SWORD))return 4;
        if(s.is(Items.IRON_SWORD))return 3;if(s.is(Items.STONE_SWORD))return 2;
        return s.getItem() instanceof SwordItem?1:0;
    }
    public static void cooperate(Survivor s){
        if(!(s.level() instanceof ServerLevel level))return;
        for(var ally:level.getEntitiesOfClass(Survivor.class,s.getBoundingBox().inflate(16),a->a!=s&&a.isAlive()&&!a.isNoAi())){
            // Warn idle allies without pulling anyone away from a fight already in progress.
            if(hostile(s.getTarget())&&(ally.getTarget()==null||!ally.getTarget().isAlive())
                &&ally.distanceToSqr(s.getTarget())<=32*32&&s.hasLineOfSight(ally))ally.setTarget(s.getTarget());
            if(s.tickCount%100==0&&s.distanceToSqr(ally)<=16&&s.hasLineOfSight(ally))share(s,ally);
        }
    }
    public static void share(Survivor donor,Survivor receiver){
        if(donor==receiver||donor.level()!=receiver.level()||donor.distanceToSqr(receiver)>16||!donor.hasLineOfSight(receiver))return;
        int spare=donor.supplies().countItem(Items.ARROW)-(donor.archer()?16:0);
        if(receiver.archer()&&receiver.supplies().countItem(Items.ARROW)<8&&spare>0)
            transfer(donor,receiver,Items.ARROW,Math.min(spare,8-receiver.supplies().countItem(Items.ARROW)));
        if(receiver.getHealth()<receiver.getMaxHealth()&&!hasFood(receiver)){
            for(int i=0;i<donor.supplies().getContainerSize();i++){
                var stack=donor.supplies().getItem(i);
                if(stack.get(net.minecraft.core.component.DataComponents.FOOD)!=null&&stack.getCount()>2){transfer(donor,receiver,stack.getItem(),1);break;}
            }
        }
    }
    private static boolean hasFood(Survivor s){
        for(int i=0;i<s.supplies().getContainerSize();i++)
            if(s.supplies().getItem(i).get(net.minecraft.core.component.DataComponents.FOOD)!=null)return true;
        return false;
    }
    private static void transfer(Survivor donor,Survivor receiver,Item item,int count){
        for(int i=0;i<donor.supplies().getContainerSize()&&count>0;i++){
            var stack=donor.supplies().getItem(i);if(!stack.is(item))continue;
            int n=Math.min(count,stack.getCount());var gift=stack.copyWithCount(n);
            var left=receiver.supplies().addItem(gift);int accepted=n-left.getCount();stack.shrink(accepted);count-=accepted;
        }
    }
    public static void guard(Survivor s){
        var target=s.getTarget();boolean raise=target!=null&&target.isAlive()&&!s.usingBow()&&s.shieldCooldown()==0
            &&s.getOffhandItem().is(Items.SHIELD)&&s.distanceToSqr(target)<=36&&s.hasLineOfSight(target)&&s.tickCount%40<24;
        if(raise){if(!s.isUsingItem())s.startUsingItem(InteractionHand.OFF_HAND);}
        else if(s.isUsingItem()&&s.getUsedItemHand()==InteractionHand.OFF_HAND)s.stopUsingItem();
    }
    public static final class Melee extends MeleeAttackGoal {
        private final Survivor survivor;
        public Melee(Survivor s){super(s,1.15,true);survivor=s;}
        @Override public boolean canUse(){return !survivor.usingBow()&&super.canUse();}
        @Override public boolean canContinueToUse(){return !survivor.usingBow()&&super.canContinueToUse();}
        @Override public void tick(){guard(survivor);super.tick();}
        @Override protected void checkAndPerformAttack(LivingEntity target){if(!survivor.isUsingItem())super.checkAndPerformAttack(target);}
        @Override public void stop(){super.stop();survivor.stopUsingItem();}
    }
}
