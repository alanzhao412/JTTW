package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.s2c.SyncTransformationPacket;
import com.jttw.engine.transformation.SuffocationDamageBlocker;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.UUID;

import static com.jttw.engine.gametest.GT.*;

/**
 * Transforming and reverting a player, what is saved on the player, how it survives death and
 * relogging, the TransformationData value and its two wire formats, and phasing.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransformationCoreGameTests {

    private static final String NBT_KEY = "jttw_transformation";

    // ── transform / demorph ───────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void transformTakesTheFormsHitboxAndEyeHeight(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        check(h, !TransformationManager.isTransformed(p), "fresh player is not transformed");
        TransformationManager.transform(p, EntityType.IRON_GOLEM);
        TransformationData data = TransformationManager.getTransformation(p);
        check(h, data != null, "transformed");
        eq(h, EntityType.IRON_GOLEM, data.getEntityType(), "form");
        eq(h, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_golem"), data.getEntityId(), "form id");
        // vanilla iron golem is 1.4 x 2.7
        near(h, 1.4, p.getBbWidth(), "player hitbox width");
        near(h, 2.7, p.getBbHeight(), "player hitbox height");
        near(h, data.getEyeHeight(), p.getEyeHeight(), "player eye height follows the form");
        eq(h, data, TransformationManager.getTransformation(p.getUUID()), "lookup by UUID");
        eq(h, data, TransformationManager.getTransformation((net.minecraft.world.entity.LivingEntity) p), "lookup as LivingEntity");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void transformSavesTheFormOnThePlayer(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transform(p, EntityType.WOLF);
        CompoundTag saved = p.getPersistentData().getCompound(NBT_KEY);
        eq(h, "minecraft:wolf", saved.getString("entity_id"), "saved form");
        near(h, 0.6, saved.getFloat("width"), "saved width");
        near(h, 0.85, saved.getFloat("height"), "saved height");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void demorphRestoresThePlayerAndForgetsTheForm(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transform(p, EntityType.IRON_GOLEM);
        TransformationManager.demorph(p);
        check(h, !TransformationManager.isTransformed(p), "not transformed after demorph");
        check(h, TransformationManager.getTransformation(p.getUUID()) == null, "no data by UUID");
        check(h, !p.getPersistentData().contains(NBT_KEY), "saved form removed");
        near(h, 0.6, p.getBbWidth(), "vanilla width");
        near(h, 1.8, p.getBbHeight(), "vanilla height");
        near(h, 1.62, p.getEyeHeight(), "vanilla eye height");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void demorphWhenNotTransformedIsHarmless(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.demorph(p);
        check(h, !TransformationManager.isTransformed(p), "still not transformed");
        near(h, 1.8, p.getBbHeight(), "vanilla height");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void transformingAgainReplacesTheForm(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transform(p, EntityType.COW);
        TransformationManager.transform(p, EntityType.GHAST);
        eq(h, EntityType.GHAST, TransformationManager.getTransformation(p).getEntityType(), "form");
        eq(h, "minecraft:ghast", p.getPersistentData().getCompound(NBT_KEY).getString("entity_id"), "saved form");
        near(h, 4.0, p.getBbHeight(), "ghast height");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void playerFormsGoThroughTransformIntoPlayerNotTransform(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        // a bare PLAYER type says nothing about whose appearance, so transform() refuses it
        TransformationManager.transform(p, EntityType.PLAYER);
        check(h, !TransformationManager.isTransformed(p), "bare player type refused");
        check(h, !p.getPersistentData().contains(NBT_KEY), "nothing saved");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void objectFormsGetAConservativeCameraHeight(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transform(p, EntityType.MINECART);
        TransformationData data = TransformationManager.getTransformation(p);
        check(h, data != null, "a minecart is a valid object form");
        // non-living: eye = min(1.62, 0.85 * height); minecart is 0.7 tall -> 0.595
        near(h, 0.595, data.getEyeHeight(), "minecart camera height");
        near(h, 0.7, p.getBbHeight(), "minecart height");
        check(h, !data.canFly() && !data.canPhase(), "no flight or phasing");
        h.succeed();
    }

    // ── survival across death and relogging ───────────────────────

    @GameTest(template = PLATFORM)
    public static void deathCopyCarriesTheFormAndItsTraits(GameTestHelper h) {
        ServerPlayer before = Tf.player(h);
        ServerPlayer after = Tf.player(h);
        TransformationManager.transform(before, EntityType.WOLF);
        TransformationManager.copyTransformationData(before, after);
        eq(h, EntityType.WOLF, TransformationManager.getTransformation(after).getEntityType(), "form after respawn");
        eq(h, "minecraft:wolf", after.getPersistentData().getCompound(NBT_KEY).getString("entity_id"), "saved on new player");
        // wolf speed trait (+40% base) is re-applied to the new player
        near(h, 0.14, after.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), "wolf speed");
        near(h, 0.85, after.getBbHeight(), "wolf height");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void deathCopyOfAnUntransformedPlayerLeavesNoTraits(GameTestHelper h) {
        ServerPlayer before = Tf.player(h);
        ServerPlayer after = Tf.player(h);
        TransformationManager.copyTransformationData(before, after);
        check(h, !TransformationManager.isTransformed(after), "not transformed");
        near(h, 0.1, after.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), "vanilla speed");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void formSurvivesLogoutAndLogin(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transform(p, EntityType.WOLF);
        TransformationManager.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(p));
        check(h, !TransformationManager.isTransformed(p), "forgotten in memory while logged out");
        check(h, p.getPersistentData().contains(NBT_KEY), "still saved on the player");
        TransformationManager.onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        eq(h, EntityType.WOLF, TransformationManager.getTransformation(p).getEntityType(), "restored at login");
        near(h, 0.14, p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), "traits re-applied");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void loginWithNothingSavedStaysHuman(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        check(h, !TransformationManager.isTransformed(p), "not transformed");
        h.succeed();
    }

    // ── TransformationData ────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void dataFromEntityReadsSizeEyeAndTraits(GameTestHelper h) {
        var level = h.getLevel();
        TransformationData cow = TransformationData.fromEntity(EntityType.COW, EntityType.COW.create(level));
        near(h, 0.9, cow.getWidth(), "cow width");
        near(h, 1.4, cow.getHeight(), "cow height");
        near(h, 1.3, cow.getEyeHeight(), "adult cow eye height");
        check(h, !cow.canFly() && !cow.canPhase(), "cow neither flies nor phases");

        TransformationData ghast = TransformationData.fromEntity(EntityType.GHAST, EntityType.GHAST.create(level));
        check(h, ghast.canFly(), "ghast flies");
        check(h, !ghast.canPhase(), "ghast does not phase");

        for (EntityType<?> flyer : new EntityType<?>[]{EntityType.BAT, EntityType.BEE, EntityType.PARROT,
                EntityType.PHANTOM, EntityType.BLAZE, EntityType.ENDER_DRAGON}) {
            TransformationData d = TransformationData.fromEntity(flyer, flyer.create(level));
            check(h, d.canFly(), flyer + " flies");
            check(h, !d.canPhase(), flyer + " does not phase");
        }
        for (EntityType<?> phaser : new EntityType<?>[]{EntityType.VEX, EntityType.ALLAY}) {
            TransformationData d = TransformationData.fromEntity(phaser, phaser.create(level));
            check(h, d.canFly() && d.canPhase(), phaser + " flies and phases");
        }
        TransformationData wolf = TransformationData.fromEntity(EntityType.WOLF, EntityType.WOLF.create(level));
        check(h, !wolf.canFly(), "wolf does not fly");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void scaleFactorIsHeightOverPlayerHeight(GameTestHelper h) {
        near(h, 2.0, new TransformationData(EntityType.COW, 1f, 1f, 3.6f, false, false).getScaleFactor(), "3.6 tall");
        near(h, 0.5, new TransformationData(EntityType.COW, 1f, 1f, 0.9f, false, false).getScaleFactor(), "0.9 tall");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void dataSurvivesNbtRoundTrip(GameTestHelper h) {
        TransformationData original = new TransformationData(EntityType.ALLAY, 0.36f, 0.35f, 0.6f, true, true);
        TransformationData copy = TransformationData.deserialize(original.serialize());
        check(h, copy != null, "deserialized");
        sameData(h, original, copy);
        TransformationData plain = TransformationData.deserialize(
                new TransformationData(EntityType.HORSE, 1.52f, 1.3964844f, 1.6f, false, false).serialize());
        check(h, !plain.canFly() && !plain.canPhase(), "false flags stay false");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void oldSaveWithoutSizeFallsBackToTheTypesSize(GameTestHelper h) {
        CompoundTag tag = new CompoundTag();
        tag.putString("entity_id", "minecraft:cow");
        tag.putFloat("eye_height", 1.3f);
        TransformationData d = TransformationData.deserialize(tag);
        check(h, d != null, "deserialized");
        near(h, 0.9, d.getWidth(), "cow default width");
        near(h, 1.4, d.getHeight(), "cow default height");
        check(h, !d.canFly() && !d.canPhase(), "missing flags read as false");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void malformedSavedIdReadsAsNoTransformation(GameTestHelper h) {
        CompoundTag tag = new CompoundTag();
        tag.putString("entity_id", "Not A Valid:Id!!");
        check(h, TransformationData.deserialize(tag) == null, "invalid id -> null");
        h.succeed();
    }

    /**
     * A saved form whose mob is no longer registered (a removed mod) loads as no transformation,
     * not as the registry's default entity (the pig).
     */
    @GameTest(template = PLATFORM)
    public static void unknownSavedEntityIdReadsAsNoTransformation(GameTestHelper h) {
        CompoundTag tag = new TransformationData(EntityType.COW, 1.3f, 0.9f, 1.4f, false, false).serialize();
        tag.putString("entity_id", "jttw_engine:no_such_mob");
        TransformationData d = TransformationData.deserialize(tag);
        check(h, d == null, "unregistered id should not load, but loaded as " + (d == null ? null : d.getEntityId()));
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void dataSurvivesNetworkRoundTrip(GameTestHelper h) {
        TransformationData original = new TransformationData(EntityType.VEX, 0.51f, 0.4f, 0.8f, true, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        original.toNetwork(buf);
        TransformationData copy = TransformationData.fromNetwork(buf);
        check(h, copy != null, "decoded");
        eq(h, 0, buf.readableBytes(), "every byte read");
        sameData(h, original, copy);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void truncatedNetworkDataDecodesAsNull(GameTestHelper h) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"));
        buf.writeFloat(1.3f);
        check(h, TransformationData.fromNetwork(buf) == null, "short buffer -> null rather than a throw");
        h.succeed();
    }

    // ── SyncTransformationPacket ──────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void syncPacketRoundTripsAForm(GameTestHelper h) {
        UUID id = UUID.randomUUID();
        TransformationData data = new TransformationData(EntityType.GHAST, 2.6f, 4f, 4f, true, false);
        byte[] wire = encode(new SyncTransformationPacket(id, data));

        FriendlyByteBuf in = new FriendlyByteBuf(Unpooled.wrappedBuffer(wire));
        SyncTransformationPacket decoded = SyncTransformationPacket.decode(in);
        eq(h, 0, in.readableBytes(), "decode read every byte");
        byte[] again = encode(decoded);
        check(h, Arrays.equals(wire, again), "re-encoding the decoded packet gives the same bytes");

        FriendlyByteBuf read = new FriendlyByteBuf(Unpooled.wrappedBuffer(again));
        eq(h, id, read.readUUID(), "player id");
        check(h, read.readBoolean(), "has data");
        sameData(h, data, TransformationData.fromNetwork(read));
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void syncPacketRoundTripsARevert(GameTestHelper h) {
        UUID id = UUID.randomUUID();
        byte[] wire = encode(new SyncTransformationPacket(id, null));
        FriendlyByteBuf in = new FriendlyByteBuf(Unpooled.wrappedBuffer(wire));
        SyncTransformationPacket decoded = SyncTransformationPacket.decode(in);
        eq(h, 0, in.readableBytes(), "decode read every byte");
        FriendlyByteBuf read = new FriendlyByteBuf(Unpooled.wrappedBuffer(encode(decoded)));
        eq(h, id, read.readUUID(), "player id");
        check(h, !read.readBoolean(), "no data means revert");
        eq(h, 0, read.readableBytes(), "nothing after the flag");
        h.succeed();
    }

    // ── phasing (vex / allay) ─────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void phasingFormsPassThroughBlocks(GameTestHelper h) {
        BlockPos floor = h.absolutePos(new BlockPos(2, 1, 2));
        var level = h.getLevel();
        ServerPlayer vex = Tf.playerAs(h, EntityType.VEX);
        check(h, level.getBlockState(floor).getCollisionShape(level, floor, CollisionContext.of(vex)).isEmpty(),
                "stone has no collision for a vex-form player");
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        check(h, !level.getBlockState(floor).getCollisionShape(level, floor, CollisionContext.of(cow)).isEmpty(),
                "stone is solid for a cow-form player");
        TransformationManager.demorph(vex);
        check(h, !level.getBlockState(floor).getCollisionShape(level, floor, CollisionContext.of(vex)).isEmpty(),
                "stone is solid again after reverting");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void phasingFormsTakeNoSuffocationDamage(GameTestHelper h) {
        var sources = h.getLevel().damageSources();
        ServerPlayer allay = Tf.playerAs(h, EntityType.ALLAY);
        allay.hurtTime = 5;
        LivingIncomingDamageEvent attack = new LivingIncomingDamageEvent(allay, sources.inWall(), 1f);
        SuffocationDamageBlocker.onLivingAttack(attack);
        check(h, attack.isCanceled(), "in-wall attack cancelled");
        eq(h, 0, allay.hurtTime, "hurt flash cleared");
        LivingIncomingDamageEvent hurt = new LivingIncomingDamageEvent(allay, sources.inWall(), 1f);
        SuffocationDamageBlocker.onLivingHurt(hurt);
        check(h, hurt.isCanceled(), "in-wall hurt cancelled");

        LivingIncomingDamageEvent other = new LivingIncomingDamageEvent(allay, sources.generic(), 1f);
        SuffocationDamageBlocker.onLivingAttack(other);
        check(h, !other.isCanceled(), "other damage still lands on a phasing form");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void solidFormsStillSuffocate(GameTestHelper h) {
        var sources = h.getLevel().damageSources();
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        LivingIncomingDamageEvent attack = new LivingIncomingDamageEvent(cow, sources.inWall(), 1f);
        SuffocationDamageBlocker.onLivingAttack(attack);
        check(h, !attack.isCanceled(), "cow form suffocates");
        ServerPlayer human = Tf.player(h);
        LivingIncomingDamageEvent hurt = new LivingIncomingDamageEvent(human, sources.inWall(), 1f);
        SuffocationDamageBlocker.onLivingHurt(hurt);
        check(h, !hurt.isCanceled(), "untransformed player suffocates");
        h.succeed();
    }

    // ── helpers ───────────────────────────────────────────────────

    private static byte[] encode(SyncTransformationPacket pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncTransformationPacket.encode(pkt, buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    private static void sameData(GameTestHelper h, TransformationData expected, TransformationData actual) {
        check(h, actual != null, "data present");
        eq(h, expected.getEntityType(), actual.getEntityType(), "entity type");
        eq(h, expected.getEntityId(), actual.getEntityId(), "entity id");
        eq(h, expected.getEyeHeight(), actual.getEyeHeight(), "eye height");
        eq(h, expected.getWidth(), actual.getWidth(), "width");
        eq(h, expected.getHeight(), actual.getHeight(), "height");
        eq(h, expected.canFly(), actual.canFly(), "canFly");
        eq(h, expected.canPhase(), actual.canPhase(), "canPhase");
    }
}
