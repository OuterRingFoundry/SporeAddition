package dev.sporebound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record CorruptionPayload(ResourceLocation dimension, double index, boolean sanctuary, double regionalIndex, String region) implements CustomPacketPayload {
    public static final Type<CorruptionPayload> TYPE = new Type<>(Sporebound.id("corruption"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CorruptionPayload> CODEC = new StreamCodec<>() {
        public CorruptionPayload decode(RegistryFriendlyByteBuf buf) { return new CorruptionPayload(buf.readResourceLocation(), buf.readDouble(), buf.readBoolean(),buf.readDouble(),buf.readUtf(64)); }
        public void encode(RegistryFriendlyByteBuf buf, CorruptionPayload value) { buf.writeResourceLocation(value.dimension); buf.writeDouble(value.index); buf.writeBoolean(value.sanctuary);buf.writeDouble(value.regionalIndex);buf.writeUtf(value.region,64); }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(TYPE, CODEC, (payload, context) -> context.enqueueWork(() -> ClientState.receive(payload)));
    }
    /** No client-only class references: safe when loaded by a dedicated server. */
    public static final class ClientState {
        public static CorruptionPayload current;
        public static void receive(CorruptionPayload payload) { current = payload; }
    }
}
