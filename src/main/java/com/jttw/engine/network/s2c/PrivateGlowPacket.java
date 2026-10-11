package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Creatures (by entity id) that glow, through walls, for this player alone, for {@code ticks}. */
public class PrivateGlowPacket {

    /** The most ids one packet carries. */
    public static final int CAP = 512;

    private final int ticks;
    private final int[] entityIds;

    public PrivateGlowPacket(int ticks, int[] entityIds) {
        this.ticks = ticks;
        this.entityIds = entityIds;
    }

    public static void encode(PrivateGlowPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.ticks);
        buf.writeVarIntArray(pkt.entityIds);
    }

    public static PrivateGlowPacket decode(FriendlyByteBuf buf) {
        return new PrivateGlowPacket(buf.readVarInt(), buf.readVarIntArray(CAP));
    }

    public static void handle(PrivateGlowPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PrivateGlowPacket pkt) {
        com.jttw.engine.client.PrivateGlowClient.show(pkt.ticks, pkt.entityIds);
    }
}
