package com.jttw.engine.transformation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every form the transformation menus offer, sorted the way they show them. The mob menu, the
 * object picker and the tests all read this, so they agree on what a form is.
 *
 * <ul>
 *   <li><b>Mob forms</b>: every living entity type except the player, by {@link FormCategory}.</li>
 *   <li><b>Object forms</b>: every other entity type (arrows, minecarts, boats...), except
 *       the ones that are no body at all ({@link #NOT_FORMS}) and the mod's own machinery
 *       (Qi Wave, the thrown spear...).</li>
 *   <li><b>Block forms</b>: the blocks a player can become ({@link #BLOCK_FORMS}), offered with
 *       the objects.</li>
 * </ul>
 * Player forms are other online players and have their own picker.
 */
public final class FormCatalog {

    private FormCatalog() {}

    /**
     * Entities that are never forms: they draw nothing, or are not a thing to be (lightning, a
     * fishing line's bobber, empty display frames, a dropped item with no item, evoker fangs
     * that show only as they bite), or are a block in another state (primed TNT; the TNT block
     * form lights instead).
     */
    public static final Set<ResourceLocation> NOT_FORMS = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "lightning_bolt"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "fishing_bobber"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "marker"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "interaction"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "item"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "evoker_fangs"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "block_display"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "item_display"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "text_display"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "tnt"),
            // Wukong's shadow clone is a piece of his ability, made in his likeness: to "become"
            // it is to take a body like his own that is not his (no flight of his, nothing in hand)
            ResourceLocation.fromNamespaceAndPath(com.jttw.engine.JTTWEngine.MOD_ID, "wu_clone"));

    /**
     * Forms only a character's own ability takes, never offered in a menu: the Great White Bull
     * is the Bull Demon King's true form, with its own cost and strength (BullDemonAbilityHandler);
     * the White Tiger, Byakko's (TrueForm).
     */
    public static final Set<ResourceLocation> ABILITY_FORMS = Set.of(
            ResourceLocation.fromNamespaceAndPath(com.jttw.engine.JTTWEngine.MOD_ID, "great_white_bull"),
            ResourceLocation.fromNamespaceAndPath(com.jttw.engine.JTTWEngine.MOD_ID, "white_tiger"),
            ResourceLocation.fromNamespaceAndPath(com.jttw.engine.JTTWEngine.MOD_ID, "azure_dragon"),
            ResourceLocation.fromNamespaceAndPath(com.jttw.engine.JTTWEngine.MOD_ID, "black_tortoise"),
            ResourceLocation.fromNamespaceAndPath(com.jttw.engine.JTTWEngine.MOD_ID, "vermilion_bird"));

    /** The blocks a player can become, each with its own behaviour. */
    public static final List<Block> BLOCK_FORMS = List.of(Blocks.CHEST, Blocks.OAK_SIGN, Blocks.TNT);

    /**
     * Whether this entity may be chosen as a form. The mod's own non-living entities (Qi Wave,
     * thrown spear, howl, placed staff) are machinery, not forms.
     */
    public static boolean isOffered(ResourceLocation id, Entity instance) {
        if (NOT_FORMS.contains(id) || ABILITY_FORMS.contains(id)) return false;
        return !(com.jttw.engine.JTTWEngine.MOD_ID.equals(id.getNamespace()) && !(instance instanceof LivingEntity));
    }

    /** Block forms by id, in the order offered. */
    public static List<ResourceLocation> blockForms() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Block b : BLOCK_FORMS) ids.add(BuiltInRegistries.BLOCK.getKey(b));
        return ids;
    }

    /** Mob forms by category, each list sorted by id. */
    public static Map<FormCategory, List<ResourceLocation>> mobForms(Level level) {
        Map<FormCategory, List<ResourceLocation>> byCategory = new EnumMap<>(FormCategory.class);
        for (ResourceLocation key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
            if (type == null || type == EntityType.PLAYER) continue;
            Entity e = create(type, level);
            if (!(e instanceof LivingEntity) || !isOffered(key, e)) continue;
            byCategory.computeIfAbsent(FormClassifier.classify(type, e), k -> new ArrayList<>()).add(key);
        }
        byCategory.values().forEach(l -> l.sort(Comparator.comparing(ResourceLocation::toString)));
        return byCategory;
    }

    /** Object forms, sorted by id. */
    public static List<ResourceLocation> objectForms(Level level) {
        List<ResourceLocation> all = new ArrayList<>();
        for (ResourceLocation key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
            if (type == null || type == EntityType.PLAYER) continue;
            Entity e = create(type, level);
            boolean living = e != null ? e instanceof LivingEntity
                    : LivingEntity.class.isAssignableFrom(type.getBaseClass());
            if (!living && (e == null ? !NOT_FORMS.contains(key) : isOffered(key, e))) all.add(key);
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
