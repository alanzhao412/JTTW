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
 * Tells a Grand Saint of Nine Spirits which of his heads leads (0 fire, 1 thunder, 2 frost;
 * -1 for none), so the edge of his view can be tinted its colour.
 */
public class SyncLeadHeadPacket implements CustomPacketPayload {

    public static final Type<SyncLeadHeadPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_lead_head"));
    public static final StreamCodec<FriendlyByteBuf, SyncLeadHeadPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncLeadHeadPacket::decode);

    @Override
    public Type<SyncLeadHeadPacket> type() {
        return TYPE;
    }

    private final int head;

    public SyncLeadHeadPacket(int head) {
        this.head = head;
    }

    public static void encode(SyncLeadHeadPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.head);
    }

    public static SyncLeadHeadPacket decode(FriendlyByteBuf buf) {
        int head = buf.readByte();
        return new SyncLeadHeadPacket(head < -1 || head > 2 ? -1 : head);
    }

    public static void handle(SyncLeadHeadPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncLeadHeadPacket pkt) {
        com.jttw.engine.client.HeadTintOverlay.setLeadHead(pkt.head);
    }
}
