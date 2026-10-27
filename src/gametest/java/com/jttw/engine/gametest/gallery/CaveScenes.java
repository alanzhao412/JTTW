package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.cave.CaveMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.cave.CaveHousehold;
import com.jttw.engine.world.feature.BottomlessCaveFeature;
import com.jttw.engine.world.feature.EarthFlowLairFeature;
import com.jttw.engine.world.feature.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The Bottomless Cave in the form gallery: the fortress laid whole off the stage, to the west (its
 * floors high over the gallery's flat ground, as they are in their own heights; no mountain over
 * it), and looked into: her palace with her on her dais and her maids, the dais close, the flower
 * beds, her shrine, a lit tunnel of the burrows with mice in it, a fungus cavern, a bridal hall, a
 * gallery's ramp, a stair down between floors, a floor's guardian before its way down, the hub
 * under the great shaft, the gate hall and the great door from within. Pit-Hole Mountain, its road
 * and outposts, and the door from outside are seen in a real world: the cave bench
 * ({@code -Pbench.mode=cave}).
 */
final class CaveScenes {

    private CaveScenes() {}

    /** Where the cave is laid: its centre (west of the stage, clear of the other works off it). */
    static final int GX = -150, GZ = 0;

    private static boolean laid;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    static Labyrinth lab() {
        return Labyrinth.of(7L, GX, GZ, Labyrinth.Land.flat(-60));
    }

    /** The fortress, laid whole once, chunk by chunk as the land would be. */
    private static void lay(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Labyrinth lab = lab();
        int reach = (Labyrinth.N + 2) * Labyrinth.PITCH;
        if (!laid) {
            for (int x = GX - reach; x <= GX + reach + 15; x += 16) for (int z = GZ - reach; z <= GZ + reach + 15; z += 16) {
                BottomlessCaveFeature.build(level, lab, null, x & ~15, z & ~15);
            }
            laid = true;
        }
        level.getEntities((Entity) null, new AABB(GX - reach, -64, GZ - reach, GX + reach, 240, GZ + reach),
                e -> !(e instanceof Player)).forEach(Entity::discard);
    }

    /** A block of the palace at (across, along its front's way) from her dais, as {x, z}. */
    private static double[] palace(Labyrinth lab, double across, double along) {
        int[] f = Labyrinth.DIRS[lab.frontDir()];
        return new double[]{lab.cx + 0.5 + across * f[1] + along * f[0], lab.cz + 0.5 - across * f[0] + along * f[1]};
    }

