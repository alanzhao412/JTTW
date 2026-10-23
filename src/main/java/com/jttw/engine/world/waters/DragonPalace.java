package com.jttw.engine.world.waters;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;

import javax.annotation.Nullable;

/**
 * A Dragon King's crystal palace (水晶宫), on the floor of his trench: a plinth of prismarine, and on
 * it a throne hall of prismarine and crystal under a stepped, gilded roof, its pillars quartz, its
 * light the sea lanterns set in its walls and ceiling; his throne of gold on a dais at its heart,
 * which is the region's centre; behind it, through two low doors, his treasury, chests of the sea's
 * riches and heaps of gold, and in the Eastern Sea's the pillar that once fixed the depth of the
 * Milky Way (定海神针), which the Monkey King took for his staff. Coral gardens flank the hall, a
 * pair of lantern towers its gate, and a road of lanterns runs out from the gate across the floor.
 *
 * <p>The palace is a blueprint: what stands at each place of it, worked out from the place alone,
 * so each chunk of the trench lays its own share of it as it is made ({@code TrenchFeature}), and
 * however many chunks it spans it comes out whole. Drawn facing south (its gate toward +z) round
 * the throne at (0, 0, 0), local y 0 the floor's top; turned to face its sea ({@link Sea#rotation}).
 */
public final class DragonPalace {

    private DragonPalace() {}

    /** Its footprint, in local blocks round the throne: the plinth and the gardens, the towers. */
    public static final int MIN_X = -19, MAX_X = 19, MIN_Z = -17, MAX_Z = 25;
    /** The highest it reaches over the floor: the roof's ridge. */
    public static final int HEIGHT = 16;
    /** How far its road runs out from the plinth's front, and how wide either side of its middle. */
    public static final int ROAD = 64, ROAD_HALF = 2;
    /** How far round the throne the trench's floor is laid level for it. */
    public static final int LEVEL_RADIUS = 36;
    /** The height the King stands at over the floor: on his dais. */
    public static final int THRONE_Y = 4;
    /** Where the pillar stands in the Eastern treasury (local), and how tall. */
    public static final int PILLAR_Z = -9, PILLAR_LOW = 2, PILLAR_HIGH = 8;
    /** The treasury's chests (local x, z), against its side walls. */
    public static final int[][] CHESTS = {{-8, -7}, {-8, -11}, {8, -7}, {8, -11}};

    public static final ResourceKey<LootTable> TREASURY = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/dragon_treasury"));

    // ── Turning ─────────────────────────────────────────────────────────

    /** The world offset of a local one, the palace turned to face {@code sea}. */
    public static BlockPos toWorld(Sea sea, int x, int y, int z) {
        return new BlockPos(x, y, z).rotate(sea.rotation());
    }

    /** The local offset of a world one (dx, dz from the throne): {x, z}. */
    public static int[] toLocal(Sea sea, int dx, int dz) {
        BlockPos p = new BlockPos(dx, 0, dz).rotate(inverse(sea.rotation()));
        return new int[]{p.getX(), p.getZ()};
    }

