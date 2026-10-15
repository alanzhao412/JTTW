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
 * Server → Client: sends Xiaotian Quan's stats so the client can display the stat screen.
 */
public class SyncXtqStatsPacket implements CustomPacketPayload {

    public static final Type<SyncXtqStatsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_xtq_stats"));
    public static final StreamCodec<FriendlyByteBuf, SyncXtqStatsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncXtqStatsPacket::decode);

    @Override
    public Type<SyncXtqStatsPacket> type() {
        return TYPE;
    }

    private final float hp;
    private final float maxHp;
    private final float str;
    private final float qi;
    private final float maxQi;
    private final float will;
    private final int level;
    private final boolean hasSlow;
    private final boolean hasHowl;
    private final boolean hasTracking;

    public SyncXtqStatsPacket(float hp, float maxHp, float str, float qi, float maxQi,
                               float will, int level, boolean hasSlow, boolean hasHowl, boolean hasTracking) {
        this.hp = hp;
        this.maxHp = maxHp;
        this.str = str;
        this.qi = qi;
        this.maxQi = maxQi;
        this.will = will;
        this.level = level;
        this.hasSlow = hasSlow;
        this.hasHowl = hasHowl;
        this.hasTracking = hasTracking;
    }

    public static void encode(SyncXtqStatsPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.hp);
        buf.writeFloat(pkt.maxHp);
        buf.writeFloat(pkt.str);
        buf.writeFloat(pkt.qi);
        buf.writeFloat(pkt.maxQi);
        buf.writeFloat(pkt.will);
        buf.writeByte(pkt.level);
        buf.writeBoolean(pkt.hasSlow);
        buf.writeBoolean(pkt.hasHowl);
        buf.writeBoolean(pkt.hasTracking);
    }

    public static SyncXtqStatsPacket decode(FriendlyByteBuf buf) {
        return new SyncXtqStatsPacket(
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readByte(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean()
        );
    }

    public static void handle(SyncXtqStatsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncXtqStatsPacket pkt) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new com.jttw.engine.client.gui.XiaotianQuanStatsScreen(
                pkt.hp, pkt.maxHp, pkt.str, pkt.qi, pkt.maxQi, pkt.will,
                pkt.level, pkt.hasSlow, pkt.hasHowl, pkt.hasTracking
        ));
    }
}
