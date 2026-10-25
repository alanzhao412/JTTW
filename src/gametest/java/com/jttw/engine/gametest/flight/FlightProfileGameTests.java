package com.jttw.engine.gametest.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.divinebeast.TrueForm;
import com.jttw.engine.flight.FlightSteering;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The flight profile the server decides, and sends to the client that moves the player. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlightProfileGameTests {

    private static ServerPlayer flier(GameTestHelper h, ReincarnationOutcome who, int tier) {
        ServerPlayer p = as(h, who);
        JTTWFlight.setUniversalFlight(p, true);
        if (tier > 0) JTTWFlight.setFlightSpeedTier(p, tier);
        p.setYRot(0f); p.setXRot(0f);
        return p;
    }

    @GameTest(template = PLATFORM)
    public static void eachFlierGetsTheirTiersSpeed(GameTestHelper h) {
        near(h, FlightSteering.WUKONG_FAST_SPEED,
                JTTWFlight.profile(flier(h, ReincarnationOutcome.CELESTIAL_PRIMATE, 3)).speed(), "Wukong at III");
        near(h, FlightSteering.FAST_SPEED,
                JTTWFlight.profile(flier(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 3)).speed(), "Nezha at III");
        near(h, FlightSteering.CRUISE_SPEED,
                JTTWFlight.profile(flier(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE, 2)).speed(), "Nezha at II");
        near(h, FlightSteering.FORM_SPEED,
                JTTWFlight.profile(flier(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY, 0)).speed(), "a flying form's own wings");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFirstFlightTierOnlyGlides(GameTestHelper h) {
        ServerPlayer p = flier(h, ReincarnationOutcome.DRAGON, 1);
        JTTWFlight.setGlideOnly(p, true);
        check(h, JTTWFlight.profile(p).glide(), "glide only");
        h.succeed();
    }

    /** Flying a few ticks looking up and pressing forward: how fast it climbs (a glider never does). */
    private static double climb(ServerPlayer p) {
        p.setXRot(-45f);
        JTTWFlight.onServerPlayerTick(p, TransformationManager.getTransformation(p)); // may fly
        p.getAbilities().flying = true;
        p.zza = 1f;
        for (int i = 0; i < 10; i++) JTTWFlight.onServerPlayerTick(p, TransformationManager.getTransformation(p));
        double y = p.getDeltaMovement().y;
        p.getAbilities().flying = false;
        p.zza = 0f;
        p.setXRot(0f);
        return y;
    }

    /** The Azure Dragon, the Dragon's true form, flies in it even when the Dragon only has Elytra Glide. */
    @GameTest(template = PLATFORM, batch = "theAzureDragonFliesWithOnlyElytraGlide")
    public static void theAzureDragonFliesWithOnlyElytraGlide(GameTestHelper h) {
        ServerPlayer p = flier(h, ReincarnationOutcome.DRAGON, 1);
        JTTWFlight.setGlideOnly(p, true);
        qi(p, 100f, 100f);
        check(h, climb(p) <= 0, "in its own shape, Elytra Glide only glides");
        check(h, TrueForm.toggle(p, ModEntities.AZURE_DRAGON.get(), 1, "dr_true"), "the Azure Dragon");
        check(h, !JTTWFlight.profile(p).glide(), "the Azure Dragon flies, never glides");
        check(h, climb(p) > 0, "and climbs");
        TrueForm.end(p);
        check(h, JTTWFlight.profile(p).glide(), "itself again, it glides");
        h.succeed();
    }

    /** A flying form of the 18 Transformations flies on its own wings, whatever its taker's glide. */
    @GameTest(template = PLATFORM, batch = "aFlyingFormFliesForAGlideOnlyDragon")
    public static void aFlyingFormFliesForAGlideOnlyDragon(GameTestHelper h) {
        ServerPlayer p = flier(h, ReincarnationOutcome.DRAGON, 1);
        JTTWFlight.setGlideOnly(p, true);
        TransformationManager.transform(p, EntityType.PARROT);
        check(h, TransformationManager.getTransformation(p).canFly(), "a parrot has wings");
        check(h, !JTTWFlight.profile(p).glide(), "a parrot flies, never glides");
        check(h, climb(p) > 0, "and climbs");
        TransformationManager.demorph(p);
        check(h, JTTWFlight.profile(p).glide(), "itself again, it glides");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongReachesHisTopSpeedInFlight(GameTestHelper h) {
        ServerPlayer p = flier(h, ReincarnationOutcome.CELESTIAL_PRIMATE, 3);
        JTTWFlight.onServerPlayerTick(p, null); // may fly
        p.getAbilities().flying = true;
        p.zza = 1f;
        for (int i = 0; i < 100; i++) JTTWFlight.onServerPlayerTick(p, null);
        near(h, FlightSteering.WUKONG_FAST_SPEED, p.getDeltaMovement().length(), "2.8 blocks a tick, not capped at 2.25");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBoostRidesOnTopOfTheSpeed(GameTestHelper h) {
        ServerPlayer p = flier(h, ReincarnationOutcome.GOLD_WINGED_PENG, 2);
        JTTWFlight.setBoost(p, 2.0, true);
        near(h, FlightSteering.CRUISE_SPEED * 2, JTTWFlight.profile(p).effectiveSpeed(), "boosted");
        check(h, JTTWFlight.profile(p).thrust(), "driven forward");
        JTTWFlight.setBoost(p, 1, false);
        near(h, 1.0, JTTWFlight.profile(p).boost(), "and back");
        h.succeed();
    }
}
