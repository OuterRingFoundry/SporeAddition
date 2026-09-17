package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.function.*;

/** Exercises real private packets, through-wall glow selection, skins and appearance release. */
public final class CollectiveClientValidation {
    private static int phase,ticks;
    private static volatile UUID target;
    private static volatile boolean nativeOnly;
    private static long deadline;
    public static boolean tick(BiConsumer<Boolean,String> check,Consumer<String> screenshot){
        if(phase==4)return true;
        var mc=Minecraft.getInstance();
        if(phase==0){phase=1;deadline=System.nanoTime()+120_000_000_000L;
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=player.serverLevel();
                CorruptionData.get(level).set(6);HiveboundEvolution.data(player).putInt("EvolutionPoints",HiveboundEvolution.hyper());
                var base=player.blockPosition().above(12);
                for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-8),base.offset(8,5,8)))
                    level.setBlockAndUpdate(p,p.getY()==base.getY()-1?FungalContent.CRUST.get().defaultBlockState():Blocks.AIR.defaultBlockState());
                player.teleportTo(level,base.getX()+0.5,base.getY(),base.getZ()-5.5,Set.of(),0,0);
                var hive=com.Harbinger.Spore.core.Sentities.PROTO.get().create(level);hive.moveTo(base.getX()+6,base.getY(),base.getZ()+6);hive.setNoAi(true);
                check.accept(level.addFreshEntity(hive),"client collective fixture admits a real Hive Mind");
                var survivor=FungalContent.SURVIVOR.get().create(level);survivor.moveTo(base.getX()+0.5,base.getY(),base.getZ()+2.5);
                survivor.finalizeSpawn(level,level.getCurrentDifficultyAt(base),net.minecraft.world.entity.MobSpawnType.COMMAND,null);
                survivor.setNoAi(true);level.addFreshEntity(survivor);
                survivor.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.Harbinger.Spore.core.Seffects.MARKER,600));target=survivor.getUUID();
                for(var p:BlockPos.betweenClosed(base.offset(-2,0,0),base.offset(2,3,0)))level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(base.offset(-4,-1,-3),com.Harbinger.Spore.core.Sblocks.BIOMASS_LUMP.get().defaultBlockState());
                var destination=base.offset(-4,-1,5);
                level.setBlockAndUpdate(destination,com.Harbinger.Spore.core.Sblocks.BIOMASS_LUMP.get().defaultBlockState());
                HiveNodes.get(level).discover(level,base);var nodes=HiveNodes.get(level).available(level);int index=-1;
                for(int i=0;i<nodes.size();i++)if(nodes.get(i).pos().equals(destination))index=i;
                check.accept(HiveNodes.travel(player,index)&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(destination))<50,
                    "real Hyper player teleports safely between Spore nodes");
                player.teleportTo(level,base.getX()+0.5,base.getY(),base.getZ()-5.5,Set.of(),0,0);
                nativeOnly=HiveNetwork.marks(level).stream().noneMatch(m->m.id().equals(survivor.getUUID()));
                HiveSensePayload.sync(player);InfusionPayload.sync(player);WorldRules.sync(player);
            });
        }
        if(System.nanoTime()>deadline)throw new AssertionError("Timed out waiting for collective client phase "+phase);
        if(phase==1){
            if(target==null||!HiveVision.enabled()||InfusionPayload.ClientState.stages.getOrDefault(mc.player.getUUID(),0)<2)return false;
            var victim=java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).filter(e->e.getUUID().equals(target)).findFirst().orElse(null);
            if(victim==null||!HiveVision.marked(victim))return false;
            if(++ticks<30)return false;
            check.accept(!mc.player.hasLineOfSight(victim)&&mc.shouldEntityAppearGlowing(victim),"marked survivor is highlighted through solid blocks for Hivebound viewer");
            check.accept(!victim.isCurrentlyGlowing(),"private hive vision never sets the globally visible glowing flag");
            check.accept(nativeOnly&&HiveVision.marked(victim),"native Spore Marker targets synchronize independently of collective orders");
            check.accept(!HiveSensePayload.ClientState.current.sites().isEmpty(),"real client receives native Spore node locations");
            check.accept(mc.getEntityRenderDispatcher().getRenderer((Survivor)victim) instanceof SurvivorRenderer,"survivor uses a player skin renderer");
            check.accept(mc.getResourceManager().getResource(mc.getEntityRenderDispatcher().getRenderer(victim).getTextureLocation(victim)).isPresent(),"survivor player skin resolves to a real Minecraft texture");
            screenshot.accept("14-hive-sense.png");mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));phase=2;ticks=0;
        }else if(phase==2&&++ticks>=30){
            screenshot.accept("15-hive-infusion.png");mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);InfusionPayload.sync(p);HiveSensePayload.sync(p);
            });phase=3;ticks=0;
        }else if(phase==3){
            if(Hivebound.member(mc.player)||InfusionPayload.ClientState.stages.containsKey(mc.player.getUUID()))return false;
            check.accept(!HiveVision.enabled(),"release from Hivebound clears private sight and infused appearance");phase=4;
        }
        return phase==4;
    }
}
