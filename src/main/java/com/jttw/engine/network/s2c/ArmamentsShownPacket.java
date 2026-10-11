package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Which armaments a player's body shows (see ArmamentsShown), for the client to draw. */
public class ArmamentsShownPacket {

    private final int entityId;
    private final List<String> ids;

    public ArmamentsShownPacket(int entityId, List<String> ids) {
        this.entityId = entityId;
        this.ids = ids;
    }

    public static void encode(ArmamentsShownPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeVarInt(pkt.ids.size());
        for (String id : pkt.ids) buf.writeUtf(id, 64);
    }

    public static ArmamentsShownPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int n = Math.min(32, buf.readVarInt());
        List<String> ids = new ArrayList<>(n);
        for (int i = 0; i < n; i++) ids.add(buf.readUtf(64));
        return new ArmamentsShownPacket(entityId, ids);
    }

    public static void handle(ArmamentsShownPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ArmamentsShownPacket pkt) {
        com.jttw.engine.client.renderer.ArmamentLayer.setShown(pkt.entityId, pkt.ids);
    }
}
