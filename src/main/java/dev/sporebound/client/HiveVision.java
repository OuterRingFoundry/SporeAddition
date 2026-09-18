package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HiveVision {
    public static boolean enabled(){var mc=Minecraft.getInstance();var p=HiveSensePayload.ClientState.current;
        return mc.player!=null&&mc.level!=null&&Hivebound.member(mc.player)&&!mc.player.isSpectator()
            &&p!=null&&p.dimension().equals(mc.level.dimension().location());}
    public static boolean marked(Entity entity){return enabled()&&(HiveSensePayload.ClientState.current.targets().stream().anyMatch(t->t.id().equals(entity.getUUID()))
        ||entity instanceof com.Harbinger.Spore.Sentities.Organoids.Proto&&HiveSensePayload.ClientState.current.sites().stream().anyMatch(s->s.mind()&&s.pos().equals(entity.blockPosition())));}
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event){event.registerAboveAll(Sporebound.id("hive_vision"),(g,delta)->{
        var mc=Minecraft.getInstance();if(!enabled()||mc.options.hideGui)return;
        var state=HiveSensePayload.ClientState.current;beacons(g);int y=46;
        var nearest=state.sites().stream().sorted(java.util.Comparator.comparingDouble(n->n.pos().distToCenterSqr(mc.player.position()))).limit(3).toList();
        for(var site:nearest){
            double dx=site.pos().getX()+0.5-mc.player.getX(),dz=site.pos().getZ()+0.5-mc.player.getZ();
            double bearing=net.minecraft.util.Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx,dz))-mc.player.getYRot());
            String direction=Math.abs(bearing)<25?"Ahead":Math.abs(bearing)>155?"Behind":bearing>0?"Right":"Left";
            String label=(site.mind()?"Hive Mind":"Spore node")+" | "+direction+" | "+(int)Math.sqrt(site.pos().distToCenterSqr(mc.player.position()))+"m | "+site.pos().toShortString();
            g.drawString(mc.font,label,8,y,0xFFD3C48B,true);y+=11;
        }
        if(!state.targets().isEmpty()){g.drawString(mc.font,"Hive marks: "+state.targets().size(),8,y,0xFFFF8989,true);y+=11;}
        for(var target:state.targets())if(target.id().equals(state.assignment())){
            g.drawString(mc.font,"Assignment: "+target.pos().toShortString()+"  ["+HiveControls.DEPLOY.getTranslatedKeyMessage().getString()+"]",8,y,0xFFFFB0D4,true);break;
        }
    });}
    /** Screen-projected beacon columns remain visible through terrain, fog and unloaded chunks. */
    public static void beacons(net.minecraft.client.gui.GuiGraphics g){
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.getMainCamera();
        var rotation=new org.joml.Quaternionf(camera.rotation()).conjugate();
        var selected=HiveControls.selection();int w=g.guiWidth(),h=g.guiHeight();
        double focal=h/(2*Math.tan(Math.toRadians(mc.options.fov().get())/2));
        var sites=HiveSensePayload.ClientState.current.sites().stream()
            .sorted(java.util.Comparator.comparingDouble((HiveSensePayload.Site n)->n.pos().distToCenterSqr(camera.getPosition())).reversed()).toList();
        for(var site:sites){
            boolean chosen=selected!=null&&selected.pos().equals(site.pos());if(!site.mind()&&!chosen)continue;
            var delta=net.minecraft.world.phys.Vec3.atCenterOf(site.pos()).subtract(camera.getPosition());
            var v=new org.joml.Vector3f((float)delta.x,(float)delta.y,(float)delta.z).rotate(rotation);
            boolean front=v.z<-.01f;
            double sx=w/2.0+(front?v.x/-v.z*focal:Math.copySign(w,v.x));
            double sy=h/2.0-(front?v.y/-v.z*focal:0);
            int x=(int)Math.clamp(sx,12,w-12),y=(int)Math.clamp(sy,28,h-36);
            int color=chosen?0xFFEFDAB0:0xFFB5DE9E;
            if(front&&sx>=12&&sx<=w-12&&sy>=28&&sy<=h-36){
                int top=Math.max(22,y-(int)Math.clamp(128*focal/-v.z,24,h*.65));
                g.fill(x-5,top,x+6,y,0x335BA857);g.fill(x-2,top,x+3,y,0x998BDC78);
                g.fill(x,top,x+1,y,0xFFF2F3C9);g.fill(x-5,y,x+6,y+2,color);
            }else {g.drawString(mc.font,sx<12?"<":sx>w-12?">":sy<28?"^":"v",x-3,y,color,true);}
            if(chosen){
                String label=(site.mind()?"Hive Mind ":"Node ")+(int)delta.length()+"m";
                int tx=Math.clamp(x-mc.font.width(label)/2,2,Math.max(2,w-mc.font.width(label)-2));
                g.drawString(mc.font,label,tx,y+4,color,true);
            }
        }
    }
}
