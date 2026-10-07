package com.jttw.engine.network.c2s;

import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncAbilitiesPacket;
import com.jttw.engine.network.s2c.SyncSkillTreePacket;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.character.wukong.WukongSkillTree;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when the player clicks a node in the skill tree screen.
 *
 * <p>The server validates the spend via {@link SkillTreeData#tryUnlock(net.minecraft.world.entity.player.Player, String)}
 * and, if successful:
 * <ul>
 *   <li>Applies the passive effect (if any).</li>
 *   <li>Grants flight if a flight node was unlocked.</li>
 *   <li>Sends back a {@link SyncSkillTreePacket} + {@link SyncAbilitiesPacket} with updated state.</li>
 * </ul>
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
            handleServer(player, pkt.nodeId);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleServer(ServerPlayer player, String nodeId) {
        // Only the player's own tree. The client only offers those nodes, but the id comes
        // off the wire, and flight takes the best tier across every tree.
        PlayableCharacter character = Characters.of(player);
        if (character == null || !character.skillTree().owns(nodeId)) return;

        boolean success = SkillTreeData.tryUnlock(player, nodeId);
        if (!success) return;

        // Apply passive effects immediately
        SkillNode node = SkillTreeData.resolveNode(nodeId);
        if (node != null) {
            applyPassive(player, node);
        }

        // Unified flight logic: T1 = glide, T2 = full flight, T3 = fastest + dimension travel
        // Find the highest flight tier across all class skill trees
        int flightTier = Math.max(
                Math.max(SkillTreeData.getFlightTier(player), SkillTreeData.getEsFlightTier(player)),
                Math.max(SkillTreeData.getDrFlightTier(player), SkillTreeData.getPhEmberTier(player))
        );

        if (flightTier >= 1) {
            com.jttw.engine.flight.JTTWFlight.setUniversalFlight(player, true);
            com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(player, flightTier);
            // T1 = glide only, T2+ = full flight
            com.jttw.engine.flight.JTTWFlight.setGlideOnly(player, flightTier == 1);
        }

        // Sync updated state back to client
        NetworkHandler.sendToPlayer(player, SyncSkillTreePacket.build(player));
        NetworkHandler.sendToPlayer(player, SyncAbilitiesPacket.build(player));
    }

    private static void applyPassive(ServerPlayer player, SkillNode node) {
        // HP passives: +20 max HP (applied as attribute modifier)
        // Qi passives: +25 max Qi
        // Combat passives: tracked via SkillTreeData.hasNode() in damage events
        switch (node.id()) {
            case WukongSkillTree.VITALITY_1, WukongSkillTree.VITALITY_2 -> {
                // Add 20 max health — using Forge attribute system
                net.minecraft.world.entity.ai.attributes.AttributeInstance attr =
                        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (attr != null) {
                    String modId = "jttw_vitality_" + node.id();
                    java.util.UUID modUUID = java.util.UUID.nameUUIDFromBytes(modId.getBytes());
                    attr.removeModifier(modUUID);
                    attr.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            modUUID, "JTTW Vitality " + node.tier(),
                            20.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                    ));
                    player.setHealth(Math.min(player.getHealth() + 20f, player.getMaxHealth()));
                }
            }
            // Wukong Qi Mastery — passive regen boost, no immediate effect.
            case WukongSkillTree.QI_1, WukongSkillTree.QI_2 -> {}
            // Yao Wang, Celestial Hunt, Zhàn Shén, Draconic Pressure, Tidal Recovery,
            // Scent of Blood, Earthen Roots, Bedrock Constitution — all tracked via
            // SkillTreeData.hasNode() in gameplay event handlers. No immediate effect.
            default -> {}
        }
    }
}