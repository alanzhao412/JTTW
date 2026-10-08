package com.jttw.engine.transformation;

import com.jttw.engine.mixin.LivingEntityJumpingAccessor;
import com.jttw.engine.mixin.WalkAnimationStateAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;

/**
 * How a transformed player's stand-in entity is posed each frame: which way it faces, how it
 * tilts, whether it swims, jumps or flies, and which animations it borrows from the player.
 *
 * <p>The client renders a transformed player by drawing a "dummy" of the form's entity type in
 * their place ({@code PlayerRendererMixin}); this class holds the rules for that dummy, so they
 * can be tested without a client.
 *
 * <ul>
 *   <li>The dummy faces where the player faces: yaw, body yaw and head yaw are copied, plus a
 *       per-model offset for models built facing backwards ({@link #yawOffset}).</li>
 *   <li>Living dummies tilt with the player's pitch; a few models tilt the other way
 *       ({@link #invertsPitch}). Objects stay level.</li>
 *   <li>In water the dummy swims; in the air or flying it is never on the ground; it jumps when
 *       the player jumps.</li>
 *   <li>Walking and attack animations are the player's, except for models that animate
 *       walking their own way (rabbit hops, frog keyframes).</li>
 * </ul>
 */
public final class MorphPose {

    private MorphPose() {}

    /** Degrees added to the player's yaw so the model faces where the player looks. */
    public static float yawOffset(EntityType<?> type) {
        return type == EntityType.ENDER_DRAGON ? 180.0F : 0.0F;
    }

    /** Models whose pitch runs the other way from the player's. */
    public static boolean invertsPitch(EntityType<?> type) {
        return type == EntityType.PHANTOM || type == EntityType.ENDER_DRAGON;
    }

    /**
     * Whether the model walks with the standard limb swing, borrowed from the player. Rabbits hop
     * on their jump counter and frogs play keyframes, so the player's walk state would break them.
     */
    public static boolean usesStandardLimbSwing(Entity dummy) {
        return !(dummy instanceof Rabbit) && !(dummy instanceof Frog);
    }

    /** What the player is doing, as the dummy needs to know it. */
    public record State(boolean inWater, boolean onGround, boolean flying, boolean airborne, boolean jumping) {

        /**
         * @param canFly      the form grants flight
         * @param jumpingFlag the player's own "jumping" input flag
         */
        public static State of(Player player, boolean canFly, boolean jumpingFlag) {
            boolean inWater = player.isInWaterOrBubble();
            boolean onGround = player.onGround();
            boolean flying = player.getAbilities().flying
                    && (player.getAbilities().instabuild || player.isSpectator() || canFly);
            boolean inAir = !inWater && !onGround;
            boolean risingJump = inAir && !flying && player.getDeltaMovement().y > 0.02D;
            return new State(inWater, onGround, flying, inAir || flying, jumpingFlag || risingJump);
        }
    }

    /** Poses the dummy from the player: facing, tilt, swimming, ground contact and jumping. */
    public static void apply(Player player, Entity dummy, State state) {
        EntityType<?> type = dummy.getType();
        float yawOffset = yawOffset(type);

        dummy.yRotO = player.yRotO + yawOffset;
        dummy.setYRot(player.getYRot() + yawOffset);

        if (dummy instanceof LivingEntity living) {
            if (state.inWater()) {
                living.setOnGround(false);
                living.setSwimming(true);
                living.setPose(Pose.SWIMMING);
            } else {
                living.setSwimming(false);
                if (living.getPose() == Pose.SWIMMING) living.setPose(Pose.STANDING);
                living.setOnGround(state.onGround());
            }
            if (state.airborne()) living.setOnGround(false);

            try { ((LivingEntityJumpingAccessor) living).jttw_setJumping(state.jumping()); }
            catch (Throwable ignored) {}
            if (living instanceof Mob mob) mob.setJumping(state.jumping());

            float x = player.getXRot(), xO = player.xRotO;
            if (invertsPitch(type)) { x = -x; xO = -xO; }
            living.xRotO = xO;
            living.setXRot(x);
            living.yBodyRotO = player.yBodyRotO + yawOffset;
            living.yBodyRot  = player.yBodyRot  + yawOffset;
            living.yHeadRotO = player.yHeadRotO + yawOffset;
            living.yHeadRot  = player.yHeadRot  + yawOffset;

            // The axolotl model sits slightly turned from its body yaw; face it straight ahead
            if (living instanceof Axolotl) {
                living.yBodyRotO = living.yHeadRotO;
                living.yBodyRot  = living.yHeadRot;
            }
        } else {
            dummy.xRotO = 0f;
            dummy.setXRot(0f);
            if (state.airborne()) dummy.setOnGround(false);
        }
    }

    /**
     * The player's swing and hurt animation, so mob models play their own attack and hurt poses
     * (zombie arms, spider lunge, red flash).
     */
    public static void copyAttackAnim(LivingEntity src, LivingEntity dst) {
        dst.swinging     = src.swinging;
        dst.swingTime    = src.swingTime;
        dst.oAttackAnim  = src.oAttackAnim;
        dst.attackAnim   = src.attackAnim;
        dst.hurtTime     = src.hurtTime;
        dst.hurtDuration = src.hurtDuration;
        // Models that raise their arms when aggressive (zombies) do so while swinging
        if (dst instanceof Mob mob) mob.setAggressive(src.swinging);
    }

    /** The player's walk cycle (limb swing position and speed). */
    public static void copyWalkAnim(WalkAnimationState src, WalkAnimationState dst) {
        WalkAnimationStateAccessor s = (WalkAnimationStateAccessor) src;
        WalkAnimationStateAccessor d = (WalkAnimationStateAccessor) dst;
        d.jttw_setPosition(s.jttw_getPosition());
        d.jttw_setSpeed(s.jttw_getSpeed());
        d.jttw_setSpeedOld(s.jttw_getSpeedOld());
    }
}
