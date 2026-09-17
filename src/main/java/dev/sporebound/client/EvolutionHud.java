package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** A fungal evolution meter beside vanilla experience, independent of the world Index bar. */
@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EvolutionHud {
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event){event.registerAboveAll(Sporebound.id("evolution"),(gui,delta)->render(gui));}
    private static void render(GuiGraphics g){
        var mc=Minecraft.getInstance();var data=EvolutionPayload.ClientState.current;
        if(mc.player==null||mc.level==null||mc.options.hideGui||mc.player.isSpectator()||!ClientPreferences.HUD.get()
            ||!Hivebound.member(mc.player)||data==null||!mc.player.getUUID().equals(data.player()))return;
        int width=88,x=g.guiWidth()/2+98,y=g.guiHeight()-29;
        // Narrow windows keep the meter visible above the hotbar instead of clipping it.
        if(x+width+6>g.guiWidth()){x=g.guiWidth()/2-width/2;y-=28;}
        int stage=EvolutionMath.stage(data.points(),data.first(),data.hyper());
        int fill=(int)Math.round(width*EvolutionMath.progress(data.points(),data.first(),data.hyper()));
        g.fill(x-1,y-2,x+width+1,y+6,0xEE211D30);g.fill(x,y-1,x+width,y+5,0xFF47394B);
        if(fill>0){g.fill(x,y,x+fill,y+4,0xFFAD638E);g.fill(x,y,x+fill,y+1,0xFFE6A5BB);}
        boolean fungal=CorruptionPayload.ClientState.current!=null&&CorruptionPayload.ClientState.current.index()>0;
        if(fungal){
            for(int i=0;i<fill;i+=4)g.fill(x+i,y+2+(i/4)%2,x+i+2,y+3+(i/4)%2,0xFFEBB1C1);
            g.fill(x-3,y+2,x-1,y+8,0xFF8D5A78);g.fill(x-5,y+7,x,y+8,0xFF8D5A78);
            g.fill(x+width,y-3,x+width+2,y+4,0xFF8D5A78);
        }
        String count=stage>=2?Integer.toString(data.points()):data.points()+"/"+(stage==0?data.first():data.hyper());
        g.drawString(mc.font,EvolutionMath.name(stage)+" "+count,x,y-12,0xFFEACFD8,true);
        g.drawString(mc.font,"Kill points "+data.kills(),x,y+8,0xFFCBAFBE,true);
    }
}
