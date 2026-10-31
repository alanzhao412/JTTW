package com.jttw.engine.world.spider;

import com.jttw.engine.registry.SpiderCaveRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * The venom of Silk-Web Ridge (the spiders' bite, the insect sons' stings, the Hundred-Eyed Demon
 * Lord's poisoned tea): a status that eats {@value #SHARE_PERCENT}% of its bearer's health a second
 * for each level of it, whatever their armour (vanilla's Poison, a point or so a second, is nothing
 * to a character of these levels). Milk cures it, as it does poison; so does a bath in the Purifying
 * Spring ({@link PurifyingSpring}). It never kills by itself: it stops at a sliver of health, as
 * poison does. The ridge's own creatures are proof against it.
 */
public final class Venom {

    private Venom() {}

    public static final int SHARE_PERCENT = 1;
    /** The share of its bearer's greatest health it takes each second, a level. */
    public static final double SHARE = SHARE_PERCENT / 100.0;

    /** Envenoms {@code target} for {@code seconds} at {@code level} (0 the first), unless it is the ridge's own. */
    public static void envenom(LivingEntity target, int seconds, int level, @Nullable Entity source) {
        if (spared(target)) return;
        target.addEffect(new MobEffectInstance(SpiderCaveRegistry.VENOM, seconds * 20, level), source);
    }

    /** Whether {@code e} is proof against it: the ridge's own creatures. */
    public static boolean spared(LivingEntity e) {
        return e instanceof com.jttw.engine.entity.spider.SilkKin;
    }

    /** What a second of it at {@code level} takes from {@code e}. */
    public static float bite(LivingEntity e, int level) {
        return (float) (e.getMaxHealth() * SHARE * (level + 1));
    }

    /** The effect: each second, its bite, through any armour; never the last of its bearer's health. */
    public static final class Effect extends MobEffect {
        public Effect() {
            super(MobEffectCategory.HARMFUL, 0x7FB23A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (e.level().isClientSide) return true;
            float bite = Math.min(bite(e, amplifier), e.getHealth() - 1f);
            if (bite > 0) e.hurt(e.damageSources().magic(), bite);
            return true;
        }
    }
}
