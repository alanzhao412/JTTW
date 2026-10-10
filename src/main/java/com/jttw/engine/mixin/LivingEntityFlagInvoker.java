package com.jttw.engine.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Sets a living entity's synced flag: 4 is the riptide spin, its pose and swirl seen by all. */
@Mixin(LivingEntity.class)
public interface LivingEntityFlagInvoker {

    @Invoker("setLivingEntityFlag")
    void jttw_invokeSetLivingEntityFlag(int flag, boolean value);
}
