package dev.sporebound.client;
import dev.sporebound.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid=Sporebound.ID,value=Dist.CLIENT)
public final class ClientLifecycle {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){HiveControls.reset();CorruptionPayload.ClientState.current=null;EvolutionPayload.ClientState.current=null;HiveSensePayload.ClientState.current=null;InfusionPayload.ClientState.stages.clear();}
}
