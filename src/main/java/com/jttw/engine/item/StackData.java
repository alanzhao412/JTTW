package com.jttw.engine.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * The mod's own state on an item stack (a staff's mode, a ring's size), kept in the stack's
 * {@code minecraft:custom_data} component: what stack NBT was before 1.20.5.
 */
public final class StackData {

    private StackData() {}

    /** A copy of the stack's data; empty if it has none. Writes to it are not saved: use {@link #update}. */
    public static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static boolean has(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(key);
    }

    /** Changes the stack's data in place. */
    public static void update(ItemStack stack, Consumer<CompoundTag> change) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        return read(stack).getBoolean(key);
    }

    public static int getInt(ItemStack stack, String key) {
        return read(stack).getInt(key);
    }

    public static long getLong(ItemStack stack, String key) {
        return read(stack).getLong(key);
    }

    public static String getString(ItemStack stack, String key) {
        return read(stack).getString(key);
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        update(stack, tag -> tag.putBoolean(key, value));
    }

    public static void putInt(ItemStack stack, String key, int value) {
        update(stack, tag -> tag.putInt(key, value));
    }

    public static void putLong(ItemStack stack, String key, long value) {
        update(stack, tag -> tag.putLong(key, value));
    }

    public static void putString(ItemStack stack, String key, String value) {
        update(stack, tag -> tag.putString(key, value));
    }

    public static void remove(ItemStack stack, String key) {
        update(stack, tag -> tag.remove(key));
    }
}
