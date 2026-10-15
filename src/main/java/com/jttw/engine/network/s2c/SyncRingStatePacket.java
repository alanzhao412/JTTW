package com.jttw.engine.network.s2c;

import com.jttw.engine.client.ClientRingVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncRingStatePacket implements CustomPacketPayload {

    public static final Type<SyncRingStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_ring_state"));
    public static final StreamCodec<FriendlyByteBuf, SyncRingStatePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncRingStatePacket::decode);

    @Override
    public Type<SyncRingStatePacket> type() {
        return TYPE;
    }
    private final UUID playerId;
    private final boolean active;

    public SyncRingStatePacket(UUID playerId, boolean active) {
        this.playerId = playerId;
        this.active = active;
    }

    public static void encode(SyncRingStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeBoolean(pkt.active);
    }

    public static SyncRingStatePacket decode(FriendlyByteBuf buf) {
        return new SyncRingStatePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(SyncRingStatePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncRingStatePacket pkt) {
        if (Minecraft.getInstance().level == null) return;
        ClientRingVisuals.setActive(pkt.playerId, pkt.active);
    }
}