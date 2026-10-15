package com.jttw.engine.network.s2c;

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

/**
 * Syncs Eye of Heaven activation state from server to client.
 *
 * <p>When active, the client renders ore highlights (tier 2+) and
 * reveals real usernames above transformed players (tier 1+).
 */
public class SyncEyeOfHeavenPacket implements CustomPacketPayload {

    public static final Type<SyncEyeOfHeavenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_eye_of_heaven"));
    public static final StreamCodec<FriendlyByteBuf, SyncEyeOfHeavenPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncEyeOfHeavenPacket::decode);

    @Override
    public Type<SyncEyeOfHeavenPacket> type() {
        return TYPE;
    }

    private final UUID playerId;
    private final boolean active;
    private final int tier;

    public SyncEyeOfHeavenPacket(UUID playerId, boolean active, int tier) {
        this.playerId = playerId;
        this.active = active;
        this.tier = tier;
    }

    public static void encode(SyncEyeOfHeavenPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeBoolean(pkt.active);
        buf.writeByte(pkt.tier);
    }

    public static SyncEyeOfHeavenPacket decode(FriendlyByteBuf buf) {
        return new SyncEyeOfHeavenPacket(buf.readUUID(), buf.readBoolean(), buf.readByte());
    }

    public static void handle(SyncEyeOfHeavenPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncEyeOfHeavenPacket pkt) {
        com.jttw.engine.client.renderer.EyeOfHeavenRenderer.setActive(pkt.playerId, pkt.active, pkt.tier);
    }
}
