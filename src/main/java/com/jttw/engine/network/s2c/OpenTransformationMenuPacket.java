package com.jttw.engine.network.s2c;

import com.jttw.engine.client.gui.TransformationScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: open the transformation menu. */
public class OpenTransformationMenuPacket {

    public OpenTransformationMenuPacket() {}

    public static void encode(OpenTransformationMenuPacket msg, FriendlyByteBuf buf) {
        // no payload
    }

    public static OpenTransformationMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenTransformationMenuPacket();
    }

    public static void handle(OpenTransformationMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(OpenTransformationMenuPacket::handleClient);
        ctx.get().setPacketHandled(true);
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
