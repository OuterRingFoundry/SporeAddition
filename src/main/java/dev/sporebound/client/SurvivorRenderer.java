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
    @Override public void render(Survivor entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        var main=entity.getMainHandItem().isEmpty()?net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY:net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
        var off=entity.getOffhandItem().isEmpty()?net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY:net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
        if(entity.isUsingItem()){
            if(entity.getUseItem().is(net.minecraft.world.item.Items.BOW))main=net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
            else if(entity.getUseItem().is(net.minecraft.world.item.Items.SHIELD))off=net.minecraft.client.model.HumanoidModel.ArmPose.BLOCK;
        }
        boolean right=entity.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT;
        getModel().rightArmPose=right?main:off;getModel().leftArmPose=right?off:main;
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(Survivor entity){return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/"+SKINS[entity.skin()]+".png");}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(FungalContent.SURVIVOR.get(),SurvivorRenderer::new);}
}
