package com.jttw.engine.transformation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;

import java.util.Set;

/**
 * Shared (server + client safe) utility for tier-based transformation restrictions.
 *
 * <ul>
 *   <li>T1: Ground and water mobs only (no flying mobs, no bosses)</li>
 *   <li>T2: Unlocks flying mob forms (but not bosses)</li>
 *   <li>T3: Unlocks boss entity forms</li>
 * </ul>
 */
public final class TransformTierFilter {

    private TransformTierFilter() {}

    /** Flying mobs: require Transform Tier 2+ */
    public static final Set<EntityType<?>> FLYING_MOBS = Set.of(
            EntityType.GHAST, EntityType.BLAZE, EntityType.PHANTOM,
            EntityType.BEE, EntityType.PARROT, EntityType.BAT,
            EntityType.ENDER_DRAGON, EntityType.WITHER,
            EntityType.VEX, EntityType.ALLAY
    );

    /** Boss entities: require Transform Tier 3 */
    public static final Set<EntityType<?>> BOSS_MOBS = Set.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER,
            EntityType.ELDER_GUARDIAN, EntityType.WARDEN
    );

    /**
     * Returns true if the given entity type is allowed at the given transform tier.
     * T1: ground and water mobs only (no flying, no bosses)
     * T2: unlocks flying mob forms (but not bosses)
     * T3: unlocks boss entity forms
     */
    public static boolean isAllowedAtTier(EntityType<?> type, int tier) {
        if (BOSS_MOBS.contains(type)) {
            return tier >= 3;
        }
        if (FLYING_MOBS.contains(type)) {
            return tier >= 2;
        }
        return true; // ground/water mobs allowed at any tier
    }

    // -----------------------------------------------------------------------
    // Size filter — block entities smaller than the player
    // -----------------------------------------------------------------------

    /** Player model height is 1.8 blocks. Block anything shorter. */
    private static final float MIN_HEIGHT = 1.8f;

    /**
     * Returns true if the entity type's default bounding box is too small
     * (shorter than the player model).
     */
    public static boolean isTooSmall(EntityType<?> type) {
        return type.getHeight() < MIN_HEIGHT;
    }

    // -----------------------------------------------------------------------
    // Marshal of the Heavenly Canopy — 36 Transformations filter
    // -----------------------------------------------------------------------

    /** Nether mobs: always blocked for Marshal. */
    public static final Set<EntityType<?>> MARSHAL_BLOCKED_NETHER = Set.of(
            EntityType.BLAZE, EntityType.GHAST, EntityType.MAGMA_CUBE,
            EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.WITHER_SKELETON,
            EntityType.STRIDER, EntityType.HOGLIN, EntityType.ZOGLIN,
            EntityType.PIGLIN_BRUTE
    );

    /** Boss entities: always blocked for Marshal. */
    public static final Set<EntityType<?>> MARSHAL_BLOCKED_BOSSES = Set.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER,
            EntityType.ELDER_GUARDIAN, EntityType.WARDEN
    );

    /** The only hostile (Monster-subclass) mobs Marshal is allowed to transform into. */
    public static final Set<EntityType<?>> MARSHAL_ALLOWED_HOSTILE = Set.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER
    );

    /**
     * Returns true if the given entity type is allowed for the Marshal of the
     * Heavenly Canopy's "36 Transformations" ability.
     *
     * <p>Rules:
     * <ul>
     *   <li>Nether mobs and bosses are always blocked.</li>
     *   <li>Hostile mobs (Monster subclass) are blocked unless in the explicit allow-list
     *       (Zombie, Skeleton, Spider).</li>
     *   <li>All passive land animals, water animals, and neutral mobs pass through.</li>
     * </ul>
     */
    public static boolean isAllowedForMarshal(EntityType<?> type) {
        if (MARSHAL_BLOCKED_NETHER.contains(type)) return false;
        if (MARSHAL_BLOCKED_BOSSES.contains(type)) return false;

        // If it's a Monster type and not in the allowed list, block it.
        // We check the base class because EntityType stores the entity class.
        if (Monster.class.isAssignableFrom(type.getBaseClass())) {
            return MARSHAL_ALLOWED_HOSTILE.contains(type);
        }

        // Passive animals, water animals, neutral mobs — all allowed
        return true;
    }
}
