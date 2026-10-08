package com.jttw.engine.gametest.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongAbilityHandler;
import com.jttw.engine.flight.FlightSteering;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Wukong's Flight III: held at full speed, his somersault builds to the top speed anyone flies. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class SomersaultMomentumGameTests {

    private static ServerPlayer wukongInFlight(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        JTTWFlight.setUniversalFlight(p, true);
        JTTWFlight.setFlightSpeedTier(p, 3);
        p.getAbilities().flying = true;
        WukongAbilityHandler.tickMomentum(p); // where he starts from
        return p;
    }

    /** Moves him as his client would at {@code fraction} of his current speed, for a tick. */
    private static void fly(ServerPlayer p, double fraction) {
        double v = JTTWFlight.profile(p).effectiveSpeed() * fraction;
        p.setPos(p.getX(), p.getY(), p.getZ() + v);
        WukongAbilityHandler.tickMomentum(p);
    }

    @GameTest(template = PLATFORM)
    public static void heldAtFullSpeedItBuildsToTheTopOverTenSeconds(GameTestHelper h) {
        ServerPlayer p = wukongInFlight(h);
        near(h, FlightSteering.WUKONG_FAST_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "starts at 2.8");
        for (int i = 0; i < 100; i++) fly(p, 1.0);
        double half = JTTWFlight.profile(p).effectiveSpeed();
        check(h, half > 4.0 && half < 4.4, "halfway after 5 seconds: " + half);
        for (int i = 0; i < 100; i++) fly(p, 1.0);
        near(h, FlightSteering.TOP_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "the Peng's top speed after 10");
        for (int i = 0; i < 40; i++) fly(p, 1.0);
        near(h, FlightSteering.TOP_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "and no further");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void fallingShortOfFullSpeedLosesTheMomentum(GameTestHelper h) {
        ServerPlayer p = wukongInFlight(h);
        for (int i = 0; i < 200; i++) fly(p, 1.0);
        for (int i = 0; i < 20; i++) fly(p, 0.5);
        double slowed = JTTWFlight.profile(p).effectiveSpeed();
        check(h, slowed < 4.3, "half the momentum gone in a second: " + slowed);
        for (int i = 0; i < 40; i++) fly(p, 0.0);
        near(h, FlightSteering.WUKONG_FAST_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "back to 2.8 when he stops");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyFlightThreeBuildsMomentum(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        JTTWFlight.setUniversalFlight(p, true);
        JTTWFlight.setFlightSpeedTier(p, 2);
        p.getAbilities().flying = true;
        WukongAbilityHandler.tickMomentum(p);
        for (int i = 0; i < 100; i++) fly(p, 1.0);
        near(h, 1.0, JTTWFlight.profile(p).boost(), "tier II: none");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongsMomentumLeavesThePengsWingbeatsAlone(GameTestHelper h) {
        ServerPlayer peng = as(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        JTTWFlight.setBoost(peng, 2.5, true);
        WukongAbilityHandler.tickMomentum(peng);
        near(h, 2.5, JTTWFlight.profile(peng).boost(), "untouched");
        h.succeed();
    }
}
