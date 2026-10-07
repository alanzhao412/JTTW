package com.jttw.engine.progression.skilltree;

import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncAbilitiesPacket;
import com.jttw.engine.network.s2c.SyncSkillTreePacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Spending a skill point: validate the node, unlock it, apply what it does immediately,
 * and sync the tree and skill bar back to the client.
 */
public final class SkillPurchase {

    private SkillPurchase() {}

    /**
     * Unlocks {@code nodeId} for the player if it is in their character's tree and they can
     * afford it with its prerequisites met.
     *
     * @return true if the node was unlocked
     */
    public static boolean purchase(ServerPlayer player, String nodeId) {
        // Only the player's own tree. The client only offers those nodes, but the id comes
        // off the wire, and flight takes the best tier across every tree.
        PlayableCharacter character = Characters.of(player);
        if (character == null || !character.skillTree().owns(nodeId)) return false;

        boolean success = SkillTreeData.tryUnlock(player, nodeId);
        if (!success) return false;

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
        return true;
    }

    private static void applyPassive(ServerPlayer player, SkillNode node) {
        // Wukong's Vitality is applied once, here; other passives are refreshed continuously
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
            // Every other passive is applied continuously from the skills the player has:
            // see each character's *Passives class, and QiManager for Qi regen and max Qi.
            default -> {}
        }
    }
}
