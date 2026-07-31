package com.jttw.engine.mixin;

import net.minecraft.world.entity.animal.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Rabbit.class)
public interface RabbitJumpAccessor {
    @Accessor("jumpTicks")    int  jttw_getJumpTicks();
    @Accessor("jumpTicks")    void jttw_setJumpTicks(int v);
    @Accessor("jumpDuration") int  jttw_getJumpDuration();
    @Accessor("jumpDuration") void jttw_setJumpDuration(int v);
}