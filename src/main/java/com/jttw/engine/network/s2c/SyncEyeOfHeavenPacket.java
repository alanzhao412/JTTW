package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs Eye of Heaven activation state from server to client.
 *
 * <p>When active, the client renders ore highlights (tier 2+) and
 * reveals real usernames above transformed players (tier 1+).
 */
public class SyncEyeOfHeavenPacket {

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

    public static void handle(SyncEyeOfHeavenPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncEyeOfHeavenPacket pkt) {
        com.jttw.engine.client.renderer.EyeOfHeavenRenderer.setActive(pkt.playerId, pkt.active, pkt.tier);
    }
}
