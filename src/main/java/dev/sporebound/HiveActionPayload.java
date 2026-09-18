package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Requests only: authority, membership, destination and cooldown are checked on the server. */
public record HiveActionPayload(int action, BlockPos destination) implements CustomPacketPayload {
    public static final int TRAVEL=0, DEPLOY=1, AWAKEN=2;
    public static final Type<HiveActionPayload> TYPE=new Type<>(Sporebound.id("hive_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HiveActionPayload> CODEC=new StreamCodec<>() {
        public HiveActionPayload decode(RegistryFriendlyByteBuf b){return new HiveActionPayload(b.readVarInt(),b.readBlockPos());}
        public void encode(RegistryFriendlyByteBuf b,HiveActionPayload p){b.writeVarInt(p.action);b.writeBlockPos(p.destination);}
    };
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToServer(TYPE,CODEC,
        (p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)execute(player,p);}));}
    public static boolean execute(ServerPlayer player,HiveActionPayload request){
        if(!player.isAlive()||player.isSpectator()||request.action<0||request.action>AWAKEN)return false;
        var data=player.getPersistentData();long now=player.server.overworld().getGameTime();
        if(data.contains("sporebound:action_tick")&&now-data.getLong("sporebound:action_tick")<5)return false;
        data.putLong("sporebound:action_tick",now);
        boolean success=false;
        if(request.action==AWAKEN)success=HiveProgression.confirmAwakening(player);
        else if(Hivebound.member(player)&&HiveboundEvolution.stage(player)>=2
                &&!Protection.sterile(player.serverLevel(),player.blockPosition())) {
            var nodes=HiveNodes.get(player.serverLevel()).available(player.serverLevel());
            int chosen=-1;
            if(request.action==TRAVEL){
                for(int i=0;i<nodes.size();i++)if(nodes.get(i).pos().equals(request.destination)){chosen=i;break;}
            }else {
                var assignment=HiveNetwork.assignment(player);double nearest=Double.MAX_VALUE;
                if(assignment!=null)for(int i=0;i<nodes.size();i++){
                    double d=nodes.get(i).pos().distSqr(assignment.pos());if(d<nearest){nearest=d;chosen=i;}
                }
            }
            success=chosen>=0&&HiveNodes.travel(player,chosen);
        }
        player.displayClientMessage(Component.translatable(success?"message.sporebound.action_success":
            request.action==AWAKEN?"message.sporebound.awakening_failed":"message.sporebound.travel_failed"),true);
        return success;
    }
}
