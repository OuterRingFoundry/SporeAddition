package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.SConfig;
import com.Harbinger.Spore.core.Seffects;
import com.Harbinger.Spore.core.Sentities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import java.util.function.BiConsumer;

/** Runs only inside the explicitly enabled disposable server acceptance fixture. */
public final class FungalValidation {
    public static void run(ServerLevel level, BiConsumer<Boolean,String> check) {
        BlockPos pos = new BlockPos(176, 240, 176); level.getChunkAt(pos);
        for (int x=-8;x<=8;x++) for (int z=-8;z<=8;z++) level.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        var cow = EntityType.COW.create(level); cow.moveTo(pos.getX()+3,pos.getY(),pos.getZ()); level.addFreshEntity(cow);
        var fish = EntityType.SALMON.create(level); fish.moveTo(pos.getX()+4,pos.getY(),pos.getZ());
        var bat = EntityType.BAT.create(level); bat.moveTo(pos.getX()+5,pos.getY(),pos.getZ());
        check.accept(FungalEcology.prey(cow) && FungalEcology.prey(fish) && FungalEcology.prey(bat), "livestock, fish and flying creatures are prey");
        Infected attacker = Sentities.INF_HUMAN.get().create(level);
        attacker.moveTo(pos.getX(),pos.getY(),pos.getZ()); level.addFreshEntity(attacker);
        check.accept(!FungalEcology.prey(attacker),"Spore faction is excluded from prey");
        for(int i=0;i<150 && attacker.getTarget()!=cow;i++)attacker.targetSelector.tick();
        check.accept(attacker.getTarget()==cow,"Spore target AI acquires a non-humanoid cow");
        attacker.setTarget(null); attacker.setNoAi(true);
        var biomass = create(level,pos.offset(1,0,0));
        check.accept(!FungalEcology.prey(biomass),"biomass is an ally, never combat prey");
        attacker.setTarget(biomass);
        check.accept(attacker.getTarget()==null,"combat target event rejects biomass");
        attacker.setHunger(0); attacker.removeEffect(Seffects.STARVATION);
        check.accept(!biomass.beginAbsorption(attacker),"satiated Spore mobs cannot consume biomass");
        attacker.setHunger(SConfig.SERVER.hunger.get());
        int evolution=attacker.getEvoPoints();
        check.accept(biomass.beginAbsorption(attacker),"hungry Spore mob begins cooperative assimilation");
        for(int i=0;i<InfectedBiomass.ABSORB_TICKS-1;i++)biomass.tick();
        check.accept(!biomass.isRemoved() && biomass.absorptionTicks()==39,"assimilation has a synchronized two-second animation");
        biomass.tick();
        check.accept(biomass.isRemoved() && biomass.getHealth()>0 && attacker.getHunger()==0 && attacker.getEvoPoints()==evolution+1,
            "assimilation transfers hunger and evolution without killing the donor");
        attacker.setHunger(SConfig.SERVER.hunger.get());
        var food=new net.minecraft.world.entity.item.ItemEntity(level,attacker.getX(),attacker.getY(),attacker.getZ(),
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF,3));
        food.setPickUpDelay(40);level.addFreshEntity(food);
        check.accept(!FungalForaging.digest(attacker,food),"pickup delay prevents premature food digestion");
        food.setNoPickUpDelay();
        check.accept(FungalForaging.digest(attacker,food) && food.getItem().getCount()==2,"digestion consumes exactly one edible item");
        check.accept(!FungalForaging.digest(attacker,food),"satiated infected do not waste loot");food.discard();
        attacker.setHunger(SConfig.SERVER.hunger.get());
        var valuable=new net.minecraft.world.entity.item.ItemEntity(level,attacker.getX(),attacker.getY(),attacker.getZ(),
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        check.accept(!FungalForaging.digest(attacker,valuable),"non-food loot cannot be digested");
        BlockPos cropPos=pos.offset(0,0,1);
        level.setBlockAndUpdate(cropPos.below(),Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(cropPos,Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7));
        boolean grief=level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
        check.accept(!FungalForaging.consumeCrop(attacker,cropPos),"mobGriefing=false protects crops");
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
        check.accept(FungalForaging.consumeCrop(attacker,cropPos) && level.getBlockState(cropPos).isAir()
            && level.getBlockState(cropPos.below()).is(Blocks.DIRT),"mature crop feeds infection and ruins farmland");
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(grief,level.getServer());
        attacker.discard(); cow.discard();
        var receiver=create(level,pos); receiver.setMass(3);
        var donor=create(level,pos.offset(1,0,0)); donor.setMass(2);
        check.accept(donor.beginAbsorption(receiver),"biomass starts merging");
        for(int i=0;i<InfectedBiomass.ABSORB_TICKS;i++)donor.tick();
        check.accept(donor.isRemoved() && receiver.mass()==5,"merging conserves biomass mass");
        receiver.setOrigin("minecraft:salmon");
        CompoundTag tag=new CompoundTag();receiver.saveWithoutId(tag);
        var restored=FungalContent.BIOMASS.get().create(level);restored.load(tag);
        check.accept(restored.mass()==5 && restored.origin().equals("minecraft:salmon") && !restored.absorbing(),"mass and origin survive entity save/reload");
        var interrupted=create(level,pos.offset(1,0,0));
        check.accept(interrupted.beginAbsorption(receiver),"second merge begins");
        receiver.moveTo(pos.getX()+10,pos.getY(),pos.getZ());interrupted.tick();
        check.accept(!interrupted.absorbing() && interrupted.mass()==1 && receiver.mass()==5,"out-of-range merge cancels without mass loss");
        receiver.moveTo(pos.getX(),pos.getY(),pos.getZ());receiver.setMass(8);
        check.accept(receiver.evolve(level) && receiver.isRemoved(),"eight biomass units evolve into a powerful Spore mob");
        interrupted.discard();
        var fox=EntityType.FOX.create(level);fox.moveTo(pos.getX()+5,pos.getY(),pos.getZ());level.addFreshEntity(fox);
        fox.addEffect(new MobEffectInstance(Seffects.MYCELIUM,200));
        check.accept(!FungalEcology.hasConversion(fox),"fixture fox lacks a native fungal counterpart");
        var death=new LivingDeathEvent(fox,fox.damageSources().generic());
        check.accept(FungalEcology.convertUnmatched(death),"unmatched infected creature becomes biomass");
        check.accept(!FungalEcology.convertUnmatched(death),"conversion is idempotent for the same corpse");
        var zombie=EntityType.ZOMBIE.create(level);
        check.accept(FungalEcology.hasConversion(zombie),"native zombie fungal conversion is retained");
        var canceled=EntityType.FOX.create(level);canceled.moveTo(pos.getX(),pos.getY(),pos.getZ());
        canceled.addEffect(new MobEffectInstance(Seffects.MYCELIUM,200));
        var canceledDeath=new LivingDeathEvent(canceled,canceled.damageSources().generic());canceledDeath.setCanceled(true);
        check.accept(!FungalEcology.convertUnmatched(canceledDeath),"canceled death never converts");
        for(var mob:level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(pos).inflate(16)))mob.discard();
    }
    public static void prepareRestart(ServerLevel level) {
        var persisted=create(level,new BlockPos(176,240,176));persisted.setMass(5);persisted.setOrigin("sporebound:restart_fixture");persisted.setPersistenceRequired();
    }
    public static void afterRestart(ServerLevel level, BiConsumer<Boolean,String> check) {
        var found=level.getEntitiesOfClass(InfectedBiomass.class,new net.minecraft.world.phys.AABB(new BlockPos(176,240,176)).inflate(8),
            b -> b.origin().equals("sporebound:restart_fixture"));
        check.accept(found.size()==1 && found.getFirst().mass()==5 && !found.getFirst().absorbing(),
            "biomass mass and origin survive a full server restart");
    }
    private static InfectedBiomass create(ServerLevel level,BlockPos pos) {
        var b=FungalContent.BIOMASS.get().create(level);b.moveTo(pos.getX(),pos.getY(),pos.getZ());b.setNoAi(true);
        if(!level.addFreshEntity(b))throw new AssertionError("Biomass fixture spawn was rejected");return b;
    }
}
