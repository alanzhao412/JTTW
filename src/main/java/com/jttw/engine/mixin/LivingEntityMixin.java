package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adjusts travel() for non-phasing abilities if needed in future.
 * noPhysics is no longer set here — phasing is handled by BlockStateBaseMixin
 * and EntityPhaseMixin which make blocks return empty collision shapes.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    // Intentionally empty — kept registered so the refmap entry stays valid.
    // Add hooks here as needed for future LivingEntity behaviour changes.
}