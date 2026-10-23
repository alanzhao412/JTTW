package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.hell.HellMonster;
import com.jttw.engine.entity.hell.YinSoldierEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.fengdu.AncientFrame;
import com.jttw.engine.world.fengdu.AncientGate;
import com.jttw.engine.world.fengdu.Fengdu;
import com.jttw.engine.world.fengdu.FengduCity;
import com.jttw.engine.world.fengdu.FengduGate;
import com.jttw.engine.world.fengdu.GateRules;
import com.jttw.engine.world.fengdu.GateRules.Trip;
import com.jttw.engine.world.fengdu.GateRules.WaveKind;
import com.jttw.engine.world.fengdu.GhostGateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.jttw.engine.gametest.GT.*;

/**
 * The way in to Diyu: soul fire lights an Ancient City's frame (built as vanilla lays it, either
 * way), whose veil takes anyone to Fengdu's terrace, where a swarm of Hell's soldiers rises; the
 * Gate of Ghosts stays sealed until the last falls, then burns and takes the pilgrim down to Hell;
 * the gate's side doors lead back to the city; a traveller over the land gets nowhere; the gate
 * seals again once no one who may pass is near; an old city's open gate is sealed when someone
 * comes; and a soldier of a swarm that is no more is turned away. The tests on the real gate each
 * run in a batch of their own ({@link GateSites}).
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class AncientGateGameTests {

    private static void leaveWorld(ServerPlayer p) {
        p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** Stands {@code p} at {@code at} and runs the gates until it has stood there long enough to cross; returns whether it was taken. */
    private static boolean standThrough(ServerPlayer p, BlockPos at) {
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        boolean took = false;
        for (int i = 0; i < Fengdu.CROSS_TICKS + 5 && !took; i++) took = Fengdu.tick(p);
        return took;
    }

    private static List<HellMonster> soldiers(ServerLevel level, BlockPos ground) {
        List<HellMonster> list = new ArrayList<>();
        for (UUID id : FengduGate.swarm(level, ground)) if (level.getEntity(id) instanceof HellMonster m && m.isAlive()) list.add(m);
        return list;
    }

    private static boolean sealed(ServerLevel level, BlockPos ground) {
        return FengduGate.way(ground).stream().allMatch(at -> level.getBlockState(at).is(Blocks.IRON_BARS));
    }

    private static boolean burning(ServerLevel level, BlockPos ground) {
        return FengduGate.way(ground).stream().allMatch(at -> {
            BlockState s = level.getBlockState(at);
            return s.is(ModBlocks.GHOST_GATE.get()) && s.getValue(GhostGateBlock.WAY) == GhostGateBlock.Way.HELL;
        });
    }

    // ── The frame ───────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void soulFireLitInAnAncientCitysFrameFillsItWithTheVeilWhicheverWayItRuns(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (boolean alongX : new boolean[]{false, true}) {
            AncientFrame.Frame f = GateSites.buildFrame(h, level, GateSites.frameSite(alongX ? 1 : 0), alongX);
            GateSites.lightWithSoulFire(level, f, 7);
            for (int i = 0; i < AncientFrame.WIDTH; i++) for (int j = 0; j < AncientFrame.HEIGHT; j++) {
                BlockState s = level.getBlockState(GateSites.at(f.cell(i, j)));
                check(h, s.is(ModBlocks.GHOST_GATE.get()), "the veil at " + i + " " + j + (alongX ? " along x" : " along z") + ": " + s);
                eq(h, GhostGateBlock.Way.FENGDU, s.getValue(GhostGateBlock.WAY), "leading to Fengdu");
                eq(h, alongX ? Direction.Axis.X : Direction.Axis.Z, s.getValue(GhostGateBlock.AXIS), "in the frame's plane");
            }
            check(h, level.getBlockState(GateSites.at(f.cell(-1, 0))).is(Blocks.REINFORCED_DEEPSLATE), "its ring untouched");
            int[] foot = f.foot();
            eq(h, f, AncientGate.Lit.of(level).nearest(foot[0], foot[2]), "remembered among the lit frames");
        }
        // a frame whose ring is broken: the soul fire only burns
        AncientFrame.Frame broken = GateSites.buildFrame(h, level, GateSites.frameSite(2), true);
        level.setBlockAndUpdate(GateSites.at(broken.cell(AncientFrame.WIDTH, 3)), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        GateSites.lightWithSoulFire(level, broken, 4);
        check(h, level.getBlockState(GateSites.at(broken.cell(4, 1))).is(Blocks.SOUL_FIRE), "a broken frame: soul fire, and nothing more");
        check(h, !level.getBlockState(GateSites.at(broken.cell(10, 3))).is(ModBlocks.GHOST_GATE.get()), "no veil");
        // nor does ordinary fire light it
        AncientFrame.Frame plain = GateSites.buildFrame(h, level, GateSites.frameSite(3), false);
        BlockPos rack = GateSites.at(plain.cell(9, 0));
        level.setBlockAndUpdate(rack, Blocks.NETHERRACK.defaultBlockState());
        level.setBlockAndUpdate(rack.above(), Blocks.FIRE.defaultBlockState());
        check(h, !level.getBlockState(GateSites.at(plain.cell(2, 3))).is(ModBlocks.GHOST_GATE.get()), "ordinary fire: no veil");
        h.succeed();
    }

    // ── The whole way down ──────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 900, batch = "fengdu_gate_trip")
    public static void throughTheFrameToFengduWhereTheSwarmRisesAndOnlyWhenAllAreDownDoesTheGateBurnAndLeadToHell(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        AncientFrame.Frame f = GateSites.buildFrame(h, overworld, GateSites.frameSite(4), false);
        GateSites.lightWithSoulFire(overworld, f, 10);
        ServerPlayer p = player(h);
        PlayerLevel.setLevel(p, 12);
        // into the frame's veil from its east side: to Fengdu
        BlockPos in = GateSites.at(f.cell(10, 0));
        p.moveTo(in.getX() + 0.7, in.getY(), in.getZ() + 0.5);
        boolean took = false;
        for (int i = 0; i < Fengdu.CROSS_TICKS + 2 && !took; i++) took = Fengdu.tick(p);
        check(h, took, "taken through the frame");
        eq(h, Level.OVERWORLD, p.level().dimension(), "still among the living");
        eq(h, f, AncientGate.remembered(p), "the city remembered");
        eq(h, Trip.FIGHTING, FengduGate.trip(p), "come to fight");
        check(h, FengduGate.swarmUp(overworld, ground), "a swarm to rise");
        check(h, sealed(overworld, ground), "the gate barred");
        BlockPos arrival = Fengdu.returnSpot(overworld);
        eq(h, FengduGate.arrival(ground), arrival, "brought before the Gate of Ghosts, on the terrace");
        p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5); // where it took them (a test player ignores the move)
        check(h, !Fengdu.inVeil(p) && Fengdu.standable(overworld, arrival), "on firm ground, out of the veils");
        check(h, !standThrough(p, ground.offset(0, FengduCity.TERRACE_H + 1, FengduCity.GHOST_Z)), "the gate's way barred: nothing to step into");
        List<ServerPlayer> near = List.of(p);
        GateSites.when(h, () -> overworld.isPositionEntityTicking(arrival), 100, "Fengdu's creatures ticking", () -> {
            // the vanguard
            FengduGate.step(overworld, ground, near);
            List<HellMonster> wave = soldiers(overworld, ground);
            eq(h, GateRules.wave(WaveKind.VANGUARD, 1).size(), wave.size(), "the vanguard risen");
            for (HellMonster m : wave) {
                check(h, m instanceof YinSoldierEntity, "Yin soldiers: " + m.getType());
                eq(h, GateRules.BOTTOM_LEVEL, m.mobLevel(), "never under the gate's least, the pilgrim only 12");
                check(h, m.distanceToSqr(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5) <= GateRules.LEASH * GateRules.LEASH, "on the terrace, near");
                eq(h, arrival.getY(), m.blockPosition().getY(), "standing on the terrace");
                check(h, m.isPersistenceRequired(), "never despawning by itself");
            }
            // a soldier that strays is brought back
            HellMonster stray = wave.get(0);
            stray.teleportTo(stray.getX() + 40, stray.getY() - 3, stray.getZ());
            FengduGate.step(overworld, ground, near);
            check(h, stray.distanceToSqr(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5) <= GateRules.LEASH * GateRules.LEASH, "the stray brought back");
            // all but one down: the main body rises, its captain with it; the gate still barred
            for (int i = 1; i < wave.size(); i++) wave.get(i).kill();
            FengduGate.step(overworld, ground, near);
            List<HellMonster> second = soldiers(overworld, ground);
            eq(h, 1 + GateRules.wave(WaveKind.MAIN, 1).size(), second.size(), "the main body risen beside the last of the vanguard");
            check(h, second.stream().filter(ZoneMonster::isCaptain).count() == 1, "a captain among them");
            check(h, sealed(overworld, ground) && !FengduGate.mayPass(p), "still barred");
            second.forEach(Entity::kill);
            FengduGate.step(overworld, ground, near);
            List<HellMonster> third = soldiers(overworld, ground);
            Set<EntityType<?>> kinds = new java.util.HashSet<>();
            third.forEach(m -> kinds.add(m.getType()));
            check(h, kinds.contains(ModEntities.OX_HEAD.get()) && kinds.contains(ModEntities.HORSE_FACE.get()), "the gate's own guards last: " + kinds);
            eq(h, GateRules.wave(WaveKind.GUARD, 1).size(), third.size(), "the last wave");
            check(h, sealed(overworld, ground), "still barred before the last wave");
            third.subList(1, third.size()).forEach(Entity::kill);
            FengduGate.step(overworld, ground, near);
            check(h, sealed(overworld, ground) && FengduGate.swarmUp(overworld, ground), "one left: still barred");
            third.get(0).kill();
            FengduGate.step(overworld, ground, near);
            // the last down: the gate ignites by itself, and lets the pilgrim pass
            check(h, !FengduGate.swarmUp(overworld, ground), "the swarm beaten");
            check(h, burning(overworld, ground), "the gate burns");
            eq(h, Trip.ADMITTED, FengduGate.trip(p), "the pilgrim may pass");
            check(h, standThrough(p, ground.offset(1, FengduCity.TERRACE_H + 2, FengduCity.GHOST_Z)), "taken through the Gate of Ghosts");
            eq(h, JTTWDimensions.HELL, p.level().dimension(), "down in Diyu");
            eq(h, Trip.NONE, FengduGate.trip(p), "the trip over");
            leaveWorld(p);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "fengdu_gate_overland")
    public static void walkingToFengduOverTheLandGetsYouNowhere(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        ServerPlayer walker = player(h);
        PlayerLevel.setLevel(walker, 60);
        BlockPos arrival = FengduGate.arrival(ground);
        walker.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
        FengduGate.step(overworld, ground, List.of(walker));
        check(h, sealed(overworld, ground), "sealed: iron bars across its way");
        check(h, !FengduGate.swarmUp(overworld, ground), "no swarm rises for one who walked");
        BlockPos way = ground.offset(0, FengduCity.TERRACE_H + 1, FengduCity.GHOST_Z);
        check(h, overworld.getBlockState(way).blocksMotion(), "nothing to step into");
        // the gate burning for another, come up from Diyu: still not for the walker
        ServerPlayer back = player(h);
        back.moveTo(arrival.getX() + 1.5, arrival.getY(), arrival.getZ() + 0.5);
        FengduGate.arriveFromHell(back, ground);
        check(h, burning(overworld, ground), "it burns for the one come back up");
        List<String> heard = new ArrayList<>();
        listen(walker, heard);
        check(h, !standThrough(walker, way), "the walker not taken");
        eq(h, Level.OVERWORLD, walker.level().dimension(), "still in the world of the living");
        check(h, heard.stream().anyMatch(s -> s.contains("will not part")), "told the mist will not part: " + heard);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 600, batch = "fengdu_gate_seal")
    public static void theGateSealsAgainOnceNoOneWhoMayPassIsNearAndEveryArrivalByTheFrameIsFought(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        BlockPos arrival = FengduGate.arrival(ground);
        ServerPlayer p = player(h);
        p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
        FengduGate.arriveFromHell(p, ground);
        check(h, burning(overworld, ground), "burning for one come back up from Diyu");
        FengduGate.step(overworld, ground, List.of(p));
        check(h, burning(overworld, ground), "and while they stay");
        // they wander off: their leave lapses; a while later the gate goes out
        p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + GateRules.RANGE + 20);
        Fengdu.tick(p);
        eq(h, Trip.NONE, FengduGate.trip(p), "leave lost on leaving Fengdu");
        FengduGate.step(overworld, ground, List.of());
        check(h, burning(overworld, ground), "not at once");
        h.runAfterDelay(GateRules.SEAL_AFTER + 2 * FengduGate.EVERY, () -> {
            FengduGate.step(overworld, ground, List.of());
            check(h, sealed(overworld, ground), "sealed again once no one who may pass has been near for a while");
            // come up again: it burns; then someone comes by the frame: a swarm rises, and the gate is put out at once
            p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
            FengduGate.arriveFromHell(p, ground);
            check(h, burning(overworld, ground), "burning again");
            ServerPlayer pilgrim = player(h);
            pilgrim.moveTo(arrival.getX() - 1.5, arrival.getY(), arrival.getZ() + 0.5);
            FengduGate.arriveByCity(pilgrim, ground);
            check(h, sealed(overworld, ground) && FengduGate.swarmUp(overworld, ground), "a new arrival by the frame: sealed, a swarm to rise");
            eq(h, Trip.FIGHTING, FengduGate.trip(pilgrim), "they must fight");
            eq(h, Trip.FIGHTING, GateRules.arriveByCity(Trip.ADMITTED), "every time");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300, batch = "fengdu_gate_back")
    public static void fengdusSideDoorsLeadBackToTheAncientCityOneCameBy(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        AncientFrame.Frame f = GateSites.buildFrame(h, overworld, GateSites.frameSite(5), true);
        GateSites.lightWithSoulFire(overworld, f, 3);
        ServerPlayer p = player(h);
        // through the frame from its north side (along x, so its sides are north and south)
        BlockPos in = GateSites.at(f.cell(3, 0));
        p.moveTo(in.getX() + 0.5, in.getY(), in.getZ() + 0.3);
        boolean took = false;
        for (int i = 0; i < Fengdu.CROSS_TICKS + 2 && !took; i++) took = Fengdu.tick(p);
        check(h, took && f.equals(AncientGate.remembered(p)), "to Fengdu, the city remembered");
        // the side doors: veils back to the city
        for (BlockPos door : FengduGate.sideDoors(ground)) {
            BlockState s = overworld.getBlockState(door);
            check(h, s.is(ModBlocks.GHOST_GATE.get()) && s.getValue(GhostGateBlock.WAY) == GhostGateBlock.Way.CITY, "a side door's veil at " + door.toShortString());
        }
        BlockPos door = ground.offset(-FengduCity.SIDE_DOOR_IN, FengduCity.TERRACE_H + 1, FengduCity.GHOST_Z);
        check(h, standThrough(p, door), "taken back through the west side door");
        eq(h, Level.OVERWORLD, p.level().dimension(), "in the world of the living");
        eq(h, Trip.NONE, FengduGate.trip(p), "the trip over");
        BlockPos back = AncientGate.before(overworld, f, -1);
        int[] foot = f.foot();
        check(h, Fengdu.standable(overworld, back), "before the frame, on firm ground: " + back.toShortString());
        check(h, back.getZ() < foot[2] && Math.abs(back.getX() - foot[0]) <= 1, "on the side they went in by");
        check(h, !overworld.getBlockState(back).is(ModBlocks.GHOST_GATE.get()), "out of the veil");
        // one who never came by a city: to the lit frame nearest Fengdu (here, this one)
        ServerPlayer stranger = player(h);
        check(h, AncientGate.remembered(stranger) == null, "no city remembered");
        check(h, standThrough(stranger, door), "taken too, to a lit frame");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300, batch = "fengdu_gate_old")
    public static void anOldCitysOpenGateIsSealedAndGivenItsSideDoorsWhenSomeoneComes(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        // as an older version laid it: the veil in the gate's way, the screen walls whole
        for (BlockPos at : FengduGate.way(ground)) overworld.setBlockAndUpdate(at, ModBlocks.GHOST_GATE.get().defaultBlockState());
        for (BlockPos at : FengduGate.sideDoors(ground)) overworld.setBlockAndUpdate(at, Blocks.RED_TERRACOTTA.defaultBlockState());
        onEnd(h, () -> FengduCity.layGate(overworld, ground));
        ServerPlayer p = player(h);
        BlockPos arrival = FengduGate.arrival(ground);
        p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
        FengduGate.step(overworld, ground, List.of(p));
        check(h, sealed(overworld, ground), "the open gate sealed");
        for (BlockPos at : FengduGate.sideDoors(ground)) {
            check(h, overworld.getBlockState(at).is(ModBlocks.GHOST_GATE.get()), "a side door made at " + at.toShortString());
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300, batch = "fengdu_gate_orphan")
    public static void aSoldierOfASwarmThatIsNoMoreIsTurnedAwayWhenItsChunkLoads(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        YinSoldierEntity s = ModEntities.YIN_SOLDIER.get().create(overworld);
        s.getPersistentData().putLong(FengduGate.KEY_SWARM, 12345L);
        s.getPersistentData().putLong(FengduGate.KEY_GATE, ground.asLong());
        var stale = new EntityJoinLevelEvent(s, overworld, true);
        FengduGate.onJoin(stale);
        check(h, stale.isCanceled(), "a soldier of no standing swarm, loaded again: turned away");
        var fresh = new EntityJoinLevelEvent(s, overworld, false);
        FengduGate.onJoin(fresh);
        check(h, !fresh.isCanceled(), "one rising now is let be");
        YinSoldierEntity plain = ModEntities.YIN_SOLDIER.get().create(overworld);
        var untagged = new EntityJoinLevelEvent(plain, overworld, true);
        FengduGate.onJoin(untagged);
        check(h, !untagged.isCanceled(), "one of no swarm (an egg's) is let be");
        h.succeed();
    }
}
