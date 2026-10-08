package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.mixin.LivingEntityJumpingAccessor;
import com.jttw.engine.transformation.FormCatalog;
import com.jttw.engine.transformation.FormCategory;
import com.jttw.engine.transformation.FormClassifier;
import com.jttw.engine.transformation.MorphPose;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jttw.engine.gametest.GT.*;

/**
 * Every form the menus offer, one by one: the body the player takes on, and how the stand-in
 * model is posed to face, tilt, swim, jump and fly with the player.
 *
 * <p>Each test checks all forms and then reports every form that broke, not just the first.
 * Stand-ins are posed but not ticked here: the client ticks them on its own world, where AI and
 * explosions never run; on a server, ticking a primed TNT or lightning stand-in would set it off.
 * What the forms look like is checked by the form gallery (see {@code FormGallery}).
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class EveryFormGameTests {

    private static final float YAW = 37f, PITCH = 20f, BODY = 31f, HEAD = 41f;

    /**
     * Models whose attack pose is driven by the aggressive flag. (Others, like the panda, decide
     * "aggressive" for themselves and animate attacks from the swing alone.)
     */
    private static final java.util.Set<EntityType<?>> ARMS_RAISED_WHEN_AGGRESSIVE = java.util.Set.of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.GIANT, EntityType.SKELETON, EntityType.STRAY,
            EntityType.WITHER_SKELETON, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.VINDICATOR);

    private static List<ResourceLocation> allMobForms(GameTestHelper h) {
        List<ResourceLocation> all = new ArrayList<>();
        for (List<ResourceLocation> ids : FormCatalog.mobForms(h.getLevel()).values()) all.addAll(ids);
        return all;
    }

    private static EntityType<?> type(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.getValue(id);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-4;
    }

    private static void report(GameTestHelper h, String what, int checked, List<String> broken) {
        check(h, checked > 0, what + ": no forms found");
        check(h, broken.isEmpty(), what + ": " + broken.size() + " of " + checked + " forms broke: "
                + String.join("; ", broken));
    }

    // ── the catalog the menus read ──────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theMenusOfferEveryEntityExceptThePlayerExactlyOnce(GameTestHelper h) {
        Map<FormCategory, List<ResourceLocation>> mobs = FormCatalog.mobForms(h.getLevel());
        List<ResourceLocation> objects = FormCatalog.objectForms(h.getLevel());
        List<ResourceLocation> offered = new ArrayList<>(objects);
        mobs.values().forEach(offered::addAll);
        List<ResourceLocation> missing = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            if (type(id) == EntityType.PLAYER) continue;
            if (!offered.contains(id)) missing.add(id);
        }
        check(h, missing.isEmpty(), "entities offered nowhere: " + missing);
        eq(h, offered.size(), new java.util.HashSet<>(offered).size(), "no form offered twice");
        check(h, !offered.contains(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)), "the player is not a mob form");
        check(h, !mobs.containsKey(FormCategory.OBJECT) && !mobs.containsKey(FormCategory.PLAYER),
                "mob tabs hold only mobs");
        for (ResourceLocation id : objects) {
            Entity e = type(id).create(h.getLevel());
            check(h, !(e instanceof LivingEntity), id + " is living but filed as an object");
        }
        h.succeed();
    }

    // ── the body: hitbox, eyes, flight ─────────────────────────────

    /**
     * Every mob form gives the player that mob's standing hitbox and eye height, flight exactly
     * when the mob flies, and reverting gives the player's own body back.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void everyMobFormTakesItsOwnBodyAndRevertsCleanly(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        List<String> broken = new ArrayList<>();
        List<ResourceLocation> forms = allMobForms(h);
        for (ResourceLocation id : forms) {
            EntityType<?> t = type(id);
            LivingEntity mob = (LivingEntity) t.create(h.getLevel());
            TransformationManager.transform(p, t);
            TransformationData data = TransformationManager.getTransformation(p);
            if (data == null || !id.equals(data.getEntityId())) { broken.add(id + " did not transform"); continue; }
            var dims = mob.getDimensions(Pose.STANDING);
            if (!near(dims.width, p.getBbWidth()) || !near(dims.height, p.getBbHeight())) {
                broken.add(id + " hitbox " + p.getBbWidth() + "x" + p.getBbHeight() + " not " + dims.width + "x" + dims.height);
            }
            if (!near(mob.getEyeHeight(Pose.STANDING), p.getEyeHeight())) {
                broken.add(id + " eye " + p.getEyeHeight() + " not " + mob.getEyeHeight(Pose.STANDING));
            }
            boolean flies = FormClassifier.isFlyer(mob);
            if (p.getAbilities().mayfly != flies) broken.add(id + (flies ? " cannot fly" : " may fly"));

            TransformationManager.demorph(p);
            if (TransformationManager.isTransformed(p) || !near(0.6, p.getBbWidth()) || !near(1.8, p.getBbHeight())
                    || !near(1.62, p.getEyeHeight()) || p.getAbilities().mayfly) {
                broken.add(id + " left a trace after reverting: " + p.getBbWidth() + "x" + p.getBbHeight()
                        + " eye " + p.getEyeHeight() + " mayfly " + p.getAbilities().mayfly);
            }
        }
        report(h, "mob bodies", forms.size(), broken);
        h.succeed();
    }

    /** Objects take their own hitbox, a camera at 85% of their height (never above a player's), and never fly. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void everyObjectFormTakesItsOwnBodyAndRevertsCleanly(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        List<String> broken = new ArrayList<>();
        List<ResourceLocation> forms = FormCatalog.objectForms(h.getLevel());
        int checked = 0;
        for (ResourceLocation id : forms) {
            EntityType<?> t = type(id);
            Entity object = t.create(h.getLevel());
            if (object == null) continue; // not creatable in a level (none in vanilla today)
            checked++;
            TransformationManager.transform(p, t);
            TransformationData data = TransformationManager.getTransformation(p);
            if (data == null || !id.equals(data.getEntityId())) { broken.add(id + " did not transform"); continue; }
            var dims = object.getDimensions(Pose.STANDING);
            if (!near(dims.width, p.getBbWidth()) || !near(dims.height, p.getBbHeight())) {
                broken.add(id + " hitbox " + p.getBbWidth() + "x" + p.getBbHeight() + " not " + dims.width + "x" + dims.height);
            }
            float eye = Math.min(1.62f, dims.height * 0.85f);
            if (!near(eye, p.getEyeHeight())) broken.add(id + " camera " + p.getEyeHeight() + " not " + eye);
            if (p.getAbilities().mayfly) broken.add(id + " may fly");
            TransformationManager.demorph(p);
            if (TransformationManager.isTransformed(p) || !near(1.8, p.getBbHeight())) broken.add(id + " did not revert");
        }
        report(h, "object bodies", checked, broken);
        h.succeed();
    }

    // ── the stand-in model: facing and tilt ────────────────────────

    /** The player facing yaw 37, body 31, head 41, pitched 20 down, standing on the ground. */
    private static ServerPlayer posedPlayer(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        p.yRotO = YAW - 5; p.setYRot(YAW);
        p.xRotO = PITCH - 5; p.setXRot(PITCH);
        p.yBodyRotO = BODY - 5; p.yBodyRot = BODY;
        p.yHeadRotO = HEAD - 5; p.yHeadRot = HEAD;
        p.setOnGround(true);
        return p;
    }

    private static boolean sameAngle(float expected, float actual) {
        return Math.abs(Mth.wrapDegrees(expected - actual)) < 1e-3;
    }

    /**
     * Every form's model faces where the player faces (plus the offset for models built
     * backwards), and living models tilt with the player's pitch (or against it, for models
     * built that way); object models stay level.
     */
    @GameTest(template = PLATFORM)
    public static void everyFormsModelFacesAndTiltsWithThePlayer(GameTestHelper h) {
        ServerPlayer p = posedPlayer(h);
        List<String> broken = new ArrayList<>();
        List<ResourceLocation> forms = allMobForms(h);
        forms.addAll(FormCatalog.objectForms(h.getLevel()));
        int checked = 0;
        for (ResourceLocation id : forms) {
            EntityType<?> t = type(id);
            Entity dummy = t.create(h.getLevel());
            if (dummy == null) continue;
            checked++;
            try {
                MorphPose.apply(p, dummy, MorphPose.State.of(p, false, false));
            } catch (Throwable e) {
                broken.add(id + " threw " + e);
                continue;
            }
            float off = MorphPose.yawOffset(t);
            if (!sameAngle(YAW + off, dummy.getYRot()) || !sameAngle(YAW - 5 + off, dummy.yRotO)) {
                broken.add(id + " faces " + dummy.getYRot() + " not " + (YAW + off));
            }
            if (dummy instanceof LivingEntity living) {
                float body = living instanceof Axolotl ? HEAD : BODY; // the axolotl turns its body to its head
                if (!sameAngle(body + off, living.yBodyRot)) broken.add(id + " body " + living.yBodyRot + " not " + (body + off));
                if (!sameAngle(HEAD + off, living.yHeadRot)) broken.add(id + " head " + living.yHeadRot + " not " + (HEAD + off));
                float pitch = MorphPose.invertsPitch(t) ? -PITCH : PITCH;
                if (!sameAngle(pitch, living.getXRot())) broken.add(id + " pitch " + living.getXRot() + " not " + pitch);
            } else if (dummy.getXRot() != 0f) {
                broken.add(id + " tilts " + dummy.getXRot() + "; objects stay level");
            }
        }
        report(h, "facing", checked, broken);
        h.succeed();
    }

    /** Only the models built backwards or upside down get corrections, and nothing else does. */
    @GameTest(template = PLATFORM)
    public static void onlyTheEnderDragonIsTurnedAndOnlyItAndThePhantomTiltBackwards(GameTestHelper h) {
        List<String> turned = new ArrayList<>(), inverted = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            if (MorphPose.yawOffset(type(id)) != 0f) turned.add(id.getPath());
            if (MorphPose.invertsPitch(type(id))) inverted.add(id.getPath());
        }
        eq(h, List.of("ender_dragon"), turned, "turned 180 degrees");
        eq(h, 180f, MorphPose.yawOffset(EntityType.ENDER_DRAGON), "by half a turn");
        inverted.sort(String::compareTo);
        eq(h, List.of("ender_dragon", "phantom"), inverted, "pitch inverted");
        h.succeed();
    }

    // ── the stand-in model: swimming, ground, jumping, animations ──

    /** In water, every living model swims and leaves the ground; out again, it stands. */
    @GameTest(template = PLATFORM)
    public static void everyLivingModelSwimsInWaterAndStandsOnLand(GameTestHelper h) {
        ServerPlayer p = posedPlayer(h);
        List<String> broken = new ArrayList<>();
        List<ResourceLocation> forms = allMobForms(h);
        MorphPose.State swimming = new MorphPose.State(true, false, false, false, false);
        MorphPose.State standing = new MorphPose.State(false, true, false, false, false);
        for (ResourceLocation id : forms) {
            LivingEntity dummy = (LivingEntity) type(id).create(h.getLevel());
            MorphPose.apply(p, dummy, swimming);
            if (!dummy.isSwimming() || dummy.getPose() != Pose.SWIMMING || dummy.onGround()) {
                broken.add(id + " in water: swimming " + dummy.isSwimming() + " pose " + dummy.getPose()
                        + " onGround " + dummy.onGround());
            }
            MorphPose.apply(p, dummy, standing);
            if (dummy.isSwimming() || dummy.getPose() == Pose.SWIMMING || !dummy.onGround()) {
                broken.add(id + " back on land: swimming " + dummy.isSwimming() + " pose " + dummy.getPose()
                        + " onGround " + dummy.onGround());
            }
        }
        report(h, "swimming", forms.size(), broken);
        h.succeed();
    }

    /** In the air every model is off the ground, and jumps when the player jumps. */
    @GameTest(template = PLATFORM)
    public static void everyModelLeavesTheGroundInTheAirAndJumpsWithThePlayer(GameTestHelper h) {
        ServerPlayer p = posedPlayer(h);
        List<String> broken = new ArrayList<>();
        List<ResourceLocation> forms = allMobForms(h);
        forms.addAll(FormCatalog.objectForms(h.getLevel()));
        MorphPose.State jumping = new MorphPose.State(false, false, false, true, true);
        MorphPose.State landed = new MorphPose.State(false, true, false, false, false);
        int checked = 0;
        for (ResourceLocation id : forms) {
            Entity dummy = type(id).create(h.getLevel());
            if (dummy == null) continue;
            checked++;
            dummy.setOnGround(true);
            MorphPose.apply(p, dummy, jumping);
            if (dummy.onGround()) broken.add(id + " stays on the ground mid-jump");
            if (dummy instanceof LivingEntity living) {
                if (!((LivingEntityJumpingAccessor) living).jttw_isJumping()) broken.add(id + " does not jump");
                MorphPose.apply(p, dummy, landed);
                if (((LivingEntityJumpingAccessor) living).jttw_isJumping() || !living.onGround()) {
                    broken.add(id + " still jumping after landing");
                }
            }
        }
        report(h, "jumping", checked, broken);
        h.succeed();
    }

    /** Every living model plays the player's swing and hurt animations; most also walk with the player's stride. */
    @GameTest(template = PLATFORM)
    public static void everyLivingModelBorrowsThePlayersAnimations(GameTestHelper h) {
        ServerPlayer p = posedPlayer(h);
        p.swinging = true;
        p.swingTime = 3;
        p.attackAnim = 0.5f;
        p.oAttackAnim = 0.25f;
        p.hurtTime = 7;
        p.hurtDuration = 10;
        p.walkAnimation.setSpeed(0.8f);
        p.walkAnimation.update(0.8f, 1.0f);
        List<String> broken = new ArrayList<>();
        List<ResourceLocation> forms = allMobForms(h);
        List<String> ownWalk = new ArrayList<>();
        for (ResourceLocation id : forms) {
            LivingEntity dummy = (LivingEntity) type(id).create(h.getLevel());
            MorphPose.copyAttackAnim(p, dummy);
            if (!dummy.swinging || dummy.swingTime != 3 || dummy.attackAnim != 0.5f || dummy.oAttackAnim != 0.25f
                    || dummy.hurtTime != 7 || dummy.hurtDuration != 10) {
                broken.add(id + " did not take the swing/hurt animation");
            }
            // Models that raise their arms when aggressive need the flag while the player swings
            if (ARMS_RAISED_WHEN_AGGRESSIVE.contains(type(id))
                    && !((net.minecraft.world.entity.Mob) dummy).isAggressive()) {
                broken.add(id + " does not raise its arms while swinging");
            }
            if (MorphPose.usesStandardLimbSwing(dummy)) {
                MorphPose.copyWalkAnim(p.walkAnimation, dummy.walkAnimation);
                if (!near(p.walkAnimation.position(), dummy.walkAnimation.position())
                        || !near(p.walkAnimation.speed(), dummy.walkAnimation.speed())) {
                    broken.add(id + " does not walk with the player");
                }
            } else {
                ownWalk.add(id.getPath());
            }
        }
        report(h, "animations", forms.size(), broken);
        ownWalk.sort(String::compareTo);
        eq(h, List.of("frog", "rabbit"), ownWalk, "forms that keep their own walk");
        h.succeed();
    }

    // ── reading the player's state ─────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theStandInReadsGroundAirFlightAndJumpFromThePlayer(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        p.setOnGround(true);
        MorphPose.State still = MorphPose.State.of(p, false, false);
        check(h, still.onGround() && !still.airborne() && !still.jumping() && !still.flying(), "standing: " + still);

        p.setOnGround(false);
        p.setDeltaMovement(new Vec3(0, 0.3, 0));
        MorphPose.State rising = MorphPose.State.of(p, false, false);
        check(h, rising.airborne() && rising.jumping(), "rising off the ground counts as a jump: " + rising);

        p.setDeltaMovement(new Vec3(0, -0.3, 0));
        MorphPose.State falling = MorphPose.State.of(p, false, false);
        check(h, falling.airborne() && !falling.jumping(), "falling is airborne but no jump: " + falling);
        check(h, MorphPose.State.of(p, false, true).jumping(), "the jump key counts too");

        p.getAbilities().flying = true;
        check(h, !MorphPose.State.of(p, false, false).flying(), "flying needs a form that flies");
        MorphPose.State flying = MorphPose.State.of(p, true, false);
        check(h, flying.flying() && flying.airborne(), "a flying form flies: " + flying);
        p.setDeltaMovement(new Vec3(0, 0.3, 0));
        check(h, !MorphPose.State.of(p, true, false).jumping(), "climbing while flying is not a jump");
        h.succeed();
    }
}
