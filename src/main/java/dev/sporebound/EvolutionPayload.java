package dev.sporebound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

public record EvolutionPayload(UUID player,int points,int kills,int first,int hyper) implements CustomPacketPayload {
    public static final Type<EvolutionPayload> TYPE=new Type<>(Sporebound.id("evolution"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EvolutionPayload> CODEC=new StreamCodec<>(){
        public EvolutionPayload decode(RegistryFriendlyByteBuf b){return new EvolutionPayload(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,EvolutionPayload p){b.writeUUID(p.player);b.writeVarInt(p.points);b.writeVarInt(p.kills);b.writeVarInt(p.first);b.writeVarInt(p.hyper);}
    };
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(TYPE,CODEC,
        (payload,context)->context.enqueueWork(()->ClientState.current=payload));}
    public static void sync(ServerPlayer player){PacketDistributor.sendToPlayer(player,new EvolutionPayload(player.getUUID(),
        HiveboundEvolution.points(player),HiveboundEvolution.kills(player),HiveboundEvolution.first(),HiveboundEvolution.hyper()));}
    public static final class ClientState {public static EvolutionPayload current;}
}
