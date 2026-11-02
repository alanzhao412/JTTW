package com.jttw.engine.entity.heaven;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * One of Heaven's host: its soldiers, its star generals and gods, its great names. They answer to
 * {@link com.jttw.engine.world.heaven.HeavenHostility}: a demon (a Yaoguai) and anyone of bad karma
 * they attack on sight, anyone else they leave alone until struck, and then they and their allies
 * near fight back. They remember who struck first ({@link #grudge()}): a kill in self-defence is no
 * crime against Heaven, a kill one started is ({@link #crime()}).
 */
public interface HeavenFolk {

    /** Who it came at and who struck it first. */
    Grudge grudge();

    /** What it costs the karma of one who struck it first and killed it (negative). */
    int crime();

    /** How Heaven words that crime ("you struck down a soldier of Heaven"). */
    String crimeWhat();

    /** What its kill pays a character below level 20, in vanilla mobs' XP (a zombie's is 1). */
    int vanillaXpShare();

    /** A hit of {@code share} of a same-level character's health, as it lands from it now. */
    float hitOf(double share);

    /** Those within {@code r} that a blow on all near it falls on: those it fights. */
    List<LivingEntity> foesWithin(double r);

    /** Sets how many ticks are left of its area blow's wind-up (0: none), for its pose. */
    void setWinding(int ticks);
}
