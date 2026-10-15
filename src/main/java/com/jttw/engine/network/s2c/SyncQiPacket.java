package com.jttw.engine.network.s2c;

import net.minecraft.client.Minecraft;
import com.jttw.engine.progression.qi.QiData;
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
 * Syncs current and max Qi from server to the local client.
 *
 * <p>Written into the local player's persistent data via {@link QiData}
 * so {@link com.jttw.engine.client.JTTWHudRenderer} can read it the same way
 * as server-side code.
 *
 * <p><b>Register in {@link com.jttw.engine.network.NetworkHandler#register()}:</b>
 * <pre>{@code
 * CHANNEL.messageBuilder(SyncQiPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
 *     .encoder(SyncQiPacket::encode)
 *     .decoder(SyncQiPacket::decode)
 *     .consumerMainThread(SyncQiPacket::handle)
 *     .add();
 * }</pre>
 */
public class SyncQiPacket implements CustomPacketPayload {

    public static final Type<SyncQiPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_qi"));
    public static final StreamCodec<FriendlyByteBuf, SyncQiPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncQiPacket::decode);

    @Override
    public Type<SyncQiPacket> type() {
        return TYPE;
    }

    private final float current;
    private final float max;

    public SyncQiPacket(float current, float max) {
        this.current = current;
        this.max = max;
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(SyncQiPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.current);
        buf.writeFloat(pkt.max);
    }

    public static SyncQiPacket decode(FriendlyByteBuf buf) {
        return new SyncQiPacket(buf.readFloat(), buf.readFloat());
    }

    // -----------------------------------------------------------------------
    // Handle (client)
    // -----------------------------------------------------------------------

    public static void handle(SyncQiPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncQiPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        QiData.setMax(player, pkt.max);
        QiData.setCurrent(player, pkt.current);
    }
}