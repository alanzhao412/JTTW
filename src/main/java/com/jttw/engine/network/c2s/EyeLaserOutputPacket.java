package com.jttw.engine.network.c2s;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.EyeLaser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → Server: the mouse wheel turned while N is held: the Fiery Golden Eyes' output up (+1) or down (-1). */
public record EyeLaserOutputPacket(int delta) implements CustomPacketPayload {

    public static final Type<EyeLaserOutputPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "eye_laser_output"));
    public static final StreamCodec<FriendlyByteBuf, EyeLaserOutputPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeByte(pkt.delta), buf -> new EyeLaserOutputPacket(buf.readByte()));

    @Override
    public Type<EyeLaserOutputPacket> type() {
        return TYPE;
    }

    public static void handle(EyeLaserOutputPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) EyeLaser.changeOutput(player, pkt.delta);
        });
    }
}
