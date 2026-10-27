package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.cave.CaveMonster;
import com.jttw.engine.entity.cave.EarthFlowDecoyEntity;
import com.jttw.engine.entity.cave.FlowerMaidEntity;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.entity.cave.MouseSpiritEntity;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.cave.BottomlessCave;
import com.jttw.engine.world.cave.CaveHousehold;
import com.jttw.engine.world.cave.CaveRules;
import com.jttw.engine.world.feature.BottomlessCaveFeature;
import com.jttw.engine.world.feature.EarthFlowLairFeature;
import com.jttw.engine.world.feature.Labyrinth;
import com.jttw.engine.world.feature.ZoneLairFeature;
import com.jttw.engine.world.tough.Carving;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Bottomless Cave as a fortress, in a world: laid whole far from the test grid (its floors high
 * over the flat ground, in their own heights), then walked block by block from inside the great
 * door, floor by floor, down to her palace; its household posted floor by floor; her kept to her
 * throne room and her fight there; the great door's rule; the rock that will not yield; the Earth
 * God's landings; the Luopan's needle to the door.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class CaveFortressGameTests {

    private static final String FORTRESS = "cave_fortress", DOOR = "cave_door";
    /** Where the test fortress is laid: far from the test grid. */
    private static final int CX = 24_000, CZ = 24_000;
    private static Labyrinth lab;
    private static ServerLevel laidIn;

    private static final int REACH = (Labyrinth.N + 2) * Labyrinth.PITCH;
    /** The fortress's chunks still to lay, as chunk positions; the game time they were last laid at. */
    private static final ArrayDeque<long[]> pending = new ArrayDeque<>();
    private static long laidAt = -1;
    private static boolean garrisoned;
    /** How long a tick may spend laying the fortress. */
    private static final long BUDGET_NANOS = 10_000_000L;

    private static Labyrinth fortress(GameTestHelper h) {
        return lab;
    }

    /**
     * Before the batch: the fortress's plan, and its chunks forced (they generate on their own, off
     * the server's thread). It is laid a few chunks a tick ({@link #layStep}) while the batch's
     * tests wait for it: laid all at once it held the server some ten seconds, and its platforms'
     * chunks, starved, never came to tick.
     */
    @BeforeBatch(batch = FORTRESS)
    public static void beforeTheFortress(ServerLevel level) {
        lab = Labyrinth.of(level.getSeed(), CX, CZ, Labyrinth.Land.flat(-60));
        laidIn = level;
        garrisoned = false;
        pending.clear();
        for (int x = CX - REACH; x <= CX + REACH + 15; x += 16) for (int z = CZ - REACH; z <= CZ + REACH + 15; z += 16) {
            level.setChunkForced(x >> 4, z >> 4, true);
            pending.add(new long[]{x >> 4, z >> 4});
        }
    }

    /** Lays what of the fortress this tick allows (once a tick, whichever test asks), its generated chunks first. Returns whether all is laid. */
    private static boolean layStep(ServerLevel level) {
        if (garrisoned) return true;
        if (level.getGameTime() == laidAt) return false;
        laidAt = level.getGameTime();
        long start = System.nanoTime();
        int n = pending.size();
        for (int tried = 0; tried < n && !pending.isEmpty() && System.nanoTime() - start < BUDGET_NANOS; tried++) {
            long[] c = pending.poll();
            if (!level.getChunkSource().hasChunk((int) c[0], (int) c[1])) {
                pending.add(c);
                continue;
            }
            rock(level, (int) c[0], (int) c[1]);
            BottomlessCaveFeature.build(level, lab, null, (int) c[0] << 4, (int) c[1] << 4);
        }
        if (!pending.isEmpty()) return false;
        garrisoned = true;
        // all of it laid: now its household, so none wanders off the edge of what is not laid yet
        for (int x = CX - REACH; x <= CX + REACH + 15; x += 16) for (int z = CZ - REACH; z <= CZ + REACH + 15; z += 16) {
            EarthFlowLairFeature.garrison(level, lab, null, x & ~15, z & ~15, level.random);
        }
        // and they stand still at their posts: three hundred of them thinking, in a maze, would starve
        // the server of the spare time it makes the next tests' ground in
        level.getEntitiesOfClass(CaveMonster.class, new AABB(CX - REACH, -64, CZ - REACH, CX + REACH + 16, 320, CZ + REACH + 16))
                .forEach(m -> m.setNoAi(true));
        return true;
    }

    /**
     * Fills the chunk's rock of the cave straight into its sections, before it is laid: the test
     * world is flat, open air from its floor up, and two million blocks of rock set into it the
     * ordinary way would each be lit anew, a queue of light work that kept the batch's platforms
     * from coming to tick for longer than a test may wait. Laid over this, the cave's dressing and
     * its open ways change nothing that light cares for (rock for rock, cave air for air).
     */
    private static void rock(ServerLevel level, int cx, int cz) {
        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
        BlockState rock = ModBlocks.BOTTOMLESS_STONE.get().defaultBlockState();
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        var sections = chunk.getSections();
        boolean[] wasEmpty = new boolean[sections.length];
        for (int n = 0; n < sections.length; n++) wasEmpty[n] = sections[n].hasOnlyAir();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            lab.column((cx << 4) + dx, (cz << 4) + dz, col);
            for (int y = Labyrinth.Y_MIN; y <= Labyrinth.Y_MAX; y++) {
                byte code = col[y - Labyrinth.Y_MIN];
                if (code != Labyrinth.ROCK && code != Labyrinth.PILLAR && code != Labyrinth.COVER) continue;
                int n = chunk.getSectionIndex(y);
                if (n < 0 || n >= sections.length) continue;
                sections[n].setBlockState(dx, y & 15, dz, rock, false);
            }
        }
        for (int n = 0; n < sections.length; n++) {
            if (wasEmpty[n] && !sections[n].hasOnlyAir()) {
                level.getChunkSource().getLightEngine().updateSectionStatus(
                        net.minecraft.core.SectionPos.of(cx, chunk.getSectionYFromSectionIndex(n), cz), false);
            }
        }
        chunk.setUnsaved(true);
    }

    /** Runs {@code body} once the fortress is laid, laying it meanwhile. */
    private static void whenLaid(GameTestHelper h, Runnable body) {
        if (layStep(h.getLevel())) body.run();
        else h.runAfterDelay(1, () -> whenLaid(h, body));
    }

    @AfterBatch(batch = FORTRESS)
    public static void afterTheFortress(ServerLevel level) {
        AABB all = new AABB(CX - 200, -64, CZ - 200, CX + 200, 320, CZ + 200);
        level.getEntities((Entity) null, all, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
        for (int x = CX - REACH; x <= CX + REACH + 15; x += 16) for (int z = CZ - REACH; z <= CZ + REACH + 15; z += 16) {
            level.setChunkForced(x >> 4, z >> 4, false);
        }
        pending.clear();
        lab = null;
        laidIn = null;
    }

    @AfterBatch(batch = DOOR)
    public static void afterTheDoor(ServerLevel level) {
        level.getEntities((Entity) null, new AABB(level.getSharedSpawnPos()).inflate(30_000_000),
                e -> e instanceof CaveMonster && e.getPersistentData().getBoolean(BottomlessCave.KEY_GATE)).forEach(Entity::discard);
    }

    // ── Walking it ──────────────────────────────────────────────────────

    /** How high a block stands where one would put one's feet (its collision's top), 0 if nothing. */
    private static double height(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        var shape = s.getCollisionShape(level, p);
        return shape.isEmpty() ? 0 : shape.max(net.minecraft.core.Direction.Axis.Y);
    }

    private static boolean free(ServerLevel level, BlockPos p) {
        return height(level, p) <= 0.6 && !BottomlessCave.caveRock(level.getBlockState(p));
    }

    private static boolean bears(ServerLevel level, BlockPos p) {
        return height(level, p) > 0 || !level.getFluidState(p).isEmpty();
    }

    private static boolean stands(ServerLevel level, BlockPos p) {
        return free(level, p) && free(level, p.above()) && (bears(level, p.below()) || !level.getFluidState(p).isEmpty() || height(level, p) > 0);
    }

    /**
     * Every place one stands within the fortress reached on foot from {@code start}, as the world has
     * it: walking, stepping or jumping a block with room over one's head, dropping no more than six,
     * or into water.
     */
    static Set<Long> walk(ServerLevel level, Labyrinth lab, BlockPos start) {
        Set<Long> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        seen.add(start.asLong());
        int reach = BottomlessCave.FORTRESS_REACH + 3;
        while (!q.isEmpty() && seen.size() < 3_000_000) {
            BlockPos p = q.poll();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (Math.abs(n.getX() - lab.cx) > reach || Math.abs(n.getZ() - lab.cz) > reach) continue;
                BlockPos to = null;
                if (stands(level, n)) to = n;
                else if (free(level, p.above(2)) && stands(level, n.above())) to = n.above();
                else if (free(level, n) && free(level, n.above())) {
                    for (int fall = 1; fall <= 64; fall++) {
                        BlockPos f = n.below(fall);
                        if (f.getY() < Labyrinth.Y_MIN || !free(level, f)) break;
                        if (stands(level, f)) {
                            if (fall <= 6 || !level.getFluidState(f).isEmpty() || !level.getFluidState(f.below()).isEmpty()) to = f;
                            break;
                        }
                    }
                }
                if (to != null && seen.add(to.asLong())) q.add(to);
            }
            if (!level.getFluidState(p).isEmpty() && stands(level, p.above()) && seen.add(p.above().asLong())) q.add(p.above());
            if (free(level, p.below())) {
                for (int fall = 1; fall <= 64; fall++) {
                    BlockPos f = p.below(fall);
                    if (f.getY() < Labyrinth.Y_MIN || !free(level, f)) break;
                    if (stands(level, f)) {
                        if ((fall <= 6 || !level.getFluidState(f).isEmpty() || !level.getFluidState(f.below()).isEmpty()) && seen.add(f.asLong())) q.add(f);
                        break;
                    }
                }
            }
        }
        return seen;
    }

    private static boolean reachedCell(Set<Long> seen, Labyrinth lab, int k, int[] cell) {
        int y = Labyrinth.floorY(k);
        int[] m = lab.cellMiddle(cell[0], cell[1]);
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) for (int dy = 0; dy <= 1; dy++) {
            if (seen.contains(new BlockPos(m[0] + a, y + dy, m[1] + b).asLong())) return true;
        }
        return false;
    }

    @GameTest(template = PLATFORM, timeoutTicks = 2400, batch = FORTRESS)
    public static void fromInsideTheDoorEveryFloorsWayDownAndHerThroneAreReachedOnFoot(GameTestHelper h) {
        whenLaid(h, () -> fromInsideTheDoorEveryFloorsWayDownAndHerThroneAreReachedOnFootLaid(h));
    }

    private static void fromInsideTheDoorEveryFloorsWayDownAndHerThroneAreReachedOnFootLaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Labyrinth lab = fortress(h);
        int[] in = lab.beforeDoor(-2);
        BlockPos start = new BlockPos(in[0], lab.doorY, in[2]);
        check(h, stands(level, start), "one stands inside the door");
        long t0 = System.nanoTime();
        Set<Long> seen = walk(level, lab, start);
        for (int k = 0; k < Labyrinth.LEVELS; k++) {
            Labyrinth.Level l = lab.level(k);
            check(h, reachedCell(seen, lab, k, l.arrival()), "where one comes into level " + k + " (" + seen.size() + " places walked)");
            if (l.exit() != null) check(h, reachedCell(seen, lab, k, l.exit()), "floor " + (k + 1) + "'s way down, from the door");
        }
        check(h, reachedCell(seen, lab, Labyrinth.FLOORS, lab.antechamber()), "the antechamber before her palace");
        int[] dais = lab.dais();
        boolean palace = false;
        for (int a = -6; a <= 6 && !palace; a++) palace = seen.contains(new BlockPos(dais[0] + a, Labyrinth.THRONE, dais[2] + 8).asLong())
                || seen.contains(new BlockPos(dais[0] + 8, Labyrinth.THRONE, dais[2] + a).asLong());
        check(h, palace, "her palace");
        int[] r = lab.refuge();
        check(h, seen.contains(new BlockPos(r[0], r[1], r[2]).asLong()) || seen.contains(new BlockPos(r[0] + 1, r[1], r[2]).asLong()), "her shrine");
        JTTWEngine.LOGGER.info("the cave walked from its door: {} places in {} ms", seen.size(), (System.nanoTime() - t0) / 1_000_000);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 2400, batch = FORTRESS)
    public static void itsWallsAreTheCavesOwnRockBehindTheirDressing(GameTestHelper h) {
        whenLaid(h, () -> itsWallsAreTheCavesOwnRockBehindTheirDressingLaid(h));
    }

    private static void itsWallsAreTheCavesOwnRockBehindTheirDressingLaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Labyrinth lab = fortress(h);
        // across a floor's tunnel and the wall beside it: dressing, then the cave's rock, which nothing breaks
        int cave = 0, all = 0;
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        for (int x = lab.cx - 60; x <= lab.cx + 60; x += 5) for (int z = lab.cz - 60; z <= lab.cz + 60; z += 5) {
            lab.column(x, z, col);
            for (int k = 0; k < Labyrinth.FLOORS; k++) {
                int y = Labyrinth.FLOOR[k] + 1;
                if (col[y - Labyrinth.Y_MIN] != Labyrinth.ROCK) continue;
                all++;
                BlockState s = level.getBlockState(new BlockPos(x, y, z));
                if (BottomlessCave.caveRock(s)) cave++;
            }
        }
        check(h, all > 200 && cave > all * 0.6, "most of a wall is the cave's own rock: " + cave + " of " + all);
        BlockPos rock = null;
        for (int y = Labyrinth.FLOOR[0] - 2; y < Labyrinth.FLOOR[0] + 12 && rock == null; y++) {
            BlockPos at = new BlockPos(lab.cx + Labyrinth.PITCH * 3 + 3, y, lab.cz + 3);
            if (BottomlessCave.caveRock(level.getBlockState(at))) rock = at;
        }
        check(h, rock != null, "found the cave's rock");
        BlockState s = level.getBlockState(rock);
        check(h, s.getDestroySpeed(level, rock) < 0, "no one digs it");
        check(h, Double.isInfinite(Carving.cost(level, rock, s, 1000)), "no power carves it, however mighty");
        check(h, s.getBlock().getExplosionResistance() > 1_000_000, "no blast breaks it");
        check(h, s.is(TagKey.create(Registries.BLOCK, JTTWEngine.id("unphasable"))), "no phasing form passes through it");
        check(h, s.getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.BLOCK, "no piston moves it");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 2400, batch = FORTRESS)
    public static void theHouseholdIsPostedFloorByFloorStrongerTheDeeper(GameTestHelper h) {
        whenLaid(h, () -> theHouseholdIsPostedFloorByFloorStrongerTheDeeperLaid(h));
    }

    private static void theHouseholdIsPostedFloorByFloorStrongerTheDeeperLaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Labyrinth lab = fortress(h);
        AABB all = new AABB(CX - 120, -64, CZ - 120, CX + 120, 320, CZ + 120);
        int[] count = new int[Labyrinth.LEVELS], guardians = new int[Labyrinth.FLOORS];
        double[] levels = new double[Labyrinth.FLOORS];
        for (CaveMonster m : level.getEntitiesOfClass(CaveMonster.class, all)) {
            int k = m.floor();
            if (k < 0 || k >= Labyrinth.FLOORS) continue;
            check(h, Labyrinth.levelAt(m.getBlockY()) == k, "posted on its own floor: " + whereabouts(h, m));
            check(h, m.isPersistenceRequired(), "kept");
            if (m.getPersistentData().getBoolean(BottomlessCave.KEY_GUARDIAN)) {
                guardians[k]++;
                eq(h, CaveMonster.guardianOn(k), m.mobLevel(), "the guardian's level on floor " + (k + 1));
                int[] g = lab.level(k).guard(), mid = lab.cellMiddle(g[0], g[1]);
                check(h, Math.hypot(m.getX() - mid[0] - 0.5, m.getZ() - mid[1] - 0.5) < 2.5, "before the way down");
                continue;
            }
            count[k]++;
            if (!m.isCaptain()) levels[k] += m.mobLevel();
        }
        for (int k = 0; k < Labyrinth.FLOORS; k++) {
            eq(h, 1, guardians[k], "one guardian on floor " + (k + 1));
            check(h, count[k] >= 6, "floor " + (k + 1) + " holds " + count[k]);
        }
        check(h, count[7] > count[0], "the deepest floor holds more than the first: " + count[7] + " to " + count[0]);
        check(h, CaveMonster.regularOn(7) > CaveMonster.regularOn(0) && CaveMonster.captainOn(7) > CaveMonster.captainOn(0), "stronger the deeper");
        check(h, CaveMonster.regularOn(7) <= Zones.BOTTOMLESS.maxLevel(), "within the cave's band");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 2400, batch = FORTRESS)
    public static void sheIsInHerThroneRoomAndStaysThere(GameTestHelper h) {
        whenLaid(h, () -> sheIsInHerThroneRoomAndStaysThereLaid(h));
    }

    private static void sheIsInHerThroneRoomAndStaysThereLaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Labyrinth lab = fortress(h);
        int[] d = lab.dais();
        LadyEarthFlowEntity lady = ZoneLairFeature.placeBoss(level, ModEntities.LADY_EARTH_FLOW.get(), new BlockPos(d[0], d[1] + 1, d[2]), level.random);
        lady.bindCave(lab);
        onEnd(h, lady::discard);
        check(h, lady.labyrinth() == lab, "her cave is the one her palace is in");
        eq(h, Labyrinth.PALACE, lab.roomAt(Labyrinth.FLOORS, lady.getBlockX(), lady.getBlockZ()) == null ? -1
                : lab.roomAt(Labyrinth.FLOORS, lady.getBlockX(), lady.getBlockZ()).kind(), "she stands in her palace");
        check(h, lady.getBbHeight() + 1 < Labyrinth.PALACE_HEIGHT, "room for her");
        // carried off, up the last stair and out: back she comes
        Labyrinth.Stair last = lab.stairs().get(Labyrinth.FLOORS - 1);
        int[] s = lab.cellMiddle(last.i(), last.j());
        lady.teleportTo(s[0] + 0.5, last.upperY(), s[1] + 0.5);
        h.succeedWhen(() -> {
            check(h, Math.hypot(lady.getX() - d[0] - 0.5, lady.getZ() - d[2] - 0.5) <= LadyEarthFlowEntity.PALACE_GROUND + LadyEarthFlowEntity.LEASH
                    && Math.abs(lady.getY() - (d[1] + 1)) <= LadyEarthFlowEntity.LEASH, "back in her palace: " + lady.blockPosition());
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 2400, batch = FORTRESS)
    public static void herFightGoesOnThereAndSheFleesToHerShrineBehindIt(GameTestHelper h) {
        whenLaid(h, () -> herFightGoesOnThereAndSheFleesToHerShrineBehindItLaid(h));
    }

    private static void herFightGoesOnThereAndSheFleesToHerShrineBehindItLaid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Labyrinth lab = fortress(h);
        int[] d = lab.dais();
        LadyEarthFlowEntity lady = ZoneLairFeature.placeBoss(level, ModEntities.LADY_EARTH_FLOW.get(), new BlockPos(d[0], d[1] + 1, d[2]), level.random);
        lady.bindCave(lab);
        ServerPlayer p = player(h);
        onEnd(h, () -> {
            level.getEntities((Entity) null, new AABB(d[0] - 60, d[1] - 5, d[2] - 60, d[0] + 60, d[1] + 25, d[2] + 60),
                    e -> e instanceof LadyEarthFlowEntity || e instanceof EarthFlowDecoyEntity
                            || (e instanceof CaveMonster m && m.getPersistentData().getBoolean(LadyEarthFlowEntity.KEY_HOUSEHOLD))).forEach(Entity::discard);
        });
        int[] stage = {0};
        // (driven from the check each tick: run once the fortress is laid, inside the runner's walk
        // of its callbacks, this may not add a callback for every tick to come)
        h.succeedWhen(() -> {
            if (stage[0] == 0 && lady.tickCount > 2) {
                lady.setHealth(lady.getMaxHealth() * 0.55f);
                stage[0] = 1;
            } else if (stage[0] == 1 && lady.phase() >= 1) {
                lady.hurt(level.damageSources().playerAttack(p), lady.getMaxHealth() * 10);
                stage[0] = 2;
            }
            List<CaveMonster> household = level.getEntitiesOfClass(CaveMonster.class, new AABB(d[0] - 20, d[1] - 2, d[2] - 20, d[0] + 20, d[1] + 10, d[2] + 20),
                    m -> m.getPersistentData().getBoolean(LadyEarthFlowEntity.KEY_HOUSEHOLD));
            check(h, household.size() >= LadyEarthFlowEntity.MICE + LadyEarthFlowEntity.MAIDS, "her household came, in her palace: " + household.size());
            check(h, lady.fled() && lady.isAlive(), "she fled");
            int[] r = lab.refuge();
            check(h, Math.hypot(lady.getX() - r[0] - 0.5, lady.getZ() - r[2] - 0.5) <= LadyEarthFlowEntity.SHRINE_GROUND, "to her shrine: " + lady.blockPosition());
            check(h, lab.roomAt(Labyrinth.FLOORS, lady.getBlockX(), lady.getBlockZ()) == lab.shrine(), "in it");
            check(h, !level.getEntitiesOfClass(EarthFlowDecoyEntity.class, new AABB(d[0] - 3, d[1] - 1, d[2] - 3, d[0] + 4, d[1] + 4, d[2] + 4)).isEmpty(),
                    "her shoe left on her dais in her likeness");
            int petals = 0;
            for (int[] c : lab.trail()) {
                int[] m = lab.cellMiddle(c[0], c[1]);
                for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
                    if (level.getBlockState(new BlockPos(m[0] + a, Labyrinth.THRONE, m[1] + b)).is(Blocks.PINK_PETALS)) petals++;
                }
            }
            check(h, petals > 3, "petals strewn along her way: " + petals);
        });
    }

    // ── The door, the rock, the landings, the Luopan ────────────────────

    @GameTest(template = PLATFORM, batch = DOOR)
    public static void theGreatDoorIsBarredWhileItsGuardStandsAndOpensWhenTheyFall(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = player(h);
        p.setGameMode(GameType.SURVIVAL);
        java.util.List<String> heard = new java.util.ArrayList<>();
        listen(p, heard);
        // a door of three by three leaves, and the gate's guard before it
        for (int x = 0; x < 3; x++) for (int y = 2; y < 5; y++) h.setBlock(new BlockPos(x + 1, y, 4), ModBlocks.BOTTOMLESS_GATE.get());
        List<CaveMonster> guard = CaveHousehold.gateGuard(level, h.absolutePos(new BlockPos(2, 2, 1)), level.random);
        guard.forEach(g -> g.setNoAi(true));
        onEnd(h, () -> guard.forEach(Entity::discard));
        eq(h, 6, guard.size(), "a captain, three of his pack and two maids");
        check(h, guard.stream().anyMatch(CaveMonster::isCaptain), "led by a captain");
        BlockPos leaf = h.absolutePos(new BlockPos(2, 3, 4));
        eq(h, 0, CaveRules.knock(level, leaf, p), "it holds while they stand");
        check(h, level.getBlockState(leaf).is(ModBlocks.BOTTOMLESS_GATE.get()), "shut");
        check(h, heard.stream().anyMatch(s -> s.contains("barred") && s.contains("6")), "and the knocker is told why: " + heard);
        for (int i = 0; i < guard.size() - 1; i++) guard.get(i).kill();
        eq(h, 0, CaveRules.knock(level, leaf, p), "while one still stands");
        guard.get(guard.size() - 1).kill();
        eq(h, 9, CaveRules.knock(level, leaf, p), "the last fallen, it opens: every leaf");
        for (int x = 0; x < 3; x++) for (int y = 2; y < 5; y++) check(h, level.getBlockState(h.absolutePos(new BlockPos(x + 1, y, 4))).isAir(), "open");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void anEarthGodsLandingRaisesWhoeverStepsUpToIt(GameTestHelper h) {
        ServerPlayer p = player(h);
        Labyrinth lab = Labyrinth.of(h.getLevel().getSeed(), CX, CZ, Labyrinth.Land.flat(-60));
        List<BottomlessCave.Landing> landings = BottomlessCave.landings(lab);
        eq(h, Labyrinth.FLOORS + 2, landings.size(), "inside the door, on each floor, before the palace");
        for (BottomlessCave.Landing l : landings) {
            BlockPos at = l.at();
            p.moveTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5);
            check(h, CaveRules.landing(p, lab) == l, "the landing at " + at);
            eq(h, at, p.getRespawnPosition(), "raised there");
            eq(h, Level.OVERWORLD, p.getRespawnDimension(), "in the world of the living");
            check(h, p.isRespawnForced(), "no bed needed");
        }
        p.moveTo(lab.cx + 300, 100, lab.cz);
        check(h, CaveRules.landing(p, lab) == null, "nowhere else");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theLuopanPointsToTheGreatDoor(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[] c = Zones.BOTTOMLESS.centres(seed)[0];
        int[] door = Labyrinth.doorOf(seed, c[0], c[1]);
        var a = EarthGod.answer(Level.OVERWORLD, new Vec3(0, 64, 0), 40, seed, Set.of());
        eq(h, Zones.BOTTOMLESS, a.zone(), "the cave's band");
        eq(h, door[0], a.target().pos().getX(), "to the door, not the mountain's middle");
        eq(h, door[1], a.target().pos().getZ(), "to the door");
        check(h, Math.hypot(door[0] - c[0], door[1] - c[1]) > 40, "which is out on its flank");
        check(h, a.words().getString().contains("Pit-Hole Mountain") && a.words().getString().contains("great door"), "and he says so: " + a.words().getString());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void overTheCaveNoOtherMonsterComesOfItself(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[] c = Zones.BOTTOMLESS.centres(seed)[0];
        check(h, CaveRules.denied(seed, new BlockPos(c[0] + 140, 120, c[1])), "on the mountain");
        check(h, CaveRules.denied(seed, new BlockPos(c[0] + 30, Labyrinth.FLOOR[3], c[1] + 30)), "on a floor above the cave's own heights");
        check(h, CaveRules.denied(seed, new BlockPos(c[0] - 30, Labyrinth.FLOOR[6], c[1] + 30)), "on a floor below them");
        check(h, !CaveRules.denied(seed, new BlockPos(c[0] + 150, 20, c[1])), "in the region's caves away from the fortress, the cave's own spawn");
        check(h, !CaveRules.denied(seed, new BlockPos(c[0] + 1000, 120, c[1])), "elsewhere, the world's own");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void oneOfTheHouseholdKeepsToItsFloor(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // one bred on floor 3 and one posted there, both found far off it with no one to fight
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        var bred = CaveHousehold.group(level, at, Labyrinth.MICE, 1, false, 2, false, level.random).get(0);
        var kept = CaveHousehold.group(level, at.offset(2, 0, 0), Labyrinth.MICE, 1, false, 2, true, level.random).get(0);
        onEnd(h, () -> {
            bred.discard();
            kept.discard();
        });
        bred.setNoAi(true);
        kept.setNoAi(true);
        check(h, bred.floor() == 2 && kept.floor() == 2, "of floor 3");
        BlockPos post = kept.getRestrictCenter();
        kept.teleportTo(post.getX() + 40.5, post.getY() + 3, post.getZ() + 0.5);
        bred.keepToFloor();
        kept.keepToFloor();
        check(h, bred.isRemoved(), "the bred one slinks off into the dark");
        check(h, kept.isAlive() && kept.blockPosition().closerThan(post, 1.5), "the posted one is back at its post: " + kept.blockPosition() + " / " + post);
        h.succeed();
    }
}
