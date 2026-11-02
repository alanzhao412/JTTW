package com.jttw.engine.grind;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.progression.level.LevelManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * What the open world's grinding grounds (the raided villages, the infested wilds) pay a kill. Each of
 * their creatures carries its ground's band and its tier ({@link #mark}); its kill pays as a zone's
 * creature of its own level does: a Grunt's, a General's or a Boss's of the zone whose band holds that
 * level ({@link #zoneBandFor}), so a level-50 raider pays as a Mystical Waters soldier. Below level
 * 20 a killer is paid as for a vanilla monster, by their own level ({@link LevelManager#vanillaMobXp}),
 * times its rank's {@link #vanillaShare}. A creature under 20 pays a killer of 20 or more nothing (as
 * the open world's yaoguai do), and nothing pays a killer more than {@value #OVER_TOP} levels over its
 * ground's band (as a zone pays none past its band's top and five).
 */
public final class GrindXp {

    private GrindXp() {}

    /** On each creature of a grinding ground: its band ({@code Lo}, {@code Hi}), its tier, and its ground ({@code Village} or {@code Patch}). */
    public static final String KEY = JTTWEngine.MOD_ID + ":grind";
    /** How far past its band's top a killer may stand and still be paid. */
    public static final int OVER_TOP = 5;

    /** Marks {@code e} a creature of a grinding ground of {@code band}, of {@code tier}. */
    public static void mark(LivingEntity e, RingLevels.Band band, EnemyTier tier) {
        CompoundTag t = e.getPersistentData().getCompound(KEY);
        t.putInt("Lo", band.lo());
        t.putInt("Hi", band.hi());
        t.putString("Tier", tier.name());
        e.getPersistentData().put(KEY, t);
    }

    /** The grinding ground's tag on {@code e} (its band, tier and ground), or null if it is none's. */
    @Nullable
    public static CompoundTag of(LivingEntity e) {
        return e.getPersistentData().contains(KEY) ? e.getPersistentData().getCompound(KEY) : null;
    }

    /** The band of the ground {@code e} is of, or null. */
    @Nullable
    public static RingLevels.Band bandOf(LivingEntity e) {
        CompoundTag t = of(e);
        return t == null ? null : new RingLevels.Band(t.getInt("Lo"), t.getInt("Hi"));
    }

    public static EnemyTier tierOf(LivingEntity e) {
        CompoundTag t = of(e);
        return t == null ? EnemyTier.GRUNT : EnemyTier.parse(t.getString("Tier"));
    }

    /** The zone band that holds {@code level} (past 100, the last), or null below 20. */
    @Nullable
    public static BiomeLevelBand zoneBandFor(int level) {
        int l = Math.min(level, LevelConstants.MAX_LEVEL);
        for (BiomeLevelBand b : BiomeLevelBand.all()) if (l >= b.minLevel() && l <= b.maxLevel()) return b;
        return null;
    }

    /** Below 20, how many vanilla monsters' worth a kill of this tier is, times its kind's own weight: a regular 1, a captain 2, a miniboss 4, a boss 20. */
    public static double vanillaShare(EnemyTier tier, LivingEntity victim) {
        return switch (tier) {
            case GRUNT -> victim instanceof WildYaoguai y && y.rank() == WildYaoguai.Rank.MINIBOSS ? 4 : 1;
            case GENERAL -> victim instanceof WildYaoguai y && y.rank() == WildYaoguai.Rank.MINIBOSS ? 4 : 2;
            case BOSS -> 20;
        };
    }

    /**
     * What killing {@code victim} pays a killer of {@code killerLevel}, or -1 if it is no grinding
     * ground's creature (then the usual rules pay it).
     */
    public static long forKill(LivingEntity victim, int killerLevel) {
        CompoundTag t = of(victim);
        if (t == null) return -1;
        EnemyTier tier = EnemyTier.parse(t.getString("Tier"));
        int hi = t.getInt("Hi");
        if (killerLevel > hi + OVER_TOP) return 0;
        if (killerLevel < LevelConstants.VANILLA_CAP_LEVEL) {
            return Math.round(LevelManager.vanillaMobXp(victim, killerLevel) * vanillaShare(tier, victim));
        }
        int level = victim instanceof com.jttw.engine.entity.zone.MobLevel m ? m.mobLevel() : hi;
        BiomeLevelBand band = zoneBandFor(level);
        return band == null ? 0 : band.xpForTier(tier);
    }
}
