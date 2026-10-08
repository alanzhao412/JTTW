package com.jttw.engine.reincarnation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReincarnationResolver {
    private ReincarnationResolver() {}

    public enum Alignment {
        HEAVENLY,
        SPIRIT_BEAST,
        PRIMORDIAL,
        YAOGUAI
    }

    private record Pair(Alignment a, int v) {}

    /**
     * Determine reincarnation outcome from 4 alignment values.
     * Tie-breaker is deterministic: PRIMORDIAL > HEAVENLY > SPIRIT_BEAST > YAOGUAI.
     */
    public static ReincarnationOutcome resolve(int heavenly, int spiritBeast, int primordial, int yaoguai) {
        List<Pair> list = new ArrayList<>(4);
        list.add(new Pair(Alignment.HEAVENLY, heavenly));
        list.add(new Pair(Alignment.SPIRIT_BEAST, spiritBeast));
        list.add(new Pair(Alignment.PRIMORDIAL, primordial));
        list.add(new Pair(Alignment.YAOGUAI, yaoguai));

        list.sort(Comparator
                .comparingInt((Pair p) -> p.v).reversed()
                .thenComparingInt(p -> tiePriority(p.a))
        );

        Alignment primary = list.get(0).a;
        Alignment secondary = list.get(1).a;

        // Special case: Spirit Beast primary + all three secondaries within 10% of each other
        // → Turtle (balanced player, jack-of-all-trades)
        if (primary == Alignment.SPIRIT_BEAST) {
            int second = list.get(1).v;
            int third = list.get(2).v;
            int fourth = list.get(3).v;
            // Check if the non-primary values are all close together
            if (second > 0 && third > 0) {
                double avg = (second + third + fourth) / 3.0;
                boolean allClose = Math.abs(second - avg) <= avg * 0.10
                        && Math.abs(third - avg) <= avg * 0.10
                        && Math.abs(fourth - avg) <= avg * 0.10;
                if (allClose) {
                    return ReincarnationOutcome.TURTLE;
                }
            }
        }

        return switch (primary) {
            case PRIMORDIAL -> switch (secondary) {
                case YAOGUAI -> ReincarnationOutcome.CELESTIAL_PRIMATE;
                case HEAVENLY -> ReincarnationOutcome.YUAN_DI;
                case SPIRIT_BEAST -> ReincarnationOutcome.GOLD_WINGED_PENG;
                case PRIMORDIAL -> ReincarnationOutcome.YUAN_DI;
            };
            case HEAVENLY -> switch (secondary) {
                case PRIMORDIAL -> ReincarnationOutcome.EYE_OF_HEAVEN;
                case YAOGUAI -> ReincarnationOutcome.THIRD_LOTUS_PRINCE;
                case SPIRIT_BEAST -> ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY;
                case HEAVENLY -> ReincarnationOutcome.EYE_OF_HEAVEN;
            };
            case SPIRIT_BEAST -> switch (secondary) {
                case HEAVENLY -> ReincarnationOutcome.DRAGON;
                case PRIMORDIAL -> ReincarnationOutcome.PHOENIX;
                case YAOGUAI -> ReincarnationOutcome.WHITE_TIGER;
                case SPIRIT_BEAST -> ReincarnationOutcome.TURTLE; // unreachable, but compiler requires it
            };
            case YAOGUAI -> switch (secondary) {
                case HEAVENLY -> ReincarnationOutcome.BULL_DEMON_KING;
                case SPIRIT_BEAST -> ReincarnationOutcome.SAURIAN_DEMON_KING;
                case PRIMORDIAL -> ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS;
                case YAOGUAI -> ReincarnationOutcome.BULL_DEMON_KING;
            };
        };
    }

    private static int tiePriority(Alignment a) {
        // lower number = higher priority in the thenComparingInt
        return switch (a) {
            case PRIMORDIAL -> 0;
            case HEAVENLY -> 1;
            case SPIRIT_BEAST -> 2;
            case YAOGUAI -> 3;
        };
    }
}
