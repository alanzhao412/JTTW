package com.jttw.engine.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The wheel of realms and how a traveller's x and z carry between them. */
class RealmArrivalTest {

    @Test
    void theWheelRunsOverworldNetherEndHellHeavenAndRoundAgain() {
        assertEquals(List.of(Level.OVERWORLD, Level.NETHER, Level.END, JTTWDimensions.HELL, JTTWDimensions.HEAVEN),
                RealmArrival.WHEEL);
        assertEquals(Level.NETHER, RealmArrival.next(Level.OVERWORLD));
        assertEquals(Level.END, RealmArrival.next(Level.NETHER));
        assertEquals(JTTWDimensions.HELL, RealmArrival.next(Level.END));
        assertEquals(JTTWDimensions.HEAVEN, RealmArrival.next(JTTWDimensions.HELL));
        assertEquals(Level.OVERWORLD, RealmArrival.next(JTTWDimensions.HEAVEN), "and round again");
        assertEquals(Level.OVERWORLD, RealmArrival.next(
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("jttw_test", "nowhere"))),
                "off the wheel: home");
    }

    @Test
    void anEighthIntoTheNetherAndEightTimesOutOfIt() {
        assertEquals(100, RealmArrival.scale(800, Level.OVERWORLD, Level.NETHER));
        assertEquals(12, RealmArrival.scale(103, Level.OVERWORLD, Level.NETHER), "rounded down");
        assertEquals(-13, RealmArrival.scale(-97, Level.OVERWORLD, Level.NETHER), "rounded down, below zero too");
        assertEquals(800, RealmArrival.scale(100, Level.NETHER, Level.OVERWORLD));
        assertEquals(-80, RealmArrival.scale(-10, Level.NETHER, Level.OVERWORLD));
        assertEquals(100, RealmArrival.scale(800, JTTWDimensions.HEAVEN, Level.NETHER), "from any other realm as from the Overworld");
        assertEquals(800, RealmArrival.scale(100, Level.NETHER, Level.END));
        assertEquals(103, RealmArrival.scale(103, Level.END, Level.OVERWORLD), "unscaled between the others");
        assertEquals(103, RealmArrival.scale(103, Level.NETHER, Level.NETHER));
    }
}
