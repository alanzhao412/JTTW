package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    @Unique private Entity           jttw_dummy;
    @Unique private ResourceLocation jttw_dummyId;
    @Unique private long             jttw_lastDummyTickGameTime = Long.MIN_VALUE;

    @Unique private boolean jttw_prevJumpingFlag   = false;
    @Unique private boolean jttw_prevHeuristicJump = false;


    // Rabbit: jumpTicks counts DOWN from jumpDuration to 0.
    // getJumpCompletion(partial) = (jumpTicks - partial) / jumpDuration -> 1.0 to 0.0
    // RabbitModel uses this for BOTH walk bob (continuous short cycles) and actual jump.
    // Dummy has no AI to auto-cycle, so we drive it manually.
    @Unique private int  jttw_rabbitJumpTick      = 0;
    @Unique private int  jttw_rabbitCycleDuration = 0; // current cycle max (walk vs jump)
    @Unique private static final int RABBIT_JUMP_TICKS = 10;
    @Unique private static final int RABBIT_WALK_TICKS = 6;

    // -------------------------------------------------------------------------
    // Main render injection
    // -------------------------------------------------------------------------

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jttw_renderMorph(AbstractClientPlayer player, float unusedYawParam, float partialTicks,
                                  PoseStack poseStack, MultiBufferSource buffer, int light,
                                  CallbackInfo ci) {

        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null) { jttw_resetDummy(); return; }
        // A player form renders as a normal player; AbstractClientPlayerMixin swaps the skin
        if (data.isPlayerForm()) { jttw_resetDummy(); return; }

        EntityType<?> type = data.getEntityType();
        if (type == null) return;

        ResourceLocation id = data.getEntityId();

        if (jttw_dummy == null || jttw_dummyId == null
                || !jttw_dummyId.equals(id)
                || jttw_dummy.level() != player.level()) {

            Entity created = type.create(player.level());
            if (created == null) return;
            if (created instanceof LivingEntity le) le.setSilent(true);

            jttw_dummy   = created;
            jttw_dummyId = id;
            jttw_resetAnimState();
        }

        Entity dummy = jttw_dummy;

        try {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

            boolean inWater  = player.isInWaterOrBubble();
            boolean onGround = player.onGround();
            boolean isFlying = player.getAbilities().flying
                    && (player.getAbilities().instabuild || player.isSpectator() || data.canFly());
            boolean airborne = !inWater && !onGround;

            boolean playerJumpingFlag = false;
            try { playerJumpingFlag = ((LivingEntityJumpingAccessor)(Object)player).jttw_isJumping(); }
            catch (Throwable ignored) {}

            double vy = player.getDeltaMovement().y;
            boolean heuristicJump = airborne && !isFlying && vy > 0.02D;
            boolean shouldBeAirborne = airborne || isFlying;

            double px = player.getX(), py = player.getY(), pz = player.getZ();
            dummy.setPos(px, py, pz);
            dummy.xo = player.xo; dummy.yo = player.yo; dummy.zo = player.zo;
            dummy.setDeltaMovement(player.getDeltaMovement());
            dummy.setOnGround(onGround);

            // Copy walk animation state for limb-swing based models (goat, etc.)
            // Excluded: Rabbit (uses jumpTicks bobbing) and Frog (uses AnimationState keyframes)
            if (dummy instanceof LivingEntity dummyLiving && jttw_usesStandardLimbSwing(dummy)) {
                jttw_copyWalkAnim(player.walkAnimation, dummyLiving.walkAnimation);
            }

            // Copy attack animation state so mob models play their attack anims
            if (dummy instanceof LivingEntity dummyLiving2) {
                jttw_copyAttackAnim(player, dummyLiving2);
            }

            applyStateFromPlayer(player, dummy, inWater, shouldBeAirborne, isFlying, playerJumpingFlag, heuristicJump);

            long gameTime = player.level().getGameTime();
            if (gameTime != jttw_lastDummyTickGameTime) {
                jttw_lastDummyTickGameTime = gameTime;

                boolean flagEdge      = playerJumpingFlag && !jttw_prevJumpingFlag;
                boolean heuristicEdge = heuristicJump && !jttw_prevHeuristicJump;
                boolean jumpEdge      = (flagEdge || heuristicEdge) && !inWater && !isFlying;

                if (dummy instanceof LivingEntity living) {
                    if (jumpEdge) {
                        try {
                            living.setOnGround(true);
                            ((LivingEntityJumpInvoker)(Object)living).jttw_invokeJumpFromGround();
                            living.setOnGround(onGround);
                        } catch (Throwable ignored) {}
                    }

                    // FROG: jumpAnimationState is public — start/stop directly
                    if (dummy instanceof Frog frog) {
                        AnimationState anim = frog.jumpAnimationState;
                        if (jumpEdge) {
                            anim.start(dummy.tickCount);
                        } else if (onGround && anim.isStarted()) {
                            anim.stop();
                        }
                    }
                }

                jttw_prevJumpingFlag   = playerJumpingFlag;
                jttw_prevHeuristicJump = heuristicJump;

                dummy.tickCount = player.tickCount;
                dummy.tick();

                // Stabilize after tick()
                dummy.setPos(px, py, pz);
                dummy.xo = player.xo; dummy.yo = player.yo; dummy.zo = player.zo;
                dummy.setDeltaMovement(player.getDeltaMovement());
                dummy.setOnGround(onGround);

                // Re-copy walk anim after tick() for standard limb-swing mobs
                if (dummy instanceof LivingEntity dummyLiving && jttw_usesStandardLimbSwing(dummy)) {
                    jttw_copyWalkAnim(player.walkAnimation, dummyLiving.walkAnimation);
                }

                // Re-copy attack anim after tick() so mob keeps showing the swing
                if (dummy instanceof LivingEntity dummyLiving3) {
                    jttw_copyAttackAnim(player, dummyLiving3);
                }

                applyStateFromPlayer(player, dummy, inWater, shouldBeAirborne, isFlying, playerJumpingFlag, heuristicJump);

                // RABBIT: set jumpTicks/jumpDuration AFTER tick() so tick()'s own
                // aiStep() decrement can't interfere. We own the counter entirely.
                if (dummy instanceof Rabbit rabbit) {
                    boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
                    if (jumpEdge) {
                        // Real jump — longer arc cycle
                        jttw_rabbitJumpTick      = RABBIT_JUMP_TICKS;
                        jttw_rabbitCycleDuration = RABBIT_JUMP_TICKS;
                    } else if (jttw_rabbitJumpTick == 0 && isMoving && onGround) {
                        // Walking — restart short bob cycle
                        jttw_rabbitJumpTick      = RABBIT_WALK_TICKS;
                        jttw_rabbitCycleDuration = RABBIT_WALK_TICKS;
                    }
                    try {
                        ((RabbitJumpAccessor) rabbit).jttw_setJumpTicks(jttw_rabbitJumpTick);
                        ((RabbitJumpAccessor) rabbit).jttw_setJumpDuration(
                                jttw_rabbitJumpTick > 0 ? jttw_rabbitCycleDuration : 0);
                    } catch (Throwable ignored) {}
                    if (jttw_rabbitJumpTick > 0) jttw_rabbitJumpTick--;
                }

            } else {
                dummy.tickCount = player.tickCount;
                // Still copy walk anim every render frame (between ticks) for smooth lerp
                if (dummy instanceof LivingEntity dummyLiving && jttw_usesStandardLimbSwing(dummy)) {
                    jttw_copyWalkAnim(player.walkAnimation, dummyLiving.walkAnimation);
                }
                // Copy attack anim every render frame for smooth swing interpolation
                if (dummy instanceof LivingEntity dummyLiving4) {
                    jttw_copyAttackAnim(player, dummyLiving4);
                }
            }

            @SuppressWarnings("unchecked")
            EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) dispatcher.getRenderer(dummy);
            if (renderer == null) return;

            float renderYaw = Mth.lerp(partialTicks, dummy.yRotO, dummy.getYRot());
            poseStack.pushPose();
            renderer.render(dummy, renderYaw, partialTicks, poseStack, buffer, light);
            poseStack.popPose();

            ci.cancel();
        } catch (Throwable ignored) {}
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // Mobs whose models use standard WalkAnimationState (limb swing) for walking.
    // Rabbit walks via jumpTicks/jumpDuration bobbing — copying the player's walk state
    // overwrites the dummy's own anim and breaks it. Frog uses AnimationState keyframes.
    // For excluded mobs, let dummy.tick() drive walkAnimation naturally.
    @Unique
    private static boolean jttw_usesStandardLimbSwing(Entity dummy) {
        return !(dummy instanceof Rabbit) && !(dummy instanceof Frog);
    }

    /**
     * Copy the player's attack/swing animation state onto the dummy so mob models
     * play their built-in attack animations (zombie arm-raise, spider lunge, etc.).
     *
     * Fields copied:
     *   swinging        — whether a swing is in progress
     *   swingTime       — current swing tick counter (counts up)
     *   oAttackAnim     — previous-frame interpolation value
     *   attackAnim      — current-frame interpolation value (0..1)
     *   hurtTime        — red flash / knockback anim timer (some models use this)
     *   hurtDuration    — total hurt flash duration
     */
    @Unique
    private static void jttw_copyAttackAnim(LivingEntity src, LivingEntity dst) {
        dst.swinging    = src.swinging;
        dst.swingTime   = src.swingTime;
        dst.oAttackAnim = src.oAttackAnim;
        dst.attackAnim  = src.attackAnim;
        dst.hurtTime    = src.hurtTime;
        dst.hurtDuration = src.hurtDuration;
        // For mobs that check isAggressive() to trigger attack poses (zombies, etc.)
        if (dst instanceof Mob mob) {
            mob.setAggressive(src.swinging);
        }
    }

    @Unique
    private static void jttw_copyWalkAnim(WalkAnimationState src, WalkAnimationState dst) {
        WalkAnimationStateAccessor s = (WalkAnimationStateAccessor) src;
        WalkAnimationStateAccessor d = (WalkAnimationStateAccessor) dst;
        d.jttw_setPosition(s.jttw_getPosition());
        d.jttw_setSpeed(s.jttw_getSpeed());
        d.jttw_setSpeedOld(s.jttw_getSpeedOld());
    }

    @Unique
    private void jttw_resetDummy() {
        jttw_dummy   = null;
        jttw_dummyId = null;
        jttw_lastDummyTickGameTime = Long.MIN_VALUE;
        jttw_resetAnimState();
    }

    @Unique
    private void jttw_resetAnimState() {
        jttw_prevJumpingFlag   = false;
        jttw_prevHeuristicJump = false;
        jttw_rabbitJumpTick      = 0;
        jttw_rabbitCycleDuration = 0;
    }

    @Unique
    private static float jttw_yawOffset(Entity dummy) {
        return dummy.getType() == EntityType.ENDER_DRAGON ? 180.0F : 0.0F;
    }

    @Unique
    private static boolean jttw_invertPitch(Entity dummy) {
        EntityType<?> t = dummy.getType();
        return t == EntityType.PHANTOM || t == EntityType.ENDER_DRAGON;
    }

    @Unique
    private static void applyStateFromPlayer(AbstractClientPlayer player, Entity dummy,
                                             boolean inWater, boolean shouldBeAirborne,
                                             boolean isFlying, boolean playerJumpingFlag,
                                             boolean heuristicJump) {
        float yawOffset     = jttw_yawOffset(dummy);
        boolean invertPitch = jttw_invertPitch(dummy);

        dummy.yRotO = player.yRotO + yawOffset;
        dummy.setYRot(player.getYRot() + yawOffset);

        if (dummy instanceof LivingEntity living) {
            if (inWater) {
                living.setOnGround(false);
                living.setSwimming(true);
                living.setPose(Pose.SWIMMING);
            } else {
                living.setSwimming(false);
                if (living.getPose() == Pose.SWIMMING) living.setPose(Pose.STANDING);
                living.setOnGround(player.onGround());
            }
            if (shouldBeAirborne) living.setOnGround(false);

            boolean jumpAnim = playerJumpingFlag || heuristicJump;
            try { ((LivingEntityJumpingAccessor)(Object)living).jttw_setJumping(jumpAnim); }
            catch (Throwable ignored) {}
            if (living instanceof Mob mob) mob.setJumping(jumpAnim);

            float x = player.getXRot(), xO = player.xRotO;
            if (invertPitch) { x = -x; xO = -xO; }
            living.xRotO = xO;
            living.setXRot(x);
            living.yBodyRotO = player.yBodyRotO + yawOffset;
            living.yBodyRot  = player.yBodyRot  + yawOffset;
            living.yHeadRotO = player.yHeadRotO + yawOffset;
            living.yHeadRot  = player.yHeadRot  + yawOffset;

            // Axolotl model has a slight body rotation offset; force body yaw
            // to match head yaw exactly so the model faces straight ahead.
            if (living instanceof Axolotl) {
                living.yBodyRotO = living.yHeadRotO;
                living.yBodyRot  = living.yHeadRot;
            }
        } else {
            dummy.xRotO = 0f;
            dummy.setXRot(0f);
            if (shouldBeAirborne || isFlying) dummy.setOnGround(false);
        }
    }
}