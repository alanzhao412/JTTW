package com.jttw.engine.character.erlang;

import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.network.s2c.SyncEyeOfHeavenPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side handler for Erlang Shen's Eye of Heaven ability.
 *
 * <p>The Eye is a TOGGLE ability that drains Qi while active.
 * Visual effects (ore highlighting, username reveal) are entirely client-side;
 * the server just manages Qi drain and tells the client on/off + tier.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ErlangShenAbilityHandler {

    private ErlangShenAbilityHandler() {}

    private static final Set<UUID> ACTIVE_EYES = new HashSet<>();

    private static final float QI_DRAIN_PER_TICK_BASE = 0.08f; // Tier 1-2
    private static final float QI_DRAIN_PER_TICK_T3   = 0.05f; // Tier 3 (reduced)

    // =========================================================================
    // Public API
    // =========================================================================

    public static void toggleEye(ServerPlayer player) {
        if (ACTIVE_EYES.contains(player.getUUID())) {
            stopEye(player);
        } else {
            startEye(player);
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE_EYES.contains(playerId);
    }

    /** Alias for use by ThreePointedSpearItem. */
    public static boolean isEyeActive(UUID playerId) {
        return ACTIVE_EYES.contains(playerId);
    }

    // =========================================================================
    // Internal
    // =========================================================================

    private static void startEye(ServerPlayer player) {
        int tier = SkillTreeData.getEyeTier(player);
        if (tier < 1) return;
        ACTIVE_EYES.add(player.getUUID());
        NetworkHandler.sendToPlayer(player,
                new SyncEyeOfHeavenPacket(player.getUUID(), true, tier));
    }

    private static void stopEye(ServerPlayer player) {
        ACTIVE_EYES.remove(player.getUUID());
        NetworkHandler.sendToPlayer(player,
                new SyncEyeOfHeavenPacket(player.getUUID(), false, 0));
    }

    // =========================================================================
    // Tick — Qi drain
    // =========================================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Set<UUID> toRemove = new HashSet<>();

        for (UUID uuid : ACTIVE_EYES) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                toRemove.add(uuid);
                continue;
            }

            int tier = SkillTreeData.getEyeTier(player);
            float drain = (tier >= 3) ? QI_DRAIN_PER_TICK_T3 : QI_DRAIN_PER_TICK_BASE;

            if (!QiData.spend(player, drain)) {
                toRemove.add(uuid);
                NetworkHandler.sendToPlayer(player,
                        new SyncEyeOfHeavenPacket(uuid, false, 0));
                continue;
            }

            // Periodic Qi sync — only if changed
            if (player.tickCount % 10 == 0) {
                QiManager.syncIfChanged(player);
            }
        }

        ACTIVE_EYES.removeAll(toRemove);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_EYES.remove(event.getEntity().getUUID());
        TACTICS_HIT_TRACKER.remove(event.getEntity().getUUID());
    }

    // =========================================================================
    // CELESTIAL TACTICS — coordinated strike bonus (passive)
    // =========================================================================

    /**
     * Tracks the last tick a target was hit by a particular Erlang Shen player
     * or their Xiaotian Quan. Key: (owner UUID) -> Map(target UUID -> tick).
     *
     * <p>When either the player or XTQ hits a target, we record the tick.
     * If the other entity hits the same target within 20 ticks (1 second),
     * the second hit gains +30% bonus damage. T2 also applies Weakness II
     * and Slowness II for 2 seconds.
     */
    private static final Map<UUID, Map<UUID, TacticsHit>> TACTICS_HIT_TRACKER = new HashMap<>();

    private static class TacticsHit {
        int playerHitTick = -100;
        int xtqHitTick = -100;
    }

    /**
     * Identifies whether an attacker is an Erlang Shen player, and returns
     * their UUID. If the attacker is an XTQ companion, returns the owner UUID.
     * Otherwise returns null.
     *
     * @return a two-element result: [ownerUUID, isXtq]
     */
    private static UUID getEsOwnerIfRelevant(Entity attacker, boolean[] outIsXtq) {
        if (attacker instanceof ServerPlayer sp) {
            String outcomeId = sp.getPersistentData()
                    .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
            if ("eye_of_heaven".equals(outcomeId)) {
                outIsXtq[0] = false;
                return sp.getUUID();
            }
        }

        // Check if attacker is an XTQ companion
        if (attacker instanceof LivingEntity le) {
            CompoundTag pd = le.getPersistentData();
            if (pd.getBoolean(XiaotianQuanManager.XTQ_TAG)
                    && pd.hasUUID(XiaotianQuanManager.XTQ_OWNER_TAG)) {
                outIsXtq[0] = true;
                return pd.getUUID(XiaotianQuanManager.XTQ_OWNER_TAG);
            }
        }

        return null;
    }

    /** Erlang Shen gets +100% damage (total +80% on top of the item's +20%) with the spear. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onWeaponDamageBonus(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!isErlangShen(sp)) return;

        // Spear: item gives +20%, we need total +100%, so multiply by 1.667 (1.2 * 1.667 ≈ 2.0)
        if (sp.getMainHandItem().getItem() instanceof com.jttw.engine.item.ThreePointedSpearItem) {
            event.setAmount(event.getAmount() * 1.667f);
        }
        // Axe: item gives +50%, we need total +150%, so multiply by 1.667 (1.5 * 1.667 ≈ 2.5)
        if (sp.getMainHandItem().getItem() instanceof com.jttw.engine.item.ErlangAxeItem) {
            event.setAmount(event.getAmount() * 1.667f);
        }
    }

    private static boolean isErlangShen(ServerPlayer player) {
        String outcome = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "eye_of_heaven".equals(outcome);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onCelestialTacticsHit(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity == null) return;

        boolean[] isXtq = { false };
        UUID ownerUUID = getEsOwnerIfRelevant(sourceEntity, isXtq);
        if (ownerUUID == null) return;

        // Find the actual player to check tier
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUUID);
        if (owner == null) return;

        int tacticsTier = SkillTreeData.getEsTacticsTier(owner);
        if (tacticsTier < 1) return;

        UUID targetUUID = event.getEntity().getUUID();
        Map<UUID, TacticsHit> ownerTracker = TACTICS_HIT_TRACKER
                .computeIfAbsent(ownerUUID, k -> new HashMap<>());
        TacticsHit hit = ownerTracker.computeIfAbsent(targetUUID, k -> new TacticsHit());

        int currentTick = owner.tickCount;
        boolean coordinated = false;

        if (isXtq[0]) {
            // XTQ is attacking — check if player hit recently
            if (currentTick - hit.playerHitTick <= 20) {
                coordinated = true;
            }
            hit.xtqHitTick = currentTick;
        } else {
            // Player is attacking — check if XTQ hit recently
            if (currentTick - hit.xtqHitTick <= 20) {
                coordinated = true;
            }
            hit.playerHitTick = currentTick;
        }

        if (coordinated) {
            // +30% bonus damage on the second (coordinated) hit
            float bonus = event.getAmount() * 0.30f;
            event.setAmount(event.getAmount() + bonus);

            // T2: apply Weakness II and Slowness II for 2 seconds
            if (tacticsTier >= 2) {
                LivingEntity target = event.getEntity();
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        40, 1, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        40, 1, false, true));
            }

            // Reset so the same pair needs to coordinate again
            hit.playerHitTick = -100;
            hit.xtqHitTick = -100;
        }

        // Cleanup old entries (every 100 ticks)
        if (currentTick % 100 == 0) {
            ownerTracker.entrySet().removeIf(e -> {
                TacticsHit h = e.getValue();
                return (currentTick - h.playerHitTick > 100) && (currentTick - h.xtqHitTick > 100);
            });
        }
    }
}
