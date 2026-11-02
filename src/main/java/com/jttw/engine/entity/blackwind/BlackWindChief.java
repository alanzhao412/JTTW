package com.jttw.engine.entity.blackwind;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * One of the Black Bear's two friends, who keep the Black Wind Cave's door: Lingxuzi, the grey-wolf
 * Daoist, and the White-Robed Scholar, the white-flower snake spirit (chapter 17). Always minibosses
 * (purple-named, a captain's sigil to leave, a miniboss's hits), level {@value #CHIEF_LEVEL}, never in
 * packs, with {@value #HEALTH_OVER} times a captain's health.
 */
public abstract class BlackWindChief extends BlackWindMonster {

    /** Their level: the forecourt's keepers' ({@link com.jttw.engine.world.blackwind.BlackWindPlan#guardLevel}). */
    public static final int CHIEF_LEVEL = 34;
    /** Their health over a captain's of their level. */
    public static final double HEALTH_OVER = 1.6;

    protected BlackWindChief(EntityType<? extends BlackWindChief> type, Level level) {
        super(type, level);
    }

    @Override
    public int captainLevel() {
        return CHIEF_LEVEL;
    }

    /** Wherever they are brought, within one of {@link #CHIEF_LEVEL}. */
    @Override
    public int rollCaptainLevel(net.minecraft.util.RandomSource r) {
        return Math.min(topLevel(), CHIEF_LEVEL - 1 + r.nextInt(3));
    }

    @Override
    public int escorts() {
        return 0;
    }

    @Override
    public float captainChance() {
        return 1f;
    }

    @Override
    public boolean isMiniboss() {
        return true;
    }

    /** Always of a miniboss's strength and level, whoever brings him (a summons too). */
    @Override
    public void become(int level, boolean captain) {
        becomeMiniboss(captain ? level : rollCaptainLevel(random), HEALTH_OVER);
    }
}
