package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.tough.Carving;
import com.jttw.engine.world.tough.Tough;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * The mod's tough stone stands up to the weak and gives to the strong; the destruction powers pay
 * for what they break by one measure (Carving), and stop at what they cannot pay for.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ToughGameTests {

    @GameTest(template = PLATFORM)
    public static void carvingCostsByHardnessAndBlastResistance(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(2, 3, 2));
        Level level = h.getLevel();
        near(h, 1.5, Carving.cost(level, at, Blocks.STONE.defaultBlockState(), 1), "stone: its hardness");
        near(h, 0.5, Carving.cost(level, at, Blocks.DIRT.defaultBlockState(), 1), "dirt");
        near(h, 300, Carving.cost(level, at, Blocks.OBSIDIAN.defaultBlockState(), 1), "obsidian: a quarter of its 1200");
        check(h, Double.isInfinite(Carving.cost(level, at, Blocks.BEDROCK.defaultBlockState(), 1e9)), "bedrock: never, however strong");
        near(h, 0, Carving.cost(level, at, Blocks.AIR.defaultBlockState(), 1), "air: nothing");
        var tuff = ModBlocks.GRAVE_TUFF.get().defaultBlockState();
        near(h, 375, Carving.cost(level, at, tuff, 1), "tough stone to the weak: dearer than obsidian");
        check(h, Carving.cost(level, at, tuff, Math.sqrt(47247)) < 2, "and to the mightiest, about as stone");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void toughStoneIsMinedAsItsVanillaKin(GameTestHelper h) {
        check(h, Tough.is(ModBlocks.GRAVE_TUFF.get().defaultBlockState()), "grave tuff is tough");
        check(h, !Tough.is(Blocks.TUFF.defaultBlockState()), "vanilla tuff is not");
        near(h, Blocks.TUFF.defaultBlockState().getDestroySpeed(h.getLevel(), BlockPos.ZERO),
                ModBlocks.GRAVE_TUFF.get().defaultBlockState().getDestroySpeed(h.getLevel(), BlockPos.ZERO), "mined as tuff is");
        h.succeed();
    }

    /** A blast set off by a weak player leaves tough stone; the same blast from the mightiest breaks it. */
    @GameTest(template = PLATFORM)
    public static void toughStoneStandsTheWeakAndGivesToTheStrong(GameTestHelper h) {
        ServerPlayer weak = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        ServerPlayer strong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(strong, 100);
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, ModBlocks.GRAVE_TUFF.get());
        blastBeside(h, weak, at);
        h.assertBlockPresent(ModBlocks.GRAVE_TUFF.get(), at);
        blastBeside(h, strong, at);
        h.assertBlockNotPresent(ModBlocks.GRAVE_TUFF.get(), at);
        h.succeed();
    }

    private static void blastBeside(GameTestHelper h, ServerPlayer by, BlockPos rel) {
        Vec3 c = h.absoluteVec(Vec3.atCenterOf(rel)).add(0, 0.9, 0);
        h.getLevel().explode(by, c.x, c.y, c.z, 4f, Level.ExplosionInteraction.TNT);
    }

    // ── The falling staff pays for what it breaks ──────────────────────

    /** A wall of {@code block}, 5 wide and 8 high, at relative z = 3 across the platform. */
    private static void wall(GameTestHelper h, Block block) {
        wall(h, block, 3);
    }

    /** A wall of {@code block}, 5 wide and 8 high, at relative z = {@code z}. */
    private static void wall(GameTestHelper h, Block block, int z) {
        for (int x = 0; x < 5; x++) for (int y = 2; y < 10; y++) h.setBlock(new BlockPos(x, y, z), block);
    }

    private static PlacedStaffEntity felled(GameTestHelper h, ServerPlayer owner, float scale) {
        var at = h.absolutePos(new BlockPos(2, 2, 1));
        PlacedStaffEntity staff = new PlacedStaffEntity(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, owner.getUUID());
        staff.setStaffScale(scale);
        h.getLevel().addFreshEntity(staff);
        onEnd(h, staff::discard);
        owner.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5, 0f, 0f);
        check(h, staff.toppleBy(owner), "toppled");
        return staff;
    }

    /** How many degrees above the horizontal it lies. */
    private static double elevation(PlacedStaffEntity s) {
        Vec3 d = s.rodTip().subtract(s.rodBase());
        return Math.toDegrees(Math.asin(d.y / d.length()));
    }

    @GameTest(template = PLATFORM, batch = "staff_physics", timeoutTicks = 200)
    public static void aWeakStaffComesToRestLeaningOnObsidian(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        wall(h, Blocks.OBSIDIAN);
        PlacedStaffEntity staff = felled(h, owner, 4f);
        h.runAfterDelay(140, () -> {
            check(h, elevation(staff) > 35, "leaning on the wall at the angle it stopped at, not flat: " + elevation(staff));
            for (int y = 2; y < 10; y++) h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, y, 3));
            check(h, staff.budgetLeft() < 300, "it could not pay for a block: " + staff.budgetLeft());
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "staff_physics", timeoutTicks = 200)
    public static void aMightyStaffFallsThroughObsidian(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(owner, 100);
        // far enough that it is falling fast when it strikes (one it leans on as it tips, it only touches)
        wall(h, Blocks.OBSIDIAN, 5);
        PlacedStaffEntity staff = felled(h, owner, 4f);
        h.runAfterDelay(140, () -> {
            check(h, elevation(staff) < 20, "down through it: " + elevation(staff));
            int gone = 0;
            for (int y = 2; y < 10; y++) if (h.getBlockState(new BlockPos(2, y, 5)).isAir()) gone++;
            check(h, gone >= 3, "it broke its way: " + gone + " blocks of the wall gone");
            h.succeed();
        });
    }

    /**
     * Toppled on stone by the mightiest, who can pay for any of it: while it still stands its foot
     * stays on the floor (its weight alone breaks nothing) and it tips over; only its crash smashes.
     */
    @GameTest(template = PLATFORM, batch = "staff_physics", timeoutTicks = 200)
    public static void aToppledStaffTipsOverRatherThanDiggingIn(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(owner, 100);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        PlacedStaffEntity staff = felled(h, owner, 2f);
        double floor = h.absolutePos(new BlockPos(2, 2, 1)).getY();
        // from the push until it first leans past 45 degrees: while it still stands
        double[] lowestUpright = {Double.MAX_VALUE};
        boolean[] fallen = {false};
        h.onEachTick(() -> {
            if (fallen[0] || elevation(staff) <= 45) fallen[0] = true;
            else lowestUpright[0] = Math.min(lowestUpright[0], staff.rodBase().y);
        });
        h.runAfterDelay(60, () -> {
            check(h, lowestUpright[0] > floor - 0.3, "standing, its foot never sank: " + (lowestUpright[0] - floor));
            h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 1));
            check(h, fallen[0], "it has tipped over (and, so mighty, smashed in where it struck)");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "staff_physics", timeoutTicks = 200)
    public static void bedrockStopsEvenTheMightiest(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(owner, 100);
        wall(h, Blocks.BEDROCK);
        PlacedStaffEntity staff = felled(h, owner, 4f);
        h.runAfterDelay(140, () -> {
            check(h, elevation(staff) > 35, "leaning on the bedrock: " + elevation(staff));
            for (int y = 2; y < 10; y++) h.assertBlockPresent(Blocks.BEDROCK, new BlockPos(2, y, 3));
            h.succeed();
        });
    }

    // ── Growing pays for its column ──────────────────────────────────

    private static PlacedStaffEntity planted(GameTestHelper h, ServerPlayer owner) {
        Vec3 at = h.absoluteVec(new Vec3(2.5, 2, 2.5));
        PlacedStaffEntity staff = new PlacedStaffEntity(h.getLevel(), at.x, at.y, at.z, owner.getUUID());
        h.getLevel().addFreshEntity(staff);
        onEnd(h, staff::discard);
        return staff;
    }

    @GameTest(template = PLATFORM)
    public static void growingStopsBeneathAnObsidianCeiling(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 8, z), Blocks.OBSIDIAN);
        PlacedStaffEntity staff = planted(h, owner);
        for (int i = 0; i < 20; i++) staff.adjustScale(1, owner);
        double top = staff.getY() + staff.length(), ceiling = h.absolutePos(new BlockPos(2, 8, 2)).getY();
        check(h, top <= ceiling, "its tip stays beneath the ceiling: " + top + " vs " + ceiling);
        check(h, top > ceiling - 1.6, "but grows right up to it: " + top);
        h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 8, 2));
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void aMightyStaffGrowsThroughIt(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(owner, 100);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 8, z), Blocks.OBSIDIAN);
        PlacedStaffEntity staff = planted(h, owner);
        for (int i = 0; i < 20; i++) staff.adjustScale(1, owner);
        near(h, 21, staff.getStaffScale(), "grown all the way");
        h.succeedWhen(() -> check(h, h.getBlockState(new BlockPos(2, 8, 2)).isAir(), "through the ceiling"));
    }

    /** Its reach, shown on the meter, is how tall growing would really take it. */
    @GameTest(template = PLATFORM)
    public static void itsReachIsWhereGrowingStops(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 8, z), Blocks.OBSIDIAN);
        PlacedStaffEntity staff = planted(h, owner);
        h.runAfterDelay(12, () -> { // it works its reach out as it stands
            float reach = staff.reachScale();
            double ceiling = h.absolutePos(new BlockPos(2, 8, 2)).getY();
            check(h, reach < PlacedStaffEntity.MAX_SCALE, "the ceiling holds it short: " + reach);
            check(h, staff.getY() + staff.reachHeight() <= ceiling, "its reach in blocks ends beneath the ceiling");
            for (int i = 0; i < 20; i++) staff.adjustScale(1, owner);
            near(h, reach, staff.getStaffScale(), "it grows exactly as far as its reach said");
            h.succeed();
        });
    }

    /**
     * Growing a notch at a time spends one budget: it ends exactly where growing all at once does,
     * in dirt it cannot pay to drill all of (a fresh budget each notch would have taken it further).
     */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void growingSpendsOneBudget(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        Runnable fill = () -> {
            for (int y = 3; y < 30; y++) for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
        };
        fill.run();
        PlacedStaffEntity stepwise = planted(h, owner);
        for (int i = 0; i < 15; i++) stepwise.adjustScale(1, owner);
        float stepped = stepwise.getStaffScale();
        check(h, stepped < 16, "its budget ran out in the dirt: " + stepped);
        stepwise.discard();
        h.runAfterDelay(60, () -> { // the first's drilling done, the dirt put back
            fill.run();
            PlacedStaffEntity atOnce = planted(h, owner);
            atOnce.adjustScale(15, owner);
            near(h, stepped, atOnce.getStaffScale(), "a notch at a time ends where all at once does");
            h.succeed();
        });
    }
}
