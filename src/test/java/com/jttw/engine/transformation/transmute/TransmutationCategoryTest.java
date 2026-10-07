package com.jttw.engine.transformation.transmute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The wire carries a category as its ordinal; a bad ordinal must read as "no category". */
class TransmutationCategoryTest {

    @Test
    void ordinalsMapToTheCategoriesInDeclarationOrder() {
        assertEquals(TransmutationCategory.TOOL, TransmutationCategory.fromOrdinal(0));
        assertEquals(TransmutationCategory.CROP, TransmutationCategory.fromOrdinal(1));
        assertEquals(TransmutationCategory.RAW_FOOD, TransmutationCategory.fromOrdinal(2));
        assertEquals(TransmutationCategory.COOKED_FOOD, TransmutationCategory.fromOrdinal(3));
    }

    @Test
    void outOfRangeOrdinalsAreNull() {
        assertNull(TransmutationCategory.fromOrdinal(-1));
        assertNull(TransmutationCategory.fromOrdinal(4));
        assertNull(TransmutationCategory.fromOrdinal(Integer.MAX_VALUE));
        assertNull(TransmutationCategory.fromOrdinal(Integer.MIN_VALUE));
    }
}
