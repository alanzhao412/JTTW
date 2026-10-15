package com.jttw.engine.network.s2c;

import com.jttw.engine.world.morality.Morality;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/** A player's karma, for their character screen. */
public class SyncKarmaPacket implements CustomPacketPayload {

    public static final Type<SyncKarmaPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_karma"));
    public static final StreamCodec<FriendlyByteBuf, SyncKarmaPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncKarmaPacket::decode);

    @Override
    public Type<SyncKarmaPacket> type() {
        return TYPE;
    }

    private final int karma;

    public SyncKarmaPacket(int karma) {
        this.karma = karma;
    }

    public static void encode(SyncKarmaPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.karma);
    }

    public static SyncKarmaPacket decode(FriendlyByteBuf buf) {
        return new SyncKarmaPacket(buf.readVarInt());
    }

    public static void handle(SyncKarmaPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncKarmaPacket pkt) {
        if (Minecraft.getInstance().player != null) Morality.set(Minecraft.getInstance().player, pkt.karma);
    }
}
