package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.function.*;

/** A real client must receive and render both bow draw and offhand shield use. */
public final class SurvivorClientValidation {
    private static int phase,ticks;
    private static long deadline;
    private static volatile int archerId,guardId;
    public static boolean tick(BiConsumer<Boolean,String> check,Consumer<String> screenshot){
        if(phase==2)return true;var mc=Minecraft.getInstance();
        if(phase==0){phase=1;deadline=System.nanoTime()+120_000_000_000L;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();var base=p.blockPosition().above(8);
                for(var pos:BlockPos.betweenClosed(base.offset(-5,-1,-7),base.offset(5,4,5)))
                    level.setBlockAndUpdate(pos,pos.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                p.teleportTo(level,base.getX()+0.5,base.getY()+1,base.getZ()-5.5,Set.of(),0,8);
                for(int i=0;i<2;i++){
                    var s=FungalContent.SURVIVOR.get().create(level);s.moveTo(base.getX()+(i==0?-1.5:2.5),base.getY(),base.getZ()+0.5,180,0);
                    s.finalizeSpawn(level,level.getCurrentDifficultyAt(base),net.minecraft.world.entity.MobSpawnType.COMMAND,null);
                    if(i==0)s.equipStarterCombat(true);s.setNoAi(true);s.setNoGravity(true);s.setYHeadRot(180);s.setYBodyRot(180);
                    level.addFreshEntity(s);s.startUsingItem(i==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
                    if(i==0)archerId=s.getId();else guardId=s.getId();
                }
            });
        }
        if(System.nanoTime()>deadline)throw new AssertionError("Timed out waiting for survivor combat display");
        if(!(mc.level.getEntity(archerId) instanceof Survivor archer)||!(mc.level.getEntity(guardId) instanceof Survivor guard)
            ||!archer.isUsingItem()||!archer.getUseItem().is(net.minecraft.world.item.Items.BOW)||!guard.isBlocking())return false;
        if(++ticks<20)return false;
        check.accept(archer.getUsedItemHand()==InteractionHand.MAIN_HAND&&guard.getUsedItemHand()==InteractionHand.OFF_HAND,
            "survivor bow draw and offhand shield blocking synchronize to the real client");
        screenshot.accept("20-survivor-squad.png");
        mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().serverLevel();
            for(int id:new int[]{archerId,guardId}){var entity=level.getEntity(id);if(entity!=null)entity.discard();}
        });phase=2;return true;
    }
}
