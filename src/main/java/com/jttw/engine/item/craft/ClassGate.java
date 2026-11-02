package com.jttw.engine.item.craft;

import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Who may make a recipe: a class ({@code "class": "heavenly"}), certain characters
 * ({@code "characters": ["third_lotus_prince"]}, by their reincarnation id), or both (both must hold).
 * A gate with neither lets anyone through.
 *
 * <p>How it is enforced: a gated crafting recipe ({@link GatedRecipe}, {@link RefineRecipe}) matches
 * only while the one crafting is known and passes (the crafting grid and its result slot say who,
 * {@link Crafters}); with no one known (a Crafter block, any other machine) it never matches. The Eight
 * Trigrams Furnace answers to its smith, the last to open it.
 */
public record ClassGate(Optional<ProgressionClass> cls, List<String> characters) {

    public static final ClassGate ANYONE = new ClassGate(Optional.empty(), List.of());

    private static final Codec<ProgressionClass> CLASS_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                ProgressionClass c = ProgressionClass.parse(s.toUpperCase(Locale.ROOT));
                return c == null ? DataResult.error(() -> "no class " + s) : DataResult.success(c);
            }, c -> c.name().toLowerCase(Locale.ROOT));

    public static final Codec<ClassGate> CODEC = RecordCodecBuilder.create(i -> i.group(
            CLASS_CODEC.optionalFieldOf("class").forGetter(ClassGate::cls),
            Codec.STRING.listOf().optionalFieldOf("characters", List.of()).forGetter(ClassGate::characters)
    ).apply(i, ClassGate::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClassGate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8.map(s -> ProgressionClass.valueOf(s), ProgressionClass::name)), ClassGate::cls,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ClassGate::characters,
            ClassGate::new);

    public static ClassGate of(ProgressionClass cls) {
        return new ClassGate(Optional.of(cls), List.of());
    }

    public static ClassGate of(ReincarnationOutcome... who) {
        return new ClassGate(Optional.empty(), java.util.Arrays.stream(who).map(o -> o.id).toList());
    }

    /** Whether the character with this reincarnation id may make it. */
    public boolean allows(@Nullable String outcomeId) {
        if (cls.isEmpty() && characters.isEmpty()) return true;
        ReincarnationOutcome who = outcomeId == null ? null : ReincarnationOutcome.byId(outcomeId);
        if (who == null) return false;
        if (cls.isPresent() && ProgressionClass.fromOutcome(who) != cls.get()) return false;
        return characters.isEmpty() || characters.contains(who.id);
    }

    /** Whether this player may make it (no player: no one). */
    public boolean allows(@Nullable Player p) {
        if (p == null) return cls.isEmpty() && characters.isEmpty();
        return allows(p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));
    }

    /** "Heavenly only", "Nezha only": for tooltips and the guide. */
    public String describe() {
        if (!characters.isEmpty()) {
            List<String> names = characters.stream().map(ClassGate::characterName).toList();
            return String.join(" or ", names) + " only";
        }
        return cls.map(c -> className(c) + " only").orElse("anyone");
    }

    public static String className(ProgressionClass c) {
        return switch (c) {
            case HEAVENLY -> "Heavenly";
            case DIVINE_BEAST -> "Divine Beast";
            case YAOGUAI -> "Yaoguai";
            case PRIMORDIAL -> "Primordial";
        };
    }

    public static String characterName(String id) {
        ReincarnationOutcome o = ReincarnationOutcome.byId(id);
        if (o == null) return id;
        return switch (o) {
            case THIRD_LOTUS_PRINCE -> "Nezha";
            case EYE_OF_HEAVEN -> "Erlang Shen";
            case MARSHAL_HEAVENLY_CANOPY -> "Marshal Tianpeng";
            case PHOENIX -> "the Phoenix";
            case DRAGON -> "the Dragon";
            case WHITE_TIGER -> "the White Tiger";
            case TURTLE -> "the Black Tortoise";
            case CELESTIAL_PRIMATE -> "Sun Wukong";
            case YUAN_DI -> "Yuan Di";
            case GOLD_WINGED_PENG -> "the Peng";
            case BULL_DEMON_KING -> "the Bull Demon King";
            case SAURIAN_DEMON_KING -> "the Saurian Demon King";
            case GRAND_SAINT_NINE_SPIRITS -> "the Nine-Headed Lion";
        };
    }
}
