// FILE: src/main/java/com/jttw/engine/mixin/LivingEntityJumpingAccessor.java
package com.jttw.engine.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityJumpingAccessor {

    @Accessor("jumping")
    boolean jttw_isJumping();

    @Accessor("jumping")
    void jttw_setJumping(boolean jumping);
}