package com.jttw.engine.entity.ninespirits;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The Nine-Headed Lion, the Grand Saint of Nine Spirits' true form. Never met in the world: it exists
 * to be the shape he takes (see NineSpiritsAbilityHandler), drawn by its own model.
 */
public class NineHeadedLionEntity extends PathfinderMob {

    public NineHeadedLionEntity(EntityType<? extends NineHeadedLionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
}
