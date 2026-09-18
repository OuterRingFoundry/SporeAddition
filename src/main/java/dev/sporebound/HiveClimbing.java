package dev.sporebound;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
public final class HiveClimbing {
    public static boolean eligible(Player player){
        if(!player.isAlive()||player.isSpectator()||!Hivebound.member(player)||player.isPassenger()
            ||player.getAbilities().flying||player.isFallFlying()||player.isInWaterOrBubble()||player.isInLava())return false;
        if(player.level() instanceof ServerLevel level)
            return HiveboundEvolution.stage(player)>=1&&!Protection.sterile(level,player.blockPosition());
        var evo=EvolutionPayload.ClientState.current;var corruption=CorruptionPayload.ClientState.current;
        return evo!=null&&evo.player().equals(player.getUUID())&&EvolutionMath.stage(evo.points(),evo.first(),evo.hyper())>=1
            &&corruption!=null&&corruption.dimension().equals(player.level().dimension().location())
            &&corruption.index()>=0&&!corruption.sanctuary();
    }
}
