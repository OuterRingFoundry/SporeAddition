package dev.sporebound.client;

import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Optional Civillis notification adapter; never modifies civilization scores or spawn policy. */
@EventBusSubscriber(modid=Sporebound.ID,value=Dist.CLIENT)
public final class CivilisBridge {
    private static long epoch=Long.MIN_VALUE;
    private static int stateId=1,ticks;
    private static Component nativeLabel=Component.empty();
    private static String lastKey="";
    private static boolean resending,displayed,pending,unavailable;
    public static void observe(Object payload) {
        if(resending)return;
        try {
            epoch=(long)payload.getClass().getMethod("epoch").invoke(payload);
            stateId=(int)payload.getClass().getMethod("stateId").invoke(payload);
        }catch(ReflectiveOperationException error){disable(error);}
    }
    public static String territory(CorruptionPayload data) {
        if(data==null)return "";
        if(data.sanctuary())return "Mushroom sanctuary";
        if(data.index()==-2)return "Spore-purged territory";
        if(data.index()==-1)return ""; // Preserve ordinary Civillis labels in dormant dimensions.
        if(data.regionalIndex()>=8)return "Overrun territory";
        if(data.regionalIndex()>=5)return "Corrupted territory";
        if(data.region().equals("Remnant Grove"))return "Uncorrupted remnant";
        if(data.regionalIndex()>0)return "Spore-threatened territory";
        return "Contained territory";
    }
    public static Component decorate(Component original) {
        if(!resending)nativeLabel=original.copy();
        displayed=true;
        var data=CorruptionPayload.ClientState.current;var mc=Minecraft.getInstance();
        if(data==null||mc.level==null||!data.dimension().equals(mc.level.dimension().location()))return original;
        String label=territory(data);
        if(label.isEmpty())return original;
        return Component.literal(label).append(original.getString().isBlank()?Component.empty():Component.literal(" — ").append(original));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!ModList.get().isLoaded("civil")||unavailable)return;
        var mc=Minecraft.getInstance();var data=CorruptionPayload.ClientState.current;
        if(mc.level==null){lastKey="";epoch=Long.MIN_VALUE;nativeLabel=Component.empty();pending=false;return;}
        if(data==null||!data.dimension().equals(mc.level.dimension().location()))return;
        String key=data.dimension()+"/"+territory(data);
        if(!key.equals(lastKey)){lastKey=key;pending=true;}
        if(!pending||++ticks%20!=0)return;
        // Replay the last native epoch, never invent a newer one that could suppress future Civillis events.
        // Native onPayload retains its enabled flag, cooldown, position and font preferences.
        try {
            var type=Class.forName("civil.civilization.ZoneTransitionPayload");
            var payload=type.getConstructor(long.class,int.class,String.class).newInstance(epoch,stateId,nativeLabel.getString());
            resending=true;displayed=false;
            Class.forName("civil.civilization.ZoneTransitionHud").getMethod("onPayload",type).invoke(null,payload);
            if(displayed)pending=false;
        }catch(ReflectiveOperationException error){disable(error);}finally{resending=false;}
    }
    private static void disable(Exception error) {
        unavailable=true;org.slf4j.LoggerFactory.getLogger("Sporebound").warn("Civillis territory notifications unavailable for this version",error);
    }
}
