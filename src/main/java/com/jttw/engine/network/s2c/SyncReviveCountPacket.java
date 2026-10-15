package com.jttw.engine.network.s2c;

import com.jttw.engine.character.phoenix.PhoenixReviveData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Syncs Phoenix revive charge count from server to client for HUD display.
 */
public class SyncReviveCountPacket implements CustomPacketPayload {

    public static final Type<SyncReviveCountPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_revive_count"));
    public static final StreamCodec<FriendlyByteBuf, SyncReviveCountPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncReviveCountPacket::decode);

    @Override
    public Type<SyncReviveCountPacket> type() {
        return TYPE;
    }

    private final int remaining;

    public SyncReviveCountPacket(int remaining) {
        this.remaining = remaining;
    }

    public static void encode(SyncReviveCountPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.remaining);
    }

    public static SyncReviveCountPacket decode(FriendlyByteBuf buf) {
        return new SyncReviveCountPacket(buf.readInt());
    }

    public static void handle(SyncReviveCountPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncReviveCountPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        PhoenixReviveData.setRevives(player, pkt.remaining);
    }
}
