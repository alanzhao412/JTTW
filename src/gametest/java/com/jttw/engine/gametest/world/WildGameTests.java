package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.wild.GoblinEntity;
import com.jttw.engine.entity.wild.KoboldEntity;
import com.jttw.engine.entity.wild.OgreEntity;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.entity.wild.WolfmanEntity;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.registry.ModVillages;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * The open world's yaoguai (goblins, kobolds, wolfmen, ogres): each comes with its stats at its
 * level, fights, and leaves its loot; each one's own ways (a goblin losing its nerve, a kobold's
 * alarm, a wolfman's howl calling its pack, an ogre's slam); where they come (their biomes' spawn
 * lists); and what their kills pay (as vanilla mobs do below level 20, nothing from 20).
 *
 * <p>The tests that let them loose (fighting, running) each run in a batch of their own: they call their
 * kind and go after whatever is near. The rest, whose creatures are held still or gone at once, share
 * one batch ({@code wild}): the suite's batches are grouped in a hash map, and a few more batches
 * past its next resize would reorder every other suite's.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class WildGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-3 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    /** Removes, when the test ends, the yaoguai, golems and drops within this test's bounds (those it called up too). */
    private static void sweep(GameTestHelper h) {
        AABB bounds = h.getBounds().inflate(2);
        onEnd(h, () -> h.getLevel().getEntities((Entity) null, bounds,
                x -> x instanceof WildYaoguai || x instanceof IronGolem || x instanceof ItemEntity
                        || x instanceof net.minecraft.world.entity.ExperienceOrb).forEach(Entity::discard));
    }

    /** One of them, removed when the test ends wherever it has got to (one that ran or was flung off would outlive its test). */
    private static <T extends WildYaoguai> T wild(GameTestHelper h, EntityType<T> type, BlockPos at) {
        T m = h.spawn(type, at);
        sweep(h);
        onEnd(h, m::discard);
        return m;
    }

    /** A still iron golem with health enough for any of their blows (and few enough that a float measures one exactly): a foe to strike. */
    private static IronGolem golem(GameTestHelper h, BlockPos at) {
        IronGolem g = h.spawn(EntityType.IRON_GOLEM, at);
        g.goalSelector.removeAllGoals(x -> true);
        g.targetSelector.removeAllGoals(x -> true);
        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        g.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
        g.setHealth(g.getMaxHealth());
        sweep(h);
        onEnd(h, g::discard);
        return g;
    }

    // ── Stats ───────────────────────────────────────────────────────────

    private static void stats(GameTestHelper h, EntityType<? extends WildYaoguai> type, double hits, double blow, int min, int max, int share) {
        WildYaoguai m = wild(h, type, new BlockPos(2, 2, 2));
        eq(h, min, m.minLevel(), "its lowest level");
        eq(h, max, m.maxLevel(), "its highest level");
        eq(h, min, m.mobLevel(), "it comes at its lowest level unless rolled");
        close(h, hits * MobScaling.typicalHit(min), m.getMaxHealth(), "health: so many ordinary hits at its level");
        close(h, blow * MobScaling.typicalHealth(min), m.getAttributeValue(Attributes.ATTACK_DAMAGE), "its blow at its level");
        m.become(max);
        eq(h, max, m.mobLevel(), "levelled");
        close(h, hits * MobScaling.typicalHit(max), m.getMaxHealth(), "health at its highest level");
        close(h, m.getMaxHealth(), m.getHealth(), "healed to its new health");
        check(h, m.getMaxHealth() > hits * MobScaling.typicalHit(min), "stronger at a higher level");
        for (int i = 0; i < 40; i++) {
            int l = m.rollLevel(m.getRandom());
            check(h, l >= min && l <= max, "a rolled level in its range: " + l);
        }
        check(h, m.getDisplayName().getString().contains("Lv " + max), "its level shown with its name: " + m.getDisplayName().getString());
        eq(h, share, m.vanillaXpShare(), "what it pays below level 20, in vanilla mobs");
        eq(h, MobCategory.MONSTER, type.getCategory(), "a monster");
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void aGoblinHasItsStats(GameTestHelper h) {
        stats(h, ModEntities.GOBLIN.get(), GoblinEntity.HITS, GoblinEntity.BLOW, 1, 6, 1);
        h.succeed();
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void aKoboldHasItsStats(GameTestHelper h) {
        stats(h, ModEntities.KOBOLD.get(), KoboldEntity.HITS, KoboldEntity.BLOW, 3, 10, 1);
        h.succeed();
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void aWolfmanHasItsStats(GameTestHelper h) {
        stats(h, ModEntities.WOLFMAN.get(), WolfmanEntity.HITS, WolfmanEntity.BLOW, 10, 17, 2);
        h.succeed();
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void anOgreHasItsStats(GameTestHelper h) {
        stats(h, ModEntities.OGRE.get(), OgreEntity.HITS, OgreEntity.BLOW, 15, 20, 5);
        OgreEntity o = wild(h, ModEntities.OGRE.get(), new BlockPos(3, 2, 3));
        check(h, o.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.9, "hardly moved");
        check(h, o.getAttributeValue(Attributes.ATTACK_KNOCKBACK) > 1, "its blows throw a foe back");
        check(h, o.getAttributeValue(Attributes.MOVEMENT_SPEED) < GoblinEntity.SPEED, "slower than a goblin");
        h.succeed();
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void finalizedOnesComeAtALevelInTheirRange(GameTestHelper h) {
        for (EntityType<? extends WildYaoguai> type : List.of(ModEntities.GOBLIN.get(), ModEntities.KOBOLD.get(),
                ModEntities.WOLFMAN.get(), ModEntities.OGRE.get())) {
            WildYaoguai m = type.create(h.getLevel());
            BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            m.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(at), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
            check(h, m.mobLevel() >= m.minLevel() && m.mobLevel() <= m.maxLevel(), type + ": level " + m.mobLevel());
            close(h, m.healthAt(m.mobLevel()), m.getMaxHealth(), type + ": health at its rolled level");
            close(h, m.getMaxHealth(), m.getHealth(), type + ": at full health");
            m.discard();
        }
        h.succeed();
    }

    // ── Fighting ────────────────────────────────────────────────────────

    /** Set on a golem, it goes for it and wounds it. */
    private static void fights(GameTestHelper h, WildYaoguai m) {
        IronGolem g = golem(h, new BlockPos(1, 2, 1));
        float full = g.getHealth();
        m.setTarget(g);
        h.succeedWhen(() -> check(h, g.getHealth() < full, "it struck its foe: " + g.getHealth() + " of " + full + " (its foe: " + m.getTarget() + ")"));
    }

    @GameTest(batch = "wild_fight_goblin", template = PLATFORM, timeoutTicks = 200)
    public static void aGoblinFights(GameTestHelper h) {
        GoblinEntity m = wild(h, ModEntities.GOBLIN.get(), new BlockPos(3, 2, 3));
        // its gang about it, so it keeps its nerve
        for (BlockPos at : List.of(new BlockPos(4, 2, 4), new BlockPos(4, 2, 2))) wild(h, ModEntities.GOBLIN.get(), at).setNoAi(true);
        fights(h, m);
    }

    @GameTest(batch = "wild_fight_kobold", template = PLATFORM, timeoutTicks = 200)
    public static void aKoboldFights(GameTestHelper h) {
        fights(h, wild(h, ModEntities.KOBOLD.get(), new BlockPos(3, 2, 3)));
    }

    @GameTest(batch = "wild_fight_wolfman", template = PLATFORM, timeoutTicks = 240)
    public static void aWolfmanFights(GameTestHelper h) {
        fights(h, wild(h, ModEntities.WOLFMAN.get(), new BlockPos(3, 2, 3)));
    }

    @GameTest(batch = "wild_fight_ogre", template = PLATFORM, timeoutTicks = 240)
    public static void anOgreFights(GameTestHelper h) {
        fights(h, wild(h, ModEntities.OGRE.get(), new BlockPos(3, 2, 3)));
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void eachOnesBlowHurtsByItsLevel(GameTestHelper h) {
        for (EntityType<? extends WildYaoguai> type : List.of(ModEntities.GOBLIN.get(), ModEntities.KOBOLD.get(),
                ModEntities.WOLFMAN.get(), ModEntities.OGRE.get())) {
            WildYaoguai m = wild(h, type, new BlockPos(3, 2, 3));
            m.setNoAi(true);
            IronGolem g = golem(h, new BlockPos(2, 2, 2));
            g.getAttribute(Attributes.ARMOR).setBaseValue(0);
            float before = g.getHealth();
            check(h, m.doHurtTarget(g), type + ": its blow lands");
            close(h, m.blowAt(m.mobLevel()), before - g.getHealth(), type + ": its blow's hurt");
            m.discard();
            g.discard();
        }
        h.succeed();
    }

    // ── Their own ways ──────────────────────────────────────────────────

    @GameTest(batch = "wild_goblin_nerve", template = PLATFORM, timeoutTicks = 120)
    public static void aGoblinLosesItsNerveAloneOrHurt(GameTestHelper h) {
        GoblinEntity m = wild(h, ModEntities.GOBLIN.get(), new BlockPos(2, 2, 2));
        check(h, m.afraid(), "alone, it is afraid");
        GoblinEntity a = wild(h, ModEntities.GOBLIN.get(), new BlockPos(3, 2, 3));
        GoblinEntity b = wild(h, ModEntities.GOBLIN.get(), new BlockPos(1, 2, 3));
        a.setNoAi(true);
        b.setNoAi(true);
        check(h, !m.afraid(), "with its gang about it, it keeps its nerve");
        m.setHealth(m.getMaxHealth() * 0.4f);
        check(h, m.afraid(), "hurt below half, it loses it");
        IronGolem g = golem(h, new BlockPos(3, 2, 1));
        m.setTarget(g);
        h.succeedWhen(() -> check(h, m.isFleeing(), "afraid of its foe, it runs"));
    }

    @GameTest(batch = "wild_alarm", template = PLATFORM)
    public static void koboldsRaiseTheAlarm(GameTestHelper h) {
        KoboldEntity k = wild(h, ModEntities.KOBOLD.get(), new BlockPos(1, 2, 1));
        KoboldEntity a = wild(h, ModEntities.KOBOLD.get(), new BlockPos(3, 2, 3));
        KoboldEntity b = wild(h, ModEntities.KOBOLD.get(), new BlockPos(4, 2, 1));
        for (KoboldEntity x : List.of(k, a, b)) x.setNoAi(true);
        IronGolem g = golem(h, new BlockPos(2, 2, 4));
        eq(h, 2, k.raiseAlarm(g), "both others answer");
        check(h, a.getTarget() == g && b.getTarget() == g, "on its foe");
        eq(h, 0, k.raiseAlarm(g), "one with a foe already keeps its own");
        h.succeed();
    }

    @GameTest(batch = "wild_howl", template = PLATFORM)
    public static void aWolfmansHowlCallsItsPack(GameTestHelper h) {
        WolfmanEntity w = wild(h, ModEntities.WOLFMAN.get(), new BlockPos(1, 2, 1));
        WolfmanEntity a = wild(h, ModEntities.WOLFMAN.get(), new BlockPos(3, 2, 3));
        WolfmanEntity b = wild(h, ModEntities.WOLFMAN.get(), new BlockPos(4, 2, 1));
        for (WolfmanEntity x : List.of(w, a, b)) x.setNoAi(true);
        IronGolem g = golem(h, new BlockPos(2, 2, 4));
        eq(h, 0, w.howling(), "not howling");
        eq(h, 2, w.howl(g).size(), "its pack answers");
        check(h, a.getTarget() == g && b.getTarget() == g, "and takes up the hunt");
        eq(h, WolfmanEntity.HOWL_TICKS, w.howling(), "it throws its head back");
        eq(h, 0, w.howl(null).size(), "a howl at the moon calls no one onto anything");
        h.succeed();
    }

    @GameTest(batch = "wild_slam", template = PLATFORM, timeoutTicks = 60)
    public static void anOgresSlamStrikesAllRound(GameTestHelper h) {
        openAir(h);
        OgreEntity o = wild(h, ModEntities.OGRE.get(), new BlockPos(0, 2, 0));
        o.setNoAi(true);
        IronGolem near = golem(h, new BlockPos(2, 2, 1));
        IronGolem far = golem(h, new BlockPos(4, 2, 4));
        OgreEntity kin = wild(h, ModEntities.OGRE.get(), new BlockPos(0, 2, 2));
        kin.setNoAi(true);
        float nearFull = near.getHealth(), farFull = far.getHealth(), kinFull = kin.getHealth();
        List<LivingEntity> struck = o.slam();
        check(h, struck.contains(near), "one beside it is struck");
        close(h, o.slamDamage(), nearFull - near.getHealth(), "by its slam's hurt");
        close(h, o.blowAt(o.mobLevel()) * OgreEntity.SLAM_PERCENT / 100.0, o.slamDamage(), "three quarters of its blow");
        check(h, near.getDeltaMovement().y > 0.3, "and flung up: " + near.getDeltaMovement());
        check(h, far.getHealth() == farFull && !struck.contains(far), "one beyond its reach is not");
        check(h, kin.getHealth() == kinFull, "nor another ogre");
        h.succeed();
    }

    // ── Loot ────────────────────────────────────────────────────────────

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

    private static int count(List<ItemStack> items, Item... kinds) {
        int n = 0;
        for (ItemStack s : items) for (Item k : kinds) if (s.is(k)) n += s.getCount();
        return n;
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void eachLeavesItsLoot(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        p.setGameMode(GameType.SURVIVAL);
        GoblinEntity goblin = wild(h, ModEntities.GOBLIN.get(), new BlockPos(1, 2, 1));
        KoboldEntity kobold = wild(h, ModEntities.KOBOLD.get(), new BlockPos(3, 2, 1));
        WolfmanEntity wolfman = wild(h, ModEntities.WOLFMAN.get(), new BlockPos(1, 2, 3));
        OgreEntity ogre = wild(h, ModEntities.OGRE.get(), new BlockPos(3, 2, 3));
        for (Mob m : List.of(goblin, kobold, wolfman, ogre)) m.setNoAi(true);
        int strings = 0, silver = 0, gold = 0, rolls = 200;
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> g = loot(h, goblin, p);
            check(h, count(g, ModVillages.COPPER_CASH.get()) >= 1, "a goblin's stolen coins: " + g);
            strings += count(g, ModVillages.STRING_OF_CASH.get());
            List<ItemStack> k = loot(h, kobold, p);
            check(h, count(k, Items.RAW_COPPER, Items.RAW_IRON, Items.COAL) >= 1, "a kobold's ore: " + k);
            gold += count(k, Items.RAW_GOLD);
            List<ItemStack> w = loot(h, wolfman, p);
            int pelts = count(w, ModItems.WOLF_PELT.get());
            check(h, pelts >= 1 && pelts <= 2, "a wolfman's pelt: " + w);
            List<ItemStack> o = loot(h, ogre, p);
            int tusks = count(o, ModItems.OGRE_TUSK.get());
            check(h, tusks >= 1 && tusks <= 2, "an ogre's tusks: " + o);
            check(h, count(o, Items.IRON_INGOT) >= 2, "the iron of its bracers and club: " + o);
            silver += count(o, ModVillages.SILVER_TAEL.get());
        }
        // rare finds come now and then: a goblin's whole string of cash (5%), a kobold's gold (6%), an ogre's silver (15%)
        check(h, strings >= 1 && strings <= 30, "a string of cash now and then: " + strings + " in " + rolls);
        check(h, gold >= 1 && gold <= 50, "raw gold now and then: " + gold + " in " + rolls);
        check(h, silver >= 10 && silver <= 60, "a tael of silver now and then: " + silver + " in " + rolls);
        // and their materials are worth something
        var recipes = h.getLevel().getRecipeManager();
        var pelt = recipes.byKey(JTTWEngine.id("leather_from_wolf_pelt"));
        check(h, pelt.isPresent() && pelt.get().value().getResultItem(h.getLevel().registryAccess()).is(Items.LEATHER), "a pelt is leather");
        var tusk = recipes.byKey(JTTWEngine.id("bone_meal_from_ogre_tusk"));
        check(h, tusk.isPresent() && tusk.get().value().getResultItem(h.getLevel().registryAccess()).is(Items.BONE_MEAL), "a tusk is bone meal");
        h.succeed();
    }

    // ── Where they come ─────────────────────────────────────────────────

    private static boolean spawnsIn(GameTestHelper h, ResourceKey<Biome> biome, EntityType<?> type) {
        Biome b = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(biome);
        return b.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(d -> d.type == type);
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void theyComeInTheirHaunts(GameTestHelper h) {
        check(h, spawnsIn(h, Biomes.PLAINS, ModEntities.GOBLIN.get()), "goblins on the plains");
        check(h, spawnsIn(h, Biomes.FOREST, ModEntities.GOBLIN.get()), "goblins in the forest");
        check(h, spawnsIn(h, Biomes.WINDSWEPT_HILLS, ModEntities.GOBLIN.get()), "goblins in the hills");
        check(h, !spawnsIn(h, Biomes.DESERT, ModEntities.GOBLIN.get()), "no goblins in the desert");
        check(h, spawnsIn(h, Biomes.TAIGA, ModEntities.WOLFMAN.get()), "wolfmen in the taiga");
        check(h, spawnsIn(h, Biomes.DARK_FOREST, ModEntities.WOLFMAN.get()), "wolfmen in the dark forest");
        check(h, !spawnsIn(h, Biomes.PLAINS, ModEntities.WOLFMAN.get()), "no wolfmen on the open plains");
        check(h, spawnsIn(h, Biomes.JAGGED_PEAKS, ModEntities.OGRE.get()), "ogres in the mountains");
        check(h, spawnsIn(h, Biomes.SWAMP, ModEntities.OGRE.get()), "ogres in the swamps");
        check(h, !spawnsIn(h, Biomes.FOREST, ModEntities.OGRE.get()), "no ogres in the forest");
        check(h, spawnsIn(h, Biomes.DRIPSTONE_CAVES, ModEntities.KOBOLD.get()), "kobolds in the dripstone caves");
        check(h, spawnsIn(h, Biomes.PLAINS, ModEntities.KOBOLD.get()), "kobolds under the plains");
        check(h, !spawnsIn(h, Biomes.DEEP_DARK, ModEntities.KOBOLD.get()), "none in the deep dark");
        check(h, !spawnsIn(h, Biomes.OCEAN, ModEntities.KOBOLD.get()), "none under the sea");
        h.succeed();
    }

    // ── What their kills pay ────────────────────────────────────────────

    private static void killedBy(GameTestHelper h, ServerPlayer p, EntityType<? extends LivingEntity> type) {
        LivingEntity victim = h.spawn(type, new BlockPos(3, 2, 3));
        onEnd(h, victim::discard);
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, h.getLevel().damageSources().playerAttack(p)));
        victim.discard();
    }

    @GameTest(batch = "wild", template = PLATFORM)
    public static void belowTwentyTheyPayAsVanillaMobsDoFromTwentyNothing(GameTestHelper h) {
        sweep(h);
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 10);                              // (paced by level: a zombie pays 3/5 of it, 6 at 10)
        long xp = PlayerLevel.getXp(p);
        eq(h, 6L, LevelManager.vanillaMobXp(EntityType.ZOMBIE, 10), "a zombie at 10");
        killedBy(h, p, ModEntities.GOBLIN.get());
        eq(h, xp += 6, PlayerLevel.getXp(p), "a goblin: a zombie's worth");
        killedBy(h, p, ModEntities.KOBOLD.get());
        eq(h, xp += 6, PlayerLevel.getXp(p), "a kobold: one");
        killedBy(h, p, ModEntities.WOLFMAN.get());
        eq(h, xp += 12, PlayerLevel.getXp(p), "a wolfman: two");
        killedBy(h, p, ModEntities.OGRE.get());
        eq(h, xp += 30, PlayerLevel.getXp(p), "an ogre: five");
        PlayerLevel.setLevel(p, 20);
        long at20 = PlayerLevel.getXp(p);
        for (EntityType<? extends WildYaoguai> type : List.of(ModEntities.GOBLIN.get(), ModEntities.KOBOLD.get(),
                ModEntities.WOLFMAN.get(), ModEntities.OGRE.get())) killedBy(h, p, type);
        eq(h, at20, PlayerLevel.getXp(p), "from level 20 they pay nothing (the zones are where a character grows)");
        h.succeed();
    }
}
