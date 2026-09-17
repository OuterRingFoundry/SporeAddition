package dev.sporebound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sporebound.FungalContent;
import dev.sporebound.InfectedBiomass;
import dev.sporebound.Sporebound;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BiomassRenderer extends MobRenderer<InfectedBiomass, BiomassRenderer.Model> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(Sporebound.id("infected_biomass"), "main");
    private static final ResourceLocation TEXTURE = Sporebound.id("textures/entity/infected_biomass.png");
    public BiomassRenderer(EntityRendererProvider.Context context) { super(context, new Model(context.bakeLayer(LAYER)), 0.45F); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(FungalContent.BIOMASS.get(), BiomassRenderer::new); }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(LAYER, Model::layer); }
    @Override public ResourceLocation getTextureLocation(InfectedBiomass entity) { return TEXTURE; }
    @Override protected void scale(InfectedBiomass entity, PoseStack pose, float partial) {
        float size = entity.massScale();
        float pulse = (float)Math.sin((entity.tickCount + partial) * (entity.feeding() ? 0.5 : 0.18)) * (entity.feeding() ? 0.10F : 0.055F);
        float absorb = entity.absorbing() ? Math.max(0.12F, 1 - (entity.absorptionTicks() + partial) / InfectedBiomass.ABSORB_TICKS) : 1;
        pose.scale(size * (1 + pulse) * absorb, size * (1 - pulse * 1.8F) * absorb, size * (1 - pulse) * absorb);
    }
    public static final class Model extends HierarchicalModel<InfectedBiomass> {
        private final ModelPart root, front, rear;
        Model(ModelPart root) { this.root = root; front = root.getChild("front"); rear = root.getChild("rear"); }
        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition(); PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0,0).addBox(-6,-5,-7,12,5,9), PartPose.offset(0,24,0));
            root.addOrReplaceChild("rear", CubeListBuilder.create().texOffs(0,20).addBox(-5,-4,0,10,4,8), PartPose.offset(0,24,0));
            root.addOrReplaceChild("fold", CubeListBuilder.create().texOffs(0,34).addBox(-4,-7,-4,7,3,8), PartPose.offset(0,24,0));
            PartDefinition stem = root.addOrReplaceChild("fungus", CubeListBuilder.create().texOffs(42,0).addBox(-1,-10,0,2,5,2), PartPose.offset(2,24,0));
            stem.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(40,10).addBox(-3,-11,-2,6,2,6), PartPose.ZERO);
            root.addOrReplaceChild("buds", CubeListBuilder.create().texOffs(40,22).addBox(-6,-7,3,3,3,3).addBox(3,-6,-5,3,2,3), PartPose.offset(0,24,0));
            return LayerDefinition.create(mesh,64,64);
        }
        @Override public ModelPart root() { return root; }
        @Override public void setupAnim(InfectedBiomass entity,float swing,float amount,float age,float yaw,float pitch) {
            front.xScale = 1 + (float)Math.sin(age * 0.25) * 0.10F;
            rear.zScale = 1 + (float)Math.sin(age * 0.25 + 2) * 0.13F;
            front.z = (float)Math.sin(swing) * amount;
            rear.xRot = (float)Math.sin(age * 0.13) * 0.06F;
        }
    }
}
