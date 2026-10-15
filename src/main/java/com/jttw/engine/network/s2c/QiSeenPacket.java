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

/** Another player's Qi, for truth-seeing eyes to read (see QiShown). */
public class QiSeenPacket implements CustomPacketPayload {

    public static final Type<QiSeenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "qi_seen"));
    public static final StreamCodec<FriendlyByteBuf, QiSeenPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), QiSeenPacket::decode);

    @Override
    public Type<QiSeenPacket> type() {
        return TYPE;
    }

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

    public static void handle(QiSeenPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(QiSeenPacket pkt) {
        com.jttw.engine.client.TruthSightHud.setQi(pkt.entityId, pkt.qi, pkt.max);
    }
}
