package com.jttw.engine.world.kunlun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What stands along the pilgrim's way up Mount Kunlun ({@link KunlunShape.Path#sites}): a blueprint,
 * as the summit's is, what stands at each place of a site from the place alone, so every chunk lays
 * its own share. Placed in the site's own frame: {@code u} along the way up from its middle, {@code
 * v} across it (to the right going up), {@code ly} over its floor (the way's height at its middle).
 * <ul>
 *   <li><b>the gate</b> at the foot (a 牌坊 of three bays, white pillars, a gold-trimmed beam, a roof
 *       of celestial blue tiles), 昆仑 <i>Mount Kunlun</i> on its face and 天柱 <i>the Pillar of
 *       Heaven</i> on its back;</li>
 *   <li><b>rest pavilions</b> (亭) over the way, four white pillars under a pyramid roof of blue tiles
 *       with a gold finial, a seat along the inner side, a white rail along the drop, lanterns at the
 *       eaves and each its name on a board over the way in;</li>
 *   <li><b>shrines</b> cut into the mountain at the inner side: a little floor of stone, an altar with
 *       candles and flowers under a roof of blue tiles, walls of stone brick.</li>
 * </ul>
 */
public final class KunlunWay {

    private KunlunWay() {}

    /** The gate's pillars (across, either side), its beam's height. */
    public static final int GATE_INNER = 3, GATE_OUTER = 7, GATE_BEAM = 6;
    /** A pavilion's pillars (along and across, either side), its lintels' height, its eaves' (the roof's foot). */
    public static final int PAVILION_POST = 3, PAVILION_LINTEL = 5, PAVILION_EAVES = 6, PAVILION_HALF = 4;
    /** A shrine: from how far across the way (inner side) its floor runs, to its back wall; its half-width along; its roof's height. */
    public static final int SHRINE_FROM = 3, SHRINE_BACK = 7, SHRINE_HALF = 2, SHRINE_ROOF = 4;

    /** The pavilions' names, from the foot up (the last ones repeat if the way is long). */
    public static final List<String[]> PAVILION_NAMES = List.of(
            new String[]{"迎仙亭", "Pavilion of", "Greeting the", "Immortals"},
            new String[]{"松风亭", "Pavilion of", "the Wind in", "the Pines"},
            new String[]{"听泉亭", "Pavilion of", "Listening to", "the Springs"},
            new String[]{"中天门", "The Middle", "Gate of", "Heaven"},
            new String[]{"望云亭", "Pavilion for", "Gazing on", "the Clouds"},
            new String[]{"积雪亭", "Pavilion of", "Gathered Snow", ""},
            new String[]{"揽月亭", "Pavilion for", "Reaching the", "Moon"},
            new String[]{"凌霄亭", "Pavilion Over", "the Highest", "Clouds"},
            new String[]{"摘星亭", "Pavilion for", "Plucking", "the Stars"});

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState pillar() {
        return Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    /** The way up a site, as a direction. */
    public static Direction up(KunlunShape.Site site) {
        return Direction.fromDelta(site.ux(), 0, site.uz());
    }

    /** To the right of the way up. */
    public static Direction right(KunlunShape.Site site) {
        return up(site).getClockWise();
    }

    /**
     * Whether the column at ({@code u}, {@code v}) of a site is the site's own floor, brought to the
     * way's height there: a pavilion's under its roof, a shrine's (and its back wall's) cut into the
     * mountain. The gate stands on the ground as it is.
     */
    public static boolean floor(KunlunShape.Site site, int u, int v) {
        return switch (site.kind()) {
            case PAVILION -> Math.abs(u) <= PAVILION_HALF && Math.abs(v) <= PAVILION_POST;
            case SHRINE -> Math.abs(u) <= SHRINE_HALF + 1 && v * site.inner() >= SHRINE_FROM && v * site.inner() <= SHRINE_BACK;
            case GATE -> false;
        };
    }

    /** Whether the column at ({@code u}, {@code v}) is a pillar's, built up from the ground under it to the site's floor. */
    public static boolean footing(KunlunShape.Site site, int u, int v) {
        return switch (site.kind()) {
            case GATE -> u == 0 && (Math.abs(v) == GATE_INNER || Math.abs(v) == GATE_OUTER);
            case PAVILION -> Math.abs(u) == PAVILION_POST && Math.abs(v) == PAVILION_POST;
            case SHRINE -> false;
        };
    }

    /**
     * What stands at ({@code u}, {@code v}) of a site, {@code ly} over its floor, the column's own
     * ground at {@code ground} over it (where seats and rails stand on the way as it climbs): a block,
     * air where it is kept open, or null where the ground is left as it is.
     */
    @Nullable
    public static BlockState at(KunlunShape.Site site, int u, int v, int ly, int ground) {
        return switch (site.kind()) {
            case GATE -> gate(site, u, v, ly);
            case PAVILION -> pavilion(site, u, v, ly, ground);
            case SHRINE -> shrine(site, u, v * site.inner(), ly);
        };
    }

    private static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    @Nullable
    private static BlockState gate(KunlunShape.Site site, int u, int v, int ly) {
        int av = Math.abs(v);
        if (av > GATE_OUTER + 1 || Math.abs(u) > 1 || ly < 1) return null;
        boolean post = u == 0 && (av == GATE_INNER || av == GATE_OUTER);
        if (post && ly < GATE_BEAM) return ly == 1 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
        if (u == 0) {
            if (ly == GATE_BEAM && av <= GATE_OUTER) return av < GATE_INNER ? s(Blocks.CHISELED_QUARTZ_BLOCK) : post(av) ? s(Blocks.GOLD_BLOCK) : s(Blocks.SMOOTH_QUARTZ);
            if (ly == GATE_BEAM + 1) return s(Blocks.SMOOTH_QUARTZ);
            if (ly == GATE_BEAM + 2) return s(Blocks.PRISMARINE_BRICKS);
            if (ly == GATE_BEAM + 3 && (av == 0 || av == GATE_OUTER + 1)) return s(Blocks.GOLD_BLOCK);
            if (ly == GATE_BEAM - 1 && av == (GATE_INNER + GATE_OUTER) / 2) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            return null;
        }
        if (ly == GATE_BEAM + 2) return stair(Blocks.DARK_PRISMARINE_STAIRS, u < 0 ? up(site) : up(site).getOpposite());
        return null;
    }

    private static boolean post(int av) {
        return av == GATE_INNER || av == GATE_OUTER;
    }

    @Nullable
    private static BlockState pavilion(KunlunShape.Site site, int u, int v, int ly, int ground) {
        int au = Math.abs(u), av = Math.abs(v);
        if (au > PAVILION_HALF || av > PAVILION_HALF || ly < 1) return null;
        int ring = Math.max(au, av); // 4 the eaves, 3 the pillars' ring
        int d = PAVILION_HALF - ring; // in from the eaves
        // the roof: a pyramid of blue tiles, a gold finial
        int roof = PAVILION_EAVES + d;
        if (ly == roof) {
            if (d == PAVILION_HALF) return s(Blocks.PRISMARINE_BRICKS);
            Direction toward = au >= av ? (u > 0 ? up(site).getOpposite() : up(site)) : (v > 0 ? right(site).getOpposite() : right(site));
            return stair(Blocks.DARK_PRISMARINE_STAIRS, toward);
        }
        if (d == PAVILION_HALF && ly == roof + 1) return s(Blocks.GOLD_BLOCK);
        if (ly > roof) return null;
        if (ring == PAVILION_HALF) {
            // under the eaves: a lantern hung at each corner
            if (au == PAVILION_HALF && av == PAVILION_HALF && ly == PAVILION_EAVES - 1) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            return ly < PAVILION_EAVES && ly > ground - site.y() + 1 ? s(Blocks.AIR) : null;
        }
        if (ring == PAVILION_POST) {
            if (au == PAVILION_POST && av == PAVILION_POST) {
                if (ly < PAVILION_LINTEL) return ly == 1 && ground <= site.y() ? s(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
                return s(Blocks.SMOOTH_QUARTZ);
            }
            if (ly == PAVILION_LINTEL) return s(Blocks.SMOOTH_QUARTZ);
            // along the sides between the pillars: a seat on the inner side, a rail on the drop
            if (av == PAVILION_POST && au < PAVILION_POST && ly == ground - site.y() + 1) {
                boolean innerSide = v * site.inner() > 0;
                if (innerSide) return stair(Blocks.QUARTZ_STAIRS, v > 0 ? right(site) : right(site).getOpposite());
                return s(Blocks.DIORITE_WALL);
            }
        }
        // within: open under the roof (over the way, which is the ground's own)
        return ly > ground - site.y() + 1 && ly < roof ? s(Blocks.AIR) : null;
    }

    @Nullable
    private static BlockState shrine(KunlunShape.Site site, int u, int in, int ly) {
        int au = Math.abs(u);
        if (in < SHRINE_FROM || in > SHRINE_BACK || au > SHRINE_HALF + 1 || ly < 0) return null;
        if (ly == 0) return in == SHRINE_BACK || au > SHRINE_HALF ? s(Blocks.STONE_BRICKS) : (u + in) % 2 == 0 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS);
        if (ly == SHRINE_ROOF) {
            if (in == SHRINE_FROM) return Blocks.DARK_PRISMARINE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            return in >= SHRINE_FROM + 1 ? s(Blocks.DARK_PRISMARINE) : null;
        }
        if (ly > SHRINE_ROOF) return ly == SHRINE_ROOF + 1 && in == SHRINE_BACK - 1 && u == 0 ? s(Blocks.GOLD_BLOCK) : null;
        // its walls: the back, and the sides
        if (in == SHRINE_BACK || au == SHRINE_HALF + 1 && in >= SHRINE_FROM + 2) return s(Blocks.STONE_BRICKS);
        // its front posts
        if (au == SHRINE_HALF + 1 && in == SHRINE_FROM + 1) return s(Blocks.STONE_BRICK_WALL);
        if (au == SHRINE_HALF + 1) return null;
        // the altar against the back wall, candles on it, flowers either side
        if (in == SHRINE_BACK - 1) {
            if (u == 0 && ly == 1) return s(Blocks.CHISELED_STONE_BRICKS);
            if (u == 0 && ly == 2) return s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true);
            if (au == 1 && ly == 1) return s(Blocks.POTTED_AZURE_BLUET);
            if (au == 2 && ly == 1) return s(Blocks.LANTERN);
        }
        return s(Blocks.AIR);
    }

    /** The boards with a site's name, if it has any: where (frame u, v, ly), facing, and the words. */
    public record Board(int u, int v, int ly, Direction facing, String[] lines) {}

    public static List<Board> boards(KunlunShape.Site site) {
        return switch (site.kind()) {
            case GATE -> List.of(new Board(-1, 0, GATE_BEAM, up(site).getOpposite(), new String[]{"昆仑", "Mount Kunlun", "", "The Way Up"}),
                    new Board(1, 0, GATE_BEAM, up(site), new String[]{"天柱", "The Pillar", "of Heaven", ""}));
            case PAVILION -> List.of(new Board(-PAVILION_POST - 1, 0, PAVILION_LINTEL, up(site).getOpposite(),
                    PAVILION_NAMES.get(Math.min(site.number(), PAVILION_NAMES.size() - 1))));
            case SHRINE -> List.of();
        };
    }

    /** The world's position of a site's frame place (u, v, ly), its middle at local (0, 0) being the world's (x0, z0). */
    public static BlockPos world(KunlunShape.Site site, int x0, int z0, int u, int v, int ly) {
        Direction up = up(site), right = right(site);
        return new BlockPos(x0 + site.x() + u * up.getStepX() + v * right.getStepX(), site.y() + ly,
                z0 + site.z() + u * up.getStepZ() + v * right.getStepZ());
    }

    /** Puts a board's sign at {@code pos} (the world's). */
    public static void putBoard(WorldGenLevel level, BlockPos pos, Board board) {
        new com.jttw.engine.world.hell.Site(level, pos.getX(), pos.getZ(), pos.getX(), pos.getZ(), 0, 0, 0).sign(pos.getX(), pos.getY(), pos.getZ(),
                Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, board.facing()), board.lines());
    }
}
