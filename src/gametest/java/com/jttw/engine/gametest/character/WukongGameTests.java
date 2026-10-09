package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.wukong.WuCloneManager;
import com.jttw.engine.character.wukong.WukongAbilityHandler;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.character.wukong.WukongCheckpoints.Checkpoint;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.gametest.GT;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Sun Wukong: Ring of Fire, Fiery Golden Eyes, Shadow Clones, immortality, martial arts, checkpoints. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class WukongGameTests {

    private static ServerPlayer wukong(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        qi(p, 100f, 100f);
        return p;
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.CELESTIAL_PRIMATE.id);
    }

    // ── Ring of Fire (slot 2, held) ─────────────────────────────────

    @GameTest(template = PLATFORM, batch = "ringOfFireDoesNothingWithoutRingTierOne")
    public static void ringOfFireDoesNothingWithoutRingTierOne(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        character().activate(p, 2, AbilityAction.START);
        h.runAfterDelay(5, () -> {
            near(h, 100f, QiData.getCurrent(p), "no Qi drained");
            check(h, pig.getRemainingFireTicks() <= 0, "pig not burning, fire ticks " + pig.getRemainingFireTicks());
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "ringOfFireTierOneBurnsPushesAndDrainsTwoQiPerSecond")
    public static void ringOfFireTierOneBurnsPushesAndDrainsTwoQiPerSecond(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        grant(p, "wu_ring_1");
        Pig pig = mob(h, EntityType.PIG, 2, 4); // 2 blocks south of the player
        List<Hit> hits = watch(pig);
        long start = h.getTick();
        character().activate(p, 2, AbilityAction.START);
        h.runAfterDelay(1, () -> {
            // read straight after the ring's server tick, before the pig's own movement damps it
            check(h, pig.getDeltaMovement().z > 1.0, "pig pushed away (south), motion " + pig.getDeltaMovement());
            int fire = pig.getRemainingFireTicks();
            check(h, fire == 40 || fire == 39, "tier 1 sets 40 fire ticks (one may have burned off), was " + fire);
        });
        h.runAfterDelay(10, () -> {
            long ticks = h.getTick() - start;
            // 0.1 Qi per server tick
            close(h, 100.0 - 0.1 * ticks, QiData.getCurrent(p), 0.11, "Qi after " + ticks + " ticks");
            check(h, !of(hits, DamageTypes.MAGIC).isEmpty(), "pig hit by ring magic damage");
            near(h, 1.0, of(hits, DamageTypes.MAGIC).get(0).amount(), "ring damage per hit");
            WukongAbilityHandler.stopRing(p);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "ringOfFireTierTwoRestoresMoreQiThanItDrains")
    public static void ringOfFireTierTwoRestoresMoreQiThanItDrains(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        grant(p, "wu_ring_1", "wu_ring_2");
        QiData.setCurrent(p, 50f);
        long start = h.getTick();
        WukongAbilityHandler.startRing(p);
        h.runAfterDelay(10, () -> {
            long ticks = h.getTick() - start;
            // drains 0.1, restores 0.15: net +0.05 per tick
            close(h, 50.0 + 0.05 * ticks, QiData.getCurrent(p), 0.06, "Qi after " + ticks + " ticks");
            WukongAbilityHandler.stopRing(p);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "ringOfFireTierThreeBurnsLongerAndThrowsMobsToTheEdge")
    public static void ringOfFireTierThreeBurnsLongerAndThrowsMobsToTheEdge(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        grant(p, "wu_ring_1", "wu_ring_2", "wu_ring_3");
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        WukongAbilityHandler.startRing(p);
        h.runAfterDelay(1, () -> {
            check(h, pig.getRemainingFireTicks() > 40, "tier 3 fire longer than 40 ticks, was " + pig.getRemainingFireTicks());
            double dx = pig.getX() - p.getX(), dz = pig.getZ() - p.getZ();
            check(h, Math.sqrt(dx * dx + dz * dz) > 4.5,
                    "pig thrown out toward the ring's 5-block edge, at " + Math.sqrt(dx * dx + dz * dz));
            WukongAbilityHandler.stopRing(p);
            h.succeed();
        });
    }

    /** Tier 3 throws mobs outward horizontally, keeping their height, not down into the floor. */
    @GameTest(template = PLATFORM, batch = "ringOfFireTierThreeDoesNotBuryMobsInTheFloor")
    public static void ringOfFireTierThreeDoesNotBuryMobsInTheFloor(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        grant(p, "wu_ring_1", "wu_ring_2", "wu_ring_3");
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        double floorTop = pig.getY();
        WukongAbilityHandler.startRing(p);
        h.runAfterDelay(1, () -> {
            WukongAbilityHandler.stopRing(p);
            check(h, pig.getY() >= floorTop - 0.01,
                    "pig stays on the floor (y " + floorTop + "), but was moved to y " + pig.getY());
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "ringOfFireGoesOutWhenQiRunsOut")
    public static void ringOfFireGoesOutWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        grant(p, "wu_ring_1");
        QiData.setCurrent(p, 0.25f);
        WukongAbilityHandler.startRing(p);
        h.runAfterDelay(5, () -> {
            check(h, QiData.getCurrent(p) < 0.1f, "Qi drained to below one tick's cost");
            QiData.setCurrent(p, 50f);
            h.runAfterDelay(3, () -> {
                near(h, 50f, QiData.getCurrent(p), "ring is off: refilled Qi is not drained");
                h.succeed();
            });
        });
    }

    @GameTest(template = PLATFORM, batch = "releasingTheRingKeyStopsTheDrain")
    public static void releasingTheRingKeyStopsTheDrain(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        grant(p, "wu_ring_1");
        character().activate(p, 2, AbilityAction.START);
        h.runAfterDelay(3, () -> {
            check(h, QiData.getCurrent(p) < 100f, "draining while held");
            character().activate(p, 2, AbilityAction.STOP);
            float after = QiData.getCurrent(p);
            h.runAfterDelay(3, () -> {
                near(h, after, QiData.getCurrent(p), "no drain after release");
                h.succeed();
            });
        });
    }

    // ── Fiery Golden Eyes (slot 3) ─────────────────────────────────

    private static Pig pigOverhead(GameTestHelper h, ServerPlayer p) {
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 4, 2));
        p.setXRot(-90f); // look straight up, so the beam only crosses open air
        return pig;
    }

    @GameTest(template = PLATFORM, setupTicks = 100, batch = "goldenEyesDoNothingUntilEarned")
    public static void goldenEyesDoNothingUntilEarned(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        Pig pig = pigOverhead(h, p);
        List<Hit> hits = watch(pig);
        WukongCharacter.INSTANCE.activate(p, 3, AbilityAction.USE); // laser
        p.setShiftKeyDown(true);
        WukongCharacter.INSTANCE.activate(p, 3, AbilityAction.USE); // x-ray
        h.runAfterDelay(5, () -> {
            near(h, 100f, QiData.getCurrent(p), "Qi untouched");
            check(h, hits.isEmpty(), "no laser hit");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, setupTicks = 100, batch = "eyeLaserCostsTenQiAndBurnsWhatItHitsForEight")
    public static void eyeLaserCostsTenQiAndBurnsWhatItHitsForEight(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        PlayerLevel.setLevel(p, 20); // 8 at level 1, scaled by his power
        Pig pig = pigOverhead(h, p);
        List<Hit> hits = watch(pig);
        WukongCharacter.INSTANCE.activate(p, 3, AbilityAction.USE);
        near(h, 90f, QiData.getCurrent(p), "Qi after the laser");
        List<Hit> magic = of(hits, DamageTypes.MAGIC);
        check(h, !magic.isEmpty(), "pig in the beam is hit");
        near(h, 8.0 * LevelPower.of(p), magic.get(0).amount(), "laser damage at level 20, by his own power");
        eq(h, 60, pig.getRemainingFireTicks(), "pig set on fire for 3 s");
        h.succeed();
    }

    @GameTest(template = PLATFORM, setupTicks = 100, timeoutTicks = 200, batch = "eyeLaserHasAFiveSecondCooldown")
    public static void eyeLaserHasAFiveSecondCooldown(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        p.setXRot(-90f);
        WukongAbilityHandler.fireEyeLaser(p);
        near(h, 90f, QiData.getCurrent(p), "first shot");
        WukongAbilityHandler.fireEyeLaser(p);
        near(h, 90f, QiData.getCurrent(p), "second shot refused during cooldown");
        h.runAfterDelay(95, () -> {
            WukongAbilityHandler.fireEyeLaser(p);
            near(h, 90f, QiData.getCurrent(p), "still on cooldown before 100 ticks");
            h.runAfterDelay(6, () -> {
                WukongAbilityHandler.fireEyeLaser(p);
                near(h, 80f, QiData.getCurrent(p), "fires again after 100 ticks");
                h.succeed();
            });
        });
    }

    @GameTest(template = PLATFORM, setupTicks = 100, batch = "eyeLaserWithoutEnoughQiIsRefused")
    public static void eyeLaserWithoutEnoughQiIsRefused(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        Pig pig = pigOverhead(h, p);
        List<Hit> hits = watch(pig);
        QiData.setCurrent(p, 9.5f);
        WukongAbilityHandler.fireEyeLaser(p);
        near(h, 9.5f, QiData.getCurrent(p), "nothing spent");
        check(h, hits.isEmpty(), "no beam");
        h.succeed();
    }

    /** A shot refused for lack of Qi does not start the laser's cooldown. */
    @GameTest(template = PLATFORM, setupTicks = 100, batch = "eyeLaserRefusedForQiDoesNotStartTheCooldown")
    public static void eyeLaserRefusedForQiDoesNotStartTheCooldown(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        p.setXRot(-90f);
        QiData.setCurrent(p, 5f);
        WukongAbilityHandler.fireEyeLaser(p);
        QiData.setCurrent(p, 50f);
        WukongAbilityHandler.fireEyeLaser(p);
        near(h, 40f, QiData.getCurrent(p), "second shot, now affordable, fires");
        h.succeed();
    }

    @GameTest(template = PLATFORM, setupTicks = 100, batch = "sneakingWithGoldenEyesTogglesXrayWhichDrainsQiUntilToggledOff")
    public static void sneakingWithGoldenEyesTogglesXrayWhichDrainsQiUntilToggledOff(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        Pig pig = pigOverhead(h, p);
        List<Hit> hits = watch(pig);
        p.setShiftKeyDown(true);
        long start = h.getTick();
        WukongCharacter.INSTANCE.activate(p, 3, AbilityAction.USE);
        check(h, hits.isEmpty(), "sneaking does not fire the laser");
        h.runAfterDelay(10, () -> {
            long ticks = h.getTick() - start;
            close(h, 100.0 - 0.05 * ticks, QiData.getCurrent(p), 0.06, "x-ray drains 0.05 Qi per tick");
            WukongCharacter.INSTANCE.activate(p, 3, AbilityAction.USE);
            float after = QiData.getCurrent(p);
            h.runAfterDelay(3, () -> {
                near(h, after, QiData.getCurrent(p), "x-ray off: no drain");
                h.succeed();
            });
        });
    }

    // ── Full immortality and damage rules ───────────────────────────

    private static boolean cancels(ServerPlayer p, DamageSource src) {
        LivingHurtEvent ev = new LivingHurtEvent(p, src, 5f);
        WukongAbilityHandler.onImmortalityDamage(ev);
        return ev.isCanceled();
    }

    private static void immortal(ServerPlayer p) {
        p.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PILLS, true);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES, true);
    }

    @GameTest(template = PLATFORM, batch = "fullImmortalityBlocksProjectilesExplosionsFireAndLightning")
    public static void fullImmortalityBlocksProjectilesExplosionsFireAndLightning(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        immortal(p);
        Husk shooter = mob(h, EntityType.HUSK, 0, 0);
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        var ds = h.getLevel().damageSources();
        check(h, cancels(p, ds.arrow(arrow, shooter)), "arrow");
        check(h, cancels(p, ds.explosion(null, null)), "explosion");
        check(h, cancels(p, ds.inFire()), "fire");
        check(h, cancels(p, ds.lightningBolt()), "lightning");
        check(h, !cancels(p, ds.mobAttack(shooter)), "melee still hurts");
        check(h, !cancels(p, ds.generic()), "generic still hurts");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "immortalityNeedsBothPillsAndPeaches")
    public static void immortalityNeedsBothPillsAndPeaches(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PILLS, true);
        check(h, !cancels(p, h.getLevel().damageSources().inFire()), "pills alone");
        p.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PILLS, false);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES, true);
        check(h, !cancels(p, h.getLevel().damageSources().inFire()), "peaches alone");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "immortalityFlagsDoNothingForOtherCharacters")
    public static void immortalityFlagsDoNothingForOtherCharacters(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.YUAN_DI);
        immortal(p);
        check(h, !cancels(p, h.getLevel().damageSources().inFire()), "Yuan Di with both flags");
        LivingKnockBackEvent kb = new LivingKnockBackEvent(p, 0.4f, 1, 0);
        WukongAbilityHandler.onKnockback(kb);
        check(h, !kb.isCanceled(), "Yuan Di is still knocked back");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fullImmortalityPreventsKnockback")
    public static void fullImmortalityPreventsKnockback(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        LivingKnockBackEvent before = new LivingKnockBackEvent(p, 0.4f, 1, 0);
        WukongAbilityHandler.onKnockback(before);
        check(h, !before.isCanceled(), "knocked back before immortality");
        immortal(p);
        LivingKnockBackEvent after = new LivingKnockBackEvent(p, 0.4f, 1, 0);
        WukongAbilityHandler.onKnockback(after);
        check(h, after.isCanceled(), "no knockback once immortal");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "wukongsPhysicalResistanceAlsoGuardsAgainstMagic")
    public static void wukongsPhysicalResistanceAlsoGuardsAgainstMagic(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        setResistances(p, 50.0, 50.0);
        var src = h.getLevel().damageSources();
        // magic: 50 + 50 = 100 resistance -> (40 - 10) x 100/200
        near(h, 15.0, afterResistance(p, src.magic(), 40f), "magic");
        near(h, 15.0, afterResistance(p, src.wither(), 40f), "wither");
        // physical: 50 -> (40 - 5) x 100/150
        near(h, 35.0 * 100 / 150, afterResistance(p, src.cactus(), 40f), "physical");

        ServerPlayer other = as(h, ReincarnationOutcome.YUAN_DI);
        setResistances(other, 50.0, 50.0);
        near(h, 35.0 * 100 / 150, afterResistance(other, src.magic(), 40f), "others use magic resistance only");
        h.succeed();
    }

    // ── Martial Arts passive ────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "martialArtsTiersAddSpeedThenAttackAndResistance")
    public static void martialArtsTiersAddSpeedThenAttackAndResistance(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.tickCount = 20;
        TickEvent.PlayerTickEvent tick = new TickEvent.PlayerTickEvent(TickEvent.Phase.START, p);

        WukongAbilityHandler.onPlayerTickMartial(tick);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:martial_speed"), "no speed bonus untrained");

        grant(p, "wu_martial_1");
        WukongAbilityHandler.onPlayerTickMartial(tick);
        eq(h, 0.15, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:martial_speed"), "tier 1 speed");
        eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:martial_atk"), "no attack bonus at tier 1");

        grant(p, "wu_martial_2", "wu_martial_3");
        WukongAbilityHandler.onPlayerTickMartial(tick);
        eq(h, 0.30, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:martial_atk"), "tier 3 attack");
        eq(h, 24.0, modifier(p, PHYSICAL_RESISTANCE(), "jttw:martial_pres"), "tier 3 physical resistance");
        eq(h, null, modifier(p, Attributes.ARMOR, "jttw:martial_armor"), "no vanilla armor");
        h.succeed();
    }

    // ── Shadow Clones (slot 1) ───────────────────────────────────────

    private static List<WuCloneEntity> clones(ServerPlayer p) {
        return near(p, WuCloneEntity.class, 8).stream()
                .filter(c -> c.getOwnerUUID().map(p.getUUID()::equals).orElse(false))
                .toList();
    }

    private static void summon(ServerPlayer p, int times) {
        for (int i = 0; i < times; i++) WukongCharacter.INSTANCE.activate(p, 1, AbilityAction.USE);
    }

    @GameTest(template = PLATFORM, batch = "shadowCloneNeedsTheCloneNode")
    public static void shadowCloneNeedsTheCloneNode(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        summon(p, 1);
        eq(h, 0, clones(p).size(), "clones");
        near(h, 100f, QiData.getCurrent(p), "Qi");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierOneSummonsOneCloneForTenQi")
    public static void tierOneSummonsOneCloneForTenQi(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1");
        PlayerLevel.setLevel(p, 20); // clone health is 20 at level 1, scaled by its owner's power
        summon(p, 1);
        List<WuCloneEntity> one = clones(p);
        eq(h, 1, one.size(), "clones after one press");
        near(h, 90f, QiData.getCurrent(p), "Qi after summoning");
        check(h, WuCloneManager.isActive(p.getUUID()), "manager tracks the clone");
        near(h, 20.0 * LevelPower.of(p), one.get(0).getMaxHealth(), "clone max health at level 20, by his own power");
        summon(p, 1);
        eq(h, 1, clones(p).size(), "tier 1 caps at one clone");
        near(h, 90f, QiData.getCurrent(p), "capped press costs nothing");
        WuCloneManager.dismissAll(p);
        h.succeed();
    }

    /** A clone carries its owner's resistances, uncapped (it used to copy armor, capped at 20). */
    @GameTest(template = PLATFORM, batch = "cloneCopiesItsOwnersResistances")
    public static void cloneCopiesItsOwnersResistances(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1");
        setResistances(p, 130.0, 10.0);
        summon(p, 1);
        WuCloneEntity clone = clones(p).get(0);
        near(h, 130.0, pres(clone), "physical resistance");
        // Wukong's magic resistance counts his physical resistance too: 10 + 130
        near(h, 140.0, mres(clone), "magic resistance");
        near(h, 0.0, clone.getAttributeValue(Attributes.ARMOR), "no vanilla armor");
        WuCloneManager.dismissAll(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierTwoCapsAtTwoClones")
    public static void tierTwoCapsAtTwoClones(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1", "wu_clone_2");
        summon(p, 3);
        eq(h, 2, clones(p).size(), "clones");
        near(h, 80f, QiData.getCurrent(p), "Qi");
        WuCloneManager.dismissAll(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierThreeCapsAtNineClones")
    public static void tierThreeCapsAtNineClones(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1", "wu_clone_2", "wu_clone_3");
        summon(p, 10);
        eq(h, 9, clones(p).size(), "clones");
        near(h, 10f, QiData.getCurrent(p), "Qi");
        WuCloneManager.dismissAll(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "cloneSummonWithoutTenQiIsRefused")
    public static void cloneSummonWithoutTenQiIsRefused(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1");
        QiData.setCurrent(p, 9f);
        summon(p, 1);
        eq(h, 0, clones(p).size(), "clones");
        near(h, 9f, QiData.getCurrent(p), "Qi");
        check(h, !WuCloneManager.isActive(p.getUUID()), "nothing tracked");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "dismissAllRemovesEveryClone")
    public static void dismissAllRemovesEveryClone(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1", "wu_clone_2");
        summon(p, 2);
        eq(h, 2, clones(p).size(), "before");
        WuCloneManager.dismissAll(p);
        eq(h, 0, clones(p).size(), "after");
        check(h, !WuCloneManager.isActive(p.getUUID()), "not active");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "hittingYourOwnCloneDismissesItWithoutDamage")
    public static void hittingYourOwnCloneDismissesItWithoutDamage(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1", "wu_clone_2");
        summon(p, 2);
        WuCloneEntity target = clones(p).get(0);
        AttackEntityEvent hit = new AttackEntityEvent(p, target);
        WuCloneManager.onPlayerAttacks(hit);
        check(h, hit.isCanceled(), "attack cancelled");
        check(h, target.isRemoved(), "that clone is gone");
        eq(h, 1, clones(p).size(), "the other clone stays");

        ServerPlayer stranger = wukong(h);
        WuCloneEntity other = clones(p).get(0);
        AttackEntityEvent strangerHit = new AttackEntityEvent(stranger, other);
        WuCloneManager.onPlayerAttacks(strangerHit);
        check(h, !strangerHit.isCanceled() && !other.isRemoved(), "someone else's hit is a normal attack");
        WuCloneManager.dismissAll(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "clonesDrainQiAndVanishWhenItRunsOut")
    public static void clonesDrainQiAndVanishWhenItRunsOut(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        grant(p, "wu_clone_1", "wu_clone_2");
        QiData.setCurrent(p, 21f);
        summon(p, 2); // 1 Qi left; two clones drain 0.1 per tick
        eq(h, 2, clones(p).size(), "two clones");
        h.succeedWhen(() -> {
            check(h, !WuCloneManager.isActive(p.getUUID()), "clones dismissed for lack of Qi");
            eq(h, 0, clones(p).size(), "clone entities gone");
        });
    }

    // ── Checkpoints ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "currentBlockerIsTheEarliestUnfinishedCheckpointReached")
    public static void currentBlockerIsTheEarliestUnfinishedCheckpointReached(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        PlayerLevel.setLevel(p, 29);
        eq(h, null, WukongCheckpoints.getCurrentBlocker(p), "below the first checkpoint");
        PlayerLevel.setLevel(p, 55);
        eq(h, Checkpoint.HELLS_BOOK, WukongCheckpoints.getCurrentBlocker(p), "earliest unfinished");
        WukongCheckpoints.complete(p, Checkpoint.HELLS_BOOK);
        eq(h, Checkpoint.IMMORTAL_PEACHES, WukongCheckpoints.getCurrentBlocker(p), "next one");
        check(h, !WukongCheckpoints.isBlocked(p), "XP is only blocked at a checkpoint's exact level");
        PlayerLevel.setLevel(p, 50);
        check(h, WukongCheckpoints.isBlocked(p), "blocked at 50 (Heavenly Wine)");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "checkpointsNeverBlockNonPrimordialClasses")
    public static void checkpointsNeverBlockNonPrimordialClasses(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 30);
        check(h, !WukongCheckpoints.isBlocked(p), "Heavenly class at 30");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "projectileImmunityCancelsOnlyIndirectDamage")
    public static void projectileImmunityCancelsOnlyIndirectDamage(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        var ds = h.getLevel().damageSources();

        LivingHurtEvent before = new LivingHurtEvent(p, ds.arrow(arrow, husk), 4f);
        WukongCheckpoints.onHurt(before);
        check(h, !before.isCanceled(), "arrows hurt without the flag");

        p.getPersistentData().putBoolean("jttw_projectile_immune", true);
        LivingHurtEvent shot = new LivingHurtEvent(p, ds.arrow(arrow, husk), 4f);
        WukongCheckpoints.onHurt(shot);
        check(h, shot.isCanceled(), "arrow cancelled");
        LivingHurtEvent boom = new LivingHurtEvent(p, ds.sonicBoom(husk), 4f);
        WukongCheckpoints.onHurt(boom);
        check(h, boom.isCanceled(), "sonic boom cancelled");
        LivingHurtEvent punch = new LivingHurtEvent(p, ds.mobAttack(husk), 4f);
        WukongCheckpoints.onHurt(punch);
        check(h, !punch.isCanceled(), "melee still hurts");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "checkpointProgressSurvivesRespawn")
    public static void checkpointProgressSurvivesRespawn(GameTestHelper h) {
        ServerPlayer before = wukong(h);
        ServerPlayer after = GT.player(h);
        WukongCheckpoints.complete(before, Checkpoint.HELLS_BOOK);
        before.getPersistentData().putBoolean("jttw_projectile_immune", true);
        WukongCheckpoints.copyTo(before, after);
        check(h, WukongCheckpoints.isComplete(after, Checkpoint.HELLS_BOOK), "checkpoint");
        check(h, !WukongCheckpoints.isComplete(after, Checkpoint.IMMORTAL_PEACHES), "unfinished stays unfinished");
        check(h, after.getPersistentData().getBoolean("jttw_projectile_immune"), "projectile immunity");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "wukongKeysReachTheirAbilitiesOnlyOnTheRightAction")
    public static void wukongKeysReachTheirAbilitiesOnlyOnTheRightAction(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_clone_1");
        PlayableCharacter c = character();
        c.activate(p, 1, AbilityAction.START);
        c.activate(p, 1, AbilityAction.STOP);
        eq(h, 0, clones(p).size(), "clone key ignores START/STOP");
        c.activate(p, 1, AbilityAction.USE);
        eq(h, 1, clones(p).size(), "clone key USE summons");
        c.activate(p, 0, AbilityAction.USE); // 72 Transformations is a client-side menu
        near(h, 90f, QiData.getCurrent(p), "slot 0 does nothing server-side");
        WuCloneManager.dismissAll(p);
        h.succeed();
    }
}
