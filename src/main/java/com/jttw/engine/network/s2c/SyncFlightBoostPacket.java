package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells a flier's client how far their flight is boosted (1 = cruising) and whether they are
 * beating their wings, which drives them forward even with no key held. The client moves the
 * player, so the boost has to reach it.
 */
public class SyncFlightBoostPacket {

    private final float boost;
    private final boolean thrust;

    public SyncFlightBoostPacket(float boost, boolean thrust) {
        this.boost = boost;
        this.thrust = thrust;
    }

    public static void encode(SyncFlightBoostPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.boost);
        buf.writeBoolean(pkt.thrust);
    }

    public static SyncFlightBoostPacket decode(FriendlyByteBuf buf) {
        float boost = buf.readFloat();
        boolean thrust = buf.readBoolean();
        return new SyncFlightBoostPacket(Float.isFinite(boost) ? Math.max(1f, Math.min(5f, boost)) : 1f, thrust);
    }

    public static void handle(SyncFlightBoostPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncFlightBoostPacket pkt) {
        com.jttw.engine.client.ClientFlightHandler.setBoost(pkt.boost, pkt.thrust);
    }
}
