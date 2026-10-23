package com.jttw.engine.world;

import com.jttw.engine.world.fengdu.AncientFrame;
import com.jttw.engine.world.fengdu.AncientFrame.Cell;
import com.jttw.engine.world.fengdu.AncientFrame.Frame;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The great frame at an Ancient City's heart: found from any block of its way, whichever way it
 * runs, as vanilla lays it in each of its three city centres (read from the game's own structure
 * files) and turned either way; not found where its ring is broken, its way blocked, or the shape
 * is not its own.
 */
class AncientFrameTest {

    /** A world of cells: what is set, else open (air). */
    private static final class World implements AncientFrame.Cells {
        final Map<Long, Cell> cells = new HashMap<>();

        void set(int x, int y, int z, Cell c) {
            cells.put(key(x, y, z), c);
        }

        @Override
        public Cell at(int x, int y, int z) {
            return cells.getOrDefault(key(x, y, z), Cell.OPEN);
        }

        static long key(int x, int y, int z) {
            return ((long) (x & 0xFFFFF) << 40) | ((long) (y & 0xFFFFF) << 20) | (z & 0xFFFFF);
        }
    }

    /** A frame's ring laid round a way whose lowest, most negative block is (x0, y0, z0), of the given size. */
    private static World frame(boolean alongX, int x0, int y0, int z0, int w, int h) {
        World world = new World();
        for (int i = -1; i <= w; i++) for (int j = -1; j <= h; j++) {
            if (i >= 0 && i < w && j >= 0 && j < h) continue;
            world.set(alongX ? x0 + i : x0, y0 + j, alongX ? z0 : z0 + i, Cell.FRAME);
        }
        // a floor and stone round about, as a city's would be
        for (int i = -4; i <= w + 3; i++) for (int k = -3; k <= 3; k++) {
            world.set(alongX ? x0 + i : x0 + k, y0 - 2, alongX ? z0 + k : z0 + i, Cell.OTHER);
        }
        return world;
    }

    @Test
    void foundFromEveryBlockOfItsWayEitherWayItRuns() {
        for (boolean alongX : new boolean[]{true, false}) {
            World w = frame(alongX, 100, -40, -300, AncientFrame.WIDTH, AncientFrame.HEIGHT);
            Frame expected = new Frame(alongX, 100, -40, -300);
            for (int i = 0; i < AncientFrame.WIDTH; i++) for (int j = 0; j < AncientFrame.HEIGHT; j++) {
                int[] c = expected.cell(i, j);
                assertEquals(expected, AncientFrame.find(w, c[0], c[1], c[2]), "from " + i + ", " + j + (alongX ? " along x" : " along z"));
                assertTrue(expected.contains(c[0], c[1], c[2]));
            }
            // beside the way, before it, over it: nothing
            int[] mid = expected.cell(5, 2);
            assertNull(AncientFrame.find(w, mid[0] + (alongX ? 0 : 1), mid[1], mid[2] + (alongX ? 1 : 0)), "a block before the frame");
            assertNull(AncientFrame.find(w, mid[0], mid[1] + AncientFrame.HEIGHT + 2, mid[2]), "over it");
            assertFalse(expected.contains(mid[0] + (alongX ? 0 : 1), mid[1], mid[2] + (alongX ? 1 : 0)));
        }
    }

    @Test
    void itsCornersAreNotAskedForButTheRestOfItsRingIs() {
        Frame f = new Frame(true, 0, 0, 0);
        World w = frame(true, 0, 0, 0, AncientFrame.WIDTH, AncientFrame.HEIGHT);
        for (int[] corner : new int[][]{{-1, -1}, {AncientFrame.WIDTH, -1}, {-1, AncientFrame.HEIGHT}, {AncientFrame.WIDTH, AncientFrame.HEIGHT}}) {
            w.set(corner[0], corner[1], 0, Cell.OPEN);
        }
        assertEquals(f, AncientFrame.find(w, 3, 2, 0), "the corners gone, still a frame");
        for (int[] gap : new int[][]{{7, -1}, {7, AncientFrame.HEIGHT}, {-1, 3}, {AncientFrame.WIDTH, 0}}) {
            World broken = frame(true, 0, 0, 0, AncientFrame.WIDTH, AncientFrame.HEIGHT);
            broken.set(gap[0], gap[1], 0, Cell.OTHER);
            assertNull(AncientFrame.find(broken, 3, 2, 0), "a block of its ring not reinforced deepslate: " + gap[0] + ", " + gap[1]);
        }
        World blocked = frame(true, 0, 0, 0, AncientFrame.WIDTH, AncientFrame.HEIGHT);
        blocked.set(12, 4, 0, Cell.OTHER);
        assertNull(AncientFrame.find(blocked, 3, 2, 0), "something built in its way");
    }

