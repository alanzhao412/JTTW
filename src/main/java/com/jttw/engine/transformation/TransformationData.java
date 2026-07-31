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

    public TransformationData(EntityType<?> entityType, float eyeHeight, float width, float height, boolean canFly, boolean canPhase) {
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
            // Non-living: no real "eye height". Use a conservative camera height.
            eye = Math.min(1.62f, dims.height * 0.85f);
        }

        // Flying trait: cover common vanilla flyers + class-based flyers
        boolean canFly = isFlyer(entity);
        boolean canPhase = isPhasingMob(entity);

        return new TransformationData(type, eye, dims.width, dims.height, canFly, canPhase);
    }

    /**
     * Conservative "is this a flyer" heuristic.
     * This is for granting MAYFLY (player creative-style flight) while morphed.
     */
    private static boolean isFlyer(Entity entity) {
        // Broad class-based checks
        if (entity instanceof net.minecraft.world.entity.FlyingMob) return true;
        if (entity instanceof net.minecraft.world.entity.animal.FlyingAnimal) return true;

        // Vanilla flyers that may not match those interfaces in every case/mapping:
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

    private static boolean isPhasingMob(Entity entity) {
        EntityType<?> t = entity.getType();
        return t == EntityType.VEX || t == EntityType.ALLAY;
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
        return tag;
    }

    @Nullable
    public static TransformationData deserialize(CompoundTag tag) {
        try {
            ResourceLocation entityId = ResourceLocation.parse(tag.getString("entity_id"));

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
            if (type == null) return null;

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

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
            if (type == null) return null;

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
                '}';
    }
}