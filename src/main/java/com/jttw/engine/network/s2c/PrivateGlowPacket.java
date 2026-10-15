package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/** Creatures (by entity id) that glow, through walls, for this player alone, for {@code ticks}. */
public class PrivateGlowPacket implements CustomPacketPayload {

    public static final Type<PrivateGlowPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "private_glow"));
    public static final StreamCodec<FriendlyByteBuf, PrivateGlowPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), PrivateGlowPacket::decode);

    @Override
    public Type<PrivateGlowPacket> type() {
        return TYPE;
    }

    /** The most ids one packet carries. */
    public static final int CAP = 512;

    private final int ticks;
    private final int[] entityIds;

    public PrivateGlowPacket(int ticks, int[] entityIds) {
        this.ticks = ticks;
        this.entityIds = entityIds;
    }

    public static void encode(PrivateGlowPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.ticks);
        buf.writeVarIntArray(pkt.entityIds);
    }

    public static PrivateGlowPacket decode(FriendlyByteBuf buf) {
        return new PrivateGlowPacket(buf.readVarInt(), buf.readVarIntArray(CAP));
    }

    public static void handle(PrivateGlowPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PrivateGlowPacket pkt) {
        com.jttw.engine.client.PrivateGlowClient.show(pkt.ticks, pkt.entityIds);
    }
}
