package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the Marshal of the Heavenly Canopy how full his second stomach is (0-20), or -1 if he
 * has none, so it can be drawn as a second food row.
 */
public class SyncStomachPacket {

    private final int reserve;

    public SyncStomachPacket(int reserve) {
        this.reserve = reserve;
    }

    public static void encode(SyncStomachPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.reserve);
    }

    public static SyncStomachPacket decode(FriendlyByteBuf buf) {
        int reserve = buf.readByte();
        return new SyncStomachPacket(reserve < -1 || reserve > 20 ? -1 : reserve);
    }

    public static void handle(SyncStomachPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncStomachPacket pkt) {
        com.jttw.engine.client.StomachOverlay.setReserve(pkt.reserve);
    }
}
