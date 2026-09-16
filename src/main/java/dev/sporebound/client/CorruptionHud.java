package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** A tiny living border: meadow on the left, watching mycelium on the right. */
@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CorruptionHud {
    private static final int INK = 0xEE211D30, ROOT = 0xFF664254, VEIN = 0xFFD27B9D;
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Sporebound.id("corruption"),(gui,delta)->render(gui));
    }
    private static void render(GuiGraphics g) {
        var mc=Minecraft.getInstance(); var data=CorruptionPayload.ClientState.current;
        if(mc.player==null||mc.level==null||mc.options.hideGui||!ClientPreferences.HUD.get()
                ||data==null||!mc.level.dimension().location().equals(data.dimension()))return;
        int x=12,y=17,width=102;
        double amount=data.sanctuary()?0:Math.clamp(data.index()/10,0,1);
        // Continuous jade channel. Infection advances from the fungal end into the meadow.
        g.fill(x-1,y-2,x+width+1,y+7,INK);
        g.fill(x,y-1,x+width,y+6,0xFF526252);
        g.fill(x,y,x+width,y+5,0xFF568C64);
        g.fill(x,y,x+width,y+1,0xFFA3C786);
        g.fill(x,y+4,x+width,y+5,0xFF355744);
        int front=x+width-(int)Math.round(width*amount);
        if(front<x+width) {
            g.fill(front,y,x+width,y+5,0xFF874565);
            g.fill(front,y,x+width,y+1,0xFFDC98B0);
            g.fill(front,y+4,x+width,y+5,0xFF503345);
            // Intertwined capillaries remain inside the filled section, not separate segments.
            for(int i=front;i<x+width;i++) {
                int v=(int)Math.round(Math.sin((i-x)*0.24)*1.4);
                g.fill(i,y+2+v,i+1,y+3+v,VEIN);
            }
            g.fill(front,y-1,front+1,y+6,0xFFF1D7B1);
        }
        line(g,x-4,y+6,x-4,y-2,0xFF80AD6B);
        line(g,x-4,y+3,x-8,y-1,0xFF568C64);
        line(g,x-3,y+4,x+1,y-3,0xFFA3C786);
        flower(g,x+5,y-4,0xFFF5E9BC); flower(g,x+17,y-2,0xFFD8D5EC);
        // Roots curl beneath the frame and branch into the mushroom crown.
        line(g,x+76,y+7,x+87,y+9,ROOT);line(g,x+87,y+9,x+103,y+6,VEIN);
        line(g,x+89,y+8,x+93,y+12,ROOT);line(g,x+93,y+12,x+99,y+11,ROOT);
        mushroom(g,x+94,y-1,8); mushroom(g,x+105,y+3,5);
        eye(g,x+width-5,y+2);
        if(!data.sanctuary()&&data.regionalIndex()>=8)eye(g,x+77,y+2);
        // Regional pressure notch shares the same direction as the corruption fill.
        int local=x+width-(int)Math.round(width*Math.clamp(data.regionalIndex()/10,0,1));
        g.fill(local,y+6,local+1,y+8,0xFFE6DDB9);
        g.drawString(mc.font, roman(data.sanctuary()?0:data.index()), x+width+12, y-2, 0xFFE6DDB9, true);
        if(mc.getDebugOverlay().showDebugScreen())g.drawString(mc.font,
            String.format(java.util.Locale.ROOT,"World %.2f  Local %.2f  %s",data.index(),data.regionalIndex(),
                data.sanctuary()?"Mushroom sanctuary":data.region()),8,35,0xFFE6DDB9,true);
    }
    public static String roman(double index) {
        if(index<0)return index==-2?"Purged":"Dormant";
        return new String[]{"0","I","II","III","IV","V","VI","VII","VIII","IX","X"}[(int)Math.clamp(index,0,10)];
    }
    private static void flower(GuiGraphics g,int x,int y,int petal) {
        line(g,x,y+1,x-1,21,0xFF568C64);
        g.fill(x-2,y-1,x+3,y+2,INK);g.fill(x-1,y-2,x+2,y+3,INK);
        g.fill(x-1,y-1,x+2,y+2,petal);g.fill(x,y,x+1,y+1,0xFFE6B966);
    }
    private static void mushroom(GuiGraphics g,int x,int y,int width) {
        g.fill(x-1,y-3,x+2,y+4,INK);g.fill(x,y-2,x+1,y+3,0xFFE0B7AF);
        g.fill(x-width/2-1,y-5,x+width/2+2,y-2,INK);
        g.fill(x-width/2,y-5,x+width/2+1,y-3,0xFFB05E88);
        g.fill(x-2,y-7,x+3,y-5,INK);g.fill(x-1,y-6,x+2,y-4,0xFFDB93AE);
        g.fill(x+2,y-5,x+3,y-4,0xFFFFDEBD);
    }
    private static void eye(GuiGraphics g,int x,int y) {
        g.fill(x-4,y-1,x+5,y+2,INK);g.fill(x-2,y-2,x+3,y+3,INK);
        g.fill(x-3,y,x+4,y+1,0xFFF2D8AE);g.fill(x-1,y-1,x+2,y+2,0xFFF2D8AE);
        g.fill(x,y-1,x+1,y+2,0xFF5C284C);
    }
    private static void line(GuiGraphics g,int x0,int y0,int x1,int y1,int color) {
        int steps=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));
        for(int i=0;i<=steps;i++) {
            int x=x0+(int)Math.round((x1-x0)*i/(double)Math.max(1,steps));
            int y=y0+(int)Math.round((y1-y0)*i/(double)Math.max(1,steps));
            g.fill(x,y,x+1,y+1,color);
        }
    }
}
