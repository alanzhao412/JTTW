package com.jttw.engine.world.waters;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

/**
 * The Four Seas and their Dragon Kings (novel, chapter 3): each region of the Mystical Waters holds
 * the crystal palace of one of them, the one whose sea lies that way from the world's origin: a
 * region east of it (within 45° of +x) is the Eastern Sea's, and so on round the compass (+z is
 * south). A palace's gate opens toward its own sea.
 */
public enum Sea {
    /** Ao Guang, the Dragon King of the Eastern Sea: blue-green robes; his treasury keeps the pillar that became the Ruyi Jingu Bang. */
    EAST("ao_guang", Direction.EAST, 0x2E9C8C, () -> Blocks.CYAN_CONCRETE),
    /** Ao Qin, the Dragon King of the Southern Sea: red robes; his waters boil. */
    SOUTH("ao_qin", Direction.SOUTH, 0xB3262A, () -> Blocks.RED_CONCRETE),
    /** Ao Run, the Dragon King of the Western Sea: white robes; his currents whirl. */
    WEST("ao_run", Direction.WEST, 0xE8E6DE, () -> Blocks.WHITE_CONCRETE),
    /** Ao Shun, the Dragon King of the Northern Sea: black robes; his waters freeze. */
    NORTH("ao_shun", Direction.NORTH, 0x1C1C24, () -> Blocks.BLACK_CONCRETE);

    /** His entity id (and his loot table's name). */
    public final String king;
    /** The way his palace's gate faces, and his sea lies from the origin. */
    public final Direction facing;
    /** His robe's colour. */
    public final int robe;
    private final java.util.function.Supplier<Block> runner;

    Sea(String king, Direction facing, int robe, java.util.function.Supplier<Block> runner) {
        this.king = king;
        this.facing = facing;
        this.robe = robe;
        this.runner = runner;
    }

    /** The runner laid down his throne hall, in his colour. */
    public Block runner() {
        return runner.get();
    }

    /** The sea whose palace stands in a region centred at (x, z): by its compass direction from the origin. */
    public static Sea of(int x, int z) {
        if (Math.abs(x) >= Math.abs(z)) return x >= 0 ? EAST : WEST;
        return z >= 0 ? SOUTH : NORTH;
    }

    /**
     * How a palace drawn facing south (its gate toward +z) is turned to face this sea's way: positions
     * and the blocks' own facings alike.
     */
    public Rotation rotation() {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
