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
    public static boolean validationGallery;
    private static void render(GuiGraphics g) {
        var mc=Minecraft.getInstance(); var data=CorruptionPayload.ClientState.current;
        if(mc.player==null||mc.level==null||mc.options.hideGui||!ClientPreferences.HUD.get()
                ||data==null||!mc.level.dimension().location().equals(data.dimension()))return;
        bar(g,data,12,17);
        if(validationGallery) {
            g.fill(8,48,325,236,0xEE151821);
            g.drawString(mc.font,"Index: meadow to infestation",16,54,0xFFE6DDB9,true);
            double[] stages={-2,-1,0,3,6,10};
            for(int i=0;i<stages.length;i++) {
                double n=stages[i];
                bar(g,new CorruptionPayload(data.dimension(),n,false,n,"Preview"),24,80+i*27);
                g.drawString(mc.font,CorruptionMath.state(n),210,78+i*27,0xFFE6DDB9,true);
            }
        }
    }
    private static void bar(GuiGraphics g,CorruptionPayload data,int x,int y) {
        var mc=Minecraft.getInstance();int width=102;
        double pressure=data.sanctuary()?0:Math.clamp(data.index(),0,10);
        double amount=pressure/10;
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
        // The meadow dries, droops, then sheds its petals as the world becomes overrun.
        int grass=pressure<3?0xFF80AD6B:pressure<6?0xFF9A9962:pressure<8?0xFF87765C:0xFF66565A;
        int droop=(int)Math.floor(pressure*0.6);
        line(g,x-4,y+6,x-4-droop/2,y-2+droop,grass);
        line(g,x-4,y+3,x-8-droop/2,y-1+droop,grass);
        line(g,x-3,y+4,x+1-droop,y-3+droop,grass);
        flower(g,x+5,y-4,y+4,pressure,0xFFF5E9BC);
        flower(g,x+17,y-2,y+4,pressure,0xFFD8D5EC);
        if(pressure>0) {
            // Protected and normal worlds have no fungal decorations whatsoever.
            line(g,x+90,y+7,x+103,y+6,ROOT);
            if(pressure>=3) {
                line(g,x+76,y+7,x+87,y+9,ROOT);line(g,x+87,y+9,x+103,y+6,VEIN);
                mushroom(g,x+105,y+3,5);
            }
            if(pressure>=5) {
                line(g,x+89,y+8,x+93,y+12,ROOT);line(g,x+93,y+12,x+99,y+11,ROOT);
                mushroom(g,x+94,y-1,8);eye(g,x+width-5,y+2);
            }
            if(pressure>=8)eye(g,x+77,y+2);
        }
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
        return new String[]{"Normal","I","II","III","IV","V","VI","VII","VIII","IX","X"}[(int)Math.clamp(index,0,10)];
    }
    private static void flower(GuiGraphics g,int x,int y,int ground,double pressure,int petal) {
        int droop=(int)Math.floor(pressure*0.5);
        int headX=x+droop/2,headY=y+droop;
        int stem=pressure<3?0xFF568C64:pressure<7?0xFF8B855A:0xFF67545A;
        line(g,x-1,ground,x,y+2+droop/2,stem);
        line(g,x,y+2+droop/2,headX,headY+1,stem);
        if(pressure>=8) {
            // A bare seed head and fallen petals replace the blossom.
            g.fill(headX-1,headY,headX+2,headY+2,0xFF7C665F);
            g.fill(x+3,ground+2,x+5,ground+3,0xFF8C7675);
            return;
        }
        int color=pressure<3?petal:pressure<6?0xFFBDA77D:0xFF92706F;
        g.fill(headX-2,headY-1,headX+3,headY+2,INK);
        if(pressure<6)g.fill(headX-1,headY-2,headX+2,headY+3,INK);
        g.fill(headX-1,headY-1,headX+2,headY+2,color);
        g.fill(headX,headY,headX+1,headY+1,pressure<6?0xFFE6B966:0xFF69534F);
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
