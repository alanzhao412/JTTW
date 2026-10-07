package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.FormStage;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/** Becoming a class, earning XP, levelling up, and what each level-up grants. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class LevelingGameTests {

    private static long xpToReach(ProgressionClass cls, int level) {
        return cls.growthRate.totalXpForLevel(level);
    }

    @GameTest(template = PLATFORM)
    public static void initClassStartsAtLevelOneWithLevelOneUnlocks(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.DRAGON);
        eq(h, ProgressionClass.DIVINE_BEAST, PlayerLevel.getProgressionClass(p), "class");
        eq(h, DivineBeastLine.WATER, PlayerLevel.getBeastLine(p), "beast line");
        eq(h, 1, PlayerLevel.getLevel(p), "level");
        eq(h, 0L, PlayerLevel.getXp(p), "xp");
        check(h, PlayerLevel.hasAbility(p, "lesser_form"), "Divine Beast level-1 unlock");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void initClassAppliesLevelOneStats(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        StatBlock expected = StatBlock.compute(ProgressionClass.HEAVENLY, 1);
        near(h, expected.maxHealth(), p.getAttributeValue(Attributes.MAX_HEALTH), "max health");
        near(h, expected.attackDamage(), p.getAttributeValue(Attributes.ATTACK_DAMAGE), "attack");
        near(h, expected.armor(), p.getAttributeValue(Attributes.ARMOR), "armor");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void xpWithoutAClassIsIgnored(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.grantXp(p, 10_000, "test");
        eq(h, 0L, PlayerLevel.getXp(p), "xp");
        eq(h, 1, PlayerLevel.getLevel(p), "level");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void zeroOrNegativeXpIsIgnored(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        LevelManager.grantXp(p, 0, "test");
        LevelManager.grantXp(p, -500, "test");
        eq(h, 0L, PlayerLevel.getXp(p), "xp");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void exactlyEnoughXpLevelsUp(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        long need = xpToReach(ProgressionClass.HEAVENLY, 2);
        LevelManager.grantXp(p, need - 1, "test");
        eq(h, 1, PlayerLevel.getLevel(p), "one short");
        LevelManager.grantXp(p, 1, "test");
        eq(h, 2, PlayerLevel.getLevel(p), "exactly enough");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void bigXpGrantCrossesSeveralLevelsAndPaysSkillPoints(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        LevelManager.grantXp(p, xpToReach(ProgressionClass.HEAVENLY, 12), "test");
        eq(h, 12, PlayerLevel.getLevel(p), "level");
        // one point at each multiple of 5: levels 5 and 10
        eq(h, 2, SkillTreeData.getPoints(p), "skill points");
        check(h, PlayerLevel.hasAbility(p, "heavenly_mobility"), "level-10 Heavenly unlock");
        check(h, !PlayerLevel.hasAbility(p, "divine_perception"), "level-20 unlock not yet");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void levelUpRefillsHealthToTheNewMaximum(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.TURTLE);
        p.setHealth(1f);
        LevelManager.grantXp(p, xpToReach(ProgressionClass.DIVINE_BEAST, 5), "test");
        near(h, p.getMaxHealth(), p.getHealth(), "health after level-up");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void divineBeastEvolvesAtLevelTwenty(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.WHITE_TIGER);
        LevelManager.grantXp(p, xpToReach(ProgressionClass.DIVINE_BEAST, 20), "test");
        eq(h, 20, PlayerLevel.getLevel(p), "level");
        eq(h, FormStage.GUARDIAN.stageIndex, PlayerLevel.getFormStage(p), "form stage");
        check(h, PlayerLevel.hasAbility(p, "domain_sense"), "level-20 Divine Beast unlock");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void levelCapsAtHundred(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.DRAGON);
        LevelManager.grantXp(p, Long.MAX_VALUE / 4, "test");
        eq(h, 100, PlayerLevel.getLevel(p), "level");
        long xp = PlayerLevel.getXp(p);
        LevelManager.grantXp(p, 1000, "test");
        eq(h, xp, PlayerLevel.getXp(p), "no XP banked past the cap");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongStopsAtAnUnfinishedCheckpoint(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        // already level 30 (so the level-30 trip to Hell isn't triggered), Hell's Book not done
        PlayerLevel.setLevel(p, 30);
        PlayerLevel.setXp(p, xpToReach(ProgressionClass.PRIMORDIAL, 30));
        check(h, WukongCheckpoints.isBlocked(p), "blocked at 30 without Hell's Book");
        long before = PlayerLevel.getXp(p);
        LevelManager.grantXp(p, xpToReach(ProgressionClass.PRIMORDIAL, 35), "test");
        eq(h, 30, PlayerLevel.getLevel(p), "level held at checkpoint");
        eq(h, before, PlayerLevel.getXp(p), "XP not banked while blocked");
        WukongCheckpoints.complete(p, WukongCheckpoints.Checkpoint.HELLS_BOOK);
        check(h, !WukongCheckpoints.isBlocked(p), "unblocked once complete");
        LevelManager.grantXp(p, xpToReach(ProgressionClass.PRIMORDIAL, 35), "test");
        check(h, PlayerLevel.getLevel(p) > 30, "levels again after the checkpoint");
        h.succeed();
    }

    // ── XP from kills ─────────────────────────────────────────────

    private static void kill(GameTestHelper h, ServerPlayer killer, EntityType<? extends LivingEntity> type, String tier) {
        var victim = h.spawn(type, new BlockPos(3, 2, 3));
        if (tier != null) victim.getPersistentData().putString("jttw_enemy_tier", tier);
        LevelManager.onEntityKilled(new LivingDeathEvent(
                victim, h.getLevel().damageSources().playerAttack(killer)));
        victim.discard();
    }

    @GameTest(template = PLATFORM)
    public static void belowTwentyEveryKillPaysVanillaXp(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        kill(h, p, EntityType.ZOMBIE, null);
        eq(h, 15L, PlayerLevel.getXp(p), "standard mob (VANILLA_BASE_XP)");
        kill(h, p, EntityType.WITHER_SKELETON, null);
        eq(h, 60L, PlayerLevel.getXp(p), "plus a strong mob at 3x");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void fromTwentyOnlyTaggedEnemiesPayXp(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 20);
        long start = PlayerLevel.getXp(p);
        kill(h, p, EntityType.ZOMBIE, null);
        eq(h, start, PlayerLevel.getXp(p), "untagged mob at level 20");
        // no biome bands are registered, so a tagged kill uses the flat fallback: 15 x tier x 3
        kill(h, p, EntityType.ZOMBIE, "GENERAL");
        eq(h, start + 15L * 5 * 3, PlayerLevel.getXp(p), "tagged General");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void killsWithoutAClassOrByNonPlayersPayNothing(GameTestHelper h) {
        ServerPlayer classless = player(h);
        kill(h, classless, EntityType.ZOMBIE, null);
        eq(h, 0L, PlayerLevel.getXp(classless), "no class");
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        LevelManager.onEntityKilled(new LivingDeathEvent(
                zombie, h.getLevel().damageSources().generic()));
        eq(h, 0L, PlayerLevel.getXp(p), "environmental death");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void reapplyStatsMatchesTheCurrentLevel(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 60);
        LevelManager.reapplyStats(p);
        near(h, StatBlock.compute(ProgressionClass.HEAVENLY, 60).maxHealth(),
                p.getAttributeValue(Attributes.MAX_HEALTH), "max health at 60");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void losingTheClassRemovesLevelStats(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        p.getPersistentData().remove("jttw_prog_class");
        LevelManager.reapplyStats(p);
        near(h, 20.0, p.getAttributeValue(Attributes.MAX_HEALTH), "vanilla max health");
        near(h, 0.0, p.getAttributeValue(Attributes.ARMOR), "vanilla armor");
        h.succeed();
    }
}
