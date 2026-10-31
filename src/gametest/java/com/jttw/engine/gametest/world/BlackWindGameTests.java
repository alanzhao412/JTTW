package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.blackwind.BearSoldierEntity;
import com.jttw.engine.entity.blackwind.BlackBearDemonEntity;
import com.jttw.engine.entity.blackwind.BlackWhirlwindEntity;
import com.jttw.engine.entity.blackwind.BlackWindChief;
import com.jttw.engine.entity.blackwind.BlackWindMonster;
import com.jttw.engine.entity.blackwind.BoarDemonEntity;
import com.jttw.engine.entity.blackwind.LingxuziEntity;
import com.jttw.engine.entity.blackwind.VenomBoltEntity;
import com.jttw.engine.entity.blackwind.WhiteRobedScholarEntity;
import com.jttw.engine.entity.blackwind.WindImpEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.item.BrocadeKasayaItem;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.ZoneLayout;
import com.jttw.engine.world.ZoneMap;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * Black Wind Mountain: its one region in White Bone Ridge's ring and band, its creatures (the imps'
 * gust, the boars' charge, the bear soldiers' slam), the Black Bear's two friends (always minibosses:
 * Lingxuzi's pounce and elixir, the Scholar's venom and coils), the Black Bear Demon himself (level 51,
 * a group's fight, his black wind, his maul, his gale, his whirlwinds and phases), what they leave (the
 * kasaya, always, and what it does), XP and karma. The cave itself: {@link BlackWindCaveGameTests}.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlackWindGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        // within a float's step at the golems' health (a blow is measured as health before less after)
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-3 + Math.ulp(1_000_000f), what + ": expected " + expected + " but was " + actual);
    }

    private static void sweepAround(GameTestHelper h, Entity... held) {
        AABB bounds = h.getBounds().inflate(2);
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, bounds, x -> x instanceof BlackWindMonster || x instanceof BlackBearDemonEntity
                    || x instanceof BlackWhirlwindEntity || x instanceof VenomBoltEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends BlackWindMonster> T mob(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        m.setNoAi(true);
        sweepAround(h, m);
        return m;
    }

    private static BlackBearDemonEntity king(GameTestHelper h, BlockPos at) {
        BlackBearDemonEntity b = h.spawn(BlackWindRegistry.BLACK_BEAR_DEMON.get(), at);
        b.confine(2.5);
        sweepAround(h, b);
        return b;
    }

    /** A still iron golem, raised so no blow ends it: a target to measure blows on. */
    private static LivingEntity golem(GameTestHelper h, BlockPos at) {
        var g = h.spawn(EntityType.IRON_GOLEM, at);
        g.goalSelector.removeAllGoals(x -> true);
        g.targetSelector.removeAllGoals(x -> true);
        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000); // a float's health; any more and a hit is rounded to whole points
        g.getAttribute(Attributes.ARMOR).setBaseValue(0);
        g.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
        g.setHealth(g.getMaxHealth());
        sweepAround(h, g);
        return g;
    }

    private static float taken(LivingEntity e, float before) {
        return before - e.getHealth();
    }

    // ── Where ───────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void oneRegionAWorldInTheRidgesRingAndBandClearOfIt(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[][] cs = Zones.BLACK_WIND.centres(seed);
        eq(h, 1, cs.length, "one region a world");
        double d = Math.hypot(cs[0][0], cs[0][1]);
        ZoneLayout l = Zones.BLACK_WIND.layout();
        check(h, d >= l.near() - 1 && d <= l.far() + 500 * 4, "in White Bone Ridge's ring (1-2 km), not far past: " + Math.round(d));
        check(h, l.near() == ZoneLayout.WHITE_BONE.near() && l.far() == ZoneLayout.WHITE_BONE.far(), "the ridge's ring");
        int[] ridge = Zones.WHITE_BONE.centres(seed)[0];
        check(h, Math.hypot(ridge[0] - cs[0][0], ridge[1] - cs[0][1]) >= l.margin() + ZoneLayout.WHITE_BONE.margin() + ZoneMap.GAP,
                "clear of White Bone Ridge");
        check(h, Zones.BLACK_WIND.minLevel() == 20 && Zones.BLACK_WIND.maxLevel() == 36, "the band of 20-36");
        check(h, Zones.ALL.get(Zones.ALL.size() - 1) == Zones.BLACK_WIND || Zones.ALL.contains(Zones.BLACK_WIND), "a zone of the world");
        h.succeed();
    }

    // ── The creatures ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theMountainsCreaturesAreOfTheRidgesBandTheirCaptainsPurpleAndStronger(GameTestHelper h) {
        for (EntityType<? extends BlackWindMonster> type : List.of(BlackWindRegistry.WIND_IMP.get(), BlackWindRegistry.BOAR_DEMON.get(), BlackWindRegistry.BEAR_SOLDIER.get())) {
            BlackWindMonster r = mob(h, type, 20, false, new BlockPos(1, 2, 1)), c = mob(h, type, 27, true, new BlockPos(3, 2, 3));
            eq(h, 20, r.regularLevel(), "regulars about 20");
            eq(h, 27, r.captainLevel(), "captains about 27");
            check(h, c.isCaptain() && c.getMaxHealth() > r.getMaxHealth() * 2 && c.getAttributeValue(Attributes.ATTACK_DAMAGE) > r.getAttributeValue(Attributes.ATTACK_DAMAGE),
                    "a captain, stronger");
            check(h, c.getScale() > r.getScale(), "and larger");
            eq(h, Zones.BLACK_WIND.id(), r.getPersistentData().getString(ZoneMonster.KEY_ZONE), "its kills count toward the mountain's band");
            r.discard();
            c.discard();
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void anImpRidesItsGustAtItsFoeAndThrowsItBack(GameTestHelper h) {
        WindImpEntity imp = mob(h, BlackWindRegistry.WIND_IMP.get(), 20, false, new BlockPos(0, 2, 2));
        LivingEntity g = golem(h, new BlockPos(4, 2, 2));
        imp.whirl(g);
        eq(h, WindImpEntity.WHIRLING, imp.state(), "it spins up first (its wind-up)");
        float before = g.getHealth();
        check(h, imp.buffet(g), "its gust strikes");
        close(h, imp.gustDamage(), taken(g, before), "a gust of 7% of a level-20 character's health");
        close(h, WindImpEntity.GUST * MobScaling.typicalHealth(20), imp.gustDamage(), "its gust's share");
        check(h, g.getDeltaMovement().horizontalDistance() > 0.2, "thrown back: " + g.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBoarPawsTheGroundThenChargesAndGores(GameTestHelper h) {
        BoarDemonEntity boar = mob(h, BlackWindRegistry.BOAR_DEMON.get(), 20, false, new BlockPos(0, 2, 2));
        LivingEntity g = golem(h, new BlockPos(4, 2, 2));
        boar.paw(g);
        eq(h, BoarDemonEntity.PAWING, boar.state(), "it paws the ground first");
        float before = g.getHealth();
        check(h, boar.gore(g), "its tusks strike");
        close(h, boar.goreDamage(), taken(g, before), "a gore of 10%");
        check(h, g.getDeltaMovement().y > 0.2, "thrown up");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBearSoldierRearsThenSlamsAllNearIt(GameTestHelper h) {
        BearSoldierEntity bear = mob(h, BlackWindRegistry.BEAR_SOLDIER.get(), 20, false, new BlockPos(2, 2, 2));
        LivingEntity near = golem(h, new BlockPos(0, 2, 2)), also = golem(h, new BlockPos(4, 2, 4));
        bear.rear();
        check(h, bear.isRaising(), "reared up first");
        float a = near.getHealth(), b = also.getHealth();
        List<LivingEntity> hit = bear.slam();
        check(h, hit.contains(near) && hit.contains(also), "both within its reach");
        close(h, bear.slamDamage(), taken(near, a), "a slam of 11%");
        close(h, bear.slamDamage(), taken(also, b), "on each");
        h.succeed();
    }

    // ── The Black Bear's friends ────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void hisTwoFriendsAreAlwaysMinibosses(GameTestHelper h) {
        for (EntityType<? extends BlackWindChief> type : List.of(BlackWindRegistry.LINGXUZI.get(), BlackWindRegistry.WHITE_ROBED_SCHOLAR.get())) {
            BlackWindChief c = mob(h, type, 20, false, new BlockPos(2, 2, 2));
            check(h, c.isCaptain() && c.isMiniboss(), "a miniboss whoever brings him");
            check(h, Math.abs(c.mobLevel() - BlackWindChief.CHIEF_LEVEL) <= 1, "about level 30: " + c.mobLevel());
            check(h, c.mobLevel() <= Zones.BLACK_WIND.maxLevel(), "within the band");
            double blow = c.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(c.mobLevel());
            close(h, ZoneMonster.MINIBOSS_HIT, c.hardestShare() * c.damageScale(), "his hardest hit a miniboss's");
            check(h, blow > 0.145 && blow <= 0.2, "a miniboss's blow: " + blow);
            c.discard();
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void lingxuziPouncesOnAllWhereHeLands(GameTestHelper h) {
        LingxuziEntity l = mob(h, BlackWindRegistry.LINGXUZI.get(), 30, true, new BlockPos(2, 2, 2));
        LivingEntity g = golem(h, new BlockPos(0, 2, 2)), g2 = golem(h, new BlockPos(4, 2, 2));
        l.crouch();
        eq(h, LingxuziEntity.CROUCHING, l.state(), "he crouches first");
        float a = g.getHealth(), b = g2.getHealth();
        List<LivingEntity> hit = l.land();
        check(h, hit.contains(g) && hit.contains(g2), "all where he lands");
        close(h, l.pounceDamage(), taken(g, a), "his pounce");
        close(h, l.pounceDamage(), taken(g2, b), "on each");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void lingxuziSwallowsAnElixirOnceUnderHalfHisHealth(GameTestHelper h) {
        LingxuziEntity l = mob(h, BlackWindRegistry.LINGXUZI.get(), 30, true, new BlockPos(2, 2, 2));
        l.setNoAi(false);
        l.setHealth(l.getMaxHealth() * 0.4f);
        float low = l.getHealth();
        h.runAfterDelay(LingxuziEntity.DRINK + 10, () -> {
            check(h, l.getHealth() >= low + l.getMaxHealth() * 0.19f, "a fifth of his health back: " + l.getHealth() + " from " + low);
            float after = l.getHealth();
            l.setHealth(l.getMaxHealth() * 0.3f);
            h.runAfterDelay(LingxuziEntity.DRINK + 5, () -> {
                check(h, l.getHealth() <= l.getMaxHealth() * 0.31f, "and only once");
                h.succeed();
            });
        });
    }

    @GameTest(template = PLATFORM)
    public static void theScholarSpitsVenomAndLashesWithHisTail(GameTestHelper h) {
        WhiteRobedScholarEntity s = mob(h, BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(), 30, true, new BlockPos(2, 2, 2));
        LivingEntity g = golem(h, new BlockPos(0, 2, 2)), g2 = golem(h, new BlockPos(4, 2, 2));
        VenomBoltEntity bolt = s.spit(g);
        sweepAround(h, bolt);
        float a = g.getHealth();
        check(h, bolt.strike(g), "the venom lands");
        close(h, s.venomDamage(), taken(g, a), "his spit");
        check(h, g.hasEffect(MobEffects.POISON), "and poisons");
        float b = g2.getHealth();
        List<LivingEntity> hit = s.lash();
        check(h, hit.contains(g2), "his coils reach");
        close(h, s.lashDamage(), taken(g2, b), "his lash");
        check(h, g2.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and slow");
        h.succeed();
    }

    // ── The Black Bear Demon ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theKingTowersOverHisBandAGroupsFight(GameTestHelper h) {
        BlackBearDemonEntity k = king(h, new BlockPos(2, 2, 2));
        eq(h, 51, k.mobLevel(), "level 51: his band's top and fifteen");
        eq(h, Zones.BLACK_WIND.maxLevel() + BlackBearDemonEntity.LEVELS_ABOVE_BAND, k.mobLevel(), "the band's top and fifteen");
        close(h, 300, k.getMaxHealth() / MobScaling.typicalHit(51), "300 ordinary hits of a level-51 character");
        close(h, 0.30, k.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(51), "a spear thrust of 30%");
        check(h, k.bossBar().getColor() == BossEvent.BossBarColor.PURPLE, "a purple bar");
        check(h, !k.removeWhenFarAway(1000), "never despawned");
        eq(h, EnemyTier.BOSS.name(), k.getPersistentData().getString(ZoneMonster.KEY_TIER), "a boss's kill");
        for (double s : new double[]{BlackBearDemonEntity.WIND, BlackBearDemonEntity.SLAM, BlackBearDemonEntity.GALE}) check(h, s <= 0.33, "no special over a boss's blow");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theBlackWindStrikesBlindsAndHurlsAllRoundHim(GameTestHelper h) {
        BlackBearDemonEntity k = king(h, new BlockPos(2, 2, 2));
        LivingEntity a = golem(h, new BlockPos(0, 2, 0)), b = golem(h, new BlockPos(4, 2, 4));
        k.pose(BlackBearDemonEntity.GATHERING);
        eq(h, BlackBearDemonEntity.GATHERING, k.action(), "the wind gathering first (his wind-up)");
        float ha = a.getHealth(), hb = b.getHealth();
        List<LivingEntity> hit = k.blackWind();
        check(h, hit.contains(a) && hit.contains(b), "all round him");
        close(h, k.windDamage(), taken(a, ha), "22% of a level-51 character's health");
        close(h, k.windDamage(), taken(b, hb), "on each");
        check(h, a.hasEffect(MobEffects.BLINDNESS) && b.hasEffect(MobEffects.BLINDNESS), "blinded");
        Vec3 away = a.position().subtract(k.position());
        check(h, a.getDeltaMovement().dot(away) > 0 && a.getDeltaMovement().y > 0.3, "hurled away and up: " + a.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void hisMaulCrashesOnAllNearAndHisGaleOnlyBeforeHim(GameTestHelper h) {
        BlackBearDemonEntity k = king(h, new BlockPos(2, 2, 2));
        k.confine(4);
        LivingEntity front = golem(h, new BlockPos(2, 2, 4)), behind = golem(h, new BlockPos(2, 2, 0));
        float f = front.getHealth(), b = behind.getHealth();
        List<LivingEntity> mauled = k.maul();
        check(h, mauled.contains(front) && mauled.contains(behind), "the maul all round");
        close(h, k.slamDamage(), taken(front, f), "26%");
        front.invulnerableTime = 0; // the maul's hurt-time past, as it would be by the gale's wind-up
        f = front.getHealth();
        b = behind.getHealth();
        List<LivingEntity> blown = k.gale(front);
        check(h, blown.contains(front) && !blown.contains(behind), "the gale only before him");
        close(h, k.galeDamage(), taken(front, f), "25%");
        eq(h, b, behind.getHealth(), "nothing behind");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void atSixTenthsAndThreeTenthsTheBlackWindRisesInWhirls(GameTestHelper h) {
        BlackBearDemonEntity k = king(h, new BlockPos(2, 2, 2));
        k.setNoAi(true);
        k.setHealth(k.getMaxHealth() * 0.55f);
        h.runAfterDelay(2, () -> {
            eq(h, 1, k.phase(), "his first phase");
            eq(h, BlackBearDemonEntity.WHIRLS_FIRST, k.whirlwinds().size(), "three whirlwinds");
            for (BlackWhirlwindEntity w : k.whirlwinds()) {
                check(h, w.isHostile(), "his, hunting his foes");
                close(h, k.whirlDamage(), w.damage(), "each striking for 4% each half second");
            }
            k.setHealth(k.getMaxHealth() * 0.25f);
            h.runAfterDelay(2, () -> {
                eq(h, 2, k.phase(), "his second");
                eq(h, BlackBearDemonEntity.WHIRLS_FIRST + BlackBearDemonEntity.WHIRLS_SECOND, k.whirlwinds().size(), "two more");
                check(h, k.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.27, "and he is quicker");
                k.kill();
                h.runAfterDelay(2, () -> {
                    check(h, k.whirlwinds().isEmpty(), "his whirlwinds die with him");
                    h.succeed();
                });
            });
        });
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
    public static void subduedHeGivesUpTheKasayaEveryTime(GameTestHelper h) {
        ServerPlayer p = player(h);
        BlackBearDemonEntity k = king(h, new BlockPos(2, 2, 2));
        for (int i = 0; i < 5; i++) eq(h, 1, count(loot(h, k, p), BlackWindRegistry.BROCADE_KASAYA.get()), "the kasaya, every time");
        LingxuziEntity l = mob(h, BlackWindRegistry.LINGXUZI.get(), 30, true, new BlockPos(0, 2, 0));
        check(h, count(loot(h, l, p), net.minecraft.world.item.Items.GOLDEN_APPLE) >= 1, "Lingxuzi leaves an elixir (a golden apple)");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void noFireBurnsTheKasayasWearerAndPoisonLeavesHim(GameTestHelper h) {
        var z = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        z.setNoAi(true);
        sweepAround(h, z);
        z.setItemSlot(EquipmentSlot.CHEST, new ItemStack(BlackWindRegistry.BROCADE_KASAYA.get()));
        check(h, BrocadeKasayaItem.wears(z), "he wears it");
        float before = z.getHealth();
        z.hurt(h.getLevel().damageSources().inFire(), 6);
        z.hurt(h.getLevel().damageSources().lava(), 6);
        eq(h, before, z.getHealth(), "no fire burns him");
        var bare = h.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        bare.setNoAi(true);
        sweepAround(h, bare);
        float b = bare.getHealth();
        bare.hurt(h.getLevel().damageSources().inFire(), 6);
        check(h, bare.getHealth() < b, "the bare one burns");
        var armour = ((net.minecraft.world.item.ArmorItem) BlackWindRegistry.BROCADE_KASAYA.get()).getDefense();
        eq(h, 8, armour, "as diamond on the chest");
        h.succeed();
    }

    // ── XP and karma ────────────────────────────────────────────────────

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(template = PLATFORM)
    public static void theMountainPaysItsBandAndSubduingTheBearIsAGoodDeed(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 20);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.BLACK_WIND_BIOME.location());
        check(h, band != null && band.minLevel() == 20 && band.maxLevel() == 36, "the mountain's band is 20-36");
        killedBy(p, mob(h, BlackWindRegistry.WIND_IMP.get(), 20, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a regular");
        long xp = PlayerLevel.getXp(p);
        killedBy(p, king(h, new BlockPos(3, 2, 3)));
        eq(h, xp + band.xpForTier(EnemyTier.BOSS), PlayerLevel.getXp(p), "plus the Black Bear, a boss");
        check(h, Morality.killDeed(p, king(h, new BlockPos(2, 2, 2))) == BlackBearDemonEntity.KARMA && BlackBearDemonEntity.KARMA > 0, "subduing him a good deed");
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, mob(h, BlackWindRegistry.LINGXUZI.get(), 30, true, new BlockPos(0, 2, 4))), "Lingxuzi, a captain");
        h.succeed();
    }
}
