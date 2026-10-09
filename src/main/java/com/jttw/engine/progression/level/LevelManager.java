package com.jttw.engine.progression.level;

import com.jttw.engine.character.byakko.ByakkoAbilityHandler;
import com.jttw.engine.character.phoenix.PhoenixAbilityHandler;
import com.jttw.engine.character.turtle.TurtleAbilityHandler;
import com.jttw.engine.progression.skilltree.AbilityUnlock;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.FormStage;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.network.s2c.SyncReincarnationPacket;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncAbilitiesPacket;
import com.jttw.engine.network.s2c.SyncLevelPacket;
import com.jttw.engine.network.s2c.SyncSkillTreePacket;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Central authority for the JTTW level progression system.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Grant XP from kills, gated by level phase (vanilla vs biome)</li>
 *   <li>Process level-up chains and apply stat changes via {@link StatBlock}</li>
 *   <li>Fire ability unlock notifications via {@link AbilityUnlock}</li>
 *   <li>Manage Divine Beast form stage transitions via {@link FormStage}</li>
 *   <li>Re-apply stat modifiers on respawn, clone, and login</li>
 *   <li>Sync level data to client after every change via {@link SyncLevelPacket}</li>
 * </ul>
 *
 * <p><b>External API:</b>
 * <ul>
 *   <li>{@link #initClass(ServerPlayer, ReincarnationOutcome)} — call from
 *       {@link com.jttw.engine.reincarnation.ReincarnationHandler} after reincarnation fires.</li>
 *   <li>{@link #grantXp(ServerPlayer, long, String)} — call from custom systems
 *       (ritual completions, boss events, etc.).</li>
 * </ul>
 *
 * <p><b>Biome XP hook (TODO):</b>
 * When biome enemies are defined, tag them with:
 * <pre>{@code entity.getPersistentData().putString("jttw_enemy_tier", "GENERAL");}</pre>
 * {@link #onEntityKilled} will pick up the tag automatically once the TODO block is enabled.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LevelManager {

    private LevelManager() {}

    private static final String TIER_TAG = "jttw_enemy_tier";
    /** The zone (biome id) a tiered enemy belongs to; its kill pays by that zone's band wherever it dies. */
    private static final String ZONE_TAG = "jttw_enemy_zone";

    // -----------------------------------------------------------------------
    // Base XP values for vanilla phase (levels 1–20)
    // -----------------------------------------------------------------------

    /** Default XP for a generic vanilla mob kill in the pre-20 phase. */
    private static final long VANILLA_BASE_XP = 15L;

    // -----------------------------------------------------------------------
    // External API
    // -----------------------------------------------------------------------

    /**
     * Initialise the level system for a player who has just reincarnated.
     *
     * <p>Resets to level 1, assigns the class derived from {@code outcome},
     * fires level-1 ability unlocks, applies base stats, and sends the first
     * sync packet.
     *
     * <p><b>Call this from {@link com.jttw.engine.reincarnation.ReincarnationHandler}
     * inside the {@code onPlayerClone} handler, after the outcome is confirmed.</b>
     */
    public static void initClass(ServerPlayer player, ReincarnationOutcome outcome) {
        ProgressionClass cls = ProgressionClass.fromOutcome(outcome);

        PlayerLevel.setProgressionClass(player, cls);
        PlayerLevel.setLevel(player, 1);
        PlayerLevel.setXp(player, 0L);
        PlayerLevel.setFormStage(player, 0);

        // Set Divine Beast elemental line if applicable
        if (cls == ProgressionClass.DIVINE_BEAST) {
            DivineBeastLine line = DivineBeastLine.fromOutcome(outcome);
            if (line != null) {
                PlayerLevel.setBeastLine(player, line);
            }
        }

        // Grant all level-1 unlocks (e.g. lesser_form, primordial_seed)
        processUnlocks(player, cls, 0, 1);

        // Apply base stats immediately
        reapplyStats(player);
        syncToClient(player);

        player.sendSystemMessage(Component.literal(
                "§6[JTTW] §rYou have awakened as: §6" + cls.name()
                        + "§r | Lv §a1§r | BST §e" + cls.bst));
    }

    /**
     * Award XP to a player from any source.
     *
     * <p>Handles level-up chains, ability unlocks, form shifts, and client sync automatically.
     * No XP is granted if the player has no class, or is already at level cap.
     *
     * @param source Human-readable label for logging (e.g. {@code "vanilla_mob"},
     *               {@code "jungle_general"}, {@code "ritual_complete"}).
     */
    public static void grantXp(ServerPlayer player, long amount, String source) {
        if (amount <= 0) return;

        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        if (cls == null) return;

        int currentLevel = PlayerLevel.getLevel(player);
        if (currentLevel >= LevelConstants.MAX_LEVEL) return;

        // Wukong checkpoint gate — silently block XP at checkpoint levels
        if (com.jttw.engine.character.wukong.WukongCheckpoints.isBlocked(player)) {
            return;
        }

        long newXp = PlayerLevel.getXp(player) + amount;
        PlayerLevel.setXp(player, newXp);

        boolean leveledUp = levelUpLoop(player, cls, currentLevel);
        if (!leveledUp) {
            // Still sync XP bar even without a level-up
            syncToClient(player);
        }
    }

    /**
     * Re-apply stat modifiers to the player.
     * Call after respawn, class changes, or any time you suspect the attributes
     * may have been reset (e.g. after server restart with a fresh world load).
     */
    public static void reapplyStats(ServerPlayer player) {
        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        if (cls == null) {
            StatBlock.removeFrom(player);
            TurtleAbilityHandler.removePassiveScaling(player);
            ByakkoAbilityHandler.removePassiveScaling(player);
            return;
        }
        int level = PlayerLevel.getLevel(player);
        StatBlock.compute(cls, level).applyTo(player);

        // World Turtle innate passive: +1 physical resistance per level
        TurtleAbilityHandler.applyPassiveScaling(player);

        // Byakko innate passive: +0.8% speed and +0.01 step height per level
        ByakkoAbilityHandler.applyPassiveScaling(player);

        // Phoenix innate passive: higher regen + revive init
        PhoenixAbilityHandler.applyPassiveScaling(player);
    }

    // -----------------------------------------------------------------------
    // Forge event hooks
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        // Only process kills made by a server-side player
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (killer.level().isClientSide) return;

        ProgressionClass cls = PlayerLevel.getProgressionClass(killer);
        if (cls == null) return;

        int killerLevel = PlayerLevel.getLevel(killer);

        // ── Phase 1: Vanilla mobs (levels 1–19) ─────────────────────────────
        if (killerLevel < LevelConstants.VANILLA_CAP_LEVEL) {
            long xp = vanillaMobXp(event.getEntity().getType());
            if (xp > 0) grantXp(killer, xp, "vanilla_mob");
            return;
        }

        // ── Phase 2: Biome enemies (level 20+) ───────────────────────────────
        // Check if this entity has been tagged as a tiered enemy.
        String tierStr = event.getEntity().getPersistentData().getString(TIER_TAG);
        if (tierStr.isEmpty()) return; // not a tiered mob; no XP

        EnemyTier tier = EnemyTier.parse(tierStr);
        String zone = event.getEntity().getPersistentData().getString(ZONE_TAG);
        BiomeLevelBand band = zone.isEmpty() ? BiomeLevelBand.forPlayer(killer)
                : BiomeLevelBand.forBiome(new net.minecraft.resources.ResourceLocation(zone));

        if (band == null) {
            // Mob was tagged but player isn't in a registered biome band.
            // Still award XP using a flat fallback so kills are never wasted.
            // TODO: remove this fallback once all biome bands are registered.
            long fallbackXp = (long) (VANILLA_BASE_XP * tier.xpMultiplier * 3);
            grantXp(killer, fallbackXp, "biome_fallback:" + tierStr);
            return;
        }

        if (!band.isRewardingForLevel(killerLevel)) return; // too low or too high for this zone

        long xp = band.xpForTier(tier);
        grantXp(killer, xp, band.biome() + ":" + tier.displayName);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.isEndConquered()) return; // End portal respawn — not a death
        reapplyStats(sp);
        syncToClient(sp);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        PlayerLevel.copyTo(event.getOriginal(), newPlayer);
        SkillTreeData.copyTo(event.getOriginal(), newPlayer);
        com.jttw.engine.character.wukong.WukongCheckpoints.copyTo(event.getOriginal(), newPlayer);
        com.jttw.engine.item.ImmortalItems.copyTo(event.getOriginal(), newPlayer);

        // Copy reincarnation outcome and reincarnated flag so ability routing
        // and SyncAbilitiesPacket.build() keep working after every death —
        // not just the first reincarnation death.
        for (String key : new String[]{
                com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME,
                com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATED
        }) {
            net.minecraft.nbt.CompoundTag src = event.getOriginal().getPersistentData();
            if (src.contains(key)) {
                newPlayer.getPersistentData().put(key, src.get(key).copy());
            }
        }

        reapplyStats(newPlayer);
        com.jttw.engine.item.ImmortalPeachItem.reapplyPeachModifiers(newPlayer);
        syncToClient(newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        // Re-apply in case attributes were wiped during a server restart
        reapplyStats(sp);
        syncToClient(sp);
    }

    // -----------------------------------------------------------------------
    // Internal logic
    // -----------------------------------------------------------------------

    /**
     * Checks whether the player has accumulated enough XP to level up, applies
     * the level-up (possibly multiple times), and returns true if at least one
     * level was gained.
     */
    private static boolean levelUpLoop(ServerPlayer player, ProgressionClass cls, int startLevel) {
        int level = startLevel;
        long accumulatedXp = PlayerLevel.getXp(player);
        boolean anyLevelUp = false;

        while (level < LevelConstants.MAX_LEVEL) {
            // Stop at Wukong's story checkpoints (his alone)
            if (com.jttw.engine.character.wukong.WukongCharacter.is(player)
                    && com.jttw.engine.character.wukong.WukongCheckpoints.getCheckpointForLevel(level) != null
                    && !com.jttw.engine.character.wukong.WukongCheckpoints.isComplete(player,
                            com.jttw.engine.character.wukong.WukongCheckpoints.getCheckpointForLevel(level))) {
                break;
            }

            long needed = cls.growthRate.totalXpForLevel(level + 1);
            if (accumulatedXp < needed) break;

            int prevLevel = level;
            level++;
            anyLevelUp = true;

            PlayerLevel.setLevel(player, level);

            // Compute stat deltas (based on StatBlock, not MC attributes)
            StatBlock prevStats = StatBlock.compute(cls, prevLevel);
            StatBlock newStats  = StatBlock.compute(cls, level);

            // Apply new stats immediately
            newStats.applyTo(player);

            // Restore health to new max on level-up (quality of life)
            player.setHealth((float) player.getMaxHealth());

            // Ability unlocks (legacy system — class-level milestones)
            processUnlocks(player, cls, prevLevel, level);

            // Skill point: 1 point every 5 levels
            if (level % 5 == 0) {
                SkillTreeData.addPoints(player, 1);
                player.sendSystemMessage(Component.literal(
                        "§6[JTTW] §rSkill point earned! §e" +
                                SkillTreeData.getPoints(player) + " §rnow available. §7(Press K to open Skill Tree)"));
            }

            // Divine Beast form stage transitions
            if (cls == ProgressionClass.DIVINE_BEAST) {
                processFormStage(player, prevLevel, level);
            }

            // Level-up feedback (with exact stat gains)
            double dHp   = newStats.maxHealth()     - prevStats.maxHealth();
            double dStr  = newStats.attackDamage()  - prevStats.attackDamage();
            double dPres = newStats.physicalResistance() - prevStats.physicalResistance();
            double dMres = newStats.magicResistance()    - prevStats.magicResistance();
            double dQi   = newStats.qi()            - prevStats.qi();
            double dWill = newStats.will()          - prevStats.will();

            player.sendSystemMessage(Component.literal(
                    "§a✦ Level Up! §r" + cls.name() + " §7→ §aLv " + level
                            + "\n§c+HP: §r" + String.format("%.1f", dHp)
                            + "  §6+ATK: §r" + String.format("%.2f", dStr)
                            + "  §7+PRES: §r" + String.format("%.2f", dPres)
                            + "  §9+MRES: §r" + String.format("%.2f", dMres)
                            + "\n§b+Max Qi: §r" + String.format("%.1f", dQi)
                            + "  §d+Will: §r" + String.format("%.2f", dWill)
            ));

            // Auto-transport Wukong to Hell at level 30
            if (com.jttw.engine.character.wukong.WukongCharacter.is(player) && level == 30) {
                player.sendSystemMessage(Component.literal(
                        "§4[JTTW] §cThe gates of the Underworld open before you..."));
                com.jttw.engine.dimension.JTTWDimensions.teleportTo(player,
                        com.jttw.engine.dimension.JTTWDimensions.HELL);
            }
        }

        if (anyLevelUp) syncToClient(player);
        return anyLevelUp;
    }

    private static void processUnlocks(ServerPlayer player, ProgressionClass cls,
                                       int prevLevel, int newLevel) {
        List<AbilityUnlock> unlocks = AbilityUnlock.getNewUnlocks(cls, prevLevel, newLevel);
        for (AbilityUnlock unlock : unlocks) {
            PlayerLevel.grantAbility(player, unlock.abilityId());
            player.sendSystemMessage(Component.literal(
                    "§6[Ability Unlocked] §e" + unlock.displayName()));

            // ── Wire ability side-effects here ─────────────────────────────
            // TODO: each ability ID maps to gameplay effects.  Examples:
            //
            // "heavenly_mobility" → grant JTTWFlight universal flight flag
            //   JTTWFlight.setUniversalFlight(player, true);
            //
            // "intermediate_form" / "sovereign_form" → trigger a TransformationManager morph
            //   to the next form entity for this player's reincarnation outcome
            //
            // "demon_transformation" → unlock the transformation GUI for yaoguai
            //
            // "domain_authority" / "mandate_of_heaven" → apply a custom attribute aura
            //
            // Keep effect logic in dedicated trait/ability classes and call them here
            // to keep LevelManager focused on the progression flow only.
        }
    }

    /** Check if a Divine Beast player has crossed into a new form stage. */
    private static void processFormStage(ServerPlayer player, int prevLevel, int newLevel) {
        FormStage prevStage = FormStage.forLevel(prevLevel);
        FormStage newStage  = FormStage.forLevel(newLevel);

        if (newStage.stageIndex > prevStage.stageIndex) {
            PlayerLevel.setFormStage(player, newStage.stageIndex);
            player.sendSystemMessage(Component.literal(
                    "§d[Form Shift] §r" + newStage.displayName));
            player.sendSystemMessage(Component.literal(
                    "§7" + newStage.transitionMessage));

            // TODO: trigger morph / visual update to the new form entity.
            // The target entity type should come from the player's ReincarnationOutcome
            // and the current FormStage.  Add a mapping in FormStage or a separate
            // FormEntityMap class once entity types are defined.
        }
    }

    // -----------------------------------------------------------------------
    // Vanilla mob XP table
    // -----------------------------------------------------------------------

    /**
     * XP awarded for killing a vanilla mob during the pre-20 phase.
     *
     * <p>TODO: Expand to a full table once the vanilla mob roster is finalised.
     * Consider data-driving this (e.g. JSON config) for easy tuning.
     */
    private static long vanillaMobXp(EntityType<?> type) {
        // Vanilla boss mobs — worth a big chunk of the level-20 bar
        if (type == EntityType.ENDER_DRAGON
                || type == EntityType.WITHER
                || type == EntityType.ELDER_GUARDIAN) {
            return VANILLA_BASE_XP * 20;
        }
        // Strong overworld / nether mobs
        if (type == EntityType.WITHER_SKELETON
                || type == EntityType.BLAZE
                || type == EntityType.GHAST
                || type == EntityType.RAVAGER) {
            return VANILLA_BASE_XP * 3;
        }
        // Standard hostile mobs
        return VANILLA_BASE_XP;
    }

    // -----------------------------------------------------------------------
    // Client sync
    // -----------------------------------------------------------------------

    public static void syncToClient(ServerPlayer player) {
        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        SyncLevelPacket pkt = new SyncLevelPacket(
                player.getUUID(),
                PlayerLevel.getLevel(player),
                PlayerLevel.getXp(player),
                cls != null ? cls.name() : null,
                PlayerLevel.getFormStage(player)
        );
        NetworkHandler.sendToPlayer(player, pkt);

        // Sync the full unlocked ability list so the skill-bar HUD stays current.
        NetworkHandler.sendToPlayer(player, SyncAbilitiesPacket.build(player));

        // Sync skill tree state (points + unlocked nodes) to client.
        NetworkHandler.sendToPlayer(player, SyncSkillTreePacket.build(player));

        // Identity-tab needs reincarnation outcome on the client (persistent NBT doesn't sync automatically).
        boolean reincarnated = AlignmentData.hasReincarnated(player);
        String outcomeId = player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        NetworkHandler.sendToPlayer(player,
                new SyncReincarnationPacket(player.getUUID(), reincarnated, outcomeId));
    }
}