package com.jttw.engine.entity.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A divine beast's true form (the Azure Dragon, the Black Tortoise, the Vermilion Bird). Never met
 * in the world: it exists to be the shape a divine beast takes (see character.divinebeast.TrueForm),
 * drawn by its own model, which reaches well past its body: it is drawn while any of it is in view.
 */
public class TrueFormEntity extends PathfinderMob {

    public TrueFormEntity(EntityType<? extends TrueFormEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(8);
    }

    /** A true form that flies (the Azure Dragon, the Vermilion Bird): its taker flies in it. */
    public static class Flying extends TrueFormEntity implements FlyingAnimal {

        public Flying(EntityType<? extends Flying> type, Level level) {
            super(type, level);
        }

        @Override
        public boolean isFlying() {
            return !onGround();
        }
    }
}
