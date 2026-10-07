package com.jttw.engine.transformation;

import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Which transformation art a player practises, at what level, and so which forms they may take.
 * The menu (client) and the request check (server) both ask here, so they always agree.
 */
public final class TransformationAccess {

    private TransformationAccess() {}

    /** The player's transformation art, or null if their character has none. */
    @Nullable
    public static TransformationSchool schoolOf(Player player) {
        PlayableCharacter character = Characters.of(player);
        return character == null ? null : character.transformationSchool();
    }

    /** The player's level (0-3) in their art, given their unlocked skill nodes. */
    public static int levelOf(Player player, Set<String> unlockedNodes) {
        PlayableCharacter character = Characters.of(player);
        return character == null ? 0 : character.transformationLevel(unlockedNodes);
    }

    /** True if a player with these nodes may take a form of this category. */
    public static boolean mayTake(Player player, Set<String> unlockedNodes, FormCategory category) {
        TransformationSchool school = schoolOf(player);
        return school != null && school.allows(category, levelOf(player, unlockedNodes));
    }
}
