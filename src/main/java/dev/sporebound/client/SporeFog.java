package dev.sporebound.client;

import dev.sporebound.CorruptionMath;
import dev.sporebound.CorruptionPayload;
import dev.sporebound.Sporebound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Pressure-driven spore haze. Never replaces water, lava, blindness or darkness fog. */
@EventBusSubscriber(modid=Sporebound.ID,value=Dist.CLIENT)
public final class SporeFog {
    private static float density;
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private static boolean active() {
        var mc=Minecraft.getInstance(); var data=CorruptionPayload.ClientState.current;
        return mc.level!=null&&mc.player!=null&&mc.level.dimension().equals(Sporebound.BLIGHT)
            &&data!=null&&data.dimension().equals(mc.level.dimension().location())&&!data.sanctuary();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.level!=lastLevel){lastLevel=mc.level;density=0;}
        if(!active()){density=0;return;}
        if(mc.isPaused())return;
        float target=(float)CorruptionMath.fogDensity(CorruptionPayload.ClientState.current.regionalIndex());
        density+=(target-density)*0.06F;
        if(density<0.005F||!mc.level.canSeeSky(mc.player.blockPosition())||mc.player.isUnderWater())return;
        var random=mc.level.random;
        if(random.nextFloat()>density*0.65F)return;
        var pos=mc.player.position();
        double x=pos.x+(random.nextDouble()-0.5)*24;
        double y=pos.y+random.nextDouble()*7;
        double z=pos.z+(random.nextDouble()-0.5)*24;
        if(mc.level.getBlockState(net.minecraft.core.BlockPos.containing(x,y,z)).isAir())
            mc.level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,x,y,z,0,0,0);
    }
    private static boolean visible(ViewportEvent event) {
        if(!active()||density<0.005F||event.getCamera().getFluidInCamera()!=FogType.NONE)return false;
        return !(event.getCamera().getEntity() instanceof LivingEntity entity
            &&(entity.hasEffect(MobEffects.BLINDNESS)||entity.hasEffect(MobEffects.DARKNESS)));
    }
    @SubscribeEvent public static void distance(ViewportEvent.RenderFog event) {
        if(event.getType()!=FogType.NONE||!visible(event))return;
        float far=Math.min(event.getFarPlaneDistance(),192-152*density);
        float near=event.getMode()==FogRenderer.FogMode.FOG_SKY?0:Math.min(event.getNearPlaneDistance(),48-44*density);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Math.min(near,far-1));
        event.setCanceled(true);
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event) {
        if(!visible(event))return;
        float blend=density*0.72F;
        event.setRed(event.getRed()*(1-blend)+0.43F*blend);
        event.setGreen(event.getGreen()*(1-blend)+0.46F*blend);
        event.setBlue(event.getBlue()*(1-blend)+0.32F*blend);
    }
}
