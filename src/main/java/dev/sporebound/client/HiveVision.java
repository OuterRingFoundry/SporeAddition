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
        var state=HiveSensePayload.ClientState.current;int y=46;
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
            g.drawString(mc.font,"Assignment: "+target.pos().toShortString()+"  /hive deploy",8,y,0xFFFFB0D4,true);break;
        }
    });}
}
