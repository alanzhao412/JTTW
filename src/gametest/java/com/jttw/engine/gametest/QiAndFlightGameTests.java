package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.HeavenVoidHandler;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillPurchase;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/** Qi regeneration, flight granted by skill nodes, and the mod's dimensions. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class QiAndFlightGameTests {

    private static void qiTick(ServerPlayer p) {
        QiManager.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
    }

    // ── Qi ─────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void maxQiFollowsTheStatBlock(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 40);
        qiTick(p);
        near(h, (float) StatBlock.compute(ProgressionClass.HEAVENLY, 40).qi(), QiData.getMax(p), "max Qi at 40");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void qiRegeneratesUpToMaxAndStops(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        qiTick(p);
        QiData.setCurrent(p, 0f);
        qiTick(p);
        float afterOne = QiData.getCurrent(p);
        check(h, afterOne > 0f, "regenerates from empty");
        for (int i = 0; i < 100_000 && QiData.getCurrent(p) < QiData.getMax(p); i++) qiTick(p);
        near(h, QiData.getMax(p), QiData.getCurrent(p), "fills exactly to max");
        qiTick(p);
        near(h, QiData.getMax(p), QiData.getCurrent(p), "never above max");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void noClassMeansNoQi(GameTestHelper h) {
        ServerPlayer p = player(h);
        qiTick(p);
        near(h, 0f, QiData.getMax(p), "max");
        near(h, 0f, QiData.getCurrent(p), "current");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eachWukongQiMasteryNodeAddsFifteenPercentRegen(GameTestHelper h) {
        float[] perTick = new float[3];
        for (int nodes = 0; nodes <= 2; nodes++) {
            ServerPlayer p = player(h);
            LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
            if (nodes >= 1) SkillTreeData.grantNode(p, "wu_qi_1");
            if (nodes >= 2) SkillTreeData.grantNode(p, "wu_qi_2");
            qiTick(p);
            QiData.setCurrent(p, 0f);
            qiTick(p);
            perTick[nodes] = QiData.getCurrent(p);
        }
        near(h, 1.15, perTick[1] / perTick[0], "one node");
        near(h, 1.30, perTick[2] / perTick[0], "two nodes (additive)");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void otherCharactersQiNodesDoNotBoostRegen(GameTestHelper h) {
        ServerPlayer plain = player(h);
        ServerPlayer withNodes = player(h);
        for (ServerPlayer p : new ServerPlayer[]{plain, withNodes}) {
            LevelManager.initClass(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        }
        SkillTreeData.grantNode(withNodes, "nz_qi_1");
        for (ServerPlayer p : new ServerPlayer[]{plain, withNodes}) {
            qiTick(p);
            QiData.setCurrent(p, 0f);
            qiTick(p);
        }
        near(h, QiData.getCurrent(plain), QiData.getCurrent(withNodes), "Nezha's Qi node is max-Qi only");
        h.succeed();
    }

    // ── Flight from skill nodes ───────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void firstFlightNodeGivesGlideThenSecondGivesFullFlight(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        SkillTreeData.setPoints(p, 50);
        check(h, !JTTWFlight.hasUniversalFlight(p), "no flight before the node");
        check(h, SkillPurchase.purchase(p, "wu_flight_1"), "buy Cloud Step");
        check(h, JTTWFlight.hasUniversalFlight(p), "flight granted");
        check(h, JTTWFlight.isGlideOnly(p), "tier 1 is glide only");
        check(h, SkillPurchase.purchase(p, "wu_flight_2"), "buy Nimbus");
        check(h, !JTTWFlight.isGlideOnly(p), "tier 2 is full flight");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nonFlightNodesDoNotGrantFlight(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        SkillTreeData.setPoints(p, 50);
        check(h, SkillPurchase.purchase(p, "wu_ring_1"), "buy Ring of Fire I");
        check(h, !JTTWFlight.hasUniversalFlight(p), "no flight");
        h.succeed();
    }

    // ── Dimensions ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void heavenAndHellAreLoaded(GameTestHelper h) {
        check(h, h.getLevel().getServer().getLevel(JTTWDimensions.HEAVEN) != null, "heaven");
        check(h, h.getLevel().getServer().getLevel(JTTWDimensions.HELL) != null, "hell");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void teleportToHellMovesThePlayerThere(GameTestHelper h) {
        ServerPlayer p = player(h);
        JTTWDimensions.teleportTo(p, JTTWDimensions.HELL);
        eq(h, JTTWDimensions.HELL, p.level().dimension(), "dimension");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void fallingOutOfHeavenReturnsToTheOverworld(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerLevel heaven = h.getLevel().getServer().getLevel(JTTWDimensions.HEAVEN);
        p.teleportTo(heaven, 0.5, -200, 0.5, 0, 0);
        p.fallDistance = 50f;
        HeavenVoidHandler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        eq(h, Level.OVERWORLD, p.level().dimension(), "dimension");
        near(h, 0.0, p.fallDistance, "fall damage cancelled");
        h.succeed();
    }
}