    private static <T extends Mob> T put(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, double x, double y, double z, double lookX, double lookZ) {
        T m = type.create(level);
        if (m == null) return null;
        float yaw = (float) Math.toDegrees(Math.atan2(-(lookX - x), lookZ - z));
        m.moveTo(x, y, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        if (m instanceof CaveMonster c) c.become(c.regularLevel(), false);
        level.addFreshEntity(m);
        return m;
    }

    /** Her, on her dais, and her two maids, all turned toward (x, z). */
    private static void court(ServerLevel level, Labyrinth lab, double[] toward) {
        int f = Labyrinth.THRONE;
        put(level, ModEntities.LADY_EARTH_FLOW.get(), lab.cx + 0.5, f + 1, lab.cz + 0.5, toward[0], toward[1]);
        for (int[] m : EarthFlowLairFeature.MAIDS) put(level, ModEntities.FLOWER_MAID.get(), lab.cx + m[0] + 0.5, f, lab.cz + m[1] + 0.5, toward[0], toward[1]);
    }

    private static void cam(ServerPlayer p, double x, double y, double z, double lx, double ly, double lz) {
        int g = ground(p);
        AbilityScenes.sceneCam = new double[]{x, y - g, z, lx, ly - g, lz};
    }

    /** The first room of a kind on a floor, or null. */
    private static Labyrinth.Room room(Labyrinth lab, int k, int kind) {
        for (Labyrinth.Room r : lab.level(k).rooms()) if (r.kind() == kind) return r;
        return null;
    }

    private static AbilityScenes.Scene scene(String name, java.util.function.Consumer<ServerPlayer> setup) {
        return new AbilityScenes.Scene(name, p -> {
            lay(p);
            setup.accept(p);
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                scene("cave_palace", p -> {
                    Labyrinth lab = lab();
                    double[] cam = palace(lab, 0, 13.5), at = palace(lab, 0, -2);
                    court(p.serverLevel(), lab, cam);
                    cam(p, cam[0], Labyrinth.THRONE + 7.5, cam[1], at[0], Labyrinth.THRONE + 2, at[1]);
                }),
                scene("cave_palace_dais", p -> {
                    Labyrinth lab = lab();
                    double[] cam = palace(lab, 3.5, 6.5), at = palace(lab, 0, 0);
                    court(p.serverLevel(), lab, cam);
                    cam(p, cam[0], Labyrinth.THRONE + 3.4, cam[1], at[0], Labyrinth.THRONE + 2, at[1]);
                }),
                scene("cave_palace_beds", p -> {
                    Labyrinth lab = lab();
                    double[] cam = palace(lab, -7, 10), at = palace(lab, -13.5, 2);
                    court(p.serverLevel(), lab, cam);
                    cam(p, cam[0], Labyrinth.THRONE + 4, cam[1], at[0], Labyrinth.THRONE + 1, at[1]);
                }),
                scene("cave_shrine", p -> {
                    Labyrinth lab = lab();
                    int[] r = lab.refuge();
                    int[] d = Labyrinth.DIRS[lab.shrineGateDir()];
                    double cx = r[0] + 0.5 + d[0] * 4.2, cz = r[2] + 0.5 + d[1] * 4.2;
                    put(p.serverLevel(), ModEntities.LADY_EARTH_FLOW.get(), r[0] + 0.5, r[1], r[2] + 0.5, cx, cz);
                    cam(p, cx, r[1] + 3.2, cz, r[0] + 0.5 - d[0] * 4.5, r[1] + 1.0, r[2] + 0.5 - d[1] * 4.5);
                }),
                scene("cave_tunnel", p -> {
                    // a lit cell of the burrows with a way out of it, looked at from the next, mice in it
                    Labyrinth lab = lab();
                    Labyrinth.Level l = lab.level(0);
                    int f = Labyrinth.FLOOR[0];
                    for (int i = 0; i < Labyrinth.SIZE; i++) for (int j = 0; j < Labyrinth.SIZE; j++) {
                        if (!l.lit(i, j) || l.roomOf(i, j) != null) continue;
                        for (int dir = 0; dir < 4; dir++) {
                            if (!l.passage(i, j, dir, 0)) continue;
                            int[] a = lab.cellMiddle(i, j), b = lab.cellMiddle(i + Labyrinth.DIRS[dir][0], j + Labyrinth.DIRS[dir][1]);
                            int[] dd = Labyrinth.DIRS[dir];
                            double cx = b[0] + 0.5 + dd[0] * 1.4, cz = b[1] + 0.5 + dd[1] * 1.4;
                            for (int k = 0; k < 3; k++) {
                                CaveMonster m = put(p.serverLevel(), ModEntities.MOUSE_SPIRIT.get(), a[0] + 0.5 + (k - 1) * 0.9 * dd[1], f, a[1] + 0.5 + (k - 1) * 0.9 * dd[0], cx, cz);
                                if (m != null && k == 2) m.become(44, true);
                            }
                            cam(p, cx, f + 1.6, cz, a[0] + 0.5, f + 1.3, a[1] + 0.5);
                            return;
                        }
                    }
                }),
                scene("cave_fungus", p -> hall(p, 3, Labyrinth.FUNGUS)),
                scene("cave_granary", p -> hall(p, 1, Labyrinth.GRANARY)),
                scene("cave_storeroom", p -> hall(p, 6, Labyrinth.VAULT)),
                scene("cave_bridal", p -> hall(p, 7, Labyrinth.BRIDAL)),
                scene("cave_ramp", p -> {
                    // a ramp up from the tunnels to a gallery, from its foot
                    Labyrinth lab = lab();
                    for (int k = 0; k < Labyrinth.FLOORS; k++) {
                        Labyrinth.Level l = lab.level(k);
                        for (int i = 0; i < Labyrinth.SIZE; i++) for (int j = 0; j < Labyrinth.SIZE; j++) {
                            int d = l.ramp(i, j);
                            if (d < 0) continue;
                            int[] m = lab.cellMiddle(i, j);
                            int[] dd = Labyrinth.DIRS[d];
                            int f = l.y;
                            cam(p, m[0] + 0.5 - dd[0] * 0.8, f + 1.7, m[1] + 0.5 - dd[1] * 0.8, m[0] + 0.5 + dd[0] * 7, f + 5.5, m[1] + 0.5 + dd[1] * 7);
                            return;
                        }
                    }
                }),
                scene("cave_stair", p -> {
                    Labyrinth lab = lab();
                    Labyrinth.Stair s = lab.stairs().get(2);
                    int[] mid = lab.cellMiddle(s.i(), s.j());
                    int back = 0;
                    for (int dir = 0; dir < 4; dir++) if (lab.level(s.upper() + 1).passage(s.i(), s.j(), dir)) back = dir;
                    int[] d = Labyrinth.DIRS[back];
                    cam(p, mid[0] + 0.5 + d[0] * 4.5, s.lowerY() + 1.6, mid[1] + 0.5 + d[1] * 4.5, mid[0] + 0.5, s.lowerY() + 6.5, mid[1] + 0.5);
                }),
                scene("cave_warden", p -> {
                    // the third floor's guardian before its way down
                    Labyrinth lab = lab();
                    Labyrinth.Level l = lab.level(2);
                    int[] g = l.guard(), x = l.exit();
                    int[] gm = lab.cellMiddle(g[0], g[1]), xm = lab.cellMiddle(x[0], x[1]);
                    double cx = gm[0] + 0.5 + (gm[0] - xm[0]) * 0.5, cz = gm[1] + 0.5 + (gm[1] - xm[1]) * 0.5;
                    CaveMonster w = CaveHousehold.guardian(p.serverLevel(), new BlockPos(gm[0], l.y, gm[1]), 2, p.serverLevel().random);
                    if (w != null) {
                        float yaw = (float) Math.toDegrees(Math.atan2(-(cx - w.getX()), cz - w.getZ()));
                        w.setNoAi(true);
                        w.setYRot(yaw);
                        w.setYHeadRot(yaw);
                        w.setYBodyRot(yaw);
                    }
                    cam(p, cx, l.y + 1.7, cz, gm[0] + 0.5, l.y + 1.2, gm[1] + 0.5);
                }),
                scene("cave_hub", p -> {
                    Labyrinth lab = lab();
                    int f = Labyrinth.FLOOR[0];
                    cam(p, lab.cx + 7.5, f + 2.5, lab.cz + 5.5, lab.cx, f + 4.5, lab.cz);
                }),
                scene("cave_gate_hall", p -> {
                    // the gate hall from within, the great door shut at its end
                    Labyrinth lab = lab();
                    int[] in = lab.beforeDoor(-24), door = lab.door();
                    cam(p, in[0] + 0.5, lab.doorY + 4.5, in[2] + 0.5, door[0] + 0.5, lab.doorY + 4, door[2] + 0.5);
                }));
    }

    /** A hall of a kind on floor k, from the middle of one of its long walls (between its lanterns' chains), across it. */
    private static void hall(ServerPlayer p, int k, int kind) {
        Labyrinth lab = lab();
        Labyrinth.Room r = room(lab, k, kind);
        if (r == null) return;
        int f = Labyrinth.FLOOR[k];
        boolean wide = r.i1() - r.i0() >= r.j1() - r.j0();
        double mx = (lab.cellX(r.i0()) + lab.cellX(r.i1())) / 2.0 + 0.5, mz = (lab.cellZ(r.j0()) + lab.cellZ(r.j1())) / 2.0 + 0.5;
        double ex = wide ? mx + 3.5 : lab.cellX(r.i0()) - 0.7, ez = wide ? lab.cellZ(r.j0()) - 0.7 : mz + 3.5;
        double tx = wide ? mx - 3.5 : lab.cellX(r.i1()) + 1.5, tz = wide ? lab.cellZ(r.j1()) + 1.5 : mz - 3.5;
        put(p.serverLevel(), ModEntities.FLOWER_MAID.get(), mx, f, mz, ex, ez);
        cam(p, ex, f + 3.6, ez, tx, f + 1.2, tz);
    }
}
