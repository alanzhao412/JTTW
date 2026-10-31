package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.blackwind.BearSoldierEntity;
import com.jttw.engine.entity.blackwind.BlackBearDemonEntity;
import com.jttw.engine.entity.blackwind.BlackWindChief;
import com.jttw.engine.entity.blackwind.LingxuziEntity;
import com.jttw.engine.entity.blackwind.WhiteRobedScholarEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.world.blackwind.BlackWindBlocks;
import com.jttw.engine.world.blackwind.BlackWindCave;
import com.jttw.engine.world.blackwind.BlackWindPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Black Wind Cave, laid whole off the test grid: its door barred while its two keepers (Lingxuzi and
 * the White-Robed Scholar, minibosses of the band) live and blown open after; its garrisons mustered, kept
 * to their parts of the cave; the warden of the grand stair, whose death opens the grate; the Black Bear
 * Demon kept to his throne cavern; the door and the grate not yielding to picks.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlackWindCaveGameTests {

    private static final BlockPos CAVE = new BlockPos(30016, 120, -30016);
    private static boolean laid;
    private static final TicketType<ChunkPos> HELD = TicketType.create("jttw_gametest_black_wind_cave", java.util.Comparator.comparingLong(ChunkPos::toLong), 20 * 60 * 10);

    private static void force(ServerLevel level, BlockPos o) {
        for (int cx = (o.getX() + BlackWindPlan.X_MIN) >> 4; cx <= (o.getX() + BlackWindPlan.X_MAX) >> 4; cx++) {
            for (int cz = (o.getZ() + BlackWindPlan.Z_MIN) >> 4; cz <= (o.getZ() + BlackWindPlan.Z_MAX + 24) >> 4; cz++) {
                ChunkPos c = new ChunkPos(cx, cz);
                level.getChunkSource().addRegionTicket(HELD, c, 2, c, true);
                level.getChunk(cx, cz);
            }
        }
    }

    private static void sweep(ServerLevel level, BlockPos o) {
        level.getEntities((Entity) null, BlackWindCave.bounds(o).inflate(16), e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
    }

    /** The whole cave, laid once a run straight into its chunks' sections (quietly, as the Fire Cloud Cave's tests do), its forecourt floored. */
    private static BlockPos cave(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        force(level, CAVE);
        if (!laid) {
            BlackWindPlan.Built plan = BlackWindPlan.get();
            List<BlockPos> chests = new ArrayList<>();
            for (int x = BlackWindPlan.X_MIN; x <= BlackWindPlan.X_MAX; x++) for (int z = BlackWindPlan.Z_MIN; z <= BlackWindPlan.Z_MAX; z++) {
                int wx = CAVE.getX() + x, wz = CAVE.getZ() + z;
                var chunk = level.getChunk(wx >> 4, wz >> 4);
                for (int y = BlackWindPlan.Y_MIN; y <= BlackWindPlan.Y_MAX; y++) {
                    int wy = CAVE.getY() + y;
                    BlockState st = BlackWindBlocks.state(plan, x, y, z, wx, wy, wz);
                    if (st == null && y == 0 && z > BlackWindPlan.FACE_Z) st = Blocks.POLISHED_DEEPSLATE.defaultBlockState(); // the forecourt's floor
                    if (st == null) continue;
                    if (st.hasBlockEntity()) {
                        chests.add(new BlockPos(wx, wy, wz));
                        continue;
                    }
                    chunk.getSection(chunk.getSectionIndex(wy)).setBlockState(wx & 15, wy & 15, wz & 15, st, false);
                }
                chunk.setUnsaved(true);
            }
            for (BlockPos c : chests) BlackWindBlocks.lay(level, c, Blocks.CHEST.defaultBlockState(), level.getRandom());
            laid = true;
        }
        BlackWindCave.State.of(level).forget(CAVE);
        sweep(level, CAVE);
        onEnd(h, () -> sweep(level, CAVE));
        return CAVE;
    }

    private static void whenReady(GameTestHelper h, BlockPos o, Runnable body) {
        ServerLevel level = h.getLevel();
        for (int cx = (o.getX() + BlackWindPlan.X_MIN) >> 4; cx <= (o.getX() + BlackWindPlan.X_MAX) >> 4; cx++) {
            for (int cz = (o.getZ() + BlackWindPlan.Z_MIN) >> 4; cz <= (o.getZ() + BlackWindPlan.Z_MAX + 24) >> 4; cz++) {
                if (!level.isPositionEntityTicking(new BlockPos(cx * 16 + 8, o.getY(), cz * 16 + 8))) {
                    h.runAfterDelay(1, () -> whenReady(h, o, body));
                    return;
                }
            }
        }
        body.run();
    }

    @GameTest(template = PLATFORM, batch = "black_wind_cave_door", timeoutTicks = 900)
    public static void theDoorIsBarredWhileItsKeepersLiveThenTheBlackWindBlowsItOpen(GameTestHelper h) {
        BlockPos o = cave(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            List<Mob> court = BlackWindCave.muster(level, o, 0);
            court.forEach(m -> m.setNoAi(true));
            List<BlackWindChief> keepers = BlackWindCave.keepers(level, o);
            eq(h, 2, keepers.size(), "two keepers before the door");
            check(h, keepers.stream().anyMatch(k -> k instanceof LingxuziEntity) && keepers.stream().anyMatch(k -> k instanceof WhiteRobedScholarEntity),
                    "Lingxuzi and the White-Robed Scholar");
            for (BlackWindChief k : keepers) {
                check(h, k.isMiniboss() && k.mobLevel() == BlackWindPlan.guardLevel(0), "a miniboss of level 30");
                check(h, k.mobLevel() <= 36, "within the band");
            }
            ServerPlayer p = player(h);
            p.setGameMode(GameType.SURVIVAL);
            p.moveTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + BlackWindPlan.FACE_Z + 4.5);
            BlockPos ring = o.offset(2, 7, BlackWindPlan.DOOR[5]);
            check(h, BlackWindBlocks.doorish(level.getBlockState(ring)), "the door stands");
            check(h, BlackWindCave.use(level, o, p, ring, null), "it answers a knock");
            check(h, !BlackWindCave.doorOpen(level, o), "barred while its keepers live");
            for (BlackWindChief k : keepers) check(h, k.getTarget() == p, "the keepers turn on the knocker");
            check(h, BlackWindCave.guarded(level, o, ring), "the door does not yield to picks");
            BlackWindCave.look(level, o, List.of(p));
            check(h, !BlackWindCave.doorOpen(level, o), "coming near does not open it while they live");
            for (BlackWindChief k : keepers) k.kill();
            check(h, BlackWindCave.keepers(level, o).isEmpty(), "the keepers are dead");
            BlackWindCave.look(level, o, List.of(p)); // someone near the door, its keepers dead: the black wind blows it open
            String why = "mustered " + BlackWindCave.State.of(level).mustered(o, 0) + ", keepers " + BlackWindCave.keepers(level, o).size()
                    + ", loaded " + BlackWindCave.loaded(level, o) + ", open " + BlackWindCave.doorOpen(level, o) + ", near " + Math.sqrt(p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(o.offset(0, 1, BlackWindPlan.FACE_Z + 1))));
            h.runAfterDelay(40, () -> {
                check(h, BlackWindCave.doorOpen(level, o), "blown open (" + why + ")");
                int[] d = BlackWindPlan.DOOR;
                for (int x = d[0]; x <= d[3]; x++) for (int y = d[1]; y <= d[4]; y++) for (int z = d[2]; z <= d[5]; z++) {
                    check(h, level.getBlockState(o.offset(x, y, z)).isAir(), "the doorway clear at " + x + " " + y + " " + z);
                }
                check(h, !BlackWindCave.knock(level, o, p), "and it stays open");
                // the door laid again, for any test after
                BlackWindBlocks.layBox(level, o, d[0], d[1], d[2], d[3], d[4], d[5]);
                h.succeed();
            });
        });
    }

    @GameTest(template = PLATFORM, batch = "black_wind_cave_court", timeoutTicks = 900)
    public static void theCourtIsMusteredItsWardensDeathOpensTheGrate(GameTestHelper h) {
        BlockPos o = cave(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            List<Mob> g = BlackWindCave.muster(level, o, 1);
            g.forEach(m -> m.setNoAi(true));
            BlackWindPlan.Floor f = BlackWindPlan.get().floor(1);
            eq(h, f.garrison().size() + 1, g.size(), "its garrison and its warden");
            eq(h, g.size(), BlackWindCave.garrison(level, o, 1).size(), "counted as the court's");
            Mob warden = null;
            for (Mob m : g) {
                check(h, m instanceof ZoneMonster z && z.mobLevel() >= 20 && z.mobLevel() <= 36, "of the band: " + m);
                check(h, BlackWindCave.floorOf(o, m) == 1, "on the court's floor");
                if (BlackWindCave.guards(m)) warden = m;
            }
            check(h, warden instanceof BearSoldierEntity b && b.isMiniboss(), "a Black Bear Soldier miniboss holds the stair");
            check(h, BlackWindCave.sealed(level, o), "the grate shut");
            int[] s = BlackWindPlan.SEAL_BOX;
            check(h, BlackWindCave.guarded(level, o, o.offset(0, s[1] + 1, s[2])), "the grate does not yield to picks");
            warden.kill();
            check(h, !BlackWindCave.sealed(level, o), "his death throws it open");
            BlackWindBlocks.layBox(level, o, s[0], s[1], s[2], s[3], s[4], s[5]);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "black_wind_cave_leash", timeoutTicks = 900)
    public static void eachIsKeptToItsPartOfTheCaveAndTheKingToHisThroneCavern(GameTestHelper h) {
        BlockPos o = cave(h);
        whenReady(h, o, () -> {
            ServerLevel level = h.getLevel();
            List<Mob> g = BlackWindCave.muster(level, o, 1);
            Mob stray = g.get(0);
            BlockPos post = stray.getRestrictCenter();
            // down the grand stair into the throne cavern: taken back
            stray.teleportTo(o.getX() + 0.5, o.getY() + BlackWindPlan.THRONE_Y, o.getZ() + BlackWindPlan.THRONE_Z + 6.5);
            BlackBearDemonEntity king = BlackWindRegistry.BLACK_BEAR_DEMON.get().create(level);
            BlockPos throne = o.offset(0, BlackWindPlan.THRONE_Y, BlackWindPlan.THRONE_Z);
            king.moveTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5);
            king.finalizeSpawn(level, level.getCurrentDifficultyAt(throne), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(king);
            check(h, king.getRestrictCenter().equals(throne), "his lair is his post");
            check(h, BlackWindPlan.get().standable(0, BlackWindPlan.THRONE_Y, BlackWindPlan.THRONE_Z), "his post on open floor");
            // up into the court: taken back
            king.teleportTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5);
            int n = BlackWindCave.leash(level, o);
            check(h, n >= 2, "both taken back: " + n);
            check(h, stray.blockPosition().closerThan(post, 2), "the stray at its post");
            check(h, king.blockPosition().closerThan(throne, 2), "the king before his dais");
            h.succeed();
        });
    }
}
