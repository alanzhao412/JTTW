package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the "push entity out of blocks" logic for phasing players.
 *
 * Without this, even if collision shape returns empty, Entity.pushOutOfBlocks()
 * still detects overlap and teleports the entity to the nearest clear space.
 * Palladium's EntityMixin cancels the same method (moveTowardsClosestSpace /
 * pushOutOfBlocks) when IntangibilityAbility is active.
 */
@Mixin(Entity.class)
public class EntityPhaseMixin {

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void jttw_cancelPushOutOfBlocks(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof LivingEntity)) return;

        TransformationData data = TransformationManager.getTransformation((LivingEntity) self);
        if (data != null && data.canPhase()) {
            ci.cancel();
        }
    }
}