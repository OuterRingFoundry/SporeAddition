package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.function.BiConsumer;

/** Real arrows, real directional shield damage, finite squad supplies and saved roles. */
public final class SurvivorCombatValidation {
    public static void run(ServerLevel level,BiConsumer<Boolean,String> check){
        var base=new BlockPos(256,280,256);
        for(var p:BlockPos.betweenClosed(base.offset(-5,-1,-5),base.offset(18,4,18))){
            level.getChunkAt(p);level.setBlockAndUpdate(p,p.getY()==279?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        }
        var archer=resident(level,base,true);var guard=resident(level,base.offset(3,0,0),false);
        var foe=EntityType.ZOMBIE.create(level);foe.setNoAi(true);foe.setNoGravity(true);foe.setPersistenceRequired();
        foe.moveTo(base.getX()+0.5,base.getY(),base.getZ()+12.5);level.addFreshEntity(foe);
        guard.setTarget(foe);SurvivorCombat.cooperate(guard);
        check.accept(archer.getTarget()==foe,"idle survivor answers a nearby ally's hostile target alert");
        check.accept(archer.usingBow()&&guard.getMainHandItem().is(Items.STONE_SWORD)&&guard.getOffhandItem().is(Items.SHIELD),"squad contains an archer and sword/shield defender");
        int arrows=archer.supplies().countItem(Items.ARROW);
        check.accept(SurvivorCombat.shoot(archer,foe,1)&&archer.supplies().countItem(Items.ARROW)==arrows-1
            &&archer.getMainHandItem().getDamageValue()==1,"survivor shoots a real arrow consuming one ammunition and bow durability");
        var projectiles=level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,archer.getBoundingBox().inflate(3));
        check.accept(projectiles.size()==1&&projectiles.getFirst().getOwner()==archer,"survivor arrow has the correct owner for combat credit");
        float hp=guard.getHealth();
        guard.hurt(level.damageSources().arrow(projectiles.getFirst(),archer),8);
        check.accept(guard.getHealth()==hp,"survivor arrows cannot hurt their squad mates");projectiles.forEach(Entity::discard);
        guard.moveTo(base.getX()+0.5,base.getY(),base.getZ()+5.5);
        arrows=archer.supplies().countItem(Items.ARROW);
        check.accept(!SurvivorCombat.shoot(archer,foe,1)&&archer.supplies().countItem(Items.ARROW)==arrows,"archer holds fire without spending ammunition when an ally crosses the shot");
        guard.moveTo(base.getX()+3.5,base.getY(),base.getZ()+0.5);
        archer.supplies().clearContent();archer.supplies().addItem(new ItemStack(Items.STONE_SWORD));
        SurvivorCombat.equipWeapon(archer);
        check.accept(!archer.usingBow()&&archer.getMainHandItem().is(Items.STONE_SWORD)&&archer.supplies().countItem(Items.BOW)==1,"out-of-ammunition archer keeps the bow and switches to a sword");
        guard.supplies().addItem(new ItemStack(Items.ARROW,12));guard.supplies().addItem(new ItemStack(Items.BREAD,4));archer.setHealth(12);
        SurvivorCombat.share(guard,archer);
        check.accept(archer.supplies().countItem(Items.ARROW)==8&&guard.supplies().countItem(Items.ARROW)==4,"squad resupply transfers eight existing arrows without duplication");
        check.accept(archer.supplies().countItem(Items.BREAD)==1&&guard.supplies().countItem(Items.BREAD)==3,"survivor shares real food with an injured ally");
        SurvivorCombat.equipWeapon(archer);check.accept(archer.usingBow(),"resupplied archer resumes bow combat");
        CompoundTag save=new CompoundTag();archer.saveWithoutId(save);var restored=FungalContent.SURVIVOR.get().create(level);restored.load(save);
        check.accept(restored.archer()&&restored.usingBow()&&restored.supplies().countItem(Items.ARROW)==8,"archer role, weapon and ammunition survive save/load");
        // Use a stationary front attacker to exercise vanilla shield direction and raise delay.
        guard.setNoAi(true);guard.moveTo(base.getX()+3.5,base.getY(),base.getZ()+0.5,0,0);guard.setYHeadRot(0);
        foe.moveTo(guard.getX(),guard.getY(),guard.getZ()+2);guard.setTarget(foe);guard.tickCount=0;
        SurvivorCombat.guard(guard);for(int i=0;i<6;i++)guard.tick();
        check.accept(guard.isBlocking(),"defender raises an actual shield after the vanilla wind-up");
        hp=guard.getHealth();guard.hurt(level.damageSources().mobAttack(foe),6);
        check.accept(guard.getHealth()==hp&&guard.getOffhandItem().getDamageValue()>0,"frontal melee hit is blocked and wears the real shield");
        foe.moveTo(guard.getX(),guard.getY(),guard.getZ()-2);guard.invulnerableTime=0;
        guard.hurt(level.damageSources().mobAttack(foe),4);
        check.accept(guard.getHealth()<hp,"shield does not block attacks from behind");
        foe.moveTo(guard.getX(),guard.getY(),guard.getZ()+2);foe.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
        guard.invulnerableTime=0;guard.hurt(level.damageSources().mobAttack(foe),4);
        check.accept(guard.shieldCooldown()==100&&!guard.isUsingItem(),"axe strike disables the survivor shield for five seconds");
        guard.setTarget(null);guard.stopUsingItem();guard.supplies().clearContent();guard.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        guard.supplies().addItem(new ItemStack(Items.OAK_PLANKS,6));guard.supplies().addItem(new ItemStack(Items.IRON_INGOT));
        SurvivorProgression.improve(guard);
        check.accept(guard.getOffhandItem().is(Items.SHIELD)&&guard.supplies().countItem(Items.OAK_PLANKS)==0
            &&guard.supplies().countItem(Items.IRON_INGOT)==0,"replacement shield costs exactly six planks and one iron");
        archer.supplies().clearContent();archer.supplies().addItem(new ItemStack(Items.FLINT));archer.supplies().addItem(new ItemStack(Items.FEATHER));archer.supplies().addItem(new ItemStack(Items.STICK));
        SurvivorProgression.improve(archer);
        check.accept(archer.supplies().countItem(Items.ARROW)==4&&archer.supplies().countItem(Items.FLINT)==0
            &&archer.supplies().countItem(Items.FEATHER)==0&&archer.supplies().countItem(Items.STICK)==0,"four crafted arrows consume one flint, feather and stick");
        archer.discard();guard.discard();foe.discard();
    }
    private static Survivor resident(ServerLevel level,BlockPos pos,boolean archer){
        var s=FungalContent.SURVIVOR.get().create(level);s.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);s.setNoGravity(true);
        s.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.COMMAND,null);
        if(archer)s.equipStarterCombat(true);level.addFreshEntity(s);return s;
    }
}
