package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.wuzhuang.CloudCraneEntity;
import com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity;
import com.jttw.engine.entity.wuzhuang.Sleeve;
import com.jttw.engine.entity.wuzhuang.TempleImmortal;
import com.jttw.engine.entity.wuzhuang.TempleYouthEntity;
import com.jttw.engine.entity.wuzhuang.WuzhuangMonster;
import com.jttw.engine.entity.wuzhuang.ZhenyuanEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.item.FlywhiskItem;
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
import com.jttw.engine.world.wuzhuang.CloudStepBlock;
import com.jttw.engine.world.wuzhuang.GinsengFruitBlock;
import com.jttw.engine.world.wuzhuang.GinsengTree;
import com.jttw.engine.world.wuzhuang.IslandShape;
import com.jttw.engine.world.wuzhuang.MountWuzhuangFeature;
import com.jttw.engine.world.wuzhuang.Temple;
import com.jttw.engine.world.wuzhuang.TempleRecord;
import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * Mount Wuzhuang: its band and heights, the cloud stair, the temple's blueprint, the ginseng fruit
 * (all ripe, never ripening or growing back; gold to knock it down; it sinks into the earth, rests
 * on wool, is caught in a tray, dissolves in water; eaten, it lengthens a soul's life once and turns
 * the household against the eater for good), the disciples (finite, never spawning of themselves)
 * and cranes and their captains, the youths Clear Wind and Bright Moon (fought to the death, leaving
 * the striker and the tray), the temple's record (the last disciple's killer marked, Zhenyuan sent
 * after a day wherever they are, once; kept through saving), Zhenyuan's sleeve (held, struggled out
 * of, cast out of), his death and loot, no trial anywhere, and the karma of it all.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class WuzhuangGameTests {

    /** The tests that play out over time, run on their own: no other zone's tests sweep their platforms meanwhile. */
    private static final String LIVE = "wuzhuang_live";

    @net.minecraft.gametest.framework.AfterBatch(batch = LIVE)
    public static void afterTheLiveOnes(ServerLevel level) {
        level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(level.getSharedSpawnPos()).inflate(30_000_000),
                e -> e instanceof WuzhuangMonster || e instanceof TempleImmortal).forEach(Entity::discard);
    }

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-3 + 1e-2, what + ": expected " + expected + " but was " + actual);
    }

    /**
     * Removes, when the test ends, what this test brought and whatever of the mountain's came of it
     * on its own platform (only there: a neighbouring test's creatures are its own).
     */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, h.getBounds().inflate(2),
                    x -> x instanceof WuzhuangMonster || x instanceof TempleImmortal || x instanceof ItemEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends WuzhuangMonster> T mob(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    private static <T extends TempleImmortal> T immortal(GameTestHelper h, EntityType<T> type, BlockPos at, boolean ai) {
        T m = h.spawn(type, at);
        m.setNoAi(!ai);
        sweepAround(h, m);
        return m;
    }

    private static IronGolem dummy(GameTestHelper h, BlockPos at) {
        IronGolem golem = h.spawn(EntityType.IRON_GOLEM, at);
        golem.setNoAi(true);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10_000);
        golem.setHealth(10_000);
        sweepAround(h, golem);
        return golem;
    }

    private static ServerPlayer leveled(GameTestHelper h, int level) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, level);
        PlayerLevel.setXp(p, 0);
        return p;
    }

    private static boolean has(ServerPlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().contains(new ItemStack(item));
    }

    // ── The zone ────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theMountainIsAerialOverItsHostsAndPaysItsBand(GameTestHelper h) {
        Zones.Zone z = Zones.MOUNT_WUZHUANG;
        eq(h, Zones.Vertical.above(192), z.vertical(), "from over the clouds up");
        check(h, Zones.ALL.indexOf(z) > Zones.ALL.indexOf(Zones.FIRE_MOUNTAIN), "after the Flaming Mountains");
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.MOUNT_WUZHUANG_BIOME.location());
        check(h, band != null && band.minLevel() == 78 && band.maxLevel() == 88, "its band is 78 to 88");
        eq(h, 145, band.baseGruntXp(), "its grunt XP, stepped up from the Flaming Mountains' 130");
        eq(h, 972L, band.xpForTier(EnemyTier.GRUNT), "a grunt pays 972 there");
        // a column it covers: its biome only from y 192 up
        ServerLevel level = h.getLevel();
        long seed = level.getSeed();
        int[] at = null;
        for (int x = 0; x < 200_000 && at == null; x += 512) {
            int[] c = z.centre(seed, x, 0);
            if (c != null) at = c;
        }
        check(h, at != null, "a region of it");
        check(h, z.covers(seed, at[0], 192, at[1]) && z.covers(seed, at[0], IslandShape.TERRACE, at[1]), "over 192 it is the mountain's");
        check(h, !z.covers(seed, at[0], 191, at[1]) && !z.covers(seed, at[0], 70, at[1]), "the ground under it is not");
        // where the world places biomes by climate, the biome source agrees, if the host is there
        if (level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource source) {
            var sampler = level.getChunkSource().randomState().sampler();
            var host = source.getNoiseBiome(QuartPos.fromBlock(at[0]), QuartPos.fromBlock(IslandShape.TERRACE), QuartPos.fromBlock(at[1]), sampler);
            var ground = source.getNoiseBiome(QuartPos.fromBlock(at[0]), QuartPos.fromBlock(100), QuartPos.fromBlock(at[1]), sampler);
            check(h, !ground.is(Zones.MOUNT_WUZHUANG_BIOME), "the ground under the island keeps its own biome");
            if (host.is(Zones.MOUNT_WUZHUANG_BIOME)) {
                check(h, !source.getNoiseBiome(QuartPos.fromBlock(at[0]), QuartPos.fromBlock(180), QuartPos.fromBlock(at[1]), sampler)
                        .is(Zones.MOUNT_WUZHUANG_BIOME), "nor under the clouds");
            }
        }
        h.succeed();
    }

    // ── The cloud stair ─────────────────────────────────────────────────

    /** An island whose stair's axis stands at {@code axis} (its centre found so). */
    private static IslandShape islandWithStairAt(long seed, BlockPos axis) {
        IslandShape is = IslandShape.of(seed, axis.getX(), axis.getZ() - 95);
        for (int i = 0; i < 4; i++) {
            int dz = axis.getZ() - is.stairAxis()[1];
            if (dz == 0) break;
            is = IslandShape.of(seed, axis.getX(), is.cz + dz);
        }
        return is;
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void theCloudStairClimbsInHalfStepsAndBearsUpWhoeverWalksIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos axis = h.absolutePos(new BlockPos(2, 0, 2));
        IslandShape is = islandWithStairAt(level.getSeed(), axis);
        eq(h, axis.getZ(), is.stairAxis()[1], "the stair's axis over the platform");
        // its first dozen steps, high over the platform
        List<BlockPos> laid = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int last = 12;
        for (int x = axis.getX() - 9; x <= axis.getX() + 9; x++) for (int z = axis.getZ() - 9; z <= axis.getZ() + 9; z++) {
            int k = is.stairStep(x, z);
            if (k < 0 || k > last) continue;
            MountWuzhuangFeature.stair(level, is, x, z, p, last);
            laid.add(new BlockPos(x, IslandShape.stepY(k), z));
        }
        onEnd(h, () -> laid.forEach(b -> level.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)));
        check(h, !laid.isEmpty(), "steps laid");
        // every step stands where it should, half a block under the one before, and next to it
        for (int k = 0; k < last; k++) {
            boolean found = false, joined = false;
            for (BlockPos b : laid) {
                if (is.stairStep(b.getX(), b.getZ()) != k) continue;
                found = true;
                BlockState s = level.getBlockState(b);
                check(h, s.is(ModBlocks.CLOUD_STEP.get()), "a cloud step at " + b + " for step " + k);
                double top = b.getY() + s.getCollisionShape(level, b).max(net.minecraft.core.Direction.Axis.Y);
                close(h, IslandShape.stepTop(k), top, "step " + k + "'s top");
                for (BlockPos n : laid) {
                    if (is.stairStep(n.getX(), n.getZ()) == k + 1 && Math.abs(n.getX() - b.getX()) <= 1 && Math.abs(n.getZ() - b.getZ()) <= 1) joined = true;
                }
            }
            check(h, found, "step " + k + " is laid");
            check(h, joined, "step " + (k + 1) + " is beside step " + k);
        }
        eq(h, (double) IslandShape.TERRACE + 1, IslandShape.stepTop(0), "the top step is level with the terrace");
        check(h, level.getBlockState(new BlockPos(laid.get(0).getX(), laid.get(0).getY(), laid.get(0).getZ())).getValue(SlabBlock.TYPE) != SlabType.TOP, "never a top slab");
        // the cloud bears up whoever walks it
        ServerPlayer walker = player(h);
        ((CloudStepBlock) ModBlocks.CLOUD_STEP.get()).stepOn(level, laid.get(0), level.getBlockState(laid.get(0)), walker);
        check(h, walker.hasEffect(MobEffects.SLOW_FALLING) && walker.getEffect(MobEffects.SLOW_FALLING).getDuration() >= CloudStepBlock.BORNE - 1,
                "borne up: slow falling");
        h.succeed();
    }

    // ── The temple ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theTemplesBlueprint(GameTestHelper h) {
        check(h, Temple.size() > 8000, "a temple of some size: " + Temple.size());
        eq(h, Blocks.POLISHED_DEEPSLATE, Temple.at(0, 1, 0).getBlock(), "Zhenyuan's hall floor under him");
        check(h, open(Temple.at(0, Temple.ZHENYUAN_Y, 0)) && open(Temple.at(0, Temple.ZHENYUAN_Y + 1, 0)), "room for him to stand");
        check(h, Temple.SIGNS.stream().anyMatch(s -> s.lines()[1].contains("天")), "the altar's 天地");
        check(h, Temple.SIGNS.stream().anyMatch(s -> s.lines()[0].equals("五庄观")), "the gate's plaque");
        check(h, Temple.at(0, 3, 32).isAir() && Temple.at(0, 3, 28).isAir(), "the gate is open");
        check(h, Temple.at(0, 3, -12).isAir(), "the moon gate is open");
        check(h, Temple.isLog(Temple.at(Temple.TREE_X, 1, Temple.TREE_Z)), "the tree's trunk");
        int ripe = 0;
        for (int[] f : Temple.FRUIT) {
            BlockState s = Temple.at(f[0], f[1], f[2]);
            check(h, s != null && Temple.isFruit(s), "a fruit at " + f[0] + "," + f[1] + "," + f[2]);
            check(h, Temple.at(f[0], f[1] + 1, f[2]) != null && Temple.at(f[0], f[1] + 1, f[2]).is(Blocks.JUNGLE_LEAVES), "hanging from leaves");
            check(h, Temple.at(f[0], f[1] - 1, f[2]) == null, "with nothing under it");
            check(h, f[1] >= 4 && f[1] <= 8, "within reach from the ground (with a rod): " + f[1]);
            if (f[3] == GinsengFruitBlock.RIPE) ripe++;
        }
        eq(h, GinsengTree.FRUIT, Temple.FRUIT.size(), "a dozen fruit");
        eq(h, Temple.FRUIT.size(), ripe, "every one ripe as the land is made");
        // the household: twenty-four, all told, each with room to stand and ground under them
        eq(h, 24, Temple.RESIDENTS.size(), "twenty-four of them");
        eq(h, 24, TempleRecord.household(), "the record's household");
        eq(h, 22L, Temple.RESIDENTS.stream().filter(r -> r.who().equals("disciple")).count(), "twenty-two disciples and the two youths");
        java.util.Set<Long> spots = new java.util.HashSet<>();
        for (Temple.Resident r : Temple.RESIDENTS) {
            String at = r.who() + " at " + r.x() + "," + r.y() + "," + r.z();
            check(h, spots.add(BlockPos.asLong(r.x(), r.y(), r.z())), "one to a spot: " + at);
            check(h, open(Temple.at(r.x(), r.y(), r.z())) && open(Temple.at(r.x(), r.y() + 1, r.z())), "room to stand: " + at);
            BlockState under = Temple.at(r.x(), r.y() - 1, r.z());
            check(h, under != null ? under.blocksMotion() : r.y() == 1, "ground under: " + at);
        }
        h.succeed();
    }

    private static boolean open(BlockState s) {
        return s == null || !s.blocksMotion();
    }

    // ── The ginseng fruit ───────────────────────────────────────────────

    private static BlockPos fruit(GameTestHelper h, BlockPos rel, int age) {
        BlockPos at = h.absolutePos(rel);
        h.getLevel().setBlock(at, ModBlocks.HANGING_GINSENG_FRUIT.get().defaultBlockState().setValue(GinsengFruitBlock.AGE, age), Block.UPDATE_ALL);
        return at;
    }

    private static List<ItemEntity> fruitsNear(GameTestHelper h, BlockPos at) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(4),
                e -> e.isAlive() && e.getItem().is(ModItems.GINSENG_FRUIT.get()));
    }

    @GameTest(template = PLATFORM)
    public static void goldKnocksARipeFruitDownWholeAndAnythingElseWithersIt(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        BlockPos a = fruit(h, new BlockPos(1, 5, 1), GinsengFruitBlock.RIPE);
        eq(h, GinsengFruitBlock.Outcome.KNOCKED_DOWN, GinsengFruitBlock.knock(h.getLevel(), a, h.getLevel().getBlockState(a), p,
                new ItemStack(Items.GOLDEN_SWORD)), "a golden sword knocks it down");
        check(h, h.getLevel().getBlockState(a).isAir(), "off the tree");
        List<ItemEntity> falling = fruitsNear(h, a);
        eq(h, 1, falling.size(), "it falls whole");
        check(h, falling.get(0).getPersistentData().getBoolean(Wuzhuang.KEY_KNOCKED) && falling.get(0).hasPickUpDelay(), "falling: no hand can catch it");
        falling.forEach(Entity::discard);
        BlockPos b = fruit(h, new BlockPos(3, 5, 1), GinsengFruitBlock.RIPE);
        eq(h, GinsengFruitBlock.Outcome.KNOCKED_DOWN, GinsengFruitBlock.knock(h.getLevel(), b, h.getLevel().getBlockState(b), p,
                new ItemStack(ModItems.GOLDEN_STRIKER.get())), "so does the Golden Striker");
        fruitsNear(h, b).forEach(Entity::discard);
        BlockPos c = fruit(h, new BlockPos(1, 5, 3), GinsengFruitBlock.RIPE);
        eq(h, GinsengFruitBlock.Outcome.WITHERED, GinsengFruitBlock.knock(h.getLevel(), c, h.getLevel().getBlockState(c), p,
                new ItemStack(Items.IRON_SWORD)), "iron: it withers");
        BlockPos d = fruit(h, new BlockPos(3, 5, 3), GinsengFruitBlock.RIPE);
        eq(h, GinsengFruitBlock.Outcome.WITHERED, GinsengFruitBlock.knock(h.getLevel(), d, h.getLevel().getBlockState(d), p, ItemStack.EMPTY), "a bare hand: it withers");
        BlockPos e = fruit(h, new BlockPos(2, 5, 2), 1);
        eq(h, GinsengFruitBlock.Outcome.UNRIPE_WITHERED, GinsengFruitBlock.knock(h.getLevel(), e, h.getLevel().getBlockState(e), p,
                new ItemStack(Items.GOLDEN_SWORD)), "unripe: it only withers");
        check(h, fruitsNear(h, d).isEmpty(), "nothing else fell whole");
        check(h, GinsengFruitBlock.isGold(new ItemStack(Items.GOLDEN_PICKAXE)) && !GinsengFruitBlock.isGold(new ItemStack(Items.NETHERITE_SWORD)), "gold is gold");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = LIVE)
    public static void aFallenFruitSinksIntoTheEarthButRestsOnWool(GameTestHelper h) {
        h.setBlock(new BlockPos(3, 1, 3), Blocks.WHITE_WOOL);
        ItemEntity onEarth = h.spawnItem(ModItems.GINSENG_FRUIT.get(), new Vec3(1.5, 3.5, 1.5));
        ItemEntity onWool = h.spawnItem(ModItems.GINSENG_FRUIT.get(), new Vec3(3.5, 3.5, 3.5));
        onWool.getPersistentData().putBoolean(Wuzhuang.KEY_KNOCKED, true);
        onWool.setNeverPickUp();
        sweepAround(h, onEarth, onWool);
        h.succeedWhen(() -> {
            check(h, !onEarth.isAlive(), "the fruit sank into the bare earth");
            check(h, onWool.isAlive() && onWool.onGround(), "on the wool it rests");
            check(h, !onWool.getPersistentData().getBoolean(Wuzhuang.KEY_KNOCKED) && !onWool.hasPickUpDelay() || onWool.getPersistentData().isEmpty()
                    || !onWool.getPersistentData().getBoolean(Wuzhuang.KEY_KNOCKED), "and may be picked up");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = LIVE)
    public static void aFallingFruitIsCaughtInATrayHeldUnderIt(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SILK_LINED_TRAY.get()));
        h.getLevel().addNewPlayer(p);
        onEnd(h, () -> p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED));
        ItemEntity falling = h.spawnItem(ModItems.GINSENG_FRUIT.get(), new Vec3(2.5, 6.5, 2.5));
        falling.getPersistentData().putBoolean(Wuzhuang.KEY_KNOCKED, true);
        falling.setNeverPickUp();
        sweepAround(h, falling);
        h.succeedWhen(() -> {
            check(h, !falling.isAlive(), "caught out of the air");
            check(h, has(p, ModItems.GINSENG_FRUIT.get()), "in the tray, and so the inventory");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60, batch = LIVE)
    public static void aFruitDissolvesInWater(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
        ItemEntity fruit = h.spawnItem(ModItems.GINSENG_FRUIT.get(), new Vec3(2.5, 2.3, 2.5));
        sweepAround(h, fruit);
        h.succeedWhen(() -> check(h, !fruit.isAlive(), "dissolved"));
    }

    @GameTest(template = PLATFORM)
    public static void eatingTheFruitLengthensASoulsLifeOnce(GameTestHelper h) {
        ServerPlayer p = leveled(h, 80);
        double before = p.getMaxHealth();
        p.setHealth(1);
        check(h, Wuzhuang.eat(p), "the first fruit");
        close(h, before * (1 + Wuzhuang.LONGEVITY_PERCENT / 100.0), p.getMaxHealth(), "+20% max health");
        close(h, p.getMaxHealth(), p.getHealth(), "made whole");
        check(h, p.hasEffect(MobEffects.REGENERATION) && p.hasEffect(MobEffects.ABSORPTION), "renewed");
        double after = p.getMaxHealth();
        ItemStack another = new ItemStack(ModItems.GINSENG_FRUIT.get());
        another.getItem().finishUsingItem(another, h.getLevel(), p);
        close(h, after, p.getMaxHealth(), "a second fruit adds nothing to it");
        check(h, !Wuzhuang.eat(p), "only the first lengthens life");
        // through death into a new life: re-applied, once
        Wuzhuang.applyLongevity(p);
        close(h, after, p.getMaxHealth(), "re-applied, not doubled");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void takingTheFruitIsTheft(GameTestHelper h) {
        openAir(h);
        ServerPlayer thief = player(h);
        DaoistDiscipleEntity d = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(4, 2, 4));
        d.setNoAi(true);
        BlockPos a = fruit(h, new BlockPos(1, 5, 1), GinsengFruitBlock.RIPE);
        GinsengFruitBlock.knock(h.getLevel(), a, h.getLevel().getBlockState(a), thief, new ItemStack(Items.GOLDEN_SWORD));
        eq(h, Morality.STEAL_GINSENG, Morality.karma(thief), "a theft: -40 (no one has leave: the Master is away)");
        check(h, Wuzhuang.isThief(thief) && Wuzhuang.isTempleEnemy(thief), "the temple's thief now");
        eq(h, thief, d.getTarget(), "its disciples turn on him");
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(a).inflate(4)).forEach(Entity::discard);
        h.succeed();
    }

    // ── The fruit never comes again ─────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aFruitNeverRipensAndNothingGrowsBack(GameTestHelper h) {
        BlockState fresh = ModBlocks.HANGING_GINSENG_FRUIT.get().defaultBlockState();
        eq(h, GinsengFruitBlock.RIPE, fresh.getValue(GinsengFruitBlock.AGE), "a fruit is made ripe");
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7);
        for (int age = 0; age <= GinsengFruitBlock.RIPE; age++) {
            BlockPos at = fruit(h, new BlockPos(1 + age, 5, 2), age);
            BlockState s = h.getLevel().getBlockState(at);
            check(h, !s.isRandomlyTicking(), "no random ticks at age " + age);
            for (int i = 0; i < 2000; i++) s.randomTick(h.getLevel(), at, random);
            eq(h, age, h.getLevel().getBlockState(at).getValue(GinsengFruitBlock.AGE), "still age " + age + " after two thousand ticks");
        }
        // knocked down, the place it hung stays empty: nothing grows back
        ServerPlayer p = player(h);
        BlockPos at = fruit(h, new BlockPos(3, 5, 3), GinsengFruitBlock.RIPE);
        GinsengFruitBlock.knock(h.getLevel(), at, h.getLevel().getBlockState(at), p, new ItemStack(Items.IRON_SWORD));
        for (int i = 0; i < 200; i++) {
            BlockState s = h.getLevel().getBlockState(at);
            if (s.isRandomlyTicking()) s.randomTick(h.getLevel(), at, random);
        }
        check(h, h.getLevel().getBlockState(at).isAir(), "gone for good");
        h.succeed();
    }

    // ── Eating it turns the household ───────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void eatingAFruitMakesOneTheTemplesEnemyForGood(GameTestHelper h) {
        ServerPlayer p = leveled(h, 80);
        check(h, !Wuzhuang.isTempleEnemy(p) && !Wuzhuang.ateFruit(p), "not before");
        ItemStack fruit = new ItemStack(ModItems.GINSENG_FRUIT.get());
        fruit.getItem().finishUsingItem(fruit, h.getLevel(), p);
        check(h, Wuzhuang.ateFruit(p) && Wuzhuang.isTempleEnemy(p), "marked once eaten");
        // through death into a new body, and through being saved and loaded (a relog)
        ServerPlayer reborn = player(h);
        Wuzhuang.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(reborn, p, true));
        check(h, Wuzhuang.isTempleEnemy(reborn), "still, after death");
        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        p.saveWithoutId(saved);
        ServerPlayer again = player(h);
        again.load(saved);
        check(h, Wuzhuang.isTempleEnemy(again), "still, after a relog");
        // long after any thief's mark would have worn off
        p.getPersistentData().remove(Wuzhuang.KEY_THIEF);
        check(h, !Wuzhuang.isThief(p) && Wuzhuang.isTempleEnemy(p), "for good, not for a while");
        h.succeed();
    }

    /** A disciple and a youth, left to themselves, leave a player be; once the player has eaten a fruit, they come for them. */
    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = LIVE)
    public static void theHouseholdTurnsOnWhoeverAteTheFruitAndNotBefore(GameTestHelper h) {
        ServerPlayer p = leveled(h, 80);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.getLevel().addNewPlayer(p);
        onEnd(h, () -> p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED));
        p.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        DaoistDiscipleEntity d = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(0, 2, 0));
        var wind = immortal(h, ModEntities.CLEAR_WIND.get(), new BlockPos(0, 2, 4), true);
        boolean[] ate = {false};
        h.onEachTick(() -> {
            if (!ate[0]) {
                check(h, d.getTarget() == null && wind.getTarget() == null, "they leave one who has not eaten be");
            }
        });
        h.runAfterDelay(60, () -> {
            ItemStack fruit = new ItemStack(ModItems.GINSENG_FRUIT.get());
            fruit.getItem().finishUsingItem(fruit, h.getLevel(), p);
            ate[0] = true;
        });
        h.succeedWhen(() -> {
            check(h, ate[0], "eaten");
            eq(h, p, d.getTarget(), "the disciple comes for the eater");
            eq(h, p, wind.getTarget(), "and so does Clear Wind");
        });
    }

    // ── Disciples and cranes ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theMountainsCreaturesAreOfItsBandWithCaptains(GameTestHelper h) {
        DaoistDiscipleEntity d = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(1, 2, 1));
        eq(h, Zones.MOUNT_WUZHUANG.id(), d.getPersistentData().getString(ZoneMonster.KEY_ZONE), "the mountain's");
        close(h, DaoistDiscipleEntity.HITS * MobScaling.typicalHit(78), d.getMaxHealth(), "six hits of a level-78 character");
        CloudCraneEntity c = mob(h, ModEntities.CLOUD_CRANE.get(), 87, true, new BlockPos(4, 2, 4));
        check(h, c.getDisplayName().getString().equals("Cloud Crane Captain · Lv 87"), "named: " + c.getDisplayName().getString());
        eq(h, ZoneMonster.PURPLE, c.getDisplayName().getStyle().getColor().getValue(), "purple");
        eq(h, EnemyTier.GENERAL.name(), c.getPersistentData().getString(ZoneMonster.KEY_TIER), "a General");
        check(h, c.getBbHeight() > ModEntities.CLOUD_CRANE.get().getHeight(), "a captain is larger");
        for (int i = 0; i < 30; i++) check(h, d.rollCaptainLevel(h.getLevel().random) <= 88 && c.rollCaptainLevel(h.getLevel().random) <= 88, "never past 88");
        check(h, c.isNoGravity() && !c.causeFallDamage(30, 1, h.getLevel().damageSources().fall()), "a crane flies");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theMountainsCreaturesPayItsXp(GameTestHelper h) {
        ServerPlayer p = leveled(h, 80);
        BiomeLevelBand band = Zones.MOUNT_WUZHUANG.levelBand();
        LevelManager.onEntityKilled(new LivingDeathEvent(mob(h, ModEntities.CLOUD_CRANE.get(), 80, false, new BlockPos(1, 2, 1)),
                p.level().damageSources().playerAttack(p)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a crane, a grunt");
        LevelManager.onEntityKilled(new LivingDeathEvent(mob(h, ModEntities.DAOIST_DISCIPLE.get(), 85, true, new BlockPos(4, 2, 4)),
                p.level().damageSources().playerAttack(p)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a captain, a General");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void disciplesNeverSpawnOfThemselves(GameTestHelper h) {
        var biome = h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).get(Zones.MOUNT_WUZHUANG_BIOME);
        check(h, biome != null, "the biome");
        var monsters = biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap();
        check(h, monsters.stream().noneMatch(m -> m.type == ModEntities.DAOIST_DISCIPLE.get()), "no disciple among the biome's spawns");
        check(h, monsters.stream().anyMatch(m -> m.type == ModEntities.CLOUD_CRANE.get()), "the cranes still come");
        for (var category : net.minecraft.world.entity.MobCategory.values()) {
            check(h, biome.getMobSettings().getMobs(category).unwrap().stream().noneMatch(m -> m.type == ModEntities.DAOIST_DISCIPLE.get()), "nor in " + category);
        }
        DaoistDiscipleEntity d = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(2, 2, 2));
        check(h, !d.checkSpawnRules(h.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL), "a natural spawn is refused");
        check(h, !d.checkSpawnRules(h.getLevel(), net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION), "and a chunk's");
        // Zhenyuan is put nowhere as the land is made
        var placed = h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        check(h, !placed.containsKey(JTTWEngine.id("zhenyuan_lair")), "no lair for Zhenyuan");
        check(h, biome.getGenerationSettings().features().stream().flatMap(set -> set.stream())
                .noneMatch(f -> f.unwrapKey().map(k -> k.location().getPath().contains("zhenyuan")).orElse(false)), "nor in the biome's features");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aDiscipleIsRighteousAndBindsWithATalisman(GameTestHelper h) {
        ServerPlayer p = player(h);
        DaoistDiscipleEntity d = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(1, 2, 1));
        d.setNoAi(true);
        check(h, !Wuzhuang.isThief(p), "no thief, no quarrel");
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        check(h, d.bind(golem), "the talisman flies");
        check(h, golem.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null && golem.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() >= 6
                && golem.hasEffect(MobEffects.WEAKNESS), "bound fast, and weakened");
        check(h, d.getMainHandItem().is(Items.IRON_SWORD) || d.getMainHandItem().isEmpty(), "a sword at hand");
        h.succeed();
    }

    /**
     * Its own batch: a crane hunts players as far as 32 blocks off, and the live batch's
     * neighbours put real players in the world; let one catch its eye first and, when it is
     * given its quarry, its hunt for the player ends and drops the quarry with it.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 120, batch = "wuzhuang_live_crane")
    public static void aCraneSwoopsOnWhatItHuntsAndThrowsIt(GameTestHelper h) {
        openAir(h);
        CloudCraneEntity c = mob(h, ModEntities.CLOUD_CRANE.get(), 80, false, new BlockPos(1, 9, 1));
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        float full = golem.getHealth();
        boolean[] swooped = {false};
        h.runAfterDelay(3, () -> c.setTarget(golem));
        h.onEachTick(() -> swooped[0] |= c.isSwooping());
        h.succeedWhen(() -> {
            check(h, swooped[0], "it swooped (" + (c.getY() - golem.getY()) + " over its quarry, hunting " + c.getTarget() + ")");
            check(h, golem.getHealth() < full, "and struck");
        });
    }

    @GameTest(template = PLATFORM)
    public static void aCranesSwoopStrikesHardAndThrows(GameTestHelper h) {
        CloudCraneEntity c = mob(h, ModEntities.CLOUD_CRANE.get(), 80, false, new BlockPos(1, 2, 1));
        c.setNoAi(true);
        IronGolem golem = dummy(h, new BlockPos(3, 2, 3));
        float full = golem.getHealth();
        check(h, c.swoopHit(golem), "struck");
        close(h, c.getAttributeValue(Attributes.ATTACK_DAMAGE) * CloudCraneEntity.SWOOP_HIT, full - golem.getHealth(), "its beak and a half");
        check(h, Math.hypot(golem.getDeltaMovement().x, golem.getDeltaMovement().z) > 0.3, "thrown");
        h.succeed();
    }

    // ── Clear Wind and Bright Moon ──────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theYouthsAreMiniBossesWhoFightAsAPair(GameTestHelper h) {
        var wind = immortal(h, ModEntities.CLEAR_WIND.get(), new BlockPos(1, 2, 1), false);
        var moon = immortal(h, ModEntities.BRIGHT_MOON.get(), new BlockPos(1, 2, 3), false);
        close(h, TempleYouthEntity.HITS * MobScaling.typicalHit(TempleYouthEntity.LEVEL), wind.getMaxHealth(), "seventy hits of a level-84 character");
        check(h, wind.getDisplayName().getString().endsWith("· Lv 84"), "named: " + wind.getDisplayName().getString());
        eq(h, BossEvent.BossBarColor.GREEN, wind.bossBar().getColor(), "his bar");
        eq(h, moon, wind.partner(), "each other's partner");
        eq(h, wind, moon.partner(), "both ways");
        IronGolem golem = dummy(h, new BlockPos(3, 2, 2));
        float full = golem.getHealth();
        check(h, wind.art(golem), "Clear Wind's gust");
        check(h, golem.getHealth() < full && Math.hypot(golem.getDeltaMovement().x, golem.getDeltaMovement().z) > 1, "stung and hurled away");
        wind.setHealth(wind.getMaxHealth() / 2);
        float half = wind.getHealth();
        check(h, moon.art(golem), "Bright Moon's moonlight");
        check(h, golem.hasEffect(MobEffects.BLINDNESS) && golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "blinded and slowed");
        close(h, half + wind.getMaxHealth() * TempleYouthEntity.BrightMoon.MENDS, wind.getHealth(), "and his partner mended");
        // strike one, and the other comes
        moon.setTarget(null);
        wind.hurt(h.getLevel().damageSources().mobAttack(golem), 5);
        eq(h, golem, moon.getTarget(), "Bright Moon comes to Clear Wind's side");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theYouthsFightToTheDeathAndLeaveWhatItTakesToBringDownTheFruit(GameTestHelper h) {
        ServerPlayer p = leveled(h, 84);
        var wind = immortal(h, ModEntities.CLEAR_WIND.get(), new BlockPos(1, 2, 1), false);
        var moon = immortal(h, ModEntities.BRIGHT_MOON.get(), new BlockPos(4, 2, 4), false);
        eq(h, EnemyTier.GENERAL.name(), wind.getPersistentData().getString(ZoneMonster.KEY_TIER), "each pays as a General");
        for (TempleYouthEntity y : List.of(wind, moon)) {
            y.setHealth(y.getMaxHealth() * 0.03f);
            for (int i = 0; i < 4 && y.isAlive(); i++) {
                y.invulnerableTime = 0;
                y.hurt(h.getLevel().damageSources().playerAttack(p), 1e9f);
            }
            check(h, y.isDeadOrDying(), "struck down at the end: no yielding");
        }
        for (int i = 0; i < 5; i++) {
            check(h, count(loot(h, wind, p), ModItems.GOLDEN_STRIKER.get()) == 1, "Clear Wind leaves the Golden Striker, always");
            check(h, count(loot(h, moon, p), ModItems.SILK_LINED_TRAY.get()) == 1, "Bright Moon leaves the Silk-Lined Tray, always");
        }
        var tray = h.getLevel().getRecipeManager().getRecipes().stream().anyMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(ModItems.SILK_LINED_TRAY.get()));
        check(h, tray, "the tray can still be made");
        h.succeed();
    }

    private static List<ItemStack> loot(GameTestHelper h, net.minecraft.world.entity.Mob mob, ServerPlayer p) {
        var source = h.getLevel().damageSources().playerAttack(p);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, mob)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, mob.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, p)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_ATTACKING_ENTITY, p)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, p)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
        return h.getLevel().getServer().reloadableRegistries().getLootTable(mob.getLootTable()).getRandomItems(params);
    }

    private static int count(List<ItemStack> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    // ── Zhenyuan: the sleeve ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theSleeveHoldsAndTheHeldStruggleFree(GameTestHelper h) {
        ZhenyuanEntity zh = immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(1, 2, 1), false);
        ServerPlayer p = player(h);
        p.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        eq(h, 1, zh.sweep(List.of(p)), "swept into his sleeve");
        check(h, Sleeve.isHeld(p) && Sleeve.holder(p) == zh, "held, by him");
        check(h, p.hasEffect(MobEffects.INVISIBILITY) && p.hasEffect(MobEffects.BLINDNESS), "out of the world: unseen, blind");
        check(h, p.position().distanceTo(Sleeve.sleevePoint(zh)) < 0.5, "at his side, in his sleeve");
        IronGolem golem = dummy(h, new BlockPos(4, 2, 1));
        float full = golem.getHealth();
        golem.invulnerableTime = 0;
        check(h, !golem.hurt(h.getLevel().damageSources().playerAttack(p), 10) && golem.getHealth() == full, "the held strike nothing in the world");
        eq(h, 0, zh.sweep(List.of(p)), "one cannot be swept in twice");
        for (int i = 1; i < Sleeve.STRUGGLES; i++) check(h, !Sleeve.struggle(p), "struggling");
        check(h, Sleeve.struggle(p), "burst free at the fifth blow");
        check(h, !Sleeve.isHeld(p) && !p.hasEffect(MobEffects.INVISIBILITY), "out, and seen");
        check(h, zh.held().isEmpty(), "his sleeve empty");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = LIVE)
    public static void heldToTheEndOneIsCastOutBruised(GameTestHelper h) {
        ZhenyuanEntity zh = immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(1, 2, 1), false);
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        float full = golem.getHealth();
        long[] at = {-1};
        h.runAfterDelay(3, () -> {
            check(h, zh.sweep(List.of(golem)) == 1, "swept in");
            at[0] = h.getLevel().getGameTime();
        });
        h.succeedWhen(() -> {
            check(h, at[0] > 0 && !Sleeve.isHeld(golem), "let out");
            check(h, h.getLevel().getGameTime() - at[0] >= Sleeve.HOLD, "after its time");
            close(h, full * Sleeve.BRUISE, full - golem.getHealth(), "cast out, bruised a tenth (" + golem.getPersistentData() + ", zh alive " + zh.isAlive() + " " + zh.getRemovalReason() + ", last " + golem.getLastDamageSource() + ")");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 260, batch = LIVE)
    public static void heOpensHisSleeveOnWhoeverHeFights(GameTestHelper h) {
        openAir(h);
        ZhenyuanEntity zh = immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(0, 2, 0), true);
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        boolean[] raised = {false};
        h.runAfterDelay(3, () -> zh.setTarget(golem));
        h.onEachTick(() -> {
            raised[0] |= zh.sleeveRaised();
            if (!Sleeve.isHeld(golem)) golem.setHealth(golem.getMaxHealth()); // it stands up to his whisk
            if (zh.getTarget() != golem && !Sleeve.isHeld(golem)) zh.setTarget(golem);
        });
        h.succeedWhen(() -> {
            check(h, raised[0], "his sleeve raised: " + zh.sleeveState());
            check(h, Sleeve.isHeld(golem), "it swept the golem in");
        });
    }

    // ── Zhenyuan: a fight to the death ──────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void heIsHardToHarmAndCallsWhatDisciplesRemain(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        ZhenyuanEntity zh = immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(2, 2, 2), false);
        close(h, ZhenyuanEntity.HITS * MobScaling.typicalHit(ZhenyuanEntity.LEVEL), zh.getMaxHealth(), "160 hits of a level-88 character");
        eq(h, BossEvent.BossBarColor.YELLOW, zh.bossBar().getColor(), "his bar");
        check(h, zh.getDisplayName().getString().endsWith("· Lv 88"), "named: " + zh.getDisplayName().getString());
        zh.stagger();
        float full = zh.getHealth();
        zh.hurt(h.getLevel().damageSources().playerAttack(p), 1e9f);
        check(h, full - zh.getHealth() <= zh.getMaxHealth() * ZhenyuanEntity.HIT_CAP_PERMILLE / 1000.0 + 1e-3, "no blow takes more than 2.5%");
        check(h, zh.isAlive(), "and he stands");
        // at 60% his household near him come to his side; he makes none
        DaoistDiscipleEntity d = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(0, 2, 4));
        d.setNoAi(true);
        int before = h.getLevel().getEntitiesOfClass(DaoistDiscipleEntity.class, zh.getBoundingBox().inflate(16)).size();
        zh.setTarget(p);
        zh.setHealth(zh.getMaxHealth() * 0.59f);
        zh.tick();
        eq(h, before, h.getLevel().getEntitiesOfClass(DaoistDiscipleEntity.class, zh.getBoundingBox().inflate(16)).size(), "no disciple made out of nothing");
        eq(h, p, d.getTarget(), "the one near came to his side");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void zhenyuanFightsToTheDeathAndLeavesHisFlywhisk(GameTestHelper h) {
        ServerPlayer p = leveled(h, 86);
        ZhenyuanEntity zh = immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(2, 2, 2), false);
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        zh.sweep(List.of(golem));
        zh.setHealth(zh.getMaxHealth() * 0.02f);
        for (int i = 0; i < 20 && zh.isAlive(); i++) {
            zh.stagger();
            zh.invulnerableTime = 0;
            zh.hurt(h.getLevel().damageSources().playerAttack(p), 1e9f);
        }
        check(h, zh.isDeadOrDying(), "struck down: he dies, he does not yield");
        check(h, !Sleeve.isHeld(golem), "and lets go of whoever he held");
        for (int i = 0; i < 5; i++) {
            List<ItemStack> items = loot(h, zh, p);
            eq(h, 1, count(items, ModItems.ZHENYUAN_FLYWHISK.get()), "his flywhisk, always: " + items);
            eq(h, 0, count(items, ModItems.GINSENG_FRUIT.get()), "and no fruit: the world has only what its trees bore");
        }
        h.succeed();
    }

    /** Nothing of the trial is left: no yielding, no rest, no reward, no guest, no trial karma. */
    @GameTest(template = PLATFORM)
    public static void noTrialRemains(GameTestHelper h) {
        java.util.Set<String> gone = java.util.Set.of("yieldTrial", "isYielded", "rise", "reward", "mayReward", "passedBy", "restUntil",
                "firstGift", "onPassed", "trialTier", "challengers", "addTrialTargets");
        for (Class<?> c : List.of(TempleImmortal.class, TempleYouthEntity.class, ZhenyuanEntity.class, TempleYouthEntity.ClearWind.class, TempleYouthEntity.BrightMoon.class)) {
            for (var m : c.getDeclaredMethods()) check(h, !gone.contains(m.getName()), c.getSimpleName() + "." + m.getName() + " is the trial's");
        }
        for (String name : List.of("isGuest", "makeGuest")) {
            check(h, java.util.Arrays.stream(Wuzhuang.class.getDeclaredMethods()).noneMatch(m -> m.getName().equals(name)), "no guests: " + name);
        }
        check(h, java.util.Arrays.stream(Morality.class.getDeclaredFields()).noneMatch(f -> f.getName().equals("PASS_THE_TRIAL")), "no trial karma");
        var lang = net.minecraft.locale.Language.getInstance();
        for (String key : List.of("entity.jttw_engine.zhenyuan.yields", "entity.jttw_engine.clear_wind.yields", "entity.jttw_engine.temple_immortal.already")) {
            check(h, !lang.has(key), "no line for it: " + key);
        }
        h.succeed();
    }

    // ── The temple's record: the last disciple, and Zhenyuan's coming ───

    /** A temple far from any real one, for a test's own record. */
    private static long testTemple(GameTestHelper h) {
        long key = TempleRecord.key(29_000_000 - h.getLevel().random.nextInt(1_000_000), 29_000_000 - h.getLevel().random.nextInt(1_000_000));
        onEnd(h, () -> TempleRecord.get(h.getLevel().getServer()).forget(key));
        return key;
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void theLastDisciplesKillerIsSoughtOutADayLaterWhereverTheyAre(GameTestHelper h) {
        openAir(h);
        var server = h.getLevel().getServer();
        TempleRecord record = TempleRecord.get(server);
        long temple = testTemple(h);
        int n = TempleRecord.household();
        eq(h, n, record.living(temple), "the whole household lives");
        // all but one fall, none of it at a player's hand that matters yet
        for (int i = 0; i < n - 1; i++) check(h, !record.fall(temple, java.util.UUID.randomUUID(), null, TempleRecord.now(server)), "not yet the last");
        eq(h, 1, record.living(temple), "one left");
        check(h, record.marked(temple) == null, "no one marked yet");
        // the last, a real disciple of that temple, struck down by a player
        ServerPlayer p = leveled(h, 85);
        DaoistDiscipleEntity last = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(1, 2, 1));
        last.setNoAi(true);
        last.getPersistentData().putLong(TempleRecord.KEY_TEMPLE, temple);
        long killedAt = TempleRecord.now(server);
        for (int i = 0; i < 4 && last.isAlive(); i++) {
            last.invulnerableTime = 0;
            last.hurt(h.getLevel().damageSources().playerAttack(p), 1e9f);
        }
        check(h, last.isDeadOrDying(), "the last disciple falls");
        eq(h, 0, record.living(temple), "none left");
        eq(h, p.getUUID(), record.marked(temple), "his killer is marked");
        eq(h, killedAt + TempleRecord.DAY, record.due(temple), "a full day from the kill");
        // they have gone elsewhere since: he will come to them there
        p.moveTo(h.absoluteVec(new Vec3(3.5, 2, 3.5)));
        java.util.function.Function<java.util.UUID, ServerPlayer> here = u -> u.equals(p.getUUID()) ? p : null;
        long due = record.due(temple);
        check(h, record.sendDue(due - 1, here).isEmpty() && !record.sent(temple), "not a tick before the day is up");
        // not in the world when the time comes: he waits
        check(h, record.sendDue(due + 5, u -> null).isEmpty() && !record.sent(temple), "not while they are away");
        // they come back (log in): he comes
        List<ZhenyuanEntity> sent = record.sendDue(due + 600, here);
        eq(h, 1, sent.size(), "he comes when they are here");
        ZhenyuanEntity zh = sent.get(0);
        onEnd(h, zh::discard);
        check(h, zh.isAlive() && zh.level() == p.level(), "in their world");
        double d = zh.distanceTo(p);
        check(h, d <= ZhenyuanEntity.ARRIVE_FAR + 2, "near them: " + d);
        eq(h, p, zh.getTarget(), "and comes at them");
        eq(h, p.getUUID(), zh.quarry(), "hunting them");
        check(h, !zh.hasRestriction(), "keeping to no lair");
        check(h, record.sent(temple), "sent");
        check(h, record.sendDue(due + 50_000, here).isEmpty(), "only once");
        check(h, !record.fall(temple, java.util.UUID.randomUUID(), p, due + 60_000), "nothing more comes of more deaths");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heArrivesOnFirmGroundNearAnOrdinaryPlace(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        p.moveTo(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        ZhenyuanEntity zh = ZhenyuanEntity.arrive(p);
        check(h, zh != null, "he comes");
        onEnd(h, zh::discard);
        BlockPos at = zh.blockPosition();
        check(h, h.getLevel().getBlockState(at.below()).isFaceSturdy(h.getLevel(), at.below(), net.minecraft.core.Direction.UP), "on firm ground: " + at);
        check(h, h.getLevel().noCollision(zh), "with room for him");
        check(h, zh.distanceTo(p) <= ZhenyuanEntity.ARRIVE_FAR + 2, "near them");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theTemplesRecordIsKeptThroughSavingAndLoading(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        TempleRecord record = new TempleRecord();
        long a = TempleRecord.key(-4096, 8192), b = TempleRecord.key(12_000, -300);
        java.util.UUID killer = java.util.UUID.randomUUID();
        ServerPlayer p = player(h);
        for (int i = 0; i < 5; i++) record.fall(a, java.util.UUID.randomUUID(), null, 100);
        for (int i = 0; i < TempleRecord.household() - 1; i++) record.fall(b, java.util.UUID.randomUUID(), null, 100);
        check(h, record.fall(b, killer, p, 1000), "the last of b's household");
        TempleRecord loaded = TempleRecord.load(record.save(new net.minecraft.nbt.CompoundTag(), registries), registries);
        eq(h, TempleRecord.household() - 5, loaded.living(a), "a's household, still five short");
        eq(h, 0, loaded.living(b), "b's, none");
        eq(h, p.getUUID(), loaded.marked(b), "b's last killer");
        eq(h, 1000L + TempleRecord.DAY, loaded.due(b), "and when he comes");
        check(h, !loaded.sent(b) && loaded.marked(a) == null, "and nothing else");
        // the same one falling twice counts once
        loaded.fall(a, killer, null, 2000);
        loaded.fall(a, killer, null, 2000);
        eq(h, TempleRecord.household() - 6, loaded.living(a), "counted once");
        // and the world keeps it where it keeps its saved data
        TempleRecord world = TempleRecord.get(h.getLevel().getServer());
        check(h, world == TempleRecord.get(h.getLevel().getServer()), "one record to a world");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFlywhiskSweepsAwayWhatStandsBeforeIt(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        p.moveTo(h.absoluteVec(new Vec3(2.5, 2, 0.5)), 0f, 0f); // facing south (+z)
        IronGolem golem = dummy(h, new BlockPos(2, 2, 3));
        check(h, FlywhiskItem.sweep(p) >= 1, "swept");
        check(h, golem.getDeltaMovement().z > 0.5, "away from the whisk: " + golem.getDeltaMovement());
        h.succeed();
    }

    // ── Karma ───────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void karmaForTheMountain(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, Morality.KILL_DISCIPLE, Morality.killDeed(p, mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(1, 2, 1))), "a disciple: a bad deed");
        eq(h, Morality.KILL_DISCIPLE, Morality.killDeed(p, mob(h, ModEntities.DAOIST_DISCIPLE.get(), 85, true, new BlockPos(4, 2, 1))), "even a senior one");
        eq(h, 0, Morality.killDeed(p, mob(h, ModEntities.CLOUD_CRANE.get(), 80, false, new BlockPos(1, 2, 4))), "a crane: no deed");
        eq(h, 0, Morality.killDeed(p, mob(h, ModEntities.CLOUD_CRANE.get(), 87, true, new BlockPos(2, 2, 2))), "nor its captain");
        eq(h, Morality.SLAY_TEMPLE_YOUTH, Morality.killDeed(p, immortal(h, ModEntities.BRIGHT_MOON.get(), new BlockPos(4, 2, 4), false)), "a youth");
        eq(h, Morality.SLAY_ZHENYUAN, Morality.killDeed(p, immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(3, 2, 3), false)), "the Great Immortal");
        eq(h, -12, Morality.KILL_DISCIPLE, "a disciple, -12");
        check(h, Morality.STEAL_GINSENG == Morality.STEAL_PEACH, "the fruit's theft as the peaches'");
        h.succeed();
    }
}
