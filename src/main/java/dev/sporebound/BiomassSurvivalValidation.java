package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.Sblocks;
import com.Harbinger.Spore.core.SConfig;
import com.Harbinger.Spore.core.Sentities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.function.BiConsumer;

/** Opt-in native fixtures for hunger, corpse scavenging, combat and surplus assimilation. */
public final class BiomassSurvivalValidation {
    public static void run(ServerLevel level,BiConsumer<Boolean,String> check) {
        BlockPos pos=new BlockPos(176,240,176);
        for(var p:BlockPos.betweenClosed(pos.offset(-7,0,-7),pos.offset(7,5,7)))level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        var eater=FungalContent.BIOMASS.get().create(level);eater.setNoAi(true);eater.moveTo(pos.getX(),pos.getY(),pos.getZ());level.addFreshEntity(eater);
        eater.setMass(3);eater.setHunger(SConfig.SERVER.hunger.get());
        for(int i=0;i<InfectedBiomass.SHRINK_SECONDS;i++)eater.hungerSecond();
        check.accept(eater.isAlive()&&eater.mass()==2,"starvation shrinks biomass before death");
        for(int i=0;i<InfectedBiomass.SHRINK_SECONDS;i++)eater.hungerSecond();
        check.accept(eater.isAlive()&&eater.mass()==1,"starvation keeps the smallest biomass alive for its last feeding window");
        for(int i=0;i<InfectedBiomass.SHRINK_SECONDS;i++)eater.hungerSecond();
        check.accept(eater.isRemoved()&&BlockPos.betweenClosedStream(pos.offset(-1,0,-1),pos.offset(1,1,1))
            .anyMatch(p->level.getBlockState(p).is(Sblocks.FUNGAL_STEM_SAPLING.get())),"starved smallest biomass leaves a native Spore mushroom");
        for(var p:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,1,1)))level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        eater=FungalContent.BIOMASS.get().create(level);eater.setNoAi(true);eater.moveTo(pos.getX(),pos.getY(),pos.getZ());level.addFreshEntity(eater);
        BlockPos food=pos.offset(1,0,0);
        level.setBlockAndUpdate(food,Sblocks.REMAINS.get().defaultBlockState());
        check.accept(!BiomassScavengingGoal.consume(eater,food),"satiated biomass leaves native remains intact");
        eater.setHunger(SConfig.SERVER.hunger.get());
        boolean grief=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
        check.accept(!BiomassScavengingGoal.consume(eater,food),"remains scavenging respects mobGriefing");
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
        check.accept(BiomassScavengingGoal.consume(eater,food)&&level.getBlockState(food).isAir()&&eater.hunger()==0,
            "hungry biomass eats human remains and resets starvation");
        for(var block:new net.minecraft.world.level.block.Block[]{Sblocks.WALL_REMAINS.get(),Sblocks.FROZEN_REMAINS.get()}) {
            level.setBlock(food,block.defaultBlockState(),2);eater.setHunger(SConfig.SERVER.hunger.get());
            check.accept(BiomassScavengingGoal.consume(eater,food),"biomass consumes native remains variant "+block);
        }
        var corpse=Sentities.CORPSE_PIECE.get().create(level);corpse.moveTo(pos.getX()+1,pos.getY(),pos.getZ());
        corpse.getInventory().addItem(new ItemStack(Items.BEEF,2));level.addFreshEntity(corpse);eater.setHunger(SConfig.SERVER.hunger.get());
        check.accept(BiomassScavengingGoal.consumeCarcass(eater,corpse)&&corpse.isRemoved()&&corpse.getInventory().isEmpty()
            && !BiomassScavengingGoal.consumeCarcass(eater,corpse),"native corpse is consumed once and its inventory is released once");
        eater.setHunger(100);CompoundTag save=new CompoundTag();eater.saveWithoutId(save);
        var restored=FungalContent.BIOMASS.get().create(level);restored.load(save);
        check.accept(restored.hunger()==100&&!restored.feeding(),"hunger persists and unfinished feeding resets on reload");
        var cow=EntityType.COW.create(level);cow.moveTo(pos.getX()+1,pos.getY(),pos.getZ());level.addFreshEntity(cow);
        float health=cow.getHealth();
        check.accept(eater.doHurtTarget(cow)&&cow.getHealth()<health&&health-cow.getHealth()<4,
            "biomass can attack prey with modest damage");
        check.accept(eater.getAttributeValue(Attributes.FOLLOW_RANGE)==6,"biomass has short-range awareness");cow.discard();
        eater.setMass(7);eater.setHunger(0);eater.setTarget(null);
        var donors=new ArrayList<Infected>();
        for(int i=0;i<BiomassAssimilationGoal.CROWD;i++) {
            var donor=Sentities.INF_HUMAN.get().create(level);donor.moveTo(pos.getX()+1,pos.getY(),pos.getZ());
            donor.setHunger(0);donor.getPersistentData().putInt(BiomassAssimilationGoal.IDLE,BiomassMath.IDLE_TICKS);level.addFreshEntity(donor);donors.add(donor);
        }
        var donor=donors.getFirst();
        check.accept(!eater.doHurtTarget(donor),"biomass never attacks live Spore allies");
        check.accept(eater.beginAssimilation(donor),"crowded idle basic infected can volunteer for assimilation");
        for(int i=0;i<10;i++)eater.tick();
        donor.setTarget(EntityType.COW.create(level));eater.tick();
        check.accept(!eater.feeding()&&donor.isAlive()&&eater.mass()==7,"new combat interrupts live assimilation without consuming mass");
        donor.setTarget(null);
        check.accept(eater.beginAssimilation(donor),"eligible donor can retry interrupted assimilation");
        int units=Math.min(8,BiomassMath.fromHealth(donor.getMaxHealth()));
        for(int i=0;i<InfectedBiomass.ABSORB_TICKS;i++)eater.tick();
        check.accept(donor.isRemoved()&&donor.getHealth()>0&&eater.mass()==7+units,
            "live assimilation transfers mass without death or loot");
        check.accept(!BiomassAssimilationGoal.crowded(donors.get(1)),"assimilation stops when the group is no longer overcrowded");
        var elite=Sentities.SLASHER.get().create(level);
        check.accept(!BiomassAssimilationGoal.basic(elite),"evolved Spore mobs never volunteer as surplus basic infected");
        donors.forEach(net.minecraft.world.entity.Entity::discard);eater.discard();
        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(8)))item.discard();
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,level.getServer());
    }
}
