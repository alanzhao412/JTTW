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
 * Tells the Marshal of the Heavenly Canopy how full his second stomach is (0-20), or -1 if he
 * has none, so it can be drawn as a second food row.
 */
public class SyncStomachPacket implements CustomPacketPayload {

    public static final Type<SyncStomachPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_stomach"));
    public static final StreamCodec<FriendlyByteBuf, SyncStomachPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncStomachPacket::decode);

    @Override
    public Type<SyncStomachPacket> type() {
        return TYPE;
    }

    private final int reserve;

    public SyncStomachPacket(int reserve) {
        this.reserve = reserve;
    }

    public static void encode(SyncStomachPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.reserve);
    }

    public static SyncStomachPacket decode(FriendlyByteBuf buf) {
        int reserve = buf.readByte();
        return new SyncStomachPacket(reserve < -1 || reserve > 20 ? -1 : reserve);
    }

    public static void handle(SyncStomachPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncStomachPacket pkt) {
        com.jttw.engine.client.StomachOverlay.setReserve(pkt.reserve);
    }
}
