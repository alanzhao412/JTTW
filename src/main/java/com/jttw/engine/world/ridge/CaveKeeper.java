package com.jttw.engine.world.ridge;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.RidgeMonster;
import com.jttw.engine.world.feature.WhiteBoneCaveFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the White Bone Cave while players are at it (each second, while one is near):
 * <ul>
 *   <li><b>The great door</b> stays shut while either of its two wardens, the Keepers of the Bone
 *       Gate in the forecourt, stands; once both are dead, it grinds open as a player comes up to it
 *       ({@value #DOOR_NEAR} blocks), its leaves folding back against the passage's walls, and stays
 *       open for good.</li>
 *   <li><b>The seals</b>: a floor's seal (the iron bars over its way down) breaks when the floor's
 *       guardian, the Warden of its Seal, dies (or is found gone), and stays broken. With it the
 *       grate in the well of the dead over the next floor's landing opens: a <b>checkpoint</b>, the
 *       ladder down from the first floor to the deepest one won, after a death.</li>
 *   <li><b>Restocking</b>: a floor no player has been on for {@value #RESTOCK_MINUTES} minutes is
 *       manned again when one comes back to it: every empty post refilled (the guardian too; the seal
 *       stays broken), but never within {@value #NOT_IN_SIGHT} blocks of a player.</li>
 *   <li><b>The leash</b>: one of a floor's garrison found off its floor with no one to fight is put
 *       back at its post; and none of them, fighting or not, is let into the throne room.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class CaveKeeper {

    private CaveKeeper() {}

    public static final int EVERY = 20, DOOR_NEAR = 12, RESTOCK_MINUTES = com.jttw.engine.world.ZonePosts.RESPAWN_MINUTES,
            RESTOCK_AFTER = 20 * 60 * RESTOCK_MINUTES, NOT_IN_SIGHT = 12;
    /** How near the fortress a player must be for the keeper to keep it (local, each way; to the south, the forecourt and beyond). */
    public static final int NEAR = 90;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        if (level.getGameTime() % EVERY != 0 || level.players().isEmpty()) return;
        if (!CaveSites.fortress(level.getSeed()) || CaveSites.pinned(level.getSeed())) return; // a cave laid by hand is kept by its tests
        WhiteBoneCave.Site site = CaveSites.site(level);
        if (site == null) return;
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            int lx = p.getBlockX() - site.cx(), lz = p.getBlockZ() - site.cz();
            if (Math.abs(lx) <= NEAR && lz >= -NEAR && lz <= NEAR + 40) near.add(p);
        }
        if (near.isEmpty()) return;
        tick(level, site, CaveState.of(level), near);
    }

    /** One look at the cave, with these players near it. */
    public static void tick(ServerLevel level, WhiteBoneCave.Site site, CaveState state, List<? extends Player> players) {
        WhiteBoneCave cave = WhiteBoneCave.of(level.getSeed());
        door(level, site, state, players);
        for (int k = 0; k < WhiteBoneCave.FLOORS; k++) {
            if (!state.sealBroken(k) && onFloor(site, players, k) && guardianGone(level, site, cave, k)) breakSeal(level, site, state, k);
            if (state.sealBroken(k)) openSeal(level, site, k);
        }
        long now = level.getGameTime();
        Set<Integer> seen = new HashSet<>();
        for (Player p : players) {
            int k = floorOf(site, p.blockPosition());
            if (k < 0 || !seen.add(k)) continue;
            long last = state.lastSeen(k);
            if (last != 0 && now - last > RESTOCK_AFTER) restock(level, site, cave, k, players);
            state.seen(k, now);
        }
        leash(level, site, cave, players);
    }

    /** The floor a world place is on (0 to 7, 8 the throne room and its approach), or -1 outside the fortress. */
    public static int floorOf(WhiteBoneCave.Site site, BlockPos at) {
        return WhiteBoneCave.floorOf(at.getX() - site.cx(), at.getY() - site.y0(), at.getZ() - site.cz());
    }

    private static boolean onFloor(WhiteBoneCave.Site site, List<? extends Player> players, int k) {
        for (Player p : players) if (floorOf(site, p.blockPosition()) == k) return true;
        return false;
    }

    // ── The door ────────────────────────────────────────────────────────

    /** The door's wardens still standing (in the forecourt, or anywhere near it). */
    public static int wardens(ServerLevel level, WhiteBoneCave.Site site) {
        AABB court = new AABB(site.at(-40, -20, 20)).minmax(new AABB(site.at(40, 40, 120)));
        return level.getEntitiesOfClass(RidgeMonster.class, court, m -> m.isAlive() && CaveGarrison.of(m)
                && CaveGarrison.rank(m) == WhiteBoneCave.WARDEN).size();
    }

    private static void door(ServerLevel level, WhiteBoneCave.Site site, CaveState state, List<? extends Player> players) {
        BlockPos door = site.door();
        if (!level.isLoaded(door)) return;
        if (state.doorOpen()) {
            if (!level.getBlockState(site.at(0, 0, WhiteBoneCave.DOOR_Z)).isAir()) openDoor(level, site); // as it was left
            return;
        }
        boolean near = false;
        for (Player p : players) near |= p.blockPosition().distSqr(door) <= DOOR_NEAR * DOOR_NEAR;
        if (!near) return;
        for (int s = -1; s <= 1; s += 2) {
            if (!level.areEntitiesLoaded(ChunkPos.asLong(site.at(s * 9, 0, 67)))) return; // the wardens' ground not yet awake
        }
        if (wardens(level, site) > 0) return;
        openDoor(level, site);
        state.openDoor();
        for (Player p : players) {
            if (p.blockPosition().distSqr(door) <= 48 * 48) {
                p.sendSystemMessage(Component.literal("§7With its keepers fallen, the bone gate of the White Bone Cave grinds open."));
            }
        }
    }

    /** Opens the great door: its leaves gone from the doorway, folded back against the passage's walls. */
    public static void openDoor(ServerLevel level, WhiteBoneCave.Site site) {
        for (int x = -WhiteBoneCave.DOOR_HALF; x <= WhiteBoneCave.DOOR_HALF; x++) {
            for (int y = 0; y < WhiteBoneCave.DOOR_H; y++) {
                level.setBlock(site.at(x, y, WhiteBoneCave.DOOR_Z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (int s = -1; s <= 1; s += 2) {
            for (int z = WhiteBoneCave.DOOR_Z - 6; z < WhiteBoneCave.DOOR_Z; z++) {
                for (int y = 0; y < WhiteBoneCave.DOOR_H; y++) {
                    level.setBlock(site.at(s * (WhiteBoneCave.DOOR_HALF + 1), y, z), WhiteBoneCaveFeature.leaf(s * (z - (WhiteBoneCave.DOOR_Z - 7)), y), Block.UPDATE_ALL);
                }
            }
        }
        BlockPos door = site.door();
        level.playSound(null, door, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.BLOCKS, 3f, 0.5f);
        level.playSound(null, door, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3f, 0.4f);
        level.sendParticles(ParticleTypes.WHITE_ASH, door.getX() + 0.5, door.getY() + 8, door.getZ() + 0.5, 300, 6, 6, 1, 0.02);
    }

    /** Whether the great door stands shut (its leaves in the doorway). */
    public static boolean doorShut(ServerLevel level, WhiteBoneCave.Site site) {
        return !level.getBlockState(site.at(0, 1, WhiteBoneCave.DOOR_Z)).isAir();
    }

    // ── The seals ───────────────────────────────────────────────────────

    /** Whether floor k's guardian is gone: its post's ground awake and no guardian of it about. */
    public static boolean guardianGone(ServerLevel level, WhiteBoneCave.Site site, WhiteBoneCave cave, int k) {
        BlockPos post = CaveGarrison.where(level, site, cave.posts(k).get(0));
        if (!level.isLoaded(post) || !level.areEntitiesLoaded(ChunkPos.asLong(post))) return false;
        return level.getEntitiesOfClass(RidgeMonster.class, floorBox(site, k), m -> m.isAlive() && CaveGarrison.of(m)
                && CaveGarrison.floor(m) == k && CaveGarrison.rank(m) == WhiteBoneCave.GUARDIAN).isEmpty();
    }

    /** Breaks floor k's seal for good, opens its bars and the well's grate under it, and says so to those near. */
    public static void breakSeal(ServerLevel level, WhiteBoneCave.Site site, CaveState state, int k) {
        state.breakSeal(k);
        openSeal(level, site, k);
        BlockPos at = site.at(-12, WhiteBoneCave.floorY(k) + 2, WhiteBoneCave.side(k) * WhiteBoneCave.MOUTH_Z);
        level.playSound(null, at, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 2f, 0.6f);
        level.playSound(null, at, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 1f, 0.8f);
        String where = k + 1 < WhiteBoneCave.FLOORS ? WhiteBoneCave.NAMES[k + 1] : "her throne room";
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(at) <= 64 * 64) {
                p.sendSystemMessage(Component.literal("§7The seal of " + WhiteBoneCave.NAMES[k] + " breaks: the way down to " + where + " lies open."));
            }
        }
    }

    /** Opens floor k's seal (its bars gone) and the well's grate over floor k + 1's landing, where they are loaded and still shut. */
    public static void openSeal(ServerLevel level, WhiteBoneCave.Site site, int k) {
        int f = WhiteBoneCave.floorY(k), z = WhiteBoneCave.side(k) * WhiteBoneCave.MOUTH_Z;
        BlockPos probe = site.at(WhiteBoneCave.SEAL_X0, f, z);
        if (level.isLoaded(probe) && level.getBlockState(probe).is(Blocks.IRON_BARS)) {
            for (int x = WhiteBoneCave.SEAL_X0; x <= WhiteBoneCave.SEAL_X1; x++) {
                for (int y = f; y < f + WhiteBoneCave.SEAL_H; y++) level.setBlock(site.at(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (k + 1 < WhiteBoneCave.FLOORS) {
            BlockPos grate = site.at(WhiteBoneCave.SHAFT_X, WhiteBoneCave.grateY(k + 1), WhiteBoneCave.SHAFT_Z);
            if (level.isLoaded(grate) && level.getBlockState(grate).is(Blocks.IRON_BARS)) {
                level.setBlock(grate, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            }
        }
    }

    /** Whether floor k's seal's bars still stand. */
    public static boolean sealShut(ServerLevel level, WhiteBoneCave.Site site, int k) {
        return level.getBlockState(site.at(WhiteBoneCave.SEAL_X0 + 2, WhiteBoneCave.floorY(k) + 1, WhiteBoneCave.side(k) * WhiteBoneCave.MOUTH_Z)).is(Blocks.IRON_BARS);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof RidgeMonster m) || !(m.level() instanceof ServerLevel level) || !CaveGarrison.of(m)) return;
        if (CaveGarrison.rank(m) != WhiteBoneCave.GUARDIAN || !level.dimension().equals(Level.OVERWORLD)) return;
        WhiteBoneCave.Site site = CaveSites.site(level);
        if (site == null) return;
        CaveState state = CaveState.of(level);
        int k = CaveGarrison.floor(m);
        if (k >= 0 && k < WhiteBoneCave.FLOORS && !state.sealBroken(k)) breakSeal(level, site, state, k);
    }

    // ── The garrison ────────────────────────────────────────────────────

    /** Floor k's bounds (world): its hall and the rock round it, its height and the pits under it (8: the throne room and its approach). */
    public static AABB floorBox(WhiteBoneCave.Site site, int k) {
        int r = WhiteBoneCave.REACH;
        if (k >= WhiteBoneCave.FLOORS) {
            return new AABB(site.at(-r, WhiteBoneCave.THRONE_Y - 2, -r)).minmax(new AABB(site.at(r + 1, WhiteBoneCave.floorY(7) - 8, r + 1)));
        }
        int f = WhiteBoneCave.floorY(k);
        return new AABB(site.at(-r, f - 9, -r)).minmax(new AABB(site.at(r + 1, f + 17, r + 1)));
    }

    /** Mans floor k's empty posts, out of the players' sight. Returns how many were filled. */
    public static int restock(ServerLevel level, WhiteBoneCave.Site site, WhiteBoneCave cave, int k, List<? extends Player> players) {
        List<WhiteBoneCave.Post> posts = cave.posts(k);
        Set<Integer> manned = new HashSet<>();
        for (RidgeMonster m : level.getEntitiesOfClass(RidgeMonster.class, floorBox(site, k), m -> m.isAlive() && CaveGarrison.of(m) && CaveGarrison.floor(m) == k)) {
            manned.add(CaveGarrison.post(m));
        }
        int filled = 0;
        for (int i = 0; i < posts.size(); i++) {
            if (manned.contains(i)) continue;
            BlockPos at = CaveGarrison.where(level, site, posts.get(i));
            if (!level.isLoaded(at) || !level.areEntitiesLoaded(ChunkPos.asLong(at))) continue; // its keeper may be there, not yet loaded
            boolean seen = false;
            for (Player p : players) seen |= p.blockPosition().distSqr(at) < NOT_IN_SIGHT * NOT_IN_SIGHT;
            if (seen) continue;
            if (CaveGarrison.spawn(level, posts.get(i), i, at, level.random, MobSpawnType.STRUCTURE) != null) filled++;
        }
        return filled;
    }

    /** Puts back at its post any of the garrison near the players off its floor with no one to fight, or in the throne room unbidden. Returns how many. */
    public static int leash(ServerLevel level, WhiteBoneCave.Site site, WhiteBoneCave cave, List<? extends Player> players) {
        int sent = 0;
        Set<RidgeMonster> seen = new HashSet<>();
        for (Player p : players) {
            for (RidgeMonster m : level.getEntitiesOfClass(RidgeMonster.class, p.getBoundingBox().inflate(80), CaveGarrison::of)) {
                if (!seen.add(m) || !m.isAlive()) continue;
                int f = CaveGarrison.floor(m);
                if (f < 0 || f > WhiteBoneCave.FLOORS) continue;
                int on = floorOf(site, m.blockPosition());
                if (on == f) continue;
                boolean intruder = on == WhiteBoneCave.FLOORS || on < 0;
                if (!intruder && m.getTarget() != null && m.getTarget().isAlive()) continue;
                List<WhiteBoneCave.Post> posts = cave.posts(f);
                int i = CaveGarrison.post(m);
                if (i < 0 || i >= posts.size()) continue;
                BlockPos home = CaveGarrison.where(level, site, posts.get(i));
                m.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                m.getNavigation().stop();
                m.setTarget(null);
                sent++;
            }
        }
        return sent;
    }
}
