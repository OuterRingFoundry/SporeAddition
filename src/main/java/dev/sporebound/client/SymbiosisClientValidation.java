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
    public static boolean tick(BiConsumer<Boolean,String> check,Consumer<String> screenshot){
        var mc=Minecraft.getInstance();
        if(phase==0){
            deadline=System.nanoTime()+120_000_000_000L;phase=1;CorruptionHud.validationGallery=true;
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                var items=List.of(Hivebound.HELMET,Hivebound.CHEST,Hivebound.LEGS,Hivebound.BOOTS);
                for(int i=0;i<4;i++)player.setItemSlot(Hivebound.SLOTS.get(i),new ItemStack(items.get(i).get()));
                Hivebound.update(player);WorldRules.sync(player);
            });
        }
        if(System.nanoTime()>deadline)throw new AssertionError("Timed out waiting for symbiotic client phase "+phase);
        if(phase==1){
            if(!Hivebound.member(mc.player)||++ticks<30)return false;
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
            check.accept(CorruptionHud.roman(0).equals("Normal"),"client names Index zero Normal");
            screenshot.accept("11-index-art-stages.png");CorruptionHud.validationGallery=false;
            mc.setScreen(new InventoryScreen(mc.player));phase=2;ticks=0;
        }else if(phase==2&&++ticks>=30){
            screenshot.accept("12-hivebound-armor.png");mc.setScreen(null);phase=3;
        }
        return phase==3;
    }
}
