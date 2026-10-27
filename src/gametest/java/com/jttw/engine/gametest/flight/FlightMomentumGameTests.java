package com.jttw.engine.gametest.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.FlightMomentum;
import com.jttw.engine.flight.FlightSteering;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Each flies as fast as who they are: held at full speed in their highest flight, they build to it. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlightMomentumGameTests {

    /** Blocks a second. */
    private static double top(GameTestHelper h, ReincarnationOutcome who) {
        return FlightMomentum.top(as(h, who)) * 20;
    }

    @GameTest(template = PLATFORM)
    public static void eachHasTheirOwnTopSpeed(GameTestHelper h) {
        near(h, 150, top(h, ReincarnationOutcome.CELESTIAL_PRIMATE), "Wukong");
        near(h, 150, top(h, ReincarnationOutcome.GOLD_WINGED_PENG), "the Peng");
        near(h, 150, top(h, ReincarnationOutcome.EYE_OF_HEAVEN), "Erlang");
        near(h, 120, top(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE), "Nezha");
        near(h, 100, top(h, ReincarnationOutcome.DRAGON), "the Dragon");
        near(h, 100, top(h, ReincarnationOutcome.PHOENIX), "the Phoenix");
        near(h, 100, top(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY), "Heaven's Marshal");
        near(h, 100, top(h, ReincarnationOutcome.YUAN_DI), "Yuan Di");
        near(h, 100, top(h, ReincarnationOutcome.WHITE_TIGER), "the White Tiger");
        for (ReincarnationOutcome bulky : new ReincarnationOutcome[] {ReincarnationOutcome.BULL_DEMON_KING,
                ReincarnationOutcome.SAURIAN_DEMON_KING, ReincarnationOutcome.TURTLE, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS}) {
            near(h, 65, top(h, bulky), bulky.id + ", great and heavy");
        }
        h.succeed();
    }

    /** Flies {@code p} at {@code fraction} of their speed for {@code ticks}, as their client would. */
    private static void fly(ServerPlayer p, double fraction, int ticks) {
        for (int i = 0; i < ticks; i++) {
            double v = JTTWFlight.profile(p).effectiveSpeed() * fraction;
            p.setPos(p.getX(), p.getY(), p.getZ() + v);
            FlightMomentum.tick(p);
        }
    }

    private static ServerPlayer flying(GameTestHelper h, ReincarnationOutcome who, int tier) {
        ServerPlayer p = as(h, who);
        JTTWFlight.setUniversalFlight(p, true);
        if (tier > 0) JTTWFlight.setFlightSpeedTier(p, tier);
        p.getAbilities().flying = true;
        FlightMomentum.tick(p); // where they start from
        return p;
    }

    @GameTest(template = PLATFORM)
    public static void erlangAtCelestialGaleBuildsTo150InTenSeconds(GameTestHelper h) {
        ServerPlayer p = flying(h, ReincarnationOutcome.EYE_OF_HEAVEN, 3);
        near(h, FlightSteering.FAST_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "starts at his swiftest tier");
        fly(p, 1.0, 210);
        near(h, 150, JTTWFlight.profile(p).effectiveSpeed() * 20, "150 after 10 seconds");
        fly(p, 0.0, 60);
        near(h, FlightSteering.FAST_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "stopping loses it");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nezhaBuildsTo120(GameTestHelper h) {
        ServerPlayer p = flying(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 3);
        fly(p, 1.0, 210);
        near(h, 120, JTTWFlight.profile(p).effectiveSpeed() * 20, "Nezha's top");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theBulkyBuildTo65(GameTestHelper h) {
        ServerPlayer p = flying(h, ReincarnationOutcome.BULL_DEMON_KING, 0); // flight with no tier, as a form's wings
        near(h, FlightSteering.FORM_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "starts at a form's speed");
        fly(p, 1.0, 210);
        near(h, 65, JTTWFlight.profile(p).effectiveSpeed() * 20, "the Bull Demon King's top");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aLowerTierDoesNotBuild(GameTestHelper h) {
        ServerPlayer p = flying(h, ReincarnationOutcome.DRAGON, 2);
        fly(p, 1.0, 210);
        near(h, FlightSteering.CRUISE_SPEED, JTTWFlight.profile(p).effectiveSpeed(), "Dragon Flight stays at cruising");
        h.succeed();
    }
}
