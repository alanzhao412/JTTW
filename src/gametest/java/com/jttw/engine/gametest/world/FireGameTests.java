package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.fire.EmberBoltEntity;
import com.jttw.engine.entity.fire.EmberImpEntity;
import com.jttw.engine.entity.fire.FireCartEntity;
import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.entity.fire.FurnaceBruteEntity;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.item.BananaLeafFanItem;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.BurningEarthBlock;
import com.jttw.engine.world.fire.FireCloudCave;
import com.jttw.engine.world.fire.FireMountain;
import com.jttw.engine.world.fire.SamadhiFire;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Flaming Mountains: the heat of the burning earth and the fan that puts it out, the mountains'
 * creatures and captains, Red Boy's Six Strong Generals, Red Boy himself (his Samadhi fire, which
 * water does not put out, his carts and his phases), what they all leave, the band's XP and karma,
 * and the Fire Cloud Cave's blueprint.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FireGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-3 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    /**
     * Removes, when the test ends, what this test brought and anything of the mountains' within its
     * own bounds (escorts, carts, embers), never wider.
     */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        AABB bounds = h.getBounds().inflate(1);
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, bounds, x -> x instanceof FireMonster || x instanceof RedBoyEntity
                    || x instanceof FireCartEntity || x instanceof EmberBoltEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends ZoneMonster> T fire(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    private static RedBoyEntity boy(GameTestHelper h, BlockPos at) {
        RedBoyEntity b = h.spawn(ModEntities.RED_BOY.get(), at);
        b.confine(1.8);
        sweepAround(h, b);
        return b;
    }

    /** A still iron golem, its health raised so no level-77 blow ends it: a target to measure blows on. */
    private static LivingEntity golem(GameTestHelper h, BlockPos at) {
        var g = h.spawn(EntityType.IRON_GOLEM, at);
        g.goalSelector.removeAllGoals(x -> true);
        g.targetSelector.removeAllGoals(x -> true);
        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
        g.setHealth(g.getMaxHealth());
        sweepAround(h, g);
        return g;
    }

    /** Lays the platform's floor in {@code state}. */
    private static void floor(GameTestHelper h, BlockState state) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), state);
    }

    private static int flames(GameTestHelper h) {
        int n = 0;
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) for (int y = 2; y <= 5; y++) {
            if (h.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.SAMADHI_FLAME.get())) n++;
        }
        return n;
    }

    // ── The heat ────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theBurningEarthScorchesWhatHasNoFireResistance(GameTestHelper h) {
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        float before = g.getHealth();
        check(h, FireMountain.scorch(g), "it scorches");
        close(h, FireMountain.HEAT_SHARE * g.getMaxHealth(), before - g.getHealth(), "a share of its health");
        check(h, g.isOnFire(), "and sets it alight");
        LivingEntity resisting = golem(h, new BlockPos(0, 2, 0));
        resisting.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        float r = resisting.getHealth();
        check(h, !FireMountain.scorch(resisting) && resisting.getHealth() == r && !resisting.isOnFire(), "Fire Resistance keeps it off");
        EmberImpEntity imp = fire(h, ModEntities.EMBER_IMP.get(), 67, false, new BlockPos(4, 2, 4));
        float i = imp.getHealth();
        check(h, !FireMountain.scorch(imp) && imp.getHealth() == i, "the mountains' own are fire-proof");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 80)
    public static void standingOnTheBurningEarthBurns(GameTestHelper h) {
        floor(h, ModBlocks.BURNING_EARTH.get().defaultBlockState());
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        float before = g.getHealth();
        h.succeedWhen(() -> check(h, g.getHealth() <= before - FireMountain.heatOf(g) * 0.99f && g.isOnFire(),
                "scorched where it stands: " + (before - g.getHealth())));
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void fannedEarthIsSafeToStandOn(GameTestHelper h) {
        floor(h, ModBlocks.BURNING_EARTH.get().defaultBlockState().setValue(BurningEarthBlock.LIT, false));
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        float before = g.getHealth();
        h.runAfterDelay(60, () -> {
            check(h, g.getHealth() == before && !g.isOnFire(), "no heat from fanned earth: " + (before - g.getHealth()));
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void theFanPutsOutTheBurningGroundInItsSweepForAWhile(GameTestHelper h) {
        floor(h, ModBlocks.BURNING_EARTH.get().defaultBlockState());
        h.setBlock(new BlockPos(2, 2, 4), Blocks.FIRE);
        h.setBlock(new BlockPos(3, 2, 4), ModBlocks.SAMADHI_FLAME.get());
        ServerPlayer p = player(h);
        BlockPos stand = h.absolutePos(new BlockPos(2, 2, 2));
        // facing +z of the platform, whichever way the test is turned
        Vec3 ahead = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 4))).subtract(Vec3.atCenterOf(stand));
        p.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, (float) Math.toDegrees(Math.atan2(-ahead.x, ahead.z)), 0f);
        LivingEntity g = golem(h, new BlockPos(2, 2, 3));
        BananaLeafFanItem.fan(p);
        BlockPos ahead1 = new BlockPos(2, 1, 4), behind = new BlockPos(2, 1, 0);
        check(h, !BurningEarthBlock.isLit(h.getBlockState(ahead1)), "the earth before it is out");
        check(h, !h.getBlockState(new BlockPos(2, 2, 4)).is(Blocks.FIRE), "and the fire on it");
        check(h, BurningEarthBlock.isLit(h.getBlockState(behind)), "the earth behind still burns");
        check(h, h.getBlockState(new BlockPos(3, 2, 4)).is(ModBlocks.SAMADHI_FLAME.get()), "the True Samadhi Fire is not the fan's to put out");
        check(h, g.getDeltaMovement().horizontalDistance() > 0.5, "and it still throws back what is in front: " + g.getDeltaMovement());
        BlockPos abs = h.absolutePos(ahead1);
        check(h, h.getLevel().getBlockTicks().hasScheduledTick(abs, ModBlocks.BURNING_EARTH.get()), "it will kindle again");
        // a minute on: it kindles
        h.getBlockState(ahead1).tick(h.getLevel(), abs, h.getLevel().random);
        check(h, BurningEarthBlock.isLit(h.getBlockState(ahead1)), "and it does");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void flamesCreepOverTheBurningEarthButNeverCrowdIt(GameTestHelper h) {
        floor(h, ModBlocks.BURNING_EARTH.get().defaultBlockState());
        int lit = 0;
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            if (BurningEarthBlock.kindle(h.getLevel(), h.absolutePos(new BlockPos(x, 1, z)))) lit++;
        }
        check(h, lit > 0 && lit < 25, "some kindle, not all: " + lit);
        int fires = 0;
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) if (h.getBlockState(new BlockPos(x, 2, z)).is(BlockTags.FIRE)) fires++;
        eq(h, lit, fires, "a fire for each");
        check(h, ModBlocks.BURNING_EARTH.get().defaultBlockState().isFireSource(h.getLevel(), h.absolutePos(new BlockPos(0, 1, 0)), net.minecraft.core.Direction.UP),
                "the fire on it never dies");
        check(h, !ModBlocks.BURNING_EARTH.get().defaultBlockState().setValue(BurningEarthBlock.LIT, false)
                .isFireSource(h.getLevel(), h.absolutePos(new BlockPos(0, 1, 0)), net.minecraft.core.Direction.UP), "fanned, it does");
        h.succeed();
    }

    // ── The True Samadhi Fire ───────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 80)
    public static void waterDoesNotPutOutTheSamadhiFireItFeedsIt(GameTestHelper h) {
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        SamadhiFire.ignite(g, 30, 0);
        float dry = g.getHealth();
        float hurt = SamadhiFire.burn(g, 0);
        close(h, SamadhiFire.burnOf(g, 0), hurt, "a beat of it, dry");
        close(h, dry - g.getHealth(), hurt, "taken");
        // into the water with it
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
        h.succeedWhen(() -> {
            check(h, g.isInWater(), "in the water");
            check(h, SamadhiFire.burning(g), "and still it burns");
            float wet = SamadhiFire.burn(g, 0);
            close(h, SamadhiFire.burnOf(g, 0) * SamadhiFire.WET, wet, "and harder: the water feeds it");
        });
    }

    @GameTest(template = PLATFORM)
    public static void onlyFireResistanceStandsAgainstItAndOnlyHalvesIt(GameTestHelper h) {
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        g.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400));
        SamadhiFire.ignite(g, 10, 0);
        check(h, SamadhiFire.burning(g), "it burns under Fire Resistance");
        close(h, SamadhiFire.burnOf(g, 0) * SamadhiFire.RESISTED, SamadhiFire.burn(g, 0), "halved");
        // the fire-proof burn too
        FurnaceBruteEntity brute = fire(h, ModEntities.FURNACE_BRUTE.get(), 67, false, new BlockPos(0, 2, 0));
        check(h, brute.fireImmune(), "a fire-proof creature");
        check(h, !g.fireImmune(), "(and the golem not)");
        var blaze = h.spawn(EntityType.BLAZE, new BlockPos(4, 2, 4));
        sweepAround(h, blaze);
        blaze.setNoAi(true);
        float b = blaze.getHealth();
        check(h, SamadhiFire.burn(blaze, 0) > 0 && blaze.getHealth() < b, "a blaze, fire-proof, burns");
        check(h, SamadhiFire.burn(brute, 0) == 0, "but Red Boy's own household it spares");
        // no milk cures it
        g.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        check(h, SamadhiFire.burning(g), "milk does nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void theSamadhiFlameOnTheGroundTakesNoWater(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, ModBlocks.SAMADHI_FLAME.get());
        BlockState flame = h.getBlockState(at);
        check(h, flame.is(ModBlocks.SAMADHI_FLAME.get()), "lit");
        check(h, !flame.is(BlockTags.FIRE), "not ordinary fire: no splash of water douses it");
        check(h, !flame.canBeReplaced(Fluids.WATER), "water takes no hold of it");
        check(h, flame.getDestroySpeed(h.getLevel(), h.absolutePos(at)) < 0, "no hand beats it out");
        check(h, h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(at), ModBlocks.SAMADHI_FLAME.get()), "it burns out by itself");
        h.setBlock(new BlockPos(1, 2, 2), Blocks.WATER);
        h.setBlock(new BlockPos(2, 3, 2), Blocks.WATER);
        h.runAfterDelay(40, () -> {
            check(h, h.getBlockState(at).is(ModBlocks.SAMADHI_FLAME.get()), "the water flowed round it and it burns on: " + h.getBlockState(at));
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void walkingIntoTheFlameSetsOneBurning(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.SAMADHI_FLAME.get());
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        h.succeedWhen(() -> check(h, SamadhiFire.burning(g), "burning"));
    }

    // ── Red Boy ─────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void redBoyIsTheHolyChildKing(GameTestHelper h) {
        RedBoyEntity boy = boy(h, new BlockPos(2, 2, 2));
        check(h, boy.getDisplayName().getString().contains("Lv " + RedBoyEntity.LEVEL), boy.getDisplayName().getString());
        close(h, RedBoyEntity.HEALTH, boy.getMaxHealth(), "seventy ordinary hits of a level-77 character");
        eq(h, EnemyTier.BOSS.name(), boy.getPersistentData().getString(ZoneMonster.KEY_TIER), "a Boss");
        eq(h, Zones.FIRE_MOUNTAIN.id(), boy.getPersistentData().getString(ZoneMonster.KEY_ZONE), "of the mountains");
        eq(h, BossEvent.BossBarColor.RED, boy.bossBar().getColor(), "a red bar");
        check(h, boy.fireImmune(), "fire is nothing to him");
        boy.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(boy.blockPosition()), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null);
        check(h, boy.getMainHandItem().is(ModItems.FIRE_TIP_SPEAR.get()), "his fire-tipped spear in his hand");
        check(h, boy.hasRestriction(), "he keeps to his lair");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void hisBreathCatchesWhatIsBeforeHimAndLeavesTheFireOnTheGround(GameTestHelper h) {
        RedBoyEntity boy = boy(h, new BlockPos(0, 2, 2));
        boy.setNoAi(true);
        LivingEntity before = golem(h, new BlockPos(3, 2, 2));
        LivingEntity aside = golem(h, new BlockPos(0, 2, 4));
        float hp = before.getHealth(), asideHp = aside.getHealth();
        List<LivingEntity> caught = boy.breathe(before);
        check(h, caught.contains(before) && !caught.contains(aside), "the one before him, not the one at his side: " + caught.size());
        close(h, boy.breathDamage(), hp - before.getHealth(), "its blow");
        eq(h, asideHp, aside.getHealth(), "untouched at his side");
        check(h, SamadhiFire.burning(before), "and it burns");
        check(h, flames(h) > 0, "flames left on the ground");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void fireResistanceHalvesHisBreath(GameTestHelper h) {
        RedBoyEntity boy = boy(h, new BlockPos(0, 2, 2));
        boy.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(3, 2, 2));
        g.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400));
        float hp = g.getHealth();
        boy.breathe(g);
        close(h, boy.breathDamage() * SamadhiFire.RESISTED, hp - g.getHealth(), "half its blow");
        check(h, SamadhiFire.burning(g), "and still it burns");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void atTwoThirdsHisFiveCartsWardHim(GameTestHelper h) {
        ServerPlayer p = player(h);
        RedBoyEntity boy = boy(h, new BlockPos(2, 2, 2));
        boy.setNoAi(true);
        boy.setHealth(boy.getMaxHealth() * 0.6f);
        h.succeedWhen(() -> {
            eq(h, 1, boy.phase(), "his second phase");
            List<FireCartEntity> carts = boy.carts();
            eq(h, 5, carts.size(), "his five carts, set out");
            for (FireCartEntity cart : carts) check(h, cart.wards(boy), "his");
            // a blow on him, halved while they stand
            float hp = boy.getHealth();
            boy.invulnerableTime = 0;
            boy.hurt(h.getLevel().damageSources().magic(), 100f);
            close(h, 100f * RedBoyEntity.CART_WARD, hp - boy.getHealth(), "halved");
            carts.forEach(c -> c.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE));
            check(h, boy.carts().isEmpty(), "broken, every one");
            hp = boy.getHealth();
            boy.invulnerableTime = 0;
            boy.hurt(h.getLevel().damageSources().magic(), 100f);
            close(h, 100f, hp - boy.getHealth(), "then whole");
        });
    }

    @GameTest(template = PLATFORM)
    public static void hisCartsBelchTheFireAndIgnoreFire(GameTestHelper h) {
        RedBoyEntity boy = boy(h, new BlockPos(2, 2, 2));
        boy.setNoAi(true);
        List<FireCartEntity> carts = boy.setOutCarts();
        eq(h, 5, carts.size(), "five");
        FireCartEntity cart = carts.get(0);
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        g.moveTo(cart.getX() + 0.8, cart.getY(), cart.getZ());
        check(h, cart.puff().contains(g), "what stands by it is set burning");
        check(h, SamadhiFire.burning(g), "burning");
        float hp = cart.getHealth();
        check(h, !cart.hurt(h.getLevel().damageSources().onFire(), 50f) && cart.getHealth() == hp, "fire does not break it");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void atAThirdTheFireComesFromHisNoseAndMouthBoth(GameTestHelper h) {
        RedBoyEntity boy = boy(h, new BlockPos(2, 2, 2));
        boy.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        boy.setHealth(boy.getMaxHealth() * 0.3f);
        h.succeedWhen(() -> {
            eq(h, 2, boy.phase(), "his last phase");
            eq(h, 1, boy.fierceness(), "his fire fiercer");
            check(h, SamadhiFire.burning(g), "his ring of fire caught what stood by him");
            var burning = g.getEffect(ModEffects.SAMADHI_FIRE);
            check(h, burning != null && burning.getAmplifier() == 1, "fiercely");
        });
    }

    // ── The creatures ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 80)
    public static void anEmberImpFlingsEmbersThatBurn(GameTestHelper h) {
        EmberImpEntity imp = fire(h, ModEntities.EMBER_IMP.get(), 67, false, new BlockPos(0, 2, 0));
        imp.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(4, 2, 4));
        close(h, EmberImpEntity.EMBER * MobScaling.typicalHealth(67), imp.emberDamage(), "an ember's hurt");
        List<EmberBoltEntity> embers = imp.fling(g);
        eq(h, 1, embers.size(), "one");
        float hp = g.getHealth();
        EmberBoltEntity ember = embers.get(0);
        check(h, ember.strike(g), "it strikes");
        close(h, imp.emberDamage(), hp - g.getHealth(), "for its hurt");
        check(h, g.isOnFire(), "and sets alight");
        check(h, !ember.strike(fire(h, ModEntities.FURNACE_BRUTE.get(), 67, false, new BlockPos(2, 2, 0))), "it spares its own");
        EmberImpEntity captain = fire(h, ModEntities.EMBER_IMP.get(), 74, true, new BlockPos(4, 2, 0));
        captain.setNoAi(true);
        eq(h, 3, captain.fling(g).size(), "a captain flings three");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void aBruteRaisesItsFistsAndSlamsTheGround(GameTestHelper h) {
        FurnaceBruteEntity brute = fire(h, ModEntities.FURNACE_BRUTE.get(), 67, false, new BlockPos(2, 2, 2));
        LivingEntity g = golem(h, new BlockPos(4, 2, 2));
        float hp = g.getHealth();
        List<LivingEntity> caught = brute.slam();
        check(h, caught.contains(g), "what is near is struck");
        close(h, brute.slamDamage(), hp - g.getHealth(), "for the slam's hurt");
        close(h, FurnaceBruteEntity.SLAM * MobScaling.typicalHealth(67), brute.slamDamage(), "22% of a level-67 character's health");
        check(h, g.isOnFire(), "and set alight");
        // fighting, it telegraphs the slam
        brute.setTarget(g);
        h.succeedWhen(() -> check(h, brute.isRaising(), "its fists go up first"));
    }

    @GameTest(template = PLATFORM)
    public static void aFireCaptainIsPurpleLargerAndBringsItsKind(GameTestHelper h) {
        EmberImpEntity regular = fire(h, ModEntities.EMBER_IMP.get(), 67, false, new BlockPos(1, 2, 1));
        FurnaceBruteEntity captain = fire(h, ModEntities.FURNACE_BRUTE.get(), 74, true, new BlockPos(2, 2, 2));
        eq(h, ZoneMonster.PURPLE, captain.getDisplayName().getStyle().getColor().getValue(), "a captain's name is purple");
        check(h, captain.getDisplayName().getString().contains("Captain") && captain.getDisplayName().getString().contains("Lv 74"), captain.getDisplayName().getString());
        check(h, regular.getDisplayName().getString().contains("Lv 67"), regular.getDisplayName().getString());
        close(h, FurnaceBruteEntity.HITS * MobScaling.typicalHit(74) * ZoneMonster.CAPTAIN_HEALTH, captain.getMaxHealth(), "a captain's health");
        eq(h, FireMonster.LEVEL + ZoneMonster.CAPTAIN_OVER, captain.captainLevel(), "captains about 74");
        captain.bringEscorts(h.getLevel());
        List<FurnaceBruteEntity> escorts = h.getLevel().getEntitiesOfClass(FurnaceBruteEntity.class, captain.getBoundingBox().inflate(6), e -> e != captain);
        eq(h, captain.escorts(), escorts.size(), "escorts");
        for (FurnaceBruteEntity e : escorts) check(h, !e.isCaptain() && Math.abs(e.mobLevel() - 67) <= 1, "regulars about 67");
        check(h, captain.fireImmune() && regular.fireImmune(), "fire-proof");
        h.succeed();
    }

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(template = PLATFORM)
    public static void theMountainsPayTheirBand(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 67);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.FIRE_MOUNTAIN_BIOME.location());
        check(h, band != null && band.minLevel() == 67 && band.maxLevel() == 77, "the mountains' band is 67-77");
        eq(h, 728L, band.xpForTier(EnemyTier.GRUNT), "a grunt pays 130 x 5.6");
        killedBy(p, fire(h, ModEntities.EMBER_IMP.get(), 67, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a regular");
        StrongGeneralEntity general = fire(h, ModEntities.STRONG_GENERAL.get(), 74, true, new BlockPos(3, 2, 3));
        killedBy(p, general);
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a general");
        long xp = PlayerLevel.getXp(p);
        killedBy(p, boy(h, new BlockPos(2, 2, 0)));
        eq(h, xp + band.xpForTier(EnemyTier.BOSS), PlayerLevel.getXp(p), "plus Red Boy, a boss");
        PlayerLevel.setLevel(p, 83);
        xp = PlayerLevel.getXp(p);
        killedBy(p, fire(h, ModEntities.FURNACE_BRUTE.get(), 67, false, new BlockPos(2, 2, 4)));
        eq(h, xp, PlayerLevel.getXp(p), "nothing past 77 and its five levels' grace");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void subduingRedBoyIsAGoodDeed(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, Morality.SLAY_RED_BOY, Morality.killDeed(p, boy(h, new BlockPos(2, 2, 2))), "he wanted to eat the Tang Monk");
        check(h, Morality.SLAY_RED_BOY > 0, "a good deed");
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, fire(h, ModEntities.STRONG_GENERAL.get(), 74, true, new BlockPos(0, 2, 0))), "a general, a captain");
        eq(h, Morality.SLAY_MONSTER, Morality.killDeed(p, fire(h, ModEntities.EMBER_IMP.get(), 67, false, new BlockPos(4, 2, 4))), "an imp, a monster");
        h.succeed();
    }

    // ── The Six Strong Generals ─────────────────────────────────────────

    private static StrongGeneralEntity general(GameTestHelper h, int variant, BlockPos at) {
        StrongGeneralEntity g = fire(h, ModEntities.STRONG_GENERAL.get(), 74, true, at);
        g.setVariant(variant);
        g.setNoAi(true);
        return g;
    }

    @GameTest(template = PLATFORM)
    public static void theSixAreEachTheirOwnAndAlwaysOfACaptainsStrength(GameTestHelper h) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int v = 0; v < 6; v++) {
            StrongGeneralEntity g = general(h, v, new BlockPos(v % 3 + 1, 2, v / 3 * 2 + 1));
            String name = g.getDisplayName().getString();
            check(h, name.contains("Lv 74") && !name.contains("Captain"), name);
            eq(h, ZoneMonster.PURPLE, g.getDisplayName().getStyle().getColor().getValue(), "purple");
            names.add(name);
            g.discard();
        }
        eq(h, 6, names.size(), "six names");
        StrongGeneralEntity egg = h.spawn(ModEntities.STRONG_GENERAL.get(), new BlockPos(2, 2, 2));
        sweepAround(h, egg);
        egg.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(egg.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
        check(h, egg.isCaptain() && Math.abs(egg.mobLevel() - 74) <= 1, "however he comes, a captain's strength: " + egg.mobLevel());
        eq(h, EnemyTier.GENERAL.name(), egg.getPersistentData().getString(ZoneMonster.KEY_TIER), "a General's pay");
        eq(h, 0, egg.escorts(), "and alone");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFogDivingMonsterComesOutBehindHisFoe(GameTestHelper h) {
        StrongGeneralEntity fog = general(h, StrongGeneralEntity.FOG, new BlockPos(0, 2, 0));
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        g.setYRot(0);
        g.setYHeadRot(0);
        check(h, fog.diveBehind(g), "he dives");
        Vec3 look = g.getLookAngle();
        Vec3 to = fog.position().subtract(g.position());
        check(h, to.x * look.x + to.z * look.z < 0, "behind it: " + to + " look " + look);
        check(h, fog.hasEffect(MobEffects.INVISIBILITY), "unseen a moment");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFlameHeadGeneralBurstsIntoFlame(GameTestHelper h) {
        StrongGeneralEntity flame = general(h, StrongGeneralEntity.FLAME_HEAD, new BlockPos(2, 2, 2));
        LivingEntity g = golem(h, new BlockPos(4, 2, 2));
        float hp = g.getHealth();
        eq(h, 1, flame.flameBurst(), "caught the golem");
        close(h, flame.trickDamage(), hp - g.getHealth(), "his trick's hurt");
        check(h, g.isOnFire(), "alight");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theCloudSoaringSpriteBlindsWhereHeLands(GameTestHelper h) {
        StrongGeneralEntity cloud = general(h, StrongGeneralEntity.CLOUD, new BlockPos(2, 2, 2));
        LivingEntity g = golem(h, new BlockPos(4, 2, 2));
        eq(h, 1, cloud.cloudBurst(), "caught");
        check(h, g.hasEffect(MobEffects.BLINDNESS), "blinded");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theWindsChargeAndHurlAndTheEyeSparkOfficerShootsSparks(GameTestHelper h) {
        StrongGeneralEntity swift = general(h, StrongGeneralEntity.SWIFT_WIND, new BlockPos(2, 2, 2));
        LivingEntity g = golem(h, new BlockPos(4, 2, 2));
        g.setDeltaMovement(Vec3.ZERO);
        eq(h, 1, swift.gust(), "the Swift-Rushing Wind's gust caught it");
        Vec3 away = g.position().subtract(swift.position());
        check(h, g.getDeltaMovement().x * away.x + g.getDeltaMovement().z * away.z > 0.5, "hurled away: " + g.getDeltaMovement());
        check(h, swift.getAttributeValue(Attributes.MOVEMENT_SPEED) > StrongGeneralEntity.SPEED, "the fastest of them");
        StrongGeneralEntity mad = general(h, StrongGeneralEntity.MAD_WIND, new BlockPos(0, 2, 0));
        check(h, mad.charge(g), "the Mad-Hurrying Wind charges");
        Vec3 to = g.position().subtract(mad.position());
        check(h, mad.getDeltaMovement().x * to.x + mad.getDeltaMovement().z * to.z > 0, "at it");
        StrongGeneralEntity eye = general(h, StrongGeneralEntity.EYE_SPARK, new BlockPos(0, 2, 4));
        eq(h, 2, eye.sparks(g).size(), "two sparks from his eyes");
        h.succeed();
    }

    // ── What they leave ─────────────────────────────────────────────────

    private static List<ItemStack> loot(GameTestHelper h, Mob mob, ServerPlayer p) {
        var source = h.getLevel().damageSources().playerAttack(p);
        LootParams params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, p)
                .create(LootContextParamSets.ENTITY);
        return h.getLevel().getServer().reloadableRegistries().getLootTable(mob.getLootTable()).getRandomItems(params);
    }

    private static int count(List<ItemStack> items, Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = PLATFORM)
    public static void redBoyLeavesTheTrueSamadhiFire(GameTestHelper h) {
        ServerPlayer p = player(h);
        RedBoyEntity boy = boy(h, new BlockPos(2, 2, 2));
        int spears = 0, rolls = 40;
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> items = loot(h, boy, p);
            int fire = count(items, ModItems.TRUE_SAMADHI_FIRE.get());
            check(h, fire >= 2 && fire <= 4, "two or three True Samadhi Fire (four with Looting): " + items);
            check(h, count(items, ModItems.EMBER_CORE.get()) >= 3, "embers");
            spears += count(items, ModItems.FIRE_TIP_SPEAR.get());
        }
        // a quarter of the time: in forty kills, between one and twenty-four (a miss at either end is one in a hundred thousand)
        check(h, spears >= 1 && spears <= 24, "his spear, now and then: " + spears + " in " + rolls);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theMountainsCreaturesLeaveItsMaterials(GameTestHelper h) {
        ServerPlayer p = player(h);
        List<ItemStack> brute = loot(h, fire(h, ModEntities.FURNACE_BRUTE.get(), 67, false, new BlockPos(1, 2, 1)), p);
        check(h, count(brute, ModItems.FURNACE_BRICK.get()) >= 1, "a furnace brick: " + brute);
        List<ItemStack> general = loot(h, fire(h, ModEntities.STRONG_GENERAL.get(), 74, true, new BlockPos(3, 2, 3)), p);
        check(h, count(general, ModItems.EMBER_CORE.get()) >= 1 && count(general, ModItems.FURNACE_BRICK.get()) >= 1, "a general's: " + general);
        int cores = 0;
        for (int i = 0; i < 20; i++) cores += count(loot(h, fire(h, ModEntities.EMBER_IMP.get(), 67, false, new BlockPos(2, 2, 2)), p), ModItems.EMBER_CORE.get());
        check(h, cores >= 1, "imps leave ember cores: " + cores + " in 20");
        var recipe = h.getLevel().getRecipeManager().byKey(JTTWEngine.id("true_samadhi_fire_from_the_mountains"));
        check(h, recipe.isPresent() && recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(ModItems.TRUE_SAMADHI_FIRE.get()),
                "and the furnace's fuel is forged from them");
        h.succeed();
    }

    // ── The cave ────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theFireCloudCavesBlueprint(GameTestHelper h) {
        check(h, FireCloudCave.at(0, 1, 0).isAir() && FireCloudCave.at(0, 2, 0).isAir(), "room for him on his dais");
        check(h, FireCloudCave.at(0, 0, 0).is(Blocks.GILDED_BLACKSTONE), "a gilded dais");
        check(h, FireCloudCave.at(0, 1, FireCloudCave.THRONE_Z).is(Blocks.POLISHED_BLACKSTONE_STAIRS), "his throne behind him");
        int lava = 0, lamps = 0;
        for (int x = FireCloudCave.MIN_X; x <= FireCloudCave.MAX_X; x++) for (int z = FireCloudCave.MIN_Z; z <= FireCloudCave.MAX_Z; z++) {
            for (int y = FireCloudCave.BOTTOM; y <= FireCloudCave.TOP; y++) {
                BlockState s = FireCloudCave.at(x, y, z);
                if (s == null) continue;
                if (s.is(Blocks.LAVA)) lava++;
                if (s.is(Blocks.SHROOMLIGHT)) lamps++;
            }
        }
        eq(h, 16, lava, "four pools of lava, two by two");
        check(h, lamps >= 12, "lit: " + lamps);
        // the way in: from the gate's arch along the passage to the hall, open all the way
        for (int z = FireCloudCave.GATE_Z + 1; z >= 0; z--) {
            check(h, FireCloudCave.at(0, 1, z) != null && !FireCloudCave.at(0, 1, z).isSolid() && FireCloudCave.at(0, 3, z).isAir(), "open at z " + z);
        }
        check(h, FireCloudCave.at(0, 7, FireCloudCave.GATE_Z + 1).is(Blocks.GILDED_BLACKSTONE), "the gate's plaque");
        h.succeed();
    }
}
