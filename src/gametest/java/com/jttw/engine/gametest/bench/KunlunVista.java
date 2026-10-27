package com.jttw.engine.gametest.bench;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

/**
 * Mount Kunlun seen from far off, farther than the game draws (32 chunks at most, and fewer on
 * modest graphics): the world's surface round it as really made (each column's ground and its map
 * colour, and any cloud over it) is read a strip of chunks at a time, and the view drawn from it,
 * as a landscape painter would, by casting a ray out across the ground for each column of the
 * picture, shaded by the sun, hazed with distance, the cloud belts laid over half seen through.
 * Past the read ground the land runs on level at the massif's foot. Pictures, not the game's own:
 * the bench says so in their names ({@code kunlun_vista_<km>km_<seed>}).
 */
final class KunlunVista {

    private KunlunVista() {}

    private static final TicketType<ChunkPos> READ = TicketType.create("jttw_kunlun_vista", Comparator.comparingLong(ChunkPos::toLong));

    /** The ground read round the centre (half the square's side), and the strips of chunks it is read in. */
    static final int HALF = 520, STRIP = 6;

    /** The surface round (cx, cz): ground heights, their colours, cloud tops and bottoms (or MIN over none). */
    static final class Surface {
        final int cx, cz, side = 2 * HALF + 1;
        final short[] ground = new short[side * side], cloudTop = new short[side * side], cloudBottom = new short[side * side];
        final int[] colour = new int[side * side];
        int strip = 0;

        Surface(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
            java.util.Arrays.fill(cloudTop, Short.MIN_VALUE);
            java.util.Arrays.fill(cloudBottom, Short.MIN_VALUE);
        }

        int strips() {
            return (int) Math.ceil(side / (16.0 * STRIP)) + 1;
        }

        private int chunkX0() {
            return (cx - HALF) >> 4;
        }

        private int chunkX1() {
            return (cx + HALF) >> 4;
        }

        private int stripZ0(int s) {
            return ((cz - HALF) >> 4) + s * STRIP;
        }

        /** Asks for the chunks of strip {@code s}. */
        void ask(ServerLevel level, int s) {
            for (int x = chunkX0(); x <= chunkX1(); x++) for (int z = stripZ0(s); z < stripZ0(s) + STRIP; z++) {
                ChunkPos c = new ChunkPos(x, z);
                level.getChunkSource().addRegionTicket(READ, c, 0, c);
            }
        }

