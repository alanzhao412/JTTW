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
 * Syncs Fiery Golden Eyes activation state from server to client.
 *
 * <p>When active, the client renders ore highlights (x-ray mode)
 * and entity info overlays (passive mode).
 *
 * <p>Mode:
 * <ul>
 *   <li>0 = off</li>
 *   <li>1 = passive only (entity info, always on when unlocked)</li>
 *   <li>2 = xray mode active (ore highlights + entity info)</li>
 * </ul>
 */
public class SyncGoldenEyesPacket implements CustomPacketPayload {

    public static final Type<SyncGoldenEyesPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_golden_eyes"));
    public static final StreamCodec<FriendlyByteBuf, SyncGoldenEyesPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncGoldenEyesPacket::decode);

    @Override
    public Type<SyncGoldenEyesPacket> type() {
        return TYPE;
    }

    private final UUID playerId;
    private final int mode; // 0=off, 1=passive, 2=xray

    public SyncGoldenEyesPacket(UUID playerId, int mode) {
        this.playerId = playerId;
        this.mode = mode;
    }

    public static void encode(SyncGoldenEyesPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeByte(pkt.mode);
    }

    public static SyncGoldenEyesPacket decode(FriendlyByteBuf buf) {
        return new SyncGoldenEyesPacket(buf.readUUID(), buf.readByte());
    }

    public static void handle(SyncGoldenEyesPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncGoldenEyesPacket pkt) {
        com.jttw.engine.client.renderer.GoldenEyesRenderer.setMode(pkt.playerId, pkt.mode);
    }
}
