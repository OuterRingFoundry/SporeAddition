package dev.sporebound.client;
import dev.sporebound.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
/** Uses the actual vanilla mycelium pixels, with a subtle fungal mauve top tint. */
@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MyceliumColors {
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block e){e.register((s,l,p,i)->0xD9BFCB,FungalContent.CRUST.get());}
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item e){e.register((s,i)->0xD9BFCB,FungalContent.CRUST.get());}
}
