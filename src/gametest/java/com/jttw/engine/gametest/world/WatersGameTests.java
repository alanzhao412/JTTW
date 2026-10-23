package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.waters.CrabGeneralEntity;
import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.entity.waters.SeaBoltEntity;
import com.jttw.engine.entity.waters.SeaMonster;
import com.jttw.engine.entity.waters.ShrimpSoldierEntity;
import com.jttw.engine.entity.waters.YakshaEntity;
import com.jttw.engine.gametest.character.CharTest;
import com.jttw.engine.item.DragonRegaliaItem;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.morality.Morality;
import com.jttw.engine.world.waters.DragonPalace;
import com.jttw.engine.world.waters.SeaBreath;
import com.jttw.engine.world.waters.SeaCalmingPillarBlock;
import com.jttw.engine.world.waters.Sea;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Mystical Waters: its creatures in the water, its captains and its XP, the four Dragon Kings'
 * own attacks and what each leaves, the pillar of the Eastern treasury, the palace's blueprint, and
 * how a player breathes down there.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class WatersGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-4 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    /**
     * Removes, when the test ends, the sea's creatures this test brought, and any (escorts, a guard,
     * bolts) within its own bounds: never a sweep wider than them, which would take a neighbouring
     * test's creatures mid-test.
     */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        AABB bounds = h.getBounds().inflate(1);
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, bounds,
                    x -> x instanceof SeaMonster || x instanceof DragonKingEntity || x instanceof SeaBoltEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends SeaMonster> T sea(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    private static <T extends DragonKingEntity> T king(GameTestHelper h, EntityType<T> type, BlockPos at) {
        T k = h.spawn(type, at);
        sweepAround(h, k);
        return k;
    }

    /** The middle of block (x, z) of the platform at height y (absoluteVec turns a rotated test's points about another corner). */
    private static Vec3 at(GameTestHelper h, double x, double y, double z) {
        BlockPos b = h.absolutePos(new BlockPos((int) Math.floor(x), 0, (int) Math.floor(z)));
        return new Vec3(b.getX() + 0.5, b.getY() + y, b.getZ() + 0.5);
    }

    /** Floods the platform: water over its floor to the roof. */
    private static void flood(GameTestHelper h) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) for (int y = 2; y <= 5; y++) {
            h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
        }
    }

    /** A still iron golem, its health raised so no level-55 blow ends it: a target to measure blows on. */
    private static LivingEntity golem(GameTestHelper h, BlockPos at) {
        var g = h.spawn(EntityType.IRON_GOLEM, at);
        g.setNoAi(true);
        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
        g.setHealth(g.getMaxHealth());
        return g;
    }

    // ── In the water ────────────────────────────────────────────────────

    private static String nodes(net.minecraft.world.entity.Mob m) {
        var path = m.getNavigation().getPath();
        if (path == null) return "none";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < path.getNodeCount(); i++) b.append(path.getNode(i).asBlockPos()).append(i == path.getNextNodeIndex() ? "*" : "").append(' ');
        return b.toString();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void aShrimpSwimsAcrossAndUpThroughTheWaterAndNeverDrowns(GameTestHelper h) {
        flood(h);
        ShrimpSoldierEntity shrimp = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 47, false, new BlockPos(0, 2, 0));
        check(h, shrimp.getNavigation() instanceof AmphibiousPathNavigation, "it swims (amphibious navigation)");
        check(h, !shrimp.canDrownInFluidType(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()), "it breathes water");
        shrimp.goalSelector.removeAllGoals(g -> true);
        shrimp.targetSelector.removeAllGoals(g -> true);
        double start = shrimp.getY();
        // across the flooded platform and up, as far as the roof leaves it room: two blocks
        Vec3 up = at(h, 3.5, 4, 3.5);
        h.succeedWhen(() -> {
            if (shrimp.getNavigation().isDone()) shrimp.getNavigation().moveTo(up.x, up.y, up.z, 1.2);
            check(h, shrimp.isInWater(), "in the water");
            check(h, shrimp.getAirSupply() == shrimp.getMaxAirSupply(), "never short of air");
            check(h, Math.hypot(shrimp.getX() - up.x, shrimp.getZ() - up.z) < 1.0, "it swam across to the far side of the pool: at " + shrimp.position());
            check(h, shrimp.getY() > start + 0.2, "it swam up: from " + start + " to " + shrimp.getY() + " (its path: " + nodes(shrimp) + ")");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void aShrimpHangsInTheWaterWhereItStops(GameTestHelper h) {
        flood(h);
        ShrimpSoldierEntity shrimp = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 47, false, new BlockPos(2, 2, 2));
        shrimp.moveTo(at(h, 2.5, 3.2, 2.5));
        shrimp.goalSelector.removeAllGoals(g -> true);
        double y = shrimp.getY();
        h.runAfterDelay(60, () -> {
            check(h, Math.abs(shrimp.getY() - y) < 0.4, "it neither sank nor floated: from " + y + " to " + shrimp.getY());
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void aCrabWalksTheFloorAndSpringsAtWhatSwimsAbove(GameTestHelper h) {
        flood(h);
        CrabGeneralEntity crab = sea(h, ModEntities.CRAB_GENERAL.get(), 47, false, new BlockPos(2, 2, 2));
        check(h, crab.getNavigation() instanceof GroundPathNavigation && !crab.swims(), "it walks, not swims");
        crab.moveTo(at(h, 2.5, 4, 2.5));
        h.runAfterDelay(50, () -> {
            check(h, crab.getY() < at(h, 0, 2.5, 0).y, "it sank to the floor: y " + crab.getY() + " floor " + at(h, 0, 2, 0).y);
            LivingEntity above = golem(h, new BlockPos(2, 5, 2));
            check(h, crab.springAt(above), "it springs");
            check(h, crab.getDeltaMovement().y >= CrabGeneralEntity.SPRING - 1e-6, "upward");
            check(h, !crab.springAt(above), "and must gather itself before the next");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void aCrabsPinchHolds(GameTestHelper h) {
        CrabGeneralEntity crab = sea(h, ModEntities.CRAB_GENERAL.get(), 47, false, new BlockPos(1, 2, 1));
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        float before = g.getHealth();
        crab.doHurtTarget(g);
        close(h, crab.blowAt(47, false), before - g.getHealth(), "its pinch");
        check(h, g.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and holds them");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 80)
    public static void aYakshasBoltCutsThroughTheWaterAndThrowsBack(GameTestHelper h) {
        flood(h);
        YakshaEntity yaksha = sea(h, ModEntities.YAKSHA.get(), 47, false, new BlockPos(0, 2, 0));
        yaksha.setNoAi(true);
        check(h, yaksha.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() || yaksha.getItemBySlot(EquipmentSlot.MAINHAND).is(net.minecraft.world.item.Items.TRIDENT), "its trident");
        LivingEntity g = golem(h, new BlockPos(4, 2, 4));
        float before = g.getHealth();
        SeaBoltEntity bolt = yaksha.hurl(g);
        check(h, bolt.element() == SeaBoltEntity.TIDE, "a bolt of the tide");
        close(h, yaksha.boltDamage(), YakshaEntity.BOLT * MobScaling.typicalHealth(47), "its hurt");
        h.succeedWhen(() -> check(h, g.getHealth() < before, "it struck through the water (bolt at " + whereabouts(h, bolt) + ")"));
    }

    @GameTest(template = PLATFORM)
    public static void aYakshaRaisesTheAlarm(GameTestHelper h) {
        ServerPlayer p = player(h);
        YakshaEntity yaksha = sea(h, ModEntities.YAKSHA.get(), 47, false, new BlockPos(0, 2, 0));
        ShrimpSoldierEntity shrimp = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 47, false, new BlockPos(4, 2, 4));
        shrimp.setNoAi(true);
        yaksha.setTarget(p);
        check(h, shrimp.getTarget() == p, "the patrol's alarm turns the sea on its foe");
        h.succeed();
    }

    // ── Captains and XP ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aSeaCaptainIsPurpleLargerAndBringsItsKind(GameTestHelper h) {
        ShrimpSoldierEntity regular = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 47, false, new BlockPos(1, 2, 1));
        ShrimpSoldierEntity captain = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 54, true, new BlockPos(2, 2, 2));
        eq(h, SeaMonster.PURPLE, captain.getDisplayName().getStyle().getColor().getValue(), "a captain's name is purple");
        check(h, captain.getDisplayName().getString().contains("Captain") && captain.getDisplayName().getString().contains("Lv 54"), captain.getDisplayName().getString());
        check(h, regular.getDisplayName().getString().contains("Lv 47"), regular.getDisplayName().getString());
        close(h, ShrimpSoldierEntity.HITS * MobScaling.typicalHit(54) * SeaMonster.CAPTAIN_HEALTH, captain.getMaxHealth(), "a captain's health");
        near(h, regular.getBbHeight() * SeaMonster.CAPTAIN_SCALE, captain.getBbHeight(), "it stands taller");
        eq(h, SeaMonster.LEVEL + SeaMonster.CAPTAIN_OVER, captain.captainLevel(), "captains about 54");
        captain.bringEscorts(h.getLevel());
        List<ShrimpSoldierEntity> escorts = h.getLevel().getEntitiesOfClass(ShrimpSoldierEntity.class, captain.getBoundingBox().inflate(6), e -> e != captain && e != regular);
        eq(h, SeaMonster.ESCORTS, escorts.size(), "escorts");
        for (ShrimpSoldierEntity e : escorts) check(h, !e.isCaptain() && Math.abs(e.mobLevel() - 47) <= 1, "regulars about 47");
        h.succeed();
    }

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(template = PLATFORM)
    public static void theWatersPayTheirBand(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 47);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.MYSTICAL_WATERS_BIOME.location());
        check(h, band != null && band.minLevel() == 47 && band.maxLevel() == 56, "the waters' band is 47-56");
        eq(h, 396L, band.xpForTier(EnemyTier.GRUNT), "a grunt pays 110 x 3.6");
        killedBy(p, sea(h, ModEntities.SHRIMP_SOLDIER.get(), 47, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a regular");
        killedBy(p, sea(h, ModEntities.CRAB_GENERAL.get(), 54, true, new BlockPos(3, 2, 3)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a captain");
        PlayerLevel.setLevel(p, 62);
        long xp = PlayerLevel.getXp(p);
        killedBy(p, sea(h, ModEntities.YAKSHA.get(), 47, false, new BlockPos(2, 2, 4)));
        eq(h, xp, PlayerLevel.getXp(p), "nothing past 56 and its five levels' grace");
        h.succeed();
    }

    // ── The Dragon Kings ────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void eachKingIsHisSeas(GameTestHelper h) {
        Object[][] kings = {
                {ModEntities.AO_GUANG.get(), Sea.EAST, SeaBoltEntity.TIDE, BossEvent.BossBarColor.GREEN},
                {ModEntities.AO_QIN.get(), Sea.SOUTH, SeaBoltEntity.STEAM, BossEvent.BossBarColor.RED},
                {ModEntities.AO_RUN.get(), Sea.WEST, SeaBoltEntity.CURRENT, BossEvent.BossBarColor.WHITE},
                {ModEntities.AO_SHUN.get(), Sea.NORTH, SeaBoltEntity.ICE, BossEvent.BossBarColor.PURPLE}};
        for (Object[] k : kings) {
            @SuppressWarnings("unchecked")
            DragonKingEntity king = king(h, (EntityType<? extends DragonKingEntity>) k[0], new BlockPos(2, 2, 2));
            eq(h, k[1], king.sea(), "his sea");
            eq(h, k[2], king.element(), king.sea() + "'s bolts");
            eq(h, k[3], king.bossBar().getColor(), king.sea() + "'s bar");
            eq(h, k[0], DragonKingEntity.typeOf(king.sea()), "his type");
            check(h, king.getDisplayName().getString().contains("Lv " + DragonKingEntity.LEVEL), king.getDisplayName().getString());
            close(h, DragonKingEntity.HEALTH, king.getMaxHealth(), "sixty ordinary hits of a level-55 character");
            eq(h, EnemyTier.BOSS.name(), king.getPersistentData().getString(SeaMonster.KEY_TIER), "a Boss of the waters");
            check(h, !king.canDrownInFluidType(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()), "he breathes water");
            check(h, king.getNavigation() instanceof AmphibiousPathNavigation, "he swims");
            king.discard();
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aoGuangsTidalSurgeHurlsAllAway(GameTestHelper h) {
        DragonKingEntity king = king(h, ModEntities.AO_GUANG.get(), new BlockPos(0, 2, 0));
        king.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        ShrimpSoldierEntity own = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 47, false, new BlockPos(1, 2, 3));
        float before = g.getHealth(), ownBefore = own.getHealth();
        eq(h, 1, king.tidalSurge(), "caught the golem alone");
        close(h, king.wrathDamage(), before - g.getHealth(), "the surge's hurt");
        Vec3 away = g.position().subtract(king.position());
        check(h, g.getDeltaMovement().x * away.x + g.getDeltaMovement().z * away.z > 0.5, "thrown away from him: " + g.getDeltaMovement());
        eq(h, ownBefore, own.getHealth(), "his own soldiers untouched");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aoQinsBoilingSeaScaldsAndSaps(GameTestHelper h) {
        DragonKingEntity king = king(h, ModEntities.AO_QIN.get(), new BlockPos(0, 2, 0));
        king.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        float before = g.getHealth();
        eq(h, 1, king.boilingSea(), "caught");
        close(h, king.wrathDamage(), before - g.getHealth(), "it scalds");
        check(h, g.hasEffect(MobEffects.WEAKNESS), "and saps their strength");
        check(h, g.getDeltaMovement().horizontalDistance() < 0.3, "but throws no one about");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void aoRunsWhirlpoolDragsAllToItsHeart(GameTestHelper h) {
        DragonKingEntity king = king(h, ModEntities.AO_RUN.get(), new BlockPos(0, 2, 0));
        king.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(4, 2, 4));
        Vec3 heart = at(h, 1.5, 2, 1.5);
        king.whirlpool(heart);
        check(h, king.whirlpool() != null, "it turns");
        g.setDeltaMovement(Vec3.ZERO);
        king.turnWhirlpool();
        Vec3 in = heart.subtract(g.position());
        check(h, g.getDeltaMovement().x * in.x + g.getDeltaMovement().z * in.z > 0, "dragged toward its heart: " + g.getDeltaMovement());
        for (int i = 0; i < DragonKingEntity.WHIRL_TICKS; i++) king.turnWhirlpool();
        check(h, king.whirlpool() == null, "and it spends itself");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aoShunsFrozenAbyssFreezes(GameTestHelper h) {
        DragonKingEntity king = king(h, ModEntities.AO_SHUN.get(), new BlockPos(0, 2, 0));
        king.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        float before = g.getHealth();
        eq(h, 1, king.frozenAbyss(), "caught");
        check(h, g.getHealth() < before, "it bites");
        check(h, g.isFullyFrozen(), "frozen through: " + g.getTicksFrozen());
        check(h, g.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and slowed hard");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eachKingsBoltCarriesHisSea(GameTestHelper h) {
        DragonKingEntity king = king(h, ModEntities.AO_SHUN.get(), new BlockPos(0, 2, 0));
        king.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        SeaBoltEntity ice = new SeaBoltEntity(h.getLevel(), king, new Vec3(1, 0, 0), 5f, SeaBoltEntity.ICE);
        ice.touch(g, king);
        check(h, g.isFullyFrozen(), "ice freezes");
        SeaBoltEntity steam = new SeaBoltEntity(h.getLevel(), king, new Vec3(1, 0, 0), 5f, SeaBoltEntity.STEAM);
        steam.touch(g, king);
        check(h, g.hasEffect(MobEffects.WEAKNESS), "steam saps");
        g.setDeltaMovement(Vec3.ZERO);
        new SeaBoltEntity(h.getLevel(), king, new Vec3(1, 0, 0), 5f, SeaBoltEntity.CURRENT).touch(g, king);
        Vec3 toKing = king.position().subtract(g.position());
        check(h, g.getDeltaMovement().x * toKing.x + g.getDeltaMovement().z * toKing.z > 0, "the current drags toward him");
        ice.discard();
        steam.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void aKingCallsHisGuardAtTwoThirds(GameTestHelper h) {
        DragonKingEntity king = king(h, ModEntities.AO_GUANG.get(), new BlockPos(2, 2, 2));
        king.setNoAi(true);
        king.setHealth(king.getMaxHealth() * 0.6f);
        h.succeedWhen(() -> {
            eq(h, 1, king.phase(), "his second phase");
            int guard = h.getLevel().getEntitiesOfClass(SeaMonster.class, king.getBoundingBox().inflate(8)).size();
            check(h, guard >= 3, "his soldiers came: " + guard);
        });
    }

    private static List<ItemStack> loot(GameTestHelper h, DragonKingEntity king, ServerPlayer p) {
        var source = h.getLevel().damageSources().playerAttack(p);
        LootParams params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, king)
                .withParameter(LootContextParams.ORIGIN, king.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, p)
                .create(LootContextParamSets.ENTITY);
        return h.getLevel().getServer().reloadableRegistries().getLootTable(king.getLootTable()).getRandomItems(params);
    }

    private static boolean has(List<ItemStack> items, Item item) {
        return items.stream().anyMatch(s -> s.is(item));
    }

    @GameTest(template = PLATFORM)
    public static void eachKingLeavesWhatHeGaveTheMonkeyKing(GameTestHelper h) {
        ServerPlayer p = player(h);
        Item cap = ModItems.PHOENIX_PLUME_CAP.get(), mail = ModItems.GOLDEN_CHAIN_MAIL.get(), boots = ModItems.CLOUD_STEPPING_BOOTS.get();
        List<ItemStack> west = loot(h, king(h, ModEntities.AO_RUN.get(), new BlockPos(1, 2, 1)), p);
        check(h, has(west, cap) && !has(west, mail) && !has(west, boots), "Ao Run: the Phoenix-Plume Cap: " + west);
        List<ItemStack> south = loot(h, king(h, ModEntities.AO_QIN.get(), new BlockPos(2, 2, 2)), p);
        check(h, has(south, mail) && !has(south, cap), "Ao Qin: the Golden Chain Mail: " + south);
        List<ItemStack> north = loot(h, king(h, ModEntities.AO_SHUN.get(), new BlockPos(3, 2, 3)), p);
        check(h, has(north, boots) && !has(north, cap), "Ao Shun: the Cloud-Stepping Boots: " + north);
        List<ItemStack> east = loot(h, king(h, ModEntities.AO_GUANG.get(), new BlockPos(4, 2, 4)), p);
        check(h, !has(east, cap) && !has(east, mail) && !has(east, boots) && !has(east, ModItems.RUYI_JINGU_BANG.get()),
                "Ao Guang's gift is in his treasury, not on him: " + east);
        // the regalia: each its slot, and its bonus
        check(h, cap instanceof DragonRegaliaItem c && c.getEquipmentSlot() == EquipmentSlot.HEAD, "a cap");
        check(h, mail instanceof DragonRegaliaItem m && m.getEquipmentSlot() == EquipmentSlot.CHEST, "a mail");
        check(h, boots instanceof DragonRegaliaItem b && b.getEquipmentSlot() == EquipmentSlot.FEET, "boots");
        double pres = mail.getDefaultAttributeModifiers(new ItemStack(mail)).modifiers().stream()
                .filter(e -> e.attribute().is(com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.getKey()) && e.slot().test(EquipmentSlot.CHEST))
                .mapToDouble(e -> e.modifier().amount()).sum();
        near(h, DragonRegaliaItem.MAIL_PRES, pres, "the mail's physical resistance, worn on the chest");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void slayingAKingIsACrimeAgainstHeaven(GameTestHelper h) {
        ServerPlayer p = player(h);
        DragonKingEntity king = king(h, ModEntities.AO_GUANG.get(), new BlockPos(2, 2, 2));
        eq(h, Morality.SLAY_DRAGON_KING, Morality.killDeed(p, king), "a Dragon King is Heaven's official");
        check(h, 4 * Morality.SLAY_DRAGON_KING <= Morality.OUTLAW, "all four make an outlaw");
        ShrimpSoldierEntity captain = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 54, true, new BlockPos(4, 2, 4));
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, captain), "a captain of the sea");
        ShrimpSoldierEntity regular = sea(h, ModEntities.SHRIMP_SOLDIER.get(), 47, false, new BlockPos(0, 2, 0));
        eq(h, Morality.SLAY_MONSTER, Morality.killDeed(p, regular), "a soldier of the sea, a monster");
        h.succeed();
    }

    // ── The palace and the Eastern treasury ─────────────────────────────

    @GameTest(template = PLATFORM)
    public static void thePalacesBlueprint(GameTestHelper h) {
        for (Sea sea : Sea.values()) {
            check(h, DragonPalace.at(sea, 0, 3, 0).is(Blocks.GOLD_BLOCK), "his dais of gold, at the throne");
            check(h, DragonPalace.at(sea, 0, DragonPalace.THRONE_Y, 0) == null, "and room for him on it");
            int chests = 0, pillar = 0, lanterns = 0;
            for (int x = DragonPalace.MIN_X; x <= DragonPalace.MAX_X; x++) for (int z = DragonPalace.MIN_Z; z <= DragonPalace.MAX_Z; z++) {
                for (int y = 0; y <= DragonPalace.HEIGHT; y++) {
                    var s = DragonPalace.at(sea, x, y, z);
                    if (s == null) continue;
                    if (s.is(Blocks.CHEST)) chests++;
                    if (s.is(ModBlocks.SEA_CALMING_PILLAR.get())) pillar++;
                    if (s.is(Blocks.SEA_LANTERN)) lanterns++;
                }
            }
            eq(h, DragonPalace.CHESTS.length, chests, sea + "'s treasury chests");
            eq(h, sea == Sea.EAST ? DragonPalace.PILLAR_HIGH - DragonPalace.PILLAR_LOW + 1 : 0, pillar, sea + "'s pillar");
            check(h, lanterns >= 40, sea + "'s palace is lit: " + lanterns);
            check(h, DragonPalace.at(sea, 0, 3, 20) == null, "its gate is open");
        }
        h.succeed();
    }

    private static void pillar(GameTestHelper h) {
        for (int y = 2; y <= 4; y++) h.setBlock(new BlockPos(2, y, 3), ModBlocks.SEA_CALMING_PILLAR.get());
    }

    @GameTest(template = PLATFORM)
    public static void theEasternPillarBecomesTheStaffOnce(GameTestHelper h) {
        pillar(h);
        ServerPlayer wukong = CharTest.as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        BlockPos at = h.absolutePos(new BlockPos(2, 3, 3));
        check(h, SeaCalmingPillarBlock.canLift(wukong), "the Monkey King can lift it");
        check(h, SeaCalmingPillarBlock.take(h.getLevel(), at, wukong), "he takes it");
        eq(h, 1, wukong.getInventory().countItem(ModItems.RUYI_JINGU_BANG.get()), "it is his staff");
        for (int y = 2; y <= 4; y++) check(h, !h.getBlockState(new BlockPos(2, y, 3)).is(ModBlocks.SEA_CALMING_PILLAR.get()), "the whole pillar is gone");
        check(h, !SeaCalmingPillarBlock.take(h.getLevel(), at, wukong), "and there is no second");
        eq(h, 1, wukong.getInventory().countItem(ModItems.RUYI_JINGU_BANG.get()), "one staff, once");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thePillarDoesNotStirForTheWeak(GameTestHelper h) {
        pillar(h);
        ServerPlayer weak = CharTest.as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        BlockPos at = h.absolutePos(new BlockPos(2, 3, 3));
        check(h, !SeaCalmingPillarBlock.canLift(weak), "a level-1 character cannot lift it");
        check(h, !SeaCalmingPillarBlock.take(h.getLevel(), at, weak), "it does not stir");
        check(h, h.getBlockState(new BlockPos(2, 2, 3)).is(ModBlocks.SEA_CALMING_PILLAR.get()), "it stands");
        eq(h, 0, weak.getInventory().countItem(ModItems.RUYI_JINGU_BANG.get()), "no staff");
        check(h, ModBlocks.SEA_CALMING_PILLAR.get().defaultBlockState().getDestroySpeed(h.getLevel(), at) < 0, "and nothing breaks it");
        h.succeed();
    }

    // ── Breathing ───────────────────────────────────────────────────────

    private static boolean breathes(ServerPlayer p) {
        LivingBreatheEvent e = new LivingBreatheEvent(p, false, 4, 1);
        SeaBreath.onBreathe(e);
        return e.canBreathe();
    }

    @GameTest(template = PLATFORM)
    public static void thePearlLetsItsBearerBreatheUnderTheSea(GameTestHelper h) {
        ServerPlayer p = player(h);
        check(h, !breathes(p), "without it, they drown");
        p.getInventory().add(new ItemStack(ModItems.WATER_REPELLING_PEARL.get()));
        check(h, SeaBreath.protects(p) && breathes(p), "carried anywhere, they breathe");
        check(h, !SeaBreath.grant(p), "out of the water, nothing more");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theRegaliaWornWholeLetItsWearerBreathe(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PHOENIX_PLUME_CAP.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GOLDEN_CHAIN_MAIL.get()));
        check(h, !breathes(p), "two of three are not enough");
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CLOUD_STEPPING_BOOTS.get()));
        check(h, DragonRegaliaItem.wearsFullSet(p) && breathes(p), "all three: they breathe");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thePearlIsCraftedFromTheSeasOwnHeart(GameTestHelper h) {
        var recipe = h.getLevel().getRecipeManager().byKey(JTTWEngine.id("water_repelling_pearl"));
        check(h, recipe.isPresent(), "its recipe");
        check(h, recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(ModItems.WATER_REPELLING_PEARL.get()), "makes the pearl");
        check(h, recipe.get().value().getIngredients().stream().anyMatch(i -> i.test(new ItemStack(net.minecraft.world.item.Items.HEART_OF_THE_SEA))),
                "from a Heart of the Sea");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSeasCreaturesComeOnlyInDeepWater(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos dry = h.absolutePos(new BlockPos(2, 2, 2));
        check(h, !SeaMonster.checkSeaSpawnRules(ModEntities.SHRIMP_SOLDIER.get(), level, net.minecraft.world.entity.MobSpawnType.NATURAL, dry, level.random),
                "not on dry ground");
        h.succeed();
    }
}
