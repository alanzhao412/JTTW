package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells a Grand Saint of Nine Spirits which of his heads leads (0 fire, 1 thunder, 2 frost;
 * -1 for none), so the edge of his view can be tinted its colour.
 */
public class SyncLeadHeadPacket {

    private final int head;

    public SyncLeadHeadPacket(int head) {
        this.head = head;
    }

    public static void encode(SyncLeadHeadPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.head);
    }

    public static SyncLeadHeadPacket decode(FriendlyByteBuf buf) {
        int head = buf.readByte();
        return new SyncLeadHeadPacket(head < -1 || head > 2 ? -1 : head);
    }

    public static void handle(SyncLeadHeadPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncLeadHeadPacket pkt) {
        com.jttw.engine.client.HeadTintOverlay.setLeadHead(pkt.head);
    }
}
