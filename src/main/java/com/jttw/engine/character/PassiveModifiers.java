package com.jttw.engine.character;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * Keeps a skill passive's attribute bonus in step with the skills a player has: called on a
 * regular tick with the amount the current skills give, it adds, updates or (at 0) removes the
 * bonus. Modifiers are transient, so nothing stale is saved; the next refresh rebuilds them.
 */
public final class PassiveModifiers {

    private PassiveModifiers() {}

    /** A stable id for a named passive bonus. */
    public static UUID id(String name) {
        return UUID.nameUUIDFromBytes(("jttw_passive:" + name).getBytes());
    }

    public static void set(LivingEntity e, Attribute attribute, String name, double amount,
                           AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) return;
        UUID id = id(name);
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && existing.getAmount() == amount && existing.getOperation() == op) return;
        if (existing != null) inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    public static void add(LivingEntity e, Attribute attribute, String name, double amount) {
        set(e, attribute, name, amount, AttributeModifier.Operation.ADD_VALUE);
    }
}
