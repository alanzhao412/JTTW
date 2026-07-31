// FILE: src/main/java/com/jttw/engine/mixin/LivingEntityJumpInvoker.java
package com.jttw.engine.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityJumpInvoker {

    @Invoker("jumpFromGround")
    void jttw_invokeJumpFromGround();
}