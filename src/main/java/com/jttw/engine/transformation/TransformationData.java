package com.jttw.engine.transformation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Stores transformation state for a player
 */
public class TransformationData {
    private final EntityType<?> entityType;
    private final ResourceLocation entityId;
    private final float eyeHeight;
    private final float width;
    private final float height;

    // NEW: trait flags
    private final boolean canFly;
        private final boolean canPhase;
    // Player forms only: whose appearance this is
    @Nullable private final UUID disguiseId;
    @Nullable private final String disguiseName;

    public TransformationData(EntityType<?> entityType, float eyeHeight, float width, float height, boolean canFly, boolean canPhase) {
        this(entityType, eyeHeight, width, height, canFly, canPhase, null, null);
    }

    private TransformationData(EntityType<?> entityType, float eyeHeight, float width, float height,
                               boolean canFly, boolean canPhase, @Nullable UUID disguiseId, @Nullable String disguiseName) {
        this.disguiseId = disguiseId;
        this.disguiseName = disguiseName;
        this.entityType = entityType;
        this.entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        this.eyeHeight = eyeHeight;
        this.width = width;
        this.height = height;
        this.canFly = canFly;
        this.canPhase = canPhase;
    }

    /**
     * Factory: build from a real instantiated Entity (server-side).
     * Supports both LivingEntity and non-living entities (arrows, minecarts, etc.).
     */
    public static TransformationData fromEntity(EntityType<?> type, Entity entity) {
        Pose pose = Pose.STANDING; // consistent sizing baseline
        EntityDimensions dims = entity.getDimensions(pose);

        float eye;
        if (entity instanceof LivingEntity le) {
            eye = le.getEyeHeight(pose);
        } else {
            eye = objectEyeHeight(dims.height);
        }

        // Flying trait: cover common vanilla flyers + class-based flyers
        boolean canFly = FormClassifier.isFlyer(entity);
        boolean canPhase = isPhasingMob(entity);

        return new TransformationData(type, eye, dims.width, dims.height, canFly, canPhase);
    }


    /** An object form's camera never sits lower than this above the feet. */
    public static final float MIN_OBJECT_EYE_HEIGHT = 0.25f;

    /**
     * Objects have no eyes: the camera goes at 85% of their height, never above a player's eyes,
     * and never below {@link #MIN_OBJECT_EYE_HEIGHT}. Zero-height objects (displays, markers,
     * lightning) would otherwise put the camera at the feet, where the game sees the head inside
     * the floor block and covers the screen with its texture.
     */
    public static float objectEyeHeight(float height) {
        return Math.max(MIN_OBJECT_EYE_HEIGHT, Math.min(1.62f, height * 0.85f));
    }

    private static boolean isPhasingMob(Entity entity) {
        EntityType<?> t = entity.getType();
        return t == EntityType.VEX || t == EntityType.ALLAY;
    }

        /** A player form: looks like the player {@code id} ({@code name}), at player size. */
    public static TransformationData ofPlayer(UUID id, String name) {
        return new TransformationData(EntityType.PLAYER, 1.62f, 0.6f, 1.8f, false, false, id, name);
    }

    /** True for a player form (another player's appearance). */
    public boolean isPlayerForm() {
        return disguiseId != null;
    }

    /** The player whose appearance this form wears, or null if it isn't a player form. */
    @Nullable
    public UUID getDisguiseId() {
        return disguiseId;
    }

    /** That player's name, shown over the transformed player's head. */
    @Nullable
    public String getDisguiseName() {
        return disguiseName;
    }

    /** The entity type for a saved or received id, or null if no such type is registered. */
    @Nullable
    private static EntityType<?> knownType(ResourceLocation id) {
        // The registry returns its default (pig) for unknown ids rather than null
        return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public ResourceLocation getEntityId() {
        return entityId;
    }

    public float getEyeHeight() {
        return eyeHeight;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean canFly() {
        return canFly;
    }

    public boolean canPhase() {
        return canPhase;
    }

    public float getScaleFactor() {
        // no longer used for rendering (keep if you want for other features)
        return height / 1.8f;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("entity_id", entityId.toString());
        tag.putFloat("eye_height", eyeHeight);
        tag.putFloat("width", width);
        tag.putFloat("height", height);
        tag.putBoolean("can_fly", canFly);
                tag.putBoolean("can_phase", canPhase);
        if (disguiseId != null) {
            tag.putUUID("disguise_id", disguiseId);
            tag.putString("disguise_name", disguiseName == null ? "" : disguiseName);
        }
        return tag;
    }

    @Nullable
    public static TransformationData deserialize(CompoundTag tag) {
        try {
            ResourceLocation entityId = ResourceLocation.parse(tag.getString("entity_id"));

            EntityType<?> type = knownType(entityId);
            if (type == null) return null;
            if (tag.hasUUID("disguise_id")) return ofPlayer(tag.getUUID("disguise_id"), tag.getString("disguise_name"));
            if (type == EntityType.PLAYER) return null; // a player form must say whose

            float eyeHeight = tag.getFloat("eye_height");
            float width = tag.contains("width") ? tag.getFloat("width") : type.getDimensions().width;
            float height = tag.contains("height") ? tag.getFloat("height") : type.getDimensions().height;

            boolean canFly = tag.contains("can_fly") && tag.getBoolean("can_fly");
            boolean canPhase = tag.contains("can_phase") && tag.getBoolean("can_phase");

            return new TransformationData(type, eyeHeight, width, height, canFly, canPhase);
        } catch (Exception e) {
            return null;
        }
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(entityId);
        buf.writeFloat(eyeHeight);
        buf.writeFloat(width);
        buf.writeFloat(height);
        buf.writeBoolean(canFly);
                buf.writeBoolean(canPhase);
        buf.writeBoolean(disguiseId != null);
        if (disguiseId != null) {
            buf.writeUUID(disguiseId);
            buf.writeUtf(disguiseName == null ? "" : disguiseName);
        }
    }

    @Nullable
    public static TransformationData fromNetwork(FriendlyByteBuf buf) {
        try {
            ResourceLocation entityId = buf.readResourceLocation();
            float eyeHeight = buf.readFloat();
            float width = buf.readFloat();
            float height = buf.readFloat();
            boolean canFly = buf.readBoolean();
            boolean canPhase = buf.readBoolean();
            boolean disguised = buf.readBoolean();
            UUID disguiseId = disguised ? buf.readUUID() : null;
            String disguiseName = disguised ? buf.readUtf() : null;

            EntityType<?> type = knownType(entityId);
            if (type == null) return null;
            if (disguised) return ofPlayer(disguiseId, disguiseName);
            if (type == EntityType.PLAYER) return null;
            return new TransformationData(type, eyeHeight, width, height, canFly, canPhase);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "TransformationData{" +
                "entityId=" + entityId +
                ", eyeHeight=" + eyeHeight +
                ", dimensions=" + width + "x" + height +
                ", canFly=" + canFly +
                ", canPhase=" + canPhase +
                (disguiseId != null ? ", disguise=" + disguiseName + "/" + disguiseId : "") +
                '}';
    }
}