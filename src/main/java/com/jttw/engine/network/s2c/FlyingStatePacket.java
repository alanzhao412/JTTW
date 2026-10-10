package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the players who can see someone whether that someone is flying. Vanilla keeps a player's
 * flying to the player themself, so without this nobody else could draw them lying along their
 * flight on their cloud.
 */
public class FlyingStatePacket {

    private final int entityId;
    private final boolean flying;

    public FlyingStatePacket(int entityId, boolean flying) {
        this.entityId = entityId;
        this.flying = flying;
    }

    public static void encode(FlyingStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeBoolean(pkt.flying);
    }

    public static FlyingStatePacket decode(FriendlyByteBuf buf) {
        return new FlyingStatePacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(FlyingStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(FlyingStatePacket pkt) {
        com.jttw.engine.client.flight.FlightPose.setFlying(pkt.entityId, pkt.flying);
    }
}
