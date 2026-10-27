package com.jttw.engine.network.c2s;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.GreatVoid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → Server: the Great Void key (Y) was pressed: in, or back out ({@link GreatVoid#toggle}). */
public class GreatVoidPacket implements CustomPacketPayload {

    public static final Type<GreatVoidPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "great_void"));
    public static final GreatVoidPacket INSTANCE = new GreatVoidPacket();
    public static final StreamCodec<FriendlyByteBuf, GreatVoidPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<GreatVoidPacket> type() {
        return TYPE;
    }

    public static void handle(GreatVoidPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) GreatVoid.toggle(player);
        });
    }
}
