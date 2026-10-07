package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.marshal.MarshalSkillTree;
import com.jttw.engine.gametest.character.CharTest;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.FormCategory;
import com.jttw.engine.transformation.FormClassifier;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.transformation.FormCategory.*;

/**
 * Who may take which form: the classifier that sorts mobs into categories, and the server's
 * check of each request against the player's transformation art (18 / 36 / 72) and level.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransformAccessGameTests {

    // ── Classification ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void mobsAreSortedByTemperamentAndHabitat(GameTestHelper h) {
        Map<EntityType<?>, FormCategory> expected = Map.ofEntries(
                Map.entry(EntityType.COW, NEUTRAL_LAND), Map.entry(EntityType.WOLF, NEUTRAL_LAND),
                Map.entry(EntityType.VILLAGER, NEUTRAL_LAND), Map.entry(EntityType.IRON_GOLEM, NEUTRAL_LAND),
                Map.entry(EntityType.TURTLE, NEUTRAL_LAND), Map.entry(EntityType.FROG, NEUTRAL_LAND),
                Map.entry(EntityType.SQUID, NEUTRAL_WATER), Map.entry(EntityType.COD, NEUTRAL_WATER),
                Map.entry(EntityType.DOLPHIN, NEUTRAL_WATER), Map.entry(EntityType.AXOLOTL, NEUTRAL_WATER),
                Map.entry(EntityType.BEE, NEUTRAL_AIR), Map.entry(EntityType.PARROT, NEUTRAL_AIR),
                Map.entry(EntityType.BAT, NEUTRAL_AIR), Map.entry(EntityType.ALLAY, NEUTRAL_AIR),
                Map.entry(EntityType.ZOMBIE, HOSTILE_LAND), Map.entry(EntityType.SKELETON, HOSTILE_LAND),
                Map.entry(EntityType.ENDERMAN, HOSTILE_LAND), Map.entry(EntityType.CREEPER, HOSTILE_LAND),
                Map.entry(EntityType.DROWNED, HOSTILE_WATER), Map.entry(EntityType.GUARDIAN, HOSTILE_WATER),
                Map.entry(EntityType.PHANTOM, HOSTILE_AIR), Map.entry(EntityType.GHAST, HOSTILE_AIR),
                Map.entry(EntityType.BLAZE, HOSTILE_AIR), Map.entry(EntityType.VEX, HOSTILE_AIR),
                Map.entry(EntityType.ENDER_DRAGON, BOSS), Map.entry(EntityType.WITHER, BOSS),
                Map.entry(EntityType.ELDER_GUARDIAN, BOSS), Map.entry(EntityType.WARDEN, BOSS),
                Map.entry(EntityType.MINECART, OBJECT), Map.entry(EntityType.ARROW, OBJECT),
                Map.entry(EntityType.BOAT, OBJECT));
        expected.forEach((type, category) ->
                eq(h, category, FormClassifier.classify(type, type.create(h.getLevel())), type.toShortString()));
        eq(h, PLAYER, FormClassifier.classify(EntityType.PLAYER, null), "player");
        h.succeed();
    }

    // ── Requests: 72 Transformations (Wukong / Erlang Shen) ─────────

    private static ServerPlayer wukong(GameTestHelper h, int level) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        Tf.grantWukongTier(p, level);
        return p;
    }

    private static ServerPlayer marshal(GameTestHelper h, int level) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        String[] nodes = {MarshalSkillTree.TRANSFORM_1, MarshalSkillTree.TRANSFORM_2, MarshalSkillTree.TRANSFORM_3};
        for (int i = 0; i < level; i++) SkillTreeData.grantNode(p, nodes[i]);
        return p;
    }

    /** Sends a request and reports whether the player ended up in that form. */
    private static boolean takes(ServerPlayer p, EntityType<?> type) {
        TransformationManager.demorph(p);
        boolean object = FormClassifier.classify(type, type.create(p.serverLevel())) == OBJECT;
        Tf.send(p, object ? Tf.requestObject(type) : Tf.request(type));
        return formOf(p) == type;
    }

    @GameTest(template = PLATFORM)
    public static void seventyTwoLevelOneTakesEveryNeutralFormButNoHostiles(GameTestHelper h) {
        ServerPlayer p = wukong(h, 1);
        check(h, takes(p, EntityType.COW), "neutral land");
        check(h, takes(p, EntityType.SQUID), "neutral water");
        check(h, takes(p, EntityType.BEE), "neutral air");
        check(h, !takes(p, EntityType.ZOMBIE), "hostile land is level 2");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void seventyTwoLevelTwoAddsHostileLandButNoBossesOrHostileSeaAndSky(GameTestHelper h) {
        ServerPlayer p = wukong(h, 2);
        check(h, takes(p, EntityType.ZOMBIE), "hostile land");
        check(h, !takes(p, EntityType.WARDEN), "boss");
        check(h, !takes(p, EntityType.DROWNED), "hostile sea");
        check(h, !takes(p, EntityType.PHANTOM), "hostile sky");
        check(h, !takes(p, EntityType.MINECART), "object");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void seventyTwoLevelThreeTakesBossesHostileSeaAndSkyAndObjects(GameTestHelper h) {
        ServerPlayer p = wukong(h, 3);
        check(h, takes(p, EntityType.WARDEN), "boss");
        check(h, takes(p, EntityType.DROWNED), "hostile sea");
        check(h, takes(p, EntityType.PHANTOM), "hostile sky");
        check(h, takes(p, EntityType.MINECART), "object");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangShenPractisesSeventyTwoToo(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        Tf.grantErlangTier(p, 1);
        check(h, takes(p, EntityType.BEE), "neutral air at level 1");
        check(h, !takes(p, EntityType.ZOMBIE), "hostile land at level 1");
        h.succeed();
    }

    // ── Requests: 36 Transformations (the Marshal) ─────────────────

    @GameTest(template = PLATFORM)
    public static void thirtySixLevelOneIsNeutralLandAndWater(GameTestHelper h) {
        ServerPlayer p = marshal(h, 1);
        check(h, takes(p, EntityType.COW), "neutral land");
        check(h, takes(p, EntityType.SQUID), "neutral water");
        check(h, !takes(p, EntityType.BEE), "neutral air is level 2");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thirtySixLevelTwoAddsNeutralAir(GameTestHelper h) {
        ServerPlayer p = marshal(h, 2);
        check(h, takes(p, EntityType.BEE), "neutral air");
        check(h, !takes(p, EntityType.ZOMBIE), "hostile land is level 3");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thirtySixLevelThreeAddsHostileLandAndNothingElse(GameTestHelper h) {
        ServerPlayer p = marshal(h, 3);
        check(h, takes(p, EntityType.ZOMBIE), "hostile land");
        check(h, takes(p, EntityType.SPIDER), "small forms are fine");
        check(h, !takes(p, EntityType.DROWNED), "hostile sea");
        check(h, !takes(p, EntityType.PHANTOM), "hostile sky");
        check(h, !takes(p, EntityType.WARDEN), "boss");
        check(h, !takes(p, EntityType.MINECART), "object");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongsNodesDoNotLevelTheMarshal(GameTestHelper h) {
        ServerPlayer p = marshal(h, 0);
        Tf.grantWukongTier(p, 3);
        check(h, !takes(p, EntityType.COW), "another tree's transform nodes");
        h.succeed();
    }

    // ── Refusals ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void noTransformNodeMeansNoForms(GameTestHelper h) {
        check(h, !takes(wukong(h, 0), EntityType.COW), "Wukong without a node");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void charactersWithoutAnArtCannotTransform(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.YUAN_DI);
        check(h, !takes(p, EntityType.COW), "Yuan Di");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aRefusedRequestKeepsTheCurrentForm(GameTestHelper h) {
        ServerPlayer p = wukong(h, 1);
        Tf.send(p, Tf.request(EntityType.COW));
        Tf.send(p, Tf.request(EntityType.ZOMBIE));
        eq(h, EntityType.COW, formOf(p), "still a cow");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void mobAndObjectRequestsMustMatchWhatTheyName(GameTestHelper h) {
        ServerPlayer p = wukong(h, 3);
        Tf.send(p, Tf.requestObject(EntityType.COW));
        check(h, formOf(p) == null, "object request naming a mob");
        Tf.send(p, Tf.request(EntityType.MINECART));
        check(h, formOf(p) == null, "mob request naming an object");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void unknownEntityIdIsRefusedNotAPig(GameTestHelper h) {
        ServerPlayer p = wukong(h, 3);
        Tf.send(p, new RequestTransformPacket(RequestTransformPacket.Kind.ENTITY,
                new ResourceLocation("jttw_engine", "no_such_mob")));
        check(h, formOf(p) == null, "unknown id, but became " + formOf(p));
        h.succeed();
    }

    // ── Player forms ───────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void seventyTwoLevelThreeTakesAnotherPlayersAppearance(GameTestHelper h) {
        ServerPlayer p = wukong(h, 3);
        ServerPlayer target = Tf.player(h);
        CharTest.online(h, target);
        Tf.send(p, RequestTransformPacket.player(target.getUUID()));
        TransformationData d = TransformationManager.getTransformation(p);
        check(h, d != null && d.isPlayerForm(), "player form");
        eq(h, target.getUUID(), d.getDisguiseId(), "whose appearance");
        eq(h, target.getGameProfile().getName(), d.getDisguiseName(), "name shown");
        near(h, 1.8, d.getHeight(), "player height");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void playerFormsNeedSeventyTwoLevelThree(GameTestHelper h) {
        ServerPlayer target = Tf.player(h);
        CharTest.online(h, target);
        ServerPlayer w2 = wukong(h, 2);
        Tf.send(w2, RequestTransformPacket.player(target.getUUID()));
        check(h, TransformationManager.getTransformation(w2) == null, "72 at level 2");
        ServerPlayer m3 = marshal(h, 3);
        Tf.send(m3, RequestTransformPacket.player(target.getUUID()));
        check(h, TransformationManager.getTransformation(m3) == null, "36 at level 3");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void playerFormNeedsAnotherOnlinePlayer(GameTestHelper h) {
        ServerPlayer p = wukong(h, 3);
        CharTest.online(h, p);
        Tf.send(p, RequestTransformPacket.player(p.getUUID()));
        check(h, TransformationManager.getTransformation(p) == null, "yourself");
        Tf.send(p, RequestTransformPacket.player(java.util.UUID.randomUUID()));
        check(h, TransformationManager.getTransformation(p) == null, "nobody online by that id");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void playerFormSurvivesSaveAndNetworkRoundTrips(GameTestHelper h) {
        java.util.UUID id = java.util.UUID.randomUUID();
        TransformationData d = TransformationData.ofPlayer(id, "Somebody");
        TransformationData loaded = TransformationData.deserialize(d.serialize());
        check(h, loaded != null && loaded.isPlayerForm(), "saved player form loads");
        eq(h, id, loaded.getDisguiseId(), "saved id");
        eq(h, "Somebody", loaded.getDisguiseName(), "saved name");
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        d.toNetwork(buf);
        TransformationData received = TransformationData.fromNetwork(buf);
        check(h, received != null && id.equals(received.getDisguiseId()) && "Somebody".equals(received.getDisguiseName()), "network");
        eq(h, 0, buf.readableBytes(), "no bytes left over");
        h.succeed();
    }

    private static EntityType<?> formOf(ServerPlayer p) {
        TransformationData d = TransformationManager.getTransformation(p);
        return d == null ? null : d.getEntityType();
    }
}
