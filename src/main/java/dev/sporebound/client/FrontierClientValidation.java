package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.function.*;

/** Real key input, screen rendering, private distant markers and a serverbound travel packet. */
public final class FrontierClientValidation {
    private static int phase,ticks;
    private static long deadline;
    private static volatile BlockPos destination,remote;
    private static com.Harbinger.Spore.Sentities.Organoids.Proto remoteHive;
    private static double originalIndex;
    public static boolean tick(BiConsumer<Boolean,String> check,Consumer<String> screenshot){
        if(phase==5)return true;var mc=Minecraft.getInstance();
        if(phase==0){phase=1;deadline=System.nanoTime()+120_000_000_000L;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
                originalIndex=CorruptionData.get(level).index();CorruptionData.get(level).set(8);
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(Hivebound.HELMET.get()));
                HiveboundEvolution.data(p).putInt("EvolutionPoints",HiveboundEvolution.hyper());HiveboundEvolution.data(p).remove("NodeTravelReady");
                var base=p.blockPosition().above(10);
                for(var pos:BlockPos.betweenClosed(base.offset(-3,-1,-3),base.offset(3,3,16)))
                    level.setBlockAndUpdate(pos,pos.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                var from=base.below();destination=from.offset(0,0,12);
                level.setBlockAndUpdate(from,com.Harbinger.Spore.core.Sblocks.BIOMASS_LUMP.get().defaultBlockState());
                level.setBlockAndUpdate(destination,com.Harbinger.Spore.core.Sblocks.BIOMASS_LUMP.get().defaultBlockState());
                HiveNodes.get(level).discover(level,from);HiveNodes.get(level).discover(level,destination);
                remote=from.offset(0,0,50000);remoteHive=com.Harbinger.Spore.core.Sentities.PROTO.get().create(level);
                remoteHive.moveTo(remote.getX(),remote.getY(),remote.getZ());HivePopulation.added(remoteHive);
                p.teleportTo(level,base.getX()+0.5,base.getY(),base.getZ()+0.5,Set.of(),0,0);WorldRules.sync(p);HiveSensePayload.sync(p);
            });
        }
        if(System.nanoTime()>deadline)throw new AssertionError("Timed out waiting for frontier client phase "+phase);
        if(phase==1){
            if(destination==null||!HiveVision.enabled()||HiveSensePayload.ClientState.current.sites().stream().noneMatch(n->n.pos().equals(remote)))return false;
            KeyMapping.click(HiveControls.NETWORK.getKey());phase=2;ticks=0;
        }else if(phase==2){
            if(!(mc.screen instanceof HiveNetworkScreen)||++ticks<20)return false;
            check.accept(true,"configured Hive network key opens the real control screen");screenshot.accept("16-hive-network-controls.png");
            mc.setScreen(null);select(remote);phase=3;ticks=0;
        }else if(phase==3&&++ticks>=20){
            check.accept(HiveControls.selection()!=null&&HiveControls.selection().pos().equals(remote),"selected Hive beacon remains available fifty thousand blocks away");
            check.accept(mc.level.getChunkSource().getChunk(remote.getX()>>4,remote.getZ()>>4,
                net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null,"distant beacon does not require an actual client chunk");
            screenshot.accept("17-distant-hive-beacon.png");select(destination);KeyMapping.click(HiveControls.TRAVEL.getKey());phase=4;
        }else if(phase==4){
            if(!destination.above().closerToCenterThan(mc.player.position(),3))return false;
            check.accept(true,"travel key sends a real serverbound ability packet and teleports the player");
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                HivePopulation.removed(remoteHive,Entity.RemovalReason.DISCARDED);CorruptionData.get(p.serverLevel()).set(originalIndex);
                HiveSensePayload.sync(p);WorldRules.sync(p);
            });phase=5;
        }
        return phase==5;
    }
    private static void select(BlockPos pos){
        var sites=HiveSensePayload.ClientState.current.sites();
        for(int i=0;i<sites.size();i++){
            var site=HiveControls.selection();if(site!=null&&site.pos().equals(pos))return;HiveControls.cycle(1);
        }
        throw new AssertionError("Fixture node missing from client network: "+pos);
    }
}
