package com.jttw.engine.village;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A village of the Tang (唐朝, the seventh century of the novel), as a blueprint: what stands at
 * each local place, from the place and the village's plan alone, so each chunk lays its own share
 * ({@link TangVillagePiece}) and the village comes out whole whichever chunk is made first. Local y
 * 0 is the village's ground (its streets' level), (0, 0) the crossroads at its heart, its gate
 * toward +z.
 * <ul>
 *   <li><b>the streets</b>: a main street from the village gate north to the Earth God's shrine, a
 *       cross street through the square at the crossroads (a well at its heart), a back lane; lanterns
 *       on posts along them; trodden earth;</li>
 *   <li><b>the gate</b> (牌坊), red posts and grey-tiled roofs over the main street, the village's
 *       name on it (Gao Village, Chen Village and others of the novel);</li>
 *   <li><b>the shrine to the Earth God</b> (土地庙), small, in its walled yard, red pillars, an altar,
 *       an incense burner before it: the village's own, as every village had;</li>
 *   <li><b>courtyard houses</b> (四合院): a whitewashed compound wall round a paved court, the main
 *       hall at its head and wing rooms either side, a water vat in the court; <b>cottages</b>;</li>
 *   <li><b>the inn</b> (客栈), two storeys, its ground floor an open teahouse; <b>the herbalist's</b>
 *       (药铺), jars and drying herbs; <b>the smithy</b> (铁匠铺), an open shed round its forge;
 *       <b>market stalls</b> under coloured awnings down the main street; a fortune teller's table on
 *       the square;</li>
 *   <li><b>the fields</b> either side: rice paddies on the plains and by rivers, millet and wheat in
 *       the mountains, a pond and date palms at a desert oasis; rows of mulberry trees.</li>
 * </ul>
 * Whitewashed rammed-earth walls on stone plinths, red pillars and dark beams, grey tile roofs with
 * upturned eaves; in the mountains, fieldstone and spruce; at an oasis, sandstone and mud brick.
 */
public final class TangVillage {

    private TangVillage() {}

    /** The kinds of village, by the land it stands in. */
    public enum Variant implements StringRepresentable {
        /** Plains, meadows and river country: rice paddies, whitewashed walls, grey tiles. */
        PLAINS("plains"),
        /** Taiga and snowy country: millet and wheat, fieldstone and spruce. */
        MOUNTAIN("mountain"),
        /** The desert's edge and the savanna: a caravan halt by an oasis, sandstone and mud brick. */
        OASIS("oasis");

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);
        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** The village's reach (local x and z), how far down its footings go, how high it reaches, how high open ground is cleared. */
    public static final int WEST = -46, EAST = 46, NORTH = -34, SOUTH = 35, BOTTOM = -4, TOP = 16, CLEAR = 20;
    /** The streets: the main street's and cross street's half-width, the main street's ends, the back lane's z, the square's half-width. */
    public static final int STREET = 2, MAIN_N = -24, BACK_LANE = -20, SQUARE = 6, GATE_Z = 33;
    /** How many tiers a roof rises before it levels off under its ridge (a broad hall's is not a pyramid). */
    public static final int ROOF_TIERS = 3;
    /** The fields: their inner edge (|x|), and where they run (z). */
    public static final int FIELDS = 29, FIELDS_N = -28, FIELDS_S = 31;

    /** Whether local (x, z) is the village's. */
    public static boolean covers(int x, int z) {
        return x >= WEST && x <= EAST && z >= NORTH && z <= SOUTH;
    }

    // ── The palette ─────────────────────────────────────────────────────

    /** What a village is built of. */
    record Palette(BlockState base, BlockState wall, BlockState beam, BlockState pillar, Block roofStairs, Block roofSlab, BlockState roofFull,
                   Block planks, Block stairs, Block slab, Block fence, Block door, Block trapdoor, BlockState pave, BlockState pave2,
                   BlockState log, BlockState leaves) {}

    static Palette palette(Variant v) {
        return switch (v) {
            case PLAINS -> new Palette(s(Blocks.STONE_BRICKS), s(Blocks.CALCITE), log(Blocks.STRIPPED_DARK_OAK_LOG), log(Blocks.STRIPPED_MANGROVE_LOG),
                    Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, s(Blocks.DEEPSLATE_TILES),
                    Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_TRAPDOOR,
                    s(Blocks.STONE_BRICKS), s(Blocks.POLISHED_ANDESITE), log(Blocks.DARK_OAK_LOG), leaves(Blocks.DARK_OAK_LEAVES));
            case MOUNTAIN -> new Palette(s(Blocks.COBBLESTONE), s(Blocks.CALCITE), log(Blocks.SPRUCE_LOG), log(Blocks.STRIPPED_MANGROVE_LOG),
                    Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, s(Blocks.DEEPSLATE_TILES),
                    Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_TRAPDOOR,
                    s(Blocks.STONE_BRICKS), s(Blocks.MOSSY_STONE_BRICKS), log(Blocks.SPRUCE_LOG), leaves(Blocks.SPRUCE_LEAVES));
            case OASIS -> new Palette(s(Blocks.CUT_SANDSTONE), s(Blocks.SMOOTH_SANDSTONE), log(Blocks.STRIPPED_JUNGLE_LOG), log(Blocks.STRIPPED_MANGROVE_LOG),
                    Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICK_SLAB, s(Blocks.MUD_BRICKS),
                    Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB, Blocks.JUNGLE_FENCE, Blocks.JUNGLE_DOOR, Blocks.JUNGLE_TRAPDOOR,
                    s(Blocks.SMOOTH_SANDSTONE), s(Blocks.CUT_SANDSTONE), log(Blocks.JUNGLE_LOG), leaves(Blocks.JUNGLE_LEAVES));
        };
    }

    // ── Small helpers ───────────────────────────────────────────────────

