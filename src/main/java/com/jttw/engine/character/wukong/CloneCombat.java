package com.jttw.engine.character.wukong;

import com.jttw.engine.entity.WuCloneEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * How a shadow clone fights, at its owner's side.
 *
 * <ul>
 *   <li><b>Whom.</b> What attacks him first, then what he attacks (even another player, while he
 *       fights him), then what attacks the clone, then any hostile creature near him (or near its
 *       post). Never him, his clones, his pets and summons, those allied to him, other players or
 *       their pets unless he is fighting them, and no neutral creature that has not turned on
 *       them.</li>
 *   <li><b>Spread.</b> Each enemy already fought by one of his clones weighs against it, so with
 *       several enemies the clones split between them; and those on one enemy come at it from
 *       evenly spread sides (flanking it), not in a queue.</li>
 *   <li><b>Retreat.</b> Badly hurt with no Ring of Fire to light, it falls back to him; out of the
 *       fight it heals as a player does, and goes back in when it is fit again, or fights where it
 *       stands if cornered.</li>
 *   <li><b>Leash.</b> It does not chase far from him (or its post).</li>
 * </ul>
 */
public final class CloneCombat {

    private CloneCombat() {}

    /** How far round him (or its post) it takes on hostile creatures by itself. */
    public static final double GUARD_RANGE = 12;
    /** How far it chases before letting go. */
    public static final double LEASH = 24;
    /** How long a blow given or taken counts as a fight going on. */
    public static final int FIGHT_MEMORY = 20 * 10;
    /** Below this share of health it falls back (if it has no ring to light), and goes back in above this. */
    public static final float RETREAT_AT = 0.3f, RETURN_AT = 0.6f;

    // ── Whom it fights ───────────────────────────────────────────────────

    /** Whether {@code e} belongs to {@code ownerId}: a tamed pet, a clone, a summon that names him. */
    static boolean ownedBy(Entity e, UUID ownerId) {
        if (e instanceof OwnableEntity o && ownerId.equals(o.getOwnerUUID())) return true;
        if (WuCloneManager.isAllyClone(e, ownerId)) return true;
        if (e instanceof WuCloneEntity c && c.getOwnerUUID().map(ownerId::equals).orElse(false)) return true;
        CompoundTag pd = e.getPersistentData();
        for (String key : pd.getAllKeys()) {
            if (pd.hasUUID(key) && ownerId.equals(pd.getUUID(key))) return true;
        }
        return false;
    }

    /** The player {@code e} belongs to (a pet, a summon), or null. */
    @Nullable
    static UUID keeperOf(Entity e) {
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return o.getOwnerUUID();
        if (e instanceof WuCloneEntity c) return c.getOwnerUUID().orElse(null);
        return null;
    }

    /** Whether he is fighting {@code e} now: he struck it, or it struck him, lately. */
    static boolean fighting(ServerPlayer owner, LivingEntity e) {
        int now = owner.tickCount;
        if (owner.getLastHurtMob() == e && now - owner.getLastHurtMobTimestamp() < FIGHT_MEMORY) return true;
        if (owner.getLastHurtByMob() == e && now - owner.getLastHurtByMobTimestamp() < FIGHT_MEMORY) return true;
        return e instanceof Mob m && m.getTarget() == owner;
    }

    /** Whether {@code e} is after him or the clone. */
    private static boolean threatens(WuCloneEntity clone, ServerPlayer owner, LivingEntity e) {
        if (e instanceof Mob m && (m.getTarget() == owner || m.getTarget() == clone)) return true;
        return clone.getLastHurtByMob() == e && clone.tickCount - clone.getLastHurtByMobTimestamp() < FIGHT_MEMORY;
    }

    /** Whether a clone of {@code owner} may fight {@code e} at all. */
    public static boolean mayFight(WuCloneEntity clone, ServerPlayer owner, LivingEntity e) {
        if (!e.isAlive() || e == owner || e == clone || e.isSpectator()) return false;
        if (e.level() != clone.level() || e.isInvulnerable()) return false;
        UUID ownerId = owner.getUUID();
        if (ownedBy(e, ownerId) || e.isAlliedTo(owner) || owner.isAlliedTo(e)) return false;
        if (e instanceof Player p) return !p.isCreative() && owner.canHarmPlayer(p) && fighting(owner, p);
        UUID keeper = keeperOf(e);
        if (keeper != null) return fighting(owner, e) || threatens(clone, owner, e); // someone's pet: only if it is in the fight
        return true;
    }

