package com.jttw.engine.network.s2c;

import com.jttw.engine.JTTWEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → Client: the Fiery Golden Eyes' chosen output (1-5), for the HUD by slot N. */
public record SyncEyeLaserOutputPacket(int output) implements CustomPacketPayload {

    public static final Type<SyncEyeLaserOutputPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_eye_laser_output"));
    public static final StreamCodec<FriendlyByteBuf, SyncEyeLaserOutputPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeByte(pkt.output), buf -> new SyncEyeLaserOutputPacket(buf.readByte()));

    @Override
    public Type<SyncEyeLaserOutputPacket> type() {
        return TYPE;
    }

    public static void handle(SyncEyeLaserOutputPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncEyeLaserOutputPacket pkt) {
        com.jttw.engine.client.ClientAbilityData.setEyeLaserOutput(pkt.output);
    }
}
