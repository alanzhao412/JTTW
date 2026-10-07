package com.jttw.engine.transformation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraftforge.common.Tags;

import java.util.Set;

/**
 * Sorts an entity into a {@link FormCategory}: player, object, boss, or a mob by temperament
 * (hostile or not) and habitat (land, water, air). Works the same on client and server, so
 * the menu shows exactly what the server will grant.
 */
public final class FormClassifier {

    private FormClassifier() {}

    /** Bosses the Forge boss tag doesn't list. */
    private static final Set<EntityType<?>> EXTRA_BOSSES = Set.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.ELDER_GUARDIAN, EntityType.WARDEN);

    /**
     * @param type     the form's entity type
     * @param instance a created instance of it (null when one can't be created, e.g. players)
     */
    public static FormCategory classify(EntityType<?> type, Entity instance) {
        if (type == EntityType.PLAYER) return FormCategory.PLAYER;
        if (!(instance instanceof LivingEntity living)) return FormCategory.OBJECT;
        if (EXTRA_BOSSES.contains(type) || type.is(Tags.EntityTypes.BOSSES)) return FormCategory.BOSS;

        boolean hostile = living instanceof Enemy;
        if (isFlyer(living)) return hostile ? FormCategory.HOSTILE_AIR : FormCategory.NEUTRAL_AIR;
        if (isAquatic(living)) return hostile ? FormCategory.HOSTILE_WATER : FormCategory.NEUTRAL_WATER;
        return hostile ? FormCategory.HOSTILE_LAND : FormCategory.NEUTRAL_LAND;
    }

    /**
     * Mobs that fly. Also decides whether a form grants flight ({@link TransformationData}),
     * so a sky form always flies.
     */
    public static boolean isFlyer(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.FlyingMob) return true;
        if (entity instanceof net.minecraft.world.entity.animal.FlyingAnimal) return true;
        EntityType<?> t = entity.getType();
        return t == EntityType.ENDER_DRAGON
                || t == EntityType.GHAST
                || t == EntityType.PHANTOM
                || t == EntityType.BAT
                || t == EntityType.BEE
                || t == EntityType.BLAZE
                || t == EntityType.PARROT
                || t == EntityType.VEX
                || t == EntityType.ALLAY;
    }

    /** Mobs that live in water. Amphibians that walk on land (turtles, frogs) count as land. */
    public static boolean isAquatic(Entity entity) {
        if (entity instanceof WaterAnimal || entity instanceof Guardian || entity instanceof Drowned) return true;
        MobCategory c = entity.getType().getCategory();
        return c == MobCategory.WATER_CREATURE || c == MobCategory.WATER_AMBIENT
                || c == MobCategory.UNDERGROUND_WATER_CREATURE || c == MobCategory.AXOLOTLS;
    }
}