    /** Whether it takes {@code e} on of its own accord: a hostile creature, a neutral one only once angry at them. */
    static boolean hostile(WuCloneEntity clone, ServerPlayer owner, LivingEntity e) {
        if (e instanceof NeutralMob n) return n.isAngryAt(owner) || n.isAngryAt(clone) || threatens(clone, owner, e);
        return e instanceof Enemy;
    }

    /** Where it fights round: him, while it follows him; its post or work site otherwise. */
    static Vec3 anchor(WuCloneEntity clone, ServerPlayer owner) {
        var site = clone.workSite();
        return site != null ? Vec3.atBottomCenterOf(site) : owner.position();
    }

    /**
     * How many of his other clones are fighting {@code e}. On its own enemy only those that came
     * first (lower ids) count, so of two clones on one enemy the later one moves on and the
     * first stays: they never swap back and forth.
     */
    static int othersOn(WuCloneEntity clone, ServerPlayer owner, LivingEntity e) {
        boolean mine = clone.getTarget() == e;
        int n = 0;
        for (WuCloneEntity c : WuCloneManager.clonesOf(owner)) {
            if (c != clone && c.getTarget() == e && (!mine || c.getId() < clone.getId())) n++;
        }
        return n;
    }

    /**
     * Its best enemy now, or null: lower is better. Distance, less for what is after him, what he
     * fights, and what is after the clone; more for each fellow already on it.
     */
    @Nullable
    static LivingEntity choose(WuCloneEntity clone, ServerPlayer owner) {
        Vec3 anchor = anchor(clone, owner);
        AABB box = new AABB(anchor, anchor).inflate(GUARD_RANGE, 6, GUARD_RANGE);
        List<LivingEntity> near = clone.level().getEntitiesOfClass(LivingEntity.class, box.minmax(clone.getBoundingBox().inflate(8)),
                e -> mayFight(clone, owner, e));
        LivingEntity hisFoe = owner.getLastHurtByMob(), hisTarget = owner.getLastHurtMob(), mine = clone.getLastHurtByMob();
        LivingEntity now = clone.getTarget();
        if (now != null && !near.contains(now) && mayFight(clone, owner, now)) near.add(now);
        for (LivingEntity extra : new LivingEntity[]{hisFoe, hisTarget, mine}) {
            if (extra != null && !near.contains(extra) && extra.distanceToSqr(owner) < LEASH * LEASH && mayFight(clone, owner, extra)) near.add(extra);
        }
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : near) {
            boolean fights = fighting(owner, e), threat = threatens(clone, owner, e);
            if (!fights && !threat && !hostile(clone, owner, e)) continue;
            if (!clone.hasLineOfSight(e) && !fights && !threat) continue; // not one it cannot see, unless in the fight
            double score = score(clone, owner, e);
            if (score < bestScore) {
                best = e;
                bestScore = score;
            }
        }
        return best;
    }

    static double score(WuCloneEntity clone, ServerPlayer owner, LivingEntity e) {
        double score = Math.sqrt(clone.distanceToSqr(e)) * 0.5 + othersOn(clone, owner, e) * 7;
        if (e instanceof Mob m && m.getTarget() == owner) score -= 10;
        if (fighting(owner, e)) score -= owner.getLastHurtByMob() == e ? 10 : 8;
        if (threatens(clone, owner, e)) score -= 4;
        return score;
    }

    /**
     * Its choice of enemy, looked at again every half second: a better one (by its weighing, with
     * some stickiness so it does not dither) takes the place of the one it has; one it may no
     * longer fight, or that has drawn it too far, is let go.
     */
    static final class TargetChoice extends TargetGoal {

        private final WuCloneEntity clone;
        private int lookIn;

        TargetChoice(WuCloneEntity clone) {
            super(clone, false);
            this.clone = clone;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (--lookIn > 0) return false;
            lookIn = 5; // every other tick it is asked: about half a second
            ServerPlayer owner = WuCloneManager.resolveOwner(clone);
            if (owner == null) return false;
            LivingEntity now = clone.getTarget();
            if (now != null && !keep(owner, now)) {
                clone.setTarget(null);
                now = null;
            }
            if (clone.isRetreating()) return false;
            LivingEntity best = choose(clone, owner);
            if (best == null || best == now) return false;
            if (now != null && score(clone, owner, best) > score(clone, owner, now) - 4) return false;
            clone.setTarget(best);
            return false; // the choice is made; nothing to run
        }

        private boolean keep(ServerPlayer owner, LivingEntity e) {
            if (!mayFight(clone, owner, e)) return false;
            return e.position().distanceToSqr(anchor(clone, owner)) < LEASH * LEASH;
        }
    }

    // ── How it fights ────────────────────────────────────────────────────

    /** Ticks between its blows: as its owner's attack speed allows (a sword's 0.6 s), at least a quarter second. */
    static int cooldown(ServerPlayer owner) {
        double speed = owner.getAttributeValue(Attributes.ATTACK_SPEED);
        return Math.max(5, (int) Math.round(20 / Math.max(0.5, speed)));
    }

    /** Its reach against a creature: a player's, from its body to the creature's. */
    static final double MELEE_REACH = 3.0;

    /**
     * Closing and striking: it comes at its enemy from its own side of it (the clones on one
     * enemy spread round it evenly, starting from his side), and strikes when in reach, as fast
     * as his weapon allows.
     */
    static final class Fight extends Goal {

        private final WuCloneEntity clone;
        private int cooldown, repathIn;
        private Vec3 lastGoal = Vec3.ZERO;

        Fight(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = clone.getTarget();
            return t != null && t.isAlive() && !clone.isRetreating();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            repathIn = 0;
            clone.setAggressive(true);
        }

        @Override
        public void stop() {
            clone.setAggressive(false);
            clone.getNavigation().stop();
        }

        /** Where round {@code t} it should stand: its share of a ring round it, from his side. */
        private Vec3 flank(ServerPlayer owner, LivingEntity t) {
            List<WuCloneEntity> on = WuCloneManager.clonesOf(owner).stream().filter(c -> c.getTarget() == t).toList();
            int n = Math.max(1, on.size()), i = Math.max(0, on.indexOf(clone));
            double base = Math.atan2(owner.getZ() - t.getZ(), owner.getX() - t.getX());
            double a = base + (n == 1 ? 0 : (Math.PI * 2 / n) * i + Math.PI / n);
            if (n == 1) a = Math.atan2(clone.getZ() - t.getZ(), clone.getX() - t.getX()); // alone: straight in
            double r = t.getBbWidth() / 2 + 1.4;
            return new Vec3(t.getX() + Math.cos(a) * r, t.getY(), t.getZ() + Math.sin(a) * r);
        }

        @Override
        public void tick() {
            LivingEntity t = clone.getTarget();
            ServerPlayer owner = WuCloneManager.resolveOwner(clone);
            if (t == null || owner == null) return;
            clone.markBusy();
            clone.getLookControl().setLookAt(t, 30f, 30f);
            if (cooldown > 0) cooldown--;
            double reach = Math.sqrt(t.getBoundingBox().distanceToSqr(clone.getEyePosition()));
            if (--repathIn <= 0) {
                repathIn = 8 + clone.getRandom().nextInt(4);
                Vec3 goal = reach > 6 ? t.position() : flank(owner, t);
                if (goal.distanceToSqr(lastGoal) > 1 || clone.getNavigation().isDone()) {
                    lastGoal = goal;
                    double speed = reach > 6 ? 1.25 : 1.05;
                    if (!clone.getNavigation().moveTo(goal.x, goal.y, goal.z, speed)) clone.getNavigation().moveTo(t, speed);
                }
            }
            if (reach <= MELEE_REACH && cooldown == 0 && clone.hasLineOfSight(t)) {
                clone.swing(InteractionHand.MAIN_HAND);
                clone.doHurtTarget(t);
                cooldown = cooldown(owner);
            }
        }
    }

    /**
     * Falling back: badly hurt and with no ring to light, it goes to the far side of him from
     * its enemy until it has healed (see {@link WuCloneEntity}); cornered (its enemy on it and
     * nowhere to go), it fights.
     */
    static final class Retreat extends Goal {

        private final WuCloneEntity clone;
        private int ticks;

        Retreat(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return clone.isRetreating();
        }

        @Override
        public void start() {
            ticks = 0;
            clone.setTarget(null);
        }

        @Override
        public void stop() {
            clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer owner = WuCloneManager.resolveOwner(clone);
            if (owner == null) return;
            ticks++;
            LivingEntity foe = clone.getLastHurtByMob();
            Vec3 from = foe != null && foe.isAlive() ? foe.position() : clone.position();
            Vec3 away = owner.position().subtract(from);
            if (away.lengthSqr() < 1e-3) away = new Vec3(1, 0, 0);
            Vec3 to = owner.position().add(away.normalize().scale(3));
            if (ticks % 10 == 1) clone.getNavigation().moveTo(to.x, to.y, to.z, 1.3);
            // cornered: it fights back where it stands
            if (foe != null && foe.isAlive() && clone.distanceToSqr(foe) < 4 && clone.getNavigation().isDone()
                    && mayFight(clone, owner, foe)) {
                clone.stopRetreating();
                clone.setTarget(foe);
            }
        }
    }
}
