package com.jttw.engine.network;

import com.jttw.engine.client.gui.TransformationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenTransformationMenuPacket {

    public OpenTransformationMenuPacket() {}

    public static void encode(OpenTransformationMenuPacket msg, FriendlyByteBuf buf) {
        // no payload
    }

    public static OpenTransformationMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenTransformationMenuPacket();
    }

    public static void handle(OpenTransformationMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.setScreen(new TransformationScreen());
        });
        ctx.get().setPacketHandled(true);
    }
}