package com.jttw.engine.transformation.transmute;

/**
 * Identifies which group of items can be transmuted together.
 */
public enum TransmutationCategory {
    TOOL,
    CROP,
    RAW_FOOD,
    COOKED_FOOD;

    public static TransmutationCategory fromOrdinal(int ord) {
        TransmutationCategory[] values = values();
        return (ord >= 0 && ord < values.length) ? values[ord] : null;
    }
}
