package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformTierFilter;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/**
 * Who may take which form: the tier filter (72 Transformations I/II/III), the Marshal of the
 * Heavenly Canopy's separate 36 Transformations filter, and the server's handling of a
 * client's transform request.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransformAccessGameTests {

    // ── TransformTierFilter ───────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void tierOneAllowsGroundAndWaterMobsOnly(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.COW, EntityType.ZOMBIE, EntityType.SQUID,
                EntityType.IRON_GOLEM, EntityType.DOLPHIN, EntityType.CREEPER}) {
            check(h, TransformTierFilter.isAllowedAtTier(t, 1), t + " at tier 1");
        }
        for (EntityType<?> t : new EntityType<?>[]{EntityType.GHAST, EntityType.BLAZE, EntityType.PHANTOM,
                EntityType.BEE, EntityType.PARROT, EntityType.BAT, EntityType.VEX, EntityType.ALLAY}) {
            check(h, !TransformTierFilter.isAllowedAtTier(t, 1), t + " (flying) refused at tier 1");
        }
        for (EntityType<?> t : TransformTierFilter.BOSS_MOBS) {
            check(h, !TransformTierFilter.isAllowedAtTier(t, 1), t + " (boss) refused at tier 1");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void tierTwoAddsFlyersButNotBosses(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.GHAST, EntityType.BLAZE, EntityType.PHANTOM,
                EntityType.BEE, EntityType.PARROT, EntityType.BAT, EntityType.VEX, EntityType.ALLAY, EntityType.COW}) {
            check(h, TransformTierFilter.isAllowedAtTier(t, 2), t + " at tier 2");
        }
        // bosses, flying (dragon, wither) or not (warden, elder guardian)
        for (EntityType<?> t : new EntityType<?>[]{EntityType.ENDER_DRAGON, EntityType.WITHER,
                EntityType.WARDEN, EntityType.ELDER_GUARDIAN}) {
            check(h, !TransformTierFilter.isAllowedAtTier(t, 2), t + " (boss) refused at tier 2");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void tierThreeAllowsBosses(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.ENDER_DRAGON, EntityType.WITHER,
                EntityType.WARDEN, EntityType.ELDER_GUARDIAN, EntityType.GHAST, EntityType.COW}) {
            check(h, TransformTierFilter.isAllowedAtTier(t, 3), t + " at tier 3");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void tooSmallMeansShorterThanThePlayer(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.PIG, EntityType.COW, EntityType.SPIDER,
                EntityType.WOLF, EntityType.CHICKEN}) {
            check(h, TransformTierFilter.isTooSmall(t), t + " is too small");
        }
        for (EntityType<?> t : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.IRON_GOLEM,
                EntityType.VILLAGER, EntityType.ENDERMAN}) {
            check(h, !TransformTierFilter.isTooSmall(t), t + " is tall enough");
        }
        // exactly player height (1.8) is not "shorter than the player"
        check(h, !TransformTierFilter.isTooSmall(EntityType.BLAZE), "blaze (1.8 tall) is not too small");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void marshalFilterBlocksNetherAndBossFormsAndAllowsTheRest(GameTestHelper h) {
        for (EntityType<?> t : TransformTierFilter.MARSHAL_BLOCKED_NETHER) {
            check(h, !TransformTierFilter.isAllowedForMarshal(t), t + " (nether) blocked");
        }
        for (EntityType<?> t : TransformTierFilter.MARSHAL_BLOCKED_BOSSES) {
            check(h, !TransformTierFilter.isAllowedForMarshal(t), t + " (boss) blocked");
        }
        for (EntityType<?> t : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER}) {
            check(h, TransformTierFilter.isAllowedForMarshal(t), t + " (allow-listed hostile) allowed");
        }
        for (EntityType<?> t : new EntityType<?>[]{EntityType.COW, EntityType.SQUID, EntityType.WOLF,
                EntityType.IRON_GOLEM, EntityType.VILLAGER, EntityType.LLAMA}) {
            check(h, TransformTierFilter.isAllowedForMarshal(t), t + " (passive/neutral) allowed");
        }
        h.succeed();
    }

    /**
     * SUSPECTED BUG: isAllowedForMarshal() means to block every hostile (Monster) form except
     * zombie, skeleton and spider, but it tests {@code Monster.class.isAssignableFrom(type.getBaseClass())}
     * and EntityType.getBaseClass() is always Entity.class, so no hostile is ever blocked.
     */
    @GameTest(template = PLATFORM, required = false)
    public static void marshalFilterBlocksHostilesOutsideItsAllowList(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.CREEPER, EntityType.ENDERMAN,
                EntityType.WITCH, EntityType.HUSK, EntityType.STRAY, EntityType.CAVE_SPIDER}) {
            check(h, !TransformTierFilter.isAllowedForMarshal(t), t + " (hostile) should be blocked");
        }
        h.succeed();
    }

    /** SUSPECTED BUG (same cause): a Marshal's request for a tall hostile such as the enderman is granted. */
    @GameTest(template = PLATFORM, required = false)
    public static void marshalIsRefusedATallHostileForm(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        Tf.send(p, Tf.request(EntityType.ENDERMAN));
        check(h, formOf(p) == null, "enderman should be refused, but became " + formOf(p));
        h.succeed();
    }

    // ── RequestTransformPacket (server side) ──────────────────────

    @GameTest(template = PLATFORM)
    public static void requestAtTierOneTakesAGroundForm(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        Tf.grantWukongTier(p, 1);
        Tf.send(p, Tf.request(EntityType.COW));
        eq(h, EntityType.COW, formOf(p), "cow form");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void requestAtTierOneRefusesFlyersAndBosses(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        Tf.grantWukongTier(p, 1);
        Tf.send(p, Tf.request(EntityType.GHAST));
        check(h, formOf(p) == null, "ghast refused at tier 1");
        Tf.send(p, Tf.request(EntityType.WARDEN));
        check(h, formOf(p) == null, "warden refused at tier 1");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void requestAtTierTwoTakesFlyersButNotBosses(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        Tf.grantWukongTier(p, 2);
        Tf.send(p, Tf.request(EntityType.ENDER_DRAGON));
        check(h, formOf(p) == null, "dragon refused at tier 2");
        Tf.send(p, Tf.request(EntityType.WARDEN));
        check(h, formOf(p) == null, "warden refused at tier 2");
        Tf.send(p, Tf.request(EntityType.GHAST));
        eq(h, EntityType.GHAST, formOf(p), "ghast at tier 2");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void requestAtTierThreeTakesABoss(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        Tf.grantWukongTier(p, 3);
        Tf.send(p, Tf.request(EntityType.WITHER));
        eq(h, EntityType.WITHER, formOf(p), "wither at tier 3");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangShenTransformTierCountsToo(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        Tf.grantErlangTier(p, 2);
        Tf.send(p, Tf.request(EntityType.BEE));
        eq(h, EntityType.BEE, formOf(p), "bee at Erlang tier 2");
        Tf.send(p, Tf.request(EntityType.ELDER_GUARDIAN));
        eq(h, EntityType.BEE, formOf(p), "elder guardian refused, bee kept");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aRefusedRequestKeepsTheCurrentForm(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 1);
        Tf.send(p, Tf.request(EntityType.WOLF));
        Tf.send(p, Tf.request(EntityType.PHANTOM));
        eq(h, EntityType.WOLF, formOf(p), "still a wolf");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void requestForAPlayerFormIsRefused(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        Tf.send(p, Tf.request(EntityType.PLAYER));
        check(h, formOf(p) == null, "player form refused");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void entityRequestMustNameALivingThing(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        Tf.send(p, Tf.request(EntityType.MINECART));
        check(h, formOf(p) == null, "ENTITY request for a minecart refused");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void objectRequestMustNameANonLivingThing(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        Tf.send(p, Tf.requestObject(EntityType.COW));
        check(h, formOf(p) == null, "OBJECT_ENTITY request for a cow refused");
        Tf.send(p, Tf.requestObject(EntityType.MINECART));
        eq(h, EntityType.MINECART, formOf(p), "minecart object form");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void marshalTakesTallAllowedForms(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        Tf.send(p, Tf.request(EntityType.ZOMBIE));
        eq(h, EntityType.ZOMBIE, formOf(p), "zombie (allow-listed hostile)");
        Tf.send(p, Tf.request(EntityType.IRON_GOLEM));
        eq(h, EntityType.IRON_GOLEM, formOf(p), "iron golem (neutral)");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void marshalIsRefusedSmallNetherAndBossForms(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        Tf.send(p, Tf.request(EntityType.PIG));
        check(h, formOf(p) == null, "pig refused (smaller than the player)");
        Tf.send(p, Tf.request(EntityType.WITHER_SKELETON));
        check(h, formOf(p) == null, "wither skeleton refused (nether)");
        Tf.send(p, Tf.request(EntityType.WARDEN));
        check(h, formOf(p) == null, "warden refused (boss)");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void marshalIgnoresTransformTiers(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        Tf.grantWukongTier(p, 3);
        Tf.send(p, Tf.request(EntityType.ENDER_DRAGON));
        check(h, formOf(p) == null, "Marshal cannot take a boss even with tier-3 nodes");
        h.succeed();
    }

    /**
     * SUSPECTED BUG: the Marshal filter allow-lists the spider among hostiles, but the size
     * filter the server applies first (spider is 0.9 tall) always refuses it, so the allow-list
     * entry can never be used.
     */
    @GameTest(template = PLATFORM, required = false)
    public static void marshalCanTakeTheAllowListedSpider(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        Tf.send(p, Tf.request(EntityType.SPIDER));
        eq(h, EntityType.SPIDER, formOf(p), "spider form for the Marshal");
        h.succeed();
    }

    /**
     * SUSPECTED BUG: "72 Transformations I" says it opens the menu for ground and water mobs,
     * but the server checks only the tier filter, which allows ground mobs at tier 0, so a
     * player with no transform node (and no character) is granted the form.
     */
    @GameTest(template = PLATFORM, required = false)
    public static void requestWithoutAnyTransformNodeIsRefused(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.send(p, Tf.request(EntityType.COW));
        check(h, formOf(p) == null, "no transform node, but became " + formOf(p));
        h.succeed();
    }

    /**
     * SUSPECTED BUG: an unregistered id resolves to Forge's default entity type (the pig), so
     * the "type == null" refusal never fires and the requester becomes a pig.
     */
    @GameTest(template = PLATFORM, required = false)
    public static void requestForAnUnknownEntityIsRefused(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        Tf.send(p, new RequestTransformPacket(RequestTransformPacket.Kind.ENTITY,
                new ResourceLocation("jttw_engine", "no_such_mob")));
        check(h, formOf(p) == null, "unknown id, but became " + formOf(p));
        h.succeed();
    }

    private static EntityType<?> formOf(ServerPlayer p) {
        TransformationData d = TransformationManager.getTransformation(p);
        return d == null ? null : d.getEntityType();
    }
}
