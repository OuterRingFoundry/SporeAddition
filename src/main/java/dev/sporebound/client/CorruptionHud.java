package dev.sporebound.client;

import java.util.Locale;
import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CorruptionHud {
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Sporebound.id("corruption"),(gui,delta)->render(gui));
    }
    private static void render(GuiGraphics g) {
        var mc=Minecraft.getInstance(); var data=CorruptionPayload.ClientState.current;
        if(mc.player==null||mc.level==null||mc.options.hideGui||data==null||!mc.level.dimension().location().equals(data.dimension()))return;
        double index=data.index();
        int color=data.sanctuary()?0xFF81DBC3:index<0?0xFF77BAC6:index<5?0xFFB4C96A:index<8?0xFFDAAB68:0xFFDF708A;
        g.fill(7,7,237,72,0xD9181920); g.fill(7,7,9,72,color); g.fill(9,71,237,72,0xFF484554);
        // Original pixel geometry: fungal cap, gills, stem and branching mycelium.
        g.fill(15,21,37,25,color);g.fill(18,17,34,21,color);g.fill(22,14,30,17,color);
        g.fill(17,25,35,27,0xFF777186);g.fill(24,26,28,39,color);
        g.fill(19,38,32,40,color);g.fill(16,40,21,42,color);g.fill(31,40,36,42,color);
        g.fill(21,20,23,22,0xFFF1EACF);g.fill(29,18,31,20,0xFFF1EACF);
        String label=data.sanctuary()?"MUSHROOM SANCTUARY":CorruptionMath.state(index);
        g.drawString(mc.font,label,44,12,color,false);
        String value=String.format(Locale.ROOT,"World %.2f / 10",index);
        g.drawString(mc.font,value,44,25,0xFFE9E3D6,false);
        g.drawString(mc.font,String.format(Locale.ROOT,"Local %.2f | %s",data.regionalIndex(),data.region()),14,56,0xFFB6B5C5,false);
        for(int i=0;i<10;i++) {
            int x=44+i*15;g.fill(x,39,x+13,44,0xFF36343F);
            double amount=Math.clamp(index-i,0,1); if(amount>0)g.fill(x,39,x+(int)Math.ceil(13*amount),44,color);
        }
    }
}
