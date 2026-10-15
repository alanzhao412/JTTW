package com.jttw.engine.network.s2c;

import com.jttw.engine.client.gui.TransformationScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/** Server → client: open the transformation menu. */
public class OpenTransformationMenuPacket implements CustomPacketPayload {

    public static final Type<OpenTransformationMenuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "open_transformation_menu"));
    public static final StreamCodec<FriendlyByteBuf, OpenTransformationMenuPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), OpenTransformationMenuPacket::decode);

    @Override
    public Type<OpenTransformationMenuPacket> type() {
        return TYPE;
    }

    public OpenTransformationMenuPacket() {}

    public static void encode(OpenTransformationMenuPacket msg, FriendlyByteBuf buf) {
        // no payload
    }

    public static OpenTransformationMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenTransformationMenuPacket();
    }

    public static void handle(OpenTransformationMenuPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(OpenTransformationMenuPacket::handleClient);
    }

    /**
     * Client half, kept in its own @OnlyIn method so the dedicated server strips it.
     * Inlined in a lambda, the screen class was verified (and refused) when the server
     * registered this packet, and the server failed to start.
     */
    @OnlyIn(Dist.CLIENT)
    private static void handleClient() {
        TransformationScreen.openForLocalPlayer();
    }
}
