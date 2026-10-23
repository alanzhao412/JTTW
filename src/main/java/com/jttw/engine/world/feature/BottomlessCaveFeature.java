package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Digs the Bottomless Cave under each of its regions, chunk by chunk: the {@link Labyrinth}'s
 * tunnels, chambers, stairs, hub, palace and shrine; the shaft down from the surface; and the
 * sinkhole its mouth opens in. Everything it puts is worked out from the block's place alone, so
 * the chunks of one region agree whichever is made first, and a chunk made later never undoes
 * another's work: so the palace's flowers and lanterns are laid here too, not by the lair.
 *
 * <ul>
 *   <li>Each level's heights within the labyrinth are its own: rock where it is not open, so no
 *       vanilla cave, water or lava breaks into it (the rock it finds is kept where it is solid;
 *       the open ways are lined with the burrow's earth, which turns to stone deeper down);</li>
 *   <li>the tunnels are lit here and there by a lantern, a few of them pale soul-lanterns; the
 *       stairs by a light in their pillar; the chambers each by a lantern; the palace well;</li>
 *   <li>the shaft is lined with rock all the way up, its stair laid in mud brick, a lantern once a turn.</li>
 * </ul>
 * Runs first among the zone's features (raw generation).
 */
public class BottomlessCaveFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = Zones.BOTTOMLESS;
    private static final int H = Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1;

    public BottomlessCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The labyrinth under the region this chunk lies in, if the region is there (its host was found); else null. */
    public static Labyrinth labyrinthFor(WorldGenLevel level, int x, int z) {
        int[] c = ZONE.centre(level.getSeed(), x, z);
        if (c == null) return null;
        // asked of the biome source itself: the centre's chunk may be far outside what this chunk's generation may read
        var chunks = level.getLevel().getChunkSource();
        if (!chunks.getGenerator().getBiomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(c[0]),
                net.minecraft.core.QuartPos.fromBlock(Labyrinth.FLOOR[Labyrinth.LEVELS - 1]), net.minecraft.core.QuartPos.fromBlock(c[1]),
                chunks.randomState().sampler()).is(ZONE.biome())) return null;
        return Labyrinth.of(level.getSeed(), c[0], c[1]);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = ZONE.centre(level.getSeed(), x0 + 8, z0 + 8);
        if (c == null) return false;
        int reach = Labyrinth.HALF + 2;
        if (x0 + 15 < c[0] - reach || x0 > c[0] + reach || z0 + 15 < c[1] - reach || z0 > c[1] + reach) return false;
        Labyrinth lab = labyrinthFor(level, x0 + 8, z0 + 8);
        if (lab == null) return false;
        build(level, lab, x0, z0);
        return true;
    }

    /** Lays the labyrinth's part in the chunk starting at (x0, z0) (the gallery lays one in a flat world). */
    public static void build(WorldGenLevel level, Labyrinth lab, int x0, int z0) {
        // what the labyrinth holds in this chunk's columns and the ring round it (for its walls' faces)
        byte[][][] codes = new byte[18][18][H];
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) lab.column(x0 + i - 1, z0 + j - 1, codes[i][j]);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        long seed = level.getSeed();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            byte[] col = codes[dx + 1][dz + 1];
            for (int y = Labyrinth.Y_MIN; y <= Labyrinth.Y_MAX; y++) {
                byte code = col[y - Labyrinth.Y_MIN];
                if (code == Labyrinth.UNTOUCHED) continue;
                p.set(x, y, z);
                if (code == Labyrinth.ROCK) {
                    BlockState here = level.getBlockState(p);
                    int face = face(codes, dx + 1, dz + 1, y);
                    if (face != 0) level.setBlock(p, skin(lab, seed, x, y, z, face), Block.UPDATE_CLIENTS);
                    else if (!solid(here)) level.setBlock(p, fill(y), Block.UPDATE_CLIENTS);
                    continue;
                }
                level.setBlock(p, openBlock(code, y), Block.UPDATE_CLIENTS);
            }
            shaft(level, lab, x, z, p);
            decorate(level, lab, seed, x, z, col, p);
        }
    }

    private static boolean solid(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && s.blocksMotion();
    }

    /** Which way a rock block faces something open: 1 floor (open over it), 2 ceiling (open under it), 3 wall; 0 none. */
    private static int face(byte[][][] codes, int i, int j, int y) {
        int k = y - Labyrinth.Y_MIN;
        if (k + 1 < H && Labyrinth.open(codes[i][j][k + 1])) return 1;
        if (k > 0 && Labyrinth.open(codes[i][j][k - 1])) return 2;
        if (Labyrinth.open(codes[i + 1][j][k]) || Labyrinth.open(codes[i - 1][j][k])
                || Labyrinth.open(codes[i][j + 1][k]) || Labyrinth.open(codes[i][j - 1][k])) return 3;
        return 0;
    }

    private static BlockState fill(int y) {
        return (y < 0 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
    }

    /** The face of the rock about the open ways: the burrow's earth above, stone and deepslate deeper, the palace's and the shrine's own. */
    private static BlockState skin(Labyrinth lab, long seed, int x, int y, int z, int face) {
        int k = Labyrinth.levelAt(y);
        double n = Labyrinth.noise(seed, x, y, z);
        Labyrinth.Room room = k < 0 ? null : nearRoom(lab, k, x, z);
        if (room != null && room.kind() == Labyrinth.PALACE) {
            if (face == 1) return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            if (face == 2) return Blocks.DEEPSLATE_TILES.defaultBlockState();
            // lamps set in the walls, every four blocks
            if (y == Labyrinth.FLOOR[k] + 5 && Math.floorMod(x - lab.cx + z - lab.cz, 4) == 0) return Blocks.SHROOMLIGHT.defaultBlockState();
            return n < 0.12 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (room != null && room.kind() == Labyrinth.SHRINE) {
            if (face == 1) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
            return Blocks.MUD_BRICKS.defaultBlockState();
        }
        if (face == 1) {
            // floors: trodden earth, all of the dirt kind (so petals and flowers take on them)
            if (k == 2) return (n < 0.45 ? Blocks.ROOTED_DIRT : n < 0.75 ? Blocks.COARSE_DIRT : Blocks.MUD).defaultBlockState();
            return (n < 0.5 ? Blocks.COARSE_DIRT : n < 0.8 ? Blocks.ROOTED_DIRT : Blocks.DIRT).defaultBlockState();
        }
        if (k <= 0 && y >= Labyrinth.FLOOR[0] - Labyrinth.SLAB_BELOW) {
            return (n < 0.55 ? Blocks.PACKED_MUD : n < 0.8 ? Blocks.ROOTED_DIRT : Blocks.DIRT).defaultBlockState();
        }
        if (k == 1) return (n < 0.4 ? Blocks.PACKED_MUD : n < 0.7 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
        if (k == 2) return (n < 0.45 ? Blocks.DEEPSLATE : n < 0.75 ? Blocks.TUFF : Blocks.PACKED_MUD).defaultBlockState();
        return (n < 0.6 ? Blocks.STONE : Blocks.PACKED_MUD).defaultBlockState(); // the shaft's lining, and between levels
    }

    /** The room at (x, z) on level k, or one beside it (for its walls). */
    private static Labyrinth.Room nearRoom(Labyrinth lab, int k, int x, int z) {
        Labyrinth.Room r = lab.roomAt(k, x, z);
        if (r != null) return r;
        for (int[] d : Labyrinth.DIRS) {
            r = lab.roomAt(k, x + d[0], z + d[1]);
            if (r != null && r.kind() >= Labyrinth.PALACE) return r;
        }
        int[] cell = lab.cellAt(x, z);
        return cell == null ? null : lab.level(k).roomOf(cell[0], cell[1]) != null
                && lab.level(k).roomOf(cell[0], cell[1]).kind() >= Labyrinth.PALACE ? lab.level(k).roomOf(cell[0], cell[1]) : null;
    }

    private static BlockState openBlock(byte code, int y) {
        boolean deep = y < Labyrinth.FLOOR[1];
        return switch (code) {
            case Labyrinth.STEP_N, Labyrinth.STEP_E, Labyrinth.STEP_S, Labyrinth.STEP_W ->
                    (deep ? Blocks.TUFF_BRICK_STAIRS : Blocks.MUD_BRICK_STAIRS).defaultBlockState()
                            .setValue(StairBlock.FACING, facing(code - Labyrinth.STEP_N));
            case Labyrinth.PILLAR, Labyrinth.COVER -> (deep ? Blocks.TUFF_BRICKS : Blocks.MUD_BRICKS).defaultBlockState();
            case Labyrinth.CORE -> Blocks.SHROOMLIGHT.defaultBlockState();
            case Labyrinth.WATER -> Blocks.WATER.defaultBlockState();
            case Labyrinth.LANTERN -> Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case Labyrinth.SOUL_LANTERN -> Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case Labyrinth.HELIX -> Blocks.MUD_BRICKS.defaultBlockState();
            case Labyrinth.HELIX_SLAB -> Blocks.MUD_BRICK_SLAB.defaultBlockState();
            case Labyrinth.HELIX_LANTERN -> Blocks.LANTERN.defaultBlockState();
            default -> Blocks.CAVE_AIR.defaultBlockState();
        };
    }

    /** North, east, south, west, as the step codes count them. */
    static Direction facing(int dir) {
        return switch (dir) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    // ── The shaft and the sinkhole ──────────────────────────────────────

    /** The shaft above the labyrinth's heights in this column, up to the surface; the sinkhole round its mouth. */
    private static void shaft(WorldGenLevel level, Labyrinth lab, int x, int z, BlockPos.MutableBlockPos p) {
        int dx = x - lab.cx, dz = z - lab.cz;
        double r = Math.hypot(dx, dz);
        if (r > Labyrinth.DISH_RADIUS) return;
        int surface = level.getHeight(level instanceof net.minecraft.server.level.ServerLevel
                ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        // the sinkhole: the ground sinks toward the mouth
        int sink = r <= Labyrinth.MOUTH_RADIUS ? 0
                : (int) Math.round(3.5 * (Labyrinth.DISH_RADIUS - r) / (Labyrinth.DISH_RADIUS - Labyrinth.MOUTH_RADIUS));
        for (int y = Labyrinth.Y_MAX + 1; y <= surface; y++) {
            p.set(x, y, z);
            byte code;
            if (r <= Labyrinth.MOUTH_RADIUS) code = Labyrinth.shaftCode(dx, dz, y);
            else if (y <= surface - 6) code = Labyrinth.shaftCode(dx, dz, y);
            else code = y > surface - sink ? Labyrinth.AIR : Labyrinth.UNTOUCHED;
            if (code == Labyrinth.UNTOUCHED) continue;
            if (code == Labyrinth.ROCK) {
                if (!solid(level.getBlockState(p))) level.setBlock(p, fill(y), Block.UPDATE_CLIENTS);
                continue;
            }
            level.setBlock(p, code == Labyrinth.AIR && y > surface - 6 ? Blocks.AIR.defaultBlockState() : openBlock(code, y), Block.UPDATE_CLIENTS);
        }
        if (sink > 0 && r > Labyrinth.MOUTH_RADIUS) {
            p.set(x, surface - sink, z);
            if (solid(level.getBlockState(p))) {
                double n = Labyrinth.noise(level.getSeed(), x, 0, z);
                level.setBlock(p, (n < 0.5 ? Blocks.COARSE_DIRT : n < 0.8 ? Blocks.ROOTED_DIRT : Blocks.GRAVEL).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    // ── What stands in it ───────────────────────────────────────────────

    private static void decorate(WorldGenLevel level, Labyrinth lab, long seed, int x, int z, byte[] col, BlockPos.MutableBlockPos p) {
        for (int k = 0; k < Labyrinth.LEVELS; k++) {
            int f = Labyrinth.FLOOR[k];
            Labyrinth.Room room = lab.roomAt(k, x, z);
            byte floorAir = col[f - Labyrinth.Y_MIN];
            if (room == null) {
                // tunnels: roots hang from the earth above; now and then a cobweb in a corner
                if (floorAir == Labyrinth.AIR && k < 2) {
                    int top = f + Labyrinth.TUNNEL - 1;
                    if (col[top - Labyrinth.Y_MIN] == Labyrinth.AIR && Labyrinth.noise(seed, x, top, z) < 0.05) {
                        set(level, p.set(x, top, z), Blocks.HANGING_ROOTS.defaultBlockState());
                    }
                }
                continue;
            }
            switch (room.kind()) {
                case Labyrinth.PALACE -> palace(level, lab, seed, x, z, p);
                case Labyrinth.SHRINE -> shrine(level, lab, seed, x, z, p);
                case Labyrinth.HUB -> {}
                default -> chamber(level, lab, room, seed, x, z, col, p);
            }
        }
    }

    private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    /** A chamber: a lantern at its middle, and what its kind keeps: a nest, a larder, a heap of bones, a grotto. */
    private static void chamber(WorldGenLevel level, Labyrinth lab, Labyrinth.Room room, long seed, int x, int z, byte[] col,
                                BlockPos.MutableBlockPos p) {
        int f = Labyrinth.FLOOR[room.level()];
        int h = lab.roomHeight(room, x, z);
        double[] m = lab.middle(room);
        if (x == (int) Math.floor(m[0]) && z == (int) Math.floor(m[1])) {
            set(level, p.set(x, f + h - 1, z), (room.kind() == Labyrinth.BONES ? Blocks.SOUL_LANTERN : Blocks.LANTERN)
                    .defaultBlockState().setValue(LanternBlock.HANGING, true));
            return;
        }
        if (col[f - Labyrinth.Y_MIN] != Labyrinth.AIR) return;
        double n = Labyrinth.noise(seed, x, f, z), n2 = Labyrinth.noise(seed, z, f + 7, x);
        boolean edge = h <= Labyrinth.TUNNEL;
        p.set(x, f, z);
        switch (room.kind()) {
            case Labyrinth.NEST -> {
                // bedding: scraps of wool and straw
                if (n < 0.35) set(level, p, (n2 < 0.5 ? Blocks.BROWN_CARPET : n2 < 0.8 ? Blocks.WHITE_CARPET : Blocks.LIGHT_GRAY_CARPET).defaultBlockState());
                else if (edge && n < 0.5) set(level, p, Blocks.HAY_BLOCK.defaultBlockState());
            }
            case Labyrinth.LARDER -> {
                // what the mice have carried down: barrels, sheaves, gourds
                if (edge && n < 0.35) set(level, p, (n2 < 0.4 ? Blocks.BARREL : n2 < 0.7 ? Blocks.HAY_BLOCK : n2 < 0.85 ? Blocks.PUMPKIN : Blocks.MELON).defaultBlockState());
            }
            case Labyrinth.BONES -> {
                // what they did not eat
                if (n < 0.10) set(level, p, Blocks.BONE_BLOCK.defaultBlockState());
                else if (n < 0.13) set(level, p, Blocks.SKELETON_SKULL.defaultBlockState());
                if (n2 < 0.08) set(level, p.set(x, f + h - 1, z), Blocks.COBWEB.defaultBlockState());
            }
            default -> {
                // a grotto: moss and flowers, as in her palace, but wild
                set(level, p.set(x, f - 1, z), Blocks.MOSS_BLOCK.defaultBlockState());
                p.set(x, f, z);
                if (n < 0.3) set(level, p, Blocks.MOSS_CARPET.defaultBlockState());
                else if (n < 0.42) set(level, p, smallFlower(n2));
                else if (n < 0.47) set(level, p, Blocks.AZALEA.defaultBlockState());
                if (n2 < 0.05) set(level, p.set(x, f + h - 1, z), Blocks.SPORE_BLOSSOM.defaultBlockState());
            }
        }
    }

    private static BlockState smallFlower(double n) {
        Block[] flowers = {Blocks.ALLIUM, Blocks.PINK_TULIP, Blocks.WHITE_TULIP, Blocks.OXEYE_DAISY, Blocks.LILY_OF_THE_VALLEY,
                Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.RED_TULIP};
        return flowers[(int) (n * flowers.length) % flowers.length].defaultBlockState();
    }

    private static void tall(WorldGenLevel level, BlockPos.MutableBlockPos p, Block flower) {
        int y = p.getY();
        set(level, p, flower.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, p.setY(y + 1), flower.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        p.setY(y);
    }

    /** Pillars of the palace, as {dx, dz} from its middle. */
    public static final int[][] PALACE_PILLARS = {{-9, -11}, {-9, -4}, {-9, 4}, {-9, 11}, {9, -11}, {9, -4}, {9, 4}, {9, 11}};
    /** Its censers (a fire on a bale, its smoke climbing), and its hanging lanterns. */
    public static final int[][] CENSERS = {{-4, -6}, {4, -6}, {-4, 6}, {4, 6}};
    public static final int[][] CHAINED_LANTERNS = {{0, -8}, {0, 8}, {-6, 0}, {6, 0}, {-6, -10}, {6, -10}, {-6, 10}, {6, 10}};

    private static boolean at(int[][] list, int dx, int dz) {
        for (int[] a : list) if (a[0] == dx && a[1] == dz) return true;
        return false;
    }

    /**
     * Her palace: a dark hall, gilded, a red carpet from gate to gate up to her dais, red pillars
     * capped with gold, beds of flowers down both sides (peonies, roses, lilacs, azaleas, tulips,
     * petals), censers smoking either side of the carpet, candles on the dais, lanterns hung on chains,
     * spore blossoms from the roof, and lamps of shroomlight set in the walls.
     */
    private static void palace(WorldGenLevel level, Labyrinth lab, long seed, int x, int z, BlockPos.MutableBlockPos p) {
        int f = Labyrinth.FLOOR[Labyrinth.LEVELS - 1], top = f + Labyrinth.PALACE_HEIGHT - 1;
        int dx = x - lab.cx, dz = z - lab.cz;
        int ax = Math.abs(dx), az = Math.abs(dz);
        double n = Labyrinth.noise(seed, x, f, z), n2 = Labyrinth.noise(seed, z, f + 3, x);
        // the floor: polished deepslate, bordered with blackstone; lamps in the walls about it
        set(level, p.set(x, f - 1, z), (ax >= 14 || az >= 14 ? Blocks.POLISHED_BLACKSTONE : Blocks.POLISHED_DEEPSLATE).defaultBlockState());
        p.set(x, f, z);
        if (at(PALACE_PILLARS, dx, dz)) {
            for (int y = f; y < top; y++) set(level, p.setY(y), Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState());
            set(level, p.setY(top), Blocks.GOLD_BLOCK.defaultBlockState());
            return;
        }
        if (at(CHAINED_LANTERNS, dx, dz)) {
            set(level, p.setY(top), Blocks.CHAIN.defaultBlockState());
            set(level, p.setY(top - 1), Blocks.CHAIN.defaultBlockState());
            set(level, p.setY(top - 2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            p.setY(f);
        } else if (n2 < 0.03 && ax > 2) {
            set(level, p.setY(top), Blocks.SPORE_BLOSSOM.defaultBlockState());
            p.setY(f);
        }
        if (ax <= 2 && az <= 2) {
            // her dais, gold at its corners, candles burning there; she stands in its middle
            boolean corner = ax == 2 && az == 2;
            set(level, p, (corner ? Blocks.GOLD_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
            if (corner) set(level, p.setY(f + 1), Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
            if (dx == 0 && dz == -2) set(level, p.setY(f + 1), Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            return;
        }
        if (at(CENSERS, dx, dz)) {
            set(level, p.setY(f - 1), Blocks.HAY_BLOCK.defaultBlockState());
            set(level, p.setY(f), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
            return;
        }
        if (ax <= 1) {
            set(level, p, Blocks.RED_CARPET.defaultBlockState());
            return;
        }
        if (ax >= 12 && az <= 13) {
            // the flower beds
            set(level, p.set(x, f - 1, z), Blocks.MOSS_BLOCK.defaultBlockState());
            p.set(x, f, z);
            if (n < 0.28) tall(level, p, n2 < 0.4 ? Blocks.PEONY : n2 < 0.75 ? Blocks.ROSE_BUSH : Blocks.LILAC);
            else if (n < 0.43) set(level, p, (n2 < 0.5 ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA).defaultBlockState());
            else if (n < 0.7) set(level, p, smallFlower(n2));
            else set(level, p, petals(n2));
            return;
        }
        if (ax >= 10 && n < 0.25) set(level, p, petals(n2));
    }

    static BlockState petals(double n) {
        return Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (n * 4) % 4)
                .setValue(PinkPetalsBlock.FACING, facing((int) (n * 16) % 4));
    }

    /**
     * Her shrine, where she flees: a small room of mud brick, floored in dark wood, and against its
     * back wall an altar with two tablets on it, to her adoptive father and brother, Li Jing the
     * Pagoda-Bearing Heavenly King and his son Nezha (as Sun Wukong found them), candles between
     * them, incense smoking before it, and flowers in pots in its corners.
     */
    private static void shrine(WorldGenLevel level, Labyrinth lab, long seed, int x, int z, BlockPos.MutableBlockPos p) {
        Labyrinth.Room s = lab.shrine();
        int f = Labyrinth.FLOOR[Labyrinth.LEVELS - 1];
        int x0 = lab.cellX(s.i0()) + Labyrinth.ROOM_LO, x1 = lab.cellX(s.i1()) + Labyrinth.ROOM_HI;
        int z0 = lab.cellZ(s.j0()) + Labyrinth.ROOM_LO, z1 = lab.cellZ(s.j1()) + Labyrinth.ROOM_HI;
        int g = lab.shrineGateDir();
        // d: how far in from its gate's side; t: across
        int d = switch (g) {
            case 0 -> z - z0;
            case 1 -> x1 - x;
            case 2 -> z1 - z;
            default -> x - x0;
        };
        int t = switch (g) {
            case 0 -> x - x0;
            case 1 -> z - z0;
            case 2 -> x1 - x;
            default -> z1 - z;
        };
        int depth = (g % 2 == 0 ? z1 - z0 : x1 - x0), across = (g % 2 == 0 ? x1 - x0 : z1 - z0);
        p.set(x, f, z);
        if (d == depth && t >= 3 && t <= across - 3) {
            set(level, p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            p.setY(f + 1);
            if (t == 3 || t == across - 3) {
                set(level, p, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, signFacing(g)));
                if (level.getBlockEntity(p) instanceof SignBlockEntity sign) {
                    boolean father = t == 3;
                    SignText text = new SignText()
                            .setMessage(0, Component.literal(father ? "Revered Father" : "Revered Brother"))
                            .setMessage(1, Component.literal(father ? "Li Jing, the" : "Nezha, the"))
                            .setMessage(2, Component.literal(father ? "Pagoda-Bearing" : "Third Prince"))
                            .setMessage(3, Component.literal(father ? "Heavenly King" : ""));
                    // as saved data: while the land is made the sign has no level to tell of a change
                    var registries = level.registryAccess();
                    var tag = sign.saveWithoutMetadata(registries);
                    SignText.DIRECT_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), text)
                            .result().ifPresent(lines -> {
                                tag.put("front_text", lines);
                                tag.put("back_text", lines.copy());
                            });
                    tag.putBoolean("is_waxed", true);
                    sign.loadWithComponents(tag, registries);
                }
            } else {
                set(level, p, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
            }
            return;
        }
        if (d == depth - 2 && t == across / 2) {
            set(level, p.setY(f - 1), Blocks.HAY_BLOCK.defaultBlockState());
            set(level, p.setY(f), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
            return;
        }
        if ((d == 0 || d == depth) && (t == 0 || t == across)) {
            Block[] pots = {Blocks.POTTED_ALLIUM, Blocks.POTTED_PINK_TULIP, Blocks.POTTED_FLOWERING_AZALEA, Blocks.POTTED_LILY_OF_THE_VALLEY};
            set(level, p, pots[(t == 0 ? 0 : 1) + (d == 0 ? 0 : 2)].defaultBlockState());
            return;
        }
        if ((d == 2 || d == depth - 2) && (t == 2 || t == across - 2)) {
            set(level, p.setY(f + Labyrinth.SHRINE_HEIGHT - 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    /** A standing sign's rotation that faces the way {@code dir} (north, east, south, west). */
    private static int signFacing(int dir) {
        return switch (dir) {
            case 0 -> 8;
            case 1 -> 12;
            case 2 -> 0;
            default -> 4;
        };
    }

    /**
     * Strews petals on the tunnel floors along the way from the palace to her shrine, as she flees
     * down it shedding them: one patch at the middle of each cell on the way, where the floor takes
     * them and nothing stands there. Returns how many were strewn.
     */
    public static int strewTrail(net.minecraft.world.level.Level level, Labyrinth lab) {
        int strewn = 0;
        int f = Labyrinth.FLOOR[Labyrinth.LEVELS - 1];
        for (int[] c : lab.trail()) {
            int[] m = lab.cellMiddle(c[0], c[1]);
            BlockPos at = new BlockPos(m[0], f, m[1]);
            if (!level.isLoaded(at)) continue;
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).is(BlockTags.DIRT)) continue;
            level.setBlock(at, petals(Labyrinth.noise(lab.seed, m[0], f, m[1])), Block.UPDATE_ALL);
            strewn++;
        }
        return strewn;
    }
}
