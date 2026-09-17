package dev.sporebound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sporebound.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Small shoulder/back fungal growths follow the player's skin model and vanish on release. */
@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusionLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public InfusionLayer(PlayerRenderer renderer){super(renderer);}
    @SubscribeEvent public static void attach(EntityRenderersEvent.AddLayers event){
        for(var skin:event.getSkins()){var renderer=event.getSkin(skin);if(renderer instanceof PlayerRenderer player)player.addLayer(new InfusionLayer(player));}
    }
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,
        float swing,float amount,float partial,float age,float yaw,float pitch){
        int stage=InfusionPayload.ClientState.stages.getOrDefault(player.getUUID(),0);
        if(stage<1||!Hivebound.member(player)||player.isInvisible())return;
        pose.pushPose();getParentModel().body.translateAndRotate(pose);
        bud(pose,buffers,light,-0.35,-0.15,0.10,0.32f);
        bud(pose,buffers,light,0.15,0.30,0.13,0.24f);
        if(stage>=2)bud(pose,buffers,light,0.1,-0.23,0.10,0.42f);
        pose.popPose();
    }
    private static void bud(PoseStack pose,MultiBufferSource buffers,int light,double x,double y,double z,float size){
        pose.pushPose();pose.translate(x,y,z);pose.scale(size,-size,size);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(com.Harbinger.Spore.core.Sblocks.FUNGAL_STEM_TOP.get().defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
