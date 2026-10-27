package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.TallOverworld;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.registry.ModWorldgen;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.KunlunFeature;
import com.jttw.engine.world.kunlun.Kunlun;
import com.jttw.engine.world.kunlun.KunlunPalace;
import com.jttw.engine.world.kunlun.KunlunShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import static com.jttw.engine.gametest.GT.*;

/**
 * Mount Kunlun and the taller sky it stands under: the summit's blueprint (the way from the stair's
 * head through the gate, over the Jade Pool's causeway, up the broad steps to the upper court and up
 * the palace's steps to the treasure chest, room to stand, one chest, water only in the pool, every
 * sign on a wall); the treasure's loot (the Banana-Leaf Fan in it once, every time, and no other
 * signature weapon), and the chest as laid holding it; the test world's Overworld under the taller
 * sky (as a new world's is), vanilla's kept for the older; a stretch of the stair as the feature
 * really lays it, walked block by block (half steps at most, room over head, no water, ground under
 * it); an older world's Kunlun pressed under its lower sky; and the Earth God's Luopan, asked
 * sneaking in the world of the living, pointing there for anyone of any level, its needle too.
 * Its place, its massif and its stair's plan: KunlunPlacementTest.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class KunlunGameTests {

    // ── The blueprint ───────────────────────────────────────────────────

    /** Whether one could stand on the blueprint's block at local (x, y, z): solid (the summit's ground where it leaves the ground be). */
    private static boolean solid(int x, int y, int z) {
        BlockState s = KunlunPalace.at(x, y, z);
        return s == null ? y <= 0 : s.blocksMotion();
    }

    /** Whether the blueprint's local (x, y, z) is open to walk through. */
    private static boolean open(int x, int y, int z) {
        BlockState s = KunlunPalace.at(x, y, z);
        if (s == null) return y > 0;
        return !s.blocksMotion() && s.getFluidState().isEmpty();
    }

    private static boolean standable(int x, int y, int z) {
        return solid(x, y - 1, z) && open(x, y, z) && open(x, y + 1, z);
    }

    @GameTest(template = PLATFORM)
    public static void theSummitLeadsFromTheStairsHeadToTheTreasureChest(GameTestHelper h) {
        int top = KunlunPalace.SUMMIT;
        // the stair arrives at the summit's south edge, where the way in begins
        int[] start = {0, 1, top};
        check(h, KunlunPalace.onApproach(0, top) && standable(start[0], start[1], start[2]), "standing at the stair's head");
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> todo = new ArrayDeque<>();
        todo.add(start);
        seen.add(BlockPos.asLong(start[0], start[1], start[2]));
        while (!todo.isEmpty()) {
            int[] c = todo.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -2; dy--) {
                    int x = c[0] + d.getStepX(), y = c[1] + dy, z = c[2] + d.getStepZ();
                    if (Math.abs(x) > top + 1 || Math.abs(z) > top + 2 || y < 1 || y > KunlunPalace.TOP) continue;
                    if (dy == 1 && !open(c[0], c[1] + 2, c[2])) continue; // no room to step up
                    if (!standable(x, y, z)) continue;
                    if (seen.add(BlockPos.asLong(x, y, z))) todo.add(new int[]{x, y, z});
                    break;
                }
            }
        }
        int up = KunlunPalace.UP + 1, floor = KunlunPalace.FLOOR + 1;
        check(h, seen.contains(BlockPos.asLong(0, 1, KunlunPalace.GATE_Z)), "through the gate");
        check(h, seen.contains(BlockPos.asLong(0, 1, KunlunPalace.POOL_HALL_Z)), "over the Jade Pool's causeway, under the Jade Terrace");
        check(h, seen.contains(BlockPos.asLong(0, up, KunlunPalace.UPPER_S - 1)), "up the broad steps to the upper court");
        check(h, seen.contains(BlockPos.asLong(KunlunPalace.TOWER_X - KunlunPalace.TOWER_HALF + 2, up + 1, 0)), "into the bell tower");
        check(h, seen.contains(BlockPos.asLong(-KunlunPalace.SIDE_X, up + 1, (KunlunPalace.SIDE_N + KunlunPalace.SIDE_S) / 2)), "into a side hall");
        check(h, seen.contains(BlockPos.asLong(0, floor, KunlunPalace.PLINTH_S - 1)), "up the palace's steps");
        check(h, seen.contains(BlockPos.asLong(0, floor, KunlunPalace.HALL_S)), "in at its door");
        check(h, seen.contains(BlockPos.asLong(KunlunPalace.CHEST_X, floor, KunlunPalace.CHEST_Z + 2)), "before the treasure chest");
        check(h, seen.contains(BlockPos.asLong(-20, 1, 38)) && seen.contains(BlockPos.asLong(0, 1, -43)), "round the terrace, before and behind");
        check(h, seen.size() > 2500, "much standing room: " + seen.size());
        // the chest: on the dais, facing the door, at hand from the floor before it
        BlockState chest = KunlunPalace.at(KunlunPalace.CHEST_X, KunlunPalace.CHEST_Y, KunlunPalace.CHEST_Z);
        check(h, chest != null && chest.is(Blocks.CHEST) && chest.getValue(ChestBlock.FACING) == Direction.SOUTH, "the chest, facing the door: " + chest);
        check(h, solid(KunlunPalace.CHEST_X, KunlunPalace.CHEST_Y - 1, KunlunPalace.CHEST_Z), "on the dais");
        check(h, open(KunlunPalace.CHEST_X, KunlunPalace.CHEST_Y + 1, KunlunPalace.CHEST_Z), "its lid free to open");
        check(h, KunlunPalace.CHEST_Y - floor <= 1, "within reach of one standing before it");
        int chests = 0;
        for (int x = -top - 1; x <= top + 1; x++) for (int z = -top - 1; z <= top + 2; z++) for (int y = KunlunPalace.BOTTOM; y <= KunlunPalace.TOP; y++) {
            BlockState s = KunlunPalace.at(x, y, z);
            if (s != null && s.is(Blocks.CHEST)) chests++;
            if (s != null && !s.getFluidState().isEmpty()) {
                check(h, KunlunPalace.inPool(x, z), "water only in the pool: " + x + " " + y + " " + z);
            }
        }
        eq(h, 1, chests, "one chest");
        // every sign hangs on a wall
        for (KunlunPalace.Sign s : KunlunPalace.SIGNS) {
            int bx = s.x() - s.facing().getStepX(), bz = s.z() - s.facing().getStepZ();
            check(h, solid(bx, s.y(), bz) && KunlunPalace.at(bx, s.y(), bz) != null, "the sign " + s.lines()[1] + " on a wall");
            BlockState own = KunlunPalace.at(s.x(), s.y(), s.z());
            check(h, own == null || own.isAir(), "the sign " + s.lines()[1] + " in the open");
        }
        h.succeed();
    }

    // ── The treasure ────────────────────────────────────────────────────

    private static int count(List<ItemStack> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static final List<net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ? extends net.minecraft.world.item.Item>> WEAPONS =
            List.of(ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR, ModItems.ERLANG_AXE, ModItems.FIRE_TIP_SPEAR, ModItems.QIANKUN_RING,
                    ModItems.YIN_YANG_SWORD, ModItems.WIND_FIRE_WHEELS, ModItems.HUNTIAN_CLOTH, ModItems.BELL_OF_TAIYI, ModItems.NINE_TOOTH_RAKE,
                    ModItems.YIN_YANG_VASE, ModItems.MIXED_IRON_CUDGEL);

    @GameTest(template = PLATFORM)
    public static void theTreasureHoldsTheBananaLeafFanOnceEveryTime(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(Kunlun.TREASURE);
        check(h, table != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "the table is there");
        for (int i = 0; i < 40; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            List<ItemStack> items = table.getRandomItems(params);
            eq(h, 1, count(items, ModItems.BANANA_LEAF_FAN.get()), "the fan, once, roll " + i);
            check(h, items.size() >= 4 && items.size() <= 6, "and a few gifts with it: " + items);
            for (var w : WEAPONS) eq(h, 0, count(items, w.get()), "no " + w.getId().getPath());
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "kunlun_summit")
    public static void theChestOnTheSummitHoldsTheFan(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = Kunlun.summit(overworld);
        int[] c = Kunlun.centre(overworld.getSeed());
        eq(h, c[0], ground.getX(), "at the region's centre");
        eq(h, KunlunShape.SUMMIT_Y, ground.getY(), "under the taller sky, the summit's own height");
        check(h, ground.getY() + KunlunPalace.TOP < overworld.getMaxBuildHeight(), "the palace's roof under the sky");
        GateSites.hold(h, overworld, ground);
        KunlunPalace.layWhole(overworld, ground);
        BlockPos at = Kunlun.chest(overworld);
        onEnd(h, () -> {
            if (overworld.getBlockEntity(at) instanceof ChestBlockEntity box) box.clearContent();
            overworld.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        });
        check(h, overworld.getBlockState(at).is(Blocks.CHEST), "the chest laid on the summit at " + at.toShortString());
        if (!(overworld.getBlockEntity(at) instanceof ChestBlockEntity box)) {
            h.fail("no chest there");
            return;
        }
        ServerPlayer p = player(h);
        box.unpackLootTable(p);
        int fans = 0, filled = 0;
        for (int i = 0; i < box.getContainerSize(); i++) {
            ItemStack s = box.getItem(i);
            if (s.is(ModItems.BANANA_LEAF_FAN.get())) fans += s.getCount();
            if (!s.isEmpty()) filled++;
        }
        eq(h, 1, fans, "the Banana-Leaf Fan in it, once");
        check(h, filled >= 4, "with its gifts: " + filled);
        h.succeed();
    }

    // ── The taller sky ──────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theTestWorldStandsUnderTheTallerSkyAsANewWorldDoes(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        eq(h, TallOverworld.MIN_Y, overworld.getMinBuildHeight(), "the floor where it was");
        eq(h, TallOverworld.TOP, overworld.getMaxBuildHeight(), "the sky at y 640");
        check(h, overworld.dimensionTypeRegistration().is(TallOverworld.TYPE), "the taller Overworld's type");
        check(h, TallOverworld.isTall(overworld), "tall");
        var types = h.getLevel().getServer().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        DimensionType vanilla = types.getOrThrow(BuiltinDimensionTypes.OVERWORLD), tall = types.getOrThrow(TallOverworld.TYPE);
        eq(h, 384, vanilla.height(), "vanilla's Overworld kept for older worlds");
        check(h, !TallOverworld.isTall(vanilla), "vanilla's is not tall");
        eq(h, 704, tall.height(), "the taller one's height");
        eq(h, 704, tall.logicalHeight(), "and its logical height");
        eq(h, vanilla.minY(), tall.minY(), "the same floor");
        // in all else, vanilla's Overworld
        eq(h, vanilla.effectsLocation(), tall.effectsLocation(), "its sky");
        eq(h, vanilla.ambientLight(), tall.ambientLight(), "its light");
        eq(h, vanilla.natural(), tall.natural(), "natural");
        eq(h, vanilla.bedWorks(), tall.bedWorks(), "beds");
        eq(h, vanilla.hasRaids(), tall.hasRaids(), "raids");
        eq(h, vanilla.monsterSettings().toString(), tall.monsterSettings().toString(), "its monsters");
        // the presets new worlds are made from give the taller one; superflat keeps vanilla's
        var presets = h.getLevel().getServer().registryAccess().registryOrThrow(Registries.WORLD_PRESET);
        for (var key : List.of(WorldPresets.NORMAL, WorldPresets.LARGE_BIOMES, WorldPresets.AMPLIFIED, WorldPresets.SINGLE_BIOME_SURFACE)) {
            WorldPreset preset = presets.getOrThrow(key);
            LevelStem stem = preset.createWorldDimensions().dimensions().get(LevelStem.OVERWORLD);
            check(h, stem.type().is(TallOverworld.TYPE), key.location() + ": the taller Overworld");
            check(h, preset.createWorldDimensions().dimensions().get(LevelStem.NETHER).type().is(BuiltinDimensionTypes.NETHER), key.location() + ": the Nether as it was");
        }
        check(h, presets.getOrThrow(WorldPresets.FLAT).createWorldDimensions().dimensions().get(LevelStem.OVERWORLD).type().is(BuiltinDimensionTypes.OVERWORLD),
                "superflat keeps vanilla's");
        // a block can stand near the new top, under open sky
        BlockPos high = new BlockPos(h.absolutePos(BlockPos.ZERO).getX(), TallOverworld.TOP - 1, h.absolutePos(BlockPos.ZERO).getZ());
        overworld.setBlock(high, Blocks.STONE.defaultBlockState(), 3);
        onEnd(h, () -> overworld.setBlock(high, Blocks.AIR.defaultBlockState(), 3));
        check(h, overworld.getBlockState(high).is(Blocks.STONE), "a block at y 639");
        check(h, overworld.canSeeSky(high.above()) || overworld.isOutsideBuildHeight(high.above()), "the sky over it");
        h.succeed();
    }

    // ── The stair, as it is laid ────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 600, batch = "kunlun_stair")
    public static void theStairAsLaidIsWalkedInHalfStepsWithRoomOverHead(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        int[] c = Kunlun.centre(overworld.getSeed());
        KunlunShape.Path path = KunlunFeature.path(overworld, c[0], c[1]);
        int n = path.size();
        // a stretch of it below the summit, and what stands on it
        int from = Math.max(0, n - 190), to = n - 30;
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        int pad = (int) Math.ceil(KunlunShape.SHOULDER + KunlunShape.BANK) + 2;
        for (int i = from; i <= to; i++) {
            for (int dx = -pad; dx <= pad; dx += pad) for (int dz = -pad; dz <= pad; dz += pad) {
                chunks.add(new ChunkPos((c[0] + (int) Math.floor(path.x[i]) + dx) >> 4, (c[1] + (int) Math.floor(path.z[i]) + dz) >> 4));
            }
        }
        check(h, chunks.size() < 80, "a stretch of some chunks: " + chunks.size());
        for (ChunkPos cp : chunks) overworld.setChunkForced(cp.x, cp.z, true);
        onEnd(h, () -> chunks.forEach(cp -> overworld.setChunkForced(cp.x, cp.z, false)));
        @SuppressWarnings("unchecked")
        Feature<NoneFeatureConfiguration> feature = (Feature<NoneFeatureConfiguration>) ModWorldgen.KUNLUN.get();
        for (ChunkPos cp : chunks) {
            feature.place(new FeaturePlaceContext<>(Optional.empty(), overworld, overworld.getChunkSource().getGenerator(), RandomSource.create(cp.toLong()),
                    new BlockPos(cp.getMinBlockX(), 0, cp.getMinBlockZ()), NoneFeatureConfiguration.INSTANCE));
        }
        // walked along its middle: the ground under each step, its height, room over head
        double prev = Double.NaN, worst = 0;
        int walked = 0;
        for (int i = from; i <= to; i++) {
            int lx = (int) Math.floor(path.x[i]), lz = (int) Math.floor(path.z[i]);
            KunlunShape.Hit hit = path.nearest(lx, lz, KunlunShape.PATH_HALF);
            if (hit == null || KunlunPalace.covers(lx, lz)) continue;
            int want = (int) Math.floor(hit.y());
            BlockPos feet = new BlockPos(c[0] + lx, want + 1, c[1] + lz);
            BlockState on = overworld.getBlockState(feet);
            boolean slab = on.is(Blocks.STONE_BRICK_SLAB);
            double stand = slab ? want + 1.5 : want + 1;
            check(h, overworld.getBlockState(feet.below()).blocksMotion(), "ground under the way at " + i + " " + feet.below().toShortString());
            check(h, slab || !on.blocksMotion(), "nothing on the way at " + i + " " + feet.toShortString() + ": " + on);
            BlockPos head = slab ? feet.above() : feet;
            for (int k = 0; k <= 1; k++) {
                BlockState over = overworld.getBlockState(head.above(k));
                check(h, !over.blocksMotion() && over.getFluidState().isEmpty(), "room over head at " + i + " " + head.above(k).toShortString() + ": " + over);
            }
            check(h, overworld.getFluidState(feet).isEmpty(), "no water on the way at " + i);
            if (!Double.isNaN(prev)) worst = Math.max(worst, Math.abs(stand - prev));
            prev = stand;
            walked++;
        }
        check(h, walked > 150, "walked a good stretch: " + walked);
        check(h, worst <= 0.5, "never more than a half step: " + worst);
        h.succeed();
    }

    // ── An older world ──────────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void anOlderWorldsKunlunIsPressedUnderItsLowerSky(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        long seed = overworld.getSeed();
        int[] c = Kunlun.centre(seed);
        IntBinaryOperator land = KunlunFeature.land(overworld);
        int old = KunlunFeature.summit(seed, c[0], c[1], land, TallOverworld.VANILLA_TOP);
        int tall = KunlunFeature.summit(seed, c[0], c[1], land, TallOverworld.TOP);
        eq(h, TallOverworld.VANILLA_TOP - KunlunShape.UNDER_SKY, old, "in an older world, as high as its sky allows");
        eq(h, KunlunShape.SUMMIT_Y, tall, "in a new one, its own height");
        eq(h, tall, Kunlun.summit(overworld).getY(), "this world's is the taller");
        check(h, old + KunlunPalace.TOP < TallOverworld.VANILLA_TOP, "the palace's roof under the older sky");
        // its stair, laid out under the lower summit: from the land at its foot to the summit's edge, in half steps
        KunlunShape.Path path = KunlunShape.path(seed, c[0], c[1], old, land);
        int n = path.size();
        eq(h, (double) old, path.y[n - 1], "the stair arrives at the lower summit");
        check(h, n > 400, "still a long way up: " + n);
        for (int i = 1; i < n; i++) {
            check(h, Math.abs(path.y[i] - path.y[i - 1]) <= KunlunShape.MAX_CLIMB + 1e-9, "never steeper than its climb at " + i);
            check(h, path.y[i] <= old, "never over the summit");
        }
        // and nothing of the massif over that summit, nor out of the sky
        for (int a = 0; a < 360; a += 10) for (int r = KunlunShape.TOP + 1; r < KunlunShape.REACH; r += 20) {
            int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * r), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * r);
            int natural = land.applyAsInt(c[0] + dx, c[1] + dz);
            double y = KunlunShape.height(seed, c[0] + dx, c[1] + dz, dx, dz, natural, old);
            check(h, y <= old + 2 && y < TallOverworld.VANILLA_TOP, "under the summit at " + dx + " " + dz + ": " + y);
        }
        h.succeed();
    }

    // ── The luopan ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theLuopanAskedSneakingPointsAnyoneToKunlun(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[] k = Kunlun.centre(seed);
        for (int lv : new int[]{1, 30, 60, 100}) {
            var a = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, lv, seed, Set.of(), true);
            eq(h, Zones.KUNLUN, a.zone(), "level " + lv + ", sneaking: Kunlun");
            eq(h, new BlockPos(k[0], 64, k[1]), a.target().pos(), "its needle to the summit");
            String words = a.words().getString();
            check(h, words.contains("Mount Kunlun") && words.contains("Banana-Leaf Fan") && words.contains("west") && words.contains("blocks"),
                    "told where it is: " + words);
            // not sneaking: as ever, the zone for the level, and a word on how to ask
            var plain = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, lv, seed, Set.of(), false);
            check(h, plain.zone() != Zones.KUNLUN, "standing: the zone for the level");
            check(h, plain.words().getString().contains("Sneak as you ask, for the way to Mount Kunlun"), "and how to ask: " + plain.words().getString());
        }
        eq(h, Zones.WHITE_BONE, EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 25, seed, Set.of(), false).zone(), "the bands' guidance as before");
        var there = EarthGod.answer(Level.OVERWORLD, new Vec3(k[0] + 40.5, 200, k[1] + 0.5), 50, seed, Set.of(), true);
        check(h, there.words().getString().contains("on its slopes"), "on the mountain: " + there.words().getString());
        // in Diyu, sneaking, still the way back up; in the Nether, no one
        var diyu = EarthGod.answer(com.jttw.engine.dimension.JTTWDimensions.HELL, Vec3.ZERO, 60, seed, Set.of(), true);
        check(h, diyu.zone() == null && diyu.words().getString().contains("back to the living"), "in Diyu, the way out: " + diyu.words().getString());
        eq(h, null, EarthGod.answer(Level.NETHER, Vec3.ZERO, 60, seed, Set.of(), true).target(), "in the Nether, no Earth God");
        // the luopan itself: used sneaking, and held sneaking
        ServerPlayer p = player(h);
        ItemStack luopan = new ItemStack(ModItems.EARTH_GOD_LUOPAN.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, luopan);
        PlayerLevel.setLevel(p, 40);
        p.setShiftKeyDown(true);
        luopan.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        eq(h, new BlockPos(k[0], 64, k[1]), luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos(), "used sneaking: to Kunlun");
        p.setShiftKeyDown(false);
        p.tickCount = 0;
        luopan.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        check(h, !luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos().equals(new BlockPos(k[0], 64, k[1])), "held standing: the zone for the level");
        p.setShiftKeyDown(true);
        luopan.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        eq(h, new BlockPos(k[0], 64, k[1]), luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos(), "held sneaking: to Kunlun");
        h.succeed();
    }
}
