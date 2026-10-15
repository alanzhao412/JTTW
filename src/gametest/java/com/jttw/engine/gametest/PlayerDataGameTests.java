package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillPurchase;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/** What the mod saves on a player: Qi, skill points and nodes, alignment, class and level. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PlayerDataGameTests {

    // ── Qi ─────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void qiIsCappedByMaxAndNeverNegative(GameTestHelper h) {
        ServerPlayer p = player(h);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 250f);
        near(h, 100f, QiData.getCurrent(p), "current clamps to max");
        QiData.setCurrent(p, -5f);
        near(h, 0f, QiData.getCurrent(p), "current never negative");
        QiData.setMax(p, -10f);
        near(h, 0f, QiData.getMax(p), "max never negative");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void qiSpendOnlyWhenAffordable(GameTestHelper h) {
        ServerPlayer p = player(h);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 30f);
        check(h, !QiData.spend(p, 31f), "cannot spend more than current");
        near(h, 30f, QiData.getCurrent(p), "failed spend costs nothing");
        check(h, QiData.spend(p, 30f), "can spend exactly current");
        near(h, 0f, QiData.getCurrent(p), "spent down to zero");
        QiData.restore(p, 500f);
        near(h, 100f, QiData.getCurrent(p), "restore stops at max");
        h.succeed();
    }

    // ── Skill points and nodes ────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void skillPointsNeverGoNegative(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, 0, SkillTreeData.getPoints(p), "starting points");
        SkillTreeData.addPoints(p, 3);
        SkillTreeData.addPoints(p, -10);
        eq(h, 0, SkillTreeData.getPoints(p), "points after over-spending");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void grantingANodeTwiceKeepsOneCopy(GameTestHelper h) {
        ServerPlayer p = player(h);
        SkillTreeData.grantNode(p, "wu_ring_1");
        SkillTreeData.grantNode(p, "wu_ring_1");
        eq(h, Set.of("wu_ring_1"), SkillTreeData.getUnlockedNodes(p), "unlocked");
        check(h, SkillTreeData.hasNode(p, "wu_ring_1"), "hasNode");
        check(h, !SkillTreeData.hasNode(p, "wu_ring_2"), "not unlocked");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void unlockNeedsPointsPrerequisitesAndARealNode(GameTestHelper h) {
        ServerPlayer p = player(h);
        SkillTreeData.setPoints(p, 3);
        check(h, !SkillTreeData.tryUnlock(p, "wu_nothing_1"), "unknown node");
        check(h, !SkillTreeData.tryUnlock(p, "wu_ring_2"), "prerequisite wu_ring_1 missing");
        // wu_ring_1 costs 3 (WukongSkillTree)
        int cost = SkillTreeData.resolveNode("wu_ring_1").cost();
        SkillTreeData.setPoints(p, cost - 1);
        check(h, !SkillTreeData.tryUnlock(p, "wu_ring_1"), "one point short");
        SkillTreeData.setPoints(p, cost);
        check(h, SkillTreeData.tryUnlock(p, "wu_ring_1"), "affordable with prerequisites met");
        eq(h, 0, SkillTreeData.getPoints(p), "points deducted");
        SkillTreeData.setPoints(p, 99);
        check(h, !SkillTreeData.tryUnlock(p, "wu_ring_1"), "already unlocked");
        eq(h, 99, SkillTreeData.getPoints(p), "no charge for a refused unlock");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void purchaseIsLimitedToTheCharactersOwnTree(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setProgressionClass(p, ProgressionClass.PRIMORDIAL);
        SkillTreeData.setPoints(p, 50);
        check(h, !SkillPurchase.purchase(p, "dr_flight_1"), "Wukong buying a Dragon node");
        eq(h, 50, SkillTreeData.getPoints(p), "refused purchase costs nothing");
        check(h, SkillPurchase.purchase(p, "wu_ring_1"), "Wukong buying his own node");
        check(h, SkillTreeData.hasNode(p, "wu_ring_1"), "node unlocked");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void playerWithNoCharacterCannotBuyNodes(GameTestHelper h) {
        ServerPlayer p = player(h);
        SkillTreeData.setPoints(p, 50);
        check(h, !SkillPurchase.purchase(p, "wu_ring_1"), "no class, no outcome");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void vitalityNodeRaisesMaxHealth(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        SkillTreeData.setPoints(p, 50);
        float before = p.getMaxHealth();
        check(h, SkillPurchase.purchase(p, "wu_vitality_1"), "buy Vitality I");
        near(h, before + 20.0, p.getMaxHealth(), "max health after Vitality I");
        h.succeed();
    }

    // ── Alignment and reincarnation flags ──────────────────────────

    @GameTest(template = PLATFORM)
    public static void alignmentAccumulatesAndNeverGoesNegative(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.addHeavenly(p, 5);
        AlignmentData.addHeavenly(p, 3);
        AlignmentData.addYaoguai(p, 2);
        AlignmentData.addPrimordial(p, -4);
        eq(h, 8, AlignmentData.getHeavenly(p), "heavenly");
        eq(h, 0, AlignmentData.getPrimordial(p), "primordial floored at 0");
        eq(h, 10, AlignmentData.getTotal(p), "total");
        AlignmentData.setSpiritBeast(p, -7);
        eq(h, 0, AlignmentData.getSpiritBeast(p), "set floors at 0");
        AlignmentData.resetAlignment(p);
        eq(h, 0, AlignmentData.getTotal(p), "total after reset");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void markingReincarnationSavesTheOutcome(GameTestHelper h) {
        ServerPlayer p = player(h);
        check(h, !AlignmentData.hasReincarnated(p), "fresh player");
        AlignmentData.markReincarnated(p, ReincarnationOutcome.PHOENIX);
        check(h, AlignmentData.hasReincarnated(p), "reincarnated");
        eq(h, "phoenix", p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME), "outcome id");
        h.succeed();
    }

    // ── Class, level, form ─────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void levelAndXpAreClamped(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, 1, PlayerLevel.getLevel(p), "unset level reads as 1");
        PlayerLevel.setLevel(p, 400);
        eq(h, 100, PlayerLevel.getLevel(p), "level capped at 100");
        PlayerLevel.setLevel(p, -3);
        eq(h, 1, PlayerLevel.getLevel(p), "level floored at 1");
        PlayerLevel.setXp(p, -50);
        eq(h, 0L, PlayerLevel.getXp(p), "xp floored at 0");
        PlayerLevel.setFormStage(p, -1);
        eq(h, 0, PlayerLevel.getFormStage(p), "form stage floored at 0");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void classAndBeastLineRoundTrip(GameTestHelper h) {
        ServerPlayer p = player(h);
        check(h, !PlayerLevel.hasClass(p), "no class yet");
        PlayerLevel.setProgressionClass(p, ProgressionClass.DIVINE_BEAST);
        PlayerLevel.setBeastLine(p, DivineBeastLine.EARTH);
        eq(h, ProgressionClass.DIVINE_BEAST, PlayerLevel.getProgressionClass(p), "class");
        eq(h, DivineBeastLine.EARTH, PlayerLevel.getBeastLine(p), "line");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void abilitiesAreRememberedPerId(GameTestHelper h) {
        ServerPlayer p = player(h);
        PlayerLevel.grantAbility(p, "domain_sense");
        check(h, PlayerLevel.hasAbility(p, "domain_sense"), "granted");
        check(h, !PlayerLevel.hasAbility(p, "apex_domain"), "not granted");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void copyCarriesProgressToTheRespawnedPlayer(GameTestHelper h) {
        ServerPlayer before = player(h);
        ServerPlayer after = player(h);
        PlayerLevel.setProgressionClass(before, ProgressionClass.HEAVENLY);
        PlayerLevel.setLevel(before, 42);
        PlayerLevel.setXp(before, 123_456L);
        PlayerLevel.grantAbility(before, "heavenly_mobility");
        SkillTreeData.setPoints(before, 4);
        SkillTreeData.grantNode(before, "es_eye_1");
        PlayerLevel.copyTo(before, after);
        SkillTreeData.copyTo(before, after);
        eq(h, ProgressionClass.HEAVENLY, PlayerLevel.getProgressionClass(after), "class");
        eq(h, 42, PlayerLevel.getLevel(after), "level");
        eq(h, 123_456L, PlayerLevel.getXp(after), "xp");
        check(h, PlayerLevel.hasAbility(after, "heavenly_mobility"), "ability");
        eq(h, 4, SkillTreeData.getPoints(after), "points");
        check(h, SkillTreeData.hasNode(after, "es_eye_1"), "node");
        h.succeed();
    }
}
