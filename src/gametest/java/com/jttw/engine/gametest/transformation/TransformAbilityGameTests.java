package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.transformation.TransformationAbilityHandler;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * The mob abilities a transformed player gets (TransformationAbilityHandler): right-click
 * attacks and their cooldowns, melee on-hit effects, and the per-tick abilities.
 *
 * <p>Projectiles are discarded as soon as they are counted so they cannot fly into other tests.
 * Cooldowns are checked a little before and a little after their documented length.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransformAbilityGameTests {

    // ── right-click projectiles ───────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void untransformedRightClickDoesNothing(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.rightClick(p);
        eq(h, 0, Tf.near(p, Projectile.class, 6).size(), "projectiles");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void ghastShootsAnExplodingFireballEveryTwoSeconds(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.GHAST);
        Tf.rightClick(p);
        List<LargeFireball> balls = Tf.near(p, LargeFireball.class, 3);
        eq(h, 1, balls.size(), "fireballs");
        CompoundTag nbt = new CompoundTag();
        balls.get(0).saveWithoutId(nbt);
        eq(h, (byte) 1, nbt.getByte("ExplosionPower"), "explosion power");
        check(h, balls.get(0).getOwner() == p, "owned by the player");
        Tf.discard(balls);
        Tf.rightClick(p);
        eq(h, 0, discardCount(p, LargeFireball.class), "on cooldown at once");
        h.runAfterDelay(30, () -> {
            Tf.rightClick(p);
            eq(h, 0, discardCount(p, LargeFireball.class), "still on cooldown after 1.5s");
        });
        h.runAfterDelay(41, () -> {
            Tf.rightClick(p);
            eq(h, 1, discardCount(p, LargeFireball.class), "ready after 2s");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void enderDragonAlsoShootsLargeFireballs(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.ENDER_DRAGON);
        Tf.rightClick(p);
        eq(h, 1, discardCount(p, LargeFireball.class), "dragon fireball");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void blazeShootsSmallFireballs(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.BLAZE);
        Tf.rightClick(p);
        eq(h, 0, discardCount(p, LargeFireball.class), "no large fireball");
        eq(h, 1, discardCount(p, SmallFireball.class), "small fireball");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void skeletonShootsUnpickableArrowsEverySecond(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.SKELETON);
        Tf.rightClick(p);
        List<Arrow> arrows = Tf.near(p, Arrow.class, 3);
        eq(h, 1, arrows.size(), "arrows");
        eq(h, AbstractArrow.Pickup.DISALLOWED, arrows.get(0).pickup, "pickup");
        check(h, arrows.get(0).getOwner() == p, "owned by the player");
        eq(h, 0, arrowEffects(arrows.get(0)).size(), "plain arrow");
        Tf.discard(arrows);
        Tf.rightClick(p);
        eq(h, 0, discardCount(p, Arrow.class), "on cooldown at once");
        h.runAfterDelay(15, () -> {
            Tf.rightClick(p);
            eq(h, 0, discardCount(p, Arrow.class), "still on cooldown after 0.75s");
        });
        h.runAfterDelay(21, () -> {
            Tf.rightClick(p);
            eq(h, 1, discardCount(p, Arrow.class), "ready after 1s");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void witherSkeletonArrowsWitherAndStrayArrowsSlow(GameTestHelper h) {
        ServerPlayer ws = Tf.playerAs(h, EntityType.WITHER_SKELETON);
        Tf.rightClick(ws);
        List<Arrow> a = Tf.near(ws, Arrow.class, 3);
        eq(h, 1, a.size(), "wither skeleton arrows");
        List<MobEffectInstance> fx = arrowEffects(a.get(0));
        Tf.discard(a);
        eq(h, 1, fx.size(), "one effect");
        effectIs(h, fx.get(0), MobEffects.WITHER, 0, 60, "wither skeleton arrow");

        ServerPlayer stray = Tf.playerAs(h, EntityType.STRAY);
        Tf.rightClick(stray);
        List<Arrow> b = Tf.near(stray, Arrow.class, 3);
        eq(h, 1, b.size(), "stray arrows");
        List<MobEffectInstance> fx2 = arrowEffects(b.get(0));
        Tf.discard(b);
        eq(h, 1, fx2.size(), "one effect");
        effectIs(h, fx2.get(0), MobEffects.MOVEMENT_SLOWDOWN, 0, 600, "stray arrow");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void witchThrowsStrongPoisonEveryThreeSeconds(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.WITCH);
        Tf.rightClick(p);
        List<ThrownPotion> potions = Tf.near(p, ThrownPotion.class, 3);
        eq(h, 1, potions.size(), "potions");
        eq(h, Potions.STRONG_POISON, potions.get(0).getItem()
                .getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().orElse(null), "potion");
        Tf.discard(potions);
        Tf.rightClick(p);
        eq(h, 0, discardCount(p, ThrownPotion.class), "on cooldown at once");
        h.runAfterDelay(50, () -> {
            Tf.rightClick(p);
            eq(h, 0, discardCount(p, ThrownPotion.class), "still on cooldown after 2.5s");
        });
        h.runAfterDelay(61, () -> {
            Tf.rightClick(p);
            eq(h, 1, discardCount(p, ThrownPotion.class), "ready after 3s");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void llamasSpit(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.LLAMA, EntityType.TRADER_LLAMA}) {
            ServerPlayer p = Tf.playerAs(h, t);
            Tf.rightClick(p);
            List<LlamaSpit> spit = Tf.near(p, LlamaSpit.class, 3);
            eq(h, 1, spit.size(), t + " spit");
            check(h, spit.get(0).getOwner() == p, "owned by the player");
            Tf.discard(spit);
            Tf.rightClick(p);
            eq(h, 0, discardCount(p, LlamaSpit.class), t + " on cooldown at once");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void witherShootsSkulls(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.WITHER);
        Tf.rightClick(p);
        List<WitherSkull> skulls = Tf.near(p, WitherSkull.class, 3);
        eq(h, 1, skulls.size(), "skulls");
        check(h, skulls.get(0).getOwner() == p, "owned by the player");
        Tf.discard(skulls);
        Tf.rightClick(p);
        eq(h, 0, discardCount(p, WitherSkull.class), "on cooldown at once");
        h.runAfterDelay(41, () -> {
            Tf.rightClick(p);
            eq(h, 1, discardCount(p, WitherSkull.class), "ready after 2s");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void snowGolemSnowballsDealThreeDamage(GameTestHelper h) {
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(0, 2, 0));
        ServerPlayer p = Tf.playerAs(h, EntityType.SNOW_GOLEM);
        Tf.rightClick(p);
        List<Snowball> balls = Tf.near(p, Snowball.class, 3);
        eq(h, 1, balls.size(), "snowballs");
        Tf.rightClick(p);
        eq(h, 1, Tf.near(p, Snowball.class, 3).size(), "on cooldown at once");
        // the snowball lands on the pig
        Snowball ball = balls.get(0);
        ball.setPos(pig.getX(), pig.getY() + 0.3, pig.getZ());
        ball.discard();
        TransformationAbilityHandler.tickSnowballDamage(h.getLevel());
        near(h, pig.getMaxHealth() - 3.0, pig.getHealth(), "pig health");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "evokerSummonsAFangLine") // its line of fangs runs past the platform, into the next test's ground
    public static void evokerSummonsAFangLine(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.EVOKER);
        Tf.rightClick(p);
        List<EvokerFangs> fangs = Tf.near(p, EvokerFangs.class, 8);
        eq(h, 5, fangs.size(), "fangs");
        for (EvokerFangs f : fangs) check(h, f.getOwner() == p, "fangs owned by the player");
        Tf.discard(fangs);
        Tf.rightClick(p);
        eq(h, 0, discardCount(p, EvokerFangs.class), "on cooldown at once");
        h.succeed();
    }

    // ── Warden sonic boom ─────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 160)
    public static void wardenSonicBoomHitsTheTargetForTenAndCostsFiveQi(GameTestHelper h) {
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.WARDEN);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 50f);
        aim(p, golem);
        Tf.rightClick(p);
        near(h, golem.getMaxHealth() - 10.0, golem.getHealth(), "golem health");
        near(h, 45f, QiData.getCurrent(p), "Qi spent");
        Tf.rightClick(p);
        near(h, 45f, QiData.getCurrent(p), "on cooldown: nothing spent");
        golem.discard(); // knocked back; keep it out of the later aims
        h.runAfterDelay(90, () -> {
            IronGolem fresh = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
            aim(p, fresh);
            Tf.rightClick(p);
            near(h, fresh.getMaxHealth(), fresh.getHealth(), "still on cooldown after 4.5s");
            fresh.discard();
        });
        h.runAfterDelay(101, () -> {
            IronGolem fresh = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
            aim(p, fresh);
            Tf.rightClick(p);
            near(h, fresh.getMaxHealth() - 10.0, fresh.getHealth(), "ready after 5s");
            near(h, 40f, QiData.getCurrent(p), "Qi spent again");
            h.succeed();
        });
    }

    /** At level 20 the boom's 10 grows by the level's power (the transformed player is the owner). */
    @GameTest(template = PLATFORM)
    public static void wardenSonicBoomFiresWithoutEnoughQi(GameTestHelper h) {
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.WARDEN);
        PlayerLevel.setLevel(p, 20);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 3f);
        aim(p, golem);
        Tf.rightClick(p);
        near(h, golem.getMaxHealth() - 10.0 * LevelPower.of(20), golem.getHealth(), "boom still lands, at power");
        near(h, 3f, QiData.getCurrent(p), "Qi untouched when short");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wardenSonicBoomWithNoTargetCostsNothing(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.WARDEN);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 50f);
        p.setXRot(-90f); // straight up, nothing there
        Tf.rightClick(p);
        near(h, 50f, QiData.getCurrent(p), "no target, no Qi");
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        aim(p, golem);
        Tf.rightClick(p);
        near(h, golem.getMaxHealth() - 10.0, golem.getHealth(), "a miss starts no cooldown");
        h.succeed();
    }

    // ── Guardian laser ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void guardianLaserFatiguesAndHitsOncePerHalfSecond(GameTestHelper h) {
        openAir(h); // the golem, 1.4 wide at the platform's edge, would choke in the barrier wall
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.GUARDIAN);
        aim(p, golem);
        Tf.rightClick(p);
        float max = golem.getMaxHealth();
        for (int i = 1; i <= 15; i++) {
            final int t = i;
            h.runAfterDelay(t, () -> {
                TransformationAbilityHandler.tickGuardianLaser(p);
                if (t == 1) {
                    Tf.hasEffect(h, golem, MobEffects.DIG_SLOWDOWN, 0, 30, "mining fatigue on the target");
                    near(h, max, golem.getHealth(), "no damage before 10 ticks");
                }
                if (t == 15) {
                    near(h, max - 1.0, golem.getHealth(), "one damage at 10 ticks");
                    h.succeed();
                }
            });
        }
    }

    @GameTest(template = PLATFORM, timeoutTicks = 160)
    public static void guardianLaserStopsAfterFiveSeconds(GameTestHelper h) {
        openAir(h); // the golem, 1.4 wide at the platform's edge, would choke in the barrier wall
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.GUARDIAN);
        aim(p, golem);
        Tf.rightClick(p);
        float max = golem.getMaxHealth();
        for (int i = 1; i <= 104; i++) {
            final int t = i;
            h.runAfterDelay(t, () -> {
                if (t == 96 || t == 104) golem.removeEffect(MobEffects.DIG_SLOWDOWN);
                TransformationAbilityHandler.tickGuardianLaser(p);
                if (t == 96) check(h, golem.hasEffect(MobEffects.DIG_SLOWDOWN), "still channelling at 4.8s");
                if (t == 104) {
                    check(h, !golem.hasEffect(MobEffects.DIG_SLOWDOWN), "beam over after 5s");
                    // hits at 10, 20, ... 90 ticks
                    near(h, max - 9.0, golem.getHealth(), "nine hits over the channel");
                    h.succeed();
                }
            });
        }
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void guardianLaserShiftCancelsAndStartsTheCooldown(GameTestHelper h) {
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.GUARDIAN);
        aim(p, golem);
        Tf.rightClick(p);
        p.setShiftKeyDown(true);
        TransformationAbilityHandler.tickGuardianLaser(p);
        p.setShiftKeyDown(false);
        TransformationAbilityHandler.tickGuardianLaser(p);
        check(h, !golem.hasEffect(MobEffects.DIG_SLOWDOWN), "beam cancelled by sneaking");
        Tf.rightClick(p);
        TransformationAbilityHandler.tickGuardianLaser(p);
        check(h, !golem.hasEffect(MobEffects.DIG_SLOWDOWN), "cannot restart during the cooldown");
        h.runAfterDelay(50, () -> {
            Tf.rightClick(p);
            TransformationAbilityHandler.tickGuardianLaser(p);
            check(h, !golem.hasEffect(MobEffects.DIG_SLOWDOWN), "still on cooldown after 2.5s");
        });
        // a few ticks past the 60-tick cooldown: callback-vs-tick order varies with load
        h.runAfterDelay(65, () -> {
            aim(p, golem);
            Tf.rightClick(p);
            TransformationAbilityHandler.tickGuardianLaser(p);
            check(h, golem.hasEffect(MobEffects.DIG_SLOWDOWN), "ready after 3s");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void guardianLaserEndsWhenThePlayerReverts(GameTestHelper h) {
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.ELDER_GUARDIAN);
        aim(p, golem);
        Tf.rightClick(p);
        TransformationManager.demorph(p);
        TransformationAbilityHandler.tickGuardianLaser(p);
        check(h, !golem.hasEffect(MobEffects.DIG_SLOWDOWN), "no beam after reverting");
        h.succeed();
    }

    // ── Iron Golem slam ───────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void ironGolemSlamHitsAndLaunchesEverythingWithinFourBlocks(GameTestHelper h) {
        Pig close = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 4));
        Pig far = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 8));
        ServerPlayer p = Tf.playerAs(h, EntityType.IRON_GOLEM);
        Tf.rightClick(p);
        near(h, close.getMaxHealth() - 6.0, close.getHealth(), "close pig health");
        near(h, 0.8, close.getDeltaMovement().y, "close pig launched");
        near(h, far.getMaxHealth(), far.getHealth(), "far pig untouched");
        near(h, 0.0, far.getDeltaMovement().y, "far pig not launched");
        Pig late = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 3));
        Tf.rightClick(p);
        near(h, late.getMaxHealth(), late.getHealth(), "slam on cooldown");
        far.discard();
        h.succeed();
    }

    // ── Enderman teleport ─────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void endermanTeleportsUpToTheBlockItLooksAt(GameTestHelper h) {
        for (int y = 2; y <= 5; y++) h.setBlock(new BlockPos(2, y, 4), Blocks.STONE);
        ServerPlayer p = Tf.teleportable(Tf.playerAs(h, EntityType.ENDERMAN));
        p.setYRot(0f); // facing +z, toward the wall
        p.setXRot(0f);
        Vec3 start = p.position();
        Tf.rightClick(p);
        Vec3 dest = h.relativeVec(p.position());
        near(h, 2.5, dest.x, "x unchanged");
        near(h, 3.5, dest.z, "stopped half a block before the wall");
        check(h, dest.y >= 2.0, "not inside the floor: y=" + dest.y);
        p.teleportTo(start.x, start.y, start.z);
        Tf.rightClick(p);
        near(h, start.z, p.getZ(), "on cooldown at once");
        h.succeed();
    }

    // ── Creeper detonation (own batches: explosions reach neighbouring tests) ──

    @GameTest(template = PLATFORM, batch = "jttw_creeper")
    public static void creeperDetonatesAfterThirtyTicksAndWithersNearbyMobs(GameTestHelper h) {
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.CREEPER);
        Tf.rightClick(p);
        for (int i = 0; i < 29; i++) TransformationAbilityHandler.tickCreeperCharge(p);
        check(h, !golem.hasEffect(MobEffects.WITHER), "not yet after 29 ticks");
        MobEffectInstance hold = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(h, hold != null && hold.getAmplifier() == 255, "player frozen while charging");
        TransformationAbilityHandler.tickCreeperCharge(p);
        check(h, golem.isAlive(), "golem survives the blast");
        Tf.hasEffect(h, golem, MobEffects.WITHER, 1, 100, "Wither II on nearby mob");
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "player unfrozen after the blast");
        // 30 s cooldown
        golem.removeAllEffects();
        Tf.rightClick(p);
        for (int i = 0; i < 30; i++) TransformationAbilityHandler.tickCreeperCharge(p);
        check(h, !golem.hasEffect(MobEffects.WITHER), "no second blast during the cooldown");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "jttw_creeper")
    public static void creeperChargeIsDroppedWhenThePlayerReverts(GameTestHelper h) {
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.CREEPER);
        Tf.rightClick(p);
        for (int i = 0; i < 10; i++) TransformationAbilityHandler.tickCreeperCharge(p);
        TransformationManager.demorph(p);
        for (int i = 0; i < 30; i++) TransformationAbilityHandler.tickCreeperCharge(p);
        TransformationManager.transform(p, EntityType.CREEPER);
        for (int i = 0; i < 30; i++) TransformationAbilityHandler.tickCreeperCharge(p);
        check(h, !golem.hasEffect(MobEffects.WITHER), "no blast: the charge ended with the form");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "jttw_creeper_charged")
    public static void chargedCreeperBlastIsStronger(GameTestHelper h) {
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.playerAs(h, EntityType.CREEPER);
        p.setShiftKeyDown(true);
        Tf.rightClick(p); // charged on
        Tf.rightClick(p); // charged off
        Tf.rightClick(p); // charged on
        p.setShiftKeyDown(false);
        Tf.rightClick(p);
        for (int i = 0; i < 30; i++) TransformationAbilityHandler.tickCreeperCharge(p);
        check(h, golem.isAlive(), "golem survives the blast");
        Tf.hasEffect(h, golem, MobEffects.WITHER, 2, 160, "Wither III");
        Tf.hasEffect(h, golem, MobEffects.MOVEMENT_SLOWDOWN, 1, 100, "Slowness II");
        h.succeed();
    }

    // ── Evoker vex (own batch: the vex fly about for ten seconds) ──

    @GameTest(template = PLATFORM, batch = "jttw_vex", timeoutTicks = 260)
    public static void evokerVexAreSummonedInThreesAndVanishAfterTenSeconds(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.EVOKER);
        p.setShiftKeyDown(true);
        Tf.rightClick(p);
        List<Vex> vex = Tf.near(p, Vex.class, 4);
        eq(h, 3, vex.size(), "vex summoned");
        for (Vex v : vex) eq(h, "jttw_vex:" + p.getUUID(), v.getCustomName().getString(), "vex tag");
        Tf.rightClick(p);
        eq(h, 3, Tf.near(p, Vex.class, 4).size(), "summon on cooldown");
        eq(h, 0, Tf.near(p, EvokerFangs.class, 8).size(), "sneaking summons vex, not fangs");
        h.runAfterDelay(150, () -> {
            TransformationAbilityHandler.tickVexDespawn(h.getLevel());
            for (Vex v : vex) check(h, !v.isRemoved(), "vex still here at 7.5s (removed: " + v.getRemovalReason() + ", health " + v.getHealth() + ", last hurt by " + v.getLastDamageSource() + ")");
        });
        h.runAfterDelay(201, () -> {
            TransformationAbilityHandler.tickVexDespawn(h.getLevel());
            for (Vex v : vex) check(h, v.isRemoved(), "vex gone after 10s");
            h.succeed();
        });
    }

    // ── melee on-hit effects ──────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void beeStingSlowsAndPoisons(GameTestHelper h) {
        Pig pig = melee(h, EntityType.BEE);
        Tf.hasEffect(h, pig, MobEffects.MOVEMENT_SLOWDOWN, 0, 60, "Slowness I 3s");
        Tf.hasEffect(h, pig, MobEffects.POISON, 0, 60, "Poison I 3s");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void spiderBitesPoisonHarderThanTheySlow(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.SPIDER, EntityType.CAVE_SPIDER}) {
            Pig pig = melee(h, t);
            Tf.hasEffect(h, pig, MobEffects.POISON, 1, 80, t + " Poison II 4s");
            Tf.hasEffect(h, pig, MobEffects.MOVEMENT_SLOWDOWN, 0, 40, t + " Slowness I 2s");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void zombieHitsSlow(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.HUSK,
                EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER}) {
            Pig pig = melee(h, t);
            Tf.hasEffect(h, pig, MobEffects.MOVEMENT_SLOWDOWN, 0, 60, t + " Slowness I 3s");
            check(h, !pig.hasEffect(MobEffects.POISON), t + " does not poison");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void witherFormsWither(GameTestHelper h) {
        Tf.hasEffect(h, melee(h, EntityType.WITHER), MobEffects.WITHER, 1, 100, "Wither: Wither II 5s");
        Tf.hasEffect(h, melee(h, EntityType.WITHER_SKELETON), MobEffects.WITHER, 0, 200, "Wither Skeleton: Wither I 10s");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void ravagerKnocksTargetsFarBack(GameTestHelper h) {
        Pig pig = melee(h, EntityType.RAVAGER); // pig is 2 blocks in +z from the player
        Vec3 v = pig.getDeltaMovement();
        near(h, 0.0, v.x, "no sideways push");
        near(h, 0.3, v.y, "lifted");
        near(h, 1.2, v.z, "pushed away 3x the usual 0.4");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void ironGolemPunchesLaunchTargetsUp(GameTestHelper h) {
        Pig pig = melee(h, EntityType.IRON_GOLEM);
        near(h, 0.5, pig.getDeltaMovement().y, "launched");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void meleeEffectsNeedADirectHitFromATransformedPlayer(GameTestHelper h) {
        Pig plain = melee(h, EntityType.COW);
        eq(h, 0, plain.getActiveEffects().size(), "cow form adds nothing");

        Pig human = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 4));
        ServerPlayer p = Tf.player(h);
        hit(h, p, human);
        eq(h, 0, human.getActiveEffects().size(), "untransformed adds nothing");

        Pig shot = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 4));
        ServerPlayer bee = Tf.playerAs(h, EntityType.BEE);
        Arrow arrow = new Arrow(h.getLevel(), bee, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        LivingIncomingDamageEvent e = incoming(shot, h.getLevel().damageSources().arrow(arrow, bee), 1f);
        TransformationAbilityHandler.onLivingHurt(e);
        eq(h, 0, shot.getActiveEffects().size(), "projectile hit from a bee form adds nothing");
        h.succeed();
    }

    // ── per-tick abilities ────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void spidersClimbWalls(GameTestHelper h) {
        for (EntityType<?> t : new EntityType<?>[]{EntityType.SPIDER, EntityType.CAVE_SPIDER}) {
            ServerPlayer p = Tf.playerAs(h, t);
            p.horizontalCollision = true;
            p.fallDistance = 3f;
            p.zza = 1f;
            p.setDeltaMovement(0, -0.3, 0);
            TransformationAbilityHandler.tickSpiderClimb(p);
            near(h, 0.2, p.getDeltaMovement().y, t + " climbs when pressing forward");
            near(h, 0.0, p.fallDistance, t + " fall distance cleared");
            p.zza = 0f;
            TransformationAbilityHandler.tickSpiderClimb(p);
            near(h, 0.0, p.getDeltaMovement().y, t + " clings with no input");
            p.setShiftKeyDown(true);
            TransformationAbilityHandler.tickSpiderClimb(p);
            near(h, -0.2, p.getDeltaMovement().y, t + " descends while sneaking");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlySpidersAgainstAWallClimb(GameTestHelper h) {
        ServerPlayer spider = Tf.playerAs(h, EntityType.SPIDER);
        spider.horizontalCollision = false;
        spider.zza = 1f;
        spider.setDeltaMovement(0, -0.3, 0);
        TransformationAbilityHandler.tickSpiderClimb(spider);
        near(h, -0.3, spider.getDeltaMovement().y, "no wall, no climb");
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        cow.horizontalCollision = true;
        cow.zza = 1f;
        cow.setDeltaMovement(0, -0.3, 0);
        TransformationAbilityHandler.tickSpiderClimb(cow);
        near(h, -0.3, cow.getDeltaMovement().y, "cow cannot climb");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void snowGolemsLeaveSnowInCoolBiomes(GameTestHelper h) {
        ServerPlayer p = Tf.playerAs(h, EntityType.SNOW_GOLEM);
        BlockPos feet = p.blockPosition();
        float temperature = h.getLevel().getBiome(feet).value().getBaseTemperature();
        TransformationAbilityHandler.tickSnowGolemTrail(p);
        boolean snow = h.getLevel().getBlockState(feet).is(Blocks.SNOW);
        if (temperature <= 1.0f) check(h, snow, "snow at the feet (biome temperature " + temperature + ")");
        else check(h, !snow, "no snow in a hot biome (temperature " + temperature + ")");

        h.getLevel().setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        TransformationAbilityHandler.tickSnowGolemTrail(cow);
        check(h, h.getLevel().getBlockState(feet).isAir(), "cow form leaves no snow");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void silverfishSlipsThroughUpToThreeBlocksOfStone(GameTestHelper h) {
        for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(2, 2, z), Blocks.STONE);
        ServerPlayer p = silverfishFacingWall(h);
        TransformationAbilityHandler.tickSilverfishPhase(p);
        Vec3 dest = h.relativeVec(p.position());
        near(h, 4.5, dest.z, "came out the far side");
        near(h, 2.5, dest.x, "same column");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void silverfishCannotPassFourBlocksOrNonStone(GameTestHelper h) {
        for (int z = 1; z <= 4; z++) h.setBlock(new BlockPos(2, 2, z), Blocks.STONE);
        ServerPlayer thick = silverfishFacingWall(h);
        double z0 = thick.getZ();
        TransformationAbilityHandler.tickSilverfishPhase(thick);
        near(h, z0, thick.getZ(), "four blocks is too thick");

        h.setBlock(new BlockPos(1, 2, 1), Blocks.OBSIDIAN);
        ServerPlayer obsidian = silverfishFacingWall(h);
        obsidian.moveTo(Tf.abs(h, 1.5, 2, 0.5));
        TransformationAbilityHandler.tickSilverfishPhase(obsidian);
        near(h, Tf.abs(h, 0, 0, 0.5).z, obsidian.getZ(), "obsidian is not phaseable");

        ServerPlayer cow = Tf.teleportable(Tf.playerAs(h, EntityType.COW));
        h.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
        cow.moveTo(Tf.abs(h, 2.5, 2, 0.5));
        cow.setYRot(0f);
        cow.horizontalCollision = true;
        TransformationAbilityHandler.tickSilverfishPhase(cow);
        near(h, Tf.abs(h, 0, 0, 0.5).z, cow.getZ(), "cow form cannot phase");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void silverfishPhaseHasAOneSecondCooldown(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
        ServerPlayer p = silverfishFacingWall(h);
        TransformationAbilityHandler.tickSilverfishPhase(p);
        near(h, 2.5, h.relativeVec(p.position()).z, "phased through one block");
        back(h, p);
        TransformationAbilityHandler.tickSilverfishPhase(p);
        near(h, 0.5, h.relativeVec(p.position()).z, "on cooldown at once");
        h.runAfterDelay(15, () -> {
            back(h, p);
            TransformationAbilityHandler.tickSilverfishPhase(p);
            near(h, 0.5, h.relativeVec(p.position()).z, "still on cooldown after 0.75s");
        });
        h.runAfterDelay(21, () -> {
            back(h, p);
            TransformationAbilityHandler.tickSilverfishPhase(p);
            near(h, 2.5, h.relativeVec(p.position()).z, "ready after 1s");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void villagerAndGolemFormsGetHeroOfTheVillageNearVillagers(GameTestHelper h) {
        var villager = h.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(0, 2, 0));
        for (EntityType<?> t : new EntityType<?>[]{EntityType.VILLAGER, EntityType.IRON_GOLEM}) {
            ServerPlayer p = Tf.playerAs(h, t);
            TransformationAbilityHandler.tickVillagerDiscount(p);
            Tf.hasEffect(h, p, MobEffects.HERO_OF_THE_VILLAGE, 0, 220, t + " near a villager");
        }
        ServerPlayer p = Tf.playerAs(h, EntityType.VILLAGER);
        TransformationAbilityHandler.tickVillagerDiscount(p);
        TransformationManager.transform(p, EntityType.COW);
        TransformationAbilityHandler.tickVillagerDiscount(p);
        check(h, !p.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "removed in another form");

        ServerPlayer q = Tf.playerAs(h, EntityType.VILLAGER);
        TransformationAbilityHandler.tickVillagerDiscount(q);
        villager.discard();
        TransformationAbilityHandler.tickVillagerDiscount(q);
        check(h, !q.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "removed when no villager is near");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void witchFormShakesOffPoison(GameTestHelper h) {
        ServerPlayer witch = Tf.playerAs(h, EntityType.WITCH);
        witch.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        TransformationAbilityHandler.tickWitchPoisonImmunity(witch);
        check(h, !witch.hasEffect(MobEffects.POISON), "poison removed from a witch form");
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        cow.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        TransformationAbilityHandler.tickWitchPoisonImmunity(cow);
        check(h, cow.hasEffect(MobEffects.POISON), "cow form keeps its poison");
        h.succeed();
    }

    // ── helpers ───────────────────────────────────────────────────

    private static <T extends Entity> int discardCount(ServerPlayer p, Class<T> cls) {
        List<T> found = Tf.near(p, cls, 8);
        Tf.discard(found);
        return found.size();
    }

    private static void aim(ServerPlayer p, LivingEntity target) {
        p.lookAt(EntityAnchorArgument.Anchor.EYES, target.position().add(0, target.getBbHeight() * 0.5, 0));
    }

    private static List<MobEffectInstance> arrowEffects(Arrow arrow) {
        // An arrow's own effects now live on the item it would drop, as potion contents
        PotionContents contents = arrow.getPickupItemStackOrigin()
                .getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return new ArrayList<>(contents.customEffects());
    }

    private static void effectIs(GameTestHelper h, MobEffectInstance inst, Holder<MobEffect> effect, int amp, int dur, String what) {
        check(h, inst != null && inst.getEffect().is(effect), what + ": wrong effect " + inst);
        eq(h, amp, inst.getAmplifier(), what + " amplifier");
        eq(h, dur, inst.getDuration(), what + " duration");
    }

    /** A fresh NoAI pig two blocks in front (+z) of a player in {@code form}, hit once in melee. */
    private static Pig melee(GameTestHelper h, EntityType<?> form) {
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 4));
        hit(h, Tf.playerAs(h, form), pig);
        return pig;
    }

    private static void hit(GameTestHelper h, ServerPlayer attacker, LivingEntity target) {
        LivingIncomingDamageEvent e = incoming(target, h.getLevel().damageSources().playerAttack(attacker), 1f);
        TransformationAbilityHandler.onLivingHurt(e);
    }

    private static ServerPlayer silverfishFacingWall(GameTestHelper h) {
        ServerPlayer p = Tf.teleportable(Tf.playerAs(h, EntityType.SILVERFISH));
        back(h, p);
        return p;
    }

    /** Silverfish start: (2.5, 2, 0.5), facing +z, pressed against the wall. */
    private static void back(GameTestHelper h, ServerPlayer p) {
        p.moveTo(Tf.abs(h, 2.5, 2, 0.5));
        p.setYRot(0f);
        p.setXRot(0f);
        p.horizontalCollision = true;
    }
}
