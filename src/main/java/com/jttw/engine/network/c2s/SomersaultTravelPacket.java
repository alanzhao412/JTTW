package com.jttw.engine.network.c2s;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.SomersaultTravel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the Fighting Buddha chose where to somersault to: a menu entry's id, or
 * {@link SomersaultTravel#COORDS} with typed x, y, z and a realm. The server checks he may.
 */
public record SomersaultTravelPacket(String id, int x, int y, int z, String dim) implements CustomPacketPayload {

    public static final Type<SomersaultTravelPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "somersault_travel"));
    public static final StreamCodec<FriendlyByteBuf, SomersaultTravelPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.id); buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z); buf.writeUtf(p.dim); },
            buf -> new SomersaultTravelPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf()));

    @Override
    public Type<SomersaultTravelPacket> type() {
        return TYPE;
    }

    public static void handle(SomersaultTravelPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) SomersaultTravel.travel(p, pkt.id, pkt.x, pkt.y, pkt.z, pkt.dim);
        });
    }
}
