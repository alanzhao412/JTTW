package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.ridge.RidgeMonster;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.WhiteBoneCaveFeature;
import com.jttw.engine.world.ridge.CaveGarrison;
import com.jttw.engine.world.ridge.CaveKeeper;
import com.jttw.engine.world.ridge.CaveSites;
import com.jttw.engine.world.ridge.CaveState;
import com.jttw.engine.world.ridge.WhiteBoneCave;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * The White Bone Cave, laid whole by hand far from the other tests (as a world's ridge would lay it,
 * chunk by chunk, and garrisoned so): the great door opens only once its two keepers are dead; every
 * floor's way down, and the throne room, are walked to on foot from the door, block by block, over
 * what was really laid (stairs, decorations and all); every floor is manned at its posts; a guardian's
 * death breaks its seal and opens the well's grate under it; a floor left alone is manned again; Lady
 * White Bone is in her throne room and is brought back if ever she is outside it; her fight (found
 * out, her blinks, her false deaths) still works there, and never takes her out of it; and the Earth
 * God's luopan points to the great door.
 *
 * <p>Two batches share the one cave (laid once); each test puts back what it needs as it starts
 * (the door shut, a seal shut), since the other batch may have opened it.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class WhiteBoneCaveGameTests {

    private static final String KEEP = "white_bone_cave_keep", WALK = "white_bone_cave_walk";
    /** The cave's site in the test world: far from every test's platform. */
    private static final int CX = 24_000, CY = 120, CZ = 24_000;
    private static final WhiteBoneCave.Site SITE = new WhiteBoneCave.Site(CX, CY, CZ);
    private static final String FIGHT_TAG = "jttw_test_fight_lady";
    private static boolean laid;

    private static WhiteBoneCave cave(ServerLevel level) {
        return WhiteBoneCave.of(level.getSeed());
    }

    /** The chunks the cave is laid over: its inside, and the forecourt before its door. */
    private static void forEachChunk(java.util.function.BiConsumer<Integer, Integer> f) {
        for (int x0 = (CX - WhiteBoneCave.REACH - 1) & ~15; x0 <= CX + WhiteBoneCave.REACH + 1; x0 += 16) {
            for (int z0 = (CZ - WhiteBoneCave.REACH - 1) & ~15; z0 <= CZ + WhiteBoneCave.COURT_Z1; z0 += 16) f.accept(x0, z0);
        }
    }

    /** The chunks kept loaded for it: those it is laid over, and a ring round them, so that its own all tick their entities. */
    private static void forEachForced(java.util.function.BiConsumer<Integer, Integer> f) {
        for (int x0 = ((CX - WhiteBoneCave.REACH - 1) & ~15) - 16; x0 <= CX + WhiteBoneCave.REACH + 17; x0 += 16) {
            for (int z0 = ((CZ - WhiteBoneCave.REACH - 1) & ~15) - 16; z0 <= CZ + WhiteBoneCave.COURT_Z1 + 16; z0 += 16) f.accept(x0, z0);
        }
    }

    /** Whether the cave was found already laid (a test world kept from an earlier run): its garrison is there too. */
    private static boolean foundLaid, manned;

    /** Lays the cave once, and keeps its chunks loaded and ticking for the batch. */
    private static synchronized void lay(ServerLevel level) {
        CaveSites.pin(level.getSeed(), SITE);
        forEachForced((x0, z0) -> level.setChunkForced(x0 >> 4, z0 >> 4, true));
        // made now, while the server waits, and two chunks round them: the test server's ticks run
        // far faster than chunks are made, so a cave left to load by itself is not ready in time
        for (int x0 = ((CX - WhiteBoneCave.REACH - 1) & ~15) - 48; x0 <= CX + WhiteBoneCave.REACH + 49; x0 += 16) {
            for (int z0 = ((CZ - WhiteBoneCave.REACH - 1) & ~15) - 48; z0 <= CZ + WhiteBoneCave.COURT_Z1 + 48; z0 += 16) level.getChunk(x0 >> 4, z0 >> 4);
        }
        if (laid) return;
        laid = true;
        // a test world kept from an earlier run holds the cave and its garrison already: laid once a world
        if (level.getBlockState(SITE.at(0, WhiteBoneCave.THRONE_Y + 13, WhiteBoneCave.THRONE_N)).is(com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get())) {
            foundLaid = true;
            return;
        }
        WhiteBoneCave cave = cave(level);
        forEachChunk((x0, z0) -> WhiteBoneCaveFeature.build(level, cave, SITE, x0, z0));
        CaveState.of(level).reset();
    }

    /**
     * Mans the cave's posts once, as the land's making would, but only once every chunk of it holds
     * its entities: one put in a chunk whose entities are still loading is in the world, but no
     * search finds it.
     */
    private static synchronized void man(ServerLevel level) {
        if (manned) return;
        manned = true;
        if (foundLaid) return;
        WhiteBoneCave cave = cave(level);
        forEachChunk((x0, z0) -> CaveGarrison.fill(level, cave, SITE, x0, z0, level.random));
    }

    /** Runs {@code then} once every chunk of the cave holds its entities and ticks them (checked every ten ticks), the garrison at its posts. */
    private static void whenLoaded(GameTestHelper h, Runnable then) {
        whenLoaded(h, then, System.nanoTime());
    }

    private static void whenLoaded(GameTestHelper h, Runnable then, long since) {
        h.runAfterDelay(10, () -> {
            StringBuilder not = new StringBuilder();
            ServerLevel level = h.getLevel();
            forEachChunk((x0, z0) -> {
                boolean loaded = level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(x0 >> 4, z0 >> 4));
                boolean ticking = level.isPositionEntityTicking(new BlockPos(x0, CY, z0));
                if (!loaded || !ticking) {
                    not.append((x0 - CX) >> 4).append(',').append((z0 - CZ) >> 4).append(loaded ? "" : " unloaded").append(ticking ? "" : " still")
                            .append(level.getChunkSource().getChunkNow(x0 >> 4, z0 >> 4) == null ? " absent" : "").append("; ");
                }
            });
            long waited = (System.nanoTime() - since) / 1_000_000;
            if (not.isEmpty()) {
                if (waited > 1000) JTTWEngine.LOGGER.info("White Bone Cave tests: its chunks ready after {} ms", waited);
                man(level);
                then.run();
            } else {
                check(h, waited < 180_000, "the cave's chunks' entities not ready after " + waited + " ms: " + not);
                whenLoaded(h, then, since);
            }
        });
    }

    private static int batchesDone;

    /** Lets the cave's chunks go once both batches are done (unloaded between them, its garrison would be saved and loaded again mid-test). */
    private static synchronized void release(ServerLevel level) {
        if (++batchesDone < 2) return;
        forEachForced((x0, z0) -> level.setChunkForced(x0 >> 4, z0 >> 4, false));
    }

    @BeforeBatch(batch = KEEP)
    public static void layForKeeping(ServerLevel level) {
        lay(level);
    }

    @AfterBatch(batch = KEEP)
    public static void afterKeeping(ServerLevel level) {
        release(level);
    }

    @BeforeBatch(batch = WALK)
    public static void layForWalking(ServerLevel level) {
        lay(level);
    }

    @AfterBatch(batch = WALK)
    public static void afterWalking(ServerLevel level) {
        level.getEntitiesOfClass(LadyWhiteBoneEntity.class, CaveKeeper.floorBox(SITE, WhiteBoneCave.FLOORS),
                l -> l.getPersistentData().getBoolean(FIGHT_TAG)).forEach(Entity::discard);
        release(level);
    }

    private static ServerPlayer at(GameTestHelper h, BlockPos where) {
        ServerPlayer p = player(h);
        p.setGameMode(GameType.SURVIVAL);
        p.moveTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5);
        return p;
    }

    // ── The door ────────────────────────────────────────────────────────

    /** Shuts the great door again (its leaves back in the doorway, gone from the passage's walls). */
    private static void shutDoor(ServerLevel level) {
        for (int x = -WhiteBoneCave.DOOR_HALF; x <= WhiteBoneCave.DOOR_HALF; x++) {
            for (int y = 0; y < WhiteBoneCave.DOOR_H; y++) level.setBlock(SITE.at(x, y, WhiteBoneCave.DOOR_Z), WhiteBoneCaveFeature.leaf(x, y), 3);
        }
        for (int s = -1; s <= 1; s += 2) for (int z = WhiteBoneCave.DOOR_Z - 6; z < WhiteBoneCave.DOOR_Z; z++) {
            for (int y = 0; y < WhiteBoneCave.DOOR_H; y++) level.setBlock(SITE.at(s * (WhiteBoneCave.DOOR_HALF + 1), y, z), Blocks.BONE_BLOCK.defaultBlockState(), 3);
        }
    }

    @GameTest(template = PLATFORM, batch = KEEP, timeoutTicks = 20000)
    public static void theGreatDoorOpensOnlyOnceItsKeepersAreDead(GameTestHelper h) {
        whenLoaded(h, () -> theGreatDoorOpensOnlyOnceItsKeepersAreDeadNow(h));
    }

    private static void theGreatDoorOpensOnlyOnceItsKeepersAreDeadNow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        shutDoor(level);
        WhiteBoneCave cave = cave(level);
        List<WhiteBoneCave.Post> wardens = cave.outside().stream().filter(p -> p.rank() == WhiteBoneCave.WARDEN).toList();
        eq(h, 2, wardens.size(), "two keepers");
        // the cave's own keepers stand on the forecourt (no ground there in the test world): two more inside the doorway, for the test
        List<RidgeMonster> keepers = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            RidgeMonster k = CaveGarrison.spawn(level, wardens.get(i), i, SITE.at(i == 0 ? -3 : 3, 0, 52), level.random, MobSpawnType.STRUCTURE);
            check(h, k != null, "a keeper");
            k.setNoAi(true);
            keepers.add(k);
        }
        level.getEntitiesOfClass(RidgeMonster.class, new AABB(SITE.at(-40, -20, 60)).minmax(new AABB(SITE.at(40, 40, 120))),
                m -> CaveGarrison.of(m) && CaveGarrison.rank(m) == WhiteBoneCave.WARDEN).forEach(Entity::discard);
        onEnd(h, () -> keepers.forEach(Entity::discard));
        ServerPlayer p = at(h, SITE.door().south(4));
        CaveState state = new CaveState();
        CaveKeeper.tick(level, SITE, state, List.of(p));
        check(h, CaveKeeper.doorShut(level, SITE) && !state.doorOpen(), "shut while its keepers stand");
        keepers.get(0).kill();
        CaveKeeper.tick(level, SITE, state, List.of(p));
        check(h, CaveKeeper.doorShut(level, SITE), "shut while one keeper stands");
        keepers.get(1).kill();
        p.moveTo(SITE.door().getX() + 0.5, SITE.door().getY(), SITE.door().getZ() + 40.5); // far off
        CaveKeeper.tick(level, SITE, state, List.of(p));
        check(h, CaveKeeper.doorShut(level, SITE), "shut until someone comes up to it");
        p.moveTo(SITE.door().getX() + 0.5, SITE.door().getY(), SITE.door().getZ() + 6.5);
        CaveKeeper.tick(level, SITE, state, List.of(p));
        check(h, !CaveKeeper.doorShut(level, SITE) && state.doorOpen(), "open once its keepers are dead and one comes to it");
        check(h, !level.getBlockState(SITE.at(-(WhiteBoneCave.DOOR_HALF + 1), 5, WhiteBoneCave.DOOR_Z - 2)).is(Blocks.BONE_BLOCK),
                "its leaves folded back against the passage's walls");
        shutDoor(level);
        CaveKeeper.tick(level, SITE, state, List.of(p));
        check(h, !CaveKeeper.doorShut(level, SITE), "and it stays open");
        h.succeed();
    }

    // ── The way down ────────────────────────────────────────────────────

    /** Whether one passes through a block: nothing to stand in, a ladder, or something low enough to step over. */
    private static boolean through(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.is(Blocks.LAVA) || s.is(BlockTags.FIRE) || s.is(Blocks.IRON_BARS)) return false;
        if (s.is(BlockTags.CLIMBABLE)) return true;
        VoxelShape shape = s.getCollisionShape(level, pos);
        return shape.isEmpty() || shape.max(net.minecraft.core.Direction.Axis.Y) <= 0.5;
    }

    private static boolean stands(ServerLevel level, BlockPos feet) {
        if (!through(level, feet) || !through(level, feet.above())) return false;
        BlockState here = level.getBlockState(feet);
        if (here.is(BlockTags.CLIMBABLE) || !here.getCollisionShape(level, feet).isEmpty()) return true;
        BlockState under = level.getBlockState(feet.below());
        return !under.is(Blocks.LAVA) && !under.getCollisionShape(level, feet.below()).isEmpty();
    }

    /** Every place stood on, reached on foot from the door's threshold: steps up of one, drops of up to three, ladders. */
    private static Set<BlockPos> walk(ServerLevel level) {
        Set<BlockPos> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        BlockPos start = SITE.door();
        seen.add(start);
        q.add(start);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            for (int[] d : dirs) {
                int lx = p.getX() + d[0] - CX, lz = p.getZ() + d[1] - CZ;
                if (Math.abs(lx) > WhiteBoneCave.REACH || lz < -WhiteBoneCave.REACH || lz > WhiteBoneCave.THRESHOLD_Z) continue;
                for (int dy = 1; dy >= -3; dy--) {
                    if (dy == 1 && !through(level, p.above(2))) continue;
                    BlockPos n = p.offset(d[0], dy, d[1]);
                    if (!stands(level, n)) continue;
                    if (seen.add(n)) q.add(n);
                    break;
                }
            }
            if (level.getBlockState(p).is(BlockTags.CLIMBABLE)) {
                for (BlockPos n : new BlockPos[]{p.above(), p.below()}) {
                    if (through(level, n) && seen.add(n)) q.add(n);
                }
            }
        }
        return seen;
    }

    @GameTest(template = PLATFORM, batch = WALK, timeoutTicks = 20000)
    public static void everyFloorsWayDownAndTheThroneRoomAreReachedOnFootFromTheDoor(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        CaveKeeper.openDoor(level, SITE);
        for (int k = 0; k < WhiteBoneCave.FLOORS; k++) CaveKeeper.openSeal(level, SITE, k);
        Set<BlockPos> seen = walk(level);
        for (int k = 0; k < WhiteBoneCave.FLOORS; k++) {
            int s = WhiteBoneCave.side(k), f = WhiteBoneCave.floorY(k);
            check(h, seen.contains(SITE.at(-12, f, s * WhiteBoneCave.CORRIDOR_Z)), "through " + WhiteBoneCave.NAMES[k] + "'s seal");
            check(h, seen.contains(SITE.at(WhiteBoneCave.lastStep(k) + 2, WhiteBoneCave.floorY(k + 1), s * 53)),
                    "down " + WhiteBoneCave.NAMES[k] + "'s stair");
        }
        check(h, seen.contains(SITE.at(0, WhiteBoneCave.THRONE_Y, 0)), "the throne room");
        check(h, seen.contains(SITE.at(6, WhiteBoneCave.THRONE_Y + 3, -32)), "the dais");
        h.succeed();
    }

    // ── The garrison ────────────────────────────────────────────────────

    /** Floor k's garrison, wherever in the cave they are (a Wraith drifts). */
    private static List<RidgeMonster> garrison(ServerLevel level, int k) {
        AABB cave = CaveKeeper.floorBox(SITE, 0).minmax(CaveKeeper.floorBox(SITE, WhiteBoneCave.FLOORS));
        return level.getEntitiesOfClass(RidgeMonster.class, cave, m -> m.isAlive() && CaveGarrison.of(m) && CaveGarrison.floor(m) == k);
    }

    @GameTest(template = PLATFORM, batch = KEEP, timeoutTicks = 20000)
    public static void everyFloorIsMannedAtItsPostsHarderFloorByFloor(GameTestHelper h) {
        whenLoaded(h, () -> everyFloorIsMannedAtItsPostsHarderFloorByFloorNow(h));
    }

    private static void everyFloorIsMannedAtItsPostsHarderFloorByFloorNow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        WhiteBoneCave cave = cave(level);
        int before = 0;
        for (int k : new int[]{1, 3, 4, 5, 6, 7, WhiteBoneCave.FLOORS}) { // floors 0 and 2 are the seal's and the restocking's tests'
            List<RidgeMonster> mobs = garrison(level, k);
            StringBuilder missing = new StringBuilder();
            for (int i = 0; i < cave.posts(k).size(); i++) {
                final int post = i;
                if (mobs.stream().noneMatch(m -> CaveGarrison.post(m) == post)) {
                    BlockPos at = CaveGarrison.where(level, SITE, cave.posts(k).get(i));
                    missing.append(i).append(" at ").append(at.getX() - CX).append(' ').append(at.getY() - CY).append(' ').append(at.getZ() - CZ).append("; ");
                }
            }
            eq(h, cave.posts(k).size(), mobs.size(), WhiteBoneCave.NAMES[k] + " manned at every post; unmanned: " + missing);
            int top = mobs.stream().filter(m -> !m.isCaptain()).mapToInt(RidgeMonster::mobLevel).max().orElse(0);
            check(h, top >= before, WhiteBoneCave.NAMES[k] + " no easier than the floor above: " + top);
            before = top;
            if (k < WhiteBoneCave.FLOORS) {
                check(h, mobs.stream().anyMatch(m -> CaveGarrison.rank(m) == WhiteBoneCave.GUARDIAN), WhiteBoneCave.NAMES[k] + "'s guardian");
                check(h, mobs.stream().filter(m -> !(m instanceof com.jttw.engine.entity.ridge.WraithEntity))
                        .allMatch(m -> CaveKeeper.floorOf(SITE, m.blockPosition()) == k), WhiteBoneCave.NAMES[k] + "'s servants on its floor");
            }
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = KEEP, timeoutTicks = 20000)
    public static void aGuardiansDeathBreaksItsSealAndOpensTheWellsGrateBelow(GameTestHelper h) {
        whenLoaded(h, () -> aGuardiansDeathBreaksItsSealAndOpensTheWellsGrateBelowNow(h));
    }

    private static void aGuardiansDeathBreaksItsSealAndOpensTheWellsGrateBelowNow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        CaveState state = CaveState.of(level);
        state.reset();
        int f = WhiteBoneCave.floorY(0), z = WhiteBoneCave.side(0) * WhiteBoneCave.MOUTH_Z;
        for (int x = WhiteBoneCave.SEAL_X0; x <= WhiteBoneCave.SEAL_X1; x++) for (int y = f; y < f + WhiteBoneCave.SEAL_H; y++) {
            level.setBlock(SITE.at(x, y, z), Blocks.IRON_BARS.defaultBlockState(), 3);
        }
        level.setBlock(SITE.at(WhiteBoneCave.SHAFT_X, WhiteBoneCave.grateY(1), WhiteBoneCave.SHAFT_Z), Blocks.IRON_BARS.defaultBlockState(), 3);
        check(h, CaveKeeper.sealShut(level, SITE, 0), "the first seal shut");
        RidgeMonster guardian = garrison(level, 0).stream().filter(m -> CaveGarrison.rank(m) == WhiteBoneCave.GUARDIAN).findFirst()
                .orElseGet(() -> CaveGarrison.spawn(level, cave(level).posts(0).get(0), 0,
                        CaveGarrison.where(level, SITE, cave(level).posts(0).get(0)), level.random, MobSpawnType.STRUCTURE));
        check(h, guardian.getCustomName() != null && guardian.getCustomName().getString().startsWith("Warden of the First Seal"),
                "named: " + guardian.getName().getString());
        check(h, guardian.isCaptain() && guardian.getMaxHealth() > guardian.healthAt(guardian.mobLevel(), true) * 1.5, "a captain, and more");
        ServerPlayer p = at(h, SITE.at(0, 0, 0));
        guardian.hurt(level.damageSources().playerAttack(p), Float.MAX_VALUE);
        check(h, !guardian.isAlive(), "the guardian dead");
        check(h, state.sealBroken(0) && !CaveKeeper.sealShut(level, SITE, 0), "its seal broken");
        check(h, state.grateOpen(1) && level.getBlockState(SITE.at(WhiteBoneCave.SHAFT_X, WhiteBoneCave.grateY(1), WhiteBoneCave.SHAFT_Z)).is(Blocks.LADDER),
                "the well's grate over the second floor's landing open: the way back down to it");
        check(h, !state.grateOpen(2), "but not the one under it");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = KEEP, timeoutTicks = 20000)
    public static void aFloorLeftAloneIsMannedAgainOutOfSight(GameTestHelper h) {
        whenLoaded(h, () -> aFloorLeftAloneIsMannedAgainOutOfSightNow(h));
    }

    private static void aFloorLeftAloneIsMannedAgainOutOfSightNow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int k = 2;
        WhiteBoneCave cave = cave(level);
        garrison(level, k).forEach(Entity::discard);
        eq(h, 0, garrison(level, k).size(), "the floor cleared");
        BlockPos stand = SITE.at(-30, WhiteBoneCave.floorY(k), 30);
        ServerPlayer p = at(h, stand);
        CaveState state = new CaveState();
        CaveKeeper.tick(level, SITE, state, List.of(p));
        eq(h, 0, garrison(level, k).size(), "not manned again at once: first seen now");
        state.seen(k, level.getGameTime() - CaveKeeper.RESTOCK_AFTER - 1);
        CaveKeeper.tick(level, SITE, state, List.of(p));
        List<RidgeMonster> back = garrison(level, k);
        long outOfSight = cave.posts(k).stream().filter(post -> CaveGarrison.where(level, SITE, post).distSqr(stand) >= CaveKeeper.NOT_IN_SIGHT * CaveKeeper.NOT_IN_SIGHT).count();
        StringBuilder missing = new StringBuilder();
        for (int i = 0; i < cave.posts(k).size(); i++) {
            final int post = i;
            if (back.stream().noneMatch(m -> CaveGarrison.post(m) == post)) {
                BlockPos at = CaveGarrison.where(level, SITE, cave.posts(k).get(i));
                missing.append(i).append(" at ").append(at.getX() - CX).append(' ').append(at.getY() - CY).append(' ').append(at.getZ() - CZ)
                        .append(" (").append(level.getBlockState(at).getBlock().getName().getString()).append(", ").append(Math.round(Math.sqrt(at.distSqr(stand)))).append(" off")
                        .append(", loaded ").append(level.isLoaded(at)).append(", entities ").append(level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at)))
                        .append(", forced ").append(level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(at))).append("); ");
            }
        }
        eq(h, (int) outOfSight, back.size(), "manned again at every post out of sight (of " + cave.posts(k).size() + "); unmanned: " + missing);
        check(h, back.stream().noneMatch(m -> m.distanceToSqr(p) < 10 * 10), "none put where the player stands");
        h.succeed();
    }

    // ── Her ─────────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = WALK, timeoutTicks = 20000)
    public static void sheIsInHerThroneRoomAndIsBroughtBackIfEverSheLeavesIt(GameTestHelper h) {
        whenLoaded(h, () -> sheIsInHerThroneRoomAndIsBroughtBackIfEverSheLeavesItNow(h));
    }

    private static void sheIsInHerThroneRoomAndIsBroughtBackIfEverSheLeavesItNow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<LadyWhiteBoneEntity> ladies = level.getEntitiesOfClass(LadyWhiteBoneEntity.class, CaveKeeper.floorBox(SITE, WhiteBoneCave.FLOORS),
                l -> !l.getPersistentData().getBoolean(FIGHT_TAG));
        eq(h, 1, ladies.size(), "one of her, in the throne room's depth");
        LadyWhiteBoneEntity lady = ladies.get(0);
        check(h, lady.keep() != null && lady.inKeep(), "kept to her throne room, and in it");
        check(h, WhiteBoneCave.inThroneRoom(lady.getBlockX() - CX, lady.getBlockY() - CY, lady.getBlockZ() - CZ), "in the throne room itself");
        check(h, lady.disguised(), "passing for one of her dead");
        eq(h, LadyWhiteBoneEntity.THRONE_KEEP, lady.lairRadius(), "she wanders about its middle");
        BlockPos out = SITE.at(0, WhiteBoneCave.floorY(7), 20);
        lady.teleportTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
        check(h, !lady.inKeep(), "put out of it");
        h.runAfterDelay(30, () -> {
            check(h, lady.inKeep(), "back in her throne room: " + lady.blockPosition().toShortString());
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = WALK, timeoutTicks = 20000)
    public static void herFightStillWorksInHerThroneRoomAndNeverTakesHerOutOfIt(GameTestHelper h) {
        whenLoaded(h, () -> herFightStillWorksInHerThroneRoomAndNeverTakesHerOutOfItNow(h));
    }

    private static void herFightStillWorksInHerThroneRoomAndNeverTakesHerOutOfItNow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        LadyWhiteBoneEntity lady = CaveGarrison.placeLady(level, SITE, level.random);
        check(h, lady != null, "placed");
        lady.getPersistentData().putBoolean(FIGHT_TAG, true);
        onEnd(h, lady::discard);
        check(h, lady.disguised() && !lady.showsBossBar(), "disguised, her bar hidden");
        ServerPlayer p = at(h, SITE.at(4, WhiteBoneCave.THRONE_Y, 10));
        lady.hurt(level.damageSources().playerAttack(p), 1f);
        check(h, !lady.disguised() && lady.showsBossBar(), "found out by a blow: herself, her bar shown");
        for (int i = 0; i < 20; i++) {
            lady.vanish(p);
            check(h, lady.inKeep(), "her blinks keep to the throne room");
        }
        // put the player by the room's gate: she will not blink out of the room after him
        p.moveTo(SITE.x(0) + 0.5, SITE.y(WhiteBoneCave.THRONE_Y), SITE.z(WhiteBoneCave.THRONE_S + 2) + 0.5);
        for (int i = 0; i < 20; i++) {
            lady.vanish(p);
            check(h, lady.inKeep(), "not out of the gate after him");
        }
        lady.hurt(level.damageSources().playerAttack(p), Float.MAX_VALUE);
        check(h, lady.isAlive() && lady.falseDeaths() == 1, "she falls and rises again, as ever");
        check(h, lady.inKeep(), "still in her throne room");
        p.moveTo(lady.getX() + 3, lady.getY(), lady.getZ());
        check(h, lady.dropVeil(List.of(p)) == 1, "her veil falls on him");
        h.succeed();
    }

    // ── The way there ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theLuopanPointsToTheGreatDoor(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[] c = Zones.WHITE_BONE.centres(seed)[0];
        var a = EarthGod.answer(Level.OVERWORLD, new Vec3(c[0] + 2000, 70, c[1]), 25, seed, Set.of());
        eq(h, Zones.WHITE_BONE, a.zone(), "the ridge for 25");
        eq(h, new BlockPos(c[0], 64, c[1] + WhiteBoneCave.THRESHOLD_Z + 2), a.target().pos(), "to the great door");
        check(h, a.words().getString().contains("White Bone Cave"), "he names it: " + a.words().getString());
        var near = EarthGod.answer(Level.OVERWORLD, new Vec3(c[0], 70, c[1] + 200), 25, seed, Set.of());
        check(h, near.words().getString().contains("great door"), "near, he speaks of its door: " + near.words().getString());
        h.succeed();
    }
}
