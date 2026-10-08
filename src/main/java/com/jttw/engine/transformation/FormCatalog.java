package com.jttw.engine.transformation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every form the transformation menus offer, sorted the way they show them. The mob menu, the
 * object picker and the tests all read this, so they agree on what a form is.
 *
 * <ul>
 *   <li><b>Mob forms</b>: every living entity type except the player, by {@link FormCategory}.</li>
 *   <li><b>Object forms</b>: every other entity type (arrows, minecarts, boats...).</li>
 * </ul>
 * Player forms are other online players and have their own picker.
 */
public final class FormCatalog {

    private FormCatalog() {}

    /** Mob forms by category, each list sorted by id. */
    public static Map<FormCategory, List<ResourceLocation>> mobForms(Level level) {
        Map<FormCategory, List<ResourceLocation>> byCategory = new EnumMap<>(FormCategory.class);
        for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);
            if (type == null || type == EntityType.PLAYER) continue;
            Entity e = create(type, level);
            if (!(e instanceof LivingEntity)) continue;
            byCategory.computeIfAbsent(FormClassifier.classify(type, e), k -> new ArrayList<>()).add(key);
        }
        byCategory.values().forEach(l -> l.sort(Comparator.comparing(ResourceLocation::toString)));
        return byCategory;
    }

    /** Object forms, sorted by id. */
    public static List<ResourceLocation> objectForms(Level level) {
        List<ResourceLocation> all = new ArrayList<>();
        for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);
            if (type == null || type == EntityType.PLAYER) continue;
            Entity e = create(type, level);
            boolean living = e != null ? e instanceof LivingEntity
                    : LivingEntity.class.isAssignableFrom(type.getBaseClass());
            if (!living) all.add(key);
        }
        all.sort(Comparator.comparing(ResourceLocation::toString));
        return all;
    }

    private static Entity create(EntityType<?> type, Level level) {
        try {
            return type.create(level);
        } catch (Throwable t) {
            return null;
        }
    }
}
