package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.AzureLionEntity;
import com.jttw.engine.entity.lion.DemonKingEntity;
import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.entity.lion.WhiteElephantEntity;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionFortress;
import com.jttw.engine.world.lion.LionGates;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * The Lion-Camel Cave ({@link LionFortress}, {@link LionGates}): its blueprint walked on foot with
 * the game's own blocks, from the great door down every floor to the throne room; the great door
 * holding while its wardens stand and opening to a hand once they have fallen (at once to the
 * Peng); a floor's portcullis rising as its warden falls; who keeps each floor; the kings kept to
 * the throne room, and their fight there, true forms and all; the Earth God's needle to the door.
 *
 * <p>Each test runs in a batch of its own: they lay pieces of the fortress round their platforms,
 * wider than them.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class LionFortressGameTests {

    /** A fortress laid so that its local (lx, ly, lz) is at the test's {@code at}. */
    private static LionGates.Fortress fortressAt(GameTestHelper h, BlockPos rel, int lx, int ly, int lz) {
        BlockPos at = h.absolutePos(rel);
        return new LionGates.Fortress(at.getX() - lx, at.getZ() - lz, at.getY() - ly);
    }

    /** Lays the blueprint's blocks from local (lx0..lx1, ly0..ly1, lz0..lz1) of fortress {@code f}, and clears them when the test ends. */
    private static void lay(GameTestHelper h, LionGates.Fortress f, int lx0, int lx1, int ly0, int ly1, int lz0, int lz1) {
        ServerLevel level = h.getLevel();
        List<BlockPos> laid = new ArrayList<>();
        for (int x = lx0; x <= lx1; x++) for (int y = ly0; y <= ly1; y++) for (int z = lz0; z <= lz1; z++) {
            BlockState s = LionFortress.at(x, y, z);
            BlockPos p = f.world(x, y, z);
            LionFortress.lay(level, p, s == null ? Blocks.AIR.defaultBlockState() : s, level.getRandom());
            laid.add(p);
        }
        onEnd(h, () -> {
            for (BlockPos p : laid) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        });
    }

    private static ServerPlayer survivor(GameTestHelper h, ReincarnationOutcome outcome) {
        ServerPlayer p = as(h, outcome);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static <T extends LionCamelMonster> T demon(GameTestHelper h, EntityType<T> type, BlockPos rel) {
        T m = h.spawn(type, rel);
        m.become(m.regularLevel(), false);
        onEnd(h, m::discard);
        return m;
    }

    // ── Walking the blueprint ───────────────────────────────────────────

    /** The blueprint's blocks over its extent, whether each stops one walking, gates open or shut. */
    private static final class Walk {
        final int u0 = -LionFortress.HALF - LionFortress.SHELL - 1, u1 = -u0, v0 = LionFortress.THRONE_N - LionFortress.SHELL - 1,
                v1 = LionFortress.FACADE_V + 8, y0 = LionFortress.BOTTOM - 1, y1 = LionFortress.TOP + 1;
        final byte[] solid;

        Walk(boolean gatesOpen) {
            solid = new byte[(u1 - u0 + 1) * (v1 - v0 + 1) * (y1 - y0 + 1)];
            for (int u = u0; u <= u1; u++) for (int v = v0; v <= v1; v++) for (int y = y0; y <= y1; y++) {
                BlockState s = LionFortress.at(u, y, LionFortress.FZ + v);
                int kind = LionFortress.kind(LionFortress.cell(u, y, LionFortress.FZ + v));
                boolean blocks;
                if (s == null) blocks = y <= 0; // the forecourt's ground before the door, rock elsewhere under
                else if (gatesOpen && (kind == LionFortress.GATE || kind == LionFortress.DOOR)) blocks = false;
                else blocks = s.blocksMotion();
                byte b = (byte) (blocks ? 1 : 0);
                if (s != null && s.is(Blocks.LAVA)) b = 2;
                solid[i(u, y, v)] = b;
            }
        }

        int i(int u, int y, int v) {
            return ((u - u0) * (v1 - v0 + 1) + (v - v0)) * (y1 - y0 + 1) + (y - y0);
        }

        boolean blocks(int u, int y, int v) {
            if (u < u0 || u > u1 || v < v0 || v > v1 || y < y0 || y > y1) return true;
            return solid[i(u, y, v)] == 1;
        }

        boolean stand(int u, int y, int v) {
            if (u < u0 || u > u1 || v < v0 || v > v1 || y - 1 < y0 || y + 1 > y1) return false;
            return solid[i(u, y - 1, v)] == 1 && solid[i(u, y, v)] == 0 && solid[i(u, y + 1, v)] == 0;
        }

        Set<Long> from(int u, int y, int v) {
            Set<Long> seen = new HashSet<>();
            ArrayDeque<int[]> todo = new ArrayDeque<>();
            todo.add(new int[]{u, y, v});
            seen.add(key(u, y, v));
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!todo.isEmpty()) {
                int[] p = todo.poll();
                for (int[] d : dirs) {
                    int nu = p[0] + d[0], nv = p[2] + d[1];
                    for (int dy = 1; dy >= -3; dy--) {
                        int ny = p[1] + dy;
                        if (dy == 1 && (blocks(p[0], p[1] + 2, p[2]) || blocks(nu, ny + 1, nv))) continue;
                        if (dy < 0) {
                            boolean clear = true;
                            for (int y2 = p[1]; y2 > ny && clear; y2--) clear = !blocks(nu, y2, nv) && !blocks(nu, y2 + 1, nv);
                            if (!clear) continue;
                        }
                        if (!stand(nu, ny, nv)) continue;
                        if (seen.add(key(nu, ny, nv))) todo.add(new int[]{nu, ny, nv});
                        break;
                    }
                }
            }
            return seen;
        }

        static long key(int u, int y, int v) {
            return ((long) (u + 512) << 40) | ((long) (y + 512) << 20) | (v + 512);
        }
    }

    @GameTest(batch = "lion_fortress_eachFloorsWayDownIsReachedOnFootFromTheDoor", template = PLATFORM, timeoutTicks = 400)
    public static void eachFloorsWayDownIsReachedOnFootFromTheDoor(GameTestHelper h) {
        Walk open = new Walk(true);
        Set<Long> seen = open.from(0, 1, LionFortress.DOOR_V + 4);
        for (int k = 0; k <= LionFortress.FLOORS; k++) {
            LionFortress.Stair s = LionFortress.stair(k);
            int mid = (s.u0() + s.u1()) / 2;
            check(h, seen.contains(Walk.key(mid, LionFortress.floorY(k) + 1, s.landing())), "floor " + k + "'s landing over its stair down is reached");
            // and down the stair, every step
            for (int i = 1; i <= s.steps(); i++) {
                check(h, seen.contains(Walk.key(mid, LionFortress.floorY(k) - i + 1, s.stepV(i))), "floor " + k + ", step " + i);
            }
        }
        check(h, seen.contains(Walk.key(LionFortress.LION_POST[0], LionFortress.THRONE_Y + 1, LionFortress.LION_POST[1])), "the throne room, at the Lion's place");
        // with the gates shut, the door keeps everyone out
        Walk shut = new Walk(false);
        check(h, !shut.from(0, 1, LionFortress.DOOR_V + 4).contains(Walk.key(0, 1, 0)), "the great door shut, the gate hall is not reached");
        h.succeed();
    }

    @GameTest(batch = "lion_fortress_eachFloorIsKeptAndItsKeepersStandOnItsGround", template = PLATFORM)
    public static void eachFloorIsKeptAndItsKeepersStandOnItsGround(GameTestHelper h) {
        for (int k = 0; k <= LionFortress.FLOORS; k++) {
            final int floor = k;
            List<LionFortress.Post> posts = LionFortress.POSTS.stream().filter(p -> p.floor() == floor).toList();
            check(h, posts.size() == LionFortress.regulars(k) + 1, "floor " + k + ": " + posts.size() + " keepers and its warden");
            check(h, posts.stream().anyMatch(p -> p.role() == LionFortress.WARDEN), "floor " + k + " has its warden");
            for (LionFortress.Post p : posts) {
                BlockState under = LionFortress.at(p.u(), p.ly() - 1, p.lz()), at = LionFortress.at(p.u(), p.ly(), p.lz()), over = LionFortress.at(p.u(), p.ly() + 1, p.lz());
                check(h, under != null && under.blocksMotion() && !under.is(Blocks.LAVA), "ground under the post at " + p.u() + " " + p.v() + " of floor " + k + ": " + under);
                check(h, at != null && !at.blocksMotion() && over != null && !over.blocksMotion(), "room at the post at " + p.u() + " " + p.v() + " of floor " + k);
                // within its floor's hall
                check(h, LionFortress.bandOf(p.ly()) == k, "the post at " + p.u() + " " + p.v() + " is on floor " + k);
            }
        }
        // the kings in the throne room, before their thrones
        for (LionFortress.Post p : LionFortress.POSTS) {
            if (p.role() != LionFortress.KING) continue;
            check(h, p.floor() == LionFortress.THRONE && LionFortress.bandOf(p.ly()) == LionFortress.THRONE, "a king in the throne room");
            BlockState under = LionFortress.at(p.u(), p.ly() - 1, p.lz());
            check(h, under != null && under.blocksMotion(), "the throne room's floor under a king");
        }
        h.succeed();
    }

    // ── The great door ──────────────────────────────────────────────────

    /** Lays the great door so its middle's foot is a block before the platform, and returns the fortress it belongs to. */
    private static LionGates.Fortress door(GameTestHelper h) {
        openAir(h);
        LionGates.Fortress f = fortressAt(h, new BlockPos(2, 2, 0), 0, 1, LionFortress.DOOR_LZ);
        lay(h, f, -LionFortress.DOOR_HALF, LionFortress.DOOR_HALF, 1, LionFortress.DOOR_H, LionFortress.DOOR_LZ, LionFortress.DOOR_LZ);
        return f;
    }

    private static LionCamelMonster warden(GameTestHelper h, LionGates.Fortress f, int gate, BlockPos rel) {
        LionCamelMonster m = demon(h, ModEntities.LION_DEMON.get(), rel);
        LionGates.makeWarden(m, f, gate);
        m.setNoAi(true);
        return m;
    }

    @GameTest(batch = "lion_fortress_theGreatDoorHoldsWhileItsWardensStandThenOpensToAHand", template = PLATFORM, timeoutTicks = 200)
    public static void theGreatDoorHoldsWhileItsWardensStandThenOpensToAHand(GameTestHelper h) {
        LionGates.Fortress f = door(h);
        ServerLevel level = h.getLevel();
        ServerPlayer p = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        LionCamelMonster a = warden(h, f, LionGates.DOOR, new BlockPos(1, 2, 3)), b = warden(h, f, LionGates.DOOR, new BlockPos(3, 2, 3));
        check(h, LionGates.isShut(level, f, LionGates.DOOR), "the door is shut");
        check(h, LionGates.wardens(level, f, LionGates.DOOR).size() == 2, "its two wardens are counted");
        check(h, !LionGates.tryOpen(level, f, LionGates.DOOR, p), "it holds while its wardens stand");
        a.kill();
        check(h, !LionGates.tryOpen(level, f, LionGates.DOOR, p), "it holds while one stands");
        b.kill();
        h.runAfterDelay(2, () -> {
            check(h, LionGates.wardens(level, f, LionGates.DOOR).isEmpty(), "its wardens have fallen");
            check(h, LionGates.isShut(level, f, LionGates.DOOR), "it does not open of itself");
            check(h, LionGates.tryOpen(level, f, LionGates.DOOR, p), "it opens to a hand");
        });
        h.runAfterDelay(60, () -> {
            check(h, !LionGates.isShut(level, f, LionGates.DOOR), "it stands open");
            for (int[] c : LionGates.cells(LionGates.DOOR)) check(h, level.getBlockState(f.world(c[0], c[1], c[2])).isAir(), "its leaves drawn back at " + c[0] + " " + c[1]);
            h.succeed();
        });
    }

    @GameTest(batch = "lion_fortress_theWardensOpenTheGreatDoorToThePengAtOnce", template = PLATFORM, timeoutTicks = 200)
    public static void theWardensOpenTheGreatDoorToThePengAtOnce(GameTestHelper h) {
        LionGates.Fortress f = door(h);
        ServerLevel level = h.getLevel();
        ServerPlayer peng = survivor(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        warden(h, f, LionGates.DOOR, new BlockPos(2, 2, 3));
        check(h, LionGates.tryOpen(level, f, LionGates.DOOR, peng), "it opens to their Third Great King, its warden standing");
        h.runAfterDelay(60, () -> {
            check(h, !LionGates.isShut(level, f, LionGates.DOOR), "open");
            h.succeed();
        });
    }

    @GameTest(batch = "lion_fortress_theFortressStandsAgainstPicksButNotItsFurnishings", template = PLATFORM, timeoutTicks = 100)
    public static void theFortressStandsAgainstPicksButNotItsFurnishings(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long seed = level.getSeed();
        int[] c = Zones.LION_CAMEL.centres(seed)[0];
        LionGates.Fortress f = LionGates.of(level, c[0], c[1]);
        check(h, f != null, "the region's fortress is found");
        // three places of the fortress in this world's region: a wall of its shell, a lantern of the gate hall, and a wall as an old world left it
        int[] wall = {LionFortress.HALF + 1, 5, LionFortress.FZ}, old = {LionFortress.HALF + 2, 5, LionFortress.FZ};
        int[] lamp = null;
        for (int u = -LionFortress.HALF; u <= LionFortress.HALF && lamp == null; u++) for (int v = -LionFortress.HALF; v <= LionFortress.HALF && lamp == null; v++) {
            if (LionFortress.kind(LionFortress.cell(u, LionFortress.HALL_AIR - 1, LionFortress.FZ + v)) == LionFortress.LOW) lamp = new int[]{u, LionFortress.HALL_AIR - 1, LionFortress.FZ + v};
        }
        check(h, lamp != null, "a lantern in the gate hall");
        BlockPos w = f.world(wall[0], wall[1], wall[2]), l = f.world(lamp[0], lamp[1], lamp[2]), o = f.world(old[0], old[1], old[2]);
        level.setBlock(w, LionFortress.at(wall[0], wall[1], wall[2]), 3);
        level.setBlock(l, LionFortress.at(lamp[0], lamp[1], lamp[2]), 3);
        level.setBlock(o, Blocks.DIRT.defaultBlockState(), 3);
        onEnd(h, () -> {
            for (BlockPos b : List.of(w, l, o)) level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
        });
        ServerPlayer p = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, LionGates.isFabric(level, w, level.getBlockState(w)), "its wall is its fabric");
        check(h, !LionGates.isFabric(level, l, level.getBlockState(l)), "its lantern is not");
        check(h, !LionGates.isFabric(level, o, level.getBlockState(o)), "nor is ground other than the blueprint's (a world made before)");
        var breakWall = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, w, level.getBlockState(w), p);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(breakWall);
        check(h, breakWall.isCanceled(), "a pick will not break its wall");
        var breakLamp = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, l, level.getBlockState(l), p);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(breakLamp);
        check(h, !breakLamp.isCanceled(), "but takes its lantern down");
        h.succeed();
    }

    // ── A floor's portcullis ────────────────────────────────────────────

    @GameTest(batch = "lion_fortress_aFloorsPortcullisRisesWhenItsWardenFalls", template = PLATFORM, timeoutTicks = 200)
    public static void aFloorsPortcullisRisesWhenItsWardenFalls(GameTestHelper h) {
        openAir(h);
        ServerLevel level = h.getLevel();
        int k = 3;
        LionFortress.Stair s = LionFortress.stair(k);
        LionGates.Fortress f = fortressAt(h, new BlockPos(2, 2, 0), s.u0(), LionFortress.floorY(k) + 1, LionFortress.FZ + s.gate());
        lay(h, f, s.u0(), s.u1(), LionFortress.floorY(k) + 1, LionFortress.floorY(k) + LionFortress.AIR, LionFortress.FZ + s.gate(), LionFortress.FZ + s.gate());
        check(h, LionGates.isShut(level, f, k), "the portcullis is down");
        ServerPlayer p = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        LionCamelMonster w = warden(h, f, k, new BlockPos(2, 2, 3));
        check(h, !LionGates.tryOpen(level, f, k, p), "it will not rise to a hand while its warden stands");
        w.kill();
        h.runAfterDelay(60, () -> {
            check(h, !LionGates.isShut(level, f, k), "its warden fallen, it has risen of itself");
            h.succeed();
        });
    }

    @GameTest(batch = "lion_fortress_aWonFloorIsACheckpoint", template = PLATFORM, timeoutTicks = 100)
    public static void aWonFloorIsACheckpoint(GameTestHelper h) {
        openAir(h);
        ServerLevel level = h.getLevel();
        int k = 2;
        int[] cp = LionGates.checkpointOf(k);
        LionFortress.Stair s = LionFortress.stair(k);
        // the landing is behind the portcullis, on the floor's ground, room over it
        check(h, LionFortress.ground(LionFortress.kind(LionFortress.cell(cp[0], cp[1] - 1, cp[2]))), "ground under the landing");
        check(h, !LionFortress.solid(LionFortress.kind(LionFortress.cell(cp[0], cp[1], cp[2])))
                && !LionFortress.solid(LionFortress.kind(LionFortress.cell(cp[0], cp[1] + 1, cp[2]))), "room on the landing");
        check(h, (cp[2] - LionFortress.FZ - s.gate()) * s.dir() > 0, "behind the gate");
        // a player in the hall, and one outside it
        LionGates.Fortress f = fortressAt(h, new BlockPos(2, 2, 2), 0, LionFortress.floorY(k) + 1, LionFortress.FZ);
        ServerPlayer in = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        in.moveTo(h.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        List<ServerPlayer> told = LionGates.checkpoint(level, f, k, List.of(in));
        check(h, told.contains(in), "the player in the hall is told");
        eq(h, f.world(cp[0], cp[1], cp[2]), in.getRespawnPosition(), "and will rise again on the landing");
        check(h, in.isRespawnForced() && level.dimension().equals(in.getRespawnDimension()), "whatever bed he had");
        // one on another floor is not
        in.moveTo(in.getX(), in.getY() + LionFortress.PITCH, in.getZ());
        in.setRespawnPosition(Level.OVERWORLD, null, 0f, false, false);
        check(h, !LionGates.checkpoint(level, f, k, List.of(in)).contains(in), "a floor up, he is not");
        h.succeed();
    }

    // ── The kings in the throne room ────────────────────────────────────

    @GameTest(batch = "lion_fortress_aKingOfAnOldWorldGoesDownToHisThrone", template = PLATFORM, timeoutTicks = 100)
    public static void aKingOfAnOldWorldGoesDownToHisThrone(GameTestHelper h) {
        openAir(h);
        ServerLevel level = h.getLevel();
        int[] post = LionFortress.LION_POST;
        int ly = LionFortress.THRONE_Y + 1, lz = LionFortress.FZ + post[1];
        LionGates.Fortress f = fortressAt(h, new BlockPos(2, 2, 2), post[0], ly, lz);
        // the throne room's floor about his place, nothing more
        lay(h, f, post[0] - 3, post[0] + 3, ly - 1, ly + 3, lz - 3, lz + 3);
        // a king of the old world, in the city, his lair there
        AzureLionEntity old = enthrone(h, ModEntities.AZURE_LION.get(), new BlockPos(6, 2, 6));
        BlockPos city = old.getRestrictCenter();
        check(h, old.goDownToHisThrone(level, f), "he goes down to his throne");
        BlockPos throne = f.world(post[0], ly, lz);
        eq(h, throne, old.blockPosition(), "there at once");
        eq(h, throne, old.getRestrictCenter(), "his lair with him");
        check(h, !throne.equals(city), "no longer the city's");
        // another of the old world's: the throne is his brother's already, so he gives way
        AzureLionEntity second = enthrone(h, ModEntities.AZURE_LION.get(), new BlockPos(6, 2, 0));
        check(h, second.goDownToHisThrone(level, f), "the second gives way");
        check(h, second.isRemoved() && old.isAlive(), "one Azure Lion, before his throne");
        // where the throne room is not made, a king keeps the city
        WhiteElephantEntity elephant = enthrone(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(0, 2, 6));
        BlockPos home = elephant.getRestrictCenter();
        check(h, !elephant.goDownToHisThrone(level, f), "no throne room at the Elephant's place: he stays");
        eq(h, home, elephant.getRestrictCenter(), "his lair in the city still");
        h.succeed();
    }

    /** A king put at his place as the land puts him (his lair there). */
    private static <T extends DemonKingEntity> T enthrone(GameTestHelper h, EntityType<T> type, BlockPos rel) {
        T k = type.create(h.getLevel());
        BlockPos at = h.absolutePos(rel);
        k.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        k.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        h.getLevel().addFreshEntity(k);
        onEnd(h, () -> {
            k.stopRiding();
            k.discard();
        });
        return k;
    }

    @GameTest(batch = "lion_fortress_theKingsKeepToTheThroneRoom", template = PLATFORM, timeoutTicks = 100)
    public static void theKingsKeepToTheThroneRoom(GameTestHelper h) {
        openAir(h);
        AzureLionEntity lion = enthrone(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 2));
        WhiteElephantEntity elephant = enthrone(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 2));
        for (DemonKingEntity k : List.<DemonKingEntity>of(lion, elephant)) {
            check(h, k.hasRestriction() && k.getRestrictCenter().equals(h.absolutePos(new BlockPos(2, 2, 2))), "his lair is where he was put");
            check(h, !k.strayed(), "at home he has not strayed");
            // drawn far off, up the stair after a foe
            Vec3 away = Vec3.atBottomCenterOf(k.getRestrictCenter()).add(k.lairRadius() + DemonKingEntity.LEASH + 6, 0, 0);
            k.moveTo(away.x, away.y, away.z);
            check(h, k.strayed(), "drawn " + (k.lairRadius() + DemonKingEntity.LEASH + 6) + " blocks off, he has strayed");
            for (int t = 0; t < 21; t++) {
                k.tickCount++; // as the level counts its ticks
                k.tick();
            }
            check(h, k.distanceToSqr(Vec3.atBottomCenterOf(k.getRestrictCenter())) < 4, "and is back before his throne: " + k.position());
            // a little way off, in his lair, he is let be
            k.moveTo(k.getX() + k.lairRadius() - 2, k.getY(), k.getZ());
            check(h, !k.strayed(), "within his lair he may roam");
        }
        h.succeed();
    }

    @GameTest(batch = "lion_fortress_inTheThroneRoomTheLionStillFightsAndRevealsHisTrueForm", template = PLATFORM, timeoutTicks = 200)
    public static void inTheThroneRoomTheLionStillFightsAndRevealsHisTrueForm(GameTestHelper h) {
        openAir(h);
        int[] post = LionFortress.LION_POST;
        int ly = LionFortress.THRONE_Y + 1, lz = LionFortress.FZ + post[1];
        LionGates.Fortress f = fortressAt(h, new BlockPos(2, 2, 2), post[0], ly, lz);
        // the throne room about his place: its floor, its pillars, the dais and the thrones behind him, its roof
        lay(h, f, post[0] - 14, post[0] + 14, LionFortress.THRONE_Y - 1, LionFortress.THRONE_Y + LionFortress.THRONE_AIR + 1,
                LionFortress.FZ + LionFortress.THRONE_N - 1, lz + 14);
        AzureLionEntity lion = enthrone(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 2));
        lion.setNoAi(true);
        ServerPlayer p = survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        BlockPos stand = h.absolutePos(new BlockPos(2, 2, 10));
        p.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        var foe = h.spawn(EntityType.IRON_GOLEM, new BlockPos(2, 2, 8));
        foe.goalSelector.removeAllGoals(x -> true);
        foe.targetSelector.removeAllGoals(x -> true);
        foe.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1_000_000);
        foe.setHealth(foe.getMaxHealth());
        onEnd(h, foe::discard);
        lion.setTarget(foe);
        // the roar in his hall strikes what is before him
        check(h, lion.roar(foe).contains(foe), "his roar reaches his foe across the hall");
        // brought to a third: his true form, room for it, still his lair
        lion.setHealth(lion.getMaxHealth() * 0.3f);
        h.runAfterDelay(DemonKingEntity.REVEAL + 20, () -> {
            check(h, lion.inTrueForm(), "at a third he shows his true form");
            check(h, !lion.isInWall(), "the throne room holds the lion he truly is: not in its walls");
            BlockPos at = lion.blockPosition();
            for (int y = 0; y < Math.ceil(AzureLionEntity.TRUE_HEIGHT); y++) {
                check(h, !h.getLevel().getBlockState(at.above(y)).blocksMotion(), "room over him at " + y);
            }
            check(h, lion.phase() == 2, "his phases went on: " + lion.phase());
            lion.updateBossBar(List.of(p));
            check(h, lion.bossBarViewers().contains(p), "his bar shows to his foe in the hall");
            check(h, !lion.strayed(), "and he keeps to his lair");
            h.succeed();
        });
    }

    // ── The needle ──────────────────────────────────────────────────────

    @GameTest(batch = "lion_fortress_theEarthGodsNeedlePointsToTheGreatDoor", template = PLATFORM)
    public static void theEarthGodsNeedlePointsToTheGreatDoor(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[] c = Zones.LION_CAMEL.centres(seed)[0];
        var answer = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 95, seed, Set.of());
        eq(h, Zones.LION_CAMEL, answer.zone(), "the ridge at 95");
        eq(h, new BlockPos(c[0], 64, c[1] + LionFortress.DOOR_LZ), answer.target().pos(), "to the fortress's great door, behind the city");
        check(h, answer.words().getString().contains("Lion-Camel Cave"), "and he names it: " + answer.words().getString());
        h.succeed();
    }

    // ── One vase a world ────────────────────────────────────────────────

    /** The Yin-Yang Vases the treasury's table rolls for a chest at {@code at}. */
    private static int vasesAt(ServerLevel level, BlockPos at, long seed) {
        var table = level.getServer().reloadableRegistries().getLootTable(com.jttw.engine.world.lion.LionCamelCity.TREASURY_LOOT);
        var items = table.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(at))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST), seed);
        return items.stream().filter(i -> i.is(com.jttw.engine.registry.ModItems.YIN_YANG_VASE.get())).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
    }

    @GameTest(batch = "lion_fortress_aWorldMadeBeforeKeepsOneVase", template = PLATFORM, timeoutTicks = 100)
    public static void aWorldMadeBeforeKeepsOneVase(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int[] c = Zones.LION_CAMEL.centres(level.getSeed())[0];
        LionGates.Fortress f = LionGates.of(level, c[0], c[1]);
        check(h, f != null, "the region's fortress is found");
        int[] t = LionFortress.TREASURY;
        BlockPos cave = f.world(t[0], t[1], LionFortress.FZ + t[2]);
        BlockPos city = f.world(com.jttw.engine.world.lion.LionTreasury.OLD_X, 4, com.jttw.engine.world.lion.LionTreasury.OLD_Z);
        BlockState was = level.getBlockState(city);
        onEnd(h, () -> level.setBlock(city, was, 3));
        // a world made new: the throne room's treasury holds the vase
        check(h, com.jttw.engine.world.lion.LionTreasury.oldCityChest(level, f) == null, "no old chest in a city made new");
        for (long seed = 0; seed < 5; seed++) eq(h, 1, vasesAt(level, cave, seed), "the throne room's vase, in a world made new");
        // a world whose city was made before: its old chest keeps the vase, the throne room's has none
        level.setBlock(city, Blocks.CHEST.defaultBlockState(), 3);
        check(h, city.equals(com.jttw.engine.world.lion.LionTreasury.oldCityChest(level, f)), "the old chest is found");
        for (long seed = 0; seed < 5; seed++) {
            eq(h, 0, vasesAt(level, cave, seed), "no second vase in the throne room");
            eq(h, 1, vasesAt(level, city, seed), "the old city chest's vase");
        }
        h.succeed();
    }
}
