package com.jttw.engine.mixin;

import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An explosion's power (its radius), which the game keeps private. */
@Mixin(Explosion.class)
public interface ExplosionAccessor {

    @Accessor("radius")
    float jttw_radius();
}
