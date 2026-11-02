package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A telegraphed blow on all in an area, as Heaven's minibosses and great names strike: a wind-up of
 * {@link Spec#windUp} ticks while the ground it will fall on is marked (and its striker holds still,
 * posed), so one can step out of it; then it falls on every foe of its striker
 * ({@link HeavenFolk#foesWithin}) still in it, {@link Spec#share} of a same-level character's health as
 * its striker's rank lands it ({@link HeavenFolk#hitOf}), with whatever else it does ({@code effect}).
 *
 * <p>Its shapes: {@link Shape#AROUND} a ring round the striker; {@link Shape#AT_FOES} a ring at the feet
 * of each foe near as it began (stars, thunderbolts, the pagoda), struck where it was marked;
 * {@link Shape#CONE} a cone before it toward its foe as it began; {@link Shape#LINE} a straight line
 * toward its foe as it began.
 */
public final class AreaBlow {

    public enum Shape { AROUND, AT_FOES, CONE, LINE }

    /**
     * What it is: its shape; how far it reaches (a ring's radius, a cone's or a line's length, how far a
     * mark may be laid); its width (a mark's radius, a cone's half-angle in degrees, a line's half-width);
     * how long its wind-up and how often; how hard (a regular's share, as {@code hitOf} takes it); the
     * particle that marks it, and its sounds as it winds up and as it falls.
     */
    public record Spec(Shape shape, double reach, double width, int windUp, int every, double share,
                       ParticleOptions mark, SoundEvent windSound, SoundEvent strikeSound) {}

    /** The most foes an {@link Shape#AT_FOES} blow marks. */
    public static final int MOST_MARKS = 4;

    private final Mob owner;
    private final HeavenFolk folk;
    private final Spec spec;
    private final BiConsumer<LivingEntity, Vec3> effect;
    private int cooldown, left = -1;
    private Vec3 aim = Vec3.ZERO;
    private final List<Vec3> marks = new ArrayList<>();
    @Nullable
    private java.util.function.Consumer<List<Vec3>> after;

    public <T extends Mob & HeavenFolk> AreaBlow(T owner, Spec spec, BiConsumer<LivingEntity, Vec3> effect) {
        this.owner = owner;
        this.folk = owner;
        this.spec = spec;
        this.effect = effect;
        this.cooldown = spec.every() / 2;
    }

    public Spec spec() {
        return spec;
    }

    /** Also, as it falls, {@code then} (given where it was marked: an AT_FOES blow's marks, else the striker's place). */
    public AreaBlow then(java.util.function.Consumer<List<Vec3>> then) {
        this.after = then;
        return this;
    }

    /** Whether it is winding up (its striker holds still meanwhile). */
    public boolean winding() {
        return left >= 0;
    }

    /** Ticks left of its wind-up, or -1. */
    public int left() {
        return left;
    }

    /** Where an {@link Shape#AT_FOES} blow will fall. */
    public List<Vec3> marks() {
        return List.copyOf(marks);
    }

    /** Whether {@code target} is near enough to begin it on. */
    public boolean inReach(LivingEntity target) {
        double r = switch (spec.shape()) {
            case AROUND -> spec.reach() + 1;
            default -> spec.reach();
        };
        return owner.distanceTo(target) <= r && owner.hasLineOfSight(target);
    }

    /**
     * One tick: winding up, the wind-up's marks shown and, at its end, the blow struck; else, its time
     * come and {@code target} in reach, begun. Returns whether it is winding up (its striker should hold still).
     */
    public boolean tick(@Nullable LivingEntity target) {
        if (left >= 0) {
            owner.getNavigation().stop();
            if (target != null && spec.shape() != Shape.AT_FOES) owner.getLookControl().setLookAt(target, 30f, 30f);
            show();
            if (left-- <= 0) {
                left = -1;
                folk.setWinding(0);
                strike();
            } else folk.setWinding(left + 1);
            return true;
        }
        if (cooldown > 0) cooldown--;
        if (cooldown == 0 && target != null && target.isAlive() && inReach(target)) begin(target);
        return left >= 0;
    }

    /** Begins it on {@code target}: marks where it will fall and winds up. Returns whether it began. */
    public boolean begin(@Nullable LivingEntity target) {
        if (left >= 0) return false;
        marks.clear();
        Vec3 to = target != null ? target.position().subtract(owner.position()) : owner.getLookAngle();
        to = new Vec3(to.x, 0, to.z);
        aim = to.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, owner.getYRot()) : to.normalize();
        if (spec.shape() == Shape.AT_FOES) {
            for (LivingEntity e : folk.foesWithin(spec.reach())) {
                if (marks.size() >= MOST_MARKS) break;
                marks.add(e.position());
            }
            if (marks.isEmpty() && target != null) marks.add(target.position());
            if (marks.isEmpty()) return false;
        }
        left = spec.windUp();
        cooldown = spec.every();
        folk.setWinding(left + 1);
        owner.getNavigation().stop();
        owner.playSound(spec.windSound(), 1.6f, 0.9f);
        return true;
    }

    /** Strikes it now: everyone of its striker's foes in its area. Returns who. */
    public List<LivingEntity> strike() {
        List<LivingEntity> caught = caught();
        float hit = folk.hitOf(spec.share());
        for (LivingEntity e : caught) {
            e.invulnerableTime = 0;
            if (!e.hurt(owner.damageSources().mobAttack(owner), hit)) continue;
            owner.setLastHurtMob(e);
            effect.accept(e, owner.position());
        }
        owner.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        owner.playSound(spec.strikeSound(), 2f, 0.8f);
        if (owner.level() instanceof ServerLevel server) burst(server);
        if (after != null) after.accept(spec.shape() == Shape.AT_FOES ? List.copyOf(marks) : List.of(owner.position()));
        marks.clear();
        return caught;
    }

    /** Those of its striker's foes now in its area. */
    public List<LivingEntity> caught() {
        List<LivingEntity> out = new ArrayList<>();
        double far = spec.shape() == Shape.AT_FOES ? spec.reach() + spec.width() + 4 : spec.reach() + 2;
        for (LivingEntity e : folk.foesWithin(far)) {
            if (inArea(e)) out.add(e);
        }
        return out;
    }

    /** Whether {@code e} stands in its area now. */
    public boolean inArea(LivingEntity e) {
        Vec3 from = owner.position(), at = e.position();
        Vec3 flat = new Vec3(at.x - from.x, 0, at.z - from.z);
        double reachBody = e.getBbWidth() / 2;
        return switch (spec.shape()) {
            case AROUND -> flat.length() <= spec.reach() + reachBody && Math.abs(at.y - from.y) <= 4;
            case AT_FOES -> marks.stream().anyMatch(m -> Math.hypot(at.x - m.x, at.z - m.z) <= spec.width() + reachBody && Math.abs(at.y - m.y) <= 3);
            case CONE -> {
                double d = flat.length();
                if (d > spec.reach() + reachBody || Math.abs(at.y - from.y) > 4) yield false;
                if (d < 1) yield true;
                double cos = flat.normalize().dot(aim);
                yield cos >= Math.cos(Math.toRadians(spec.width()));
            }
            case LINE -> {
                double along = flat.dot(aim);
                if (along < -0.5 || along > spec.reach() + reachBody || Math.abs(at.y - from.y) > 4) yield false;
                yield flat.subtract(aim.scale(along)).length() <= spec.width() + reachBody;
            }
        };
    }

    /** The wind-up's marks: the ground it will fall on outlined. */
    private void show() {
        if (!(owner.level() instanceof ServerLevel server) || left % 2 != 0) return;
        double y = owner.getY() + 0.15;
        switch (spec.shape()) {
            case AROUND -> ring(server, owner.getX(), y, owner.getZ(), spec.reach(), 28);
            case AT_FOES -> {
                for (Vec3 m : marks) ring(server, m.x, m.y + 0.15, m.z, spec.width(), 14);
            }
            case CONE -> {
                double half = Math.toRadians(spec.width()), base = Math.atan2(aim.z, aim.x);
                for (int i = 0; i <= 10; i++) {
                    double a = base - half + 2 * half * i / 10;
                    server.sendParticles(spec.mark(), owner.getX() + Math.cos(a) * spec.reach(), y, owner.getZ() + Math.sin(a) * spec.reach(), 1, 0, 0, 0, 0);
                }
                for (int i = 1; i <= 6; i++) {
                    double r = spec.reach() * i / 6;
                    for (double a : new double[]{base - half, base + half}) {
                        server.sendParticles(spec.mark(), owner.getX() + Math.cos(a) * r, y, owner.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    }
                }
            }
            case LINE -> {
                Vec3 side = new Vec3(-aim.z, 0, aim.x).scale(spec.width());
                for (int i = 0; i <= 12; i++) {
                    Vec3 p = owner.position().add(aim.scale(spec.reach() * i / 12));
                    server.sendParticles(spec.mark(), p.x + side.x, y, p.z + side.z, 1, 0, 0, 0, 0);
                    server.sendParticles(spec.mark(), p.x - side.x, y, p.z - side.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private void ring(ServerLevel server, double x, double y, double z, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + left * 0.05;
            server.sendParticles(spec.mark(), x + Math.cos(a) * r, y, z + Math.sin(a) * r, 1, 0, 0.02, 0, 0);
        }
    }

    /** As it falls: a burst over its area. */
    private void burst(ServerLevel server) {
        switch (spec.shape()) {
            case AROUND -> {
                for (int i = 0; i < 40; i++) {
                    double a = i * Math.PI * 2 / 40;
                    server.sendParticles(spec.mark(), owner.getX() + Math.cos(a) * 1.2, owner.getY(0.4), owner.getZ() + Math.sin(a) * 1.2,
                            0, Math.cos(a), 0.05, Math.sin(a), spec.reach() * 0.08);
                }
            }
            case AT_FOES -> {
                for (Vec3 m : marks) server.sendParticles(spec.mark(), m.x, m.y + 0.5, m.z, 24, spec.width() / 2, 0.6, spec.width() / 2, 0.05);
            }
            case CONE, LINE -> {
                for (int i = 1; i <= 10; i++) {
                    Vec3 p = owner.position().add(aim.scale(spec.reach() * i / 10)).add(0, 1, 0);
                    server.sendParticles(spec.mark(), p.x, p.y, p.z, 4, 0.4, 0.4, 0.4, 0.02);
                }
            }
        }
    }

    /** For tests and scenes: ready at once. */
    public void ready() {
        cooldown = 0;
    }
}
