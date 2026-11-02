package com.jttw.engine.entity.heaven;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Who struck first, between one of Heaven's host and each player: the players it came at before they
 * struck it (it targeted them: on sight, rallying to an ally, hunting them) and the players who struck
 * it before it came at them. Killing it is self-defence for the first, a crime for the second
 * ({@link com.jttw.engine.world.heaven.HeavenHostility#killDeed}).
 */
public final class Grudge {

    private final Set<UUID> cameAt = new HashSet<>(), struckFirst = new HashSet<>();

    /** It has taken {@code e} for its target: if a player who had not struck it, it came at them. */
    public void targeted(Entity e) {
        if (e instanceof Player p && !struckFirst.contains(p.getUUID())) cameAt.add(p.getUUID());
    }

    /** It was struck by {@code e}: if a player it had not come at, they struck first. */
    public void struckBy(Entity e) {
        if (e instanceof Player p && !cameAt.contains(p.getUUID())) struckFirst.add(p.getUUID());
    }

    /** Whether it came at {@code p} before they struck it: killing it is self-defence. */
    public boolean selfDefence(Player p) {
        return cameAt.contains(p.getUUID());
    }

    /** Whether {@code p} struck it before it came at them. */
    public boolean struckFirst(Player p) {
        return struckFirst.contains(p.getUUID());
    }

    public void save(CompoundTag tag) {
        tag.put("CameAt", list(cameAt));
        tag.put("StruckFirst", list(struckFirst));
    }

    public void load(CompoundTag tag) {
        read(tag.getList("CameAt", Tag.TAG_INT_ARRAY), cameAt);
        read(tag.getList("StruckFirst", Tag.TAG_INT_ARRAY), struckFirst);
    }

    private static ListTag list(Set<UUID> ids) {
        ListTag l = new ListTag();
        for (UUID id : ids) l.add(NbtUtils.createUUID(id));
        return l;
    }

    private static void read(ListTag l, Set<UUID> into) {
        into.clear();
        for (Tag t : l) into.add(NbtUtils.loadUUID(t));
    }
}
