package com.jttw.engine.character.wukong;

import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.EyeLaserBeamPacket;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncEyeLaserOutputPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Fiery Golden Eyes' laser (火眼金睛): a shaft of golden fire from Wukong's eyes, at one of five
 * outputs he chooses (held N and the mouse wheel, {@link #changeOutput}). Each output is wider,
 * longer, harder-hitting and hotter than the one below, and costs more Qi; it is kept with him
 * (his persistent data, through death too).
 *
 * <p>The beam is a straight cut along his look: every block within its radius of the axis goes
 * (the radius flares out from his eyes over the first blocks, so it never takes the ground from
 * under his feet), and every creature it touches is struck by his power and set burning. What it
 * cannot break stops it: an unbreakable block (bedrock) always, an obsidian-hard one (hardness 50
 * and over) below the fifth output. The Fighting Buddha's beam breaks anything at every output
 * ({@link BuddhaBreaking}). Liquids it passes through. Blocks it cuts drop as mined up to the third
 * output; above it they are burnt away to nothing.
 *
 * <p>The players near it are told ({@link EyeLaserBeamPacket}) and draw it: two beams from his
 * eyes meeting in one shaft to where it stopped, sparks, glowing debris and a burst where it ends.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.jttw.engine.JTTWEngine.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.GAME)
public final class EyeLaser {

    private EyeLaser() {}

    /** His client is told his chosen output as he joins. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    /**
     * One output of the laser.
     *
     * @param radius   how far from the beam's axis it cuts, in blocks, to a block's nearest face
     *                 (0: the blocks the axis itself passes through)
     * @param damage   its hit at level 1, scaled by his power
     * @param drops    whether what it cuts drops, as mined, or is burnt away
     * @param melts    whether it burns through obsidian-hard blocks
     * @param lights   whether it lights the furnaces it meets (stopping there) rather than cutting them
     */
    public record Output(int level, String name, float qi, double range, double radius, float damage,
                         int burnTicks, int cooldownTicks, float fireChance, boolean drops, boolean melts, boolean lights) {
        public String numeral() { return NUMERALS[level - 1]; }
    }

    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V"};

    /** The five outputs, weakest first. The first is the laser as it was: 10 Qi, a hit of 8. */
    public static final List<Output> OUTPUTS = List.of(
            new Output(1, "Glint",    10f, 20, 0.0,  8f,  60,  60, 0.20f, true,  false, true),
            new Output(2, "Gaze",     20f, 30, 0.5, 13f,  80,  80, 0.30f, true,  false, true),
            new Output(3, "Glare",    35f, 40, 1.0, 20f, 100, 100, 0.40f, true,  false, false),
            new Output(4, "Blaze",    55f, 52, 1.8, 32f, 140, 140, 0.50f, false, false, false),
            new Output(5, "Sunfire",  80f, 64, 2.8, 50f, 200, 200, 0.60f, false, true,  false));

    public static final int MIN = 1, MAX = OUTPUTS.size();

    /** The output he has chosen, in his persistent data (absent: the first). */
    public static final String KEY_OUTPUT = "jttw_eye_laser_output";
    /** The game time the laser is ready again (absent: ready). */
    public static final String KEY_READY_AT = "jttw_eye_laser_ready_at";
    public static final String ABILITY_ID = "wu_golden_eyes";

    /** Obsidian's hardness: below the fifth output, this hard or harder stops the beam. */
    public static final float OBSIDIAN_HARD = 50f;
    /** How far the players who are told of a beam may be from his eyes. */
    public static final double TELL_RADIUS = 160;
    /** The most destroyed blocks a beam tells the clients of, for their glowing debris. */
    public static final int DEBRIS_SAMPLES = 40;
    /** How far along the beam it starts cutting, from his eyes, and how fast its radius flares out. */
    private static final double START = 0.5, FLARE = 0.6;
    private static final double STEP = 0.25;

    public static Output output(int level) {
        return OUTPUTS.get(Mth.clamp(level, MIN, MAX) - 1);
    }

    /** The output he has chosen. */
    public static Output output(ServerPlayer player) {
        return output(level(player));
    }

    public static int level(ServerPlayer player) {
        var data = player.getPersistentData();
        return data.contains(KEY_OUTPUT) ? Mth.clamp(data.getInt(KEY_OUTPUT), MIN, MAX) : MIN;
    }

    /** Sets his output (held within the five), tells him on the action bar and his HUD. */
    public static void setOutput(ServerPlayer player, int level) {
        Output o = output(level);
        player.getPersistentData().putInt(KEY_OUTPUT, o.level());
        sync(player);
        player.displayClientMessage(describe(o), true);
    }

    /** One step of the mouse wheel while N is held: up for more output, down for less. */
    public static void changeOutput(ServerPlayer player, int delta) {
        if (!ImmortalItems.hasFieryGoldenEyes(player) || delta == 0) return;
        int now = level(player), next = Mth.clamp(now + Integer.signum(delta), MIN, MAX);
        if (next == now) { // at the end already: say so, but nothing changes
            player.displayClientMessage(describe(output(now)), true);
            return;
        }
        setOutput(player, next);
    }

    /** "Fiery Golden Eyes: Output III, Glare (35 Qi, 40 blocks)", for the action bar. */
    public static Component describe(Output o) {
        String pips = "▮".repeat(o.level()) + "§8" + "▮".repeat(MAX - o.level());
        return Component.literal("§6Fiery Golden Eyes §e" + pips + " §6Output " + o.numeral()
                + "§e, " + o.name() + " §7(" + (int) o.qi() + " Qi, " + (int) o.range() + " blocks)");
    }

    /** Tells his client which output is chosen (the HUD shows it by slot N). */
    public static void sync(ServerPlayer player) {
        NetworkHandler.sendToPlayer(player, new SyncEyeLaserOutputPacket(level(player)));
    }

    /** What a shot did: where it began and ended, whether something stopped it, what it cut and struck. */
    public record Shot(Output output, Vec3 from, Vec3 to, boolean stopped, Set<BlockPos> cut, List<LivingEntity> struck, Set<BlockPos> lit) {
        public double length() { return from.distanceTo(to); }
    }

    /**
     * Fires the laser at his chosen output, if the eyes are his, it is ready and he has the Qi.
     * Returns what it did, or null if it did not fire.
     */
    @Nullable
    public static Shot fire(ServerPlayer player) {
        if (!ImmortalItems.hasFieryGoldenEyes(player)) return null;
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(KEY_READY_AT)) {
            player.sendSystemMessage(Component.literal("§cEye Laser is on cooldown!"));
            return null;
        }
        Output o = output(player);
        if (!QiData.spend(player, o.qi())) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi! §7(Output " + o.numeral()
                    + " needs " + (int) o.qi() + ")"));
            return null;
        }
        // only a shot actually fired starts the cooldown
        player.getPersistentData().putLong(KEY_READY_AT, now + o.cooldownTicks());
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));
        NetworkHandler.sendToPlayer(player, new SyncCooldownPacket(player.getUUID(), ABILITY_ID, o.cooldownTicks(), o.cooldownTicks()));
        return burn(player, o);
    }

    /** The beam itself, at this output: cut, struck, set burning, heard, and shown. */
    static Shot burn(ServerPlayer player, Output o) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        boolean unstoppable = BuddhaBreaking.unstoppable(player);

        // where it stops: the first block on its axis it cannot break, or its range
        double length = o.range();
        boolean stopped = false;
        for (double t = START; t <= o.range(); t += STEP) {
            BlockPos at = BlockPos.containing(eye.add(look.scale(t)));
            BlockState state = level.getBlockState(at);
            if (!state.isAir() && (lights(o, state) || !breaks(level, at, state, o, unstoppable))) {
                length = Math.max(START, exitDistance(eye, look, at, t));
                stopped = true;
                break;
            }
        }

        // what it cuts: every block within its radius of the axis, up to where it stopped
        AABB self = player.getBoundingBox().expandTowards(0, -1, 0).inflate(0.2, 0, 0.2);
        Set<BlockPos> cut = new LinkedHashSet<>();
        Set<BlockPos> lit = new LinkedHashSet<>();
        if (stopped) {
            BlockPos at = BlockPos.containing(eye.add(look.scale(length + 0.01)));
            if (lights(o, level.getBlockState(at))) lit.add(at);
        }
        // nothing past what stopped it goes (its end is flat there, not rounded)
        double farthest = length + (stopped ? 0 : 0.5);
        for (double t = START; t <= length + 1e-6; t += STEP) {
            Vec3 p = eye.add(look.scale(Math.min(t, length)));
            double r = radiusAt(o, t);
            int reach = (int) Math.ceil(r) + 1;
            BlockPos c = BlockPos.containing(p);
            for (int dx = -reach; dx <= reach; dx++) for (int dy = -reach; dy <= reach; dy++) for (int dz = -reach; dz <= reach; dz++) {
                BlockPos b = c.offset(dx, dy, dz);
                if (cut.contains(b)) continue;
                if (distanceToBlock(p, b) > r + 1e-4) continue;
                if (Vec3.atCenterOf(b).subtract(eye).dot(look) > farthest) continue;
                if (new AABB(b).intersects(self)) continue;
                BlockState state = level.getBlockState(b);
                if (state.isAir() || state.getBlock() instanceof LiquidBlock) continue;
                if (lights(o, state)) { lit.add(b.immutable()); continue; } // lit, not cut
                if (!breaks(level, b, state, o, unstoppable)) continue;
                cut.add(b.immutable());
            }
        }

        // the players near are told first, while the blocks still stand for their debris
        Vec3 end = eye.add(look.scale(length));
        List<EyeLaserBeamPacket.Debris> debris = new ArrayList<>();
        int every = Math.max(1, cut.size() / DEBRIS_SAMPLES);
        int i = 0;
        for (BlockPos b : cut) {
            if (i++ % every == 0 && debris.size() < DEBRIS_SAMPLES) debris.add(new EyeLaserBeamPacket.Debris(b, Block.getId(level.getBlockState(b))));
        }
        NetworkHandler.sendToNear(level, eye, TELL_RADIUS,
                new EyeLaserBeamPacket(player.getId(), eye, end, o.level(), (float) visualRadius(o), stopped, unstoppable, debris));

        for (BlockPos b : cut) destroy(level, b, o.drops(), player);

        // fire on the floor it leaves, and all round where it ends
        var random = player.getRandom();
        for (BlockPos b : cut) {
            if (random.nextFloat() < o.fireChance()) light(level, b);
        }
        BlockPos tip = BlockPos.containing(end);
        int ring = (int) Math.ceil(o.radius()) + 1;
        for (int dx = -ring; dx <= ring; dx++) for (int dy = -ring; dy <= ring; dy++) for (int dz = -ring; dz <= ring; dz++) {
            if (random.nextFloat() < 0.5f) light(level, tip.offset(dx, dy, dz));
        }

        // what it strikes: every creature it touches, once, by his power
        float damage = LevelPower.scaled(player, o.damage());
        double hitRadius = Math.max(0.35, o.radius() + 0.3);
        AABB box = new AABB(eye, end).inflate(hitRadius + 2);
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            if (e instanceof com.jttw.engine.entity.WuCloneEntity clone
                    && clone.getOwnerUUID().filter(player.getUUID()::equals).isPresent()) continue; // his own clones
            AABB bb = e.getBoundingBox();
            Vec3 nearest = closestOnSegment(eye, end, bb.getCenter());
            double d = Math.sqrt(distanceSq(nearest, bb));
            if (d > hitRadius) continue;
            e.hurt(level.damageSources().magic(), damage);
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), o.burnTicks()));
            FieryGaze.ignite(e, player, o.level()); // and it burns on, until it dies
            struck.add(e);
        }

        // a deep roar from his eyes, searing; a blast where it ends
        float lv = o.level();
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f + 0.35f * lv, 0.95f - 0.08f * lv);
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f + 0.3f * lv, 0.55f - 0.04f * lv);
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f + 0.3f * lv, 0.6f);
        level.playSound(null, end.x, end.y, end.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f + 0.3f * lv, 0.5f);
        if (o.level() >= 3) level.playSound(null, end.x, end.y, end.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4f * lv, 0.7f);

        Set<BlockPos> lighted = new LinkedHashSet<>();
        for (BlockPos b : lit) if (lightFurnace(level, b)) lighted.add(b);

        return new Shot(o, eye, end, stopped, cut, struck, lighted);
    }

    /** Whether the beam at this output lights the block rather than cutting it: a furnace (any kind, the Eight Trigrams Furnace too) at outputs I and II. */
    static boolean lights(Output o, BlockState state) {
        if (!o.lights()) return false;
        var block = state.getBlock();
        return block instanceof net.minecraft.world.level.block.AbstractFurnaceBlock
                || block instanceof com.jttw.engine.world.furnace.TrigramsFurnaceBlock
                || block instanceof com.jttw.engine.world.furnace.FurnacePartBlock;
    }

    /**
     * Lights the furnace at {@code pos}: a furnace, blast furnace or smoker takes as much fuel as a
     * lava bucket gives it (a furnace 20,000 ticks, a hundred items; the others half the time, the
     * same hundred items, as they burn twice as fast), if it has less; the Eight Trigrams Furnace
     * is lit as true fire lights it ({@link com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity#ignite}).
     * Returns whether it was a furnace.
     */
    static boolean lightFurnace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos core = state.getBlock() instanceof com.jttw.engine.world.furnace.FurnacePartBlock
                ? com.jttw.engine.world.furnace.FurnacePartBlock.corePos(state, pos) : pos;
        BlockEntity be = level.getBlockEntity(core);
        if (be instanceof com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity trigrams) {
            trigrams.ignite(com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity.IGNITED_BURN);
            return true;
        }
        if (!(be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace)) return false;
        try {
            int lava = (int) LAVA_BURN.invoke(furnace, new ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
            if ((int) LIT_TIME.get(furnace) < lava) {
                LIT_TIME.set(furnace, lava);
                LIT_DURATION.set(furnace, lava);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        if (state.hasProperty(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT) && !state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)) {
            level.setBlock(core, state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true), Block.UPDATE_ALL);
        }
        furnace.setChanged();
        return true;
    }

    /** A furnace's fire left (litTime) and its fuel's whole (litDuration), and what an item burns in it (getBurnDuration). */
    private static final java.lang.reflect.Field LIT_TIME = net.neoforged.fml.util.ObfuscationReflectionHelper.findField(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "litTime");
    private static final java.lang.reflect.Field LIT_DURATION = net.neoforged.fml.util.ObfuscationReflectionHelper.findField(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "litDuration");
    private static final java.lang.reflect.Method LAVA_BURN = net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "getBurnDuration", ItemStack.class);

    /** Whether the beam at this output breaks the block: everything for the Fighting Buddha. */
    static boolean breaks(ServerLevel level, BlockPos pos, BlockState state, Output o, boolean unstoppable) {
        if (unstoppable) return true;
        if (state.getBlock() instanceof LiquidBlock) return true; // it passes through, leaving it
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return false;
        return o.melts() || hardness < OBSIDIAN_HARD;
    }

    /** The cut's radius {@code t} blocks from his eyes: flaring out to the output's own. */
    static double radiusAt(Output o, double t) {
        return Math.min(o.radius(), Math.max(0, (t - START) * FLARE));
    }

    /** How wide the beam is drawn: its bright heart three quarters of its cut, its glow beyond; never a hair. */
    static double visualRadius(Output o) {
        return 0.15 + o.radius() * 0.75;
    }

    /** How far along the axis it reaches a block that stops it: its near face. */
    private static double exitDistance(Vec3 eye, Vec3 look, BlockPos block, double hitT) {
        var hit = new AABB(block).clip(eye, eye.add(look.scale(hitT + 1)));
        return hit.map(v -> v.distanceTo(eye)).orElse(hitT);
    }

    private static double distanceToBlock(Vec3 p, BlockPos b) {
        return Math.sqrt(distanceSq(p, new AABB(b)));
    }

    private static double distanceSq(Vec3 p, AABB box) {
        double dx = Math.max(0, Math.max(box.minX - p.x, p.x - box.maxX));
        double dy = Math.max(0, Math.max(box.minY - p.y, p.y - box.maxY));
        double dz = Math.max(0, Math.max(box.minZ - p.z, p.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vec3 closestOnSegment(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        if (len < 1e-9) return a;
        double t = Mth.clamp(p.subtract(a).dot(ab) / len, 0, 1);
        return a.add(ab.scale(t));
    }

    /** Takes a block away as {@link net.minecraft.world.level.Level#destroyBlock} does, without its crumbs and crack (the clients draw their own). */
    private static void destroy(ServerLevel level, BlockPos pos, boolean drops, ServerPlayer by) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        if (drops) {
            BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            Block.dropResources(state, level, pos, be, by, ItemStack.EMPTY);
        }
        if (level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), 3)) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(by, state));
        }
    }

    /** Fire at {@code pos}, if it is open and fire would stand there. */
    private static void light(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        }
    }
}
