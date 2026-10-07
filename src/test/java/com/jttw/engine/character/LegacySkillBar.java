package com.jttw.engine.character;

import com.jttw.engine.character.byakko.ByakkoSkillTree;
import com.jttw.engine.character.dragon.DragonSkillTree;
import com.jttw.engine.character.erlang.ErlangShenSkillTree;
import com.jttw.engine.character.nezha.NezhaSkillTree;
import com.jttw.engine.character.phoenix.PhoenixSkillTree;
import com.jttw.engine.character.turtle.TurtleSkillTree;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.character.yuandi.YuanDiSkillTree;
import com.jttw.engine.progression.skilltree.AbilityType;

import java.util.List;
import java.util.Set;

/**
 * The skill-bar builder as it was in SyncAbilitiesPacket before characters got their own
 * classes (commit b88d97c), extracted mechanically as a reference for that refactor.
 * Only the class-fallback blocks that read the player are removed; CharactersTest covers those.
 *
 * <p>Generated test oracle: do not edit by hand. Delete it once the refactor has shipped.
 */
final class LegacySkillBar {

    private LegacySkillBar() {}

    static List<AbilitySlot> build(String outcomeId, Set<String> unlocked) {
        

        // Fixed 4-slot array — V=0, B=1, N=2, M=3
        AbilitySlot[] slots = { AbilitySlot.EMPTY, AbilitySlot.EMPTY, AbilitySlot.EMPTY, AbilitySlot.EMPTY };

        

        // Fallback: if outcome tag missing (e.g. after death before first sync),
        // treat PRIMORDIAL class as celestial_primate so slots are never lost.
        

        if ("celestial_primate".equals(outcomeId)) {
            // Slot 0 (V) — 72 Transformations
            int transformTier = WukongSkillTree.TREE.highestTier(unlocked, "wu_transform");
            if (transformTier >= 1) {
                slots[0] = new AbilitySlot("wu_transform", "72 Transformations",
                        AbilityType.MENU, transformTier);
            }
            // Slot 1 (B) — Shadow Clone (each press summons one more)
            int cloneTier = WukongSkillTree.TREE.highestTier(unlocked, "wu_clone");
            if (cloneTier >= 1) {
                slots[1] = new AbilitySlot("wu_clone", "Shadow Clone", AbilityType.TOGGLE, cloneTier);
            }
            // Slot 2 (N) — Ring of Fire (HELD)
            int ringTier = WukongSkillTree.TREE.highestTier(unlocked, "wu_ring");
            if (ringTier >= 1) {
                slots[2] = new AbilitySlot("wu_ring", "Ring of Fire", AbilityType.HELD, ringTier);
            }
        }

        if ("yuan_di".equals(outcomeId)) {
            buildYuanDiSlots(slots, unlocked);
        }

        // Marshal of the Heavenly Canopy — 36 Transformations (innate, no skill tree needed)
        if ("marshal_of_the_heavenly_canopy".equals(outcomeId)) {
            // Slot 0 (V) — 36 Transformations (always available for Marshal)
            slots[0] = new AbilitySlot("mc_transform", "36 Transformations",
                    AbilityType.MENU, 1);
        }

        // Third Lotus Prince (Nezha)
        if ("third_lotus_prince".equals(outcomeId)) {
            int armTier = NezhaSkillTree.TREE.highestTier(unlocked, "nz_arm");
            if (armTier >= 1) slots[0] = new AbilitySlot("nz_cycle", "Weapon Cycle", AbilityType.TOGGLE, armTier);
            int fireTier = NezhaSkillTree.TREE.highestTier(unlocked, "nz_fire");
            if (fireTier >= 1) slots[1] = new AbilitySlot("nz_fire", "Samadhi Fire", AbilityType.TOGGLE, fireTier);
            int detTier = NezhaSkillTree.TREE.highestTier(unlocked, "nz_det");
            if (detTier >= 1) slots[2] = new AbilitySlot("nz_det", "Lotus Detonation", AbilityType.HELD, detTier);
            int formTier = NezhaSkillTree.TREE.highestTier(unlocked, "nz_form");
            if (formTier >= 1) slots[3] = new AbilitySlot("nz_form", "Three Heads Six Arms", AbilityType.TOGGLE, formTier);
        }

        // Gold Winged Peng — no abilities yet
        // if ("gold_winged_peng".equals(outcomeId)) { buildPengSlots(slots, unlocked); }

        // Erlang Shen (Eye of Heaven outcome)
        
        if ("eye_of_heaven".equals(outcomeId)) {
            // Slot 0 (V) — 72 Transformations
            int esTransformTier = ErlangShenSkillTree.TREE.highestTier(unlocked, "es_transform");
            if (esTransformTier >= 1) {
                slots[0] = new AbilitySlot("es_transform", "72 Transformations",
                        AbilityType.MENU, esTransformTier);
            }
            // Slot 1 (B) — Xiaotian Quan
            int xtqTier = ErlangShenSkillTree.TREE.highestTier(unlocked, "es_xtq");
            if (xtqTier >= 1) {
                slots[1] = new AbilitySlot("es_xtq", "Xiaotian Quan",
                        AbilityType.TOGGLE, xtqTier);
            }
            // Slot 2 (N) — Eye of Heaven (TOGGLE)
            int eyeTier = ErlangShenSkillTree.TREE.highestTier(unlocked, "es_eye");
            if (eyeTier >= 1) {
                slots[2] = new AbilitySlot("es_eye", "Eye of Heaven",
                        AbilityType.TOGGLE, eyeTier);
            }
        }

        // Divine Beast lines
        if ("dragon".equals(outcomeId)) {
            buildDragonSlots(slots, unlocked);
        }
        if ("white_tiger".equals(outcomeId)) {
            buildByakkoSlots(slots, unlocked);
        }
        if ("phoenix".equals(outcomeId)) {
            buildPhoenixSlots(slots, unlocked);
        }
        if ("turtle".equals(outcomeId)) {
            buildTurtleSlots(slots, unlocked);
        }
        // Fallback: route by class + beast line (debug command path)
        

        return java.util.Arrays.asList(slots);
    }


