package com.jttw.engine.network.s2c;

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

/**
 * Tells the client to start (or refresh) a cooldown countdown for one ability.
 *
 * <p>The server sends this whenever an ability is activated. The client ticks
 * the countdown down in {@link com.jttw.engine.client.ClientAbilityData#tickCooldowns()}
 * every game tick so the HUD drains smoothly with no further packets needed
 * until the ability fires again.
 *
 * <p><b>Register in {@link com.jttw.engine.network.NetworkHandler#register()}:</b>
 * <pre>{@code
 * CHANNEL.messageBuilder(SyncCooldownPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
 *     .encoder(SyncCooldownPacket::encode)
 *     .decoder(SyncCooldownPacket::decode)
 *     .consumerMainThread(SyncCooldownPacket::handle)
 *     .add();
 * }</pre>
 *
 * <p><b>Usage from gameplay code:</b>
 * <pre>{@code
 * NetworkHandler.sendToPlayer(player,
 *     new SyncCooldownPacket(player.getUUID(), "heavenly_mobility", 100, 100));
 * }</pre>
 */
public class SyncCooldownPacket implements CustomPacketPayload {

    public static final Type<SyncCooldownPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_cooldown"));
    public static final StreamCodec<FriendlyByteBuf, SyncCooldownPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncCooldownPacket::decode);

    @Override
    public Type<SyncCooldownPacket> type() {
        return TYPE;
    }

    private final java.util.UUID playerId;
    private final String abilityId;
    /** How many ticks remain on this cooldown right now. */
    private final int remainingTicks;
    /** The full cooldown duration in ticks (used to draw the drain bar ratio). */
    private final int maxTicks;

    public SyncCooldownPacket(java.util.UUID playerId, String abilityId,
                              int remainingTicks, int maxTicks) {
        this.playerId       = playerId;
        this.abilityId      = abilityId;
        this.remainingTicks = remainingTicks;
        this.maxTicks       = maxTicks;
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(SyncCooldownPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeUtf(pkt.abilityId);
        buf.writeInt(pkt.remainingTicks);
        buf.writeInt(pkt.maxTicks);
    }

    public static SyncCooldownPacket decode(FriendlyByteBuf buf) {
        return new SyncCooldownPacket(
                buf.readUUID(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt()
        );
    }

    // -----------------------------------------------------------------------
    // Handle
    // -----------------------------------------------------------------------

    public static void handle(SyncCooldownPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncCooldownPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.getUUID().equals(pkt.playerId)) return;

        com.jttw.engine.client.ClientAbilityData.setCooldown(
                pkt.abilityId, pkt.remainingTicks, pkt.maxTicks);
    }
}