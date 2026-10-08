package com.jttw.engine.network.s2c;

import com.jttw.engine.flight.FlightProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells a player's client how they fly (speed, glide-only, boost, thrust). The client moves the
 * player, so without this it would fly everyone at one speed.
 */
public class SyncFlightPacket {

    private final FlightProfile profile;

    public SyncFlightPacket(FlightProfile profile) {
        this.profile = profile;
    }

    public static void encode(SyncFlightPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat((float) pkt.profile.speed());
        buf.writeBoolean(pkt.profile.glide());
        buf.writeFloat((float) pkt.profile.boost());
        buf.writeBoolean(pkt.profile.thrust());
    }

    public static SyncFlightPacket decode(FriendlyByteBuf buf) {
        float speed = buf.readFloat();
        boolean glide = buf.readBoolean();
        float boost = buf.readFloat();
        boolean thrust = buf.readBoolean();
        return new SyncFlightPacket(new FlightProfile(
                Float.isFinite(speed) ? Math.max(0.1, Math.min(5.0, speed)) : FlightProfile.DEFAULT.speed(),
                glide,
                Float.isFinite(boost) ? Math.min(5.0, boost) : 1.0,
                thrust));
    }

    public static void handle(SyncFlightPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncFlightPacket pkt) {
        com.jttw.engine.client.ClientFlightHandler.setProfile(pkt.profile);
    }
}
