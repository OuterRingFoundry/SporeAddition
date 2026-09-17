package dev.sporebound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;

public record InfusionPayload(UUID player,int stage) implements CustomPacketPayload {
    public static final Type<InfusionPayload> TYPE=new Type<>(Sporebound.id("infusion"));
    public static final StreamCodec<RegistryFriendlyByteBuf,InfusionPayload> CODEC=new StreamCodec<>() {
        public InfusionPayload decode(RegistryFriendlyByteBuf b){return new InfusionPayload(b.readUUID(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,InfusionPayload p){b.writeUUID(p.player);b.writeVarInt(p.stage);}
    };
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,
        (p,c)->c.enqueueWork(()->{if(p.stage==0)ClientState.stages.remove(p.player);else ClientState.stages.put(p.player,p.stage);}));}
    public static void sync(ServerPlayer p){PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,
        new InfusionPayload(p.getUUID(),Hivebound.member(p)?HiveboundEvolution.stage(p):0));}
    public static final class ClientState { public static final Map<UUID,Integer> stages=new HashMap<>(); }
}
