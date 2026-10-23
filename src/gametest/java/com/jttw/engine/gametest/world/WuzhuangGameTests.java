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
import com.jttw.engine.world.wuzhuang.IslandShape;
import com.jttw.engine.world.wuzhuang.MountWuzhuangFeature;
import com.jttw.engine.world.wuzhuang.Temple;
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
 * (gold to knock it down; it sinks into the earth, rests on wool, is caught in a tray, dissolves in
 * water; eaten, it lengthens a soul's life once), the disciples and cranes and their captains, the
 * youths Clear Wind and Bright Moon, Zhenyuan's sleeve (held, struggled out of, cast out of), his
 * trial (he yields, never dies, and rewards), and the karma of it all.
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
        check(h, Temple.FRUIT.size() >= 10 && ripe >= 5, "a dozen fruit, half ripe: " + Temple.FRUIT.size() + ", " + ripe);
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
        Wuzhuang.makeGuest(p);
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
    public static void takingTheFruitWithoutLeaveIsTheft(GameTestHelper h) {
        openAir(h);
        ServerPlayer thief = player(h), guest = player(h);
        Wuzhuang.makeGuest(guest);
        DaoistDiscipleEntity d = mob(h, ModEntities.DAOIST_DISCIPLE.get(), 78, false, new BlockPos(4, 2, 4));
        d.setNoAi(true);
        BlockPos a = fruit(h, new BlockPos(1, 5, 1), GinsengFruitBlock.RIPE);
        GinsengFruitBlock.knock(h.getLevel(), a, h.getLevel().getBlockState(a), thief, new ItemStack(Items.GOLDEN_SWORD));
        eq(h, Morality.STEAL_GINSENG, Morality.karma(thief), "a theft: -40");
        check(h, Wuzhuang.isThief(thief), "the temple's thief now");
        eq(h, thief, d.getTarget(), "its disciples turn on him");
        BlockPos b = fruit(h, new BlockPos(2, 5, 1), GinsengFruitBlock.RIPE);
        GinsengFruitBlock.knock(h.getLevel(), b, h.getLevel().getBlockState(b), guest, new ItemStack(Items.GOLDEN_SWORD));
        eq(h, 0, Morality.karma(guest), "with Zhenyuan's leave it is no theft");
        check(h, !Wuzhuang.isThief(guest), "nor is the guest a thief");
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(a).inflate(4)).forEach(Entity::discard);
        h.succeed();
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

    @GameTest(template = PLATFORM, timeoutTicks = 120, batch = LIVE)
    public static void aCraneSwoopsOnWhatItHuntsAndThrowsIt(GameTestHelper h) {
        openAir(h);
        CloudCraneEntity c = mob(h, ModEntities.CLOUD_CRANE.get(), 80, false, new BlockPos(1, 9, 1));
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        float full = golem.getHealth();
        boolean[] swooped = {false};
        h.runAfterDelay(3, () -> c.setTarget(golem));
        h.onEachTick(() -> swooped[0] |= c.isSwooping());
        h.succeedWhen(() -> {
            check(h, swooped[0], "it swooped");
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
    public static void theYouthsYieldAndGiveWhatItTakesToBringDownTheFruit(GameTestHelper h) {
        ServerPlayer p = leveled(h, 84);
        var wind = immortal(h, ModEntities.CLEAR_WIND.get(), new BlockPos(1, 2, 1), false);
        var moon = immortal(h, ModEntities.BRIGHT_MOON.get(), new BlockPos(4, 2, 4), false);
        for (TempleImmortal y : List.of(wind, moon)) {
            y.setHealth(y.getMaxHealth() * 0.12f);
            for (int i = 0; i < 4 && !y.isYielded(); i++) {
                y.invulnerableTime = 0;
                y.hurt(h.getLevel().damageSources().playerAttack(p), 1e9f);
            }
            check(h, y.isYielded() && y.isAlive() && y.getHealth() > 0, "it yields, and lives");
            check(h, y.getHealth() <= y.getMaxHealth() * TempleImmortal.YIELD_PERCENT / 100f, "at a tenth of its health");
            float h0 = y.getHealth();
            y.invulnerableTime = 0;
            check(h, !y.hurt(h.getLevel().damageSources().playerAttack(p), 100), "no blow after it has yielded");
            eq(h, h0, y.getHealth(), "untouched");
            check(h, !y.showsBossBar(), "its bar gone");
        }
        check(h, has(p, ModItems.GOLDEN_STRIKER.get()), "Clear Wind gives the Golden Striker");
        check(h, has(p, ModItems.SILK_LINED_TRAY.get()), "Bright Moon gives the Silk-Lined Tray");
        long xp = PlayerLevel.getXp(p);
        check(h, xp >= 2 * Zones.MOUNT_WUZHUANG.levelBand().xpForTier(EnemyTier.GENERAL) || PlayerLevel.getLevel(p) > 84, "each trial paid a General's XP: " + xp);
        // rested and risen, the same day: no second reward
        wind.rise();
        check(h, !wind.isYielded() && wind.getHealth() == wind.getMaxHealth(), "risen whole");
        check(h, !wind.mayReward(p), "once a day");
        h.succeed();
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

    // ── Zhenyuan: the trial ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void heIsHardToHarmAndCallsHisDisciples(GameTestHelper h) {
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
        check(h, zh.isAlive() && !zh.isYielded(), "and he stands");
        zh.setHealth(zh.getMaxHealth() * 0.59f);
        zh.tick();
        List<DaoistDiscipleEntity> called = h.getLevel().getEntitiesOfClass(DaoistDiscipleEntity.class, zh.getBoundingBox().inflate(8),
                d -> d.getPersistentData().getBoolean(ZhenyuanEntity.KEY_CALLED));
        eq(h, ZhenyuanEntity.DISCIPLES, called.size(), "his disciples came at 60%");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void zhenyuanYieldsNeverDiesAndGrantsHisLeave(GameTestHelper h) {
        ServerPlayer p = leveled(h, 86);
        ZhenyuanEntity zh = immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(2, 2, 2), false);
        ZhenyuanEntity heldIn = zh;
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        heldIn.sweep(List.of(golem));
        int karma = Morality.karma(p);
        zh.setHealth(zh.getMaxHealth() * 0.105f);
        for (int i = 0; i < 20 && !zh.isYielded(); i++) {
            zh.stagger();
            zh.invulnerableTime = 0;
            zh.hurt(h.getLevel().damageSources().playerAttack(p), 1e9f);
        }
        check(h, zh.isYielded(), "he yields");
        check(h, zh.isAlive() && !zh.isDeadOrDying() && zh.getHealth() > 0, "he does not die");
        check(h, !Sleeve.isHeld(golem), "and lets go of whoever he held");
        check(h, has(p, ModItems.ZHENYUAN_FLYWHISK.get()) && has(p, ModItems.GINSENG_FRUIT.get()), "his flywhisk and a ginseng fruit, the first time");
        check(h, Wuzhuang.isGuest(p), "the temple's guest now");
        eq(h, karma + Morality.PASS_THE_TRIAL, Morality.karma(p), "a good deed, not a killing");
        check(h, PlayerLevel.getXp(p) > 0 || PlayerLevel.getLevel(p) > 86, "a boss's XP");
        zh.invulnerableTime = 0;
        check(h, !zh.hurt(h.getLevel().damageSources().playerAttack(p), 100), "untouchable while he rests");
        check(h, zh.restUntil() > h.getLevel().getGameTime(), "he rests");
        zh.rise();
        check(h, !zh.isYielded() && zh.getHealth() == zh.getMaxHealth() && zh.phase() == 0, "and rises whole, his trial to be had again");
        check(h, !zh.mayReward(p), "though not rewarded twice in a day");
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
        eq(h, Morality.SLAY_TEMPLE_YOUTH, Morality.killDeed(p, immortal(h, ModEntities.BRIGHT_MOON.get(), new BlockPos(4, 2, 4), false)), "a youth, killed outright");
        eq(h, Morality.SLAY_ZHENYUAN, Morality.killDeed(p, immortal(h, ModEntities.ZHENYUAN.get(), new BlockPos(3, 2, 3), false)), "the Great Immortal, killed outright");
        check(h, Morality.PASS_THE_TRIAL > 0 && Morality.STEAL_GINSENG == Morality.STEAL_PEACH, "the trial a good deed; the fruit's theft as the peaches'");
        h.succeed();
    }
}
