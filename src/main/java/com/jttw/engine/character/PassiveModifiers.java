package com.jttw.engine.character;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Keeps a skill passive's attribute bonus in step with the skills a player has: called on a
 * regular tick with the amount the current skills give, it adds, updates or (at 0) removes the
 * bonus. Modifiers are transient, so nothing stale is saved; the next refresh rebuilds them.
 */
public final class PassiveModifiers {

    private PassiveModifiers() {}

    /** A stable id for a named passive bonus. */
    public static ResourceLocation id(String name) {
        return JTTWEngine.id("passive/" + name);
    }

    public static void set(LivingEntity e, Holder<Attribute> attribute, String name, double amount,
                           AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) return;
        ResourceLocation id = id(name);
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && existing.amount() == amount && existing.operation() == op) return;
        if (existing != null) inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    public static void add(LivingEntity e, Holder<Attribute> attribute, String name, double amount) {
        set(e, attribute, name, amount, AttributeModifier.Operation.ADD_VALUE);
    }
}
