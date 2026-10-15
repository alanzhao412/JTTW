package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Which armaments a player's body shows (see ArmamentsShown), for the client to draw. */
public class ArmamentsShownPacket implements CustomPacketPayload {

    public static final Type<ArmamentsShownPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "armaments_shown"));
    public static final StreamCodec<FriendlyByteBuf, ArmamentsShownPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ArmamentsShownPacket::decode);

    @Override
    public Type<ArmamentsShownPacket> type() {
        return TYPE;
    }

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

    public static void handle(ArmamentsShownPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ArmamentsShownPacket pkt) {
        com.jttw.engine.client.renderer.ArmamentLayer.setShown(pkt.entityId, pkt.ids);
    }
}
