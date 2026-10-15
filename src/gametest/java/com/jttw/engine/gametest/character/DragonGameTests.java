package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.dragon.DragonAbilityHandler;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The Dragon: Call Thunder, Weather Control, Tidal Wave, Dimension Travel (Shift + V), Scales, Tidal Force. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class DragonGameTests {

    private static ServerPlayer dragon(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        qi(p, 100f, 100f);
        return p;
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.DRAGON.id);
    }

    /** Attack attribute fixed at 3 so lightning damage is 4 + 3 x 0.5 = 5.5 before the tier multiplier. */
    private static void attackThree(ServerPlayer p) {
        var atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
        atk.removeModifiers();
        atk.setBaseValue(3.0);
    }

    // ── Lightning Strike (slot 0) ───────────────────────────────────

    @GameTest(template = PLATFORM, batch = "lightningNeedsTheNode")
    public static void lightningNeedsTheNode(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        p.setXRot(90f);
        character().use(p, "dr_lightning", AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "Qi");
        eq(h, 0, near(p, LightningBolt.class, 6).size(), "no bolt");
        h.succeed();
    }

    /** At level 20 the fixed 4 grows by the level's power; the attack share is the attribute's own. */
    @GameTest(template = PLATFORM, batch = "lightningCostsEightQiAndScalesWithAttack")
    public static void lightningCostsEightQiAndScalesWithAttack(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_lightning_1");
        PlayerLevel.setLevel(p, 20);
        LevelManager.reapplyStats(p);
        attackThree(p);
        p.setXRot(90f); // strike the floor at the player's feet
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 3);
        List<Hit> hits = watch(golem);
        character().use(p, "dr_lightning", AbilityAction.USE);
        near(h, 92f, QiData.getCurrent(p), "Qi");
        eq(h, 1, near(p, LightningBolt.class, 6).size(), "one bolt");
        List<Hit> magic = of(hits, DamageTypes.INDIRECT_MAGIC);
        eq(h, 1, magic.size(), "one strike hit");
        near(h, 4.0 * LevelPower.of(20) + 1.5, magic.get(0).amount(), "4 x power + attack x 0.5");
        eq(h, -1, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "no slow at tier 1");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "lightningTierTwoHitsHarderAndSlows")
    public static void lightningTierTwoHitsHarderAndSlows(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_lightning_1", "dr_lightning_2");
        attackThree(p);
        p.setXRot(90f);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 3);
        List<Hit> hits = watch(golem);
        DragonAbilityHandler.castLightning(p);
        near(h, 5.5 * 1.5, of(hits, DamageTypes.INDIRECT_MAGIC).get(0).amount(), "x1.5");
        eq(h, 1, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "Slowness II");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "lightningTierThreeCallsThreeBoltsThatBurn")
    public static void lightningTierThreeCallsThreeBoltsThatBurn(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_lightning_1", "dr_lightning_2", "dr_lightning_3");
        attackThree(p);
        p.setXRot(90f);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 3);
        List<Hit> hits = watch(golem);
        DragonAbilityHandler.castLightning(p);
        eq(h, 3, near(p, LightningBolt.class, 6).size(), "three bolts");
        near(h, 5.5 * 2.5, of(hits, DamageTypes.INDIRECT_MAGIC).get(0).amount(), "x2.5");
        eq(h, 100, golem.getRemainingFireTicks(), "set on fire for 5 s");
        near(h, 92f, QiData.getCurrent(p), "still one 8-Qi cast");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "lightningWithoutEightQiIsRefused")
    public static void lightningWithoutEightQiIsRefused(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_lightning_1");
        p.setXRot(90f);
        QiData.setCurrent(p, 7.5f);
        DragonAbilityHandler.castLightning(p);
        near(h, 7.5f, QiData.getCurrent(p), "Qi");
        eq(h, 0, near(p, LightningBolt.class, 6).size(), "no bolt");
        h.succeed();
    }

    /** Lightning Strike's 20-tick cooldown is enforced on the server, not just shown on the HUD. */
    @GameTest(template = PLATFORM, batch = "lightningCannotBeRecastWithinItsOneSecondCooldown")
    public static void lightningCannotBeRecastWithinItsOneSecondCooldown(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_lightning_1");
        p.setXRot(90f);
        DragonAbilityHandler.castLightning(p);
        DragonAbilityHandler.castLightning(p);
        near(h, 92f, QiData.getCurrent(p), "second cast in the same tick should be refused");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "dragonsOwnLightningDoesNotHurtThem")
    public static void dragonsOwnLightningDoesNotHurtThem(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        ServerPlayer other = dragon(h);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(h.getLevel());
        bolt.setCause(p);
        LivingIncomingDamageEvent own = incoming(p, src(h, DamageTypes.LIGHTNING_BOLT, bolt), 5f);
        DragonAbilityHandler.onLightningSelfDamage(own);
        check(h, own.isCanceled(), "caster immune to their bolt");
        LivingIncomingDamageEvent theirs = incoming(other, src(h, DamageTypes.LIGHTNING_BOLT, bolt), 5f);
        DragonAbilityHandler.onLightningSelfDamage(theirs);
        check(h, !theirs.isCanceled(), "others are hurt by it");
        h.succeed();
    }

    // ── Weather Control (slot 1) ────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weatherControlSummonsRainOrStormThenClearsIt")
    public static void weatherControlSummonsRainOrStormThenClearsIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerLevelData data = (ServerLevelData) level.getLevelData();
        boolean wasRaining = data.isRaining(), wasThundering = data.isThundering();
        CharTest.onEnd(h, () -> level.setWeatherParameters(0, 0, wasRaining, wasThundering));

        ServerPlayer none = dragon(h);
        DragonAbilityHandler.toggleWeather(none);
        near(h, 100f, QiData.getCurrent(none), "no node: nothing spent");

        ServerPlayer p = dragon(h);
        grant(p, "dr_weather_1");
        level.setWeatherParameters(6000, 0, false, false);
        h.runAfterDelay(30, () -> { // let the sky fully clear
            character().use(p, "dr_weather", AbilityAction.USE);
            near(h, 85f, QiData.getCurrent(p), "15 Qi");
            check(h, data.isRaining(), "rain summoned");
            check(h, !data.isThundering(), "tier 1 brings no storm");
            eq(h, 12000, data.getRainTime(), "for 10 minutes");

            ServerPlayer storm = dragon(h);
            grant(storm, "dr_weather_1", "dr_weather_2");
            level.setWeatherParameters(6000, 0, false, false);
            h.runAfterDelay(30, () -> {
                DragonAbilityHandler.toggleWeather(storm);
                check(h, data.isThundering(), "tier 2 summons a thunderstorm");
                h.runAfterDelay(40, () -> { // rain has set in
                    DragonAbilityHandler.toggleWeather(storm);
                    check(h, !data.isRaining() && !data.isThundering(), "second use clears the sky");
                    eq(h, 6000, data.getClearWeatherTime(), "clear for 5 minutes");
                    near(h, 70f, QiData.getCurrent(storm), "each use costs 15 Qi");
                    h.succeed();
                });
            });
        });
    }

    // ── Shift + V (slot 4): Dimension Travel ──────────────────────────

    @GameTest(template = PLATFORM, batch = "dimensionTravelNeedsFlightTierThree")
    public static void dimensionTravelNeedsFlightTierThree(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_flight_1", "dr_flight_2");
        character().use(p, "dr_dimension", AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "nothing happens below flight tier 3");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "dimensionSelectionWrapsAroundFiveDimensions")
    public static void dimensionSelectionWrapsAroundFiveDimensions(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        DragonAbilityHandler.scrollDimension(p, 1);
        eq(h, 0, p.getPersistentData().getInt("jttw_dragon_dim_select"), "no scrolling below flight tier 3");
        grant(p, "dr_flight_1", "dr_flight_2", "dr_flight_3");
        DragonAbilityHandler.scrollDimension(p, -1);
        eq(h, 4, p.getPersistentData().getInt("jttw_dragon_dim_select"), "back from Overworld wraps to Heaven");
        DragonAbilityHandler.scrollDimension(p, 1);
        DragonAbilityHandler.scrollDimension(p, 1);
        eq(h, 1, p.getPersistentData().getInt("jttw_dragon_dim_select"), "forward wraps to Nether");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "travellingToTheDimensionYouAreInRefundsTheQi")
    public static void travellingToTheDimensionYouAreInRefundsTheQi(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_flight_1", "dr_flight_2", "dr_flight_3");
        eq(h, Level.OVERWORLD, p.level().dimension(), "tests run in the overworld");
        QiData.setCurrent(p, 50f);
        character().use(p, "dr_dimension", AbilityAction.USE); // Shift + V; selected: Overworld
        near(h, 50f, QiData.getCurrent(p), "20 Qi refunded");
        eq(h, Level.OVERWORLD, p.level().dimension(), "stayed");
        QiData.setCurrent(p, 19f);
        DragonAbilityHandler.travelDimension(p);
        near(h, 19f, QiData.getCurrent(p), "too little Qi to try");
        h.succeed();
    }

    // ── Tidal Wave (slot 2) ─────────────────────────────────────────

    /** Golem straight ahead (south) and a pig 37 degrees off the look line. */
    private static void faceSouth(ServerPlayer p) {
        p.setYRot(0f);
        p.setXRot(0f);
    }

    @GameTest(template = PLATFORM, batch = "tidalWaveTierOneHitsAThirtyDegreeConeForTenQi")
    public static void tidalWaveTierOneHitsAThirtyDegreeConeForTenQi(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_flight_1", "dr_flight_2", "dr_flight_3", "dr_wave_1");
        faceSouth(p);
        IronGolem ahead = mob(h, EntityType.IRON_GOLEM, 2, 4);
        Pig aside = mob(h, EntityType.PIG, 1, 4);
        Husk behind = mob(h, EntityType.HUSK, 2, 0);
        character().use(p, "dr_wave", AbilityAction.USE); // N: the wave
        near(h, 90f, QiData.getCurrent(p), "10 Qi");
        check(h, ahead.getDeltaMovement().z >= 2.0, "golem swept south, motion " + ahead.getDeltaMovement());
        eq(h, 1, amp(ahead, MobEffects.MOVEMENT_SLOWDOWN), "Slowness II");
        eq(h, 60, duration(ahead, MobEffects.MOVEMENT_SLOWDOWN), "for 3 s");
        eq(h, -1, amp(aside, MobEffects.MOVEMENT_SLOWDOWN), "37 degrees off is outside a 30-degree cone");
        eq(h, -1, amp(behind, MobEffects.MOVEMENT_SLOWDOWN), "nothing behind");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tidalWaveTierTwoWidensToFortyFiveDegreesAndPutsOutFires")
    public static void tidalWaveTierTwoWidensToFortyFiveDegreesAndPutsOutFires(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_wave_1", "dr_wave_2");
        faceSouth(p);
        Pig aside = mob(h, EntityType.PIG, 1, 4);
        aside.setRemainingFireTicks(100);
        DragonAbilityHandler.castTidalWave(p);
        eq(h, 1, amp(aside, MobEffects.MOVEMENT_SLOWDOWN), "caught in a 45-degree cone");
        eq(h, 0, aside.getRemainingFireTicks(), "fire put out");
        h.succeed();
    }

    /** The drowning damage is 1 at level 1, grown by the caster's power (online, so the tick finds them). */
    @GameTest(template = PLATFORM, batch = "tidalWaveTierThreeTrapsTargetsInADrowningBubble")
    public static void tidalWaveTierThreeTrapsTargetsInADrowningBubble(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_wave_1", "dr_wave_2", "dr_wave_3");
        PlayerLevel.setLevel(p, 20);
        online(h, p);
        faceSouth(p);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        DragonAbilityHandler.castTidalWave(p);
        eq(h, 4, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "Slowness V: held in place");
        tick(p, DragonAbilityHandler::onPlayerTick, 80);
        List<Hit> drown = of(hits, DamageTypes.DROWN);
        check(h, !drown.isEmpty(), "bubble drowns its prisoner");
        near(h, LevelPower.of(20), drown.get(0).amount(), "1 x power drowning damage per second");
        h.succeed();
    }

    /** The bubble deals 1 damage per second for 3 seconds: three hits. */
    @GameTest(template = PLATFORM, batch = "tidalWaveBubbleDrownsForThreeSeconds")
    public static void tidalWaveBubbleDrownsForThreeSeconds(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_wave_1", "dr_wave_2", "dr_wave_3");
        faceSouth(p);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        DragonAbilityHandler.castTidalWave(p);
        tick(p, DragonAbilityHandler::onPlayerTick, 100);
        eq(h, 3, of(hits, DamageTypes.DROWN).size(), "drowning hits");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "tidalWaveHasAFifteenSecondCooldownAndNeedsTenQi")
    public static void tidalWaveHasAFifteenSecondCooldownAndNeedsTenQi(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_wave_1");
        faceSouth(p);
        QiData.setCurrent(p, 9f);
        DragonAbilityHandler.castTidalWave(p);
        near(h, 9f, QiData.getCurrent(p), "9 Qi is not enough");
        QiData.setCurrent(p, 100f);
        DragonAbilityHandler.castTidalWave(p);
        near(h, 90f, QiData.getCurrent(p), "cast");
        DragonAbilityHandler.castTidalWave(p);
        near(h, 90f, QiData.getCurrent(p), "on cooldown");
        h.runAfterDelay(295, () -> {
            DragonAbilityHandler.castTidalWave(p);
            near(h, 90f, QiData.getCurrent(p), "still cooling down before 300 ticks");
            h.runAfterDelay(6, () -> {
                DragonAbilityHandler.castTidalWave(p);
                near(h, 80f, QiData.getCurrent(p), "ready after 300 ticks");
                h.succeed();
            });
        });
    }

    // ── Dragon Scales (passive) ─────────────────────────────────────

    private static float hurt(ServerPlayer p, net.minecraft.world.damagesource.DamageSource src, float amount) {
        LivingIncomingDamageEvent ev = incoming(p, src, amount);
        DragonAbilityHandler.onDragonScalesHurt(ev);
        return ev.getAmount();
    }

    @GameTest(template = PLATFORM, batch = "scalesTierOneShaveTenPercentAndAQuarterMoreOffArrows")
    public static void scalesTierOneShaveTenPercentAndAQuarterMoreOffArrows(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        var ds = h.getLevel().damageSources();
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        near(h, 10f, hurt(p, ds.generic(), 10f), "no scales node");
        grant(p, "dr_scales_1");
        near(h, 9f, hurt(p, ds.generic(), 10f), "10% resistance");
        near(h, 6.75f, hurt(p, ds.arrow(arrow, husk), 10f), "arrows: x0.9 x0.75");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "scalesTierTwoResistTwentyPercentAndReflectMelee")
    public static void scalesTierTwoResistTwentyPercentAndReflectMelee(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_scales_1", "dr_scales_2");
        var ds = h.getLevel().damageSources();
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        List<Hit> reflected = watch(husk);
        near(h, 8f, hurt(p, ds.mobAttack(husk), 10f), "20% resistance");
        List<Hit> magic = of(reflected, DamageTypes.INDIRECT_MAGIC);
        eq(h, 1, magic.size(), "attacker takes reflected damage");
        near(h, 1.5, magic.get(0).amount(), "15% of the original 10");
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        hurt(p, ds.arrow(arrow, husk), 10f);
        eq(h, 1, of(reflected, DamageTypes.INDIRECT_MAGIC).size(), "arrows are not reflected");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "scalesTierThreeHardenBelowAQuarterHealth")
    public static void scalesTierThreeHardenBelowAQuarterHealth(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_scales_1", "dr_scales_2", "dr_scales_3");
        var ds = h.getLevel().damageSources();
        p.setHealth(p.getMaxHealth() * 0.2f);
        near(h, 5f, hurt(p, ds.generic(), 10f), "hardened: 50%");
        p.setHealth(p.getMaxHealth());
        near(h, 5f, hurt(p, ds.generic(), 10f), "hardening lasts 5 s even at full health");
        h.runAfterDelay(101, () -> {
            near(h, 8f, hurt(p, ds.generic(), 10f), "back to 20% after 100 ticks");
            p.setHealth(p.getMaxHealth() * 0.2f);
            near(h, 8f, hurt(p, ds.generic(), 10f), "60 s cooldown before hardening again");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "scalesDoNothingForOtherCharacters")
    public static void scalesDoNothingForOtherCharacters(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.PHOENIX);
        grant(p, "dr_scales_1", "dr_scales_2", "dr_scales_3");
        near(h, 10f, hurt(p, h.getLevel().damageSources().generic(), 10f), "Phoenix with dragon nodes");
        h.succeed();
    }

    // ── Tidal Force (passive) ───────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "tidalForceMultipliesMeleeKnockback")
    public static void tidalForceMultipliesMeleeKnockback(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        var ds = h.getLevel().damageSources();
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        DragonAbilityHandler.onTidalForceHurt(incoming(golem, ds.playerAttack(p), 4f));
        near(h, 0.0, golem.getDeltaMovement().z, "no node, no extra knockback");

        grant(p, "dr_force_1");
        DragonAbilityHandler.onTidalForceHurt(incoming(golem, ds.playerAttack(p), 4f));
        near(h, 1.2, golem.getDeltaMovement().z, "tier 1: 0.4 x 3");
        near(h, 0.4, golem.getDeltaMovement().y, "lifted");

        IronGolem golem2 = mob(h, EntityType.IRON_GOLEM, 2, 0);
        grant(p, "dr_force_2");
        DragonAbilityHandler.onTidalForceHurt(incoming(golem2, ds.playerAttack(p), 4f));
        near(h, -2.0, golem2.getDeltaMovement().z, "tier 2: 0.4 x 5, away from the player (north)");

        IronGolem golem3 = mob(h, EntityType.IRON_GOLEM, 4, 2);
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        DragonAbilityHandler.onTidalForceHurt(incoming(golem3, ds.arrow(arrow, p), 4f));
        near(h, 0.0, golem3.getDeltaMovement().x, "arrows are not melee");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "dragonKeysOnlyReactToUse")
    public static void dragonKeysOnlyReactToUse(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_lightning_1", "dr_weather_1", "dr_wave_1");
        p.setXRot(90f);
        PlayableCharacter c = character();
        for (int slot = 0; slot < 4; slot++) {
            c.activate(p, slot, AbilityAction.START);
            c.activate(p, slot, AbilityAction.STOP);
        }
        c.activate(p, 3, AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "START/STOP and the empty slot 3 do nothing");
        c.use(p, "dr_lightning", AbilityAction.USE);
        near(h, 92f, QiData.getCurrent(p), "slot 0 is lightning (8 Qi)");
        c.use(p, "dr_wave", AbilityAction.USE);
        near(h, 82f, QiData.getCurrent(p), "slot 2 is the wave (10 Qi)");
        h.succeed();
    }

    /** Tidal Wave II tears away the weak things in its cone (leaves, glass), never earth. */
    @GameTest(template = PLATFORM, batch = "tidalWaveTwoTearsAwayWeakBlocks", timeoutTicks = 60)
    public static void tidalWaveTwoTearsAwayWeakBlocks(GameTestHelper h) {
        ServerPlayer p = dragon(h);
        grant(p, "dr_wave_1", "dr_wave_2");
        var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 0.5));
        p.moveTo(at.x, at.y, at.z, 0f, 0f); // facing south, along the platform
        var leaves = new net.minecraft.core.BlockPos(2, 3, 3);
        var glass = new net.minecraft.core.BlockPos(2, 3, 4);
        var dirt = new net.minecraft.core.BlockPos(2, 2, 3);
        h.setBlock(leaves, net.minecraft.world.level.block.Blocks.OAK_LEAVES);
        h.setBlock(glass, net.minecraft.world.level.block.Blocks.GLASS);
        h.setBlock(dirt, net.minecraft.world.level.block.Blocks.DIRT);
        DragonAbilityHandler.castTidalWave(p);
        h.succeedWhen(() -> {
            h.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.OAK_LEAVES, leaves);
            h.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.GLASS, glass);
            h.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT, dirt);
        });
    }
}
