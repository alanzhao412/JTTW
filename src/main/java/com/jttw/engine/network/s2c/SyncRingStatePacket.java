package com.jttw.engine.network.s2c;

import com.jttw.engine.client.ClientRingVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncRingStatePacket {
    private final UUID playerId;
    private final boolean active;

    public SyncRingStatePacket(UUID playerId, boolean active) {
        this.playerId = playerId;
        this.active = active;
    }

    public static void encode(SyncRingStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeBoolean(pkt.active);
    }

    public static SyncRingStatePacket decode(FriendlyByteBuf buf) {
        return new SyncRingStatePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(SyncRingStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncRingStatePacket pkt) {
        if (Minecraft.getInstance().level == null) return;
        ClientRingVisuals.setActive(pkt.playerId, pkt.active);
    }
}