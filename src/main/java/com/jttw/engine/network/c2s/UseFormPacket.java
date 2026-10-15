package com.jttw.engine.network.c2s;

import com.jttw.engine.transformation.BlockForms;
import com.jttw.engine.transformation.TransformationAbilityHandler;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Sent client → server when a transformed player right-clicks with an empty hand. Forge tells
 * only the client about empty-hand clicks, so without this a form's right-click ability (a
 * blaze's fireball, opening a chest form, lighting a TNT form) needed an item in hand.
 */
public class UseFormPacket implements CustomPacketPayload {

    public static final Type<UseFormPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "use_form"));
    public static final StreamCodec<FriendlyByteBuf, UseFormPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), UseFormPacket::decode);

    @Override
    public Type<UseFormPacket> type() {
        return TYPE;
    }

    public static void encode(UseFormPacket msg, FriendlyByteBuf buf) {}

    public static UseFormPacket decode(FriendlyByteBuf buf) {
        return new UseFormPacket();
    }

    public static void handle(UseFormPacket msg, IPayloadContext ctx) {
        ServerPlayer sender = (ServerPlayer) ctx.player();
        if (sender != null) ctx.enqueueWork(() -> handleServer(sender));
    }

    public static void handleServer(ServerPlayer sender) {
        if (!TransformationManager.isTransformed(sender) || !sender.getMainHandItem().isEmpty()) return;
        TransformationAbilityHandler.useFormAbilities(sender);
        BlockForms.use(sender);
    }
}
