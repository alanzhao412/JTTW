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

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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
    // Block forms only (chest, sign, TNT): the block, the sign's text, when a lit TNT goes off
    @Nullable private final Block block;
    private final List<String> signLines;
    private final long litUntil;

    /** Lines a sign form holds, as on a real sign. */
    public static final int SIGN_LINES = 4;
    /** Characters per sign line. */
    public static final int SIGN_LINE_LENGTH = 30;

    public TransformationData(EntityType<?> entityType, float eyeHeight, float width, float height, boolean canFly, boolean canPhase) {
        this(entityType, eyeHeight, width, height, canFly, canPhase, null, null, null, List.of(), 0L);
    }

    private TransformationData(EntityType<?> entityType, float eyeHeight, float width, float height,
                               boolean canFly, boolean canPhase, @Nullable UUID disguiseId, @Nullable String disguiseName,
                               @Nullable Block block, List<String> signLines, long litUntil) {
        this.disguiseId = disguiseId;
        this.disguiseName = disguiseName;
        this.block = block;
        this.signLines = List.copyOf(signLines);
        this.litUntil = litUntil;
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
        return new TransformationData(EntityType.PLAYER, 1.62f, 0.6f, 1.8f, false, false, id, name,
                null, List.of(), 0L);
    }

    /**
     * A block form (a chest, a sign, TNT): the block's own size, an object's camera. Its entity
     * type is the falling block, the entity that is "a block", so object rules apply to it.
     */
    public static TransformationData ofBlock(Block block) {
        VoxelShape shape = block.defaultBlockState().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        AABB box = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        float width = (float) Math.max(box.getXsize(), box.getZsize());
        float height = (float) box.getYsize();
        return new TransformationData(EntityType.FALLING_BLOCK, objectEyeHeight(height), width, height,
                false, false, null, null, block, List.of(), 0L);
    }

    /** True for a block form. */
    public boolean isBlockForm() {
        return block != null;
    }

    /** The block this form is, or null if it isn't a block form. */
    @Nullable
    public Block getBlock() {
        return block;
    }

    /** The block's id, or the entity's for other forms: what the player has become. */
    public ResourceLocation getFormId() {
        return block != null ? ForgeRegistries.BLOCKS.getKey(block) : entityId;
    }

    /** A sign form's text: up to {@value #SIGN_LINES} lines. */
    public List<String> getSignLines() {
        return signLines;
    }

    /** The same form with this text, cut to {@value #SIGN_LINES} lines of {@value #SIGN_LINE_LENGTH} characters. */
    public TransformationData withSignLines(List<String> lines) {
        List<String> clean = new ArrayList<>();
        for (int i = 0; i < Math.min(SIGN_LINES, lines.size()); i++) {
            String line = lines.get(i) == null ? "" : lines.get(i);
            clean.add(line.length() > SIGN_LINE_LENGTH ? line.substring(0, SIGN_LINE_LENGTH) : line);
        }
        return new TransformationData(entityType, eyeHeight, width, height, canFly, canPhase,
                disguiseId, disguiseName, block, clean, litUntil);
    }

    /** Game time a lit TNT form explodes; 0 if it is not lit. */
    public long getLitUntil() {
        return litUntil;
    }

    public boolean isLit() {
        return litUntil > 0;
    }

    /** The same form, lit to explode at this game time (0 puts it out). */
    public TransformationData withLitUntil(long gameTime) {
        return new TransformationData(entityType, eyeHeight, width, height, canFly, canPhase,
                disguiseId, disguiseName, block, signLines, gameTime);
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

    /** The block for a saved or received id, or null if no such block is registered. */
    @Nullable
    private static Block knownBlock(ResourceLocation id) {
        return ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
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
        if (block != null) {
            tag.putString("block", ForgeRegistries.BLOCKS.getKey(block).toString());
            ListTag lines = new ListTag();
            for (String line : signLines) lines.add(StringTag.valueOf(line));
            tag.put("sign_lines", lines);
            // a lit fuse is not saved: a TNT form logs back in unlit
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
            if (tag.contains("block")) {
                Block block = knownBlock(ResourceLocation.parse(tag.getString("block")));
                if (block == null) return null;
                List<String> lines = new ArrayList<>();
                for (Tag t : tag.getList("sign_lines", Tag.TAG_STRING)) lines.add(t.getAsString());
                return ofBlock(block).withSignLines(lines);
            }
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
        buf.writeBoolean(block != null);
        if (block != null) {
            buf.writeResourceLocation(ForgeRegistries.BLOCKS.getKey(block));
            buf.writeVarInt(signLines.size());
            for (String line : signLines) buf.writeUtf(line, SIGN_LINE_LENGTH);
            buf.writeLong(litUntil);
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
            if (buf.readBoolean()) {
                Block block = knownBlock(buf.readResourceLocation());
                int count = buf.readVarInt();
                if (count < 0 || count > SIGN_LINES) return null;
                List<String> lines = new ArrayList<>();
                for (int i = 0; i < count; i++) lines.add(buf.readUtf(SIGN_LINE_LENGTH));
                long litUntil = buf.readLong();
                return block == null ? null : ofBlock(block).withSignLines(lines).withLitUntil(litUntil);
            }

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
                (block != null ? ", block=" + getFormId() + (litUntil > 0 ? " lit until " + litUntil : "") : "") +
                '}';
    }
}