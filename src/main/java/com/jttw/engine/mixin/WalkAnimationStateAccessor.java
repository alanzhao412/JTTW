package com.jttw.engine.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("position") float jttw_getPosition();
    @Accessor("position") void jttw_setPosition(float v);

    @Accessor("speed") float jttw_getSpeed();
    @Accessor("speed") void jttw_setSpeed(float v);

    @Accessor("speedOld") float jttw_getSpeedOld();
    @Accessor("speedOld") void jttw_setSpeedOld(float v);
}