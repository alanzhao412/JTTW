package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/** A Divine Beast's line and how far grown into it (see BeastFormShown), for the client to draw. */
public class BeastFormPacket implements CustomPacketPayload {

    public static final Type<BeastFormPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "beast_form"));
    public static final StreamCodec<FriendlyByteBuf, BeastFormPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), BeastFormPacket::decode);

    @Override
    public Type<BeastFormPacket> type() {
        return TYPE;
    }

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

    public static void handle(BeastFormPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(BeastFormPacket pkt) {
        com.jttw.engine.client.renderer.BeastFormLayer.setForm(pkt.entityId, pkt.packed);
    }
}
