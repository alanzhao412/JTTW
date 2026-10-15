package com.jttw.engine.network.s2c;

import com.jttw.engine.flight.FlightProfile;
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
 * Tells a player's client how they fly (speed, glide-only, boost, thrust). The client moves the
 * player, so without this it would fly everyone at one speed.
 */
public class SyncFlightPacket implements CustomPacketPayload {

    public static final Type<SyncFlightPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_flight"));
    public static final StreamCodec<FriendlyByteBuf, SyncFlightPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncFlightPacket::decode);

    @Override
    public Type<SyncFlightPacket> type() {
        return TYPE;
    }

    private final FlightProfile profile;

    public SyncFlightPacket(FlightProfile profile) {
        this.profile = profile;
    }

    public static void encode(SyncFlightPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat((float) pkt.profile.speed());
        buf.writeBoolean(pkt.profile.glide());
        buf.writeFloat((float) pkt.profile.boost());
        buf.writeBoolean(pkt.profile.thrust());
        buf.writeBoolean(pkt.profile.own());
    }

    public static SyncFlightPacket decode(FriendlyByteBuf buf) {
        float speed = buf.readFloat();
        boolean glide = buf.readBoolean();
        float boost = buf.readFloat();
        boolean thrust = buf.readBoolean();
        boolean own = buf.readBoolean();
        return new SyncFlightPacket(new FlightProfile(
                Float.isFinite(speed) ? Math.max(0.1, Math.min(5.0, speed)) : FlightProfile.DEFAULT.speed(),
                glide,
                Float.isFinite(boost) ? Math.min(5.0, boost) : 1.0,
                thrust, own));
    }

    public static void handle(SyncFlightPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncFlightPacket pkt) {
        com.jttw.engine.client.ClientFlightHandler.setProfile(pkt.profile);
        com.jttw.engine.flight.FlyingState.localOwnFlight = pkt.profile.own();
    }
}