    static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    static BlockState log(Block b) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** A hash of a place, 0-99. */
    static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    static BlockState stair(Block b, Direction facing, Half half, StairsShape shape) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half).setValue(StairBlock.SHAPE, shape);
    }

    static BlockState slab(Block b, SlabType type) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    static BlockState hanging(Block lantern) {
        return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    static BlockState candles(int n) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, n).setValue(CandleBlock.LIT, true);
    }

    /** A fence joined along x (east-west) or z. */
    static BlockState rail(Block fence, boolean alongX) {
        return alongX ? fence.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.WEST, true)
                : fence.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
    }

    static BlockState door(Block door, Direction facing, boolean upper, boolean rightHinge) {
        return door.defaultBlockState().setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.HINGE, rightHinge ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
    }

    /** A bed's part: its head toward {@code facing}. */
    static BlockState bed(Block bed, Direction facing, boolean head) {
        return bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, head ? BedPart.HEAD : BedPart.FOOT);
    }

    static BlockState trapdoorTop(Block trapdoor) {
        return trapdoor.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
    }

    static BlockState banner(Block banner, Direction facing) {
        return banner.defaultBlockState().setValue(WallBannerBlock.FACING, facing);
    }

    // ── The plan ────────────────────────────────────────────────────────

    /** The kinds of building, each with its own size (u across its front, v from back to front). */
    public enum Kind {
        COURTYARD(15, 15), INN(13, 11), HERBALIST(9, 8), SMITHY(9, 8), SHRINE(9, 9), COTTAGE(9, 8), STALL(3, 3), WELL(3, 3);

        final int w, d;

        Kind(int w, int d) {
            this.w = w;
            this.d = d;
        }
    }

    /**
     * A building of the plan: its kind, the corner its footprint starts at (least x and z), and the
     * way its front faces. In its own frame (u, v) it faces south: u runs east across its front, v
     * from its back (0) to its front (d - 1).
     */
    public record Building(Kind kind, int x0, int z0, Direction facing, int salt) {
        int sizeX() {
            return facing.getAxis() == Direction.Axis.Z ? kind.w : kind.d;
        }

        int sizeZ() {
            return facing.getAxis() == Direction.Axis.Z ? kind.d : kind.w;
        }

        boolean contains(int x, int z) {
            return x >= x0 && x < x0 + sizeX() && z >= z0 && z < z0 + sizeZ();
        }

        /** Whether (x, z) is in the block-wide ring round it, where its eaves overhang. */
        boolean near(int x, int z) {
            return !contains(x, z) && x >= x0 - 1 && x <= x0 + sizeX() && z >= z0 - 1 && z <= z0 + sizeZ();
        }

        /** Local (x, z) in its own frame, as {u, v}. */
        int[] frame(int x, int z) {
            int dx = x - x0, dz = z - z0, w = kind.w, d = kind.d;
            return switch (facing) {
                case NORTH -> new int[]{w - 1 - dx, d - 1 - dz};
                case WEST -> new int[]{dz, d - 1 - dx};
                case EAST -> new int[]{w - 1 - dz, dx};
                default -> new int[]{dx, dz};
            };
        }

        /** Its own (u, v) as local {x, z}. */
        int[] place(int u, int v) {
            int w = kind.w, d = kind.d;
            return switch (facing) {
                case NORTH -> new int[]{x0 + w - 1 - u, z0 + d - 1 - v};
                case WEST -> new int[]{x0 + d - 1 - v, z0 + u};
                case EAST -> new int[]{x0 + v, z0 + w - 1 - u};
                default -> new int[]{x0 + u, z0 + v};
            };
        }

        Rotation rotation() {
            return switch (facing) {
                case NORTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.CLOCKWISE_90;
                case EAST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }
    }

    /** A villager the plan puts in place: its calling, whether a woman, where it stands (local, on the ground) and its bed's head (or none). */
    public record Resident(Calling calling, boolean woman, int x, int z, @Nullable int[] bed) {}

    /** The height of the inn's upper floor's beds (the rest stand on the ground floor, y 1). */
    public static final int UPPER_BEDS = 6;

    /** A sign of the plan: where (local; y over the ground), the way it faces, its lines. */
    public record Sign(int x, int y, int z, Direction facing, String... lines) {}

    /** A chest of the plan: where, and whether it is the smithy's (else a household's). */
    public record Chest(int x, int y, int z, boolean smithy) {}

    /** The names the villages are given: Gao Village and Chen Village of the novel, and others of its country. */
    static final String[][] NAMES = {{"高老庄", "Gao Village"}, {"陈家庄", "Chen Village"}, {"杏花村", "Apricot Blossom", "Village"},
            {"柳林村", "Willow Grove", "Village"}, {"桃源村", "Peach Spring", "Village"}, {"白水村", "White Water", "Village"},
            {"石门村", "Stone Gate", "Village"}, {"李家村", "Li Family", "Village"}};
    static final String[][] OASIS_NAMES = {{"沙州驿", "Shazhou Post"}, {"流沙驿", "Flowing Sands", "Post"}, {"玉门驿", "Jade Gate Post"},
            {"月泉驿", "Moon Spring Post"}};

    /** A village's whole plan: what it is made of, where each building stands, its folk, signs and chests. Mirrored east for west, maybe. */
    public static final class Plan {
        public final Variant variant;
        final Palette p;
        /** Whether it is laid mirrored (east for west): the plan's x is the world's -x. */
        public final boolean mirror;
        final long seed;
        public final List<Building> buildings = new ArrayList<>();
        public final List<Resident> residents = new ArrayList<>();
        public final List<Sign> signs = new ArrayList<>();
        public final List<Chest> chests = new ArrayList<>();
        public final String[] name;
        /** Where the oasis's camels stand. */
        public final List<int[]> camels = new ArrayList<>();

        Plan(Variant variant, long seed) {
            this.variant = variant;
            this.seed = seed;
            this.p = palette(variant);
            this.mirror = (seed & 1) == 1;
            String[][] names = variant == Variant.OASIS ? OASIS_NAMES : NAMES;
            this.name = names[(int) Math.floorMod(seed >> 3, names.length)];
            int salt = (int) (seed >> 8);
            // the north row: the shrine at the head of the main street, a cottage and the herbalist's either side
            add(new Building(Kind.SHRINE, -4, -33, Direction.SOUTH, salt + 1));
            add(new Building(Kind.COTTAGE, -17, -31, Direction.SOUTH, salt + 2));
            add(new Building(Kind.HERBALIST, 9, -31, Direction.SOUTH, salt + 3));
            // the middle row, between the back lane and the cross street: a courtyard house, the inn
            add(new Building(Kind.COURTYARD, -23, -18, Direction.SOUTH, salt + 4));
            add(new Building(Kind.INN, 9, -17, Direction.SOUTH, salt + 5));
            // the south row: a courtyard house and the smithy facing the cross street, a cottage by the market
            add(new Building(Kind.COURTYARD, -23, 4, Direction.NORTH, salt + 6));
            add(new Building(Kind.SMITHY, 10, 4, Direction.NORTH, salt + 7));
            add(new Building(Kind.COTTAGE, 10, 18, Direction.WEST, salt + 8));
            // the market: stalls down both sides of the main street; the well on the square
            for (int k = 0; k < 4; k++) {
                add(new Building(Kind.STALL, 4, 9 + 5 * k, Direction.WEST, salt + 20 + k));
                add(new Building(Kind.STALL, -6, 9 + 5 * k, Direction.EAST, salt + 30 + k));
            }
            add(new Building(Kind.WELL, -1, -1, Direction.SOUTH, salt + 9));
            folk();
            signs.add(new Sign(0, 4, GATE_Z + 1, Direction.SOUTH, name));
            if (variant == Variant.OASIS) for (int[] c : new int[][]{{36, 3}, {39, 6}, {-37, 4}}) camels.add(c);
        }

        private void add(Building b) {
            buildings.add(b);
        }

        private Building first(Kind kind, int nth) {
            int n = 0;
            for (Building b : buildings) if (b.kind == kind && n++ == nth) return b;
            throw new IllegalStateException("no " + kind);
        }

        /** Someone living in building {@code b}: standing at its (u, v), the head of their bed at (bu, bv) (bv < 0: no bed). */
        private void resident(Calling c, boolean woman, Building b, int u, int v, int bu, int bv) {
            int[] at = b.place(u, v);
            int[] bed = bv < 0 ? null : b.place(bu, bv);
            residents.add(new Resident(c, woman, at[0], at[1], bed == null ? null : new int[]{bed[0], b.kind == Kind.INN ? UPPER_BEDS : 1, bed[1]}));
        }

        private boolean coin(int salt) {
            return hash((int) seed, salt, 77) < 50;
        }

        private void folk() {
            Building shrine = first(Kind.SHRINE, 0), inn = first(Kind.INN, 0), herb = first(Kind.HERBALIST, 0), smithy = first(Kind.SMITHY, 0);
            Building houseA = first(Kind.COURTYARD, 0), houseB = first(Kind.COURTYARD, 1), cottageA = first(Kind.COTTAGE, 0), cottageB = first(Kind.COTTAGE, 1);
            resident(Calling.MONK, false, shrine, 4, 7, 2, 2);
            resident(Calling.INNKEEPER, coin(1), inn, 6, 7, 4, 1);
            resident(Calling.FORTUNE_TELLER, false, inn, 6, 9, 7, 1);
            resident(Calling.HERBALIST, coin(2), herb, 4, 5, 7, 3);
            resident(Calling.SMITH, false, smithy, 4, 6, 3, 1);
            resident(Calling.FARMER, false, houseA, 7, 8, 3, 1);
            resident(Calling.FARMER, true, houseA, 6, 10, 2, 8);
            resident(Calling.SCHOLAR, false, houseB, 7, 8, 3, 1);
            resident(Calling.MERCHANT, coin(3), houseB, 8, 10, 12, 8);
            resident(Calling.FARMER, false, cottageA, 4, 4, 2, 2);
            resident(Calling.FARMER, true, cottageB, 4, 4, 2, 2);
            for (Building b : buildings) {
                if (b.kind == Kind.COURTYARD) chest(b, 12, 1, 1, false);
                if (b.kind == Kind.SMITHY) chest(b, 6, 1, 1, true);
                if (b.kind == Kind.INN) chest(b, 11, UPPER_BEDS, 1, false);
            }
            // the signs over their doors
            sign(shrine, 4, 3, 5, "土地庙", "Shrine of the", "Earth God");
            sign(inn, 6, 4, 11, "客栈", variant == Variant.OASIS ? "Caravan Inn" : "Inn and", variant == Variant.OASIS ? "" : "Teahouse");
            sign(herb, 4, 4, 8, "药铺", "Herbalist");
            sign(smithy, 4, 4, 8, "铁匠铺", "Smithy");
        }

        private void chest(Building b, int u, int y, int v, boolean smithy) {
            int[] at = b.place(u, v);
            chests.add(new Chest(at[0], y, at[1], smithy));
        }

        private void sign(Building b, int u, int y, int v, String... lines) {
            int[] at = b.place(u, v);
            signs.add(new Sign(at[0], y, at[1], b.rotation().rotate(Direction.SOUTH), lines));
        }

        /** The plan's x for the world's local x. */
        public int planX(int x) {
            return mirror ? -x : x;
        }

        /** What stands at local (x, y, z) (the world's, mirrored if the plan is): a block, or null where the land is left as it is. */
        @Nullable
        public BlockState at(int x, int y, int z) {
            BlockState s = planAt(planX(x), y, z);
            return s == null || !mirror ? s : s.mirror(Mirror.FRONT_BACK);
        }

        /** A sign's facing, mirrored if the plan is. */
        public Direction facing(Direction d) {
            return mirror ? Mirror.FRONT_BACK.mirror(d) : d;
        }

        @Nullable
        private BlockState planAt(int x, int y, int z) {
            if (!covers(x, z) || y < BOTTOM || y > CLEAR) return null;
            for (Building b : buildings) {
                if (!b.contains(x, z)) continue;
                int[] uv = b.frame(x, z);
                BlockState st = building(b, uv[0], y, uv[1]);
                if (st != null || y <= 0) return st != null ? st.rotate(b.rotation()) : ground(x, y, z);
                return s(Blocks.AIR);
            }
            // a roof's eaves overhang its building by a block
            if (y >= 3) {
                for (Building b : buildings) {
                    if (b.kind == Kind.STALL || b.kind == Kind.WELL || !b.near(x, z)) continue;
                    int[] uv = b.frame(x, z);
                    BlockState st = building(b, uv[0], y, uv[1]);
                    if (st != null && !st.isAir() && isRoof(st)) return st.rotate(b.rotation());
                }
            }
            BlockState d = decor(x, y, z);
            if (d != null) return d;
            if (y <= 0) return ground(x, y, z);
            return s(Blocks.AIR);
        }

        /** Whether a block is of the roofs (their tiles, slabs and stairs). */
        boolean isRoof(BlockState st) {
            return st.is(p.roofStairs) || st.is(p.roofSlab) || st.equals(p.roofFull);
        }

        // ── The ground ──────────────────────────────────────────────────

        boolean street(int x, int z) {
            if (Math.abs(x) <= STREET && z >= MAIN_N && z <= SOUTH) return true;
            if (Math.abs(z) <= STREET && Math.abs(x) < FIELDS) return true;
            return Math.abs(z - BACK_LANE) <= 1 && Math.abs(x) < FIELDS;
        }

        boolean square(int x, int z) {
            return Math.abs(x) <= SQUARE && Math.abs(z) <= SQUARE;
        }

        boolean field(int x, int z) {
            return Math.abs(x) >= FIELDS && z >= FIELDS_N && z <= FIELDS_S;
        }

        /** The ground's own blocks, from its surface (y 0) down. */
        BlockState ground(int x, int y, int z) {
            if (y < 0) {
                if (field(x, z) && y == -1 && variant != Variant.OASIS) return s(Blocks.DIRT);
                return y >= -2 ? (variant == Variant.OASIS ? s(Blocks.SANDSTONE) : s(Blocks.DIRT)) : null;
            }
            if (field(x, z)) return fieldTop(x, z);
            int r = hash(x, z, 3), patch = hash(Math.floorDiv(x, 3), Math.floorDiv(z, 3), 4);
            if (square(x, z)) {
                boolean edge = Math.abs(x) == SQUARE || Math.abs(z) == SQUARE;
                return edge ? p.pave2 : r < 82 ? p.pave : p.pave2;
            }
            if (street(x, z)) {
                // trodden earth, worn in patches; a stone at the margins now and then
                boolean margin = Math.abs(x) == STREET && Math.abs(z) > STREET || Math.abs(z) == STREET && Math.abs(x) > STREET;
                if (variant == Variant.OASIS) return patch < 20 ? s(Blocks.SANDSTONE) : r < 8 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.PACKED_MUD);
                if (variant == Variant.MOUNTAIN) return patch < 25 ? s(Blocks.GRAVEL) : margin && r < 20 ? s(Blocks.COBBLESTONE) : s(Blocks.DIRT_PATH);
                return patch < 14 ? s(Blocks.PACKED_MUD) : margin && r < 15 ? s(Blocks.GRAVEL) : s(Blocks.DIRT_PATH);
            }
            // open ground between the buildings: grass (sand at an oasis), worn to earth in places
            if (variant == Variant.OASIS) return patch < 18 ? s(Blocks.PACKED_MUD) : r < 6 ? s(Blocks.SANDSTONE) : s(Blocks.SAND);
            if (variant == Variant.MOUNTAIN && patch < 22) return s(Blocks.COARSE_DIRT);
            return patch < 12 ? s(Blocks.DIRT_PATH) : r < 4 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRASS_BLOCK);
        }

        /** Which field cell (x, z) is in, its inner corner, and what grows there. */
        int fieldCell(int x, int z) {
            int cx = Math.floorDiv(Math.abs(x) - FIELDS, 6), cz = Math.floorDiv(z - FIELDS_N, 6);
            return cx * 31 + cz * 7 + (x < 0 ? 101 : 0);
        }

        boolean bund(int x, int z) {
            int ax = Math.abs(x) - FIELDS, dz = z - FIELDS_N;
            return ax % 6 == 0 || dz % 6 == 0 || Math.abs(x) == EAST || z == FIELDS_S;
        }

        /** A field's crop: 0 paddy, 1 wheat (millet), 2 potatoes and carrots, 3 an orchard of palms or mulberries, 4 the pond. */
        int crop(int x, int z) {
            int cell = fieldCell(x, z);
            int ax = Math.abs(x) - FIELDS, dz = z - FIELDS_N;
            if (x > 0 && ax >= 6 && ax < 18 && dz >= 24 && dz < 36) return 4; // the pond
            int r = hash(cell, (int) seed, 9);
            return switch (variant) {
                case PLAINS -> r < 70 ? 0 : r < 90 ? 1 : 3;
                case MOUNTAIN -> r < 55 ? 1 : r < 85 ? 2 : 3;
                case OASIS -> r < 45 ? 3 : r < 75 ? 1 : 2;
            };
        }

        BlockState fieldTop(int x, int z) {
            int crop = crop(x, z);
            if (crop == 4) {
                int ax = Math.abs(x) - FIELDS, dz = z - FIELDS_N;
                boolean rim = ax == 6 || ax == 17 || dz == 24 || dz == 35;
                return rim ? (variant == Variant.OASIS ? s(Blocks.SAND) : s(Blocks.GRASS_BLOCK)) : s(Blocks.WATER);
            }
            if (bund(x, z)) {
                if (variant == Variant.OASIS) return s(Blocks.PACKED_MUD);
                return crop == 0 ? s(Blocks.MUD) : hash(x, z, 5) < 60 ? s(Blocks.DIRT_PATH) : s(Blocks.GRASS_BLOCK);
            }
            return switch (crop) {
                case 0 -> s(Blocks.WATER);
                case 1, 2 -> {
                    int ax = Math.abs(x) - FIELDS, dz = z - FIELDS_N;
                    yield ax % 6 == 3 && dz % 6 == 3 ? s(Blocks.WATER) : s(Blocks.FARMLAND).setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7);
                }
                default -> variant == Variant.OASIS ? (hash(x, z, 6) < 70 ? s(Blocks.SAND) : s(Blocks.GRASS_BLOCK)) : s(Blocks.GRASS_BLOCK);
            };
        }

        // ── What stands on the open ground ──────────────────────────────

        @Nullable
        BlockState decor(int x, int y, int z) {
            if (y <= 0) return null;
            if (field(x, z)) return fieldGrowth(x, y, z);
            // the village gate
            if (Math.abs(z - GATE_Z) <= 1 && Math.abs(x) <= 7) {
                BlockState g = gate(x, y, z - GATE_Z);
                if (g != null) return g;
            }
            // lanterns on posts along the streets, every eighth block, clear of the stalls and the square
            if (lanternPost(x, z)) {
                if (y <= 2) return s(p.fence);
                if (y == 3) return s(Blocks.LANTERN);
                return null;
            }
            // a row of mulberry trees between the village and its fields
            BlockState t = treeRow(x, y, z);
            if (t != null) return t;
            // the fortune teller's table on the square: a cloth-hung table, a stool, a banner
            if (x == -5 && z == 4) return y == 1 ? s(p.fence) : y == 2 ? s(Blocks.RED_CARPET) : null;
            if (x == -5 && z == 5 && y == 1) return stair(p.stairs, Direction.SOUTH);
            if (x == -4 && z == 4) return y <= 2 ? s(p.fence) : y == 3 ? s(Blocks.YELLOW_BANNER) : null;
            // grass and a flower or two on the open ground
            if (y == 1 && !street(x, z) && !square(x, z) && ground(x, 0, z).is(Blocks.GRASS_BLOCK)) {
                int r = hash(x, z, 15);
                if (r < 14) return s(Blocks.SHORT_GRASS);
                if (r < 16) return s(variant == Variant.MOUNTAIN ? Blocks.FERN : r == 14 ? Blocks.POPPY : Blocks.AZURE_BLUET);
            }
            return null;
        }

        boolean lanternPost(int x, int z) {
            int ax = Math.abs(x), az = Math.abs(z);
            if (square(x, z)) return ax == SQUARE && az == SQUARE;
            if (ax == STREET + 1 && z > MAIN_N && Math.floorMod(z, 8) == 4 && z < 8) return true;
            if (ax == STREET + 1 && z >= 29 && z <= 31 && z == 30) return true;
            return az == STREET + 1 && ax > SQUARE && ax < FIELDS - 3 && Math.floorMod(x, 8) == 4 && !(z < 0 && ax >= 9 && ax <= 21);
        }

        /** Mulberry trees (oasis: date palms) in a row along the fields' inner edge. */
        @Nullable
        BlockState treeRow(int x, int y, int z) {
            int ax = Math.abs(x);
            if (ax < 25 || ax > 28 || z < FIELDS_N || z > FIELDS_S) return null;
            int tz = FIELDS_N + 2 + Math.floorDiv(z - FIELDS_N - 2 + 4, 8) * 8;
            if (Math.abs(tz) <= STREET + 2) return null;
            return tree(ax - 26, y, z - tz);
        }

        /** A tree whose trunk stands at (0, 0): a mulberry (a short trunk, a broad round crown), or at an oasis a date palm. */
        @Nullable
        BlockState tree(int dx, int y, int dz) {
            if (variant == Variant.OASIS) {
                if (dx == 0 && dz == 0 && y <= 5) return log(Blocks.JUNGLE_LOG);
                if (y == 6 && (dx == 0 && Math.abs(dz) <= 2 || dz == 0 && Math.abs(dx) <= 2)) return leaves(Blocks.JUNGLE_LEAVES);
                if (y == 5 && (Math.abs(dx) == 2 && Math.abs(dz) <= 0 || Math.abs(dz) == 2 && dx == 0 || Math.abs(dx) == 3 && dz == 0 || Math.abs(dz) == 3 && dx == 0))
                    return leaves(Blocks.JUNGLE_LEAVES);
                if (y == 7 && dx == 0 && dz == 0) return leaves(Blocks.JUNGLE_LEAVES);
                return null;
            }
            if (dx == 0 && dz == 0 && y <= 3) return p.log;
            double r2 = dx * dx + dz * dz + (y - 4.5) * (y - 4.5) * 1.6;
            if (y >= 3 && y <= 6 && r2 <= 6.5 && !(dx == 0 && dz == 0 && y == 3)) {
                return variant == Variant.MOUNTAIN ? leaves(Blocks.SPRUCE_LEAVES) : leaves(Blocks.DARK_OAK_LEAVES);
            }
            return null;
        }

        @Nullable
        BlockState fieldGrowth(int x, int y, int z) {
            if (bund(x, z)) return y <= 2 ? s(Blocks.AIR) : null;
            int crop = crop(x, z);
            int ax = Math.abs(x) - FIELDS, dz = z - FIELDS_N;
            if (crop == 4) {
                boolean rim = ax == 6 || ax == 17 || dz == 24 || dz == 35;
                if (!rim && y == 1 && hash(x, z, 11) < 12) return s(Blocks.LILY_PAD);
                if (rim && y <= 2 && hash(x, z, 12) < 25) return s(Blocks.SUGAR_CANE);
                return y <= 2 ? s(Blocks.AIR) : null;
            }
            if (y > 1) {
                if (crop == 3) {
                    int lx = Math.floorMod(ax, 6), lz = Math.floorMod(dz, 6);
                    BlockState t = tree(lx - 3, y, lz - 3);
                    if (t != null) return t;
                }
                return y <= 8 ? s(Blocks.AIR) : null;
            }
            return switch (crop) {
                case 0 -> null; // the paddy's water, its rice set below
                case 1 -> ax % 6 == 3 && dz % 6 == 3 ? null : s(Blocks.WHEAT).setValue(CropBlock.AGE, 4 + hash(x, z, 7) % 4);
                case 2 -> ax % 6 == 3 && dz % 6 == 3 ? null : (hash(x, z, 8) < 50 ? s(Blocks.POTATOES) : s(Blocks.CARROTS)).setValue(CropBlock.AGE, 3 + hash(x, z, 7) % 5);
                default -> {
                    int lx = Math.floorMod(ax, 6), lz = Math.floorMod(dz, 6);
                    BlockState t = tree(lx - 3, y, lz - 3);
                    yield t != null ? t : hash(x, z, 13) < 30 ? s(variant == Variant.OASIS ? Blocks.DEAD_BUSH : Blocks.SHORT_GRASS) : null;
                }
            };
        }

        /** The rice in a paddy: seedlings in the water (y 0) of most of its places. */
        public boolean riceAt(int x, int z) {
            int px = planX(x);
            return field(px, z) && !bund(px, z) && crop(px, z) == 0 && hash(px, z, 14) < 80;
        }

        /** The village gate (牌坊) over the main street at {@link #GATE_Z} (dz from it): red posts, beams, three grey-tiled roofs, the name. */
        @Nullable
        BlockState gate(int x, int y, int dz) {
            int ax = Math.abs(x);
            boolean post = dz == 0 && (ax == 3 || ax == 6);
            if (post && y <= (ax == 3 ? 5 : 4)) return y == 1 ? p.base : p.pillar;
            // the lintels: a beam over the way under the middle roof, a lattice frieze, the name-board in its middle
            if (dz == 0 && y == 5 && ax <= 2) return p.beam.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            if (dz == 0 && y == 4 && ax <= 2) return ax == 0 ? s(Blocks.STRIPPED_DARK_OAK_WOOD) : rail(p.fence, true);
            if (dz == 0 && y == 4 && (ax == 4 || ax == 5)) return p.beam.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            // the middle roof over the posts at 3, the side roofs over those at 6, their ends turned up
            if (ax <= 4 && y == 6) {
                if (dz == 0) return ax == 4 ? stair(p.roofStairs, x < 0 ? Direction.EAST : Direction.WEST) : p.roofFull;
                return stair(p.roofStairs, dz < 0 ? Direction.SOUTH : Direction.NORTH);
            }
            if (ax <= 3 && y == 7 && dz == 0) return ax == 3 ? stair(p.roofStairs, x < 0 ? Direction.WEST : Direction.EAST, Half.TOP, StairsShape.STRAIGHT)
                    : slab(p.roofSlab, SlabType.BOTTOM);
            if (ax >= 5 && ax <= 7 && y == 5) {
                if (dz == 0) return ax == 7 ? stair(p.roofStairs, x < 0 ? Direction.EAST : Direction.WEST) : p.roofFull;
                return stair(p.roofStairs, dz < 0 ? Direction.SOUTH : Direction.NORTH);
            }
            if (ax == 7 && y == 6 && dz == 0) return stair(p.roofStairs, x < 0 ? Direction.WEST : Direction.EAST, Half.TOP, StairsShape.STRAIGHT);
            return null;
        }

        // ── The buildings, each in its own frame (facing south) ─────────

        @Nullable
        BlockState building(Building b, int u, int y, int v) {
            return switch (b.kind) {
                case COURTYARD -> courtyard(b, u, y, v);
                case INN -> inn(b, u, y, v);
                case HERBALIST -> herbalist(b, u, y, v);
                case SMITHY -> smithy(b, u, y, v);
                case SHRINE -> shrine(b, u, y, v);
                case COTTAGE -> cottage(b, u, y, v);
                case STALL -> stall(b, u, y, v);
                case WELL -> well(u, y, v);
            };
        }

        /**
         * A hall's walls, from y 1 to {@code h}: a stone plinth course, whitewashed wall, a dark beam
         * at the top; red pillars at its corners and every fourth block along it; lattice windows.
         * Null within and without.
         */
        @Nullable
        BlockState walls(int u, int y, int v, int u0, int u1, int v0, int v1, int h, boolean windows) {
            if (u < u0 || u > u1 || v < v0 || v > v1 || y < 1 || y > h) return null;
            boolean eu = u == u0 || u == u1, ev = v == v0 || v == v1;
            if (!eu && !ev) return null;
            boolean corner = eu && ev;
            int along = ev ? u - u0 : v - v0, len = ev ? u1 - u0 : v1 - v0;
            boolean pillar = corner || along % 4 == 0 && along != len && along != 0 || along == len;
            if (pillar) return p.pillar;
            if (y == h) return p.beam.setValue(RotatedPillarBlock.AXIS, ev ? Direction.Axis.X : Direction.Axis.Z);
            if (y == 1) return p.base;
            if (windows && (y == 2 || y == 3) && along % 4 == 2 && h >= 4) return rail(p.fence, ev);
            return p.wall;
        }

        /**
         * A hip roof's tier {@code t} (0 at the eaves) over the box (u0, v0)..(u1, v1): each tier a
         * block in on every side, stairs round its rim (their low side out), tiles within; along the
         * ridge a ridge of slabs with raised ends; at the eaves' corners, the eave turns up. Null off it.
         */
        @Nullable
        BlockState roof(int u, int t, int v, int u0, int u1, int v0, int v1) {
            if (t < 0) return null;
            int a0 = u0 + t, a1 = u1 - t, b0 = v0 + t, b1 = v1 - t;
            // the upturned eaves: the corners of the lowest tier, raised a block, an upside-down stair turned out
            if (t == 1 && (u == u0 || u == u1) && (v == v0 || v == v1)) {
                return stair(p.roofStairs, u == u0 ? Direction.WEST : Direction.EAST, Half.TOP, StairsShape.STRAIGHT);
            }
            if (t > ROOF_TIERS) {
                // over a broad roof's flat crown, the ridge along its length, its ends turned up
                int c0 = u0 + ROOF_TIERS, c1 = u1 - ROOF_TIERS, d0 = v0 + ROOF_TIERS, d1 = v1 - ROOF_TIERS;
                if (t != ROOF_TIERS + 1 || c1 - c0 <= 1 || d1 - d0 <= 1) return null;
                boolean alongU = c1 - c0 >= d1 - d0;
                int mid = alongU ? (d0 + d1) / 2 : (c0 + c1) / 2;
                if (alongU ? v != mid || u < c0 + 1 || u > c1 - 1 : u != mid || v < d0 + 1 || v > d1 - 1) return null;
                boolean end = alongU ? (u == c0 + 1 || u == c1 - 1) : (v == d0 + 1 || v == d1 - 1);
                return end ? stair(p.roofStairs, alongU ? (u == c0 + 1 ? Direction.WEST : Direction.EAST) : (v == d0 + 1 ? Direction.NORTH : Direction.SOUTH),
                        Half.TOP, StairsShape.STRAIGHT) : p.roofFull;
            }
            if (a0 > a1 || b0 > b1 || u < a0 || u > a1 || v < b0 || v > b1) return null;
            if (a1 - a0 <= 1 || b1 - b0 <= 1) {
                // the ridge: tiles, a ridge-end raised at either end
                boolean alongU = a1 - a0 > b1 - b0;
                boolean end = alongU ? (u == a0 || u == a1) : (v == b0 || v == b1);
                return end ? stair(p.roofStairs, alongU ? (u == a0 ? Direction.WEST : Direction.EAST) : (v == b0 ? Direction.NORTH : Direction.SOUTH),
                        Half.TOP, StairsShape.STRAIGHT) : slab(p.roofSlab, SlabType.BOTTOM);
            }
            boolean w = u == a0, e = u == a1, n = v == b0, so = v == b1;
            if (t == 0 && (w || e) && (n || so)) return slab(p.roofSlab, SlabType.TOP);
            if ((w || e) && (n || so)) {
                Direction f = n ? Direction.SOUTH : Direction.NORTH;
                return stair(p.roofStairs, f, Half.BOTTOM, (n == w) ? StairsShape.OUTER_RIGHT : StairsShape.OUTER_LEFT);
            }
            if (n) return stair(p.roofStairs, Direction.SOUTH);
            if (so) return stair(p.roofStairs, Direction.NORTH);
            if (w) return stair(p.roofStairs, Direction.EAST);
            if (e) return stair(p.roofStairs, Direction.WEST);
            return p.roofFull;
        }

        /** The roof over a hall whose walls are (u0, v0)..(u1, v1) and {@code h} high: its eaves a block out all round. */
        @Nullable
        BlockState hallRoof(int u, int y, int v, int u0, int u1, int v0, int v1, int h) {
            return roof(u, y - h - 1, v, u0 - 1, u1 + 1, v0 - 1, v1 + 1);
        }

        BlockState floor() {
            return s(p.planks);
        }

        /**
         * The courtyard house (15 x 15): the compound wall, a gatehouse in its front; the main hall at
         * its head (u 1-13, v 0-4), wing rooms either side (u 1-4 and 10-13, v 7-11); the paved court.
         */
        @Nullable
        BlockState courtyard(Building b, int u, int y, int v) {
            boolean hall = v <= 4 && u >= 1 && u <= 13;
            boolean westWing = u >= 1 && u <= 4 && v >= 7 && v <= 11, eastWing = u >= 10 && u <= 13 && v >= 7 && v <= 11;
            // the roofs
            if (y >= 5) {
                BlockState r = hallRoof(u, y, v, 1, 13, 0, 4, 4);
                if (r != null) return r;
            }
            if (y >= 4 && v >= 6 && v <= 12 && (u <= 5 || u >= 9)) {
                int w0 = u <= 7 ? 1 : 10, w1 = u <= 7 ? 4 : 13;
                BlockState r = hallRoof(u, y, v, w0, w1, 7, 11, 3);
                if (r != null) return r;
            }
            if (y == 0) {
                if (hall || westWing || eastWing) return floor();
                return hash(u * 7 + b.salt, v, 2) < 75 ? p.pave : p.pave2;
            }
            // the compound wall: plinth, whitewash, a tiled coping
            boolean rim = u == 0 || u == 14 || v == 14 || v == 0;
            if (rim && !(v <= 4 && u >= 1 && u <= 13)) {
                if (v == 14 && u >= 6 && u <= 8) return gatehouse(u - 7, y);
                if (y == 1) return p.base;
                if (y <= 3) return p.wall;
                if (y == 4) return slab(p.roofSlab, SlabType.BOTTOM);
                return null;
            }
            // the main hall
            BlockState w = walls(u, y, v, 1, 13, 0, 4, 4, true);
            if (w != null) {
                if (v == 4 && u == 7 && y <= 2) return door(p.door, Direction.SOUTH, y == 2, false);
                if (v == 4 && (u == 6 || u == 8) && y == 3) return s(Blocks.LANTERN);
                return w;
            }
            if (hall) {
                if (y == 1 && (u == 3 || u == 11) && v == 1) return bed(Blocks.RED_BED, Direction.NORTH, true);
                if (y == 1 && (u == 3 || u == 11) && v == 2) return bed(Blocks.RED_BED, Direction.NORTH, false);
                if (u == 7 && v == 1) return y == 1 ? s(Blocks.CHISELED_STONE_BRICKS) : y == 2 ? candles(3) : null; // the family altar
                if (u == 7 && v == 2 && y == 1) return s(p.fence);
                if (u == 7 && v == 2 && y == 2) return s(Blocks.SPRUCE_PRESSURE_PLATE);
                if ((u == 6 || u == 8) && v == 2 && y == 1) return stair(p.stairs, u == 6 ? Direction.EAST : Direction.WEST);
                if (u == 12 && v == 1 && y == 1) return null; // the chest (see the plan's chests)
                if (u == 5 && v == 1 && y == 1) return s(Blocks.BARREL);
                if ((u == 4 || u == 10) && v == 2 && y == 4) return hanging(Blocks.LANTERN);
                if (u == 9 && v == 1 && y == 1) return s(Blocks.LOOM);
                return s(Blocks.AIR);
            }
            // the wing rooms
            for (int k = 0; k < 2; k++) {
                int w0 = k == 0 ? 1 : 10, w1 = k == 0 ? 4 : 13;
                BlockState ww = walls(u, y, v, w0, w1, 7, 11, 3, true);
                int doorU = k == 0 ? 4 : 10;
                if (ww != null) {
                    if (u == doorU && v == 9 && y <= 2) return door(p.door, k == 0 ? Direction.EAST : Direction.WEST, y == 2, false);
                    return ww;
                }
                if (u > w0 && u < w1 && v > 7 && v < 11) {
                    int bu = k == 0 ? 2 : 12;
                    if (u == bu && v == 8 && y == 1) return bed(Blocks.BLUE_BED, Direction.NORTH, true);
                    if (u == bu && v == 9 && y == 1) return bed(Blocks.BLUE_BED, Direction.NORTH, false);
                    if (u == (k == 0 ? 3 : 11) && v == 10 && y == 1) return s(k == 0 ? Blocks.CRAFTING_TABLE : Blocks.COMPOSTER);
                    return s(Blocks.AIR);
                }
            }
            // the court: a water vat, potted flowers, a little tree
            if (u == 7 && v == 9) return y == 1 ? Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3) : null;
            if ((u == 6 || u == 8) && v == 6 && y == 1) return s(u == 6 ? Blocks.POTTED_RED_TULIP : Blocks.POTTED_AZALEA);
            if (u == 6 && v == 12 && y == 1) return s(Blocks.POTTED_FERN);
            if (u == 8 && v == 12) {
                BlockState t = y <= 2 ? p.log : null;
                if (t != null) return t;
            }
            if (Math.abs(u - 8) + Math.abs(v - 12) <= 2 && y >= 3 && y <= 4 && !(u == 8 && v == 12 && y == 3)) {
                return v >= 13 || u >= 10 ? null : variant == Variant.OASIS ? leaves(Blocks.JUNGLE_LEAVES) : leaves(Blocks.FLOWERING_AZALEA_LEAVES);
            }
            if (u == 8 && v == 12 && y == 3) return leaves(Blocks.FLOWERING_AZALEA_LEAVES);
            return s(Blocks.AIR);
        }

        /** The courtyard's gatehouse (du -1..1 from its middle): red posts, a door, a little roof over it. */
        @Nullable
        BlockState gatehouse(int du, int y) {
            if (du != 0) {
                if (y <= 3) return p.pillar;
                if (y == 4) return p.beam.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                if (y == 5) return stair(p.roofStairs, du < 0 ? Direction.EAST : Direction.WEST);
                return null;
            }
            if (y <= 2) return door(p.door, Direction.SOUTH, y == 2, false);
            if (y == 3) return p.wall;
            if (y == 4) return p.beam.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            if (y == 5) return slab(p.roofSlab, SlabType.BOTTOM);
            return null;
        }

        /**
         * The inn (13 x 11): two storeys. Its ground floor an open-fronted teahouse behind red pillars:
         * tables and stools, the counter and its wine jars, a stair up; above, a balcony across its front
         * and the guest room, three beds; a hip roof over all, lanterns at its eaves.
         */
        @Nullable
        BlockState inn(Building b, int u, int y, int v) {
            BlockState r = hallRoof(u, y, v, 0, 12, 0, 8, 8);
            if (y >= 9) return r;
            if (y == 0) return floor();
            boolean edgeU = u == 0 || u == 12;
            // the ground floor
            if (y <= 4) {
                if (v == 10) {
                    // the open front: pillars, a lattice frieze, lanterns under the balcony
                    if (u % 4 == 0) return p.pillar;
                    if (y == 4) return u == 6 ? p.beam.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X) : rail(p.fence, true);
                    if (y == 3 && (u == 2 || u == 10)) return hanging(Blocks.LANTERN);
                    return s(Blocks.AIR);
                }
                BlockState w = walls(u, y, v, 0, 12, 0, 10, 4, true);
                if (w != null) return w;
                // the stair up the west wall, from the front toward the back
                if (u == 1 && v >= 2 && v <= 5 && y == 6 - v) return stair(p.stairs, Direction.NORTH);
                if (u == 1 && v >= 2 && v <= 5 && y < 6 - v) return s(p.planks);
                // the counter along the back, wine jars on it and behind it
                if (v == 3 && u >= 8 && u <= 11) return y == 1 ? s(Blocks.BARREL) : y == 2 && u % 2 == 0 ? s(Blocks.DECORATED_POT) : y == 2 ? s(Blocks.CANDLE) : s(Blocks.AIR);
                if (v == 1 && u >= 8 && u <= 10 && y <= 2) return s(Blocks.BARREL);
                // tables and stools
                for (int[] t : new int[][]{{4, 6}, {8, 7}, {4, 8}}) {
                    if (u == t[0] && v == t[1]) return y == 1 ? s(p.fence) : y == 2 ? s(Blocks.SPRUCE_PRESSURE_PLATE) : s(Blocks.AIR);
                    if (Math.abs(u - t[0]) == 1 && v == t[1] && y == 1) return stair(p.stairs, u < t[0] ? Direction.EAST : Direction.WEST);
                }
                if ((u == 4 || u == 8) && v == 5 && y == 4) return hanging(Blocks.LANTERN);
                return s(Blocks.AIR);
            }
            // the upper floor (y 5) and the guest room (y 6-8), the balcony before it
            if (y == 5) {
                if (u == 1 && v >= 2 && v <= 5) return s(Blocks.AIR); // the stairwell
                if (v >= 9) return v == 10 && (u == 0 || u == 12) ? p.pillar : s(p.slab).setValue(SlabBlock.TYPE, SlabType.TOP);
                return floor();
            }
            if (v >= 9) {
                if (v == 10 && y == 6 && !edgeU) return rail(p.fence, true);
                if (v == 10 && edgeU && y == 6) return s(p.fence);
                if (v == 9 && y == 8 && (u == 3 || u == 9)) return hanging(Blocks.LANTERN);
                return s(Blocks.AIR);
            }
            BlockState w = walls(u, y - 4, v, 0, 12, 0, 8, 4, true);
            if (w != null) {
                if (v == 8 && u == 6 && y <= 7) return door(p.door, Direction.SOUTH, y == 7, false);
                if (y - 4 == 1) return s(p.planks); // the upper floor's walls stand on timber, not stone
                return w;
            }
            if (u == 1 && v >= 2 && v <= 5 && y == 6 - v) return stair(p.stairs, Direction.NORTH);
            if (y == 6 && v == 1 && (u == 4 || u == 7 || u == 10)) return bed(Blocks.RED_BED, Direction.NORTH, true);
            if (y == 6 && v == 2 && (u == 4 || u == 7 || u == 10)) return bed(Blocks.RED_BED, Direction.NORTH, false);
            if (y == 6 && v == 1 && u == 11) return null; // the chest
            if (y == 8 && v == 4 && (u == 5 || u == 9)) return hanging(Blocks.LANTERN);
            return s(Blocks.AIR);
        }

        /**
         * The herbalist's (9 x 8): a shop open on the street behind a counter, its back wall shelved
         * with jars and drying herbs, a brewing stand and a cauldron; a bed at the back.
         */
        @Nullable
        BlockState herbalist(Building b, int u, int y, int v) {
            BlockState r = hallRoof(u, y, v, 0, 8, 0, 7, 4);
            if (y >= 5) return r;
            if (y == 0) return floor();
            if (v == 7) {
                if (u == 0 || u == 8 || u == 4) return p.pillar;
                if (y == 1) return u == 5 ? door(p.door, Direction.SOUTH, false, false) : slab(p.slab, SlabType.TOP);
                if (y == 2 && u == 5) return door(p.door, Direction.SOUTH, true, false);
                if (y == 4) return p.beam.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                if (y == 3 && (u == 2 || u == 6)) return s(Blocks.AIR);
                return s(Blocks.AIR);
            }
            BlockState w = walls(u, y, v, 0, 8, 0, 7, 4, true);
            if (w != null) return w;
            // the shelves of jars and herbs
            if (v == 1 && u >= 1 && u <= 7) {
                if (y == 1) return u % 2 == 1 ? s(Blocks.BARREL) : s(Blocks.CHISELED_BOOKSHELF);
                if (y == 2) return u % 3 == 0 ? s(Blocks.DECORATED_POT) : s(new Block[]{Blocks.POTTED_FERN, Blocks.POTTED_DEAD_BUSH, Blocks.POTTED_BROWN_MUSHROOM,
                        Blocks.POTTED_RED_MUSHROOM, Blocks.POTTED_CRIMSON_ROOTS}[Math.floorMod(u + b.salt, 5)]);
                if (y == 3) return s(p.slab).setValue(SlabBlock.TYPE, SlabType.TOP);
                return s(Blocks.AIR);
            }
            if (u == 2 && v == 3 && y == 1) return Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 2);
            if (u == 4 && v == 3 && y == 1) return s(Blocks.BREWING_STAND);
            if (u == 7 && v == 3 && y == 1) return bed(Blocks.GREEN_BED, Direction.NORTH, true);
            if (u == 7 && v == 4 && y == 1) return bed(Blocks.GREEN_BED, Direction.NORTH, false);
            if (u == 4 && v == 4 && y == 4) return hanging(Blocks.LANTERN);
            // herbs drying, hung from the beams
            if (y == 4 && v == 5 && (u == 2 || u == 6)) return s(Blocks.HANGING_ROOTS);
            return s(Blocks.AIR);
        }

        /**
         * The smithy (9 x 8): an open shed on red posts round its forge (a fire in a stone hearth, a
         * chimney through the roof), the anvil, a grindstone, the quenching trough; the smith's room
         * behind it.
         */
        @Nullable
        BlockState smithy(Building b, int u, int y, int v) {
            // the chimney
            if (u == 1 && v == 4 && y >= 2 && y <= 8) return y >= 5 ? s(Blocks.BRICKS) : s(Blocks.STONE_BRICKS);
            BlockState r = hallRoof(u, y, v, 0, 8, 0, 7, 4);
            if (y >= 5) return r;
            if (y == 0) return v <= 3 ? floor() : u <= 2 && v <= 6 ? s(Blocks.COBBLESTONE) : s(Blocks.PACKED_MUD);
            // the back room
            if (v <= 3) {
                BlockState w = walls(u, y, v, 0, 8, 0, 3, 4, true);
                if (w != null) {
                    if (v == 3 && u == 5 && y <= 2) return door(p.door, Direction.SOUTH, y == 2, false);
                    return w;
                }
                if (u == 3 && v == 1 && y == 1) return bed(Blocks.BROWN_BED, Direction.EAST, true);
                if (u == 2 && v == 1 && y == 1) return bed(Blocks.BROWN_BED, Direction.EAST, false);
                if (u == 6 && v == 1 && y == 1) return null; // the chest
                if (u == 7 && v == 2 && y == 1) return s(Blocks.SMITHING_TABLE);
                return s(Blocks.AIR);
            }
            // the shed's posts
            if ((u == 0 || u == 8) && v == 7) return y == 4 ? p.beam : p.pillar;
            if (y == 4 && (v == 7 || u == 0 || u == 8)) return p.beam.setValue(RotatedPillarBlock.AXIS, v == 7 ? Direction.Axis.X : Direction.Axis.Z);
            // the forge: a fire in a hearth of stone, the chimney over it
            if (u <= 2 && v >= 4 && v <= 6 && y == 1) {
                if (u == 1 && v == 5) return Blocks.CAMPFIRE.defaultBlockState();
                return s(Blocks.STONE_BRICKS);
            }
            if (u == 1 && v == 5 && y == 2) return s(Blocks.AIR);
            if (u <= 2 && v >= 4 && v <= 6 && y == 2 && !(u == 1 && v == 4)) return s(Blocks.STONE_BRICK_WALL);
            if (u == 4 && v == 5 && y == 1) return s(Blocks.ANVIL);
            if (u == 6 && v == 5 && y == 1) return s(Blocks.GRINDSTONE);
            if (u == 7 && v == 4 && y == 1) return s(Blocks.BLAST_FURNACE);
            if (u == 7 && v == 6 && y == 1) return Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3);
            if (u == 5 && v == 4 && y == 3) return hanging(Blocks.LANTERN);
            return s(Blocks.AIR);
        }

        /**
         * The shrine to the Earth God (9 x 9): a low wall round its yard, an incense burner in the yard;
         * the little hall at the back, open on red pillars, the altar in it, candles, the god's red
         * hanging behind; a bed for the monk who keeps it.
         */
        @Nullable
        BlockState shrine(Building b, int u, int y, int v) {
            if (y >= 4) {
                BlockState r = hallRoof(u, y, v, 1, 7, 1, 4, 3);
                return r;
            }
            if (y == 0) return v <= 4 && u >= 1 && u <= 7 ? p.pave2 : p.pave;
            boolean rim = u == 0 || u == 8 || v == 0 || v == 8;
            if (rim) {
                if (v == 8 && u >= 3 && u <= 5) return u == 4 ? s(Blocks.AIR) : y <= 2 ? p.pillar : s(Blocks.AIR);
                if (y == 1) return p.base;
                if (y == 2) return p.wall;
                if (y == 3) return slab(p.roofSlab, SlabType.BOTTOM);
                return null;
            }
            // the hall, open to the yard on its pillars
            if (v == 4 && u >= 1 && u <= 7) {
                if (u == 1 || u == 7 || u == 3 || u == 5) return p.pillar;
                if (y == 3) return p.beam.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                return s(Blocks.AIR);
            }
            BlockState w = walls(u, y, v, 1, 7, 1, 4, 3, false);
            if (w != null) return w;
            if (v >= 2 && v <= 3 && u >= 2 && u <= 6) {
                if (u == 4 && v == 2) return y == 1 ? s(Blocks.CHISELED_STONE_BRICKS) : y == 2 ? s(Blocks.YELLOW_GLAZED_TERRACOTTA) : s(Blocks.AIR);
                if ((u == 3 || u == 5) && v == 2) return y == 1 ? s(Blocks.POLISHED_GRANITE) : y == 2 ? candles(2) : s(Blocks.AIR);
                if (u == 2 && v == 2 && y == 1) return bed(Blocks.YELLOW_BED, Direction.NORTH, true);
                if (u == 2 && v == 3 && y == 1) return bed(Blocks.YELLOW_BED, Direction.NORTH, false);
                if (u == 4 && v == 3 && y == 1) return s(Blocks.RED_CARPET);
                return s(Blocks.AIR);
            }
            // the incense burner in the yard, the incense in it; two old cypresses
            if (u == 4 && v == 6) return y == 1 ? s(Blocks.POLISHED_ANDESITE) : y == 2 ? s(Blocks.POTTED_DEAD_BUSH) : s(Blocks.AIR);
            if ((u == 1 || u == 7) && v == 7) return y <= 3 ? leaves(variant == Variant.OASIS ? Blocks.JUNGLE_LEAVES : Blocks.SPRUCE_LEAVES) : null;
            return s(Blocks.AIR);
        }

        /** A cottage (9 x 8): one hall with a door and lattice windows, a bed, a table, a stove, a hayloft corner; the yard before it. */
        @Nullable
        BlockState cottage(Building b, int u, int y, int v) {
            BlockState r = hallRoof(u, y, v, 0, 8, 1, 6, 4);
            if (y >= 5) return r;
            if (y == 0) return v >= 1 && v <= 6 ? floor() : null;
            BlockState w = walls(u, y, v, 0, 8, 1, 6, 4, true);
            if (w != null) {
                if (v == 6 && u == 4 && y <= 2) return door(p.door, Direction.SOUTH, y == 2, false);
                if (v == 6 && (u == 3 || u == 5) && y == 3) return s(Blocks.LANTERN);
                return w;
            }
            if (v >= 2 && v <= 5 && u >= 1 && u <= 7) {
                if (u == 2 && v == 2 && y == 1) return bed(Blocks.WHITE_BED, Direction.NORTH, true);
                if (u == 2 && v == 3 && y == 1) return bed(Blocks.WHITE_BED, Direction.NORTH, false);
                if (u == 6 && v == 2 && y == 1) return s(Blocks.SMOKER);
                if (u == 7 && v == 2 && y <= 2) return s(Blocks.HAY_BLOCK);
                if (u == 7 && v == 3 && y == 1) return s(Blocks.HAY_BLOCK);
                if (u == 5 && v == 4 && y == 1) return s(p.fence);
                if (u == 5 && v == 4 && y == 2) return s(Blocks.SPRUCE_PRESSURE_PLATE);
                if (u == 4 && v == 3 && y == 4) return hanging(Blocks.LANTERN);
                return s(Blocks.AIR);
            }
            // the yard before it: a tub and a woodpile
            if (v == 7 && u == 1 && y == 1) return s(Blocks.BARREL);
            if (v == 0 && u >= 6 && u <= 7 && y == 1) return log(Blocks.OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            return v == 0 || v == 7 ? null : s(Blocks.AIR);
        }

        /** A market stall (3 x 3): four posts, a coloured awning over trapdoor boards, the counter and goods along its front. */
        @Nullable
        BlockState stall(Building b, int u, int y, int v) {
            if (y == 0) return null;
            boolean post = (u == 0 || u == 2) && (v == 0 || v == 2);
            if (post && y <= 2) return s(p.fence);
            if (y == 3) return trapdoorTop(p.trapdoor);
            if (y == 4) {
                Block[] cloth = {Blocks.RED_CARPET, Blocks.BLUE_CARPET, Blocks.YELLOW_CARPET, Blocks.GREEN_CARPET, Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET};
                return s(cloth[Math.floorMod(b.salt, cloth.length)]);
            }
            if (v == 2 && u == 1 && y == 1) {
                Block[] goods = {Blocks.BARREL, Blocks.HAY_BLOCK, Blocks.MELON, Blocks.PUMPKIN, Blocks.CHISELED_BOOKSHELF, Blocks.DECORATED_POT};
                return s(goods[Math.floorMod(b.salt * 7, goods.length)]);
            }
            if (v == 2 && u == 1 && y == 2) {
                Block[] top = {Blocks.POTTED_RED_TULIP, Blocks.CANDLE, Blocks.FLOWER_POT, Blocks.POTTED_FERN, Blocks.AIR, Blocks.POTTED_BAMBOO};
                return s(top[Math.floorMod(b.salt * 3, top.length)]);
            }
            return s(Blocks.AIR);
        }

        /** The well (3 x 3): a stone curb round the water, two posts and a little roof, the bucket's chain. */
        @Nullable
        BlockState well(int u, int y, int v) {
            boolean mid = u == 1 && v == 1;
            if (y <= 0) return mid ? (y >= -3 ? s(Blocks.WATER) : null) : y >= -3 ? s(Blocks.STONE_BRICKS) : null;
            if (mid) return y == 3 ? s(Blocks.CHAIN) : y == 4 ? slab(p.roofSlab, SlabType.TOP) : s(Blocks.AIR);
            if ((u == 0 || u == 2) && v == 1 && y <= 3 && y >= 2) return s(p.fence);
            if (y == 1) {
                // the curb: a ring of low stone wall, each piece joined to its neighbours round the ring
                var w = net.minecraft.world.level.block.state.properties.WallSide.LOW;
                BlockState c = s(Blocks.STONE_BRICK_WALL).setValue(net.minecraft.world.level.block.WallBlock.UP, (u == 0 || u == 2) && v == 1);
                if (v == 0 || v == 2) {
                    if (u < 2) c = c.setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL, w);
                    if (u > 0) c = c.setValue(net.minecraft.world.level.block.WallBlock.WEST_WALL, w);
                }
                if (u == 0 || u == 2) {
                    if (v < 2) c = c.setValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL, w);
                    if (v > 0) c = c.setValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL, w);
                }
                return c;
            }
            if (y == 4) return u == 1 ? slab(p.roofSlab, SlabType.TOP) : stair(p.roofStairs, u == 0 ? Direction.EAST : Direction.WEST, Half.TOP, StairsShape.STRAIGHT);
            return s(Blocks.AIR);
        }
    }

    /** The plan of a village of this kind, from its seed. */
    public static Plan plan(Variant variant, long seed) {
        return new Plan(variant, seed);
    }
}
