package dev.sporebound;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Ten seconds in high-pressure outdoor air causes brief, non-stacking Weakness I. */
public final class SporeExposure {
    private static final String EXPOSURE = "sporebound:fog_exposure_seconds";
    private SporeExposure() {}
    public static boolean hazardous(ServerPlayer player) {
        return player.level().dimension().equals(Sporebound.BLIGHT)
            &&CorruptionMath.hazardousFog(RegionalCorruption.at(player.serverLevel(),player.blockPosition()))
            &&!player.isCreative()&&!player.isSpectator()&&player.isAlive()
            &&!player.isUnderWater()&&!player.isInLava()
            &&player.level().canSeeSky(player.blockPosition().above());
    }
    /** Called once per second on the server, independently of the optional HUD. */
    public static void tick(ServerPlayer player) {
        var data=player.getPersistentData();
        if(!hazardous(player)){data.remove(EXPOSURE);return;}
        int seconds=Math.min(10,data.getInt(EXPOSURE)+1);
        data.putInt(EXPOSURE,seconds);
        if(seconds>=10)player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,0,true,true,true));
    }
}
