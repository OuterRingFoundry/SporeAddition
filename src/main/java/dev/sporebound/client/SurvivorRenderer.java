package dev.sporebound.client;
import dev.sporebound.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SurvivorRenderer extends HumanoidMobRenderer<Survivor,PlayerModel<Survivor>> {
    private static final String[] SKINS={"steve","alex","ari","efe","kai","makena","noor","sunny","zuri"};
    public SurvivorRenderer(EntityRendererProvider.Context context){super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),0.5f);
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this,context.getItemInHandRenderer()));
        addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
            new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),context.getModelManager()));
    }
    @Override public ResourceLocation getTextureLocation(Survivor entity){return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/"+SKINS[entity.skin()]+".png");}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(FungalContent.SURVIVOR.get(),SurvivorRenderer::new);}
}
