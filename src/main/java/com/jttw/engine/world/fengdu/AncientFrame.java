package com.jttw.engine.world.fengdu;

import javax.annotation.Nullable;

/**
 * The great frame at the heart of an Ancient City, the "portal" of reinforced deepslate that the
 * deep dark's builders left standing in their city centre: as vanilla lays it (each of
 * {@code ancient_city/city_center/city_center_1..3}, the same frame in all three), a ring of
 * reinforced deepslate {@value #WIDTH} + 2 blocks across and {@value #HEIGHT} + 2 high, upright,
 * one block thick, round a way {@value #WIDTH} wide and {@value #HEIGHT} high; turned with its
 * piece, so its plane runs along x or along z. Found here from a block of its way (where soul fire
 * was lit), whichever way it runs: pure, so the shape is tested without a world.
 *
 * <p>Its corners are not asked for (a nether portal's are not either); every other block of the
 * ring must be reinforced deepslate, and every block of its way open: air, water, soul fire and
 * the soul sand or soil it burns on, sculk's veins, the veil itself.
 */
public final class AncientFrame {

    private AncientFrame() {}

    /** The way through the frame: its width along the frame's plane and its height. */
    public static final int WIDTH = 20, HEIGHT = 6;

    /** What a block is to the frame. */
    public enum Cell { FRAME, OPEN, OTHER }

    /** What stands at a place, to the frame. */
    @FunctionalInterface
    public interface Cells {
        Cell at(int x, int y, int z);
    }

    /**
     * A frame found: its plane along x ({@code alongX}) or along z, and the lowest, most negative
     * block of its way (x0, y0, z0).
     */
    public record Frame(boolean alongX, int x0, int y0, int z0) {

        /** The block {@code i} along the way and {@code j} up it, as {x, y, z}. */
        public int[] cell(int i, int j) {
            return alongX ? new int[]{x0 + i, y0 + j, z0} : new int[]{x0, y0 + j, z0 + i};
        }

        /** Whether (x, y, z) is in its way. */
        public boolean contains(int x, int y, int z) {
            int i = alongX ? x - x0 : z - z0, across = alongX ? z - z0 : x - x0;
            return across == 0 && i >= 0 && i < WIDTH && y >= y0 && y < y0 + HEIGHT;
        }

        /** The middle of its way at the bottom, as {x, y, z} (on the lower of the two middle blocks' sides). */
        public int[] foot() {
            return cell(WIDTH / 2, 0);
        }

        /** Packed to one number (for saving): the foot's place and the way it runs. */
        public long pack() {
            return ((long) (x0 & 0x3FFFFFF) << 38) | ((long) (z0 & 0x3FFFFFF) << 12) | ((long) ((y0 + 1024) & 0x7FF) << 1) | (alongX ? 1 : 0);
        }

        public static Frame unpack(long v) {
            int x = (int) (v >> 38), z = (int) (v << 26 >> 38), y = (int) ((v >> 1) & 0x7FF) - 1024;
            return new Frame((v & 1) == 1, x, y, z);
        }
    }

    /**
     * The frame whose way holds (x, y, z), if there is one: for each way a frame might run, down from
     * there to the ring's bottom (no farther than the way is high), along to its side (no farther
     * than the way is wide), and then the whole ring and way checked.
     */
    @Nullable
    public static Frame find(Cells cells, int x, int y, int z) {
        if (cells.at(x, y, z) != Cell.OPEN) return null;
        int y0 = y;
        while (cells.at(x, y0 - 1, z) != Cell.FRAME) {
            if (y - --y0 >= HEIGHT) return null;
        }
        for (boolean alongX : new boolean[]{true, false}) {
            int a = alongX ? x : z, a0 = a;
            boolean found = true;
            while ((alongX ? cells.at(a0 - 1, y, z) : cells.at(x, y, a0 - 1)) != Cell.FRAME) {
                if (a - --a0 >= WIDTH) {
                    found = false;
                    break;
                }
            }
            if (!found) continue;
            Frame f = alongX ? new Frame(true, a0, y0, z) : new Frame(false, x, y0, a0);
            if (whole(cells, f)) return f;
        }
        return null;
    }

    /** Whether the ring round {@code f}'s way is whole (but its corners) and the way open. */
    public static boolean whole(Cells cells, Frame f) {
        for (int i = -1; i <= WIDTH; i++) {
            for (int j = -1; j <= HEIGHT; j++) {
                boolean rimI = i == -1 || i == WIDTH, rimJ = j == -1 || j == HEIGHT;
                if (rimI && rimJ) continue; // a corner
                int[] c = f.cell(i, j);
                Cell want = rimI || rimJ ? Cell.FRAME : Cell.OPEN;
                if (cells.at(c[0], c[1], c[2]) != want) return false;
            }
        }
        return true;
    }
}
