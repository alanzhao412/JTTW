package com.jttw.engine.mixin;

import com.jttw.engine.transformation.MorphPose;
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

            boolean playerJumpingFlag = false;
            try { playerJumpingFlag = ((LivingEntityJumpingAccessor)(Object)player).jttw_isJumping(); }
            catch (Throwable ignored) {}

            MorphPose.State state = MorphPose.State.of(player, data.canFly(), playerJumpingFlag);
            boolean inWater  = state.inWater();
            boolean onGround = state.onGround();
            boolean isFlying = state.flying();
            boolean heuristicJump = !inWater && !onGround && !isFlying && player.getDeltaMovement().y > 0.02D;

            double px = player.getX(), py = player.getY(), pz = player.getZ();
            dummy.setPos(px, py, pz);
            dummy.xo = player.xo; dummy.yo = player.yo; dummy.zo = player.zo;
            dummy.setDeltaMovement(player.getDeltaMovement());
            dummy.setOnGround(onGround);

            // Copy walk animation state for limb-swing based models (goat, etc.)
            // Excluded: Rabbit (uses jumpTicks bobbing) and Frog (uses AnimationState keyframes)
            if (dummy instanceof LivingEntity dummyLiving && MorphPose.usesStandardLimbSwing(dummy)) {
                MorphPose.copyWalkAnim(player.walkAnimation, dummyLiving.walkAnimation);
            }

            // Copy attack animation state so mob models play their attack anims
            if (dummy instanceof LivingEntity dummyLiving2) {
                MorphPose.copyAttackAnim(player, dummyLiving2);
            }

            MorphPose.apply(player, dummy, state);

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
                if (dummy instanceof LivingEntity dummyLiving && MorphPose.usesStandardLimbSwing(dummy)) {
                    MorphPose.copyWalkAnim(player.walkAnimation, dummyLiving.walkAnimation);
                }

                // Re-copy attack anim after tick() so mob keeps showing the swing
                if (dummy instanceof LivingEntity dummyLiving3) {
                    MorphPose.copyAttackAnim(player, dummyLiving3);
                }

                MorphPose.apply(player, dummy, state);

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
                if (dummy instanceof LivingEntity dummyLiving && MorphPose.usesStandardLimbSwing(dummy)) {
                    MorphPose.copyWalkAnim(player.walkAnimation, dummyLiving.walkAnimation);
                }
                // Copy attack anim every render frame for smooth swing interpolation
                if (dummy instanceof LivingEntity dummyLiving4) {
                    MorphPose.copyAttackAnim(player, dummyLiving4);
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
}