        /** Whether strip {@code s} is all made. */
        boolean ready(ServerLevel level, int s) {
            for (int x = chunkX0(); x <= chunkX1(); x++) for (int z = stripZ0(s); z < stripZ0(s) + STRIP; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
            return true;
        }

        /** Reads strip {@code s} and lets its chunks go. */
        void read(ServerLevel level, int s) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = chunkX0(); x <= chunkX1(); x++) for (int z = stripZ0(s); z < stripZ0(s) + STRIP; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk != null) {
                    for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
                        int wx = (x << 4) + i, wz = (z << 4) + j, lx = wx - cx + HALF, lz = wz - cz + HALF;
                        if (lx < 0 || lz < 0 || lx >= side || lz >= side) continue;
                        int k = lx * side + lz;
                        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, i, j);
                        int top = Short.MIN_VALUE, bottom = Short.MIN_VALUE;
                        // through any cloud to the ground under it
                        while (y > level.getMinBuildHeight() && isCloudOrAir(chunk.getBlockState(p.set(wx, y, wz)))) {
                            if (isCloud(chunk.getBlockState(p))) {
                                if (top == Short.MIN_VALUE) top = y;
                                bottom = y;
                            }
                            y--;
                        }
                        BlockState st = chunk.getBlockState(p.set(wx, y, wz));
                        MapColor mc = st.getMapColor(level, p);
                        if (mc == MapColor.NONE) mc = MapColor.STONE;
                        ground[k] = (short) y;
                        colour[k] = mc.col;
                        cloudTop[k] = (short) top;
                        cloudBottom[k] = (short) bottom;
                    }
                }
                ChunkPos c = new ChunkPos(x, z);
                level.getChunkSource().removeRegionTicket(READ, c, 0, c);
            }
        }

        private static boolean isCloud(BlockState s) {
            return s.is(com.jttw.engine.registry.ModBlocks.CELESTIAL_CLOUD.get());
        }

        private static boolean isCloudOrAir(BlockState s) {
            return s.isAir() || isCloud(s);
        }
    }

    // ── Drawing ─────────────────────────────────────────────────────────

    private static final int W = 1280, H = 720;

    /**
     * Draws the view from {@code distance} blocks off the summit (along the bearing {@code bx, bz},
     * a unit step from the centre toward the eye), the eye {@code eyeOver} over the foot, looking at
     * the massif, the land past the read ground level at {@code foot}; writes it to {@code out}.
     */
    static void draw(Surface s, int summit, int foot, double distance, double bx, double bz, double eyeOver, File out) throws IOException {
        double ex = s.cx + bx * distance, ez = s.cz + bz * distance, ey = foot + eyeOver;
        double yaw = Math.atan2(-bz, -bx); // toward the centre
        double fov = Math.toRadians(distance > 1500 ? 34 : 52);
        double focal = (W / 2.0) / Math.tan(fov / 2);
        // tilted so the summit stands in the upper part of the picture
        double horizon;
        double look = Math.atan2((summit + foot) / 2.0 - ey, distance);
        horizon = H * 0.5 + Math.tan(look) * focal;
        int[] px = new int[W * H];
        float[] depth = new float[W * H];
        java.util.Arrays.fill(depth, Float.MAX_VALUE);
        for (int y = 0; y < H; y++) {
            double t = Math.max(0, Math.min(1, (horizon - y) / H + 0.5));
            int sky = mix(0xDCEBFA, 0x7FA8E8, t);
            for (int x = 0; x < W; x++) px[y * W + x] = sky;
        }
        // the sun high behind the eye's shoulder, so the faces toward it are lit
        double sunX = bx * 0.6 + bz * 0.35, sunY = 0.85, sunZ = bz * 0.6 - bx * 0.35;
        double sl = Math.sqrt(sunX * sunX + sunY * sunY + sunZ * sunZ);
        sunX /= sl;
        sunY /= sl;
        sunZ /= sl;
        float[][] cloudAcc = new float[W * H][];
        for (int col = 0; col < W; col++) {
            double a = yaw + Math.atan((col - W / 2.0) / focal);
            double dx = Math.cos(a), dz = Math.sin(a), cosFix = Math.cos(a - yaw);
            int ybuf = H;
            for (double d = 2; d < distance * 3 + 2000 && ybuf > 0; d += 0.5 + d * 0.0025) {
                double wx = ex + dx * d, wz = ez + dz * d;
                int lx = (int) Math.floor(wx) - s.cx + s.side / 2, lz = (int) Math.floor(wz) - s.cz + s.side / 2;
                double h;
                int colour;
                double shade;
                boolean in = lx > 0 && lz > 0 && lx < s.side - 1 && lz < s.side - 1;
                if (in) {
                    int k = lx * s.side + lz;
                    h = s.ground[k];
                    colour = s.colour[k];
                    int k2 = Math.max(2, Math.min(s.side - 3, lx)) * s.side + Math.max(2, Math.min(s.side - 3, lz));
                    double gx = s.ground[k2 + 2 * s.side] - s.ground[k2 - 2 * s.side], gz = s.ground[k2 + 2] - s.ground[k2 - 2];
                    double nx = -gx / 4, ny = 1, nz = -gz / 4, nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    shade = 0.62 + 0.45 * Math.max(0, (nx * sunX + ny * sunY + nz * sunZ) / nl);
                } else {
                    h = foot;
                    colour = 0x6E8B4E;
                    shade = 0.9;
                }
                double z = d * cosFix;
                int sy = (int) Math.round(horizon - (h - ey) / z * focal);
                if (sy < ybuf) {
                    double haze = 1 - Math.exp(-d / 2600.0);
                    int c = mix(scale(colour, shade), 0xDCEBFA, Math.min(0.85, haze));
                    for (int y = Math.max(0, sy); y < ybuf; y++) {
                        px[y * W + col] = c;
                        depth[y * W + col] = (float) d;
                    }
                    ybuf = Math.max(0, sy);
                }
                // clouds over this ground: half seen through, nearer ones first
                if (in) {
                    int k = lx * s.side + lz;
                    if (s.cloudTop[k] != Short.MIN_VALUE) {
                        int top = (int) Math.round(horizon - (s.cloudTop[k] + 1 - ey) / z * focal);
                        int bottom = (int) Math.round(horizon - (s.cloudBottom[k] - ey) / z * focal);
                        double haze = 1 - Math.exp(-d / 2600.0);
                        for (int y = Math.max(0, top); y <= Math.min(H - 1, bottom); y++) {
                            int i = y * W + col;
                            if (depth[i] < d) continue;
                            float[] acc = cloudAcc[i];
                            if (acc == null) acc = cloudAcc[i] = new float[4];
                            if (acc[3] > 0.97f) continue;
                            double alpha = 0.5;
                            int cc = mix(y < (top + bottom) / 2 ? 0xFFFFFF : 0xD8DEE8, 0xDCEBFA, Math.min(0.8, haze));
                            float w = (float) ((1 - acc[3]) * alpha);
                            acc[0] += w * ((cc >> 16) & 255);
                            acc[1] += w * ((cc >> 8) & 255);
                            acc[2] += w * (cc & 255);
                            acc[3] += w;
                        }
                    }
                }
            }
        }
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < W * H; i++) {
            int c = px[i];
            float[] acc = cloudAcc[i];
            if (acc != null) {
                float rest = 1 - acc[3];
                int r = (int) (acc[0] + rest * ((c >> 16) & 255)), g = (int) (acc[1] + rest * ((c >> 8) & 255)), b = (int) (acc[2] + rest * (c & 255));
                c = (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
            }
            img.setRGB(i % W, i / W, c);
        }
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int scale(int c, double k) {
        int r = (int) Math.min(255, ((c >> 16) & 255) * k), g = (int) Math.min(255, ((c >> 8) & 255) * k), b = (int) Math.min(255, (c & 255) * k);
        return (r << 16) | (g << 8) | b;
    }
}
