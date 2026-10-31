package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.spider.CockroachSonEntity;
import com.jttw.engine.entity.spider.HundredEyedDemonEntity;
import com.jttw.engine.entity.spider.MantisSonEntity;
import com.jttw.engine.entity.spider.SilkKin;
import com.jttw.engine.entity.spider.SilkMonster;
import com.jttw.engine.entity.spider.SilkStrandEntity;
import com.jttw.engine.entity.spider.SpiderDemonEntity;
import com.jttw.engine.entity.spider.SpiderSpiritEntity;
import com.jttw.engine.entity.spider.StingerSonEntity;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Stun;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.SilkRidgeFeature;
import com.jttw.engine.world.morality.Morality;
import com.jttw.engine.world.spider.BindingWebBlock;
import com.jttw.engine.world.spider.SilkRidgeWorks;
import com.jttw.engine.world.spider.SpiderCave;
import com.jttw.engine.world.spider.SpiderCavePlan;
import com.jttw.engine.world.spider.Venom;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
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
 * Silk-Web Ridge and the Spider Cave: the zone (its band, one region in its ring, its pay), its
 * creatures and captains (the spider demons' strands, the stinging sons' darts, the Mantis Son's
 * sweep, the Cockroach Son's charge, the venom), the binding webs, the Seven Spider Spirits (their
 * silk, their webs, their sons, their seals and the record of their fall), the Hundred-Eyed Demon
 * Lord (his level, the golden light, the poisoned tea, the sweep, his robe, his shield under the
 * canopy), the Purifying Spring, the cave's keepers put at their posts, what they all leave, karma.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpiderCaveGameTests {

    /** Within 3%: a golem's armour and resistances take a little off a blow. */
    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 0.03 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    /** Removes, when the test ends, what this test brought and anything of the ridge's within its own bounds. */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        AABB bounds = h.getBounds().inflate(24);
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, bounds, x -> x instanceof SilkKin || x instanceof SilkStrandEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends ZoneMonster> T silk(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        m.setNoAi(true);
        sweepAround(h, m);
        return m;
    }

    private static HundredEyedDemonEntity lord(GameTestHelper h, BlockPos at) {
        HundredEyedDemonEntity b = h.spawn(SpiderCaveRegistry.HUNDRED_EYED_DEMON.get(), at);
        b.confine(2.5);
        b.setNoAi(true);
        sweepAround(h, b);
        return b;
    }

    /** A still iron golem, its health raised so no blow ends it. */
    private static LivingEntity golem(GameTestHelper h, BlockPos at) {
        var g = h.spawn(EntityType.IRON_GOLEM, at);
        g.goalSelector.removeAllGoals(x -> true);
        g.targetSelector.removeAllGoals(x -> true);
        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000_000);
        g.setHealth(g.getMaxHealth());
        sweepAround(h, g);
        return g;
    }

    private static void face(LivingEntity e, Vec3 to) {
        Vec3 d = to.subtract(e.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        e.setYRot(yaw);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
    }

    // ── The zone ────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theRidgeIsOneRegionInWuzhuangsRingAndPaysItsBand(GameTestHelper h) {
        Zones.Zone z = Zones.SPIDER_CAVE;
        eq(h, Zones.Vertical.WHOLE_COLUMN, z.vertical(), "the whole column: on the ground");
        check(h, Zones.ALL.indexOf(z) == Zones.ALL.indexOf(Zones.MOUNT_WUZHUANG) + 1, "after Mount Wuzhuang, its band's other zone");
        long seed = h.getLevel().getSeed();
        int[][] cs = z.centres(seed);
        eq(h, 1, cs.length, "one region a world");
        double d = Math.hypot(cs[0][0], cs[0][1]);
        check(h, d >= 6000 - 1 && d <= 7500 + 1, "6-7.5 km out: " + d);
        for (Zones.Zone other : Zones.MAP.zones()) {
            if (other == z || !other.vertical().overlaps(z.vertical())) continue;
            for (int[] o : other.centres(seed)) {
                check(h, Math.hypot(o[0] - cs[0][0], o[1] - cs[0][1]) >= z.layout().margin() + other.layout().margin(), "clear of " + other.id());
            }
        }
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.SPIDER_CAVE_BIOME.location());
        check(h, band != null && band.minLevel() == 78 && band.maxLevel() == 88, "its band is 78 to 88");
        eq(h, 972L, band.xpForTier(EnemyTier.GRUNT), "a grunt pays 145 x 6.7, as Mount Wuzhuang's");
        check(h, EarthGod.needle(z, cs[0]).getZ() == cs[0][1] + SilkRidgeWorks.COTTAGE_Z, "the luopan points to the cottage over the cave");
        h.succeed();
    }

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(template = PLATFORM)
    public static void theRidgePaysItsBand(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 80);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.SPIDER_CAVE_BIOME.location());
        killedBy(p, silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 78, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a regular");
        killedBy(p, silk(h, SpiderCaveRegistry.SPIDER_SPIRIT.get(), 84, true, new BlockPos(3, 2, 3)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a sister, a general");
        long xp = PlayerLevel.getXp(p);
        killedBy(p, lord(h, new BlockPos(2, 2, 0)));
        eq(h, xp + band.xpForTier(EnemyTier.BOSS), PlayerLevel.getXp(p), "plus the Demon Lord, a boss");
        h.succeed();
    }

    // ── The creatures ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theRidgesCreaturesAreOfItsBandAndItsCaptainsLeaveSilk(GameTestHelper h) {
        List<EntityType<? extends SilkMonster>> kinds = List.of(SpiderCaveRegistry.SPIDER_DEMON.get(), SpiderCaveRegistry.STINGER_SON.get(),
                SpiderCaveRegistry.MANTIS_SON.get(), SpiderCaveRegistry.COCKROACH_SON.get());
        for (EntityType<? extends SilkMonster> kind : kinds) {
            SilkMonster m = silk(h, kind, 78, false, new BlockPos(2, 2, 2));
            eq(h, 78, m.regularLevel(), "regulars about 78");
            eq(h, 85, m.captainLevel(), "captains about 85");
            check(h, m.rollCaptainLevel(m.getRandom()) <= 88, "never past the band");
            eq(h, Zones.SPIDER_CAVE.id(), m.getPersistentData().getString(ZoneMonster.KEY_ZONE), "the ridge's");
            m.become(85, true);
            check(h, m.getDisplayName().getString().contains("Captain") && m.getDisplayName().getString().endsWith("· Lv 85"), m.getDisplayName().getString());
            m.discard();
        }
        // the stinging sons go by their kind's name
        StingerSonEntity s = silk(h, SpiderCaveRegistry.STINGER_SON.get(), 78, false, new BlockPos(1, 3, 1));
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int k = 0; k < 3; k++) {
            s.setKind(k);
            names.add(s.getDisplayName().getString());
        }
        eq(h, 3, names.size(), "the Honeybee, Hornet and Gadfly Sons: " + names);
        check(h, s.isNoGravity(), "it flies");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aSpiderDemonsStrandPinsWhatItStrikes(GameTestHelper h) {
        SpiderDemonEntity spider = silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 78, false, new BlockPos(0, 2, 0));
        LivingEntity g = golem(h, new BlockPos(4, 2, 4));
        List<SilkStrandEntity> spat = spider.spit(g);
        eq(h, 1, spat.size(), "a strand");
        float hp = g.getHealth();
        check(h, spat.get(0).strike(g), "it strikes");
        close(h, spider.strandDamage(), hp - g.getHealth(), "the strand's hurt");
        check(h, g.hasEffect(ModEffects.PINNED), "and pins");
        close(h, SpiderDemonEntity.STRAND * MobScaling.typicalHealth(78), spider.strandDamage(), "5% of a level-78 character's health");
        check(h, !spat.get(0).strike(silk(h, SpiderCaveRegistry.MANTIS_SON.get(), 78, false, new BlockPos(2, 2, 2))), "the ridge's own are spared");
        SpiderDemonEntity captain = silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 85, true, new BlockPos(0, 2, 4));
        eq(h, 2, captain.spit(g).size(), "a captain spits two");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theVenomEatsAShareOfHealthAndTheRidgesOwnAreProofAgainstIt(GameTestHelper h) {
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        Venom.envenom(g, 5, 0, null);
        check(h, g.hasEffect(SpiderCaveRegistry.VENOM), "envenomed");
        float hp = g.getHealth();
        g.getEffect(SpiderCaveRegistry.VENOM).getEffect().value().applyEffectTick(g, 0);
        close(h, g.getMaxHealth() * Venom.SHARE, hp - g.getHealth(), "1% of its health a second");
        SpiderDemonEntity spider = silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 78, false, new BlockPos(0, 2, 0));
        Venom.envenom(spider, 5, 0, null);
        check(h, !spider.hasEffect(SpiderCaveRegistry.VENOM), "the ridge's own are proof against it");
        // the Purifying Spring washes it away, and the silk's hold
        Stun.pin(g, 100, null);
        SpiderCave.bathe(g);
        check(h, !g.hasEffect(SpiderCaveRegistry.VENOM) && !g.hasEffect(ModEffects.PINNED) && g.hasEffect(MobEffects.REGENERATION), "the spring cleanses");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aBindingWebPinsWhoWalksIntoItThenLetsThemTearFree(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 2), SpiderCaveRegistry.BINDING_WEB.get());
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        h.runAfterDelay(2, () -> {
            check(h, g.hasEffect(ModEffects.PINNED), "held");
            check(h, !BindingWebBlock.bind(g), "not bound again while held");
            g.removeEffect(ModEffects.PINNED);
            check(h, !BindingWebBlock.bind(g), "a moment's grace to tear free");
            SpiderDemonEntity spider = silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 78, false, new BlockPos(0, 2, 0));
            check(h, !BindingWebBlock.bind(spider), "the ridge's own run over it");
        });
        // a web cast fresh in a fight falls apart by itself
        h.setBlock(new BlockPos(0, 2, 4), SpiderCaveRegistry.BINDING_WEB.get().defaultBlockState().setValue(BindingWebBlock.FRESH, true));
        h.runAfterDelay(BindingWebBlock.FRESH_FOR + 5, () -> {
            check(h, h.getBlockState(new BlockPos(0, 2, 4)).isAir(), "the fresh web is gone");
            check(h, h.getBlockState(new BlockPos(2, 2, 2)).is(SpiderCaveRegistry.BINDING_WEB.get()), "the strung one stays");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void theMantisSweepsBeforeItAndTheCockroachRams(GameTestHelper h) {
        MantisSonEntity mantis = silk(h, SpiderCaveRegistry.MANTIS_SON.get(), 78, false, new BlockPos(2, 2, 0));
        mantis.setYRot(0); // facing +z
        mantis.setYBodyRot(0);
        LivingEntity before = golem(h, new BlockPos(2, 2, 2));
        LivingEntity behind = golem(h, new BlockPos(2, 2, -2 + 2));
        behind.teleportTo(mantis.getX(), mantis.getY(), mantis.getZ() - 2);
        float hp = before.getHealth(), hb = behind.getHealth();
        List<LivingEntity> cut = mantis.sweep();
        check(h, cut.contains(before) && !cut.contains(behind), "the one before it is cut, not the one behind: " + cut);
        close(h, mantis.sweepDamage(), hp - before.getHealth(), "the sweep's hurt");
        check(h, behind.getHealth() == hb, "behind it, unhurt");
        CockroachSonEntity roach = silk(h, SpiderCaveRegistry.COCKROACH_SON.get(), 78, false, new BlockPos(0, 2, 4));
        LivingEntity g = golem(h, new BlockPos(4, 2, 4));
        roach.beginCharge(g);
        eq(h, CockroachSonEntity.SCRAPING, roach.chargeState(), "it scrapes the ground first");
        g.setDeltaMovement(Vec3.ZERO);
        float gh = g.getHealth();
        check(h, roach.ram(g), "it rams");
        close(h, roach.ramDamage(), gh - g.getHealth(), "the ram's hurt");
        check(h, g.getDeltaMovement().x > 0.3, "thrown back along its charge: " + g.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aStingingSonsDartStingsAndEnvenoms(GameTestHelper h) {
        StingerSonEntity s = silk(h, SpiderCaveRegistry.STINGER_SON.get(), 78, false, new BlockPos(0, 3, 0));
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        float hp = g.getHealth();
        check(h, s.dartSting(g), "the dart strikes");
        close(h, s.hitOf(StingerSonEntity.DART), hp - g.getHealth(), "9% of a character's health");
        check(h, g.hasEffect(SpiderCaveRegistry.VENOM), "and envenoms");
        h.succeed();
    }

    // ── The Seven Spider Spirits ────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theSevenAreEachTheirOwnAndAlwaysMinibosses(GameTestHelper h) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int v = 0; v < SpiderSpiritEntity.SISTERS; v++) {
            SpiderSpiritEntity s = silk(h, SpiderCaveRegistry.SPIDER_SPIRIT.get(), 84, true, new BlockPos(v % 4 + 1, 2, v / 4 * 2 + 1));
            s.setSister(v);
            String name = s.getDisplayName().getString();
            check(h, name.contains("Spider Spirit") && name.endsWith("· Lv 84") && !name.contains("Captain"), name);
            eq(h, ZoneMonster.PURPLE, s.getDisplayName().getStyle().getColor().getValue(), "purple");
            names.add(name);
            s.discard();
        }
        eq(h, 7, names.size(), "seven names");
        SpiderSpiritEntity egg = h.spawn(SpiderCaveRegistry.SPIDER_SPIRIT.get(), new BlockPos(2, 2, 2));
        sweepAround(h, egg);
        egg.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(egg.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
        check(h, egg.isCaptain() && egg.isMiniboss() && Math.abs(egg.mobLevel() - 85) <= 1, "however she comes, a miniboss: " + egg.mobLevel());
        double blow = egg.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(egg.mobLevel());
        close(h, ZoneMonster.MINIBOSS_HIT, blow, "a miniboss's blow");
        eq(h, 0, egg.escorts(), "and alone");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aSistersSilkPinsAllBeforeHer(GameTestHelper h) {
        SpiderSpiritEntity s = silk(h, SpiderCaveRegistry.SPIDER_SPIRIT.get(), 84, true, new BlockPos(2, 2, 0));
        s.setYRot(0);
        s.setYBodyRot(0);
        LivingEntity before = golem(h, new BlockPos(2, 2, 4));
        LivingEntity aside = golem(h, new BlockPos(0, 2, 0));
        aside.teleportTo(s.getX() - 3, s.getY(), s.getZ() - 1);
        float hp = before.getHealth();
        List<LivingEntity> caught = s.pourSilk();
        check(h, caught.contains(before) && !caught.contains(aside), "the one before her caught, not the one beside: " + caught);
        check(h, before.hasEffect(ModEffects.PINNED), "and pinned");
        close(h, s.silkDamage(), hp - before.getHealth(), "her silk's hurt");
        check(h, s.silkDamage() <= ZoneMonster.MINIBOSS_HIT * MobScaling.typicalHealth(84) + 1, "no harder than a miniboss's hardest");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aSisterWeavesWebsUnderHerFoesAndCallsHerSonsOnce(GameTestHelper h) {
        SpiderSpiritEntity s = silk(h, SpiderCaveRegistry.SPIDER_SPIRIT.get(), 84, true, new BlockPos(0, 2, 0));
        LivingEntity p = golem(h, new BlockPos(3, 2, 3));
        s.setTarget(p);
        List<BlockPos> marked = s.markWebs();
        check(h, !marked.isEmpty() && marked.contains(p.blockPosition()), "she marks where her foe stands: " + marked);
        p.discard();
        check(h, s.springWebs() >= 1, "webs spring up there");
        check(h, h.getLevel().getBlockState(p.blockPosition()).is(SpiderCaveRegistry.BINDING_WEB.get())
                && h.getLevel().getBlockState(p.blockPosition()).getValue(BindingWebBlock.FRESH), "a fresh binding web");
        h.getLevel().setBlockAndUpdate(p.blockPosition(), Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.blockPosition().above(), Blocks.AIR.defaultBlockState());
        eq(h, 2, s.callSons().size(), "two sons come");
        eq(h, 0, s.callSons().size(), "once only");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aSistersDeathLoosensHerSealAndTheLastBringsTheCanopyDown(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // a region laid so that the first bower's seal is over this platform
        int[] first = SpiderCavePlan.sealBlocks(SpiderCavePlan.sealOf(0)).get(0);
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        SpiderCave.Region region = new SpiderCave.Region(at.getX() - first[0], at.getZ() - first[2], at.getY() - first[1]);
        SpiderCave.Record.of(level).forget(region);
        onEnd(h, () -> SpiderCave.Record.of(level).forget(region));
        List<int[]> seal = SpiderCavePlan.sealBlocks(SpiderCavePlan.sealOf(0));
        for (int[] b : seal) level.setBlockAndUpdate(region.at(b[0], b[1], b[2]), SpiderCaveRegistry.SILK_SEAL.get().defaultBlockState());
        onEnd(h, () -> {
            for (int[] b : seal) level.setBlockAndUpdate(region.at(b[0], b[1], b[2]), Blocks.AIR.defaultBlockState());
        });
        check(h, SpiderCaveRegistry.SILK_SEAL.get().defaultBlockState().getDestroySpeed(level, at) < 0, "no hand breaks the seal");
        // the Demon Lord of this region's temple: shielded while any sister lives
        HundredEyedDemonEntity lord = lord(h, new BlockPos(4, 2, 4));
        lord.keepTemple(region);
        float hp = lord.getHealth();
        check(h, lord.shielded() && !lord.hurt(level.damageSources().playerAttack(player(h)), 100f) && lord.getHealth() == hp, "the canopy shields him");
        // the youngest falls in the first bower
        SpiderSpiritEntity youngest = silk(h, SpiderCaveRegistry.SPIDER_SPIRIT.get(), 82, true, new BlockPos(0, 2, 0));
        youngest.keepBower(new SpiderCave.Bower(region, 0));
        youngest.setSister(SpiderCavePlan.sisterOf(0));
        youngest.setNoAi(false);
        youngest.die(level.damageSources().playerAttack(player(h)));
        check(h, !level.getBlockState(at).is(SpiderCaveRegistry.SILK_SEAL.get()), "her silk loosened");
        eq(h, 1, SpiderCave.Record.of(level).fallen(region), "her fall remembered");
        check(h, lord.shielded(), "six still live: he is shielded yet");
        // the other six
        for (int b = 1; b < SpiderCavePlan.BOWER_COUNT; b++) SpiderCave.Record.of(level).fall(region, b);
        check(h, SpiderCave.sistersAllFallen(level, region), "all seven fallen");
        check(h, !lord.shielded() && lord.hurt(level.damageSources().playerAttack(player(h)), 100f), "and the Demon Lord can be struck");
        h.succeed();
    }

    // ── The Hundred-Eyed Demon Lord ─────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theDemonLordTowersOverHisBandAndIsAGroupsFight(GameTestHelper h) {
        HundredEyedDemonEntity b = lord(h, new BlockPos(2, 2, 2));
        eq(h, 103, b.mobLevel(), "his band's top and fifteen");
        check(h, b.getDisplayName().getString().endsWith("· Lv 103"), b.getDisplayName().getString());
        double hits = b.getMaxHealth() / MobScaling.typicalHit(103);
        check(h, hits >= ZoneBoss.LEAST_HITS && hits <= 350, "a group's fight at his level: " + hits);
        double blow = b.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(103);
        check(h, blow >= ZoneBoss.LEAST_BLOW && blow <= ZoneBoss.MOST_BLOW, "a boss's blow: " + blow);
        for (double special : new double[]{HundredEyedDemonEntity.LIGHT, HundredEyedDemonEntity.TEA, HundredEyedDemonEntity.SWEEP}) {
            check(h, special <= HundredEyedDemonEntity.SWORD, "no art harder than his blow: " + special);
        }
        eq(h, BossEvent.BossBarColor.YELLOW, b.bossBar().getColor(), "a yellow bar");
        check(h, !b.shielded(), "not his temple's own: no shield");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theGoldenLightDazzlesThoseWhoFaceHimAndSpareThoseTurnedAway(GameTestHelper h) {
        HundredEyedDemonEntity b = lord(h, new BlockPos(2, 2, 2));
        b.confine(HundredEyedDemonEntity.LIGHT_RANGE);
        LivingEntity facing = golem(h, new BlockPos(4, 2, 2)), away = golem(h, new BlockPos(0, 2, 2));
        face(facing, b.position().add(0, 2, 0));
        face(away, away.position().add(-5, 1.6, 0));
        float fh = facing.getHealth(), ah = away.getHealth();
        List<LivingEntity> dazzled = b.goldenLight();
        check(h, dazzled.contains(facing) && !dazzled.contains(away), "the one facing him dazzled, not the one turned away: " + dazzled);
        check(h, facing.hasEffect(MobEffects.BLINDNESS) && facing.hasEffect(ModEffects.PINNED), "blinded and held");
        check(h, !away.hasEffect(MobEffects.BLINDNESS), "the one turned away sees still");
        check(h, fh - facing.getHealth() > ah - away.getHealth(), "and took the worse of it");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thePoisonedTeaFallsWhereItWasMarkedAndTheSweepHurlsAllAround(GameTestHelper h) {
        HundredEyedDemonEntity b = lord(h, new BlockPos(2, 2, 2));
        b.confine(8);
        LivingEntity g = golem(h, new BlockPos(4, 2, 4));
        List<Vec3> spots = b.markTea();
        check(h, !spots.isEmpty(), "the tea marked where its foe stands: " + spots);
        float hp = g.getHealth();
        List<LivingEntity> caught = b.burstTea();
        check(h, caught.contains(g), "it bursts there");
        close(h, HundredEyedDemonEntity.TEA * MobScaling.typicalHealth(103), hp - g.getHealth(), "the tea's hurt");
        check(h, g.hasEffect(SpiderCaveRegistry.VENOM), "and its venom");
        LivingEntity near = golem(h, new BlockPos(0, 2, 2));
        near.setDeltaMovement(Vec3.ZERO);
        List<LivingEntity> struck = b.sweep();
        check(h, struck.contains(near) && struck.contains(g), "the sweep strikes all round him");
        check(h, near.getDeltaMovement().horizontalDistance() > 0.5, "and hurls them off");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void atTwoThirdsHeThrowsOffHisRobeAndCallsHisBrood(GameTestHelper h) {
        HundredEyedDemonEntity b = lord(h, new BlockPos(2, 2, 2));
        check(h, !b.unrobed(), "robed at first");
        b.setHealth(b.getMaxHealth() * 0.6f);
        b.tick();
        eq(h, 1, b.phase(), "the first phase passed");
        check(h, b.unrobed(), "his robe thrown off");
        int brood = h.getLevel().getEntitiesOfClass(StingerSonEntity.class, b.getBoundingBox().inflate(8)).size();
        eq(h, 3, brood, "his stinging brood comes");
        h.succeed();
    }

    // ── The keepers at their posts ──────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theCaveIsKeptWhenTheLandIsMade(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        SpiderCavePlan.Chamber bower = SpiderCavePlan.BOWERS.get(0);
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        SpiderCave.Region region = new SpiderCave.Region(at.getX() - bower.x(), at.getZ() - bower.z(), at.getY());
        sweepAround(h);
        SilkRidgeFeature.garrison(level, region, at.getX() & ~15, at.getZ() & ~15, level.random);
        List<SpiderSpiritEntity> sisters = level.getEntitiesOfClass(SpiderSpiritEntity.class, new AABB(at).inflate(3));
        eq(h, 1, sisters.size(), "the bower's sister at its heart");
        SpiderSpiritEntity s = sisters.get(0);
        eq(h, SpiderCavePlan.sisterOf(0), s.sister(), "the youngest, in the first bower");
        eq(h, SpiderCavePlan.sisterLevel(0), s.mobLevel(), "level 82");
        check(h, s.bower() != null && s.bower().index() == 0, "keeping it");
        check(h, s.isPersistenceRequired() && s.hasRestriction(), "kept, at her post");
        double over = s.getMaxHealth() / s.healthAt(s.mobLevel(), true);
        close(h, SpiderSpiritEntity.BOWER_HEALTH, over, "more than a captain's health");
        h.succeed();
    }

    // ── What they leave, and the deed ───────────────────────────────────

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

    private static int count(List<ItemStack> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = PLATFORM)
    public static void theyLeaveSilkCarapacesAndGoldenEyes(GameTestHelper h) {
        ServerPlayer p = player(h);
        for (int i = 0; i < 5; i++) {
            List<ItemStack> lord = loot(h, lord(h, new BlockPos(2, 2, 2)), p);
            check(h, count(lord, SpiderCaveRegistry.GOLDEN_LIGHT_EYE.get()) == 1, "always his Golden-Light Eye: " + lord);
            check(h, count(lord, SpiderCaveRegistry.GOLDEN_EYE.get()) >= 3, "three to five Golden Eyes: " + lord);
        }
        check(h, count(loot(h, silk(h, SpiderCaveRegistry.SPIDER_SPIRIT.get(), 84, true, new BlockPos(1, 2, 1)), p), SpiderCaveRegistry.SPIDER_SILK.get()) >= 2,
                "a sister's silk");
        int silk = 0, shell = 0;
        for (int i = 0; i < 10; i++) {
            silk += count(loot(h, silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 78, false, new BlockPos(3, 2, 3)), p), SpiderCaveRegistry.SPIDER_SILK.get());
            shell += count(loot(h, silk(h, SpiderCaveRegistry.COCKROACH_SON.get(), 78, false, new BlockPos(1, 2, 3)), p), SpiderCaveRegistry.INSECT_CARAPACE.get());
        }
        check(h, silk >= 10 && shell >= 10, "spiders leave silk, sons carapaces: " + silk + ", " + shell);
        // a captain leaves its sigil and a skein of silk besides
        SpiderDemonEntity captain = silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 85, true, new BlockPos(2, 2, 4));
        captain.setNoAi(false);
        captain.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        List<net.minecraft.world.entity.item.ItemEntity> drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, captain.getBoundingBox().inflate(3));
        check(h, drops.stream().anyMatch(d -> d.getItem().is(com.jttw.engine.registry.ModItems.CAPTAINS_SIGIL.get()))
                && drops.stream().anyMatch(d -> d.getItem().is(SpiderCaveRegistry.SPIDER_SILK.get())), "a captain's sigil and silk: " + drops);
        drops.forEach(Entity::discard);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void subduingTheDemonLordIsAGoodDeed(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, Morality.SLAY_HUNDRED_EYED, Morality.killDeed(p, lord(h, new BlockPos(2, 2, 2))), "he poisoned the Tang Monk's tea");
        check(h, Morality.SLAY_HUNDRED_EYED > 0, "a good deed");
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, silk(h, SpiderCaveRegistry.SPIDER_SPIRIT.get(), 84, true, new BlockPos(0, 2, 0))), "a sister, a captain");
        eq(h, Morality.SLAY_MONSTER, Morality.killDeed(p, silk(h, SpiderCaveRegistry.SPIDER_DEMON.get(), 78, false, new BlockPos(4, 2, 4))), "a spider, a monster");
        h.succeed();
    }
}
