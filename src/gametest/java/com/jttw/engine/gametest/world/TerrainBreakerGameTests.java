package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.TerrainBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Bulk terrain breaking: no drops, a budget a tick, updates at the edge, vanilla's blasts. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TerrainBreakerGameTests {

    private static List<BlockPos> cube(GameTestHelper h, int x0, int y0, int z0, int size) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) for (int z = 0; z < size; z++) {
            out.add(h.absolutePos(new BlockPos(x0 + x, y0 + y, z0 + z)));
        }
        return out;
    }

    private static List<ItemEntity> items(GameTestHelper h) {
        // This test's own platform: a neighbour's spilled chest is not this test's drop
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(5, 7, 5))).inflate(1), e -> true);
    }

    /**
     * Clears items already lying on this test's platform. The test world is kept between runs, so
     * a run that stops before its drops despawn leaves them for the next run to count.
     */
    private static void clearItems(GameTestHelper h) {
        items(h).forEach(net.minecraft.world.entity.Entity::discard);
    }

    @GameTest(template = PLATFORM, batch = "terrainBreaker")
    public static void aVolumeIsClearedWithNoDrops(GameTestHelper h) {
        clearItems(h);
        List<BlockPos> cube = cube(h, 1, 2, 1, 3);
        cube.forEach(p -> h.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState()));
        h.getLevel().setBlockAndUpdate(cube.get(4), Blocks.BEDROCK.defaultBlockState()); // unbreakable
        TerrainBreaker.breakBlocks(h.getLevel(), cube);
        h.succeedWhen(() -> {
            for (BlockPos p : cube) {
                boolean bedrock = p.equals(cube.get(4));
                h.assertTrue(bedrock ? h.getLevel().getBlockState(p).is(Blocks.BEDROCK) : h.getLevel().getBlockState(p).isAir(),
                        bedrock ? "bedrock stays" : "cleared");
            }
            h.assertTrue(items(h).isEmpty(), "nothing dropped: " + items(h).size());
        });
    }

    @GameTest(template = PLATFORM, batch = "terrainBreaker")
    public static void aChestSpillsWhatIsInItButDropsNothingOfItsOwn(GameTestHelper h) {
        clearItems(h);
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(at, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) h.getLevel().getBlockEntity(at)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        TerrainBreaker.breakBlocks(h.getLevel(), List.of(at));
        h.succeedWhen(() -> {
            h.assertTrue(h.getLevel().getBlockState(at).isAir(), "chest gone");
            List<ItemEntity> dropped = items(h);
            h.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().is(Items.DIAMOND),
                    "only what was stored: " + dropped.stream().map(i -> i.getItem().toString()).toList());
        });
    }

    @GameTest(template = PLATFORM, batch = "terrainBreakerBudget")
    public static void noMoreThanTheBudgetBreaksAtOnce(GameTestHelper h) {
        List<BlockPos> cube = cube(h, 1, 2, 1, 3);
        cube.forEach(p -> h.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState()));
        TerrainBreaker.breakBlocks(h.getLevel(), cube);
        eq(h, 10, TerrainBreaker.work(h.getLevel(), 10), "ten, of 27");
        eq(h, 17, TerrainBreaker.pending(h.getLevel()), "the rest wait");
        h.succeedWhen(() -> h.assertTrue(TerrainBreaker.pending(h.getLevel()) == 0, "then finish"));
    }

    @GameTest(template = PLATFORM, batch = "terrainBreaker")
    public static void sandOnTheEdgeStillFalls(GameTestHelper h) {
        BlockPos under = h.absolutePos(new BlockPos(2, 2, 2));
        BlockPos sand = under.above();
        h.getLevel().setBlockAndUpdate(under, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        TerrainBreaker.breakBlocks(h.getLevel(), List.of(under));
        h.succeedWhen(() -> h.assertTrue(!h.getLevel().getBlockState(sand).is(Blocks.SAND)
                || !h.getLevel().getEntitiesOfClass(FallingBlockEntity.class, new AABB(sand).inflate(2)).isEmpty(),
                "the sand, told its support went, falls"));
    }

    @GameTest(template = PLATFORM, batch = "terrainBreakerPlants")
    public static void whatCanNoLongerStandGoesTooWithoutDropping(GameTestHelper h) {
        clearItems(h);
        BlockPos soil = h.absolutePos(new BlockPos(2, 2, 2));
        BlockPos flower = soil.above();
        BlockPos soil2 = h.absolutePos(new BlockPos(3, 2, 2));
        BlockPos sapling = soil2.above();
        h.getLevel().setBlockAndUpdate(soil, Blocks.DIRT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(flower, Blocks.POPPY.defaultBlockState());
        h.getLevel().setBlockAndUpdate(soil2, Blocks.DIRT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(sapling, Blocks.OAK_SAPLING.defaultBlockState());
        TerrainBreaker.breakBlocks(h.getLevel(), List.of(soil, soil2));
        h.succeedWhen(() -> {
            h.assertTrue(h.getLevel().getBlockState(flower).isAir(), "the flower goes with its dirt");
            h.assertTrue(h.getLevel().getBlockState(sapling).isAir(), "and the sapling");
            h.assertTrue(items(h).isEmpty(), "dropping nothing: "
                    + items(h).stream().map(i -> i.getItem().toString()).toList());
        });
    }

    @GameTest(template = PLATFORM, batch = "terrainBreakerKelp")
    public static void kelpLosingItsFootGoesBeforeVanillaCanDropIt(GameTestHelper h) {
        clearItems(h);
        BlockPos foot = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(foot, Blocks.KELP_PLANT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(foot.above(), Blocks.KELP_PLANT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(foot.above(2), Blocks.KELP.defaultBlockState());
        h.getLevel().setBlockAndUpdate(foot.above(3), Blocks.WATER.defaultBlockState());
        TerrainBreaker.breakBlocks(h.getLevel(), List.of(foot));
        h.runAfterDelay(10, () -> {
            h.assertTrue(h.getLevel().getBlockState(foot.above()).getFluidState().isEmpty()
                    || !h.getLevel().getBlockState(foot.above()).is(Blocks.KELP_PLANT), "the kelp above went too");
            h.assertTrue(items(h).isEmpty(), "and dropped nothing: "
                    + items(h).stream().map(i -> i.getItem().toString()).toList());
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "terrainBreakerBlast")
    public static void aBlastHurtsNowAndCarvesWithoutDrops(GameTestHelper h) {
        clearItems(h);
        var golem = mob(h, EntityType.IRON_GOLEM, 3, 2); // lives through it, so drops no loot of its own
        List<Hit> log = watch(golem);
        BlockPos floor = h.absolutePos(new BlockPos(2, 1, 2));
        Vec3 at = Vec3.atCenterOf(floor.above());
        TerrainBreaker.explode(h.getLevel(), null, at.x, at.y, at.z, 3f);
        check(h, !of(log, DamageTypes.EXPLOSION).isEmpty(), "the blast hurts at once");
        check(h, TerrainBreaker.pending(h.getLevel()) > 0, "the crater is carved over the next ticks");
        h.succeedWhen(() -> {
            h.assertTrue(TerrainBreaker.pending(h.getLevel()) == 0, "carved");
            h.assertTrue(h.getLevel().getBlockState(floor).isAir(), "the floor under it is gone");
            h.assertTrue(items(h).isEmpty(), "nothing dropped: " + items(h).size());
        });
    }

    @GameTest(template = PLATFORM, batch = "terrainBreakerTnt")
    public static void tntInABlastStillGoesOff(GameTestHelper h) {
        BlockPos tnt = h.absolutePos(new BlockPos(3, 2, 2));
        h.getLevel().setBlockAndUpdate(tnt, Blocks.TNT.defaultBlockState());
        Vec3 at = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2)));
        TerrainBreaker.explode(h.getLevel(), null, at.x, at.y, at.z, 3f);
        h.succeedWhen(() -> {
            List<PrimedTnt> primed = h.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(tnt).inflate(3));
            h.assertTrue(!primed.isEmpty(), "primed");
            primed.forEach(PrimedTnt::discard); // not here, thank you
        });
    }

    @GameTest(template = PLATFORM, batch = "terrainBreaker")
    public static void whatComesAfterWaitsForTheBreaking(GameTestHelper h) {
        List<BlockPos> cube = cube(h, 1, 2, 1, 3);
        cube.forEach(p -> h.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState()));
        TerrainBreaker.breakBlocks(h.getLevel(), cube);
        AtomicBoolean ranWithAllGone = new AtomicBoolean();
        AtomicBoolean ran = new AtomicBoolean();
        TerrainBreaker.afterQueued(h.getLevel(), () -> {
            ran.set(true);
            ranWithAllGone.set(cube.stream().allMatch(p -> h.getLevel().getBlockState(p).isAir()));
        });
        h.succeedWhen(() -> {
            h.assertTrue(ran.get(), "ran");
            h.assertTrue(ranWithAllGone.get(), "after the blocks were gone");
        });
    }
}
