package dev.sporebound;

import com.Harbinger.Spore.core.Sentities;
import com.Harbinger.Spore.core.Sblocks;
import com.Harbinger.Spore.SBlockEntities.LivingStructureBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.function.BiConsumer;

public final class HiveboundEvolutionValidation {
    public static void run(ServerLevel level,ServerPlayer player,BiConsumer<Boolean,String> check){
        var data=HiveboundEvolution.data(player);data.putInt("EvolutionPoints",0);data.putInt("Kills",0);
        Hivebound.update(player);double health=player.getMaxHealth(),damage=player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        var victim=EntityType.COW.create(level);victim.setHealth(0);
        player.awardKillScore(victim,1,level.damageSources().playerAttack(player));
        player.awardKillScore(victim,1,level.damageSources().playerAttack(player));
        check.accept(HiveboundEvolution.points(player)==1&&HiveboundEvolution.kills(player)==1,
            "real player kill-score callback awards one evolution point and one kill point without duplicate conversion credit");
        check.accept(player.getMaxHealth()>health&&player.getAttributeValue(Attributes.ATTACK_DAMAGE)>damage,
            "evolution points strengthen player abilities independently of world Index");
        player.getFoodData().setExhaustion(0);HiveboundEvolution.hungerSecond(player);
        check.accept(player.getFoodData().getExhaustionLevel()>0,"Hivebound armor increases survival hunger");
        var base=player.blockPosition();
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(base.offset(-1,-1,-1),base.offset(8,-1,1)))level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        var basic=Sentities.INF_HUMAN.get().create(level);basic.moveTo(player.getX()+6,player.getY(),player.getZ());
        basic.setEvoPoints(0);basic.setKills(3);basic.tickCount=80;
        check.accept(level.addFreshEntity(basic),"lower-level native follower joins world");
        level.addNewPlayer(player);
        var goal=basic.goalSelector.getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g instanceof HiveboundFollowGoal)
            .map(g->(HiveboundFollowGoal)g).findFirst().orElseThrow();
        check.accept(HiveboundEvolution.dominates(player,basic)&&goal.canUse(),"lower-level infected chooses the nearby Hivebound player as leader");
        goal.tick();check.accept(!basic.getNavigation().isDone(),"follower starts a real navigation path toward its leader");
        basic.setEvoPoints(HiveboundEvolution.points(player));
        check.accept(!goal.canContinueToUse(),"equal-level infected stops following");basic.setEvoPoints(0);goal.stop();
        basic.moveTo(player.getX()+1,player.getY(),player.getZ());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        check.accept(!HiveboundEvolution.consume(player,basic),"normal interaction never consumes a fungal ally");
        player.setShiftKeyDown(true);player.getFoodData().setFoodLevel(6);player.getFoodData().setSaturation(0);
        check.accept(HiveboundEvolution.consume(player,basic)&&basic.isRemoved()&&player.getFoodData().getFoodLevel()>6,
            "crouch empty-hand consumption satisfies hunger and removes a weaker ally");
        check.accept(HiveboundEvolution.points(player)==2&&HiveboundEvolution.kills(player)==4,
            "consumption transfers native stored kill points and adds evolution nutrition");
        check.accept(!HiveboundEvolution.consume(player,basic)&&HiveboundEvolution.points(player)==2,
            "the same consumed ally cannot grant resources twice");
        var stronger=Sentities.OGRE.get().create(level);stronger.moveTo(player.getX()+1,player.getY(),player.getZ());
        check.accept(!HiveboundEvolution.consume(player,stronger),"a low-level player cannot consume a Hyper infected");
        player.setShiftKeyDown(false);
        var hive=Sentities.PROTO.get().create(level);hive.moveTo(player.position());int biomass=hive.getBiomass();
        check.accept(HiveboundEvolution.feedHive(player,hive,2)==2&&hive.getBiomass()==biomass+2
            &&HiveboundEvolution.kills(player)==2&&HiveboundEvolution.points(player)==2,
            "player kill points enter native Hive biomass accounting without spending evolution points");
        level.setBlockAndUpdate(base.below(),Sblocks.BIOMASS_LUMP.get().defaultBlockState());
        var structure=(LivingStructureBlocks)level.getBlockEntity(base.below());int stored=structure.getKills();
        check.accept(HiveboundEvolution.feedStructure(player)&&structure.getKills()==stored+1&&HiveboundEvolution.kills(player)==1,
            "Hivebound killing feeds native Spore structure kill scores");
        var saved=player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).copy();
        player.getPersistentData().remove(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,saved);
        check.accept(HiveboundEvolution.points(player)==2&&HiveboundEvolution.kills(player)==1,
            "evolution and remaining kill points persist in the vanilla respawn-persistent player data");
        var helmet=player.getItemBySlot(EquipmentSlot.HEAD);player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        var unboundVictim=EntityType.COW.create(level);unboundVictim.setHealth(0);
        player.awardKillScore(unboundVictim,1,level.damageSources().playerAttack(player));
        check.accept(HiveboundEvolution.points(player)==2&&!HiveboundEvolution.dominates(player,stronger),
            "partial armor cannot earn fungal kill credit or command infected");
        player.setItemSlot(EquipmentSlot.HEAD,helmet);
        level.setBlockAndUpdate(base.below(),Blocks.STONE.defaultBlockState());
        level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
    }
}
