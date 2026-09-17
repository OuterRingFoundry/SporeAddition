package dev.sporebound.client;

import dev.sporebound.*;
import com.Harbinger.Spore.Client.ArmorModelList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Opt-in real renderer coverage: every Index art stage and all equipped armor model parts. */
public final class SymbiosisClientValidation {
    private static int phase,ticks;
    private static long deadline;
    private static boolean sent;
    private static volatile boolean followerChecked;
    public static boolean tick(BiConsumer<Boolean,String> check,Consumer<String> screenshot){
        if(phase==4)return true;
        var mc=Minecraft.getInstance();
        if(phase==0){
            deadline=System.nanoTime()+120_000_000_000L;phase=1;CorruptionHud.validationGallery=true;
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                var items=List.of(Hivebound.HELMET,Hivebound.CHEST,Hivebound.LEGS,Hivebound.BOOTS);
                for(int i=0;i<4;i++)player.setItemSlot(Hivebound.SLOTS.get(i),new ItemStack(items.get(i).get()));
                HiveboundEvolution.add(player,4,3);Hivebound.update(player);WorldRules.sync(player);
            });
        }
        if(System.nanoTime()>deadline)throw new AssertionError("Timed out waiting for symbiotic client phase "+phase);
        if(phase==1){
            if(!Hivebound.member(mc.player)||EvolutionPayload.ClientState.current==null
                ||EvolutionPayload.ClientState.current.points()!=4)return false;
            if(!sent){sent=true;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(5,ItemStack.EMPTY));}
            if(++ticks<30)return false;
            check.accept(Hivebound.member(mc.player),"real creative slot packet retains bound equipment on server and client");
            check.accept(ArmorModelList.ARMOR_RENDERING_BITS.stream().filter(p->p instanceof HiveboundRendering.BoundPart).count()==9,
                "Hivebound set registers all nine native organic armor parts");
            for(var slot:Hivebound.SLOTS){
                var stack=mc.player.getItemBySlot(slot);var armor=(HiveboundArmor)stack.getItem();
                check.accept(stack.hasFoil()&&mc.getResourceManager().getResource(armor.getTextureLocation()).isPresent(),
                    "Hivebound organic texture and enhanced glint load for "+slot);
                check.accept(mc.getItemRenderer().getModel(stack,mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),
                    "Hivebound inventory model loads for "+slot);
            }
            check.accept(Hivebound.locked(mc.player,mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)),
                "client predicts binding using synchronized dimension Index");
            check.accept(EvolutionPayload.ClientState.current.kills()==3&&EvolutionPayload.ClientState.current.hyper()==HiveboundEvolution.hyper(),
                "evolution meter receives points, kill score and native thresholds from the server");
            check.accept(CorruptionHud.roman(0).equals("Normal"),"client names Index zero Normal");
            screenshot.accept("11-index-art-stages.png");CorruptionHud.validationGallery=false;
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setGameMode(GameType.SURVIVAL));
            phase=2;ticks=0;
        }else if(phase==2){
            if(mc.gameMode.hasInfiniteItems())return false;
            if(++ticks<30)return false;
            screenshot.accept("13-hivebound-evolution.png");
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                var level=player.serverLevel();var mob=com.Harbinger.Spore.core.Sentities.INF_HUMAN.get().create(level);
                mob.moveTo(player.getX()+5,player.getY(),player.getZ());mob.setEvoPoints(0);mob.setOnGround(true);mob.tickCount=80;
                check.accept(level.addFreshEntity(mob),"real-player follower fixture joins the integrated server");
                var goal=mob.goalSelector.getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g instanceof HiveboundFollowGoal)
                    .map(g->(HiveboundFollowGoal)g).findFirst().orElseThrow();
                check.accept(goal.canUse(),"lower-level infected chooses the real Hivebound player as leader");
                goal.tick();check.accept(!mob.getNavigation().isDone(),"follower starts a real path toward the actual player");
                mob.setEvoPoints(HiveboundEvolution.points(player));
                check.accept(!goal.canContinueToUse(),"follower releases the real player when their levels become equal");
                mob.discard();followerChecked=true;
            });
            mc.setScreen(new InventoryScreen(mc.player));phase=3;ticks=0;
        }else if(phase==3&&++ticks>=30&&followerChecked){
            screenshot.accept("12-hivebound-armor.png");mc.setScreen(null);phase=4;
        }
        return phase==4;
    }
}
