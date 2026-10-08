package com.jttw.engine.character;

import com.jttw.engine.character.bulldemon.BullDemonCharacter;
import com.jttw.engine.character.byakko.ByakkoCharacter;
import com.jttw.engine.character.dragon.DragonCharacter;
import com.jttw.engine.character.erlang.ErlangShenCharacter;
import com.jttw.engine.character.marshal.MarshalCharacter;
import com.jttw.engine.character.nezha.NezhaCharacter;
import com.jttw.engine.character.ninespirits.NineSpiritsCharacter;
import com.jttw.engine.character.peng.PengCharacter;
import com.jttw.engine.character.phoenix.PhoenixCharacter;
import com.jttw.engine.character.turtle.TurtleCharacter;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.character.yuandi.YuanDiCharacter;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Which character a player is playing. The one place outcome ids are mapped to characters.
 *
 * <p>A player's character is their reincarnation outcome. Players given a class with a debug
 * command never reincarnate, so when the outcome is missing or has no character yet the class
 * stands in: Primordial plays Wukong, Heavenly plays Erlang Shen, a Divine Beast plays the
 * beast of its line, and Yaoguai plays the Grand Saint of Nine Spirits (so the Saurian Demon
 * King, who has no character yet, plays him too).
 */
public final class Characters {

    private Characters() {}

    private static final List<PlayableCharacter> ALL = List.of(
            WukongCharacter.INSTANCE, YuanDiCharacter.INSTANCE, PengCharacter.INSTANCE,
            ErlangShenCharacter.INSTANCE, NezhaCharacter.INSTANCE, MarshalCharacter.INSTANCE,
            DragonCharacter.INSTANCE, PhoenixCharacter.INSTANCE, ByakkoCharacter.INSTANCE,
            TurtleCharacter.INSTANCE, NineSpiritsCharacter.INSTANCE, BullDemonCharacter.INSTANCE);

    public static List<PlayableCharacter> all() {
        return ALL;
    }

    /** The character for an outcome id, or null if that outcome has no character yet. */
    @Nullable
    public static PlayableCharacter byOutcome(String outcomeId) {
        for (PlayableCharacter c : ALL) {
            if (c.outcomeId().equals(outcomeId)) return c;
        }
        return null;
    }

    /**
     * Resolves a character from a player's saved progression state.
     *
     * @return the character, or null if neither the outcome nor the class names one
     *         (no class at all)
     */
    @Nullable
    public static PlayableCharacter resolve(String outcomeId, @Nullable ProgressionClass cls,
                                            @Nullable DivineBeastLine line) {
        PlayableCharacter byOutcome = byOutcome(outcomeId);
        if (byOutcome != null) return byOutcome;
        if (cls == null) return null;
        return switch (cls) {
            case PRIMORDIAL -> WukongCharacter.INSTANCE;
            case HEAVENLY -> ErlangShenCharacter.INSTANCE;
            case DIVINE_BEAST -> line == null ? null : switch (line) {
                case WATER -> DragonCharacter.INSTANCE;
                case EARTH -> TurtleCharacter.INSTANCE;
                case BEAST -> ByakkoCharacter.INSTANCE;
                case FIRE -> PhoenixCharacter.INSTANCE;
            };
            case YAOGUAI -> NineSpiritsCharacter.INSTANCE;
        };
    }

    /** The character this player is playing, or null. */
    @Nullable
    public static PlayableCharacter of(Player player) {
        String outcome = player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        return resolve(outcome, PlayerLevel.getProgressionClass(player), PlayerLevel.getBeastLine(player));
    }
}
