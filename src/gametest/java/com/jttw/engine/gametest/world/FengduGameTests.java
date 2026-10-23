package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fengdu.Fengdu;
import com.jttw.engine.world.fengdu.FengduCity;
import com.jttw.engine.world.fengdu.FengduGate;
import com.jttw.engine.world.fengdu.GateRules;
import com.jttw.engine.world.fengdu.GhostGateBlock;
import com.jttw.engine.world.hell.HellRules;
import com.jttw.engine.world.hell.HellTerrain;
import com.jttw.engine.world.hell.HellWorks;
import com.jttw.engine.world.hell.TenCourts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * Fengdu, the Ghost City: its Gate of Ghosts, burning, takes anyone it lets pass (any character,
 * any level) down to Hell's arrival plaza, and no one else; the side doors of Hell's Gate of Ghosts
 * take them back up to it, before its gate, which burns for them (the city laid at its place in
 * the test world); the Earth Gods tell Hell's band the way in, by an Ancient City, and in Hell show
 * the way back up; and the city's blueprint: the gate's way barred, its side doors' veils, the gate
 * reachable from outside the walls, room to stand where one comes back, the palace and the temples
 * open, every sign on a wall. The way in by an Ancient City's frame and the swarm: AncientGateGameTests.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FengduGameTests {

    /** As HellGameTests: a fake player taken to another world must leave its player list. */
    private static void leaveWorld(ServerPlayer p) {
        p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** Stands {@code p} at {@code at} (in the level they are in) and runs the gates one tick less than it takes to cross; checks it has not. */
    private static void standAlmost(GameTestHelper h, ServerPlayer p, BlockPos at) {
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        check(h, Fengdu.inVeil(p), "standing in the veil at " + at.toShortString());
        for (int i = 1; i < Fengdu.CROSS_TICKS; i++) check(h, !Fengdu.tick(p), "not yet taken, " + i + " ticks in");
    }

    /**
     * Checks {@code p} was taken to Hell's arrival plaza: in Hell, where travellers arrive (a test
     * player ignores the move itself, as it ignores every teleport: it is put there by hand).
     */
    private static void onThePlaza(GameTestHelper h, ServerPlayer p, String who) {
        eq(h, JTTWDimensions.HELL, p.level().dimension(), who + ": in Hell");
        BlockPos arrival = JTTWDimensions.findSafeSpawn(p.serverLevel());
        p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
        eq(h, HellTerrain.GROUND + 1, p.blockPosition().getY(), who + ": standing on the plaza");
        check(h, HellRules.fromArrival(p.getX(), p.getZ()) < HellWorks.PLAZA_RADIUS, who + ": on the plaza, at " + p.blockPosition().toShortString());
        check(h, HellRules.inSanctuary(p), who + ": in its sanctuary");
        check(h, !Fengdu.inVeil(p), who + ": out of the veils, so not sent straight back");
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "fengdu_gate_anyone")
    public static void anyoneTheBurningGateLetsPassIsTakenDownToHellsPlazaAndNoOneElse(GameTestHelper h) {
        ServerLevel level = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        BlockPos arrival = FengduGate.arrival(ground);
        BlockPos veil = ground.offset(0, FengduCity.TERRACE_H + 1, FengduCity.GHOST_Z);
        // one who walked there, the gate burning for another: the mist will not take them
        ServerPlayer opener = player(h);
        FengduGate.arriveFromHell(opener, ground);
        check(h, level.getBlockState(veil).is(ModBlocks.GHOST_GATE.get()), "the gate burns");
        ServerPlayer walker = player(h);
        walker.moveTo(veil.getX() + 0.5, veil.getY(), veil.getZ() + 0.5);
        for (int t = 0; t < Fengdu.CROSS_TICKS + 10; t++) check(h, !Fengdu.tick(walker), "not taken, one who never broke the guard");
        eq(h, Level.OVERWORLD, walker.level().dimension(), "still in the world of the living");
        // one not yet reborn as anything, at level 1; a Peng, a dragon, a tortoise, the Bull Demon King, of every band
        ReincarnationOutcome[] kinds = {null, ReincarnationOutcome.GOLD_WINGED_PENG, ReincarnationOutcome.DRAGON,
                ReincarnationOutcome.TURTLE, ReincarnationOutcome.BULL_DEMON_KING};
        int[] levels = {1, 25, 60, 66, 100};
        for (int i = 0; i < kinds.length; i++) {
            ServerPlayer p = player(h);
            String who = (kinds[i] == null ? "no one yet" : kinds[i].name()) + " at level " + levels[i];
            if (kinds[i] != null) {
                AlignmentData.markReincarnated(p, kinds[i]);
                LevelManager.initClass(p, kinds[i]);
            }
            PlayerLevel.setLevel(p, levels[i]);
            p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
            FengduGate.setTrip(p, GateRules.Trip.ADMITTED); // they broke the guard (AncientGateGameTests fights it)
            if (i == 0) {
                // stepping out of the veil before it takes them, it lets them go: the count starts again
                p.moveTo(veil.getX() + 0.5, veil.getY(), veil.getZ() + 0.5);
                for (int t = 0; t < Fengdu.CROSS_TICKS - 2; t++) Fengdu.tick(p);
                p.moveTo(veil.getX() + 0.5, veil.getY(), veil.getZ() + 2.5);
                check(h, !Fengdu.inVeil(p) && !Fengdu.tick(p), "out of it, nothing");
                eq(h, Level.OVERWORLD, p.level().dimension(), "still in the world of the living");
            }
            standAlmost(h, p, veil);
            eq(h, Level.OVERWORLD, p.level().dimension(), who + ": not taken yet");
            check(h, Fengdu.tick(p), who + ": taken through");
            onThePlaza(h, p, who);
            leaveWorld(p);
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 600, batch = "fengdu_gate_hell")
    public static void theSideDoorsOfHellsGateLeadBackUpToFengduWhoseGateBurnsForThemAndDownAgain(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = GateSites.freshFengdu(h);
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.WHITE_TIGER);
        LevelManager.initClass(p, ReincarnationOutcome.WHITE_TIGER);
        PlayerLevel.setLevel(p, 58);
        JTTWDimensions.teleportTo(p, JTTWDimensions.HELL);
        ServerLevel hell = p.serverLevel();
        eq(h, JTTWDimensions.HELL, hell.dimension(), "in Hell");
        // the Gate of Ghosts: its side doors filled with the veil, its middle way open for the road north
        int gz = HellWorks.RETURN_DOOR_Z;
        for (int side : new int[]{-1, 1}) {
            for (int x = HellWorks.RETURN_DOOR_X; x <= HellWorks.RETURN_DOOR_X + 1; x++) {
                for (int y = HellTerrain.GROUND + 1; y <= HellTerrain.GROUND + HellWorks.RETURN_DOOR_H; y++) {
                    check(h, hell.getBlockState(new BlockPos(side * x, y, gz)).is(ModBlocks.GHOST_GATE.get()), "the veil in the side door at " + side * x + " " + y);
                }
            }
        }
        for (int x = -3; x <= 3; x++) for (int y = HellTerrain.GROUND + 1; y <= HellTerrain.GROUND + 5; y++) {
            BlockState s = hell.getBlockState(new BlockPos(x, y, gz));
            check(h, !s.is(ModBlocks.GHOST_GATE.get()) && !s.blocksMotion(), "the arch's way open for the road at " + x + " " + y);
        }
        // up through the east door
        standAlmost(h, p, new BlockPos(HellWorks.RETURN_DOOR_X, HellTerrain.GROUND + 1, gz));
        check(h, Fengdu.tick(p), "taken through");
        eq(h, Level.OVERWORLD, p.level().dimension(), "back in the world of the living");
        BlockPos back = Fengdu.returnSpot(overworld); // where it took them (a test player ignores the move: it is put there by hand)
        p.moveTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
        eq(h, ground.offset(FengduCity.RETURN_X, FengduCity.TERRACE_H + 1, FengduCity.RETURN_Z), back, "before Fengdu's Gate of Ghosts, on its terrace");
        check(h, Fengdu.standable(overworld, back), "on firm ground, with room to stand");
        check(h, !Fengdu.inVeil(p), "out of the veil");
        eq(h, GateRules.Trip.ADMITTED, FengduGate.trip(p), "come back up: free to go down again");
        check(h, overworld.getBlockState(back.offset(0, 0, FengduCity.GHOST_Z - FengduCity.RETURN_Z)).is(ModBlocks.GHOST_GATE.get()), "the gate burning just north");
        // and down again through Fengdu's own gate
        standAlmost(h, p, back.offset(0, 0, FengduCity.GHOST_Z - FengduCity.RETURN_Z));
        check(h, Fengdu.tick(p), "taken through again");
        onThePlaza(h, p, "down again");
        leaveWorld(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void theLuopanSendsHellsBandToFengduAndShowsTheWayOut(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[] f = Fengdu.centre(seed);
        for (int lv : new int[]{57, 60, 66}) {
            var a = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, lv, seed, Set.of());
            eq(h, Zones.FENGDU, a.zone(), "level " + lv + ": Fengdu");
            eq(h, new BlockPos(f[0], 64, f[1]), a.target().pos(), "level " + lv + ", no Ancient City known: its needle to Fengdu");
            String words = a.words().getString();
            check(h, words.contains("Diyu") && words.contains("Fengdu") && words.contains("Gate of Ghosts") && words.contains("sealed")
                    && words.contains("Ancient City") && words.contains("soul fire") && words.contains("no such city"), "told the way in: " + words);
        }
        // an Ancient City known: the needle to it, its way told
        BlockPos city = new BlockPos(1200, -27, -40);
        var toCity = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 60, seed, Set.of(), false, city);
        eq(h, city, toCity.target().pos(), "the needle to the Ancient City");
        check(h, toCity.words().getString().contains("nearest Ancient City lies some 1200 blocks to the east"), "its way told: " + toCity.words().getString());
        eq(h, Zones.LION_CAMEL, EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 95, seed, Set.of(), false, city).zone(), "other levels: their zones, as before");
        eq(h, Zones.MYSTICAL_WATERS, EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 56, seed, Set.of()).zone(), "56: the seas still");
        eq(h, Zones.FIRE_MOUNTAIN, EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 67, seed, Set.of()).zone(), "67: the Flaming Mountains");
        var there = EarthGod.answer(Level.OVERWORLD, new Vec3(f[0] + 0.5, 70, f[1] + 0.5), 60, seed, Set.of());
        String atFengdu = there.words().getString();
        check(h, atFengdu.contains("Ghost City's country") && atFengdu.contains("sealed") && atFengdu.contains("Ancient City's frame"),
                "standing there, told the gate is sealed and how it opens: " + atFengdu);
        // the luopan itself, in the overworld at 60
        ServerPlayer p = player(h);
        ItemStack luopan = new ItemStack(ModItems.EARTH_GOD_LUOPAN.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, luopan);
        PlayerLevel.setLevel(p, 60);
        luopan.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        BlockPos needle = luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos();
        check(h, needle.equals(new BlockPos(f[0], 64, f[1])) || needle.equals(EarthGod.cityFor(p, false)),
                "the needle at 60: to an Ancient City known, else Fengdu: " + needle.toShortString());
        // in Hell: on to the Ten Courts; sneaking, the way out, a side door of the Gate of Ghosts
        JTTWDimensions.teleportTo(p, JTTWDimensions.HELL);
        ServerLevel hell = p.serverLevel();
        p.moveTo(-3.5, HellTerrain.GROUND + 1, 2.5);
        luopan.getItem().use(hell, p, InteractionHand.MAIN_HAND);
        eq(h, TenCourts.GATE_Z, luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos().getZ(), "the Ten Courts' gate");
        p.setShiftKeyDown(true);
        luopan.getItem().use(hell, p, InteractionHand.MAIN_HAND);
        BlockPos out = luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos();
        eq(h, new BlockPos(-HellWorks.RETURN_DOOR_X, HellTerrain.GROUND + 1, HellWorks.RETURN_DOOR_Z), out, "sneaking: the nearer side door (west)");
        check(h, hell.getBlockState(out).is(ModBlocks.GHOST_GATE.get()), "its veil there");
        var words = EarthGod.answer(p).words().getString();
        check(h, words.contains("back to the living") && words.contains("Fengdu"), "told the way out: " + words);
        // held while sneaking, the needle keeps to the way out; not sneaking, back to the courts
        p.tickCount = 0;
        luopan.inventoryTick(hell, p, p.getInventory().selected, true);
        eq(h, out, luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos(), "held, sneaking: the way out");
        p.setShiftKeyDown(false);
        luopan.inventoryTick(hell, p, p.getInventory().selected, true);
        eq(h, TenCourts.GATE_Z, luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos().getZ(), "held, standing: the courts");
        leaveWorld(p);
        h.succeed();
    }

    // ── The blueprint ───────────────────────────────────────────────────

    /** Whether one could stand on the blueprint's block at local (x, y, z): solid (the hill's ground under the city where it leaves the ground be). */
    private static boolean solid(int x, int y, int z) {
        BlockState s = FengduCity.at(x, y, z);
        return s == null ? y <= 0 : s.blocksMotion();
    }

    /** Whether the blueprint's local (x, y, z) is open to walk through: not solid, and not the veil (that is a way out of the city). */
    private static boolean open(int x, int y, int z) {
        BlockState s = FengduCity.at(x, y, z);
        if (s == null) return y > 0;
        return !s.blocksMotion() && !s.is(ModBlocks.GHOST_GATE.get());
    }

    private static boolean standable(int x, int y, int z) {
        return solid(x, y - 1, z) && open(x, y, z) && open(x, y + 1, z);
    }

    @GameTest(template = PLATFORM)
    public static void theCitysBlueprintKeepsItsGateReachableAndRoomToStand(GameTestHelper h) {
        int t = FengduCity.TERRACE_H;
        // the Gate of Ghosts' way barred (it burns only when fought open); its side doors' veils lead back to the Ancient City
        for (int x = -FengduCity.VEIL_HALF; x <= FengduCity.VEIL_HALF; x++) for (int y = t + 1; y <= t + FengduCity.VEIL_H; y++) {
            eq(h, FengduCity.sealed(), FengduCity.at(x, y, FengduCity.GHOST_Z), "barred at " + x + " " + y);
            check(h, FengduCity.inVeil(x, y, FengduCity.GHOST_Z), "known as the gate's way");
        }
        int doors = 0;
        for (int x = -FengduCity.SCREEN_TO; x <= FengduCity.SCREEN_TO; x++) for (int y = t + 1; y <= t + FengduCity.VEIL_H; y++) {
            BlockState s = FengduCity.at(x, y, FengduCity.GHOST_Z);
            boolean door = s != null && s.is(ModBlocks.GHOST_GATE.get());
            eq(h, FengduCity.inSideDoor(x, y, FengduCity.GHOST_Z), door, "a side door's veil at " + x + " " + y);
            if (door) {
                eq(h, GhostGateBlock.Way.CITY, s.getValue(GhostGateBlock.WAY), "leading back to the Ancient City");
                doors++;
            }
        }
        eq(h, 2 * 2 * FengduCity.SIDE_DOOR_H, doors, "two side doors, two wide");
        check(h, standable(FengduCity.RETURN_X, t + 1, FengduCity.RETURN_Z), "room to stand where one comes back up");
        // walking in from the hill's top outside the main gate: everywhere one can stand, reached
        int lo = -FengduCity.OUTER - 2, hi = FengduCity.ARCH_Z + 3;
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> todo = new ArrayDeque<>();
        int[] start = {0, 1, FengduCity.OUTER + 2};
        check(h, standable(start[0], start[1], start[2]), "standing before the gate");
        todo.add(start);
        seen.add(BlockPos.asLong(start[0], start[1], start[2]));
        while (!todo.isEmpty()) {
            int[] c = todo.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -3; dy--) {
                    int x = c[0] + d.getStepX(), y = c[1] + dy, z = c[2] + d.getStepZ();
                    if (x < lo || x > -lo || z < lo || z > hi || y < 1 || y > FengduCity.TOP) continue;
                    if (dy == 1 && !open(c[0], c[1] + 2, c[2])) continue; // no room to step up
                    if (!standable(x, y, z)) continue;
                    if (seen.add(BlockPos.asLong(x, y, z))) todo.add(new int[]{x, y, z});
                    break;
                }
            }
        }
        check(h, seen.contains(BlockPos.asLong(0, t + 1, FengduCity.GHOST_Z + 1)), "the Gate of Ghosts reached: one can walk up to its way");
        for (int side : new int[]{-1, 1}) {
            check(h, seen.contains(BlockPos.asLong(side * FengduCity.SIDE_DOOR_OUT, t + 1, FengduCity.GHOST_Z + 1)), "and up to a side door");
        }
        check(h, seen.contains(BlockPos.asLong(FengduCity.RETURN_X, t + 1, FengduCity.RETURN_Z)), "and from where one comes back, out to the city");
        check(h, seen.contains(BlockPos.asLong(0, t + 2, FengduCity.THRONE_Z + 2)), "the palace open, up to King Yama's dais");
        for (int side : new int[]{-1, 1}) {
            check(h, seen.contains(BlockPos.asLong(side * (FengduCity.TEMPLE_OUT - 2), t + 1, (FengduCity.TEMPLE_N + FengduCity.TEMPLE_S) / 2)),
                    "the " + (side < 0 ? "City God's temple" : "Hall of the Judges") + " open, up to its image");
        }
        check(h, seen.contains(BlockPos.asLong(-FengduCity.RING - 1, 1, FengduCity.RING)), "the road round inside the walls");
        check(h, seen.contains(BlockPos.asLong(0, 1, FengduCity.ARCH_Z + 2)), "out under the archway");
        check(h, seen.size() > 2000, "much standing room: " + seen.size());
        // every sign hangs on a wall of the city
        for (FengduCity.Sign s : FengduCity.SIGNS) {
            int bx = s.x() - s.facing().getStepX(), bz = s.z() - s.facing().getStepZ();
            check(h, solid(bx, s.y(), bz) && FengduCity.at(bx, s.y(), bz) != null, "the sign " + s.lines()[0] + " on a wall");
            BlockState own = FengduCity.at(s.x(), s.y(), s.z());
            check(h, own == null || own.isAir(), "the sign " + s.lines()[0] + " in the open");
        }
        h.succeed();
    }
}
