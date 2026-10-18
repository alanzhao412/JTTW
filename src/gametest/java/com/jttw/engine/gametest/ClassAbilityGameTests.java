package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongAbilityHandler;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.flight.FlightSteering;
import com.jttw.engine.flight.FlyingState;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.AbilityUnlock;
import com.jttw.engine.progression.skilltree.SkillPurchase;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** Class abilities bought with skill points once their level is reached, and the Heavenly cloud. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ClassAbilityGameTests {

    private static final String MOBILITY = JTTWFlight.HEAVENLY_MOBILITY;

    /** {@code who}, reincarnated and levelled to {@code level} through XP, as a player gets there. */
    private static ServerPlayer at(GameTestHelper h, ReincarnationOutcome who, int level) {
        ServerPlayer p = as(h, who);
        ProgressionClass cls = PlayerLevel.getProgressionClass(p);
        if (level > 1) LevelManager.grantXp(p, cls.growthRate.totalXpForLevel(level), "test");
        return p;
    }

    @GameTest(template = PLATFORM)
    public static void reachingTheLevelOnlyMakesItAvailable(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY, 10);
        eq(h, 10, PlayerLevel.getLevel(p), "level");
        check(h, !PlayerLevel.hasAbility(p, MOBILITY), "not given at the level");
        eq(h, 2, SkillTreeData.getPoints(p), "the level's points are there to spend");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void cannotBuyBeforeTheLevel(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY, 9);
        SkillTreeData.setPoints(p, 10);
        check(h, !SkillPurchase.purchaseClassAbility(p, MOBILITY), "level 9 cannot buy a level-10 ability");
        check(h, !PlayerLevel.hasAbility(p, MOBILITY), "not held");
        eq(h, 10, SkillTreeData.getPoints(p), "no points spent");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void cannotBuyWithoutThePoints(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY, 10);
        SkillTreeData.setPoints(p, 0);
        check(h, !SkillPurchase.purchaseClassAbility(p, MOBILITY), "no points, no ability");
        check(h, !PlayerLevel.hasAbility(p, MOBILITY), "not held");
        // a level-100 ability costs 3: two points are not enough
        PlayerLevel.setLevel(p, 100);
        SkillTreeData.setPoints(p, 2);
        eq(h, 3, AbilityUnlock.byId("mandate_of_heaven").cost(), "level-100 cost");
        check(h, !SkillPurchase.purchaseClassAbility(p, "mandate_of_heaven"), "2 of 3 points");
        eq(h, 2, SkillTreeData.getPoints(p), "no points spent");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void anotherClassCannotBuyIt(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.TURTLE, 10); // a Divine Beast
        SkillTreeData.setPoints(p, 10);
        check(h, !SkillPurchase.purchaseClassAbility(p, MOBILITY), "a Heavenly ability");
        check(h, !SkillPurchase.purchaseClassAbility(p, "no_such_ability"), "an unknown id");
        check(h, !PlayerLevel.hasAbility(p, MOBILITY), "not held");
        eq(h, 10, SkillTreeData.getPoints(p), "no points spent");
        check(h, SkillPurchase.purchaseClassAbility(p, "lesser_form"), "but its own class's, yes");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void buyingSpendsPointsAndGrantsIt(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY, 10);
        eq(h, 1, AbilityUnlock.byId(MOBILITY).cost(), "level-10 cost");
        check(h, SkillPurchase.purchaseClassAbility(p, MOBILITY), "bought");
        check(h, PlayerLevel.hasAbility(p, MOBILITY), "held");
        eq(h, 1, SkillTreeData.getPoints(p), "one point spent, from the tree's pool");
        check(h, !SkillPurchase.purchaseClassAbility(p, MOBILITY), "not twice");
        eq(h, 1, SkillTreeData.getPoints(p), "nothing spent the second time");
        h.succeed();
    }

    /** Saves from before abilities were bought had them given at the level: they keep them. */
    @GameTest(template = PLATFORM)
    public static void abilitiesGivenOnOldSavesAreKept(GameTestHelper h) {
        ServerPlayer before = at(h, ReincarnationOutcome.EYE_OF_HEAVEN, 9);
        PlayerLevel.grantAbility(before, MOBILITY); // as the old level-up did
        int points = SkillTreeData.getPoints(before);
        LevelManager.grantXp(before, ProgressionClass.HEAVENLY.growthRate.totalXpForLevel(25), "test");
        check(h, PlayerLevel.hasAbility(before, MOBILITY), "kept through level-ups");
        LevelManager.reapplyStats(before);
        LevelManager.syncToClient(before); // as on login
        check(h, PlayerLevel.hasAbility(before, MOBILITY), "kept through login");
        ServerPlayer after = player(h);
        PlayerLevel.copyTo(before, after);
        SkillTreeData.copyTo(before, after);
        check(h, PlayerLevel.hasAbility(after, MOBILITY), "kept through death");
        int now = SkillTreeData.getPoints(after);
        eq(h, points + 4, now, "levels 10 to 25 paid their four points, none taken for it");
        check(h, !SkillPurchase.purchaseClassAbility(after, MOBILITY), "already held: not bought again");
        eq(h, now, SkillTreeData.getPoints(after), "no points spent");
        check(h, JTTWFlight.hasHeavenlyCloud(after), "and it gives the cloud");
        h.succeed();
    }

    // ── Heavenly Mobility: the cloud ──────────────────────────────

    /** Flies {@code p} forward, sprinting, for {@code ticks} server ticks; returns their velocity. */
    private static double flyForward(ServerPlayer p, int ticks) {
        for (int i = 0; i < ticks; i++) {
            p.zza = 1f;
            JTTWFlight.onServerPlayerTick(p, null);
        }
        return p.getDeltaMovement().length();
    }

    @GameTest(template = PLATFORM)
    public static void heavenlyMobilityIsACloudSlowerThanWukongs(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY, 10);
        JTTWFlight.onServerPlayerTick(p, null);
        check(h, !p.getAbilities().mayfly, "no flight before it is bought");

        check(h, SkillPurchase.purchaseClassAbility(p, MOBILITY), "bought");
        JTTWFlight.onServerPlayerTick(p, null);
        check(h, p.getAbilities().mayfly, "it lets him take off");
        p.getAbilities().flying = true;
        p.setXRot(0);
        p.setYRot(0);
        eq(h, FlyingState.FLYING, FlyingState.mode(p), "in the air, not sprinting: no cloud yet");

        p.setSprinting(true); // summoned as Wukong's is: sprinting through the air
        eq(h, FlyingState.ON_HEAVENLY_CLOUD, FlyingState.mode(p), "on his cloud");
        near(h, FlightSteering.HEAVENLY_CLOUD_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "the cloud's speed");
        double v = flyForward(p, 60);
        near(h, FlightSteering.HEAVENLY_CLOUD_SPEED, v, "it carries him at the cloud's speed");

        // Wukong's cloud: Flight III, sprinting, his somersault momentum at its height
        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        SkillTreeData.setPoints(wukong, 50);
        for (String n : new String[]{WukongSkillTree.FLIGHT_1, WukongSkillTree.FLIGHT_2, WukongSkillTree.FLIGHT_3}) {
            SkillPurchase.purchase(wukong, n);
        }
        wukong.getAbilities().flying = true;
        wukong.setSprinting(true);
        eq(h, FlyingState.ON_CLOUD, FlyingState.mode(wukong), "Wukong on his");
        double wukongStart = JTTWFlight.profile(wukong).effectiveSpeed();
        JTTWFlight.setBoost(wukong, WukongAbilityHandler.momentumMax(), false);
        double wukongTop = JTTWFlight.profile(wukong).effectiveSpeed();
        near(h, FlightSteering.TOP_SPEED, wukongTop, "Wukong's cloud at its height");
        check(h, v < wukongStart, "slower than Wukong's cloud even as he first takes it");
        near(h, wukongTop / 3, v, "a third of Wukong's top speed");

        p.getAbilities().flying = false;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(p), "and none sprinting on the ground");
        h.succeed();
    }

    /** The cloud never makes anyone worse: one who flies faster on their own keeps their own speed and look. */
    @GameTest(template = PLATFORM)
    public static void aFasterFlierKeepsTheirOwnFlight(GameTestHelper h) {
        // Erlang with Flight III (2.40): his own flight, in his squall
        ServerPlayer erlang = at(h, ReincarnationOutcome.EYE_OF_HEAVEN, 10);
        SkillTreeData.setPoints(erlang, 50);
        for (String n : new String[]{com.jttw.engine.character.erlang.ErlangShenSkillTree.FLIGHT_1,
                com.jttw.engine.character.erlang.ErlangShenSkillTree.FLIGHT_2,
                com.jttw.engine.character.erlang.ErlangShenSkillTree.FLIGHT_3}) {
            check(h, SkillPurchase.purchase(erlang, n), "buy " + n);
        }
        check(h, SkillPurchase.purchaseClassAbility(erlang, MOBILITY), "Heavenly Mobility bought");
        erlang.getAbilities().flying = true;
        erlang.setSprinting(true);
        eq(h, FlyingState.IN_SQUALL, FlyingState.mode(erlang), "Erlang keeps his squall");
        near(h, FlightSteering.FAST_SPEED, JTTWFlight.profile(erlang).effectiveSpeed(), "and his own speed");
        near(h, FlightSteering.FAST_SPEED, flyForward(erlang, 60), "and flies at it");

        // Nezha at the fastest tier: his own flight, stretched out along it
        ServerPlayer nezha = at(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 10);
        JTTWFlight.setUniversalFlight(nezha, true);
        JTTWFlight.setFlightSpeedTier(nezha, 3);
        check(h, SkillPurchase.purchaseClassAbility(nezha, MOBILITY), "Heavenly Mobility bought");
        nezha.getAbilities().flying = true;
        nezha.setSprinting(true);
        eq(h, FlyingState.FLYING, FlyingState.mode(nezha), "Nezha keeps his own look");
        near(h, FlightSteering.FAST_SPEED, JTTWFlight.profile(nezha).effectiveSpeed(), "and his own speed");
        h.succeed();
    }

    /** One whose own flight is slower than the cloud rides it. */
    @GameTest(template = PLATFORM)
    public static void aSlowerFlierRidesTheCloud(GameTestHelper h) {
        ServerPlayer nezha = at(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 10);
        JTTWFlight.setUniversalFlight(nezha, true);
        JTTWFlight.setFlightSpeedTier(nezha, 2); // his full flight on the wheels, 1.55
        nezha.getAbilities().flying = true;
        nezha.setSprinting(true);
        eq(h, FlyingState.FLYING, FlyingState.mode(nezha), "before the ability, his own flight");
        near(h, FlightSteering.CRUISE_SPEED, JTTWFlight.profile(nezha).effectiveSpeed(), "at his own speed");
        check(h, SkillPurchase.purchaseClassAbility(nezha, MOBILITY), "Heavenly Mobility bought");
        eq(h, FlyingState.ON_HEAVENLY_CLOUD, FlyingState.mode(nezha), "now on the cloud");
        near(h, FlightSteering.HEAVENLY_CLOUD_SPEED, JTTWFlight.profile(nezha).effectiveSpeed(), "at the cloud's speed");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyAHeavenlyHasTheCloud(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.BULL_DEMON_KING, 10);
        PlayerLevel.grantAbility(p, MOBILITY); // held some other way: still not his class's
        check(h, !JTTWFlight.hasHeavenlyCloud(p), "a Yaoguai has no Heavenly cloud");
        JTTWFlight.onServerPlayerTick(p, null);
        check(h, !p.getAbilities().mayfly, "nor flight from it");
        h.succeed();
    }
}