    private static Rotation inverse(Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> r;
        };
    }

    /** Whether local (x, z) is under the palace (its plinth, gardens and towers). */
    public static boolean covers(int x, int z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    /** Whether local (x, z) is on the road out of its gate. */
    public static boolean onRoad(int x, int z) {
        return z > MAX_Z && z <= MAX_Z + ROAD && Math.abs(x) <= ROAD_HALF + 1;
    }

    // ── The blueprint ───────────────────────────────────────────────────

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** A spread of 0..99 from a place, the same each time. */
    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    /**
     * What stands at local (x, y, z) (y 0 the floor's top), drawn facing south; null where the sea is
     * left as it is (inside the halls too: the sea fills them).
     */
    @Nullable
    public static BlockState at(Sea sea, int x, int y, int z) {
        if (!covers(x, z) || y < 0 || y > HEIGHT) return null;
        int ax = Math.abs(x);
        // the plinth: its own floor, two courses of prismarine, rimmed dark, a step down at the gate
        if (y <= 1) {
            if (y == 1 && z >= MAX_Z - 1 && ax <= 4) return null;
            boolean rim = ax == MAX_X || z == MIN_Z || z == MAX_Z;
            if (y == 1 && inHall(x, z)) return z >= 2 && ax <= 1 ? s(sea.runner()) : ax == 2 && z >= 2 ? s(Blocks.GOLD_BLOCK) : s(Blocks.QUARTZ_BLOCK);
            if (y == 1 && inTreasury(x, z)) return (x + z) % 3 == 0 ? s(Blocks.GOLD_BLOCK) : s(Blocks.PRISMARINE_BRICKS);
            return rim ? s(Blocks.DARK_PRISMARINE) : s(Blocks.PRISMARINE_BRICKS);
        }
        BlockState hall = hall(sea, x, y, z);
        if (hall != null) return hall;
        BlockState treasury = treasury(sea, x, y, z);
        if (treasury != null) return treasury;
        BlockState roof = roof(x, y, z);
        if (roof != null) return roof;
        // a lantern tower each side of the gate
        if (ax == 6 && z == 23 && y <= 9) return y == 9 ? s(Blocks.SEA_LANTERN) : y == 8 ? s(Blocks.GOLD_BLOCK) : s(Blocks.DARK_PRISMARINE);
        // coral gardens along the hall's sides
        if (ax >= 12 && ax <= 17 && z >= -14 && z <= 22) return garden(x, y, z);
        return null;
    }

    private static boolean inHall(int x, int z) {
        return Math.abs(x) <= 9 && z >= -4 && z <= 20;
    }

    private static boolean inTreasury(int x, int z) {
        return Math.abs(x) <= 9 && z >= -14 && z < -4;
    }

    /** The throne hall: x -9..9, z -4..20, walls up to y 10, its ceiling at 11. */
    @Nullable
    private static BlockState hall(Sea sea, int x, int y, int z) {
        if (!inHall(x, z) || y > 11) return null;
        int ax = Math.abs(x);
        if (y == 11) { // the ceiling, lanterns set in it
            return ax <= 7 && Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 0 ? s(Blocks.SEA_LANTERN) : s(Blocks.DARK_PRISMARINE);
        }
        boolean side = ax == 9, back = z == -4, front = z == 20;
        if (side || back || front) {
            // pillars, each with a lantern at its middle and gold at its head
            boolean pillar = side ? Math.floorMod(z, 4) == 0 : ax == 3 || ax == 6 || ax == 9;
            if (pillar) return y == 10 ? s(Blocks.GOLD_BLOCK) : y == 6 ? s(Blocks.SEA_LANTERN) : s(Blocks.DARK_PRISMARINE);
            if (front && ax <= 2) return y <= 7 ? null : y == 8 ? s(Blocks.GOLD_BLOCK) : s(Blocks.PRISMARINE_BRICKS); // the gate
            if (back && ax == 7 && y <= 4) return null;   // the low doors to the treasury
            if (side && y >= 4 && y <= 8) return s(Blocks.LIGHT_BLUE_STAINED_GLASS); // crystal windows
            if (back && ax <= 4 && y >= 3 && y <= 9) return screen(x, y);
            return s(Blocks.PRISMARINE_BRICKS);
        }
        // inside: two rows of quartz columns down its length
        if (ax == 5 && z >= 4 && z <= 16 && z % 4 == 0) {
            return y == 2 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : y == 10 ? s(Blocks.GOLD_BLOCK) : s(Blocks.QUARTZ_PILLAR);
        }
        return throne(sea, x, y, z);
    }

    /** The screen behind the throne: gold, sea lanterns its pearls, on dark prismarine. */
    private static BlockState screen(int x, int y) {
        int ax = Math.abs(x);
        if ((ax == 0 && y == 8) || (ax == 3 && y == 5)) return s(Blocks.SEA_LANTERN);
        if (ax == 4 || y == 3 || y == 9 || (y + ax) % 3 == 0) return s(Blocks.GOLD_BLOCK);
        return s(Blocks.DARK_PRISMARINE);
    }

    /** The throne: a dais of quartz and gold, he stands on it at y 4, a seat of gold behind him. */
    @Nullable
    private static BlockState throne(Sea sea, int x, int y, int z) {
        int ax = Math.abs(x);
        if (y == 2 && ax <= 3 && z >= -3 && z <= 2) return s(Blocks.QUARTZ_BLOCK);
        if (y == 3 && ax <= 2 && z >= -3 && z <= 1) return s(Blocks.GOLD_BLOCK);
        if (z == -3 && ax <= 2 && y >= 4 && y <= 7) return y == 7 && ax == 0 ? s(Blocks.SEA_LANTERN) : s(Blocks.GOLD_BLOCK); // its back
        if (z == -2 && ax == 2 && (y == 4 || y == 5)) return s(Blocks.GOLD_BLOCK); // its arms
        return null;
    }

    /** The treasury, behind the hall: x -9..9, z -14..-5, walls to y 8, its ceiling at 9. */
    @Nullable
    private static BlockState treasury(Sea sea, int x, int y, int z) {
        if (!(Math.abs(x) <= 9 && z >= -14 && z <= -5) || y > 9) return null;
        int ax = Math.abs(x);
        if (y == 9) return x == 0 && z == -9 ? s(Blocks.SEA_LANTERN) : s(Blocks.DARK_PRISMARINE);
        if (ax == 9 || z == -14) {
            if (y == 5 && (z == -9 || (z == -14 && ax == 5))) return s(Blocks.SEA_LANTERN);
            return s(Blocks.PRISMARINE_BRICKS);
        }
        // the chests, against the side walls, facing in
        for (int[] c : CHESTS) {
            if (x == c[0] && z == c[1] && y == 2) {
                return s(Blocks.CHEST).setValue(ChestBlock.FACING, c[0] < 0 ? Direction.EAST : Direction.WEST)
                        .setValue(BlockStateProperties.WATERLOGGED, true);
            }
        }
        // heaps of gold in the far corners
        if (ax >= 7 && z <= -13 && y <= 3 - (ax == 7 ? 1 : 0)) return y == 2 ? s(Blocks.RAW_GOLD_BLOCK) : s(Blocks.GOLD_BLOCK);
        if (ax >= 7 && z == -12 && y == 2) return s(Blocks.GOLD_BLOCK);
        // the treasure at its heart: the pillar, in the East; elsewhere a pearl on a stand of gold
        if (x == 0 && z == PILLAR_Z) {
            if (sea == Sea.EAST) return y >= PILLAR_LOW && y <= PILLAR_HIGH ? ModBlocks.SEA_CALMING_PILLAR.get().defaultBlockState() : null;
            return y == 2 ? s(Blocks.GOLD_BLOCK) : y == 3 ? s(Blocks.SEA_LANTERN) : null;
        }
        return null;
    }

    /**
     * The roof: eaves overhanging the walls by two, then four courses stepping in, dark prismarine
     * edged in gold, gold at its corners turned up and along its ridge. Over the treasury, a lower,
     * flat one (its ceiling).
     */
    @Nullable
    private static BlockState roof(int x, int y, int z) {
        int ax = Math.abs(x);
        int k = y - 11;
        if (k < 0 || k > 5) return null;
        if (k == 5) return x == 0 && z >= 2 && z <= 14 ? s(Blocks.GOLD_BLOCK) : null; // the ridge
        int halfX = 11 - 2 * k, z0 = -6 + 2 * k, z1 = 22 - 2 * k;
        if (ax > halfX || z < z0 || z > z1) {
            // its corners turned up, a step over the course below
            if (k >= 1 && k <= 2) {
                int bx = 11 - 2 * (k - 1), bz0 = -6 + 2 * (k - 1), bz1 = 22 - 2 * (k - 1);
                if (ax == bx && (z == bz0 || z == bz1)) return s(Blocks.GOLD_BLOCK);
            }
            return null;
        }
        if (k == 0 && inHall(x, z)) return null; // the ceiling's own
        boolean edge = ax == halfX || z == z0 || z == z1;
        return edge && k <= 1 ? s(Blocks.GOLD_BLOCK) : s(Blocks.DARK_PRISMARINE);
    }

    /** The gardens: coral heads on the plinth, fans and plants on them, pickles glowing among them. */
    @Nullable
    private static BlockState garden(int x, int y, int z) {
        int h = hash(x, z, 31);
        Block[] heads = {Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK};
        Block[] plants = {Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL};
        int kind = hash(x / 3, z / 3, 7) % 5;
        boolean head = h < 45;
        int tall = h < 15 ? 2 : 1;
        if (head) {
            if (y < 2 + tall) return s(heads[kind]);
            if (y == 2 + tall) return h % 3 == 0 ? s(plants[kind]) : null;
            return null;
        }
        if (y == 2) {
            if (h < 55) return s(plants[(kind + 1) % 5]);
            if (h < 60) return s(Blocks.SEA_PICKLE).setValue(SeaPickleBlock.PICKLES, 1 + h % 4);
            if (h < 75) return s(Blocks.SEAGRASS);
        }
        return null;
    }

    // ── Laying it ───────────────────────────────────────────────────────

    /**
     * Lays what of the palace stands in the column at world (wx, wz), whose throne is at
     * {@code throne} (on the floor's top), into {@code level}: blocks, the treasury's chests and
     * their loot. Returns whether anything was laid.
     */
    public static boolean layColumn(LevelAccessor level, BlockPos throne, Sea sea, int wx, int wz, RandomSource random) {
        int[] l = toLocal(sea, wx - throne.getX(), wz - throne.getZ());
        if (!covers(l[0], l[1])) return false;
        Rotation rot = sea.rotation();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= HEIGHT; y++) {
            BlockState state = at(sea, l[0], y, l[1]);
            if (state == null) continue;
            p.set(wx, throne.getY() + y, wz);
            level.setBlock(p, state.rotate(rot), Block.UPDATE_CLIENTS);
            if (state.is(Blocks.CHEST)) RandomizableContainer.setBlockEntityLootTable(level, random, p, TREASURY);
        }
        return true;
    }

    /** Lays the whole palace round {@code throne} at once, the sea let into it (the gallery's, and tests'). */
    public static void layWhole(LevelAccessor level, BlockPos throne, Sea sea, RandomSource random, boolean flood) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int lx = MIN_X; lx <= MAX_X; lx++) for (int lz = MIN_Z; lz <= MAX_Z; lz++) {
            BlockPos o = toWorld(sea, lx, 0, lz);
            int wx = throne.getX() + o.getX(), wz = throne.getZ() + o.getZ();
            if (flood) for (int y = 1; y <= HEIGHT + 2; y++) level.setBlock(p.set(wx, throne.getY() + y, wz), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            layColumn(level, throne, sea, wx, wz, random);
        }
    }

    /** Where the King stands: on his dais, at the throne. */
    public static BlockPos kingAt(BlockPos throne) {
        return throne.above(THRONE_Y);
    }
}
