package dev.sporebound;

import com.Harbinger.Spore.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.function.BiConsumer;

/** Opt-in native regression checks for the 0.8 frontier update. */
public final class FrontierValidation {
    public static void run(ServerLevel level,BiConsumer<Boolean,String> check){
        BlockPos pos=new BlockPos(220,280,220);level.getChunkAt(pos);
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,Blocks.GRASS_BLOCK.defaultBlockState());
        new com.Harbinger.Spore.Sentities.FoliageSpread(){}.convertBlocks(level.getBlockState(pos),level,pos);
        check.accept(level.getBlockState(pos).is(FungalContent.CRUST.get()),"native Spore grass conversion produces modified Spore Mycelium");
        level.setBlockAndUpdate(pos,Blocks.MYCELIUM.defaultBlockState());
        check.accept(level.getBlockState(pos).is(Blocks.MYCELIUM),"ordinary vanilla mycelium placement remains vanilla");
        level.setBlockAndUpdate(pos,Sblocks.INFESTED_DIRT.get().defaultBlockState());
        check.accept(level.getBlockState(pos).is(FungalContent.CRUST.get()),"exposed infected dirt becomes Spore Mycelium");
        level.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,Sblocks.INFESTED_DIRT.get().defaultBlockState());
        check.accept(level.getBlockState(pos).is(Sblocks.INFESTED_DIRT.get()),"covered infected dirt stays underground substrate");
        level.setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());
        check.accept(!SporeMyceliumBlock.spreadTo(level,pos),"mycelium cannot spread below a solid ceiling");
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        check.accept(SporeMyceliumBlock.spreadTo(level,pos)&&level.getBlockState(pos).is(FungalContent.CRUST.get()),"living soil spreads its own registered block");
        level.setBlockAndUpdate(pos,FungalContent.PALE.get().defaultBlockState());
        check.accept(!SporeMyceliumBlock.spreadTo(level,pos)&&level.getBlockState(pos).is(FungalContent.PALE.get()),"mycelium preserves pale remnant material");
        var dormant=level.getServer().overworld();dormant.getChunkAt(pos);dormant.setBlockAndUpdate(pos,Blocks.GRASS_BLOCK.defaultBlockState());
        new com.Harbinger.Spore.Sentities.FoliageSpread(){}.convertBlocks(dormant.getBlockState(pos),dormant,pos);
        check.accept(dormant.getBlockState(pos).is(Blocks.GRASS_BLOCK)&&!SporeMyceliumBlock.spreadTo(dormant,pos),"dormant world rejects native and custom soil spread");
        var remote=new BlockPos(500000,80,500000);var remoteHive=Sentities.PROTO.get().create(level);
        remoteHive.moveTo(remote.getX(),remote.getY(),remote.getZ());boolean loadedBefore=level.hasChunkAt(remote);
        HivePopulation.added(remoteHive);
        check.accept(HiveNodes.get(level).available(level).stream().anyMatch(n->n.mind()&&n.pos().equals(remote))
            &&level.hasChunkAt(remote)==loadedBefore,"Hive census exposes distant unloaded locations without loading their chunks");
        HivePopulation.removed(remoteHive,Entity.RemovalReason.DISCARDED);
        check.accept(HiveNodes.get(level).available(level).stream().noneMatch(n->n.pos().equals(remote)),"removed distant Hive disappears from sensing");
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(UUID.fromString("30000000-0000-0000-0000-000000000008"),"frontier-test"));
        player.moveTo(pos.getX(),pos.getY()+1,pos.getZ());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getInventory().clearContent();
        Item[] bases={Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS};
        Item[] bound={Hivebound.HELMET.get(),Hivebound.CHEST.get(),Hivebound.LEGS.get(),Hivebound.BOOTS.get()};
        for(int i=0;i<4;i++){
            var base=new ItemStack(bases[i]);base.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Gathered armor"));
            for(Item mushroom:List.of(Items.RED_MUSHROOM,Items.BROWN_MUSHROOM)){
                var input=new SmithingRecipeInput(new ItemStack(mushroom),base,new ItemStack(Items.ROTTEN_FLESH));
                var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMITHING,input,level);
                check.accept(recipe.isPresent(),"accessible Hivebound recipe accepts "+mushroom+" for "+bases[i]);
                var result=recipe.orElseThrow().value().assemble(input,level.registryAccess());
                check.accept(result.is(bound[i])&&result.getHoverName().getString().equals("Gathered armor"),"affordable upgrade preserves armor components");
                player.setItemSlot(Hivebound.SLOTS.get(i),result);
            }
        }
        player.horizontalCollision=true;
        check.accept(!HiveClimbing.eligible(player),"Bound stage cannot climb walls");
        HiveboundEvolution.add(player,HiveboundEvolution.first(),0);
        check.accept(HiveClimbing.eligible(player)&&player.onClimbable(),"Evolved Hivebound uses real vanilla climbable motion at a wall");
        player.horizontalCollision=false;check.accept(!player.onClimbable(),"climbing requires wall contact");
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);check.accept(!HiveClimbing.eligible(player),"removing full symbiosis disables climbing");
        check.accept(!HiveActionPayload.execute(player,new HiveActionPayload(HiveActionPayload.TRAVEL,pos)),"nonmember cannot forge travel request");
        check.accept(!HiveActionPayload.execute(player,new HiveActionPayload(999,pos)),"unknown ability opcode is rejected");
        player.getInventory().clearContent();
        var survivor=FungalContent.SURVIVOR.get().create(level);survivor.moveTo(pos.getX()+2,pos.getY()+1,pos.getZ());
        survivor.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.COMMAND,null);level.addFreshEntity(survivor);
        var uninfected=new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(survivor,level.damageSources().generic());
        check.accept(!SurvivorConversion.convert(uninfected),"uninfected survivor deaths do not convert");
        level.setBlockAndUpdate(pos.below(),Blocks.DIAMOND_ORE.defaultBlockState());
        check.accept(!survivor.canMine(level,pos.below()),"starter stone tools cannot harvest diamond ore");
        survivor.supplies().addItem(new ItemStack(Items.IRON_INGOT,13));survivor.supplies().addItem(new ItemStack(Items.STICK,3));
        SurvivorProgression.improve(survivor);
        check.accept(survivor.miningTool().is(Items.IRON_PICKAXE)&&survivor.getMainHandItem().is(Items.IRON_SWORD)
            &&survivor.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)&&survivor.supplies().countItem(Items.IRON_INGOT)==0,
            "survivor turns exactly thirteen iron and three sticks into pickaxe, sword and chestplate");
        check.accept(survivor.canMine(level,pos.below()),"crafted iron tool unlocks diamond ore harvesting");
        survivor.learn(96);check.accept(survivor.getMaxHealth()==36,"survivor work experience raises bounded combat health");
        survivor.supplies().addItem(new ItemStack(Items.COBBLESTONE,8));survivor.supplies().addItem(new ItemStack(Items.COAL));survivor.supplies().addItem(new ItemStack(Items.RAW_IRON,2));
        SurvivorProgression.improve(survivor);
        check.accept(survivor.hasFurnace()&&survivor.fuel()==7&&survivor.supplies().countItem(Items.IRON_INGOT)==1
            &&survivor.supplies().countItem(Items.RAW_IRON)==1&&survivor.supplies().countItem(Items.COAL)==0,"smelting uses a paid furnace, finite coal and real raw ore");
        CompoundTag tag=new CompoundTag();survivor.saveWithoutId(tag);var restored=FungalContent.SURVIVOR.get().create(level);restored.load(tag);
        check.accept(restored.experience()==survivor.experience()&&restored.fuel()==7&&restored.hasFurnace()
            &&restored.miningTool().is(Items.IRON_PICKAXE)&&restored.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),"survivor experience, equipment, mining tool and fuel survive save/load");
        survivor.addEffect(new net.minecraft.world.effect.MobEffectInstance(Seffects.MYCELIUM,200));
        var event=new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(survivor,level.damageSources().generic());event.setCanceled(true);
        check.accept(!SurvivorConversion.convert(event)&&!survivor.isRemoved(),"canceled deaths cannot convert survivors");event.setCanceled(false);
        survivor.hurt(level.damageSources().generic(),1000);
        var converted=level.getEntitiesOfClass(com.Harbinger.Spore.Sentities.BasicInfected.InfectedPlayer.class,survivor.getBoundingBox().inflate(2));
        check.accept(survivor.isRemoved()&&converted.size()==1&&converted.getFirst().getMainHandItem().is(Items.IRON_SWORD)
            &&converted.getFirst().getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
            &&java.util.Objects.equals(converted.getFirst().getCustomName(),survivor.getCustomName()),"actual infected death creates exactly one equipped native Infected Adventurer");
        check.accept(level.getEntitiesOfClass(InfectedBiomass.class,survivor.getBoundingBox().inflate(2)).isEmpty(),"infected survivor death never becomes generic biomass");
        check.accept(!SurvivorConversion.convert(event),"converted survivor cannot duplicate its equipment");converted.forEach(Entity::discard);
    }
}
