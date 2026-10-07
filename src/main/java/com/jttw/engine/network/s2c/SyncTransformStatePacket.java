package com.jttw.engine.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs Yuan Di transformation state to the client for HUD rendering.
 * Includes: active flag, tier, duration remaining/max, recharge remaining/max.
 */
public class SyncTransformStatePacket {

    private final boolean transformed;
    private final int tier;
    private final int durationTicks;
    private final int maxDurationTicks;
    private final int rechargeTicks;
    private final int maxRechargeTicks;

    // Client-side cached state for HUD
    private static boolean clientTransformed = false;
    private static int clientTier = 0;
    private static int clientDurationTicks = 0;
    private static int clientMaxDurationTicks = 0;
    private static int clientRechargeTicks = 0;
    private static int clientMaxRechargeTicks = 0;

    public SyncTransformStatePacket(boolean transformed, int tier,
                                     int durationTicks, int maxDurationTicks,
                                     int rechargeTicks, int maxRechargeTicks) {
        this.transformed = transformed;
        this.tier = tier;
        this.durationTicks = durationTicks;
        this.maxDurationTicks = maxDurationTicks;
        this.rechargeTicks = rechargeTicks;
        this.maxRechargeTicks = maxRechargeTicks;
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(SyncTransformStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.transformed);
        buf.writeInt(pkt.tier);
        buf.writeInt(pkt.durationTicks);
        buf.writeInt(pkt.maxDurationTicks);
        buf.writeInt(pkt.rechargeTicks);
        buf.writeInt(pkt.maxRechargeTicks);
    }

    public static SyncTransformStatePacket decode(FriendlyByteBuf buf) {
        return new SyncTransformStatePacket(
                buf.readBoolean(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt()
        );
    }

    // -----------------------------------------------------------------------
    // Handle (client)
    // -----------------------------------------------------------------------

    public static void handle(SyncTransformStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncTransformStatePacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        clientTransformed = pkt.transformed;
        clientTier = pkt.tier;
        clientDurationTicks = pkt.durationTicks;
        clientMaxDurationTicks = pkt.maxDurationTicks;
        clientRechargeTicks = pkt.rechargeTicks;
        clientMaxRechargeTicks = pkt.maxRechargeTicks;
    }

    // -----------------------------------------------------------------------
    // Client-side getters for HUD
    // -----------------------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public static boolean isTransformed() { return clientTransformed; }

    @OnlyIn(Dist.CLIENT)
    public static int getTier() { return clientTier; }

    @OnlyIn(Dist.CLIENT)
    public static int getDurationTicks() { return clientDurationTicks; }

    @OnlyIn(Dist.CLIENT)
    public static int getMaxDurationTicks() { return clientMaxDurationTicks; }

    @OnlyIn(Dist.CLIENT)
    public static int getRechargeTicks() { return clientRechargeTicks; }

    @OnlyIn(Dist.CLIENT)
    public static int getMaxRechargeTicks() { return clientMaxRechargeTicks; }
}
