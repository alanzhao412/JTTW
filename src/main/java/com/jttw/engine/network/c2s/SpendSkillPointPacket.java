package com.jttw.engine.network.c2s;

import com.jttw.engine.progression.skilltree.SkillPurchase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when the player clicks a node in the skill tree screen.
 *
 * <p>The server hands it to {@link SkillPurchase#purchase}, which validates and applies it.
 */
public class SpendSkillPointPacket {

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

    public static void handle(SpendSkillPointPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            SkillPurchase.purchase(player, pkt.nodeId);
        });
        ctx.get().setPacketHandled(true);
    }

}
