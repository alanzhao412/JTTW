package com.jttw.engine.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Everything the mod registers has an English name, so death messages, tooltips and the like
 * never show a raw translation key. The ids are read from the registry classes' source.
 */
class LangNamesTest {

    private static final Path ROOT = Path.of(System.getProperty("jttw.projectDir", "."));

    private static String source(String file) throws Exception {
        return Files.readString(ROOT.resolve("src/main/java/com/jttw/engine/registry/" + file));
    }

    private static List<String> registered(String file, String register) throws Exception {
        return Pattern.compile("\\b" + register + "\\.register\\w*\\(\\s*\"([a-z0-9_/]+)\"")
                .matcher(source(file)).results().map(m -> m.group(1)).toList();
    }

    private static JsonObject english() throws Exception {
        return JsonParser.parseString(Files.readString(
                ROOT.resolve("src/main/resources/assets/jttw_engine/lang/en_us.json"))).getAsJsonObject();
    }

    private static void allNamed(List<String> ids, String kind, String... alsoAccepted) throws Exception {
        assertFalse(ids.isEmpty(), "found no " + kind + " ids: has the registration changed?");
        JsonObject lang = english();
        List<String> missing = new ArrayList<>();
        for (String id : ids) {
            boolean named = lang.has(kind + ".jttw_engine." + id);
            for (String other : alsoAccepted) named |= lang.has(other + ".jttw_engine." + id);
            if (!named) missing.add(id);
        }
        assertEquals(List.of(), missing, kind + " ids with no en_us name");
    }

    @Test
    void everyEntityHasAName() throws Exception {
        allNamed(registered("ModEntities.java", "ENTITIES"), "entity");
    }

    @Test
    void everyItemHasAName() throws Exception {
        // a block's item is named by its block
        allNamed(registered("ModItems.java", "ITEMS"), "item", "block");
    }

    @Test
    void everyBlockHasAName() throws Exception {
        allNamed(registered("ModBlocks.java", "BLOCKS"), "block");
    }

    @Test
    void everyEffectHasAName() throws Exception {
        allNamed(registered("ModEffects.java", "EFFECTS"), "effect");
    }
}
