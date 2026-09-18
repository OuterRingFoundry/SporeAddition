package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HiveControls {
    private static KeyMapping key(String name,int code){return new KeyMapping("key.sporebound."+name,code,"key.categories.sporebound");}
    public static final KeyMapping NETWORK=key("network",GLFW.GLFW_KEY_H),NEXT=key("next_node",GLFW.GLFW_KEY_N),
        TRAVEL=key("travel",GLFW.GLFW_KEY_J),DEPLOY=key("deploy",GLFW.GLFW_KEY_B),AWAKEN=key("awaken",GLFW.GLFW_KEY_K);
    private static BlockPos selected;
    private static net.minecraft.resources.ResourceLocation dimension;
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){for(var k:java.util.List.of(NETWORK,NEXT,TRAVEL,DEPLOY,AWAKEN))e.register(k);}
    public static HiveSensePayload.Site selection(){
        var p=HiveSensePayload.ClientState.current;
        if(!HiveVision.enabled()||p.sites().isEmpty()){selected=null;return null;}
        if(!p.dimension().equals(dimension)){selected=null;dimension=p.dimension();}
        for(var s:p.sites())if(s.pos().equals(selected))return s;
        var mc=Minecraft.getInstance();var s=p.sites().stream().min(java.util.Comparator.comparingDouble(n->n.pos().distToCenterSqr(mc.player.position()))).orElseThrow();
        selected=s.pos();return s;
    }
    public static void cycle(int step){
        var current=selection();if(current==null)return;var sites=HiveSensePayload.ClientState.current.sites();
        selected=sites.get(Math.floorMod(sites.indexOf(current)+step,sites.size())).pos();
    }
    public static void send(int action){
        var s=selection();if(action==HiveActionPayload.TRAVEL&&s==null)return;
        PacketDistributor.sendToServer(new HiveActionPayload(action,s==null?BlockPos.ZERO:s.pos()));
    }
    public static void reset(){selected=null;dimension=null;}
    @EventBusSubscriber(modid=Sporebound.ID,value=Dist.CLIENT)
    public static final class Ticks {
        @SubscribeEvent public static void tick(ClientTickEvent.Post e){
            var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
            // Drain presses while a screen is open; typing never performs abilities.
            while(NETWORK.consumeClick())if(mc.screen==null)mc.setScreen(new HiveNetworkScreen());
            while(NEXT.consumeClick())if(mc.screen==null){cycle(1);var s=selection();if(s!=null)mc.player.displayClientMessage(Component.literal("Selected: "+s.pos().toShortString()),true);}
            while(TRAVEL.consumeClick())if(mc.screen==null)send(HiveActionPayload.TRAVEL);
            while(DEPLOY.consumeClick())if(mc.screen==null)send(HiveActionPayload.DEPLOY);
            while(AWAKEN.consumeClick())if(mc.screen==null)send(HiveActionPayload.AWAKEN);
        }
    }
}
