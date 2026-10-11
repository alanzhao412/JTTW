package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A Divine Beast's line and how far grown into it (see BeastFormShown), for the client to draw. */
public class BeastFormPacket {

    private final int entityId, packed;

    public BeastFormPacket(int entityId, int packed) {
        this.entityId = entityId;
        this.packed = packed;
    }

    public static void encode(BeastFormPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeInt(pkt.packed);
    }

    public static BeastFormPacket decode(FriendlyByteBuf buf) {
        return new BeastFormPacket(buf.readVarInt(), buf.readInt());
    }

    public static void handle(BeastFormPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(BeastFormPacket pkt) {
        com.jttw.engine.client.renderer.BeastFormLayer.setForm(pkt.entityId, pkt.packed);
    }
}
