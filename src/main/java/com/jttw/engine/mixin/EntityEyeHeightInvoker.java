package com.jttw.engine.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityEyeHeightInvoker {
    @Invoker("getEyeHeight")
    float jttw_invokeGetEyeHeight(Pose pose, EntityDimensions dims);
}