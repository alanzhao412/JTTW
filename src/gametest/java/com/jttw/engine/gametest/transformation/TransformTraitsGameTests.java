package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.transformation.TransformationAbilityHandler;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import com.jttw.engine.transformation.traits.AnimalSpeedTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/**
 * What each form grants while transformed and loses on revert: size-scaled speed and reach,
 * animal speed, high jumps and safe falls, flight, nether and underwater buffs, horse step
 * assist, and hostile mobs ignoring hostile forms.
 *
 * <p>Vanilla player bases: movement speed 0.1, block reach 4.5, entity reach 3.0.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransformTraitsGameTests {

    // ── SizeTraits ────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void bigFormsMoveFasterAndReachFurther(GameTestHelper h) {
        // iron golem 1.4 x 2.7: size scale = 1.4/0.6 = 7/3
        // speed x sqrt(7/3) = 1.5275; reach + (7/3 - 1) * 4.5 = +6
        ServerPlayer p = Tf.playerAs(h, EntityType.IRON_GOLEM);
        near(h, 0.15275252, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "speed");
        near(h, 10.5, p.getAttributeValue(ForgeMod.BLOCK_REACH.get()), "block reach");
        near(h, 9.0, p.getAttributeValue(ForgeMod.ENTITY_REACH.get()), "entity reach");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void hugeFormsAreCappedAtTwoAndAHalfSpeedAndThirtyTwoReach(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.ENDER_DRAGON);
        near(h, 0.25, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "speed capped at x2.5");
        near(h, 4.5 + 32.0, p.getAttributeValue(ForgeMod.BLOCK_REACH.get()), "block reach bonus capped at 32");
        near(h, 3.0 + 32.0, p.getAttributeValue(ForgeMod.ENTITY_REACH.get()), "entity reach bonus capped at 32");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void smallFormsAreNeverSlowerOrShorterReachedThanVanilla(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.CHICKEN);
        near(h, 0.1, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "speed");
        near(h, 4.5, p.getAttributeValue(ForgeMod.BLOCK_REACH.get()), "block reach");
        near(h, 3.0, p.getAttributeValue(ForgeMod.ENTITY_REACH.get()), "entity reach");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void revertRemovesSizeBonuses(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.IRON_GOLEM);
        TransformationManager.demorph(p);
        near(h, 0.1, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "speed");
        near(h, 4.5, p.getAttributeValue(ForgeMod.BLOCK_REACH.get()), "block reach");
        near(h, 3.0, p.getAttributeValue(ForgeMod.ENTITY_REACH.get()), "entity reach");
        h.succeed();
    }

    // ── AnimalSpeedTraits / rabbit speed ──────────────────────────

    @GameTest(template = PLATFORM)
    public static void fastAnimalsGetTheirSpeedBoost(GameTestHelper h) {
        // wolf/fox/cat are no bigger than the player, so only the animal boost applies
        near(h, 0.14, speedAs(h, EntityType.WOLF), "wolf +40%");
        near(h, 0.14, speedAs(h, EntityType.OCELOT), "ocelot +40%");
        near(h, 0.122, speedAs(h, EntityType.FOX), "fox +22%");
        near(h, 0.118, speedAs(h, EntityType.CAT), "cat +18%");
        near(h, 0.135, speedAs(h, EntityType.RABBIT), "rabbit +35%");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void switchingFormsDropsTheOldSpeedBoost(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.WOLF);
        TransformationManager.transform(p, EntityType.RABBIT);
        near(h, 0.135, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "rabbit only, no wolf");
        TransformationManager.transform(p, EntityType.CHICKEN);
        near(h, 0.1, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "chicken: no boost left over");
        TransformationManager.transform(p, EntityType.WOLF);
        TransformationManager.demorph(p);
        near(h, 0.1, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "reverted");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void reapplyingAnimalSpeedNeverStacks(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.WOLF);
        TransformationData data = TransformationManager.getTransformation(p);
        for (int i = 0; i < 5; i++) AnimalSpeedTraits.apply(p, data);
        near(h, 0.14, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "still +40% after re-applying");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void fishAreFastOnlyInWater(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.COD);
        near(h, 0.1, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "cod on land");
        submerge(h, p);
        AnimalSpeedTraits.apply(p, TransformationManager.getTransformation(p));
        near(h, 0.18, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "cod in water +80%");
        h.succeed();
    }

    // ── JumpTraits and horse jumps ────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void highJumpersLeapHigherOncePerJump(GameTestHelper h) {
        near(h, 0.95, jumpAs(h, EntityType.RABBIT), "rabbit jump velocity");
        near(h, 1.30, jumpAs(h, EntityType.FROG), "frog jump velocity");
        near(h, 1.48, jumpAs(h, EntityType.GOAT), "goat jump velocity");

        ServerPlayer p = Tf.playerAs(h, EntityType.RABBIT);
        TransformationManager.onPlayerJump(new LivingEvent.LivingJumpEvent(p));
        p.setDeltaMovement(0, 0.42, 0);
        TransformationManager.onPlayerJump(new LivingEvent.LivingJumpEvent(p));
        near(h, 0.42, p.getDeltaMovement().y, "no second boost before landing");
        p.setOnGround(true);
        TransformationManager.onLivingTick(new LivingEvent.LivingTickEvent(p));
        TransformationManager.onPlayerJump(new LivingEvent.LivingJumpEvent(p));
        near(h, 0.95, p.getDeltaMovement().y, "boosted again after landing");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void ordinaryFormsJumpNormally(GameTestHelper h) {
        near(h, 0.42, jumpAs(h, EntityType.COW), "cow");
        ServerPlayer human = Tf.player(h);
        human.setDeltaMovement(0, 0.42, 0);
        TransformationManager.onPlayerJump(new LivingEvent.LivingJumpEvent(human));
        near(h, 0.42, human.getDeltaMovement().y, "untransformed");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void horsesJumpAboutThreeBlocks(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.HORSE);
        p.setDeltaMovement(0, 0.42, 0);
        TransformationAbilityHandler.onPlayerJump(new LivingEvent.LivingJumpEvent(p));
        near(h, 0.7, p.getDeltaMovement().y, "horse jump velocity");
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        cow.setDeltaMovement(0, 0.42, 0);
        TransformationAbilityHandler.onPlayerJump(new LivingEvent.LivingJumpEvent(cow));
        near(h, 0.42, cow.getDeltaMovement().y, "cow unchanged");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void highJumpersAndHorsesShrugOffTheirOwnFalls(GameTestHelper h) {
        // safe-fall allowance: rabbit 9, frog 14, goat 14, horse 6
        near(h, 3f, fallAs(h, EntityType.RABBIT, 12f), "rabbit 12 -> 3");
        near(h, 0f, fallAs(h, EntityType.RABBIT, 5f), "rabbit 5 -> 0, never negative");
        near(h, 6f, fallAs(h, EntityType.FROG, 20f), "frog 20 -> 6");
        near(h, 6f, fallAs(h, EntityType.GOAT, 20f), "goat 20 -> 6");
        near(h, 4f, fallAs(h, EntityType.HORSE, 10f), "horse 10 -> 4");
        near(h, 4f, fallAs(h, EntityType.DONKEY, 10f), "donkey 10 -> 4");
        near(h, 10f, fallAs(h, EntityType.COW, 10f), "cow unchanged");
        ServerPlayer human = Tf.player(h);
        LivingFallEvent e = new LivingFallEvent(human, 10f, 1f);
        TransformationManager.onLivingFall(e);
        near(h, 10f, e.getDistance(), "untransformed unchanged");
        check(h, !e.isCanceled(), "untransformed fall not cancelled");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void slimesTakeNoFallDamage(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.SLIME, EntityType.MAGMA_CUBE}) {
            ServerPlayer p = Tf.playerAs(h, t);
            LivingFallEvent e = new LivingFallEvent(p, 50f, 1f);
            TransformationManager.onLivingFall(e);
            check(h, e.isCanceled(), t + " fall cancelled");
            check(h, TransformationAbilityHandler.shouldCancelFall(p), t + " shouldCancelFall");
        }
        check(h, !TransformationAbilityHandler.shouldCancelFall(Tf.playerAs(h, EntityType.COW)), "cow falls");
        check(h, !TransformationAbilityHandler.shouldCancelFall(Tf.player(h)), "human falls");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void horsesStepUpAndLoseItOnRevert(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transform(p, EntityType.SKELETON_HORSE);
        near(h, 2.1, p.maxUpStep(), "horse step = 0.6 + 1.5");
        TransformationManager.transform(p, EntityType.COW);
        check(h, p.maxUpStep() <= 1.0f, "horse step gone in a non-horse form: " + p.maxUpStep());
        TransformationManager.transform(p, EntityType.MULE);
        near(h, 2.1, p.maxUpStep(), "mule step");
        TransformationManager.demorph(p);
        check(h, p.maxUpStep() <= 1.0f, "horse step gone on revert: " + p.maxUpStep());
        h.succeed();
    }

    /** Leaving a horse form restores the step height the player had before, whatever it was. */
    @GameTest(template = PLATFORM)
    public static void revertingAHorseFormRestoresTheOriginalStepHeight(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        float before = p.maxUpStep();
        TransformationManager.transform(p, EntityType.HORSE);
        TransformationManager.demorph(p);
        near(h, before, p.maxUpStep(), "step height after reverting");
        h.succeed();
    }

    // ── flight ────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void flyingFormsMayFlyUntilReverted(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        check(h, !p.getAbilities().mayfly, "human cannot fly");
        TransformationManager.transform(p, EntityType.BEE);
        check(h, p.getAbilities().mayfly, "bee may fly");
        p.getAbilities().flying = true;
        TransformationManager.transform(p, EntityType.COW);
        check(h, !p.getAbilities().mayfly, "cow may not fly");
        check(h, !p.getAbilities().flying, "and is no longer flying");
        TransformationManager.transform(p, EntityType.PHANTOM);
        TransformationManager.demorph(p);
        check(h, !p.getAbilities().mayfly, "flight removed on revert");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aquaticFormsCannotFlyEvenWithUniversalFlight(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        JTTWFlight.setUniversalFlight(p, true);
        TransformationManager.transform(p, EntityType.COW);
        check(h, p.getAbilities().mayfly, "universal flight still applies in cow form");
        TransformationManager.transform(p, EntityType.SALMON);
        check(h, !p.getAbilities().mayfly, "salmon form turns flight off");
        h.succeed();
    }

    // ── NetherTraits ──────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void netherFormsGetFireResistance(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.BLAZE, EntityType.MAGMA_CUBE, EntityType.PIGLIN,
                EntityType.ZOMBIFIED_PIGLIN, EntityType.STRIDER, EntityType.WITHER_SKELETON}) {
            ServerPlayer p = Tf.playerAs(h, t);
            tick(p); // fake player's tickCount is 0, a multiple of the 15-tick refresh
            Tf.hasEffect(h, p, MobEffects.FIRE_RESISTANCE, 0, 220, t.toString());
        }
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        tick(cow);
        check(h, !cow.hasEffect(MobEffects.FIRE_RESISTANCE), "cow form gets no fire resistance");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void striderWalksOnLava(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), Blocks.LAVA);
        Vec3 surface = Tf.abs(h, 2.5, 2.0, 2.5);
        ServerPlayer p = Tf.playerAs(h, EntityType.STRIDER);
        p.setPos(surface.x, surface.y - 0.2, surface.z);
        p.setDeltaMovement(0, -0.5, 0);
        p.fallDistance = 5f;
        tick(p);
        near(h, surface.y, p.getY(), "lifted to the lava surface");
        near(h, 0.0, p.getDeltaMovement().y, "stopped sinking");
        check(h, p.onGround(), "standing");
        near(h, 0.0, p.fallDistance, "fall distance reset");

        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        cow.setPos(surface.x, surface.y - 0.2, surface.z);
        cow.setDeltaMovement(0, -0.5, 0);
        tick(cow);
        near(h, surface.y - 0.2, cow.getY(), "cow form sinks");
        h.succeed();
    }

    // ── UnderwaterTraits ──────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aquaticFormsBreatheAndSeeUnderwater(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.COD, EntityType.SQUID, EntityType.FROG,
                EntityType.AXOLOTL, EntityType.GUARDIAN}) {
            ServerPlayer p = Tf.playerAs(h, t);
            submerge(h, p);
            tick(p);
            Tf.hasEffect(h, p, MobEffects.WATER_BREATHING, 0, 220, t + " water breathing");
            Tf.hasEffect(h, p, MobEffects.NIGHT_VISION, 0, 220, t + " night vision");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void underwaterBuffsNeedBothWaterAndAnAquaticForm(GameTestHelper h) {
        ServerPlayer dry = Tf.playerAs(h, EntityType.COD);
        tick(dry);
        check(h, !dry.hasEffect(MobEffects.WATER_BREATHING), "cod on land: no buff");
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        submerge(h, cow);
        tick(cow);
        check(h, !cow.hasEffect(MobEffects.WATER_BREATHING), "cow in water: no buff");
        h.succeed();
    }

    // ── AggroTraits ───────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void hostilesWillNotTargetAHostileForm(GameTestHelper h) {
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ServerPlayer skeleton = Tf.playerAs(h, EntityType.SKELETON);
        zombie.setTarget(skeleton);
        check(h, zombie.getTarget() == null, "zombie refuses to target a skeleton-form player");

        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        zombie.setTarget(cow);
        check(h, zombie.getTarget() == cow, "zombie targets a cow-form player");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void hostileTargetIsDroppedOnceThePlayerBecomesHostile(GameTestHelper h) {
        Creeper creeper = h.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(0, 2, 0));
        ServerPlayer p = Tf.playerAs(h, EntityType.PIG);
        creeper.setTarget(p);
        creeper.setSwellDir(1);
        TransformationManager.transform(p, EntityType.ZOMBIE);
        TransformationManager.onLivingTick(new LivingEvent.LivingTickEvent(creeper));
        check(h, creeper.getTarget() == null, "target dropped");
        eq(h, -1, creeper.getSwellDir(), "creeper stops swelling");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void neutralMobsStillTargetHostileForms(GameTestHelper h) {
        Wolf wolf = h.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(0, 2, 0));
        ServerPlayer p = Tf.playerAs(h, EntityType.ZOMBIE);
        var event = new LivingChangeTargetEvent(wolf, p, LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
        TransformationManager.onHostileChangeTarget(event);
        check(h, event.getNewTarget() == p, "a wolf is not an Enemy, so it keeps its target");
        h.succeed();
    }

    // ── helpers ───────────────────────────────────────────────────

    private static double speedAs(GameTestHelper h, EntityType<?> type) {
        return Tf.playerAs(h, type).getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    private static double jumpAs(GameTestHelper h, EntityType<?> type) {
        ServerPlayer p = Tf.playerAs(h, type);
        p.setDeltaMovement(0, 0.42, 0); // vanilla jump velocity
        TransformationManager.onPlayerJump(new LivingEvent.LivingJumpEvent(p));
        return p.getDeltaMovement().y;
    }

    private static float fallAs(GameTestHelper h, EntityType<?> type, float distance) {
        ServerPlayer p = Tf.playerAs(h, type);
        LivingFallEvent e = new LivingFallEvent(p, distance, 1f);
        TransformationManager.onLivingFall(e);
        return e.getDistance();
    }

    /** One server tick of the transformation handlers (fake players do not tick themselves). */
    private static void tick(ServerPlayer p) {
        TransformationManager.onLivingTick(new LivingEvent.LivingTickEvent(p));
    }

    /** Fills the player's two blocks with water and lets the player notice it. */
    private static void submerge(GameTestHelper h, ServerPlayer p) {
        BlockPos feet = p.blockPosition();
        h.getLevel().setBlockAndUpdate(feet, Blocks.WATER.defaultBlockState());
        h.getLevel().setBlockAndUpdate(feet.above(), Blocks.WATER.defaultBlockState());
        p.baseTick();
        h.assertTrue(p.isInWaterOrBubble(), "setup: player is in water");
    }
}
