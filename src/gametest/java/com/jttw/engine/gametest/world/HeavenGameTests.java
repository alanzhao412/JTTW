package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.character.wukong.WukongCheckpoints.Checkpoint;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.heaven.HeavenFeatures;
import com.jttw.engine.world.heaven.HeavenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** Sun Wukong's road to immortality: Heaven's buildings, peaches, pills and wine, and Hell's Book of Life and Death. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class HeavenGameTests {

    private static void use(GameTestHelper h, ServerPlayer p, BlockPos rel) {
        BlockPos at = h.absolutePos(rel);
        h.getLevel().getBlockState(at).use(h.getLevel(), p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
    }

    @GameTest(template = PLATFORM)
    public static void aJarPoursThreeCupsOfHeavenlyWine(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.WINE_JAR.get());
        for (int i = 0; i < 4; i++) use(h, p, new BlockPos(2, 2, 2));
        eq(h, 3, p.getInventory().countItem(ModItems.HEAVENLY_WINE.get()), "three cups, then dry");
        eq(h, 0, h.getBlockState(new BlockPos(2, 2, 2)).getValue(HeavenBlocks.WineJar.CUPS), "the jar is empty");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongDrinkingTheWineTakesAStepTowardImmortality(GameTestHelper h) {
        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        ItemStack wine = new ItemStack(ModItems.HEAVENLY_WINE.get());
        ItemStack left = wine.getItem().finishUsingItem(wine, h.getLevel(), wukong);
        check(h, WukongCheckpoints.isComplete(wukong, Checkpoint.HEAVENLY_WINE), "his wine checkpoint");
        check(h, left.is(Items.GLASS_BOTTLE), "and an empty bottle");
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ItemStack another = new ItemStack(ModItems.HEAVENLY_WINE.get());
        another.getItem().finishUsingItem(another, h.getLevel(), erlang);
        check(h, !WukongCheckpoints.isComplete(erlang, Checkpoint.HEAVENLY_WINE), "anyone else just reels");
        check(h, erlang.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION), "as he does");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongStrikesHisNameFromTheBook(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.BOOK_OF_LIFE_AND_DEATH.get());
        use(h, erlang, new BlockPos(2, 2, 2));
        check(h, !WukongCheckpoints.isComplete(erlang, Checkpoint.HELLS_BOOK), "only he can");
        use(h, wukong, new BlockPos(2, 2, 2));
        check(h, WukongCheckpoints.isComplete(wukong, Checkpoint.HELLS_BOOK), "his name struck out");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thePeachesArePickedFromTheTrees(GameTestHelper h) {
        player(h);
        h.setBlock(new BlockPos(2, 3, 2), ModBlocks.PEACH_FRUIT.get());
        h.getLevel().destroyBlock(h.absolutePos(new BlockPos(2, 3, 2)), true);
        var peaches = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(2, 3, 2))).inflate(2),
                e -> e.getItem().is(ModItems.IMMORTAL_PEACH.get()));
        eq(h, 1, peaches.size(), "an Immortal Peach");
        peaches.forEach(ItemEntity::discard);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void travellersToHellFindTheBookOnce(GameTestHelper h) {
        player(h);
        BlockPos arrival = h.absolutePos(new BlockPos(2, 2, -2));
        HeavenFeatures.hallOfJudgement(h.getLevel(), arrival);
        check(h, h.getBlockState(new BlockPos(2, 2, 2)).is(ModBlocks.BOOK_OF_LIFE_AND_DEATH.get()), "the book, ahead of where they arrive");
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.BOOK_OF_LIFE_AND_DEATH.get());
        HeavenFeatures.hallOfJudgement(h.getLevel(), arrival.offset(1, 0, 0));
        int books = 0;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 6; dz++) {
            if (h.getLevel().getBlockState(arrival.offset(dx, 0, dz)).is(ModBlocks.BOOK_OF_LIFE_AND_DEATH.get())) books++;
        }
        eq(h, 1, books, "one book, not a second");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void buildingsAreRareGardensRarerAndThePalaceOne(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int islands = 0, buildings = 0, gardens = 0;
        for (int cx = -40; cx < 40; cx++) for (int cz = -40; cz < 40; cz++) {
            var island = CelestialIslandsDensityFunction.island(cx, cz);
            if (island == null) continue;
            islands++;
            int kind = HeavenFeatures.buildingOn(seed, island, cx, cz);
            if (kind > 0) buildings++;
            if (kind == 2) gardens++;
        }
        double share = (double) buildings / islands, gardenShare = (double) gardens / buildings;
        check(h, share > 0.1 && share < 0.3, "a building on some islands, not most: " + share);
        check(h, gardenShare > 0.1 && gardenShare < 0.3, "a garden one building in five or so: " + gardenShare);
        int[] palace = HeavenFeatures.palaceCell(seed);
        var on = CelestialIslandsDensityFunction.island(palace[0], palace[1]);
        check(h, on == null || HeavenFeatures.buildingOn(seed, on, palace[0], palace[1]) == 0, "nothing else on the palace's island");
        eq(h, HeavenFeatures.palace(seed), HeavenFeatures.palace(seed), "and one place for it, always the same");
        h.succeed();
    }

    /** How often the pills turn up in so many of these chests. */
    private static double pillRate(GameTestHelper h, net.minecraft.resources.ResourceLocation table, int rolls) {
        var loot = h.getLevel().getServer().getLootData().getLootTable(table);
        int with = 0;
        for (int i = 0; i < rolls; i++) {
            var items = loot.getRandomItems(new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST));
            if (items.stream().anyMatch(s -> s.is(ModItems.IMMORTAL_PILL.get()))) with++;
        }
        return (double) with / rolls;
    }

    @GameTest(template = PLATFORM)
    public static void pillsAreRareInPavilionsCommonerInThePalace(GameTestHelper h) {
        double pavilion = pillRate(h, HeavenFeatures.PAVILION_CHEST, 2000), palace = pillRate(h, HeavenFeatures.PALACE_CHEST, 2000);
        check(h, pavilion > 0.01 && pavilion < 0.12, "a pavilion's chest, rarely: " + pavilion);
        check(h, palace > pavilion * 3, "the Jade Palace's, far more often: " + palace);
        h.succeed();
    }
}
