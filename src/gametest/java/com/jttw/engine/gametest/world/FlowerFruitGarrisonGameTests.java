package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.FlowerFruitGarrison;
import com.jttw.engine.world.flowerfruit.IslandShape;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * Flower-Fruit Mountain's garrisons come back as Black Wind's do: the Water Curtain Cave's and each
 * camp's band, all dead, stay empty {@value FlowerFruitGarrison#RESPAWN_MINUTES} minutes, then are
 * mustered again when someone comes near while no one is in them. Worked at an island of the tests'
 * own, far out, its chunks held (no island is built there: the holders are counted, not fought).
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlowerFruitGarrisonGameTests {

    private static final int[] C = {-40000, 40016};
    private static final TicketType<ChunkPos> HELD = TicketType.create("jttw_gametest_ff_garrison", java.util.Comparator.comparingLong(ChunkPos::toLong), 20 * 60 * 10);

    private static void hold(ServerLevel level, net.minecraft.world.phys.AABB b) {
        for (int cx = (int) Math.floor(b.minX) >> 4; cx <= (int) Math.floor(b.maxX) >> 4; cx++) {
            for (int cz = (int) Math.floor(b.minZ) >> 4; cz <= (int) Math.floor(b.maxZ) >> 4; cz++) {
                ChunkPos c = new ChunkPos(cx, cz);
                level.getChunkSource().addRegionTicket(HELD, c, 2, c, true);
            }
        }
    }

    private static void sweep(ServerLevel level, long seed, int part) {
        level.getEntities((Entity) null, FlowerFruitGarrison.bounds(seed, C, part).inflate(48),
                e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
    }

    private static void whenLoaded(GameTestHelper h, long seed, int part, Runnable body) {
        if (!FlowerFruitGarrison.loaded(h.getLevel(), seed, C, part)) {
            h.runAfterDelay(5, () -> whenLoaded(h, seed, part, body));
            return;
        }
        body.run();
    }

    /** Freezes them where they stand: no island is built here, and they are only counted. */
    private static void still(List<Mob> ms) {
        ms.forEach(m -> {
            m.setNoAi(true);
            m.setNoGravity(true);
        });
    }

    private static void returns(GameTestHelper h, int part) {
        ServerLevel level = h.getLevel();
        long seed = level.getSeed();
        hold(level, FlowerFruitGarrison.bounds(seed, C, part));
        FlowerFruitGarrison.State.of(level).forget(C);
        sweep(level, seed, part);
        onEnd(h, () -> sweep(level, seed, part));
        ServerPlayer p = player(h);
        whenLoaded(h, seed, part, () -> {
            List<Mob> g = FlowerFruitGarrison.muster(level, seed, C, part);
            still(g);
            int size = part == FlowerFruitGarrison.CAVE ? CavePlan.POSTS.size() : g.size();
            eq(h, size, g.size(), "all its posts held");
            check(h, part != FlowerFruitGarrison.CAVE || g.stream().filter(m -> m instanceof FakePilgrimEntity).count() == 3, "the three fake pilgrims among them");
            check(h, part == FlowerFruitGarrison.CAVE || (g.size() >= 4 && g.size() <= 5), "a camp's band: four or five");
            eq(h, g.size(), FlowerFruitGarrison.members(level, seed, C, part).size(), "counted as the part's");
            for (Mob m : g) eq(h, part, FlowerFruitGarrison.partOf(m), "marked with its part");
            // someone near, outside it
            BlockPos near = part == FlowerFruitGarrison.CAVE ? new BlockPos(C[0], IslandShape.BASIN_Y + 2, C[1] + IslandShape.CLIFF_Z + 30)
                    : FlowerFruitGarrison.campMiddle(seed, C, part - FlowerFruitGarrison.CAMP).offset(FlowerFruitGarrison.IN_CAMP + 6, 0, 0);
            p.moveTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
            check(h, FlowerFruitGarrison.near(seed, C, part, p) && !FlowerFruitGarrison.in(seed, C, part, p), "near it, not in it: " + p.position());
            check(h, FlowerFruitGarrison.look(level, seed, C, List.of(p)).isEmpty(), "held: nothing mustered");
            // all killed: it stays empty, marked cleared
            g.forEach(Mob::kill);
            FlowerFruitGarrison.look(level, seed, C, List.of(p));
            long cleared = FlowerFruitGarrison.State.of(level).clearedAt(C, part);
            check(h, cleared > 0, "marked cleared");
            check(h, !FlowerFruitGarrison.look(level, seed, C, List.of(p)).contains(part) && FlowerFruitGarrison.members(level, seed, C, part).isEmpty(),
                    "a cleared part stays empty");
            // its time come round, but someone in it: still empty
            FlowerFruitGarrison.State.of(level).cleared(C, part, level.getGameTime() - FlowerFruitGarrison.RESPAWN - 1);
            BlockPos in = part == FlowerFruitGarrison.CAVE ? new BlockPos(C[0], IslandShape.BASIN_Y + 2, C[1] + 10)
                    : FlowerFruitGarrison.campMiddle(seed, C, part - FlowerFruitGarrison.CAMP);
            p.moveTo(in.getX() + 0.5, in.getY(), in.getZ() + 0.5);
            check(h, FlowerFruitGarrison.in(seed, C, part, p), "in it: " + p.position());
            check(h, !FlowerFruitGarrison.look(level, seed, C, List.of(p)).contains(part), "not mustered under someone's nose");
            // and with no one in it, someone near: mustered again
            p.moveTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
            check(h, FlowerFruitGarrison.look(level, seed, C, List.of(p)).contains(part), "its time come round: mustered again");
            List<Mob> again = FlowerFruitGarrison.members(level, seed, C, part);
            still(again);
            eq(h, size, again.size(), "the whole of it back");
            eq(h, 0L, FlowerFruitGarrison.State.of(level).clearedAt(C, part), "held again");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "ff_garrison_cave", timeoutTicks = 900)
    public static void theWaterCurtainCavesGarrisonComesBack(GameTestHelper h) {
        returns(h, FlowerFruitGarrison.CAVE);
    }

    @GameTest(template = PLATFORM, batch = "ff_garrison_camp", timeoutTicks = 900)
    public static void aCampsBandComesBack(GameTestHelper h) {
        if (IslandShape.camps(h.getLevel().getSeed()).isEmpty()) {
            h.succeed();
            return;
        }
        returns(h, FlowerFruitGarrison.CAMP);
    }
}