    private static void buildTurtleSlots(AbilitySlot[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Shell Guard
        int shellTier = TurtleSkillTree.TREE.highestTier(unlocked, "tu_shell");
        if (shellTier >= 1) {
            slots[0] = new AbilitySlot("tu_shell", "Shell Guard",
                    AbilityType.HELD, shellTier);
        }
        // Slot 1 (B) — Unstoppable Force
        int forceTier = TurtleSkillTree.TREE.highestTier(unlocked, "tu_force");
        if (forceTier >= 1) {
            slots[1] = new AbilitySlot("tu_force", "Unstoppable Force",
                    AbilityType.TOGGLE, forceTier);
        }
        // Slot 2 (N) — Tectonic Slam (USE)
        int tectonicTier = TurtleSkillTree.TREE.highestTier(unlocked, "tu_tectonic");
        if (tectonicTier >= 1) {
            slots[2] = new AbilitySlot("tu_tectonic", "Tectonic Slam",
                    AbilityType.TOGGLE, tectonicTier);
        }
        // Slot 3 (M) — Gravity Well (TOGGLE)
        int gravityTier = TurtleSkillTree.TREE.highestTier(unlocked, "tu_gravity");
        if (gravityTier >= 1) {
            slots[3] = new AbilitySlot("tu_gravity", "Gravity Well",
                    AbilityType.TOGGLE, gravityTier);
        }
    }

    private static void buildByakkoSlots(AbilitySlot[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Predator's Leap
        int leapTier = ByakkoSkillTree.TREE.highestTier(unlocked, "bk_leap");
        if (leapTier >= 1) {
            slots[0] = new AbilitySlot("bk_leap", "Predator's Leap",
                    AbilityType.TOGGLE, leapTier);
        }
        // Slot 1 (B) — Territorial Roar
        int roarTier = ByakkoSkillTree.TREE.highestTier(unlocked, "bk_roar");
        if (roarTier >= 1) {
            slots[1] = new AbilitySlot("bk_roar", "Territorial Roar",
                    AbilityType.TOGGLE, roarTier);
        }
    }

    private static void buildDragonSlots(AbilitySlot[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Lightning Strike
        int lightningTier = DragonSkillTree.TREE.highestTier(unlocked, "dr_lightning");
        if (lightningTier >= 1) {
            slots[0] = new AbilitySlot("dr_lightning", "Lightning Strike",
                    AbilityType.TOGGLE, lightningTier);
        }
        // Slot 1 (B) — Weather Control
        int weatherTier = DragonSkillTree.TREE.highestTier(unlocked, "dr_weather");
        if (weatherTier >= 1) {
            slots[1] = new AbilitySlot("dr_weather", "Weather Control",
                    AbilityType.TOGGLE, weatherTier);
        }
        // Slot 2 (N) — Tidal Wave (priority) or Dimension Travel (fallback at flight T3)
        int waveTier = DragonSkillTree.TREE.highestTier(unlocked, "dr_wave");
        if (waveTier >= 1) {
            slots[2] = new AbilitySlot("dr_wave", "Tidal Wave",
                    AbilityType.TOGGLE, waveTier);
        } else {
            int dimTier = DragonSkillTree.TREE.highestTier(unlocked, "dr_flight");
            if (dimTier >= 3) {
                slots[2] = new AbilitySlot("dr_dimension", "Dimension Travel",
                        AbilityType.TOGGLE, dimTier);
            }
        }
    }

    private static void buildYuanDiSlots(AbilitySlot[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Primordial Transformation (TOGGLE at T2+, passive at T1)
        int transformTier = YuanDiSkillTree.TREE.highestTier(unlocked, "yd_transform");
        if (transformTier >= 2) {
            slots[0] = new AbilitySlot("yd_transform", "Primordial Form",
                    AbilityType.TOGGLE, transformTier);
        }
        // Slot 1 (B) — Demon's Breath (HELD)
        int breathTier = YuanDiSkillTree.TREE.highestTier(unlocked, "yd_breath");
        if (breathTier >= 1) {
            slots[1] = new AbilitySlot("yd_breath", "Demon's Breath",
                    AbilityType.HELD, breathTier);
        }
        // Slot 2 (N) — Primordial Smite
        int smiteTier = YuanDiSkillTree.TREE.highestTier(unlocked, "yd_smite");
        if (smiteTier >= 1) {
            slots[2] = new AbilitySlot("yd_smite", "Primordial Smite",
                    AbilityType.TOGGLE, smiteTier);
        }
        // Slot 3 (M) — Abyssal Gravity
        int gravityTier = YuanDiSkillTree.TREE.highestTier(unlocked, "yd_gravity");
        if (gravityTier >= 1) {
            slots[3] = new AbilitySlot("yd_gravity", "Abyssal Gravity",
                    AbilityType.TOGGLE, gravityTier);
        }
    }

    private static void buildPhoenixSlots(AbilitySlot[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Flamethrower (HELD)
        int flameTier = PhoenixSkillTree.TREE.highestTier(unlocked, "ph_flame");
        if (flameTier >= 1) {
            slots[0] = new AbilitySlot("ph_flame", "Flamethrower",
                    AbilityType.HELD, flameTier);
        }
        // Slot 1 (B) — Solar Radiance (TOGGLE)
        int radianceTier = PhoenixSkillTree.TREE.highestTier(unlocked, "ph_radiance");
        if (radianceTier >= 1) {
            slots[1] = new AbilitySlot("ph_radiance", "Solar Radiance",
                    AbilityType.TOGGLE, radianceTier);
        }
    }
}
