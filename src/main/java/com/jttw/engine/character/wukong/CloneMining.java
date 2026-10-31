package com.jttw.engine.character.wukong;

import com.jttw.engine.entity.WuCloneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How a shadow clone gathers: it plans its work as a player would, rather than grabbing at the
 * nearest block.
 *
 * <ul>
 *   <li><b>Finding work.</b> It sweeps the ground round its work (its work site, or its owner
 *       while it follows him) a layer at a time, a few thousand blocks a tick at most, for the
 *       ores it has a good enough pickaxe for, logs it has an axe for, ripe crops. A following
 *       clone takes only what lies open to the air (what a player walking by would see); a
 *       working one also digs for what is buried.</li>
 *   <li><b>Sharing it.</b> It takes a whole vein (or tree) at once, every connected block of it,
 *       and claims it, so its fellows go to other veins; it weighs the rarest ores first and
 *       veins away from where its fellows already are.</li>
 *   <li><b>Reaching it.</b> It works a block from a player's reach ({@value CloneSense#REACH}
 *       blocks, with the face in sight): if it can already, it works it from where it stands;
 *       else it finds a spot from which it could (never on the block, nor on the vein), and
 *       paths there. Within reach of a block it cannot see, it digs what is in the way. A
 *       working clone with no spot it can walk to tunnels (level, or in stairs up or down,
 *       bridging gaps with stone from its pack) or pillars up under what is overhead, and
 *       climbs down its pillar again after.</li>
 *   <li><b>Staying safe.</b> It never breaks a block that holds back lava or water, its own
 *       floor or the floor of the path it is taking, nor what would drop sand or gravel on its
 *       head; it digs only natural ground, never what someone built; and gives up on what it
 *       cannot reach (for a while) rather than trying forever.</li>
 * </ul>
 * What it breaks goes straight into its pack.
 */
public final class CloneMining {

    private CloneMining() {}

    /** Most new paths worked out per tick, by all clones together. */
    public static final int PATHS_PER_TICK = 4;
    /** How long it leaves a block it could not reach before trying it again. */
    public static final int GIVE_UP_TICKS = 20 * 120;
    /** How long it tries one block before giving up on it. */
    public static final int TRY_TICKS = 20 * 30;
    /** Most blocks it takes as one vein. */
    public static final int MAX_VEIN = 64;

    // ── Claims: one clone to a vein, one to a spot ───────────────────────

    private static final Map<Long, WuCloneEntity> CLAIMS = new ConcurrentHashMap<>();
    private static final Map<Long, WuCloneEntity> SPOTS = new ConcurrentHashMap<>();

    private static boolean heldByAnother(Map<Long, WuCloneEntity> map, WuCloneEntity me, BlockPos p) {
        WuCloneEntity by = map.get(p.asLong());
        if (by == null || by == me) return false;
        if (by.level() != me.level()) return false; // a block of the same coordinates, elsewhere
        if (!by.isAlive() || by.isRemoved()) {
            map.remove(p.asLong(), by);
            return false;
        }
        return true;
    }

    /** Whether another clone has taken this block as its work. */
    public static boolean claimedByAnother(WuCloneEntity me, BlockPos p) {
        return heldByAnother(CLAIMS, me, p);
    }

    /** The clone that has taken this block, if any. */
    @Nullable
    public static WuCloneEntity claimant(Level level, BlockPos p) {
        WuCloneEntity by = CLAIMS.get(p.asLong());
        return by != null && by.level() == level && by.isAlive() ? by : null;
    }

    /** Lets go of everything the clone has taken. */
    public static void releaseAll(WuCloneEntity me) {
        CLAIMS.values().removeIf(c -> c == me);
        SPOTS.values().removeIf(c -> c == me);
    }

    private static long budgetTick = -1;
    private static int budgetUsed;

