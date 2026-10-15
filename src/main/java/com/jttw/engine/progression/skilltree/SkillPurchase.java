package com.jttw.engine.progression.skilltree;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncAbilitiesPacket;
import com.jttw.engine.network.s2c.SyncSkillTreePacket;
import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Spending a skill point: validate the node, unlock it, apply what it does immediately,
 * and sync the tree and skill bar back to the client.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class SkillPurchase {

    private SkillPurchase() {}

    /** Max health each Vitality node gives at level 1: grows with level power. */
    public static final double VITALITY_HEALTH = 20.0;

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
        if (com.jttw.engine.world.furnace.WukongTrial.lockedUntilFurnace(player, nodeId)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cThe Yao Wang arts open only to one who survives the Eight Trigrams Furnace."));
            return false;
        }

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
        // Wukong's Vitality heals on purchase, here; its bonus and every other passive are
        // refreshed continuously
        switch (node.id()) {
            case WukongSkillTree.VITALITY_1, WukongSkillTree.VITALITY_2 -> {
                refreshVitality(player);
                player.setHealth(Math.min(player.getHealth() + LevelPower.scaled(player, VITALITY_HEALTH),
                        player.getMaxHealth()));
            }
            // Wukong Qi Mastery — passive regen boost, no immediate effect.
            case WukongSkillTree.QI_1, WukongSkillTree.QI_2 -> {}
            // Every other passive is applied continuously from the skills the player has:
            // see each character's *Passives class, and QiManager for Qi regen and max Qi.
            default -> {}
        }
    }

    /**
     * Sets Wukong's Vitality bonus: {@link #VITALITY_HEALTH} max health per node held, at the
     * player's level power. Refreshed once a second, so it follows a level-up, and a node no
     * longer held gives nothing.
     */
    public static void refreshVitality(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        for (String id : List.of(WukongSkillTree.VITALITY_1, WukongSkillTree.VITALITY_2)) {
            UUID modUUID = UUID.nameUUIDFromBytes(("jttw_vitality_" + id).getBytes());
            double amount = nodes.contains(id) ? VITALITY_HEALTH * LevelPower.of(player) : 0;
            AttributeModifier existing = attr.getModifier(modUUID);
            if (existing != null && existing.getAmount() == amount) continue;
            if (existing != null) attr.removeModifier(modUUID);
            if (amount != 0) {
                attr.addPermanentModifier(new AttributeModifier(modUUID,
                        amount, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 == 0) refreshVitality(sp);
    }
}
