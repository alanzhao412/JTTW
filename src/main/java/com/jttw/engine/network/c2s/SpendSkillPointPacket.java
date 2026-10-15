package com.jttw.engine.network.c2s;

import com.jttw.engine.progression.skilltree.SkillPurchase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Sent client → server when the player clicks a node in the skill tree screen.
 *
 * <p>The server hands it to {@link SkillPurchase#purchase}, which validates and applies it.
 */
public class SpendSkillPointPacket implements CustomPacketPayload {

    public static final Type<SpendSkillPointPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "spend_skill_point"));
    public static final StreamCodec<FriendlyByteBuf, SpendSkillPointPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SpendSkillPointPacket::decode);

    @Override
    public Type<SpendSkillPointPacket> type() {
        return TYPE;
    }

    private final String nodeId;

    public SpendSkillPointPacket(String nodeId) {
        this.nodeId = nodeId;
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(SpendSkillPointPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.nodeId);
    }

    public static SpendSkillPointPacket decode(FriendlyByteBuf buf) {
        return new SpendSkillPointPacket(buf.readUtf());
    }

    // -----------------------------------------------------------------------
    // Handle — runs on the server
    // -----------------------------------------------------------------------

    public static void handle(SpendSkillPointPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            SkillPurchase.purchase(player, pkt.nodeId);
        });
    }

}