    /** Whether there is room this tick for one more path (bounded across all clones). */
    static boolean mayPath(Level level) {
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            budgetUsed = 0;
        }
        if (budgetUsed >= PATHS_PER_TICK) return false;
        budgetUsed++;
        return true;
    }

    /** Whether some side of the block is open to the air: what a passing player would see. */
    static boolean exposed(Level level, BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockPos n = p.relative(d);
            if (level.getBlockState(n).getCollisionShape(level, n).isEmpty()) return true;
        }
        return false;
    }

    // ── The goal ─────────────────────────────────────────────────────────

    private enum Phase { PLAN, MOVE, BREAK, STEP, PILLAR }

    static final class WorkGoal extends Goal {

        private final WuCloneEntity clone;
        @Nullable private ServerPlayer owner;

        // the sweep
        private final List<BlockPos> found = new ArrayList<>();
        private final List<BlockPos> sweeping = new ArrayList<>();
        private boolean sweepOn;
        private int sweepY, sweepTop, sweepR, restIn;
        private BlockPos sweepCentre = BlockPos.ZERO;
        private boolean sweepExposedOnly;

        // the job
        private final List<BlockPos> vein = new ArrayList<>();
        @Nullable private BlockPos target;
        @Nullable private BlockPos spot;
        private Phase phase = Phase.PLAN;
        private int targetTicks, phaseTicks, fails, approaches, pillars, waitTicks;
        private final Map<Long, Long> shunned = new HashMap<>();

        // breaking
        @Nullable private BlockPos breaking;
        private boolean breakingOwnPillar;
        private ItemStack tool = ItemStack.EMPTY;
        private int progress, needed;

        // tunnelling and building
        private final ArrayDeque<BlockPos> dig = new ArrayDeque<>();
        @Nullable private BlockPos stepTo;
        @Nullable private BlockPos floorAt;
        @Nullable private BlockPos pillarFrom;
        private final Set<BlockPos> pillar = new HashSet<>();

        WorkGoal(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean fighting() {
            return clone.getTarget() != null && clone.getTarget().isAlive();
        }

        private boolean mayWork() {
            if (fighting() || clone.isRetreating() || CloneWork.nearlyFull(clone.pack())) return false;
            WuCloneEntity.Priority pr = clone.getPriority();
            if (pr == WuCloneEntity.Priority.GUARD) return false;
            owner = WuCloneManager.resolveOwner(clone);
            if (owner == null || owner.level() != clone.level()) return false;
            // following: only while he is near; first catch him up
            return pr != WuCloneEntity.Priority.FOLLOW || clone.distanceToSqr(owner) < sq(CloneWork.FOLLOW_RADIUS + 6);
        }

        private static double sq(double d) {
            return d * d;
        }

        private boolean tunnels() {
            return clone.getPriority() == WuCloneEntity.Priority.WORK;
        }

        @Override
        public boolean canUse() {
            if (!mayWork()) return false;
            if (pickJob()) return true;
            if (restIn > 0) {
                restIn--;
                return false;
            }
            if (!sweepOn) startSweep();
            int layers = sweepExposedOnly ? 3 : 1;
            for (int i = 0; i < layers && sweepOn; i++) sweepLayer();
            if (sweepOn) return false;
            found.clear();
            found.addAll(sweeping);
            sweeping.clear();
            if (pickJob()) return true;
            restIn = tunnels() ? 20 : 10; // canUse runs every other tick: a second or so before the next sweep
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (!mayWork()) return false;
            return target != null || !vein.isEmpty() || breaking != null || phase != Phase.PLAN || onOwnPillar();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            phase = Phase.PLAN;
            clone.markBusy();
        }

        @Override
        public void stop() {
            if (breaking != null) clone.level().destroyBlockProgress(clone.getId(), breaking, -1);
            breaking = null;
            needed = progress = 0;
            target = null;
            spot = null;
            vein.clear();
            dig.clear();
            stepTo = null;
            floorAt = null;
            pillarFrom = null;
            phase = Phase.PLAN;
            releaseAll(clone);
            clone.setWorkingOn(null);
            clone.setGathering(false);
            clone.getNavigation().stop();
        }

        // ── Finding work ────────────────────────────────────────────

        private void startSweep() {
            BlockPos site = clone.workSite();
            sweepExposedOnly = site == null;
            sweepCentre = site != null ? site : owner.blockPosition();
            sweepR = site != null ? CloneWork.WORK_RADIUS : CloneWork.FOLLOW_RADIUS;
            int depth = site != null ? CloneWork.WORK_DEPTH : CloneWork.FOLLOW_DEPTH;
            sweepY = sweepCentre.getY() - depth;
            sweepTop = sweepCentre.getY() + depth;
            sweeping.clear();
            fitTool.clear();
            sweepOn = true;
        }

        private final Map<Block, Boolean> fitTool = new HashMap<>();

        private void sweepLayer() {
            Level level = clone.level();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int y = sweepY++;
            if (sweepY > sweepTop) sweepOn = false;
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return;
            for (int dx = -sweepR; dx <= sweepR; dx++) for (int dz = -sweepR; dz <= sweepR; dz++) {
                if (dx * dx + dz * dz > sweepR * sweepR) continue;
                p.set(sweepCentre.getX() + dx, y, sweepCentre.getZ() + dz);
                if (!level.isLoaded(p)) continue;
                BlockState s = level.getBlockState(p);
                if (!CloneWork.workable(s)) continue;
                if (sweepExposedOnly && !exposed(level, p)) continue;
                if (!fitTool.computeIfAbsent(s.getBlock(), b -> CloneWork.toolFor(owner, s) != null)) continue;
                if (sweeping.size() < 512) sweeping.add(p.immutable());
            }
        }

        private boolean shunned(BlockPos p) {
            Long until = shunned.get(p.asLong());
            if (until == null) return false;
            if (until < clone.level().getGameTime()) {
                shunned.remove(p.asLong());
                return false;
            }
            return true;
        }

        private void shun(BlockPos p) {
            shunned.put(p.asLong(), clone.level().getGameTime() + GIVE_UP_TICKS);
            if (shunned.size() > 2048) shunned.clear();
        }

        /** Whether this block is still work it may take now. */
        private boolean takeable(BlockPos p, BlockState s) {
            if (!CloneWork.workable(s) || claimedByAnother(clone, p) || shunned(p)) return false;
            if (!CloneWork.inArea(clone, owner, p)) return false;
            if (sweepExposedOnly && !exposed(clone.level(), p)) return false;
            return CloneWork.toolFor(owner, s) != null;
        }

        /**
         * The best vein to take: near, open to the air rather than buried, the rarest ores first,
         * away from where its fellows work. Takes it whole.
         */
        private boolean pickJob() {
            if (found.isEmpty()) return false;
            Level level = clone.level();
            List<BlockPos> fellows = new ArrayList<>();
            for (WuCloneEntity other : WuCloneManager.clonesOf(owner)) {
                if (other != clone && other.workingOn() != null) fellows.add(other.workingOn());
            }
            BlockPos best = null;
            double bestCost = Double.MAX_VALUE;
            for (var it = found.iterator(); it.hasNext(); ) {
                BlockPos p = it.next();
                BlockState s = level.getBlockState(p);
                if (!CloneWork.workable(s)) {
                    it.remove();
                    continue;
                }
                if (!takeable(p, s)) continue;
                double cost = Math.sqrt(clone.distanceToSqr(Vec3.atCenterOf(p)))
                        + (exposed(level, p) ? 0 : 5) - CloneWork.worth(s) * 1.5;
                for (BlockPos f : fellows) if (f.distSqr(p) < 25) cost += 8;
                if (cost < bestCost) {
                    best = p;
                    bestCost = cost;
                }
            }
            if (best == null) return false;
            takeVein(best);
            return !vein.isEmpty();
        }

        /** Takes every block of the vein (or tree) that {@code start} is part of. */
        private void takeVein(BlockPos start) {
            Level level = clone.level();
            BlockState kind = level.getBlockState(start);
            vein.clear();
            ArrayDeque<BlockPos> open = new ArrayDeque<>();
            Set<BlockPos> seen = new HashSet<>();
            open.add(start);
            seen.add(start);
            while (!open.isEmpty() && vein.size() < MAX_VEIN) {
                BlockPos p = open.poll();
                if (claimedByAnother(clone, p)) continue;
                vein.add(p);
                CLAIMS.put(p.asLong(), clone);
                if (CloneWork.isRipeCrop(kind)) break; // a crop is its own job
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    BlockPos n = p.offset(dx, dy, dz);
                    if (seen.add(n) && CloneWork.inArea(clone, owner, n, 4) && CloneWork.sameVein(kind, level.getBlockState(n))) open.add(n);
                }
            }
            if (kind.is(net.minecraft.tags.BlockTags.LOGS) && !CloneWork.isTree(level, vein)) { // someone's log house
                for (BlockPos p : vein) {
                    CLAIMS.remove(p.asLong(), clone);
                    shun(p);
                }
                found.removeAll(vein);
                vein.clear();
                return;
            }
            found.removeAll(vein);
            target = null;
            approaches = pillars = 0;
            clone.markBusy();
        }

        /** The next block of its vein: one it can work from where it stands, else the nearest. */
        @Nullable
        private BlockPos nextInVein() {
            Level level = clone.level();
            Vec3 eye = clone.getEyePosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (var it = vein.iterator(); it.hasNext(); ) {
                BlockPos p = it.next();
                BlockState s = level.getBlockState(p);
                if (!CloneWork.workable(s) || shunned(p)) {
                    it.remove();
                    CLAIMS.remove(p.asLong(), clone);
                    continue;
                }
                double d = CloneSense.canWork(level, clone, eye, p) ? -1 : clone.distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestDist) {
                    best = p;
                    bestDist = d;
                }
            }
            return best;
        }

        private void giveUp(BlockPos p) {
            shun(p);
            vein.remove(p);
            CLAIMS.remove(p.asLong(), clone);
            if (p.equals(target)) target = null;
            resetMoves();
        }

        private void resetMoves() {
            if (breaking != null) clone.level().destroyBlockProgress(clone.getId(), breaking, -1);
            breaking = null;
            needed = progress = 0;
            dig.clear();
            stepTo = null;
            floorAt = null;
            pillarFrom = null;
            releaseSpot();
            phase = Phase.PLAN;
            phaseTicks = 0;
        }

        private void releaseSpot() {
            if (spot != null) SPOTS.remove(spot.asLong(), clone);
            spot = null;
        }

        // ── Working ─────────────────────────────────────────────────

        @Override
        public void tick() {
            owner = WuCloneManager.resolveOwner(clone);
            if (owner == null) return;
            clone.markBusy();
            phaseTicks++;
            if (target != null) targetTicks++;
            switch (phase) {
                case PLAN -> think();
                case MOVE -> moving();
                case BREAK -> breaking();
                case STEP -> stepping();
                case PILLAR -> pillaring();
            }
            clone.setWorkingOn(target);
        }

        private boolean onOwnPillar() {
            return !pillar.isEmpty() && clone.onGround() && pillar.contains(clone.blockPosition().below());
        }

        private void think() {
            if (waitTicks > 0) {
                waitTicks--;
                return;
            }
            Level level = clone.level();
            if (target != null && !CloneWork.workable(level.getBlockState(target))) {
                vein.remove(target);
                CLAIMS.remove(target.asLong(), clone);
                target = null;
            }
            if (target == null) {
                target = nextInVein();
                targetTicks = 0;
                fails = 0;
                releaseSpot();
                if (target == null) {
                    // done here: down its own pillar first, block by block, as a player would
                    if (onOwnPillar()) beginBreak(clone.blockPosition().below(), true);
                    else releaseAll(clone);
                    return;
                }
            }
            if (targetTicks > TRY_TICKS) {
                giveUp(target);
                return;
            }
            Vec3 eye = clone.getEyePosition();
            if (!standingOnIt(target) && CloneSense.canWork(level, clone, eye, target)) {
                beginBreak(target, false);
                return;
            }
            // in reach, but something is in the way: dig it, if it is plain ground
            if (CloneSense.reachTo(eye, target) <= CloneSense.REACH) {
                BlockPos way = CloneSense.firstInTheWay(level, clone, eye, target);
                if (way != null && !vein.contains(way) && diggable(way)) {
                    beginBreak(way, false);
                    return;
                }
            }
            if (!mayPath(level)) return; // its turn to work out a path comes next tick
            Path path = findSpot(target);
            if (path != null) {
                clone.getNavigation().moveTo(path, 1.0);
                setPhase(Phase.MOVE);
                return;
            }
            if (approaches < 3 && approach(target)) return;
            if (tunnels() && pillarUp(target)) return;
            if (tunnels() && digToward(target)) return;
            // nothing will do from here: wait a moment and look again (things move), then give it up
            if (++fails >= 4) giveUp(target);
            else {
                approaches = 0;
                waitTicks = 20;
            }
        }

        private void setPhase(Phase p) {
            phase = p;
            phaseTicks = 0;
        }

        /** Whether it may and safely can break this block on the way to its work. */
        private boolean diggable(BlockPos p) {
            Level level = clone.level();
            BlockState s = level.getBlockState(p);
            return !shunned(p) && CloneSense.mayBreak(owner, level, p, s) && CloneSense.toolFor(owner, s) != null
                    && CloneSense.safeToBreak(clone, p, keep());
        }

        /** Whether {@code b} holds it up now, or is the floor of where it is going. */
        private boolean standingOnIt(BlockPos b) {
            return CloneSense.underFoot(clone, b) || keep().contains(b);
        }

        /** The blocks it must not break: the floor of its spot, of its next step, and of the path it is on. */
        private Set<BlockPos> keep() {
            Set<BlockPos> keep = new HashSet<>();
            if (spot != null) keep.add(spot.below());
            if (stepTo != null) keep.add(stepTo.below());
            Path path = clone.getNavigation().getPath();
            if (path != null && !path.isDone()) {
                for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) keep.add(path.getNodePos(i).below());
            }
            return keep;
        }

        /**
         * A path to a spot from which it can work {@code t}: in reach, the face in sight, safe to
         * stand on, not on the vein, not another clone's. Null if no such spot can be walked to.
         */
        @Nullable
        private Path findSpot(BlockPos t) {
            Level level = clone.level();
            List<BlockPos> near = new ArrayList<>();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = -5; dy <= 3; dy++) {
                p.set(t.getX() + dx, t.getY() + dy, t.getZ() + dz);
                if (p.equals(t) || vein.contains(p.below()) || vein.contains(p) || vein.contains(p.above())) continue;
                if (CloneSense.reachTo(CloneSense.eyeAt(clone, p), t) > CloneSense.REACH - 0.3) continue;
                if (!CloneSense.open(level, p) || !CloneSense.open(level, p.above()) || !CloneSense.floor(level, p.below())) continue;
                if (heldByAnother(SPOTS, clone, p)) continue;
                near.add(p.immutable());
            }
            near.sort(Comparator.comparingDouble(c -> c.distToCenterSqr(clone.position())));
            Set<BlockPos> spots = new HashSet<>();
            for (BlockPos c : near) {
                if (spots.size() >= 12) break;
                if (CloneSense.canSee(level, clone, CloneSense.eyeAt(clone, c), t) && CloneSense.standable(clone, c)) spots.add(c);
            }
            if (spots.isEmpty()) return null;
            Path path = clone.getNavigation().createPath(spots, 0);
            if (path == null || !path.canReach()) return null;
            spot = path.getTarget();
            SPOTS.put(spot.asLong(), clone);
            return path;
        }

        /** Walks as near as it can get to {@code t}, if that is nearer than it is. */
        private boolean approach(BlockPos t) {
            approaches++;
            Path path = clone.getNavigation().createPath(t, 1);
            if (path == null || path.getEndNode() == null) return false;
            BlockPos end = path.getEndNode().asBlockPos();
            if (end.distSqr(t) + 4 > clone.blockPosition().distSqr(t)) return false;
            clone.getNavigation().moveTo(path, 1.0);
            setPhase(Phase.MOVE);
            return true;
        }

        private void moving() {
            if (target == null) {
                setPhase(Phase.PLAN);
                return;
            }
            // close enough on the way: work it from here
            if (phaseTicks % 4 == 0 && !standingOnIt(target) && CloneSense.canWorkNow(clone, target)) {
                clone.getNavigation().stop();
                beginBreak(target, false);
                return;
            }
            if (clone.getNavigation().isDone() || phaseTicks > 200) {
                clone.getNavigation().stop();
                if (!CloneSense.canWorkNow(clone, target)) fails++;
                releaseSpot();
                setPhase(Phase.PLAN);
                if (fails >= 4) giveUp(target);
            }
        }

        // ── Breaking ────────────────────────────────────────────────

        private void beginBreak(BlockPos b, boolean ownPillar) {
            // stand still to work: no last step of a path carrying it on (onto the block)
            clone.getNavigation().stop();
            clone.getMoveControl().setWantedPosition(clone.getX(), clone.getY(), clone.getZ(), 0);
            breaking = b;
            breakingOwnPillar = ownPillar;
            needed = progress = 0;
            setPhase(Phase.BREAK);
        }

        private void breaking() {
            ServerLevel level = (ServerLevel) clone.level();
            BlockPos b = breaking;
            if (b == null || level.getBlockState(b).isAir()) {
                afterBreak(b);
                return;
            }
            Vec3 c = Vec3.atCenterOf(b);
            clone.getLookControl().setLookAt(c.x, c.y, c.z);
            if (CloneSense.reachTo(clone.getEyePosition(), b) > CloneSense.REACH + 0.5) { // pushed away
                level.destroyBlockProgress(clone.getId(), b, -1);
                breaking = null;
                setPhase(Phase.PLAN);
                return;
            }
            BlockState s = level.getBlockState(b);
            boolean isTarget = b.equals(target);
            if (needed == 0) {
                ItemStack t = isTarget ? CloneWork.toolFor(owner, s) : CloneSense.toolFor(owner, s);
                boolean may = isTarget || breakingOwnPillar || CloneSense.mayBreak(owner, level, b, s);
                if (!breakingOwnPillar && standingOnIt(b)) { // it would fall: work it from another spot
                    level.destroyBlockProgress(clone.getId(), b, -1);
                    breaking = null;
                    setPhase(Phase.PLAN);
                    return;
                }
                if (t == null || !may || !safe(b) || !CloneSense.permitted(owner, level, b, s)) {
                    if (isTarget) giveUp(b);
                    else {
                        shun(b);
                        if (target != null) giveUp(target);
                        else resetMoves();
                    }
                    return;
                }
                tool = t;
                clone.setGathering(true);
                clone.setItemSlot(EquipmentSlot.MAINHAND, tool.copy());
                needed = CloneSense.breakTicks(level, b, s, tool);
            }
            if (progress % 5 == 0) clone.swing(InteractionHand.MAIN_HAND);
            level.destroyBlockProgress(clone.getId(), b, (int) (progress * 10f / needed));
            if (++progress < needed) return;
            if (!breakingOwnPillar && standingOnIt(b)) { // it has come to stand on it: from elsewhere, then
                level.destroyBlockProgress(clone.getId(), b, -1);
                breaking = null;
                setPhase(Phase.PLAN);
                return;
            }
            if (!safe(b)) { // something changed while it worked (lava flowed in)
                level.destroyBlockProgress(clone.getId(), b, -1);
                if (target != null) giveUp(target);
                else resetMoves();
                return;
            }
            List<ItemStack> drops = new ArrayList<>(CloneSense.harvest(level, clone, b, tool));
            if (s.getBlock() instanceof CropBlock crop && level.getBlockState(b.below()).is(Blocks.FARMLAND)) {
                ItemStack seed = crop.getCloneItemStack(level, b, s);
                for (ItemStack d : drops) {
                    if (d.is(seed.getItem())) { // replant from the harvest
                        d.shrink(1);
                        level.setBlock(b, crop.defaultBlockState(), Block.UPDATE_ALL);
                        break;
                    }
                }
            }
            CloneWork.store(clone, drops);
            level.destroyBlockProgress(clone.getId(), b, -1);
            if (breakingOwnPillar) pillar.remove(b);
            afterBreak(b);
        }

        private boolean safe(BlockPos b) {
            if (breakingOwnPillar) { // its own pillar: safe while what is under it holds, and no fluid by it
                return CloneSense.floor(clone.level(), b.below()) && clone.level().getFluidState(b).isEmpty();
            }
            return CloneSense.safeToBreak(clone, b, keep());
        }

        private void afterBreak(@Nullable BlockPos b) {
            if (b != null && b.equals(target)) {
                vein.remove(target);
                CLAIMS.remove(target.asLong(), clone);
                target = null;
            }
            breaking = null;
            breakingOwnPillar = false;
            needed = progress = 0;
            if (!dig.isEmpty()) {
                beginBreak(dig.poll(), false);
                return;
            }
            if (stepTo != null) {
                setPhase(Phase.STEP);
                return;
            }
            setPhase(Phase.PLAN);
        }

        // ── Tunnels, stairs, bridges and pillars ────────────────────

        /**
         * One step of a tunnel toward {@code t}: level, or a stair up or down, two blocks high,
         * bridging with stone from its pack where there is no floor. Every block it would break
         * must be plain ground, safe to break; and the step must hold no fluid. False if no
         * direction will do.
         */
        private boolean digToward(BlockPos t) {
            if (!clone.onGround()) return false;
            BlockPos f = clone.blockPosition();
            int dx = t.getX() - f.getX(), dz = t.getZ() - f.getZ(), dy = t.getY() - f.getY();
            List<Direction> dirs = new ArrayList<>();
            Direction ax = dx >= 0 ? Direction.EAST : Direction.WEST, az = dz >= 0 ? Direction.SOUTH : Direction.NORTH;
            if (Math.abs(dx) >= Math.abs(dz)) {
                if (dx != 0) dirs.add(ax);
                if (dz != 0) dirs.add(az);
            } else {
                if (dz != 0) dirs.add(az);
                if (dx != 0) dirs.add(ax);
            }
            for (Direction d : Direction.Plane.HORIZONTAL) if (!dirs.contains(d)) dirs.add(d); // round, if it must
            int rise = dy >= 2 ? 1 : dy <= -2 ? -1 : 0;
            // the way it wants first; else level, or up or down a step (out of a hole, round a ledge)
            int[] rises = rise == 0 ? new int[]{0, dy > 0 ? 1 : -1, dy > 0 ? -1 : 1} : new int[]{rise, 0, -rise};
            for (Direction d : dirs) {
                for (int r : rises) if (planStep(f, d, r)) return true;
            }
            return false;
        }

        private boolean planStep(BlockPos f, Direction d, int rise) {
            Level level = clone.level();
            BlockPos ahead = f.relative(d);
            BlockPos to = ahead.above(rise);
            List<BlockPos> cells = new ArrayList<>();
            if (rise > 0) cells.add(f.above(2));          // room to jump
            if (rise < 0) cells.add(ahead.above());        // head room going down
            cells.add(to.above());
            cells.add(to);
            BlockPos floor = to.below();
            List<BlockPos> toDig = new ArrayList<>();
            for (BlockPos c : cells) {
                if (CloneSense.open(level, c)) continue;
                if (!level.getFluidState(c).isEmpty() || !diggable(c)) return false;
                toDig.add(c);
            }
            for (Direction n : Direction.values()) { // no lava or water round the step itself
                if (!level.getFluidState(to.relative(n)).isEmpty() || !level.getFluidState(to.above().relative(n)).isEmpty()) return false;
            }
            BlockPos place = null;
            if (!CloneSense.floor(level, floor)) {
                BlockState fs = level.getBlockState(floor);
                if (!fs.canBeReplaced() || !level.getFluidState(floor).isEmpty() || !CloneSense.hasFiller(clone)) return false;
                place = floor; // a bridge over the gap: the block it puts down holds it
            }
            if (toDig.isEmpty() && place == null && !CloneSense.standable(clone, to)) return false;
            dig.clear();
            dig.addAll(toDig);
            stepTo = to;
            floorAt = place;
            if (!dig.isEmpty()) beginBreak(dig.poll(), false);
            else setPhase(Phase.STEP);
            return true;
        }

        private void stepping() {
            if (stepTo == null) {
                setPhase(Phase.PLAN);
                return;
            }
            if (phaseTicks == 1) clone.getNavigation().stop();
            if (floorAt != null) {
                if (!CloneSense.floor(clone.level(), floorAt) && !CloneSense.placeFiller(clone, floorAt)) {
                    if (target != null) giveUp(target);
                    else resetMoves();
                    return;
                }
                floorAt = null;
            }
            Vec3 c = Vec3.atBottomCenterOf(stepTo);
            clone.getMoveControl().setWantedPosition(c.x, c.y, c.z, 1.0);
            clone.getLookControl().setLookAt(c.x, c.y + 1.5, c.z);
            if (clone.blockPosition().equals(stepTo) && clone.onGround()) {
                stepTo = null;
                setPhase(Phase.PLAN);
            } else if (phaseTicks > 60) {
                stepTo = null;
                fails++;
                setPhase(Phase.PLAN);
                if (fails >= 4 && target != null) giveUp(target);
            }
        }

        /**
         * Pillars up under what is overhead and out of reach: jumps, and puts a block from its pack
         * under its feet, as a player would.
         */
        private boolean pillarUp(BlockPos t) {
            BlockPos f = clone.blockPosition();
            if (t.getY() <= f.getY() + 2 || pillars >= 12 || !clone.onGround() || !CloneSense.hasFiller(clone)) return false;
            int dx = t.getX() - f.getX(), dz = t.getZ() - f.getZ();
            if (dx * dx + dz * dz > 9) return false;
            Level level = clone.level();
            if (!level.getFluidState(f).isEmpty()) return false;
            BlockPos head = f.above(2);
            if (!CloneSense.open(level, head)) {
                if (head.equals(t) || vein.contains(head) || !diggable(head)) return false;
                beginBreak(head, false);
                return true;
            }
            pillars++;
            pillarFrom = f;
            clone.getNavigation().stop();
            clone.getJumpControl().jump();
            setPhase(Phase.PILLAR);
            return true;
        }

        private void pillaring() {
            if (pillarFrom == null) {
                setPhase(Phase.PLAN);
                return;
            }
            if (phaseTicks <= 2 && clone.onGround()) clone.getJumpControl().jump();
            if (clone.getY() >= pillarFrom.getY() + 1.0 && clone.level().getBlockState(pillarFrom).canBeReplaced()) {
                if (CloneSense.placeFiller(clone, pillarFrom)) pillar.add(pillarFrom);
                pillarFrom = null;
                setPhase(Phase.PLAN);
            } else if (phaseTicks > 20) {
                pillarFrom = null;
                fails++;
                setPhase(Phase.PLAN);
            }
        }
    }

    // ── Picking up what lies about its work ──────────────────────────────

    /**
     * A working clone picks up what lies loose round its work (saplings and apples from the
     * leaves of a tree it felled, what spilled from its full pack): nothing anyone threw, and
     * nothing by its owner, which is his to pick up.
     */
    static final class CollectGoal extends Goal {

        private final WuCloneEntity clone;
        @Nullable private ItemEntity item;
        private int searchIn, ticks;

        CollectGoal(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (clone.getPriority() != WuCloneEntity.Priority.WORK || (clone.getTarget() != null && clone.getTarget().isAlive())) return false;
            if (CloneWork.nearlyFull(clone.pack()) || --searchIn > 0) return false;
            searchIn = 10;
            ServerPlayer owner = WuCloneManager.resolveOwner(clone);
            if (owner == null) return false;
            List<ItemEntity> loose = clone.level().getEntitiesOfClass(ItemEntity.class, clone.getBoundingBox().inflate(8, 4, 8),
                    e -> e.isAlive() && !e.hasPickUpDelay() && e.getOwner() == null && e.distanceToSqr(owner) > 16
                            && CloneWork.inArea(clone, owner, e.blockPosition()));
            item = loose.stream().min(Comparator.comparingDouble(clone::distanceToSqr)).orElse(null);
            return item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return item != null && item.isAlive() && ticks < 100 && (clone.getTarget() == null || !clone.getTarget().isAlive());
        }

        @Override
        public void start() {
            ticks = 0;
            if (item != null) clone.getNavigation().moveTo(item, 1.0);
        }

        @Override
        public void stop() {
            item = null;
            clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (item == null) return;
            ticks++;
            clone.markBusy();
            if (ticks % 10 == 0) clone.getNavigation().moveTo(item, 1.0);
            if (clone.distanceToSqr(item) < 2.25) {
                ItemStack stack = item.getItem().copy();
                int before = stack.getCount();
                ItemStack left = clone.pack().addItem(stack);
                int taken = before - left.getCount();
                if (taken > 0) {
                    clone.take(item, taken);
                    clone.level().playSound(null, clone.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2f, 1.4f);
                }
                if (left.isEmpty()) item.discard();
                else item.setItem(left);
                item = null;
            }
        }
    }
}
