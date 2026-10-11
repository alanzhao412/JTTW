package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Another player's Qi, for truth-seeing eyes to read (see QiShown). */
public class QiSeenPacket {

    private final int entityId;
    private final float qi, max;

    public QiSeenPacket(int entityId, float qi, float max) {
        this.entityId = entityId;
        this.qi = qi;
        this.max = max;
    }

    public static void encode(QiSeenPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeFloat(pkt.qi);
        buf.writeFloat(pkt.max);
    }

    public static QiSeenPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        float qi = buf.readFloat(), max = buf.readFloat();
        return new QiSeenPacket(id, Float.isFinite(qi) ? qi : 0, Float.isFinite(max) ? max : 0);
    }

    public static void handle(QiSeenPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(QiSeenPacket pkt) {
        com.jttw.engine.client.TruthSightHud.setQi(pkt.entityId, pkt.qi, pkt.max);
    }
}
