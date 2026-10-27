package com.jttw.engine.world.cave;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.cave.CaveMonster;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.Labyrinth;
import com.jttw.engine.world.feature.PitHole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Bottomless Cave's rules, in the world as it runs:
 * <ul>
 *   <li><b>Pit-Hole Mountain is the mice's</b>: over the cave's region no other monster comes of
 *       itself, on the mountain or in the fortress; instead the household keeps the mountain
 *       crawling (packs and maids out of sight round whoever climbs it, some of them patrolling the
 *       road), and <b>breeds in the dark of each floor</b>: while someone walks a floor, a group of
 *       its strength now and then comes up out of the tunnels beyond sight, up to a few at a time.
 *       What is posted at the outposts, the gate, the halls and the ways down was put there when
 *       the land was made, and does not come back once killed;</li>
 *   <li><b>the great door</b> is barred from within while its guard stands: use it with any of the
 *       gate's guard alive near it and it will not move; when the last of them has fallen, it grinds
 *       open at a touch (its leaves are gone) and stays open;</li>
 *   <li><b>the Earth Gods' landings</b>: inside the door, on each floor where its stair comes in,
 *       before her palace, an Earth God keeps a niche; who steps up to one is raised there should
 *       they fall (their respawn is set there, no bed needed);</li>
 *   <li><b>the cave's rock will not yield</b>: none digs, blasts or phases through it (it says so to
 *       whoever tries), and none slips through it by chorus fruit.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class CaveRules {

    private CaveRules() {}

    /** How many of the household the mountain keeps round someone climbing it, and how often more come. */
    public static final int APPROACH_CAP = 16, APPROACH_EVERY = 60, APPROACH_REACH = 48;
    /** How many a floor keeps round someone walking it (more the deeper), and how often more come. */
    public static final int FLOOR_CAP = 5, FLOOR_EVERY = 300, FLOOR_REACH = 32;
    /** How near an Earth God's niche one must step to be raised there. */
    public static final double LANDING_NEAR = 3.5;

    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, Long> GRUMBLED = new HashMap<>();

    // ── Who comes of themselves ─────────────────────────────────────────

    /** Over the cave's region, no other monster comes of itself on the mountain, nor any in the fortress. */
    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL || event.getEntityType().getCategory() != MobCategory.MONSTER) return;
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (denied(level.getSeed(), event.getPos())) event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }

    /** Whether nothing comes of itself at {@code pos}: on the mountain over the region, or in the fortress. */
    public static boolean denied(long seed, BlockPos pos) {
        int[] c = BottomlessCave.ZONE.centre(seed, pos.getX(), pos.getZ());
        if (c == null) return false;
        double r = Math.hypot(pos.getX() - c[0], pos.getZ() - c[1]);
        if (r > BottomlessCave.ZONE.layout().margin()) return false;
        if (pos.getY() < Labyrinth.Y_MIN) return false;
        if (r <= BottomlessCave.FORTRESS_REACH) return true;
        return pos.getY() >= Zones.BOTTOMLESS_CAVE_TOP && BottomlessCave.ZONE.depth(seed, pos.getX(), pos.getZ()) > 0;
    }

    // ── The household, the landings ─────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        ServerLevel level = server.overworld();
        if (level == null) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            int[] c = BottomlessCave.ZONE.centre(level.getSeed(), p.getBlockX(), p.getBlockZ());
            if (c == null || Math.hypot(p.getX() - c[0], p.getZ() - c[1]) > BottomlessCave.ZONE.layout().margin() + 16) continue;
            Labyrinth lab = BottomlessCave.near(level, p.getBlockX(), p.getBlockZ());
            if (lab == null) continue;
            landing(p, lab);
            if (p.isCreative()) continue;
            int k = BottomlessCave.levelAt(lab, p.getX(), p.getY(), p.getZ());
            if (k >= 0 && k < Labyrinth.FLOORS) breed(level, lab, p, k);
            else if (!BottomlessCave.inFortress(lab, p.getX(), p.getY(), p.getZ()) && p.getY() >= Zones.BOTTOMLESS_CAVE_TOP) {
                approach(level, lab, p);
            }
        }
        patrols(level);
    }

    /** At an Earth God's niche, one is raised there should one fall. Returns the landing stepped up to, or null. */
    public static BottomlessCave.Landing landing(ServerPlayer p, Labyrinth lab) {
        for (BottomlessCave.Landing landing : BottomlessCave.landings(lab)) {
            BlockPos at = landing.at();
            if (Math.hypot(p.getX() - (at.getX() + 0.5), p.getZ() - (at.getZ() + 0.5)) > LANDING_NEAR || Math.abs(p.getY() - at.getY()) > 2) continue;
            if (at.equals(p.getRespawnPosition()) && Level.OVERWORLD.equals(p.getRespawnDimension())) return landing;
            p.setRespawnPosition(Level.OVERWORLD, at, p.getYRot(), true, false);
            String where = landing.level() < 0 ? "inside the great door" : landing.level() < Labyrinth.FLOORS
                    ? "on floor " + (landing.level() + 1) + ", " + Labyrinth.FLOOR_NAMES[landing.level()] : "before her palace";
            p.displayClientMessage(Component.literal("An Earth God keeps this way " + where + ": should you fall, he will raise you here."), true);
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 12, 0.4, 0.6, 0.4, 0.02);
            return landing;
        }
        return null;
    }

    /** On the mountain: packs and maids come up out of sight round whoever climbs it, while fewer than so many are near. */
    static void approach(ServerLevel level, Labyrinth lab, ServerPlayer p) {
        long now = level.getGameTime();
        if (now < NEXT.getOrDefault(p.getUUID(), 0L)) return;
        int near = level.getEntitiesOfClass(CaveMonster.class, p.getBoundingBox().inflate(APPROACH_REACH, 32, APPROACH_REACH)).size();
        if (near >= APPROACH_CAP) return;
        RandomSource r = level.random;
        for (int attempt = 0; attempt < 10; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, d = 22 + r.nextDouble() * 22;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (BottomlessCave.ZONE.depth(level.getSeed(), x, z) <= 0 || !level.isLoaded(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            if (y < Zones.BOTTOMLESS_CAVE_TOP || !level.getFluidState(at.below()).isEmpty()) continue;
            if (level.getNearestPlayer(x + 0.5, y, z + 0.5, 18, false) != null) continue;
            boolean mice = r.nextFloat() < 0.75f;
            var group = CaveHousehold.group(level, at, mice ? Labyrinth.MICE : Labyrinth.MAIDS, mice ? 3 + r.nextInt(3) : 1 + r.nextInt(2),
                    r.nextFloat() < 0.12f, -1, false, r);
            if (r.nextFloat() < 0.35f) {
                PitHole pit = BottomlessCave.mountain(level, lab);
                int best = nearestRoad(pit, x, z);
                for (CaveMonster m : group) {
                    m.getPersistentData().putInt(BottomlessCave.KEY_PATROL, best + 1);
                    m.getPersistentData().putInt(BottomlessCave.KEY_PATROL + "_way", r.nextBoolean() ? 1 : -1);
                }
            }
            NEXT.put(p.getUUID(), now + APPROACH_EVERY);
            return;
        }
    }

    private static int nearestRoad(PitHole pit, int x, int z) {
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int n = 0; n < pit.roadLength(); n += 3) {
            int[] q = pit.road(n);
            double d = Math.hypot(q[0] - x, q[2] - z);
            if (d < bd) {
                bd = d;
                best = n;
            }
        }
        return best;
    }

    /** Patrols walk the road, a stretch at a time, up toward the door or down, unless they have someone to fight. */
    static void patrols(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            for (CaveMonster m : level.getEntitiesOfClass(CaveMonster.class, p.getBoundingBox().inflate(64, 32, 64),
                    e -> e.getPersistentData().getInt(BottomlessCave.KEY_PATROL) > 0)) {
                if (m.getTarget() != null || !m.getNavigation().isDone()) continue;
                Labyrinth lab = BottomlessCave.near(level, m.getBlockX(), m.getBlockZ());
                if (lab == null) continue;
                PitHole pit = BottomlessCave.mountain(level, lab);
                int n = m.getPersistentData().getInt(BottomlessCave.KEY_PATROL) - 1, way = m.getPersistentData().getInt(BottomlessCave.KEY_PATROL + "_way");
                int next = n + way * 10;
                if (next < 0 || next >= pit.roadLength()) {
                    way = -way;
                    next = Math.max(0, Math.min(pit.roadLength() - 1, n + way * 10));
                    m.getPersistentData().putInt(BottomlessCave.KEY_PATROL + "_way", way);
                }
                int[] q = pit.road(next);
                int y = q[1] == Integer.MIN_VALUE ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, q[0], q[2]) : q[1] + 1;
                m.getNavigation().moveTo(q[0] + 0.5, y, q[2] + 0.5, 0.9);
                m.getPersistentData().putInt(BottomlessCave.KEY_PATROL, next + 1);
            }
        }
    }

    /**
     * On a floor: now and then a group of the floor's strength comes up out of the tunnels beyond
     * sight (14-32 blocks off, in a cell of that floor), while fewer than a few are near.
     */
    static void breed(ServerLevel level, Labyrinth lab, ServerPlayer p, int k) {
        long now = level.getGameTime();
        if (now < NEXT.getOrDefault(p.getUUID(), 0L)) return;
        int cap = FLOOR_CAP + k / 2;
        int near = level.getEntitiesOfClass(CaveMonster.class, p.getBoundingBox().inflate(FLOOR_REACH, 8, FLOOR_REACH)).size();
        if (near >= cap) return;
        int[] here = lab.cellAt(p.getBlockX(), p.getBlockZ());
        if (here == null) return;
        Labyrinth.Level l = lab.level(k);
        RandomSource r = level.random;
        for (int attempt = 0; attempt < 30; attempt++) {
            int i = here[0] - 4 + r.nextInt(9), j = here[1] - 4 + r.nextInt(9);
            boolean up = r.nextFloat() < 0.3f;
            if (up ? !l.high(i, j) : !l.low(i, j)) continue;
            int[] m = lab.cellMiddle(i, j);
            int y = Labyrinth.FLOOR[k] + (up ? Labyrinth.STOREY : 0);
            double d = Math.sqrt(p.distanceToSqr(m[0] + 0.5, y, m[1] + 0.5));
            if (d < 14 || d > FLOOR_REACH) continue;
            BlockPos at = new BlockPos(m[0], y, m[1]);
            if (!level.isLoaded(at) || !level.getBlockState(at).isAir() || level.getNearestPlayer(m[0] + 0.5, y, m[1] + 0.5, 12, false) != null) continue;
            boolean maids = r.nextFloat() < 0.2f + 0.03f * k;
            CaveHousehold.group(level, at, maids ? Labyrinth.MAIDS : Labyrinth.MICE, maids ? 1 : 2 + r.nextInt(2), r.nextFloat() < 0.04f + 0.03f * k, k, false, r);
            NEXT.put(p.getUUID(), now + FLOOR_EVERY - 15L * k);
            return;
        }
    }

    // ── The great door ──────────────────────────────────────────────────

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide || !level.getBlockState(event.getPos()).is(ModBlocks.BOTTOMLESS_GATE.get())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        knock((ServerLevel) level, event.getPos(), event.getEntity());
    }

    /** How many of the gate's guard still stand within reach of the door at {@code pos}. */
    public static int guardsStanding(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(CaveMonster.class, new AABB(pos).inflate(48),
                m -> m.isAlive() && m.getPersistentData().getBoolean(BottomlessCave.KEY_GATE)).size();
    }

    /**
     * Someone tries the great door at {@code pos}: barred while its guard stands (they are told how
     * many); else it opens. Returns how many of its blocks gave way (0 if it held).
     */
    public static int knock(ServerLevel level, BlockPos pos, Player who) {
        int guards = guardsStanding(level, pos);
        if (guards > 0) {
            who.displayClientMessage(Component.literal("The great door is barred from within. " + guards
                    + (guards == 1 ? " of its guard still stands" : " of its guard still stand") + " before it."), true);
            level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 0.8f, 0.6f);
            return 0;
        }
        int opened = open(level, pos);
        if (opened > 0) {
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 48 * 48) {
                    p.displayClientMessage(Component.literal("The great door of the Bottomless Cave grinds open."), true);
                }
            }
        }
        return opened;
    }

    /** Opens the great door whose leaf is at {@code pos}: every leaf joined to it gives way. Returns how many. */
    public static int open(ServerLevel level, BlockPos pos) {
        Block gate = ModBlocks.BOTTOMLESS_GATE.get();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(pos.immutable());
        seen.add(pos.immutable());
        int n = 0;
        while (!q.isEmpty() && n < 400) {
            BlockPos b = q.poll();
            if (!level.getBlockState(b).is(gate)) continue;
            level.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (n % 3 == 0) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, gate.defaultBlockState()), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5,
                        6, 0.3, 0.3, 0.3, 0.05);
            }
            n++;
            for (Direction d : Direction.values()) {
                BlockPos o = b.relative(d);
                if (seen.add(o)) q.add(o);
            }
        }
        if (n > 0) {
            level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 0.4f);
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 2f, 0.3f);
        }
        return n;
    }

    // ── The rock that will not yield ────────────────────────────────────

    @SubscribeEvent
    public static void onHit(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide || event.getEntity().isCreative()) return;
        if (!BottomlessCave.caveRock(event.getLevel().getBlockState(event.getPos()))) return;
        long now = event.getLevel().getGameTime();
        if (now < GRUMBLED.getOrDefault(event.getEntity().getUUID(), 0L)) return;
        GRUMBLED.put(event.getEntity().getUUID(), now + 60);
        boolean door = event.getLevel().getBlockState(event.getPos()).is(ModBlocks.BOTTOMLESS_GATE.get());
        event.getEntity().displayClientMessage(Component.literal(door ? "The great door will not be broken: it must be opened."
                : "The Bottomless Cave's rock will not yield. Its ways must be walked."), true);
    }

    /** No slipping through the cave's walls by chorus fruit. */
    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit event) {
        if (!(event.getEntityLiving().level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        Labyrinth lab = BottomlessCave.near(level, (int) event.getPrev().x, (int) event.getPrev().z);
        if (lab != null && BottomlessCave.inFortress(lab, event.getPrev().x, event.getPrev().y, event.getPrev().z)) event.setCanceled(true);
    }

    /** Forgets a player's timers (a test's, between runs). */
    public static void forget(UUID player) {
        NEXT.remove(player);
        GRUMBLED.remove(player);
    }

    /** The gate's guard standing near the door of this cave (for the bench and the tests). */
    public static List<CaveMonster> gateGuard(ServerLevel level, Labyrinth lab) {
        int[] d = lab.door();
        return level.getEntitiesOfClass(CaveMonster.class, new AABB(new BlockPos(d[0], d[1], d[2])).inflate(48),
                m -> m.getPersistentData().getBoolean(BottomlessCave.KEY_GATE));
    }
}
