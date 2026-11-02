package com.jttw.engine.world.fire;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.FlamingRangeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Fire Cloud Cave as the living find it: its door, its seals, its garrisons, its landings.
 * <ul>
 *   <li><b>The great door</b> is barred from within while its two wardens live (Furnace-Brick
 *       Brutes of a captain's strength, before it in the forecourt). Once they are dead, knock on
 *       it (use it) and it grinds open from the middle outward, and stays open
 *       ({@link #knock}).</li>
 *   <li><b>The garrisons</b> ({@link #muster}): each floor's is mustered when someone comes near it
 *       (onto the floor above, or to the door for the first), each kept to its floor
 *       ({@link #leash}: one that strays off it, up or down a stair, is taken back to its post), and
 *       none of the world's own creatures are born inside the cave. A floor whose garrison is all
 *       dead stays empty for {@value #RESPAWN_MINUTES} minutes; after that it is mustered again the
 *       next time someone comes near while no one is on it.</li>
 *   <li><b>The seals</b>: each floor's way down is shut by an iron grate its guardian holds (one of
 *       Red Boy's Six Strong Generals on the first six floors, a warden on the last two); when he
 *       dies the grate falls open, and stays open ({@link #openSeal}).</li>
 *   <li><b>The landings</b>: coming down onto a floor's landing marks it; the return brazier by
 *       the door then brings one back down to the deepest landing reached, so a death need not
 *       mean the whole descent again ({@link #returnDown}).</li>
 *   <li><b>Red Boy</b> keeps to his throne room: should anything bring him off it he is taken
 *       back to his dais.</li>
 *   <li><b>A world made before</b>: where the region's centre was made with the old cave (its hall
 *       at the gorge's level), the old cave is left as it is, Red Boy in it, and none of this
 *       applies there; where the centre is new but some of the fortress's ground was made before,
 *       the fortress is finished over it as those chunks come to be loaded with someone near ({@link #complete}), so
 *       the way down is whole and Red Boy is never shut away.</li>
 * </ul>
 * Everything here is worked out from the fortress's origin (the gorge floor's level at the region's
 * centre), so the tests' fortresses work as the world's do.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FireFortress {

    private FireFortress() {}

    /** How long a floor cleared of its garrison stays empty. */
    public static final int RESPAWN_MINUTES = com.jttw.engine.world.ZonePosts.RESPAWN_MINUTES; // every zone's posts alike
    public static final long RESPAWN = RESPAWN_MINUTES * 60 * 20L;
    /** How often the fortress is looked after, in ticks. */
    public static final int EVERY = 20;
    /** How near the region's centre someone must be for its fortress to be looked after. */
    public static final int NEAR = 160;
    /** How far the door's wardens are looked for round the door. */
    public static final int WARDENS_REACH = 64;

    /** The persistent-data key a garrison's member carries: its floor, its fortress, and whether it holds a seal (or the door). */
    public static final String KEY = JTTWEngine.MOD_ID + ":fire_post";
    /** The persistent-data key a player carries: the deepest landing reached, and in which fortress. */
    public static final String KEY_DEPTH = JTTWEngine.MOD_ID + ":fire_depth";

    // ── Where ───────────────────────────────────────────────────────────

    /** The fortress's origin in this world: the region's centre, at the gorge floor's level (its local (0, 0, 0)). */
    @Nullable
    public static BlockPos origin(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) return null;
        int[][] cs = Zones.FIRE_MOUNTAIN.centres(level.getSeed());
        if (cs.length == 0) return null;
        return new BlockPos(cs[0][0], FlamingRangeFeature.hallFloor(level, cs[0][0], cs[0][1]), cs[0][1]);
    }

    /** Where the Earth Gods' luopan points for the Flaming Mountains: the great door of the region centred at {@code c}. */
    public static BlockPos doorTarget(int[] c) {
        return new BlockPos(c[0], 64, c[1] + FortressPlan.FACE_Z + 4);
    }

    /** The world position of local (x, y, z). */
    public static BlockPos at(BlockPos origin, int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    /** The whole fortress's bounds, the forecourt with it. */
    public static AABB bounds(BlockPos origin) {
        return new AABB(origin.getX() + FortressPlan.X_MIN, origin.getY() + FortressPlan.Y_MIN, origin.getZ() + FortressPlan.Z_MIN,
                origin.getX() + FortressPlan.X_MAX + 1, origin.getY() + FortressPlan.Y_MAX + 1, origin.getZ() + FortressPlan.Z_MAX + 40);
    }

    /** Whether (x, y, z) is inside the fortress's rock (where nothing is born of itself), under its door's level and a little over. */
    public static boolean inside(BlockPos origin, BlockPos p) {
        int x = p.getX() - origin.getX(), y = p.getY() - origin.getY(), z = p.getZ() - origin.getZ();
        return Math.abs(x) <= FortressPlan.SHELL && z >= -FortressPlan.SHELL && z <= FortressPlan.DOOR[5] && y >= FortressPlan.Y_MIN && y <= 20;
    }

    // ── What it remembers ───────────────────────────────────────────────

    /** Per fortress: whether it is of this make or the old one, its door, each floor's muster and when it was cleared. */
    public static final class State extends SavedData {

        private static final String NAME = JTTWEngine.MOD_ID + "_fire_fortress";
        public static final int UNKNOWN = 0, NEW = 1, OLD = 2;

        /** By the origin's long. */
        private final Map<Long, CompoundTag> forts = new HashMap<>();

        public static State of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            for (String k : tag.getAllKeys()) s.forts.put(Long.parseLong(k), tag.getCompound(k));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            forts.forEach((k, v) -> tag.put(Long.toString(k), v));
            return tag;
        }

        CompoundTag of(BlockPos origin) {
            return forts.computeIfAbsent(origin.asLong(), k -> new CompoundTag());
        }

        public int status(BlockPos origin) {
            return of(origin).getInt("Status");
        }

        public void setStatus(BlockPos origin, int status) {
            of(origin).putInt("Status", status);
            setDirty();
        }

        public boolean mustered(BlockPos origin, int floor) {
            return (of(origin).getInt("Mustered") & (1 << floor)) != 0;
        }

        public long clearedAt(BlockPos origin, int floor) {
            return of(origin).getLong("Cleared" + floor);
        }

        void mustered(BlockPos origin, int floor, long now) {
            CompoundTag t = of(origin);
            t.putInt("Mustered", t.getInt("Mustered") | (1 << floor));
            t.putLong("Cleared" + floor, 0);
            setDirty();
        }

        void cleared(BlockPos origin, int floor, long now) {
            of(origin).putLong("Cleared" + floor, now);
            setDirty();
        }

        public boolean completed(BlockPos origin) {
            return of(origin).getBoolean("Completed");
        }

        void complete(BlockPos origin) {
            of(origin).putBoolean("Completed", true);
            setDirty();
        }

        /** Which of the fortress's chunks have been looked over (a bit each, see {@link FireFortress#complete}). */
        long checked(BlockPos origin) {
            return of(origin).getLong("Checked");
        }

        void checked(BlockPos origin, long bits) {
            of(origin).putLong("Checked", bits);
            setDirty();
        }

        /** Forgets that the fortress's chunks were looked over, so they are again (the tests'). */
        public void recheck(BlockPos origin) {
            of(origin).putLong("Checked", 0);
            of(origin).putBoolean("Completed", false);
            setDirty();
        }
    }

    /** Takes this fortress for one of the new make, laid whole, nothing mustered in it yet (the tests' and the gallery's fortresses). */
    public static void markNew(ServerLevel level, BlockPos origin) {
        State s = State.of(level);
        s.forts.remove(origin.asLong());
        s.setStatus(origin, State.NEW);
        s.complete(origin);
    }

    // ── The door ────────────────────────────────────────────────────────

    /** Whether the door is open: its middle gone. */
    public static boolean doorOpen(ServerLevel level, BlockPos origin) {
        return !FortressPlan.door(code(level, origin, at(origin, 0, 7, FortressPlan.DOOR[5])));
    }

    /** The plan's code where world {@code p} is, if what stands there is still the plan's; else air. */
    private static byte code(ServerLevel level, BlockPos origin, BlockPos p) {
        FortressPlan.Built plan = FortressPlan.get();
        int x = p.getX() - origin.getX(), y = p.getY() - origin.getY(), z = p.getZ() - origin.getZ();
        BlockState want = FortressBlocks.state(plan, x, y, z, p.getX(), p.getY(), p.getZ());
        if (want == null || !level.getBlockState(p).is(want.getBlock())) return FortressPlan.AIR;
        return plan.code(x, y, z);
    }

    /** The door's wardens still standing, near it. */
    public static List<FireMonster> wardens(ServerLevel level, BlockPos origin) {
        BlockPos door = at(origin, 0, 1, FortressPlan.FACE_Z);
        return level.getEntitiesOfClass(FireMonster.class, new AABB(door).inflate(WARDENS_REACH),
                m -> m.isAlive() && floorOf(m) == 0 && guards(m) && origin.equals(originOf(m)));
    }

    /** Doors opening, a step a few ticks. */
    private static final List<Opening> OPENING = new ArrayList<>();

    private record Opening(ServerLevel level, BlockPos origin, int[] step) {}

    /**
     * Someone knocks on the door (uses it). Barred while its wardens live (they turn on the
     * knocker, and he is told so); else it grinds open from the middle out over a couple of
     * seconds, and stays open. A player in creative opens it regardless. Returns whether it opens.
     */
    public static boolean knock(ServerLevel level, BlockPos origin, @Nullable Player who) {
        if (doorOpen(level, origin) || opening(origin)) return false;
        boolean free = who != null && who.isCreative();
        List<FireMonster> wardens = wardens(level, origin);
        if (!wardens.isEmpty() && !free) {
            level.playSound(null, at(origin, 0, 7, FortressPlan.DOOR[5]), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.BLOCKS, 2f, 0.5f);
            if (who != null) {
                who.displayClientMessage(Component.literal("§cThe great door is barred from within. Its two wardens stand before it: "
                        + "strike them down, and knock again."), true);
                for (FireMonster w : wardens) w.setTarget(who);
            }
            return false;
        }
        OPENING.add(new Opening(level, origin, new int[]{0}));
        level.playSound(null, at(origin, 0, 7, FortressPlan.DOOR[5]), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3f, 0.4f);
        if (who != null) who.displayClientMessage(Component.literal("§6The great door of the Fire Cloud Cave grinds open."), true);
        return true;
    }

    private static boolean opening(BlockPos origin) {
        for (Opening o : OPENING) if (o.origin().equals(origin)) return true;
        return false;
    }

    /** One step of each opening door: its leaves drawn back a column either side from the middle. */
    private static void openDoors() {
        for (Iterator<Opening> it = OPENING.iterator(); it.hasNext(); ) {
            Opening o = it.next();
            int s = o.step()[0]++;
            if (s % 3 != 0) continue;
            int reach = s / 3;
            int[] d = FortressPlan.DOOR;
            for (int x = d[0]; x <= d[3]; x++) {
                if (Math.abs(x) != reach) continue;
                for (int y = d[1]; y <= d[4]; y++) for (int z = d[2]; z <= d[5]; z++) {
                    BlockPos p = at(o.origin(), x, y, z);
                    if (FortressPlan.door(FortressPlan.code(x, y, z))) o.level().setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                BlockPos mid = at(o.origin(), x, 7, d[5]);
                o.level().sendParticles(ParticleTypes.LARGE_SMOKE, mid.getX() + 0.5, mid.getY(), mid.getZ() + 1, 12, 0.3, 4, 0.3, 0.02);
                o.level().playSound(null, mid, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.4f);
            }
            if (reach >= Math.max(-d[0], d[3])) it.remove();
        }
    }

    // ── The seals ───────────────────────────────────────────────────────

    /** Opens the grate over floor {@code n}'s way down. Returns how many bars it took away. */
    public static int openSeal(ServerLevel level, BlockPos origin, int n) {
        int[] b = FortressPlan.get().floor(n).seal();
        if (b == null) return 0;
        int gone = 0;
        for (int x = b[0]; x <= b[3]; x++) for (int y = b[1]; y <= b[4]; y++) for (int z = b[2]; z <= b[5]; z++) {
            BlockPos p = at(origin, x, y, z);
            if (!level.getBlockState(p).is(Blocks.IRON_BARS)) continue;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.LAVA, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0);
            gone++;
        }
        if (gone > 0) {
            BlockPos mid = at(origin, (b[0] + b[3]) / 2, b[1] + 1, (b[2] + b[5]) / 2);
            level.playSound(null, mid, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 2f, 0.5f);
            level.playSound(null, mid, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 0.6f);
            String below = n >= FortressPlan.FLOORS ? "the throne room of the Holy Child King" : FortressPlan.get().floor(n + 1).name();
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(Vec3.atCenterOf(mid)) < 64 * 64) {
                    p.displayClientMessage(Component.literal("§6The grate falls open: the way down to " + below + " lies before you."), true);
                }
            }
        }
        return gone;
    }

    /** Whether floor {@code n}'s grate still stands. */
    public static boolean sealed(ServerLevel level, BlockPos origin, int n) {
        int[] b = FortressPlan.get().floor(n).seal();
        return b != null && level.getBlockState(at(origin, b[0], b[1], b[2])).is(Blocks.IRON_BARS);
    }

    /** A guardian's death opens his floor's seal; the wardens' only unbar the door (it is knocked open). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob m) || !(m.level() instanceof ServerLevel level) || !guards(m)) return;
        int n = floorOf(m);
        BlockPos origin = originOf(m);
        if (origin != null && n >= 1 && n <= FortressPlan.FLOORS) openSeal(level, origin, n);
    }

    // ── The garrisons ───────────────────────────────────────────────────

    private static CompoundTag post(Entity e) {
        return e.getPersistentData().getCompound(KEY);
    }

    /** The floor a garrison's member keeps to (0 the forecourt); -1 if it is none of the cave's. */
    public static int floorOf(Entity e) {
        CompoundTag t = post(e);
        return t.contains("Floor") ? t.getInt("Floor") : -1;
    }

    /** Whether it holds its floor's seal (or, in the forecourt, the door). */
    public static boolean guards(Entity e) {
        return post(e).getBoolean("Guard");
    }

    @Nullable
    public static BlockPos originOf(Entity e) {
        CompoundTag t = post(e);
        return t.contains("Ox") ? new BlockPos(t.getInt("Ox"), t.getInt("Oy"), t.getInt("Oz")) : null;
    }

    /** How far from its post a member of a garrison wanders: a guardian stays by his seal. */
    public static final int ROAM = 30, GUARD_ROAM = 6, WARDEN_ROAM = 10;

    /** Puts one of a garrison at its spot. */
    @Nullable
    public static Mob spawn(ServerLevel level, BlockPos origin, int floor, FortressPlan.Spot s, boolean guard) {
        EntityType<? extends FireMonster> type = switch (s.kind()) {
            case FortressPlan.BRUTE, FortressPlan.BRUTE_CAPTAIN, FortressPlan.WARDEN -> ModEntities.FURNACE_BRUTE.get();
            case FortressPlan.GENERAL -> ModEntities.STRONG_GENERAL.get();
            default -> ModEntities.EMBER_IMP.get();
        };
        FireMonster m = type.create(level);
        if (m == null) return null;
        BlockPos p = at(origin, s.x(), s.y(), s.z());
        var shape = level.getBlockState(p).getCollisionShape(level, p);
        double over = shape.isEmpty() ? 0 : shape.max(net.minecraft.core.Direction.Axis.Y); // on a carpet, not in it
        m.moveTo(p.getX() + 0.5, p.getY() + over, p.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        if (m instanceof StrongGeneralEntity g) g.setVariant(s.variant());
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        boolean captain = s.kind() == FortressPlan.IMP_CAPTAIN || s.kind() == FortressPlan.BRUTE_CAPTAIN || s.kind() == FortressPlan.GENERAL
                || s.kind() == FortressPlan.WARDEN;
        int lvl = s.kind() == FortressPlan.GENERAL || s.kind() == FortressPlan.WARDEN ? FortressPlan.guardianLevel(Math.max(floor, 1))
                : captain ? FortressPlan.captainLevel(floor) : FortressPlan.regularLevel(floor);
        if (s.kind() == FortressPlan.WARDEN) m.becomeMiniboss(lvl, 1); // a gate's warden, a floor's guardian: minibosses (as the generals always are)
        else m.become(lvl, captain);
        m.setPersistenceRequired();
        m.restrictTo(p, guard ? (floor == 0 ? WARDEN_ROAM : GUARD_ROAM) : ROAM);
        CompoundTag t = new CompoundTag();
        t.putInt("Floor", floor);
        t.putBoolean("Guard", guard);
        t.putInt("Ox", origin.getX());
        t.putInt("Oy", origin.getY());
        t.putInt("Oz", origin.getZ());
        m.getPersistentData().put(KEY, t);
        level.addFreshEntity(m);
        return m;
    }

    /** Musters floor {@code n}'s garrison (0 the forecourt, its two wardens and their imps). Returns them. */
    public static List<Mob> muster(ServerLevel level, BlockPos origin, int n) {
        FortressPlan.Floor f = FortressPlan.get().floor(n);
        List<Mob> out = new ArrayList<>();
        for (FortressPlan.Spot s : f.garrison()) {
            Mob m = spawn(level, origin, n, s, false);
            if (m != null) out.add(m);
        }
        if (n == 0) {
            for (int[] post : FortressPlan.WARDEN_POSTS) {
                Mob m = spawn(level, origin, 0, new FortressPlan.Spot(post[0], 1, post[1], FortressPlan.WARDEN, 0), true);
                if (m != null) out.add(m);
            }
        } else if (f.guardian() != null) {
            Mob m = spawn(level, origin, n, f.guardian(), true);
            if (m != null) out.add(m);
        }
        State.of(level).mustered(origin, n, level.getGameTime());
        return out;
    }

    /** The living members of floor {@code n}'s garrison. */
    public static List<Mob> garrison(ServerLevel level, BlockPos origin, int n) {
        return level.getEntitiesOfClass(Mob.class, bounds(origin), m -> m.isAlive() && floorOf(m) == n && origin.equals(originOf(m)));
    }

    /** Whether every chunk of the fortress has its creatures loaded (so a count of them is a true one). */
    private static boolean loaded(ServerLevel level, BlockPos origin) {
        for (int cx = (origin.getX() + FortressPlan.X_MIN) >> 4; cx <= (origin.getX() + FortressPlan.X_MAX) >> 4; cx++) {
            for (int cz = (origin.getZ() + FortressPlan.Z_MIN) >> 4; cz <= (origin.getZ() + FortressPlan.Z_MAX + 24) >> 4; cz++) {
                if (!level.areEntitiesLoaded(ChunkPos.asLong(cx, cz))) return false;
            }
        }
        return true;
    }

    /** The floor a player is on (0 near the door outside), or -1. */
    public static int floorOf(BlockPos origin, Entity e) {
        BlockPos p = e.blockPosition();
        return FortressPlan.get().floorAt(p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ());
    }

    /**
     * Musters the floors someone is near, and marks the cleared ones: the forecourt when anyone is
     * near the region's heart, the first floor when someone is at the door, each floor below when
     * someone is on the floor over it (or on it, the first time).
     */
    public static void garrisons(ServerLevel level, BlockPos origin, List<ServerPlayer> near) {
        if (!loaded(level, origin)) return;
        State state = State.of(level);
        long now = level.getGameTime();
        boolean[] near0 = new boolean[FortressPlan.FLOORS + 2], on = new boolean[FortressPlan.FLOORS + 2];
        for (ServerPlayer p : near) {
            int f = floorOf(origin, p);
            near0[0] = true;
            if (f >= 0) on[f] = true;
            if (f == 0 || f == 1) near0[1] = true;
            if (f >= 1 && f < FortressPlan.FLOORS) near0[f + 1] = true;
            if (f >= 1) near0[f] = true;
        }
        for (int n = 0; n <= FortressPlan.FLOORS; n++) {
            boolean alive = !garrison(level, origin, n).isEmpty();
            boolean mustered = state.mustered(origin, n);
            long cleared = state.clearedAt(origin, n);
            if (!alive && mustered && cleared == 0) {
                state.cleared(origin, n, now);
                cleared = now;
            }
            if (alive || !near0[n]) continue;
            if (!mustered || (now - cleared >= RESPAWN && !on[n])) muster(level, origin, n);
        }
    }

    /**
     * Keeps each garrison to its floor, and Red Boy to his throne room: one found off it (up or
     * down a stair, out past the door's forecourt) is taken back to its post, its quarry forgotten.
     * Returns how many were.
     */
    public static int leash(ServerLevel level, BlockPos origin) {
        int n = 0;
        FortressPlan.Built plan = FortressPlan.get();
        for (Mob m : level.getEntitiesOfClass(Mob.class, bounds(origin).inflate(24), m -> m.isAlive()
                && (origin.equals(originOf(m)) || m instanceof RedBoyEntity))) {
            int home;
            if (m instanceof RedBoyEntity boy) {
                // only his of this make: his lair is the dais
                if (!boy.hasRestriction() || !boy.getRestrictCenter().equals(at(origin, 0, FortressPlan.THRONE_Y + 1, 0))) continue;
                home = 9;
            } else home = floorOf(m);
            BlockPos p = m.blockPosition();
            int x = p.getX() - origin.getX(), y = p.getY() - origin.getY(), z = p.getZ() - origin.getZ();
            boolean stray = home == 0 ? Math.abs(y) > 12 || Math.abs(x) > 40 || z < FortressPlan.DOOR[2] - 1 || z > FortressPlan.Z_MAX + 30
                    : plan.floorAt(x, y, z) != home;
            if (!stray || !m.hasRestriction()) continue;
            BlockPos back = m.getRestrictCenter();
            m.setTarget(null);
            m.getNavigation().stop();
            m.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
            m.setDeltaMovement(Vec3.ZERO);
            n++;
        }
        return n;
    }

    // ── The landings ────────────────────────────────────────────────────

    /** The deepest landing {@code p} has reached in this fortress (2-9), or 0. */
    public static int depth(Player p, BlockPos origin) {
        CompoundTag t = p.getPersistentData().getCompound(KEY_DEPTH);
        return t.getLong("Origin") == origin.asLong() ? t.getInt("Floor") : 0;
    }

    /** Marks landings reached: standing on floor n's landing (n 2-9), deeper than before. Returns the landing marked, or 0. */
    public static int markLanding(ServerPlayer p, BlockPos origin) {
        int f = floorOf(origin, p);
        if (f < 2) return 0;
        int[] b = FortressPlan.get().floor(f).landing();
        BlockPos at = p.blockPosition();
        int x = at.getX() - origin.getX(), y = at.getY() - origin.getY(), z = at.getZ() - origin.getZ();
        if (x < b[0] || x > b[3] || z < b[2] || z > b[5] || Math.abs(y - b[1]) > 1) return 0;
        if (depth(p, origin) >= f) return 0;
        CompoundTag t = new CompoundTag();
        t.putLong("Origin", origin.asLong());
        t.putInt("Floor", f);
        p.getPersistentData().put(KEY_DEPTH, t);
        String where = f >= 9 ? "the doors of the throne room" : FortressPlan.get().floor(f).name();
        p.displayClientMessage(Component.literal("§6You have come down to " + where + ". The brazier by the great door will bring you back here."), false);
        return f;
    }

    /** The return brazier: brings {@code p} back down to the deepest landing reached, if the door is open. Returns whether it did. */
    public static boolean returnDown(ServerLevel level, BlockPos origin, ServerPlayer p) {
        int f = depth(p, origin);
        if (f < 2) {
            p.displayClientMessage(Component.literal("§7The brazier burns low: you have come down no landing of the cave yet."), true);
            return false;
        }
        if (!doorOpen(level, origin)) {
            p.displayClientMessage(Component.literal("§7The brazier burns low: the door is shut."), true);
            return false;
        }
        int[] b = FortressPlan.get().floor(f).landing();
        BlockPos to = at(origin, (b[0] + b[3]) / 2, b[1], (b[2] + b[5]) / 2);
        level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        p.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
        level.playSound(null, to, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 0.6f);
        return true;
    }

    // ── A world made before ─────────────────────────────────────────────

    /**
     * Finds what make the fortress round {@code origin} is, once its centre is loaded: of this make
     * if its throne room's dais is there, else the old cave (left as it is). Returns the status.
     */
    public static int survey(ServerLevel level, BlockPos origin) {
        State s = State.of(level);
        int status = s.status(origin);
        if (status != State.UNKNOWN) return status;
        if (!level.hasChunkAt(origin)) return State.UNKNOWN;
        BlockPos dais = at(origin, 0, FortressPlan.THRONE_Y, 0);
        status = level.getBlockState(dais).is(Blocks.GILDED_BLACKSTONE) ? State.NEW : State.OLD;
        s.setStatus(origin, status);
        JTTWEngine.LOGGER.info("The Fire Cloud Cave at {} is of the {} make", origin.toShortString(), status == State.NEW ? "new" : "old");
        return status;
    }

    /**
     * Finishes the fortress over any of its chunks made before it was planned (they hold the land
     * as it was). Each of its chunks is looked over once, when it is loaded (none is loaded, let
     * alone made, for this): a few dozen of its plan's blocks are compared with the world's, and
     * if most are not as the plan says (an old chunk's rock and stone where the plan has halls,
     * terracotta bands and air) the plan is laid over the chunk whole. A chunk made with the plan
     * and changed since by the living (a wall mined, the door opened) differs in a few and is left
     * as it is. Once every chunk has been looked over the fortress is marked complete. Returns how
     * many chunks it laid this time.
     */
    public static int complete(ServerLevel level, BlockPos origin) {
        FortressPlan.Built plan = FortressPlan.get();
        State state = State.of(level);
        long checked = state.checked(origin);
        int laid = 0, cx0 = (origin.getX() + FortressPlan.X_MIN) >> 4, cz0 = (origin.getZ() + FortressPlan.Z_MIN) >> 4;
        int cx1 = (origin.getX() + FortressPlan.X_MAX) >> 4, cz1 = (origin.getZ() + FortressPlan.Z_MAX) >> 4;
        boolean all = true;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                long bit = 1L << ((cx - cx0) * (cz1 - cz0 + 1) + (cz - cz0));
                if ((checked & bit) != 0) continue;
                if (!level.hasChunk(cx, cz)) {
                    all = false;
                    continue;
                }
                int x0 = Math.max(cx * 16, origin.getX() + FortressPlan.X_MIN) - origin.getX(), x1 = Math.min(cx * 16 + 15, origin.getX() + FortressPlan.X_MAX) - origin.getX();
                int z0 = Math.max(cz * 16, origin.getZ() + FortressPlan.Z_MIN) - origin.getZ(), z1 = Math.min(cz * 16 + 15, origin.getZ() + FortressPlan.Z_MAX) - origin.getZ();
                if (stale(level, plan, origin, x0, z0, x1, z1)) {
                    FortressBlocks.layBox(level, origin, x0, FortressPlan.Y_MIN, z0, x1, FortressPlan.Y_MAX, z1);
                    laid++;
                }
                checked |= bit;
            }
        }
        state.checked(origin, checked);
        if (all) state.complete(origin);
        if (laid > 0) JTTWEngine.LOGGER.info("The Fire Cloud Cave at {}: {} chunks made before it were finished over", origin.toShortString(), laid);
        return laid;
    }

    /** Whether most of the plan's blocks sampled in the local columns x0..x1, z0..z1 are not as the plan says. */
    static boolean stale(ServerLevel level, FortressPlan.Built plan, BlockPos origin, int x0, int z0, int x1, int z1) {
        int seen = 0, wrong = 0;
        for (int x = x0; x <= x1; x += 4) for (int z = z0; z <= z1; z += 4) {
            for (int y = FortressPlan.Y_MIN; y <= FortressPlan.Y_MAX; y += 6) {
                BlockPos p = at(origin, x, y, z);
                BlockState want = FortressBlocks.state(plan, x, y, z, p.getX(), p.getY(), p.getZ());
                if (want == null || level.isOutsideBuildHeight(p)) continue;
                seen++;
                if (!level.getBlockState(p).is(want.getBlock())) wrong++;
            }
        }
        return seen > 0 && wrong * 2 > seen;
    }

    // ── Looking after it ────────────────────────────────────────────────

    private static BlockPos worldOrigin;
    private static long worldSeed = Long.MIN_VALUE;

    /** Looks after the world's fortress while anyone is near it. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        openDoors();
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % EVERY != 0 || level.players().isEmpty()) return;
        if (worldSeed != level.getSeed()) {
            worldSeed = level.getSeed();
            worldOrigin = null;
        }
        int[][] cs = Zones.FIRE_MOUNTAIN.centres(level.getSeed());
        if (cs.length == 0) return;
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && Math.abs(p.getX() - cs[0][0]) < NEAR && Math.abs(p.getZ() - cs[0][1]) < NEAR) near.add(p);
        }
        if (near.isEmpty()) return;
        if (worldOrigin == null) worldOrigin = origin(level);
        look(level, worldOrigin, near);
    }

    /** One look at the fortress round {@code origin}, the players near it: its make, its garrisons, its strays, the landings. */
    public static void look(ServerLevel level, BlockPos origin, List<ServerPlayer> near) {
        if (survey(level, origin) != State.NEW) return;
        if (!State.of(level).completed(origin)) complete(level, origin);
        garrisons(level, origin, near);
        leash(level, origin);
        for (ServerPlayer p : near) markLanding(p, origin);
    }

    /** The door (knocked on by using it) and the return brazier. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer p)) return;
        BlockState here = level.getBlockState(event.getPos());
        boolean doorish = FortressBlocks.doorish(here);
        if (!doorish && !here.is(Blocks.CAMPFIRE)) return;
        BlockPos origin = level.dimension().equals(Level.OVERWORLD) ? origin(level) : null;
        if (origin == null || Math.abs(event.getPos().getX() - origin.getX()) > 64 || Math.abs(event.getPos().getZ() - origin.getZ()) > 96) return;
        use(level, origin, p, event.getPos(), event);
    }

    /** Using a block of the fortress round {@code origin}: knocking on its door, or its return brazier. Returns whether it was one. */
    public static boolean use(ServerLevel level, BlockPos origin, ServerPlayer p, BlockPos pos, @Nullable PlayerInteractEvent.RightClickBlock event) {
        int x = pos.getX() - origin.getX(), y = pos.getY() - origin.getY(), z = pos.getZ() - origin.getZ();
        byte c = FortressPlan.code(x, y, z);
        if (!FortressPlan.door(c) && c != FortressPlan.BRAZIER) return false;
        if (State.of(level).status(origin) == State.OLD) return false;
        if (FortressPlan.door(c)) knock(level, origin, p);
        else returnDown(level, origin, p);
        if (event != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
        return true;
    }

    /**
     * The door and the seals do not yield to picks (nor their metal to the miner): the door is
     * opened by knocking once its wardens are dead, a seal by its guardian's death. A player in
     * creative breaks them as anything else.
     */
    @SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer().isCreative()) return;
        BlockState st = event.getState();
        if (!st.is(Blocks.IRON_BARS) && !FortressBlocks.doorish(st)) return;
        BlockPos origin = level.dimension().equals(Level.OVERWORLD) ? origin(level) : null;
        if (origin != null && guarded(level, origin, event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("§7It will not yield: the cave's own hold it."), true);
        }
    }

    /** Whether world {@code p} is part of the door or a seal of the fortress round {@code origin}, still standing. */
    public static boolean guarded(ServerLevel level, BlockPos origin, BlockPos p) {
        if (State.of(level).status(origin) == State.OLD) return false;
        byte c = code(level, origin, p);
        return FortressPlan.door(c) || FortressPlan.seal(c);
    }

    /** Nothing of the world's own is born inside the cave: its floors hold their garrisons, no more. */
    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        int[][] cs = Zones.FIRE_MOUNTAIN.centres(level.getSeed());
        if (cs.length == 0) return;
        BlockPos pos = event.getPos();
        if (Math.abs(pos.getX() - cs[0][0]) > FortressPlan.SHELL + 2 || Math.abs(pos.getZ() - cs[0][1]) > FortressPlan.Z_MAX) return;
        BlockPos origin = worldOrigin != null && worldSeed == level.getSeed() ? worldOrigin : origin(level);
        if (inside(origin, pos)) event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
}
