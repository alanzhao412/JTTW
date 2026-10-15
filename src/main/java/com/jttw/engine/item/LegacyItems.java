package com.jttw.engine.item;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/**
 * Item stacks the mod saved itself under Minecraft 1.20.1 (the armory, a thrown spear, the chest
 * form, the Eight Trigrams Furnace), brought up to 1.21's format by vanilla's own data fixer.
 * Vanilla upgrades the items it saved when it loads an old world, but not these: they live in
 * player data and in a block entity it does not know.
 */
public final class LegacyItems {

    private LegacyItems() {}

    /** The data version of Minecraft 1.20.1, which saved them. */
    private static final int MC_1_20_1 = 3465;

    /** The stack saved as {@code tag}, in 1.21's format: {@code tag} itself if it already is. */
    public static CompoundTag upgrade(CompoundTag tag) {
        // 1.20.1 always wrote "Count" (a byte); 1.21 writes "count", or nothing for one item
        if (!tag.contains("Count", Tag.TAG_ANY_NUMERIC)) return tag;
        Tag fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, (Tag) tag),
                MC_1_20_1, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        if (!(fixed instanceof CompoundTag out)) return tag;
        // the container's own field, beside the stack's
        if (tag.contains("Slot")) out.put("Slot", tag.get("Slot").copy());
        return out;
    }

    /** Upgrades, in place, every stack in a container's "Items" list (as ContainerHelper saves it). */
    public static CompoundTag upgradeContainer(CompoundTag container) {
        ListTag items = container.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) items.set(i, upgrade(items.getCompound(i)));
        return container;
    }
}
