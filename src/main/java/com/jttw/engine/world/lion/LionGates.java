package com.jttw.engine.world.lion;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.LionCamelRidgeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * The gates of the Lion-Camel Cave ({@link LionFortress}), and what holds the fortress together:
 * <ul>
 *   <li><b>the great door</b> ({@link #DOOR}) holds while any of its wardens stands, the six demons
 *       set in the forecourt before it; once they have fallen it opens to a hand (a player using
 *       it), its leaves drawn back from the middle. The Great Peng's kin open it to him at once.</li>
 *   <li><b>each floor's portcullis</b> (floor 0 to {@value LionFortress#FLOORS}, over the stair
 *       down) rises of itself when the floor's warden falls; one whose warden is gone (or never
 *       was, as in a world made before) opens to a hand.</li>
 *   <li>what is opened stays open: the fortress's gates never close again, so one who falls deep in
 *       it walks back down through the floors he has won.</li>
 *   <li><b>the checkpoints</b>: as a floor's way down opens, the players in its hall will rise
 *       again on its landing, at the head of the stair down ({@link #checkpoint}).</li>
 *   <li><b>the fortress stands</b>: its walls, floors, stairs and gates are proof against picks and
 *       blasts ({@link #onBreak}, {@link #onExplode}); its furnishings are not. A block laid by the
 *       fortress is known by being what the blueprint has there, so ground of a world made before
 *       the fortress was is left as breakable as ever.</li>
 * </ul>
 * Where a fortress is is worked out from the place: the region's centre, the city's floor there
 * (as the ridge worked it out), and the blueprint.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class LionGates {

    private LionGates() {}

    /** The great door's number, among the gates (the floors' are their own: 0 to {@value LionFortress#FLOORS}). */
    public static final int DOOR = -1;

    /** A fortress: its region's centre and its city's floor (world y of local y 0). */
    public record Fortress(int cx, int cz, int floor) {
        public BlockPos world(int lx, int ly, int lz) {
            return new BlockPos(cx + lx, floor + ly, cz + lz);
        }
    }

    /** The fortress whose blueprint covers {@code pos} (or whose forecourt it is on), or null. */
    @Nullable
    public static Fortress near(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return null;
        int[] c = Zones.LION_CAMEL.centre(level.getSeed(), pos.getX(), pos.getZ());
        if (c == null) return null;
        int lx = pos.getX() - c[0], lz = pos.getZ() - c[1];
        if (!LionFortress.covers(lx, lz) && !LionFortress.onForecourt(lx, lz)) return null;
        return new Fortress(c[0], c[1], LionCamelRidgeFeature.cityFloor(level, c[0], c[1]));
    }

    /** The fortress of the region round (x, z), wherever in it (x, z) is; null for none. */
    @Nullable
    public static Fortress of(ServerLevel level, int x, int z) {
        if (!level.dimension().equals(Level.OVERWORLD)) return null;
        int[] c = Zones.LION_CAMEL.centre(level.getSeed(), x, z);
        if (c == null) return null;
        return new Fortress(c[0], c[1], LionCamelRidgeFeature.cityFloor(level, c[0], c[1]));
    }

    /** The gate the blueprint has at local (lx, ly, lz): {@link #DOOR}, a floor's number, or null for none. */
    @Nullable
    public static Integer gateAt(int lx, int ly, int lz) {
        int kind = LionFortress.kind(LionFortress.cell(lx, ly, lz));
        if (kind == LionFortress.DOOR) return DOOR;
        if (kind == LionFortress.GATE) return LionFortress.bandOf(ly);
        return null;
    }

    /** The places (local x, y, z) a gate's leaves or bars fill. */
    public static List<int[]> cells(int gate) {
        List<int[]> out = new ArrayList<>();
        if (gate == DOOR) {
            for (int u = -LionFortress.DOOR_HALF; u <= LionFortress.DOOR_HALF; u++) for (int h = 1; h <= LionFortress.DOOR_H; h++) {
                out.add(new int[]{u, h, LionFortress.DOOR_LZ});
            }
            return out;
        }
        LionFortress.Stair s = LionFortress.stair(gate);
        int y = LionFortress.floorY(gate);
        for (int u = s.u0(); u <= s.u1(); u++) for (int h = 1; h <= LionFortress.AIR; h++) {
            out.add(new int[]{u, y + h, LionFortress.FZ + s.gate()});
        }
        return out;
    }

    /** The middle of a gate, in the world. */
    public static BlockPos centre(Fortress f, int gate) {
        List<int[]> cs = cells(gate);
        int[] mid = cs.get(cs.size() / 2);
        return f.world(mid[0], mid[1], mid[2]);
    }

    /** Whether a gate is shut: any of its leaves or bars still where the blueprint has them. */
    public static boolean isShut(ServerLevel level, Fortress f, int gate) {
        for (int[] c : cells(gate)) {
            BlockState want = LionFortress.at(c[0], c[1], c[2]);
            if (want != null && level.getBlockState(f.world(c[0], c[1], c[2])).is(want.getBlock())) return true;
        }
        return false;
    }

    /** Makes {@code m} a warden of a fortress's gate. */
    public static void makeWarden(LionCamelMonster m, Fortress f, int gate) {
        m.getPersistentData().putInt(LionCamelMonster.KEY_WARDEN, gate);
        m.getPersistentData().putIntArray(LionCamelMonster.KEY_WARDEN_AT, new int[]{f.cx(), f.cz(), f.floor()});
    }

    /** The fortress a warden keeps a gate of: as it was told, else the region's round it. */
    @Nullable
    public static Fortress fortressOf(LionCamelMonster m) {
        int[] at = m.getPersistentData().getIntArray(LionCamelMonster.KEY_WARDEN_AT);
        if (at.length == 3) return new Fortress(at[0], at[1], at[2]);
        return m.level() instanceof ServerLevel level ? of(level, m.getBlockX(), m.getBlockZ()) : null;
    }

    /** The wardens of a gate still standing near it. */
    public static List<LionCamelMonster> wardens(ServerLevel level, Fortress f, int gate) {
        BlockPos at = centre(f, gate);
        return level.getEntitiesOfClass(LionCamelMonster.class, new AABB(at).inflate(72, 40, 72),
                m -> m.isAlive() && m.getPersistentData().contains(LionCamelMonster.KEY_WARDEN)
                        && m.getPersistentData().getInt(LionCamelMonster.KEY_WARDEN) == gate);
    }

    /**
     * A player tries a gate: it opens if its wardens have fallen (or to the Great Peng at once).
     * Returns whether it is opening.
     */
    public static boolean tryOpen(ServerLevel level, Fortress f, int gate, @Nullable Player by) {
        if (!isShut(level, f, gate)) return false;
        int left = wardens(level, f, gate).size();
        boolean peng = by != null && LionCamelRidge.isPeng(by);
        if (left > 0 && !peng) {
            if (by instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal(gate == DOOR
                        ? "The great door will not move while its wardens stand: " + left + " still keep the forecourt."
                        : "The way down is barred. Its warden still holds " + LionFortress.THEMES[gate] + "."), true);
            }
            level.playSound(null, centre(f, gate), SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1f, 0.5f);
            return false;
        }
        if (peng && left > 0 && by instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("The wardens bow to their Third Great King, and open for him."), true);
        }
        open(level, f, gate);
        return true;
    }

    /** An opening under way: what of a gate is left to clear, in order, and how far along. */
    private record Opening(ServerLevel level, List<BlockPos> cells, int every, int[] at) {}

    private static final List<Opening> OPENING = new ArrayList<>();

    /**
     * Opens a gate: the great door's leaves drawn back from the middle, column by column; a
     * portcullis raised, row by row from the bottom. Its blocks are cleared without dropping anything.
     */
    public static void open(ServerLevel level, Fortress f, int gate) {
        List<int[]> cs = new ArrayList<>(cells(gate));
        if (gate == DOOR) cs.sort(Comparator.<int[]>comparingInt(c -> Math.abs(c[0])).thenComparingInt(c -> c[1]));
        else cs.sort(Comparator.<int[]>comparingInt(c -> c[1]).thenComparingInt(c -> c[0]));
        List<BlockPos> world = new ArrayList<>();
        for (int[] c : cs) world.add(f.world(c[0], c[1], c[2]));
        BlockPos mid = centre(f, gate);
        level.playSound(null, mid, gate == DOOR ? SoundEvents.RESPAWN_ANCHOR_DEPLETE.value() : SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3f, 0.4f);
        level.playSound(null, mid, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5f, 0.3f);
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(mid) < 48 * 48) {
                p.displayClientMessage(Component.literal(gate == DOOR ? "The great door of the Lion-Camel Cave grinds open."
                        : "Below, the way down from " + LionFortress.THEMES[gate] + " grinds open."), true);
            }
        }
        if (gate != DOOR) checkpoint(level, f, gate);
        synchronized (OPENING) {
            OPENING.add(new Opening(level, world, gate == DOOR ? LionFortress.DOOR_H * 2 : LionFortress.stair(gate).u1() - LionFortress.stair(gate).u0() + 1, new int[]{0}));
        }
    }

    /** Where one rises again who has won floor {@code k}'s way down: on its landing, at the head of the stair (local u, ly, lz). */
    public static int[] checkpointOf(int k) {
        LionFortress.Stair s = LionFortress.stair(k);
        return new int[]{(s.u0() + s.u1()) / 2, LionFortress.floorY(k) + 1, LionFortress.FZ + s.landing()};
    }

    /**
     * The checkpoints: as floor {@code k}'s way down opens, every player in its hall will rise again,
     * should he fall, on its landing at the head of the stair down, and not at the bottom of the
     * mountain; so one who dies deep walks on down from the last floor he won. Returns who was told.
     */
    public static List<ServerPlayer> checkpoint(ServerLevel level, Fortress f, int k) {
        return checkpoint(level, f, k, level.players());
    }

    /** The same, among {@code players}. */
    public static List<ServerPlayer> checkpoint(ServerLevel level, Fortress f, int k, java.util.Collection<ServerPlayer> players) {
        int[] at = checkpointOf(k);
        BlockPos spot = f.world(at[0], at[1], at[2]);
        int base = f.floor() + LionFortress.floorY(k);
        List<ServerPlayer> told = new ArrayList<>();
        for (ServerPlayer p : players) {
            int u = p.getBlockX() - f.cx(), v = p.getBlockZ() - f.cz() - LionFortress.FZ;
            if (Math.abs(u) > LionFortress.HALF || Math.abs(v) > LionFortress.HALF || p.getY() < base || p.getY() > base + LionFortress.airOf(k) + 1) continue;
            p.setRespawnPosition(level.dimension(), spot, p.getYRot(), true, false);
            p.displayClientMessage(Component.literal("Should you fall, you will rise again here, at the head of the stair down from "
                    + LionFortress.THEMES[k] + "."), false);
            told.add(p);
        }
        return told;
    }

    /** Opens it now, all at once (tests). */
    public static void openNow(ServerLevel level, Fortress f, int gate) {
        for (int[] c : cells(gate)) level.setBlock(f.world(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        synchronized (OPENING) {
            if (OPENING.isEmpty()) return;
            for (Iterator<Opening> it = OPENING.iterator(); it.hasNext(); ) {
                Opening o = it.next();
                if (o.level().getServer() != event.getServer()) continue;
                // a slice every two ticks: a column of the door, a row of the bars
                if (o.level().getGameTime() % 2 != 0) continue;
                int from = o.at()[0], to = Math.min(o.cells().size(), from + o.every());
                for (int i = from; i < to; i++) {
                    BlockPos p = o.cells().get(i);
                    if (!o.level().isLoaded(p)) continue;
                    o.level().setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    if (i % 3 == 0) o.level().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
                }
                if (from % (o.every() * 3) == 0 && to < o.cells().size()) {
                    o.level().playSound(null, o.cells().get(from), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.5f, 0.5f);
                }
                o.at()[0] = to;
                if (to >= o.cells().size()) it.remove();
            }
        }
    }

    // ── Hands on a gate; a warden's fall ───────────────────────────────

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.IRON_BARS) && !state.is(Blocks.STRIPPED_DARK_OAK_LOG) && !state.is(Blocks.GOLD_BLOCK)
                && !state.is(Blocks.POLISHED_BLACKSTONE) && !state.is(Blocks.CHISELED_POLISHED_BLACKSTONE) && !state.is(Blocks.GILDED_BLACKSTONE)) return;
        Fortress f = near(level, pos);
        if (f == null) return;
        Integer gate = gateAt(pos.getX() - f.cx(), pos.getY() - f.floor(), pos.getZ() - f.cz());
        if (gate == null) return;
        BlockState want = LionFortress.at(pos.getX() - f.cx(), pos.getY() - f.floor(), pos.getZ() - f.cz());
        if (want == null || !state.is(want.getBlock())) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        tryOpen(level, f, gate, event.getEntity());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** A warden falls: if none of its gate's are left, the gate opens of itself (the great door waits for a hand). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LionCamelMonster m) || !(m.level() instanceof ServerLevel level)) return;
        if (!m.getPersistentData().contains(LionCamelMonster.KEY_WARDEN)) return;
        int gate = m.getPersistentData().getInt(LionCamelMonster.KEY_WARDEN);
        Fortress f = fortressOf(m);
        if (f == null) return;
        if (gate == DOOR) {
            long left = wardens(level, f, gate).stream().filter(w -> w != m).count();
            BlockPos mid = centre(f, gate);
            for (ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(mid) < 64 * 64) {
                    p.displayClientMessage(Component.literal(left == 0 ? "The last warden of the great door falls. It will open to your hand."
                            : left + " of the great door's wardens still stand."), true);
                }
            }
            return;
        }
        if (!isShut(level, f, gate)) return;
        if (wardens(level, f, gate).stream().anyMatch(w -> w != m)) return;
        open(level, f, gate);
    }

    // ── The fortress stands ─────────────────────────────────────────────

    /** Whether the block at {@code pos} is of the fortress's fabric (walls, floors, stairs, gates), as the blueprint laid it. */
    public static boolean isFabric(ServerLevel level, BlockPos pos, BlockState state) {
        Fortress f = near(level, pos);
        if (f == null) return false;
        int lx = pos.getX() - f.cx(), ly = pos.getY() - f.floor(), lz = pos.getZ() - f.cz();
        int kind = LionFortress.kind(LionFortress.cell(lx, ly, lz));
        if (kind != LionFortress.ROCK && kind != LionFortress.WALL && kind != LionFortress.FLOOR && kind != LionFortress.STEP
                && kind != LionFortress.GATE && kind != LionFortress.DOOR) return false;
        BlockState want = LionFortress.at(lx, ly, lz);
        return want != null && state.is(want.getBlock());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer().isCreative()) return;
        if (isFabric(level, event.getPos(), event.getState())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("The demon kings' sorcery holds the Lion-Camel Cave together: it will not break."), true);
        }
    }

    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // the Fighting Buddha's blast: no sorcery holds against it
        if (com.jttw.engine.character.wukong.BuddhaBreaking.unstoppable(event.getExplosion().getIndirectSourceEntity())) return;
        event.getAffectedBlocks().removeIf(p -> isFabric(level, p, level.getBlockState(p)));
    }

    // ── Its halls ───────────────────────────────────────────────────────

    /**
     * The heights of the hall of the fortress {@code pos} is in, {min, max} in the world (its floor
     * to its roof); null if it is not in one.
     */
    @Nullable
    public static int[] hallBand(ServerLevel level, BlockPos pos) {
        Fortress f = near(level, pos);
        if (f == null) return null;
        int lx = pos.getX() - f.cx(), ly = pos.getY() - f.floor(), lz = pos.getZ() - f.cz();
        if (!LionFortress.covers(lx, lz) || ly > LionFortress.HALL_AIR || ly < LionFortress.BOTTOM) return null;
        int k = LionFortress.bandOf(ly);
        int base = LionFortress.floorY(k);
        if (ly - base > LionFortress.airOf(k)) return null; // in the rock between
        return new int[]{f.floor() + base, f.floor() + base + LionFortress.airOf(k)};
    }

    /** Whether {@code pos} is in a throne room of the Lion-Camel Cave. */
    public static boolean inThroneRoom(ServerLevel level, BlockPos pos) {
        Fortress f = near(level, pos);
        if (f == null) return false;
        int lx = pos.getX() - f.cx(), ly = pos.getY() - f.floor(), v = pos.getZ() - f.cz() - LionFortress.FZ;
        return Math.abs(lx) <= LionFortress.HALF && v >= LionFortress.THRONE_N && v <= LionFortress.THRONE_S
                && ly > LionFortress.THRONE_Y && ly <= LionFortress.THRONE_Y + LionFortress.THRONE_AIR;
    }
}
