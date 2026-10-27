package com.jttw.engine.dimension;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The taller sky, as its data gives it: the dimension type 704 high from y -64 (to y 640), its
 * logical height the same, vanilla's Overworld in all else; the presets new worlds are made from
 * (default, large biomes, amplified, single biome) give it their Overworld, their Nether and End
 * vanilla's; superflat and debug are left as vanilla has them; and the land itself untouched (no
 * noise settings of the Overworld's overridden). Older worlds keep the type written in their saves:
 * KunlunGameTests and the bench ({@code -Pbench.open=<save>}) open one.
 */
class TallOverworldTest {

    private JsonObject json(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void itsDimensionTypeIsVanillasOverworldUnderATallerSky() throws Exception {
        JsonObject t = json("/data/jttw_engine/dimension_type/tall_overworld.json");
        assertEquals(TallOverworld.MIN_Y, t.get("min_y").getAsInt());
        assertEquals(TallOverworld.HEIGHT, t.get("height").getAsInt());
        assertEquals(TallOverworld.HEIGHT, t.get("logical_height").getAsInt(), "logical height to match");
        assertEquals(640, TallOverworld.TOP);
        assertEquals(0, TallOverworld.HEIGHT % 16, "whole sections");
        assertEquals("minecraft:overworld", t.get("effects").getAsString(), "the Overworld's sky");
        assertTrue(t.get("natural").getAsBoolean() && t.get("bed_works").getAsBoolean() && t.get("has_raids").getAsBoolean()
                && t.get("has_skylight").getAsBoolean() && !t.get("has_ceiling").getAsBoolean() && !t.get("ultrawarm").getAsBoolean());
        assertEquals("#minecraft:infiniburn_overworld", t.get("infiniburn").getAsString());
        assertEquals(0.0, t.get("ambient_light").getAsDouble());
        assertEquals("minecraft:uniform", t.getAsJsonObject("monster_spawn_light_level").get("type").getAsString());
    }

    @Test
    void newWorldsArePresetUnderIt() throws Exception {
        for (String preset : List.of("normal", "large_biomes", "amplified", "single_biome_surface")) {
            JsonObject dims = json("/data/minecraft/worldgen/world_preset/" + preset + ".json").getAsJsonObject("dimensions");
            assertEquals("jttw_engine:tall_overworld", dims.getAsJsonObject("minecraft:overworld").get("type").getAsString(), preset);
            assertEquals("minecraft:the_nether", dims.getAsJsonObject("minecraft:the_nether").get("type").getAsString(), preset + ": the Nether as it was");
            assertEquals("minecraft:the_end", dims.getAsJsonObject("minecraft:the_end").get("type").getAsString(), preset + ": the End as it was");
            JsonObject generator = dims.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
            assertEquals("minecraft:noise", generator.get("type").getAsString(), preset + ": vanilla's land");
        }
        assertEquals("minecraft:amplified", json("/data/minecraft/worldgen/world_preset/amplified.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").get("settings").getAsString(), "amplified stays amplified");
        assertEquals("minecraft:large_biomes", json("/data/minecraft/worldgen/world_preset/large_biomes.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").get("settings").getAsString(), "large biomes stay large");
        // the mod's own data (not vanilla's, also on the classpath) overrides nothing else of the Overworld's
        java.nio.file.Path data = java.nio.file.Path.of(System.getProperty("jttw.projectDir", "."), "src/main/resources/data/minecraft");
        assertTrue(java.nio.file.Files.isDirectory(data), "the mod's minecraft data: " + data.toAbsolutePath());
        for (String left : List.of("flat", "debug_all_block_states")) {
            assertFalse(java.nio.file.Files.exists(data.resolve("worldgen/world_preset/" + left + ".json")), left + " left as vanilla has it");
        }
        assertFalse(java.nio.file.Files.exists(data.resolve("worldgen/noise_settings/overworld.json")), "the land's own noise untouched");
        assertFalse(java.nio.file.Files.exists(data.resolve("dimension_type/overworld.json")), "vanilla's Overworld kept as it is, for older worlds");
        assertFalse(java.nio.file.Files.exists(data.resolve("dimension/overworld.json")), "no Overworld forced on every world");
    }
}
