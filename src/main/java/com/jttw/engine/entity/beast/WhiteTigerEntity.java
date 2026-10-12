package com.jttw.engine.entity.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The White Tiger, Byakko's true form. Never met in the world: it exists to be the shape Byakko
 * takes (see character.divinebeast.TrueForm), drawn by its own model.
 */
public class WhiteTigerEntity extends PathfinderMob {

    public WhiteTigerEntity(EntityType<? extends WhiteTigerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
    }
}
