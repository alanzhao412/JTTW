package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.FireCartEntity;
import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.FireFortress;
import com.jttw.engine.world.fire.FortressBlocks;
import com.jttw.engine.world.fire.FortressPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Fire Cloud Cave as a fortress, laid whole far off the test grid (and its door again on its
 * own): the great door barred while its wardens live and knocked open after; every floor's way
 * down reachable on foot, one after another, from the door to Red Boy's dais; each floor's
 * garrison mustered on its own floor, harder each floor down; a guardian's death opening his
 * seal; the garrisons and Red Boy kept to their floors; Red Boy's fight in his throne room; the
 * landings and the return brazier; and the Earth Gods' luopan pointing to the door. Each test is
 * a batch of its own, so no two count each other's creatures.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FireFortressGameTests {

    /** Where the whole fortress is laid (its origin: the gorge floor's level at the centre), and the door on its own. */
    private static final BlockPos FORT = new BlockPos(-30016, 140, -30016), DOOR = new BlockPos(-30016, 100, -30528);
    private static boolean laid;

    /**
     * Keeps the chunks round {@code origin} (the plan's columns) loaded with their creatures
     * ticking for ten minutes (a ticket of our own: the framework lets forced chunks go between
     * batches), and loads them now.
     */
    private static void force(ServerLevel level, BlockPos origin) {
        for (int cx = (origin.getX() + FortressPlan.X_MIN) >> 4; cx <= (origin.getX() + FortressPlan.X_MAX) >> 4; cx++) {
            for (int cz = (origin.getZ() + FortressPlan.Z_MIN) >> 4; cz <= (origin.getZ() + FortressPlan.Z_MAX + 40) >> 4; cz++) {
                ChunkPos c = new ChunkPos(cx, cz);
                level.getChunkSource().addRegionTicket(HELD, c, 2, c, true);
                level.getChunk(cx, cz);
            }
        }
    }

    private static final TicketType<ChunkPos> HELD = TicketType.create("jttw_gametest_fire_fortress", java.util.Comparator.comparingLong(ChunkPos::toLong), 20 * 60 * 10);

    /** The forecourt's floor before the door (in the world it is the ravine's), forty blocks out. */
    private static void forecourt(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -24; x <= 24; x++) for (int z = FortressPlan.FACE_Z + 1; z <= FortressPlan.Z_MAX + 30; z++) {
            if (FortressPlan.code(x, 0, z) == FortressPlan.UNTOUCHED) level.setBlock(p.set(origin.getX() + x, origin.getY(), origin.getZ() + z),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Clears the fortress's creatures (a run before this one may have left some). */
    private static void sweep(ServerLevel level, BlockPos origin) {
        level.getEntities((Entity) null, FireFortress.bounds(origin).inflate(16), e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(Entity::discard);
    }

    /** The whole fortress, laid once a run. */
    private static BlockPos fortress(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        force(level, FORT);
        if (!laid) {
            long start = System.nanoTime();
            int n = layQuietly(level, FORT, FortressPlan.X_MIN, FortressPlan.Y_MIN, FortressPlan.Z_MIN,
                    FortressPlan.X_MAX, FortressPlan.Y_MAX, FortressPlan.Z_MAX);
            forecourt(level, FORT);
            FireFortress.markNew(level, FORT);
            JTTWEngine.LOGGER.info("FireFortressGameTests: the fortress laid, {} blocks in {} ms", n, (System.nanoTime() - start) / 1_000_000);
            laid = true;
        }
        FireFortress.markNew(level, FORT);
        sweep(level, FORT);
        onEnd(h, () -> sweep(level, FORT));
        return FORT;
    }

    /**
     * Lays the plan's local box round {@code origin} straight into its chunks' sections, its chest
     * aside: a million blocks laid through the level would each queue a light update, and the
     * light engine, swamped, would hold up every other test's platform chunks for minutes. Nothing
     * here is looked at, so the fortress may be lit wrong. Returns how many blocks it laid.
     */
    private static int layQuietly(ServerLevel level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1) {
        FortressPlan.Built plan = FortressPlan.get();
        int n = 0;
        List<BlockPos> chests = new ArrayList<>();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int wx = origin.getX() + x, wz = origin.getZ() + z;
            net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(wx >> 4, wz >> 4);
            for (int y = y0; y <= y1; y++) {
                int wy = origin.getY() + y;
                if (level.isOutsideBuildHeight(wy)) continue;
                BlockState st = FortressBlocks.state(plan, x, y, z, wx, wy, wz);
                if (st == null) continue;
                if (st.hasBlockEntity()) {
                    chests.add(new BlockPos(wx, wy, wz));
                    continue;
                }
                chunk.getSection(chunk.getSectionIndex(wy)).setBlockState(wx & 15, wy & 15, wz & 15, st, false);
                n++;
            }
            chunk.setUnsaved(true);
        }
        for (BlockPos c : chests) {
            FortressBlocks.lay(level, c, FortressBlocks.state(plan, c.getX() - origin.getX(), c.getY() - origin.getY(), c.getZ() - origin.getZ(),
                    c.getX(), c.getY(), c.getZ()), level.getRandom());
            n++;
        }
        return n;
    }

    /**
     * Runs {@code body} once every chunk of the fortress round {@code origin} has its creatures
     * loaded and ticking (forced chunks come up over a few ticks; until then what is put in them is
     * neither found nor moved).
     */
    private static void whenReady(GameTestHelper h, BlockPos origin, Runnable body) {
        ServerLevel level = h.getLevel();
        for (int cx = (origin.getX() + FortressPlan.X_MIN) >> 4; cx <= (origin.getX() + FortressPlan.X_MAX) >> 4; cx++) {
            for (int cz = (origin.getZ() + FortressPlan.Z_MIN) >> 4; cz <= (origin.getZ() + FortressPlan.Z_MAX + 40) >> 4; cz++) {
                if (!level.isPositionEntityTicking(new BlockPos(cx * 16 + 8, origin.getY(), cz * 16 + 8))) {
                    h.runAfterDelay(1, () -> whenReady(h, origin, body));
                    return;
                }
            }
        }
        body.run();
    }

    private static BlockPos at(BlockPos origin, int[] local) {
        return origin.offset(local[0], local[1], local[2]);
    }

    private static int[] mid(int[] box) {
        return new int[]{(box[0] + box[3]) / 2, box[1], (box[2] + box[5]) / 2};
    }

    // ── The door ────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "fire_fortress_door", timeoutTicks = 400)
    public static void theDoorIsBarredWhileItsWardensLiveThenGrindsOpen(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        force(level, DOOR);
        sweep(level, DOOR);
        FortressBlocks.layBox(level, DOOR, -26, -2, FortressPlan.SHELL, 26, 34, FortressPlan.Z_MAX);
        forecourt(level, DOOR);
        FireFortress.markNew(level, DOOR);
        onEnd(h, () -> sweep(level, DOOR));
        whenReady(h, DOOR, () -> door(h, level));
    }

    private static void door(GameTestHelper h, ServerLevel level) {
        List<Mob> court = FireFortress.muster(level, DOOR, 0);
        court.forEach(m -> m.setNoAi(true));
        List<FireMonster> wardens = FireFortress.wardens(level, DOOR);
        eq(h, 2, wardens.size(), "two wardens before the door");
        for (FireMonster w : wardens) check(h, w.isCaptain() && w.getType() == ModEntities.FURNACE_BRUTE.get(), "a brute of a captain's strength");
        ServerPlayer p = player(h);
        p.setGameMode(GameType.SURVIVAL); // a creative player opens it regardless
        p.moveTo(DOOR.getX() + 0.5, DOOR.getY() + 1, DOOR.getZ() + FortressPlan.FACE_Z + 3.5);
        BlockPos knocker = DOOR.offset(2, 7, FortressPlan.DOOR[5]);
        check(h, level.getBlockState(knocker).is(Blocks.GOLD_BLOCK), "a ring on the door");
        // knocked while they live: barred, and they turn on the knocker
        check(h, FireFortress.use(level, DOOR, p, knocker, null), "the door answers a knock");
        check(h, !FireFortress.doorOpen(level, DOOR), "barred while its wardens live");
        for (FireMonster w : wardens) check(h, w.getTarget() == p, "the wardens turn on the knocker");
        check(h, FireFortress.guarded(level, DOOR, knocker), "the door does not yield to picks");
        // struck down, and knocked again: it grinds open, middle first
        for (FireMonster w : wardens) w.kill();
        check(h, FireFortress.wardens(level, DOOR).isEmpty(), "the wardens are dead");
        check(h, FireFortress.knock(level, DOOR, p), "the door opens to a knock now");
        h.runAfterDelay(4, () -> check(h, level.getBlockState(DOOR.offset(0, 7, FortressPlan.DOOR[5])).isAir()
                && level.getBlockState(DOOR.offset(6, 7, FortressPlan.DOOR[5])).is(Blocks.WAXED_CUT_COPPER), "it opens from the middle out"));
        h.runAfterDelay(40, () -> {
            check(h, FireFortress.doorOpen(level, DOOR), "open");
            int[] d = FortressPlan.DOOR;
            for (int x = d[0]; x <= d[3]; x++) for (int y = d[1]; y <= d[4]; y++) for (int z = d[2]; z <= d[5]; z++) {
                check(h, level.getBlockState(DOOR.offset(x, y, z)).isAir(), "the doorway clear at " + x + " " + y + " " + z);
            }
            check(h, level.getBlockState(DOOR.offset(0, 18, FortressPlan.FACE_Z + 1)).is(Blocks.GILDED_BLACKSTONE), "the plaque stays over it");
            check(h, !FireFortress.knock(level, DOOR, p), "and stays open");
            h.succeed();
        });
    }

    // ── The way down ────────────────────────────────────────────────────

    /**
     * Whether a zombie walks from local {@code from} to {@code to} in the fortress round {@code o}:
     * along the plan's own way, leg by leg (a zombie's path finding gives up long before the far
     * side of a great hall), each leg a path the zombie finds in the world as laid.
     */
    private static boolean walksAlong(ServerLevel level, BlockPos o, int[] from, int[] to, List<Entity> made) {
        List<int[]> way = FortressPlan.get().route(from, to, false);
        if (way == null) return false;
        for (int i = 0; i < way.size() - 1; ) {
            int next = Math.min(way.size() - 1, i + 16);
            if (!walks(level, at(o, way.get(i)), at(o, way.get(next)), made)) return false;
            i = next;
        }
        return true;
    }

    /** A zombie's path from {@code from} to {@code to}: whether it reaches. */
    private static boolean walks(ServerLevel level, BlockPos from, BlockPos to, List<Entity> made) {
        Zombie z = EntityType.ZOMBIE.create(level);
        z.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(160);
        z.moveTo(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
        z.setNoAi(true);
        z.setOnGround(true); // a path is made only for one standing
        level.addFreshEntity(z);
        made.add(z);
        z.getNavigation().setMaxVisitedNodesMultiplier(60f);
        Path path = z.getNavigation().createPath(to, 1);
        if (path != null && !path.canReach()) {
            JTTWEngine.LOGGER.warn("FireFortressGameTests: no way from {} to {}; the path ends at {} after {} nodes", from.toShortString(),
                    to.toShortString(), path.getEndNode() == null ? "nowhere" : path.getEndNode().asBlockPos().toShortString(), path.getNodeCount());
        }
        return path != null && path.canReach();
    }

    @GameTest(template = PLATFORM, batch = "fire_fortress_paths", timeoutTicks = 900)
    public static void eachFloorsWayDownIsReachableOnFootFromTheDoor(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            FortressPlan.Built plan = FortressPlan.get();
            List<Entity> made = new ArrayList<>();
            onEnd(h, () -> made.forEach(Entity::discard));
            for (int k = 1; k <= FortressPlan.FLOORS; k++) {
                FortressPlan.Floor f = plan.floor(k);
                int[] in = mid(f.landing()), guard = {f.guardian().x(), f.guardian().y(), f.guardian().z()};
                // across the floor to its guardian at the way down, the seal still shut
                check(h, FireFortress.sealed(level, o, k), "floor " + k + " sealed");
                check(h, walksAlong(level, o, in, guard, made), "floor " + k + " (" + f.name() + "): from where one comes in to the way down");
                // and, the seal open, on down the stair to the next floor's landing
                FireFortress.openSeal(level, o, k);
                check(h, walks(level, at(o, guard), at(o, mid(plan.floor(k + 1).landing())), made), "floor " + k + ": down the stair to " + plan.floor(k + 1).name());
            }
            check(h, walksAlong(level, o, mid(plan.floor(9).landing()), new int[]{0, FortressPlan.THRONE_Y + 1, 2}, made), "into the throne room to his dais");
            // the seals laid again, for the tests after
            for (int k = 1; k <= FortressPlan.FLOORS; k++) {
                int[] b = plan.floor(k).seal();
                FortressBlocks.layBox(level, o, b[0], b[1], b[2], b[3], b[4], b[5]);
            }
            h.succeed();
        });
    }

    // ── The garrisons ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "fire_fortress_muster", timeoutTicks = 900)
    public static void eachFloorIsMusteredOnItsOwnFloorHarderEachFloorDown(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            FortressPlan.Built plan = FortressPlan.get();
            double last = 0;
            for (int k = 1; k <= FortressPlan.FLOORS; k++) {
                List<Mob> g = FireFortress.muster(level, o, k);
                g.forEach(m -> m.setNoAi(true));
                eq(h, plan.floor(k).garrison().size() + 1, g.size(), "floor " + k + ": its garrison and its guardian");
                eq(h, g.size(), FireFortress.garrison(level, o, k).size(), "floor " + k + " counted as its own");
                double sum = 0;
                int captains = 0, guards = 0;
                for (Mob m : g) {
                    BlockPos p = m.blockPosition();
                    eq(h, k, plan.floorAt(p.getX() - o.getX(), p.getY() - o.getY(), p.getZ() - o.getZ()), "on floor " + k + ": " + m);
                    check(h, level.noCollision(m), "standing free, not in the rock: " + m + " at " + p);
                    check(h, m.isPersistenceRequired(), "kept");
                    check(h, m.hasRestriction(), "kept to its post");
                    ZoneMonster z = (ZoneMonster) m;
                    sum += z.mobLevel();
                    if (z.isCaptain()) captains++;
                    if (FireFortress.guards(m)) guards++;
                }
                eq(h, 1, guards, "floor " + k + ": one guardian");
                check(h, captains >= (k >= 4 ? 4 : 2), "floor " + k + ": captains " + captains);
                double mean = sum / g.size();
                check(h, mean > last, "floor " + k + " harder than the one over it: " + mean + " after " + last);
                last = mean;
                Mob guard = g.stream().filter(FireFortress::guards).findFirst().orElseThrow();
                if (k <= 6) check(h, guard instanceof StrongGeneralEntity, "floor " + k + " held by one of the Six Strong Generals");
                check(h, ((ZoneMonster) guard).mobLevel() == FortressPlan.guardianLevel(k), "the guardian's level");
                for (Mob m : g) m.discard();
            }
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "fire_fortress_seal", timeoutTicks = 900)
    public static void aGuardiansDeathOpensHisSeal(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            int[] b = FortressPlan.get().floor(3).seal();
            FortressBlocks.layBox(level, o, b[0], b[1], b[2], b[3], b[4], b[5]);
            check(h, FireFortress.sealed(level, o, 3), "the grate shut");
            List<Mob> g = FireFortress.muster(level, o, 3);
            g.forEach(m -> m.setNoAi(true));
            Mob guard = g.stream().filter(FireFortress::guards).findFirst().orElseThrow();
            // a common demon's death opens nothing
            g.stream().filter(m -> !FireFortress.guards(m)).findFirst().orElseThrow().kill();
            check(h, FireFortress.sealed(level, o, 3), "still shut");
            guard.kill();
            check(h, !FireFortress.sealed(level, o, 3), "his death opens it");
            for (int x = b[0]; x <= b[3]; x++) for (int y = b[1]; y <= b[4]; y++) for (int z = b[2]; z <= b[5]; z++) {
                check(h, level.getBlockState(o.offset(x, y, z)).isAir(), "open at " + x + " " + y + " " + z);
            }
            FortressBlocks.layBox(level, o, b[0], b[1], b[2], b[3], b[4], b[5]);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "fire_fortress_cleared", timeoutTicks = 900)
    public static void aClearedFloorStaysEmptyUntilItsTimeComesRound(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            ServerPlayer p = player(h);
            FortressPlan.Floor two = FortressPlan.get().floor(2);
            // someone on the first floor: the second is mustered
            p.moveTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 10.5);
            FireFortress.State.of(level).setStatus(o, FireFortress.State.NEW);
            FireFortress.garrisons(level, o, List.of(p));
            int mustered = FireFortress.garrison(level, o, 2).size();
            eq(h, two.garrison().size() + 1, mustered, "the floor below mustered");
            check(h, FireFortress.garrison(level, o, 4).isEmpty(), "but not ones further down");
            // all killed: it stays empty
            FireFortress.garrison(level, o, 2).forEach(Mob::kill);
            FireFortress.garrisons(level, o, List.of(p));
            check(h, FireFortress.garrison(level, o, 2).isEmpty(), "a cleared floor stays empty");
            check(h, FireFortress.State.of(level).clearedAt(o, 2) > 0, "and is marked cleared");
            sweep(level, o);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "fire_fortress_leash", timeoutTicks = 900)
    public static void theGarrisonsAndRedBoyKeepToTheirFloors(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            FortressPlan.Built plan = FortressPlan.get();
            List<Mob> g = FireFortress.muster(level, o, 3);
            Mob imp = g.stream().filter(m -> !FireFortress.guards(m)).findFirst().orElseThrow();
            // down the stair onto the fourth floor's landing: taken back
            BlockPos below = at(o, mid(plan.floor(4).landing()));
            imp.teleportTo(below.getX() + 0.5, below.getY(), below.getZ() + 0.5);
            RedBoyEntity boy = ModEntities.RED_BOY.get().create(level);
            BlockPos dais = o.offset(0, FortressPlan.THRONE_Y + 1, 0);
            boy.moveTo(dais.getX() + 0.5, dais.getY(), dais.getZ() + 0.5);
            boy.finalizeSpawn(level, level.getCurrentDifficultyAt(dais), MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(boy);
            check(h, boy.getRestrictCenter().equals(dais), "his lair is his dais");
            // up the stair out of his throne room: taken back
            BlockPos up = at(o, mid(plan.floor(8).landing()));
            boy.teleportTo(up.getX() + 0.5, up.getY(), up.getZ() + 0.5);
            int back = FireFortress.leash(level, o);
            check(h, back >= 2, "both taken back: " + back);
            BlockPos ip = imp.blockPosition(), bp = boy.blockPosition();
            eq(h, 3, plan.floorAt(ip.getX() - o.getX(), ip.getY() - o.getY(), ip.getZ() - o.getZ()), "the imp back on its floor");
            eq(h, 9, plan.floorAt(bp.getX() - o.getX(), bp.getY() - o.getY(), bp.getZ() - o.getZ()), "Red Boy back in his throne room");
            check(h, boy.distanceToSqr(Vec3.atBottomCenterOf(dais)) < 4, "on his dais");
            eq(h, 0, FireFortress.leash(level, o), "where they belong, left be");
            h.succeed();
        });
    }

    // ── Red Boy at home ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "fire_fortress_boss", timeoutTicks = 900)
    public static void redBoyFightsInHisThroneRoom(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            FortressPlan.Built plan = FortressPlan.get();
            BlockPos dais = o.offset(0, FortressPlan.THRONE_Y + 1, 0);
            RedBoyEntity boy = ModEntities.RED_BOY.get().create(level);
            boy.moveTo(dais.getX() + 0.5, dais.getY(), dais.getZ() + 0.5);
            boy.finalizeSpawn(level, level.getCurrentDifficultyAt(dais), MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(boy);
            ServerPlayer p = player(h);
            p.moveTo(dais.getX() + 0.5, dais.getY(), dais.getZ() + 6.5);
            h.runAfterDelay(12, () -> {
                boy.updateBossBar(List.of(p));
                check(h, boy.bossBarViewers().contains(p), "his bar shows in his throne room");
                boy.setHealth(boy.getMaxHealth() * 0.6f);
        });
        h.runAfterDelay(16, () -> {
            eq(h, 1, boy.phase(), "two thirds: his carts");
            List<FireCartEntity> carts = boy.carts();
            eq(h, 5, carts.size(), "five carts");
            for (FireCartEntity c : carts) {
                BlockPos cp = c.blockPosition();
                eq(h, 9, plan.floorAt(cp.getX() - o.getX(), cp.getY() - o.getY(), cp.getZ() - o.getZ()), "a cart set out in the throne room");
                check(h, Math.abs(cp.getY() - dais.getY()) <= 1, "on its floor, not in a wall or a pit");
                check(h, level.noCollision(c), "standing free");
            }
            boy.setHealth(boy.getMaxHealth() * 0.3f);
        });
        h.runAfterDelay(20, () -> {
            eq(h, 2, boy.phase(), "a third: fire from his nose and mouth both");
            BlockPos bp = boy.blockPosition();
            eq(h, 9, plan.floorAt(bp.getX() - o.getX(), bp.getY() - o.getY(), bp.getZ() - o.getZ()), "still in his throne room");
            boy.carts().forEach(Entity::discard);
            boy.discard();
            h.succeed();
        });
        });
    }

    // ── The landings ────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "fire_fortress_landing", timeoutTicks = 900)
    public static void aLandingReachedIsWhereTheReturnBrazierBringsOne(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            FortressPlan.Built plan = FortressPlan.get();
            ServerPlayer p = player(h);
            p.getPersistentData().remove(FireFortress.KEY_DEPTH);
            BlockPos brazier = o.offset(FortressPlan.BRAZIER_AT[0], FortressPlan.BRAZIER_AT[1], FortressPlan.BRAZIER_AT[2]);
            check(h, level.getBlockState(brazier).is(Blocks.CAMPFIRE), "the return brazier by the door");
            p.moveTo(brazier.getX() + 0.5, brazier.getY(), brazier.getZ() + 1.5);
            check(h, FireFortress.use(level, o, p, brazier, null), "the brazier answers");
            eq(h, 0, FireFortress.depth(p, o), "nowhere to go back to yet");
            for (int k : new int[]{4, 2}) {
                BlockPos l = at(o, mid(plan.floor(k).landing()));
                p.moveTo(l.getX() + 0.5, l.getY(), l.getZ() + 0.5);
                FireFortress.markLanding(p, o);
            }
            eq(h, 4, FireFortress.depth(p, o), "the deepest landing reached is kept, not the last");
            // the door shut, the brazier will not; open, it brings one down
            check(h, !FireFortress.doorOpen(level, o), "the door shut");
            check(h, !FireFortress.returnDown(level, o, p), "not while the door is shut");
            int[] d = FortressPlan.DOOR;
            for (int x = d[0]; x <= d[3]; x++) for (int y = d[1]; y <= d[4]; y++) for (int z = d[2]; z <= d[5]; z++) level.removeBlock(o.offset(x, y, z), false);
            check(h, FireFortress.returnDown(level, o, p), "open: back down to the landing");
            FortressBlocks.layBox(level, o, d[0], d[1], d[2], d[3], d[4], d[5]);
            h.succeed();
        });
    }

    // ── A world made before ─────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "fire_fortress_old", timeoutTicks = 900)
    public static void chunksMadeBeforeAreFinishedOverAndChangedOnesLeftBe(GameTestHelper h) {
        BlockPos o = fortress(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            // the chunk holding Red Boy's dais made before the plan: stone all through
            int cx = o.getX() >> 4, cz = o.getZ() >> 4;
            var chunk = level.getChunk(cx, cz);
            for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
                for (int y = o.getY() + FortressPlan.Y_MIN; y <= o.getY() + FortressPlan.Y_MAX; y++) {
                    if (chunk.getBlockEntity(p.set(x, y, z)) != null) level.removeBlock(p, false);
                    chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x & 15, y & 15, z & 15, Blocks.STONE.defaultBlockState(), false);
                }
            }
            // the chunk holding the door, made with the plan, its door since opened
            int[] d = FortressPlan.DOOR;
            for (int x = d[0]; x <= d[3]; x++) for (int y = d[1]; y <= d[4]; y++) for (int z = d[2]; z <= d[5]; z++) level.removeBlock(o.offset(x, y, z), false);
            FireFortress.State.of(level).recheck(o);
            int laid = FireFortress.complete(level, o);
            check(h, laid >= 1, "the old chunk finished over: " + laid);
            check(h, level.getBlockState(o.offset(0, FortressPlan.THRONE_Y, 0)).is(Blocks.GILDED_BLACKSTONE), "his dais laid again");
            check(h, level.getBlockState(o.offset(0, FortressPlan.THRONE_Y + 1, 0)).isAir(), "room over it");
            check(h, FireFortress.doorOpen(level, o), "the opened door left open");
            check(h, FireFortress.State.of(level).completed(o), "every chunk looked over");
            eq(h, 0, FireFortress.complete(level, o), "and not again");
            FortressBlocks.layBox(level, o, d[0], d[1], d[2], d[3], d[4], d[5]);
            h.succeed();
        });
    }

    // ── Where it is ─────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theLuopanPointsToTheGreatDoor(GameTestHelper h) {
        long seed = Zones.seed();
        int[] c = Zones.FIRE_MOUNTAIN.centres(seed)[0];
        EarthGod.Answer a = EarthGod.answer(Level.OVERWORLD, new Vec3(0, 64, 0), 70, seed, Set.of());
        eq(h, Zones.FIRE_MOUNTAIN, a.zone(), "the Flaming Mountains at 70");
        check(h, a.target() != null, "it points somewhere");
        eq(h, FireFortress.doorTarget(c), a.target().pos(), "to the cave's great door");
        eq(h, c[1] + FortressPlan.FACE_Z + 4, a.target().pos().getZ(), "before the door, at the ravine's head");
        check(h, a.words().getString().contains("door"), "and says so: " + a.words().getString());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nothingOfTheWorldsOwnIsBornInTheCave(GameTestHelper h) {
        BlockPos o = new BlockPos(1000, 70, 1000);
        FortressPlan.Built plan = FortressPlan.get();
        for (int k = 1; k <= FortressPlan.FLOORS; k++) {
            FortressPlan.Spot s = plan.floor(k).garrison().get(0);
            check(h, FireFortress.inside(o, o.offset(s.x(), s.y(), s.z())), "floor " + k);
        }
        check(h, FireFortress.inside(o, o.offset(0, FortressPlan.THRONE_Y + 1, 0)), "the throne room");
        check(h, !FireFortress.inside(o, o.offset(0, 1, FortressPlan.FACE_Z + 10)), "the forecourt is the world's");
        check(h, !FireFortress.inside(o, o.offset(120, 1, 0)), "the range is the world's");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFortressIsLaidAsItsPlanSays(GameTestHelper h) {
        FortressPlan.Built plan = FortressPlan.get();
        BlockState dais = FortressBlocks.state(plan, 0, FortressPlan.THRONE_Y, 0, 0, 0, 0);
        check(h, dais.is(Blocks.GILDED_BLACKSTONE), "his gilded dais");
        BlockState throne = FortressBlocks.state(plan, 0, FortressPlan.THRONE_Y + 4, FortressPlan.THRONE_Z, 0, 0, 0);
        check(h, throne.is(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS)
                && throne.getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.NORTH, "his throne, its back to the wall");
        check(h, FortressBlocks.state(plan, 0, 7, FortressPlan.DOOR[5], 0, 0, 0).is(Blocks.NETHERITE_BLOCK), "the door's band of black iron");
        check(h, FortressBlocks.state(plan, 3, 4, FortressPlan.DOOR[5], 0, 0, 0).is(Blocks.WAXED_CUT_COPPER), "its bronze");
        int[] s = plan.floor(1).seal();
        BlockState bars = FortressBlocks.state(plan, s[0], s[1], s[2], 0, 0, 0);
        check(h, bars.is(Blocks.IRON_BARS), "a grate across the way down");
        check(h, FortressBlocks.state(plan, 3, FortressPlan.THRONE_Y + 4, FortressPlan.THRONE_Z - 2, 0, 0, 0).is(Blocks.CHEST), "his treasury");
        check(h, FortressBlocks.state(plan, 0, 1, 0, 0, 0, 0).isAir() || FortressBlocks.state(plan, 0, 1, 0, 0, 0, 0).is(Blocks.RED_CARPET), "the court open");
        check(h, FortressBlocks.state(plan, 200, 0, 0, 0, 0, 0) == null, "the range left alone outside it");
        h.succeed();
    }
}
