package dev.sporebound;

import com.Harbinger.Spore.core.*;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.function.BiConsumer;

/** Opt-in native checks for collective intelligence, player abilities and persistent survivors. */
public final class CollectiveValidation {
    public static void run(ServerLevel level,BiConsumer<Boolean,String> check){
        var base=new BlockPos(176,260,176);arena(level,base,14);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(UUID.fromString("30000000-0000-0000-0000-000000000007"),"collective-test"));
        player.moveTo(base.getX(),base.getY(),base.getZ());player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
        var armor=List.of(Hivebound.HELMET,Hivebound.CHEST,Hivebound.LEGS,Hivebound.BOOTS);
        for(int i=0;i<4;i++)player.setItemSlot(Hivebound.SLOTS.get(i),new ItemStack(armor.get(i).get()));
        player.setShiftKeyDown(false);var biomass=FungalContent.BIOMASS.get().create(level);
        biomass.moveTo(player.getX()+1,player.getY(),player.getZ());biomass.setMass(3);level.addFreshEntity(biomass);
        check.accept(!HiveProgression.consume(player,biomass),"ordinary interaction does not consume biomass");
        player.setShiftKeyDown(true);player.getFoodData().setFoodLevel(5);
        check.accept(HiveProgression.consume(player,biomass)&&biomass.isRemoved()&&HiveboundEvolution.points(player)==3
            &&player.getFoodData().getFoodLevel()>5,"Hivebound consumes biomass once for evolution and food");
        check.accept(!HiveProgression.consume(player,biomass),"removed biomass cannot be consumed twice");player.setShiftKeyDown(false);
        var scout=Sentities.INF_HUMAN.get().create(level);scout.moveTo(base.getX()+4,base.getY(),base.getZ());level.addFreshEntity(scout);
        var distant=Sentities.INF_HUMAN.get().create(level);distant.moveTo(base.getX()+12,base.getY(),base.getZ());level.addFreshEntity(distant);
        var cow=EntityType.COW.create(level);cow.moveTo(base.getX()+5,base.getY(),base.getZ()+4);level.addFreshEntity(cow);
        scout.setTarget(cow);HiveNetwork.update(level);
        check.accept(HiveNetwork.marks(level).stream().anyMatch(m->m.id().equals(cow.getUUID()))&&distant.getTarget()==cow,
            "Hive Mind shares scout target: active="+HiveNetwork.active(level)+", count="+HivePopulation.get(level).count()+", sight="+scout.hasLineOfSight(cow)+", scout="+scout.getTarget()+", marks="+HiveNetwork.marks(level)+", distant="+distant.getTarget());
        cow.discard();HiveNetwork.update(level);
        check.accept(HiveNetwork.marks(level).isEmpty(),"removed marked targets are cleared from shared intelligence");
        scout.setTarget(null);distant.setTarget(null);
        scout.setEvoPoints(20);distant.setEvoPoints(2);distant.setKills(4);scout.setHunger(100);scout.setHealth(1);
        int points=scout.getEvoPoints();
        distant.moveTo(scout.getX()+1,scout.getY(),scout.getZ());
        check.accept(SporeConsolidationGoal.transfer(scout,distant)&&distant.isRemoved()&&scout.getEvoPoints()>points
            &&scout.getHunger()==0&&scout.getHealth()>1&&scout.getKills()>=4,"consumption transfers evolution, stored kills, health and hunger without loot");
        check.accept(!SporeConsolidationGoal.transfer(scout,distant),"consumed infected cannot duplicate resources");scout.discard();
        var from=base.offset(-3,-1,0);var to=base.offset(8,-1,0);
        level.setBlockAndUpdate(from,Sblocks.BIOMASS_LUMP.get().defaultBlockState());
        level.setBlockAndUpdate(to,Sblocks.BIOMASS_LUMP.get().defaultBlockState());
        var ledger=HiveNodes.get(level);ledger.discover(level,from);ledger.discover(level,to);
        var nodes=ledger.available(level);int index=-1;for(int i=0;i<nodes.size();i++)if(nodes.get(i).pos().equals(to))index=i;
        check.accept(index>=0,"native biomass blocks become persistent travel nodes");
        check.accept(!HiveNodes.travel(player,index),"low-evolution player cannot use node travel");
        HiveboundEvolution.data(player).putInt("EvolutionPoints",HiveboundEvolution.hyper());
        player.moveTo(from.getX()+0.5,from.getY()+1,from.getZ()+0.5);
        check.accept(HiveNodes.travel(player,index)&&HiveboundEvolution.data(player).getLong("NodeTravelReady")>0,"Hyper Hivebound accepts safe node travel and records its cooldown");
        check.accept(!HiveNodes.travel(player,index),"node travel enforces its cooldown");
        level.setBlockAndUpdate(to,Blocks.STONE.defaultBlockState());
        check.accept(ledger.available(level).stream().noneMatch(n->n.pos().equals(to)),"destroyed native nodes leave the network");
        double before=CorruptionData.get(level).index();CorruptionData.get(level).set(0);
        var victim=EntityType.COW.create(level);victim.moveTo(player.getX()+1,player.getY(),player.getZ());level.addFreshEntity(victim);
        victim.hurt(level.damageSources().playerAttack(player),1);
        check.accept(victim.hasEffect(Seffects.MYCELIUM)&&CorruptionData.get(level).index()>0,"evolved melee infects prey and starts positive growth from Index zero");
        victim.discard();CorruptionData.get(level).set(before);
        survivors(level,base.offset(0,10,0),check);
        awakening(level.getServer().overworld(),check);
        player.getInventory().clearContent();level.setBlockAndUpdate(from,Blocks.STONE.defaultBlockState());
    }
    private static void survivors(ServerLevel level,BlockPos base,BiConsumer<Boolean,String> check){
        arena(level,base,5);var colonies=SurvivorColonies.get(level);
        check.accept(colonies.found(level,base),"three survivors naturally establish a colony on eligible terrain");
        var residents=level.getEntitiesOfClass(Survivor.class,new net.minecraft.world.phys.AABB(base).inflate(8));
        check.accept(residents.size()==3&&residents.stream().allMatch(s->s.home().equals(base)),"colony members retain a common home");
        var worker=residents.getFirst();worker.moveTo(base.getX(),base.getY(),base.getZ());
        var item=new net.minecraft.world.entity.item.ItemEntity(level,worker.getX()+0.5,worker.getY(),worker.getZ(),new ItemStack(Items.IRON_INGOT,3));level.addFreshEntity(item);
        check.accept(worker.collect(item)&&item.isRemoved()&&worker.supplies().countItem(Items.IRON_INGOT)==3,"survivor collects dropped resources without duplication");
        CompoundTag save=new CompoundTag();worker.saveWithoutId(save);var restored=FungalContent.SURVIVOR.get().create(level);restored.load(save);
        check.accept(restored.skin()==worker.skin()&&restored.home().equals(base)&&restored.supplies().countItem(Items.IRON_INGOT)==3,"survivor appearance, home and inventory survive serialization");
        var rules=level.getGameRules();boolean grief=rules.getBoolean(GameRules.RULE_MOBGRIEFING);rules.getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
        check.accept(colonies.build(level,base,worker,3)==0,"survivor construction respects mobGriefing");rules.getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
        int supplies=worker.supplies().countItem(Items.COBBLESTONE),built=colonies.build(level,base,worker,3);
        check.accept(built==3&&worker.supplies().countItem(Items.COBBLESTONE)==supplies-3,"survivor builds housing using actual finite supplies");
        var obstruction=base.offset(-2,0,-2);level.setBlockAndUpdate(obstruction,Blocks.DIAMOND_BLOCK.defaultBlockState());
        check.accept(colonies.build(level,base,worker,3)==0&&level.getBlockState(obstruction).is(Blocks.DIAMOND_BLOCK),"colony work preserves obstructing player blocks");
        level.setBlockAndUpdate(obstruction,Blocks.COBBLESTONE.defaultBlockState());
        worker.supplies().addItem(new ItemStack(Items.COBBLESTONE,128));colonies.build(level,base,worker,128);
        var peer=base.offset(80,0,0);
        for(var p:BlockPos.betweenClosed(base.offset(3,-1,-4),peer.offset(4,5,4))){level.getChunkAt(p);
            level.setBlockAndUpdate(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
        check.accept(colonies.found(level,peer),"a second survivor colony forms at safe spacing");
        worker.setOnGround(true);
        check.accept(colonies.road(level,worker)>0,"survivors construct a real navigable route toward a neighboring colony");
        for(var resident:level.getEntitiesOfClass(Survivor.class,new net.minecraft.world.phys.AABB(peer).inflate(8)))resident.discard();
        var infected=Sentities.INF_HUMAN.get().create(level);infected.moveTo(base.getX()+1,base.getY(),base.getZ());level.addFreshEntity(infected);
        infected.setTarget(worker);worker.setTarget(infected);
        check.accept(FungalEcology.prey(worker)&&infected.getTarget()==worker&&worker.getTarget()==infected,"Spore and survivors can target each other");
        check.accept(SurvivorColonies.allowed(level)&&SurvivorColonies.allowed(level.getServer().overworld())
            &&SurvivorColonies.allowed(level.getServer().getLevel(Level.NETHER))&&!SurvivorColonies.allowed(level.getServer().getLevel(Level.END)),"natural survivors are limited to Overworld, Nether and Blighted World");
        rules.getRule(GameRules.RULE_MOBGRIEFING).set(grief,level.getServer());infected.discard();residents.forEach(Entity::discard);
    }
    private static void awakening(ServerLevel level,BiConsumer<Boolean,String> check){
        var base=new BlockPos(96,240,96);arena(level,base,4);RiftCairn.build(level,base.below());
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(UUID.fromString("30000000-0000-0000-0000-000000000017"),"awakening-test"));
        player.moveTo(base.getX()+0.5,base.getY(),base.getZ()+0.5);player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Sporebound.TALISMAN.get()));
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.NETHER_STAR,2));
        player.setShiftKeyDown(true);
        check.accept(HiveProgression.awaken(player,base.below())&&CorruptionData.get(level).index()==-1,"first deliberate ritual interaction only arms awakening");
        check.accept(HiveProgression.awaken(player,base.below())&&CorruptionData.get(level).index()==-1,"holding ritual interaction cannot confirm awakening");
        player.setShiftKeyDown(false); // Opening chat releases held sneak on a real client.
        check.accept(HiveProgression.confirmAwakening(player)&&CorruptionData.get(level).index()==0&&player.getOffhandItem().getCount()==1,"confirmed ritual consumes exactly one Nether Star and changes minus one to zero");
        CorruptionData.get(level).set(-2);
        check.accept(!HiveProgression.awaken(player,base.below())&&CorruptionData.get(level).index()==-2,"awakening cannot unlock a purged dimension");
        CorruptionData.get(level).set(-1);player.getInventory().clearContent();
    }
    private static void arena(ServerLevel level,BlockPos base,int radius){
        for(var p:BlockPos.betweenClosed(base.offset(-radius,-1,-radius),base.offset(radius,5,radius))){level.getChunkAt(p);level.setBlockAndUpdate(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
    }
}