    @Test
    void otherShapesAreNotIt() {
        for (int[] size : new int[][]{{AncientFrame.WIDTH - 1, AncientFrame.HEIGHT}, {AncientFrame.WIDTH + 1, AncientFrame.HEIGHT},
                {AncientFrame.WIDTH, AncientFrame.HEIGHT - 1}, {AncientFrame.WIDTH, AncientFrame.HEIGHT + 1}, {4, 5}}) {
            World w = frame(false, 0, 0, 0, size[0], size[1]);
            assertNull(AncientFrame.find(w, 0, 1, 2), "a ring " + size[0] + " by " + size[1]);
        }
        assertNull(AncientFrame.find(new World(), 0, 0, 0), "open air");
    }

    @Test
    void framesPackAndUnpack() {
        for (Frame f : new Frame[]{new Frame(true, 0, 0, 0), new Frame(false, -29_999_000, -64, 29_999_000), new Frame(true, 12345, 319, -6789),
                new Frame(false, -1, -1, -1)}) {
            assertEquals(f, Frame.unpack(f.pack()));
        }
    }

    /** Vanilla's city centres, read from the game: each holds the frame, which is found from its way, turned either way. */
    @Test
    void theFrameIsFoundInEveryOneOfVanillasCityCentres() throws Exception {
        for (int n = 1; n <= 3; n++) {
            String path = "/data/minecraft/structure/ancient_city/city_center/city_center_" + n + ".nbt";
            CompoundTag nbt;
            try (InputStream in = AncientFrameTest.class.getResourceAsStream(path)) {
                assertNotNull(in, "the game's " + path);
                nbt = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            }
            ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);
            for (boolean turned : new boolean[]{false, true}) {
                World w = new World();
                int reinforced = 0;
                int[] any = null;
                for (Tag t : nbt.getList("blocks", Tag.TAG_COMPOUND)) {
                    CompoundTag b = (CompoundTag) t;
                    ListTag pos = b.getList("pos", Tag.TAG_INT);
                    String name = palette.getCompound(b.getInt("state")).getString("Name");
                    if (name.equals("minecraft:jigsaw")) name = b.getCompound("nbt").getString("final_state");
                    Cell c = name.equals("minecraft:reinforced_deepslate") ? Cell.FRAME
                            : name.equals("minecraft:air") || name.equals("minecraft:cave_air") ? Cell.OPEN : Cell.OTHER;
                    int x = pos.getInt(0), y = pos.getInt(1), z = pos.getInt(2);
                    if (turned) {
                        int s = x;
                        x = -z;
                        z = s;
                    }
                    w.set(x, y, z, c);
                    if (c == Cell.FRAME) {
                        reinforced++;
                        any = new int[]{x, y, z};
                    }
                }
                assertEquals(2 * (AncientFrame.WIDTH + 2) + 2 * AncientFrame.HEIGHT, reinforced, "the ring of city centre " + n);
                // a block of its way: the bottom row's middle, one up from the lowest reinforced block found there
                int lowest = Integer.MAX_VALUE;
                for (var e : w.cells.entrySet()) {
                    if (e.getValue() != Cell.FRAME) continue;
                    lowest = Math.min(lowest, (int) ((e.getKey() >> 20) & 0xFFFFF) << 12 >> 12);
                }
                Frame found = null;
                for (var e : w.cells.entrySet()) {
                    if (found != null || e.getValue() != Cell.FRAME) continue;
                    long k = e.getKey();
                    int x = (int) (k >> 40) << 12 >> 12, y = (int) ((k >> 20) & 0xFFFFF) << 12 >> 12, z = (int) (k & 0xFFFFF) << 12 >> 12;
                    if (y != lowest) continue;
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        Frame f = AncientFrame.find(w, x + d[0], y + 1, z + d[1]);
                        if (f != null) found = f;
                    }
                }
                assertNotNull(found, "the frame of city centre " + n + (turned ? ", turned" : "") + " (one of its blocks at " + any[0] + " " + any[1] + " " + any[2] + ")");
                assertEquals(turned, found.alongX(), "the way it runs (vanilla lays it along z, unturned)");
            }
        }
    }
}
