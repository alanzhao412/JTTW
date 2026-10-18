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

/**
 * Tells the players who can see someone whether that someone is flying, and whether on a cloud. Vanilla keeps a player's
 * flying to the player themself, so without this nobody else could draw them lying along their
 * flight on their cloud.
 */
public class FlyingStatePacket implements CustomPacketPayload {

    public static final Type<FlyingStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "flying_state"));
    public static final StreamCodec<FriendlyByteBuf, FlyingStatePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), FlyingStatePacket::decode);

    @Override
    public Type<FlyingStatePacket> type() {
        return TYPE;
    }

    private final int entityId;
    /** {@link com.jttw.engine.flight.FlyingState#GROUNDED}, {@code FLYING}, {@code ON_CLOUD}, {@code IN_SQUALL} or {@code ON_HEAVENLY_CLOUD}. */
    private final int mode;

    public FlyingStatePacket(int entityId, int mode) {
        this.entityId = entityId;
        this.mode = mode;
    }

    public static void encode(FlyingStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeByte(pkt.mode);
    }

    public static FlyingStatePacket decode(FriendlyByteBuf buf) {
        return new FlyingStatePacket(buf.readVarInt(), Math.max(0, Math.min(com.jttw.engine.flight.FlyingState.ON_HEAVENLY_CLOUD, buf.readByte())));
    }

    public static void handle(FlyingStatePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(FlyingStatePacket pkt) {
        com.jttw.engine.client.flight.FlightPose.setMode(pkt.entityId, pkt.mode);
    }
}
