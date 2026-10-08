package com.jttw.engine.network.c2s;

import com.jttw.engine.transformation.BlockForms;
import com.jttw.engine.transformation.TransformationAbilityHandler;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when a transformed player right-clicks with an empty hand. Forge tells
 * only the client about empty-hand clicks, so without this a form's right-click ability (a
 * blaze's fireball, opening a chest form, lighting a TNT form) needed an item in hand.
 */
public class UseFormPacket {

    public static void encode(UseFormPacket msg, FriendlyByteBuf buf) {}

    public static UseFormPacket decode(FriendlyByteBuf buf) {
        return new UseFormPacket();
    }

    public static void handle(UseFormPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) ctx.get().enqueueWork(() -> handleServer(sender));
        ctx.get().setPacketHandled(true);
    }

    public static void handleServer(ServerPlayer sender) {
        if (!TransformationManager.isTransformed(sender) || !sender.getMainHandItem().isEmpty()) return;
        TransformationAbilityHandler.useFormAbilities(sender);
        BlockForms.use(sender);
    }
}
