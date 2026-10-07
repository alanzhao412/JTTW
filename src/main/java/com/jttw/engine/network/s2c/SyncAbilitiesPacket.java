package com.jttw.engine.network.s2c;

import net.minecraft.client.Minecraft;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.character.dragon.DragonSkillTree;
import com.jttw.engine.character.erlang.ErlangShenSkillTree;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.character.phoenix.PhoenixSkillTree;
import com.jttw.engine.character.yuandi.YuanDiSkillTree;
import com.jttw.engine.character.byakko.ByakkoSkillTree;
import com.jttw.engine.character.turtle.TurtleSkillTree;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.character.nezha.NezhaSkillTree;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs a player's active skill-bar ability list (slot 0, 1, 2) from server to client.
 *
 * <p>Each entry carries the ability ID, display name, how it is activated ({@link AbilityType}),
 * and its current tier — so the HUD renderer and key handlers know exactly how to present
 * and activate the ability.
 *
 * <p>Sent by {@link LevelManager} on login and by {@link SpendSkillPointPacket} when a
 * relevant skill node is unlocked.
 */
public class SyncAbilitiesPacket {

    /** An active ability assigned to one of the three skill-bar slots. */
    public record Entry(String id, String displayName, AbilityType type, int tier) {}

    private final UUID playerId;
    private final List<Entry> abilities;

    public SyncAbilitiesPacket(UUID playerId, List<Entry> abilities) {
        this.playerId  = playerId;
        this.abilities = List.copyOf(abilities);
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(SyncAbilitiesPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeInt(pkt.abilities.size());
        for (Entry e : pkt.abilities) {
            buf.writeUtf(e.id());
            buf.writeUtf(e.displayName());
            buf.writeByte(e.type().ordinal());
            buf.writeByte(e.tier());
        }
    }

    public static SyncAbilitiesPacket decode(FriendlyByteBuf buf) {
        UUID id    = buf.readUUID();
        int count  = buf.readInt();
        List<Entry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String abilityId   = buf.readUtf();
            String displayName = buf.readUtf();
            int typeOrdinal    = buf.readByte();
            int tier           = buf.readByte();
            AbilityType type   = AbilityType.values()[Math.min(typeOrdinal, AbilityType.values().length - 1)];
            list.add(new Entry(abilityId, displayName, type, tier));
        }
        return new SyncAbilitiesPacket(id, list);
    }

    // -----------------------------------------------------------------------
    // Handle
    // -----------------------------------------------------------------------

    public static void handle(SyncAbilitiesPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncAbilitiesPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.getUUID().equals(pkt.playerId)) return;
        com.jttw.engine.client.ClientAbilityData.setAbilities(pkt.abilities);
    }

    // -----------------------------------------------------------------------
    // Server-side builder — constructs from the player's skill tree state
    // -----------------------------------------------------------------------

    /**
     * Sentinel entry for a slot that exists but hasn't been unlocked yet.
     * The HUD renders it as an empty placeholder; key handlers ignore it.
     */
    public static final Entry EMPTY_SLOT = new Entry("", "", AbilityType.PASSIVE, 0);

    /**
     * Builds the skill-bar entry list for the given player.
     *
     * <p>Always returns exactly 3 entries at fixed indices so the key handler and
     * server-side slot routing always agree regardless of what is unlocked:
     * <ul>
     *   <li>Index 0 (V) — 72 Transformations</li>
     *   <li>Index 1 (B) — Class ability (Shadow Clone / Xiaotian Quan)</li>
     *   <li>Index 2 (N) — Class ability (Ring of Fire / Eye of Heaven)</li>
     * </ul>
     * Locked slots are filled with {@link #EMPTY_SLOT}.
     */
    public static SyncAbilitiesPacket build(net.minecraft.server.level.ServerPlayer player) {
        Set<String> unlocked = SkillTreeData.getUnlockedNodes(player);

        // Fixed 4-slot array — V=0, B=1, N=2, M=3
        Entry[] slots = { EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT };

        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);

        // Fallback: if outcome tag missing (e.g. after death before first sync),
        // treat PRIMORDIAL class as celestial_primate so slots are never lost.
        if (outcomeId.isEmpty()) {
            com.jttw.engine.progression.stats.ProgressionClass cls =
                    com.jttw.engine.progression.level.PlayerLevel.getProgressionClass(player);
            if (cls == com.jttw.engine.progression.stats.ProgressionClass.PRIMORDIAL) {
                outcomeId = "celestial_primate";
            }
        }

        if ("celestial_primate".equals(outcomeId)) {
            // Slot 0 (V) — 72 Transformations
            int transformTier = WukongSkillTree.getHighestTier(unlocked, "wu_transform");
            if (transformTier >= 1) {
                slots[0] = new Entry("wu_transform", "72 Transformations",
                        AbilityType.MENU, transformTier);
            }
            // Slot 1 (B) — Shadow Clone (each press summons one more)
            int cloneTier = WukongSkillTree.getHighestTier(unlocked, "wu_clone");
            if (cloneTier >= 1) {
                slots[1] = new Entry("wu_clone", "Shadow Clone", AbilityType.TOGGLE, cloneTier);
            }
            // Slot 2 (N) — Ring of Fire (HELD)
            int ringTier = WukongSkillTree.getHighestTier(unlocked, "wu_ring");
            if (ringTier >= 1) {
                slots[2] = new Entry("wu_ring", "Ring of Fire", AbilityType.HELD, ringTier);
            }
        }

        if ("yuan_di".equals(outcomeId)) {
            buildYuanDiSlots(slots, unlocked);
        }

        // Marshal of the Heavenly Canopy — 36 Transformations (innate, no skill tree needed)
        if ("marshal_of_the_heavenly_canopy".equals(outcomeId)) {
            // Slot 0 (V) — 36 Transformations (always available for Marshal)
            slots[0] = new Entry("mc_transform", "36 Transformations",
                    AbilityType.MENU, 1);
        }

        // Third Lotus Prince (Nezha)
        if ("third_lotus_prince".equals(outcomeId)) {
            int armTier = NezhaSkillTree.getHighestTier(unlocked, "nz_arm");
            if (armTier >= 1) slots[0] = new Entry("nz_cycle", "Weapon Cycle", AbilityType.TOGGLE, armTier);
            int fireTier = NezhaSkillTree.getHighestTier(unlocked, "nz_fire");
            if (fireTier >= 1) slots[1] = new Entry("nz_fire", "Samadhi Fire", AbilityType.TOGGLE, fireTier);
            int detTier = NezhaSkillTree.getHighestTier(unlocked, "nz_det");
            if (detTier >= 1) slots[2] = new Entry("nz_det", "Lotus Detonation", AbilityType.HELD, detTier);
            int formTier = NezhaSkillTree.getHighestTier(unlocked, "nz_form");
            if (formTier >= 1) slots[3] = new Entry("nz_form", "Three Heads Six Arms", AbilityType.TOGGLE, formTier);
        }

        // Gold Winged Peng — no abilities yet
        // if ("gold_winged_peng".equals(outcomeId)) { buildPengSlots(slots, unlocked); }

        // Erlang Shen (Eye of Heaven outcome)
        if (outcomeId.isEmpty()) {
            com.jttw.engine.progression.stats.ProgressionClass cls2 =
                    com.jttw.engine.progression.level.PlayerLevel.getProgressionClass(player);
            if (cls2 == com.jttw.engine.progression.stats.ProgressionClass.HEAVENLY) {
                outcomeId = "eye_of_heaven";
            }
        }
        if ("eye_of_heaven".equals(outcomeId)) {
            // Slot 0 (V) — 72 Transformations
            int esTransformTier = ErlangShenSkillTree.getHighestTier(unlocked, "es_transform");
            if (esTransformTier >= 1) {
                slots[0] = new Entry("es_transform", "72 Transformations",
                        AbilityType.MENU, esTransformTier);
            }
            // Slot 1 (B) — Xiaotian Quan
            int xtqTier = ErlangShenSkillTree.getHighestTier(unlocked, "es_xtq");
            if (xtqTier >= 1) {
                slots[1] = new Entry("es_xtq", "Xiaotian Quan",
                        AbilityType.TOGGLE, xtqTier);
            }
            // Slot 2 (N) — Eye of Heaven (TOGGLE)
            int eyeTier = ErlangShenSkillTree.getHighestTier(unlocked, "es_eye");
            if (eyeTier >= 1) {
                slots[2] = new Entry("es_eye", "Eye of Heaven",
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
        if (outcomeId.isEmpty()) {
            com.jttw.engine.progression.stats.ProgressionClass cls3 =
                    com.jttw.engine.progression.level.PlayerLevel.getProgressionClass(player);
            if (cls3 == com.jttw.engine.progression.stats.ProgressionClass.DIVINE_BEAST) {
                com.jttw.engine.progression.stats.DivineBeastLine line =
                        com.jttw.engine.progression.level.PlayerLevel.getBeastLine(player);
                if (line == com.jttw.engine.progression.stats.DivineBeastLine.WATER) {
                    buildDragonSlots(slots, unlocked);
                } else if (line == com.jttw.engine.progression.stats.DivineBeastLine.EARTH) {
                    buildTurtleSlots(slots, unlocked);
                } else if (line == com.jttw.engine.progression.stats.DivineBeastLine.BEAST) {
                    buildByakkoSlots(slots, unlocked);
                } else if (line == com.jttw.engine.progression.stats.DivineBeastLine.FIRE) {
                    buildPhoenixSlots(slots, unlocked);
                }
            }
        }

        return new SyncAbilitiesPacket(player.getUUID(), java.util.Arrays.asList(slots));
    }

    private static void buildTurtleSlots(Entry[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Shell Guard
        int shellTier = TurtleSkillTree.getHighestTier(unlocked, "tu_shell");
        if (shellTier >= 1) {
            slots[0] = new Entry("tu_shell", "Shell Guard",
                    AbilityType.HELD, shellTier);
        }
        // Slot 1 (B) — Unstoppable Force
        int forceTier = TurtleSkillTree.getHighestTier(unlocked, "tu_force");
        if (forceTier >= 1) {
            slots[1] = new Entry("tu_force", "Unstoppable Force",
                    AbilityType.TOGGLE, forceTier);
        }
        // Slot 2 (N) — Tectonic Slam (USE)
        int tectonicTier = TurtleSkillTree.getHighestTier(unlocked, "tu_tectonic");
        if (tectonicTier >= 1) {
            slots[2] = new Entry("tu_tectonic", "Tectonic Slam",
                    AbilityType.TOGGLE, tectonicTier);
        }
        // Slot 3 (M) — Gravity Well (TOGGLE)
        int gravityTier = TurtleSkillTree.getHighestTier(unlocked, "tu_gravity");
        if (gravityTier >= 1) {
            slots[3] = new Entry("tu_gravity", "Gravity Well",
                    AbilityType.TOGGLE, gravityTier);
        }
    }

    private static void buildByakkoSlots(Entry[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Predator's Leap
        int leapTier = ByakkoSkillTree.getHighestTier(unlocked, "bk_leap");
        if (leapTier >= 1) {
            slots[0] = new Entry("bk_leap", "Predator's Leap",
                    AbilityType.TOGGLE, leapTier);
        }
        // Slot 1 (B) — Territorial Roar
        int roarTier = ByakkoSkillTree.getHighestTier(unlocked, "bk_roar");
        if (roarTier >= 1) {
            slots[1] = new Entry("bk_roar", "Territorial Roar",
                    AbilityType.TOGGLE, roarTier);
        }
    }

    private static void buildDragonSlots(Entry[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Lightning Strike
        int lightningTier = DragonSkillTree.getHighestTier(unlocked, "dr_lightning");
        if (lightningTier >= 1) {
            slots[0] = new Entry("dr_lightning", "Lightning Strike",
                    AbilityType.TOGGLE, lightningTier);
        }
        // Slot 1 (B) — Weather Control
        int weatherTier = DragonSkillTree.getHighestTier(unlocked, "dr_weather");
        if (weatherTier >= 1) {
            slots[1] = new Entry("dr_weather", "Weather Control",
                    AbilityType.TOGGLE, weatherTier);
        }
        // Slot 2 (N) — Tidal Wave (priority) or Dimension Travel (fallback at flight T3)
        int waveTier = DragonSkillTree.getHighestTier(unlocked, "dr_wave");
        if (waveTier >= 1) {
            slots[2] = new Entry("dr_wave", "Tidal Wave",
                    AbilityType.TOGGLE, waveTier);
        } else {
            int dimTier = DragonSkillTree.getHighestTier(unlocked, "dr_flight");
            if (dimTier >= 3) {
                slots[2] = new Entry("dr_dimension", "Dimension Travel",
                        AbilityType.TOGGLE, dimTier);
            }
        }
    }

    private static void buildYuanDiSlots(Entry[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Primordial Transformation (TOGGLE at T2+, passive at T1)
        int transformTier = YuanDiSkillTree.getHighestTier(unlocked, "yd_transform");
        if (transformTier >= 2) {
            slots[0] = new Entry("yd_transform", "Primordial Form",
                    AbilityType.TOGGLE, transformTier);
        }
        // Slot 1 (B) — Demon's Breath (HELD)
        int breathTier = YuanDiSkillTree.getHighestTier(unlocked, "yd_breath");
        if (breathTier >= 1) {
            slots[1] = new Entry("yd_breath", "Demon's Breath",
                    AbilityType.HELD, breathTier);
        }
        // Slot 2 (N) — Primordial Smite
        int smiteTier = YuanDiSkillTree.getHighestTier(unlocked, "yd_smite");
        if (smiteTier >= 1) {
            slots[2] = new Entry("yd_smite", "Primordial Smite",
                    AbilityType.TOGGLE, smiteTier);
        }
        // Slot 3 (M) — Abyssal Gravity
        int gravityTier = YuanDiSkillTree.getHighestTier(unlocked, "yd_gravity");
        if (gravityTier >= 1) {
            slots[3] = new Entry("yd_gravity", "Abyssal Gravity",
                    AbilityType.TOGGLE, gravityTier);
        }
    }

    private static void buildPhoenixSlots(Entry[] slots, Set<String> unlocked) {
        // Slot 0 (V) — Flamethrower (HELD)
        int flameTier = PhoenixSkillTree.getHighestTier(unlocked, "ph_flame");
        if (flameTier >= 1) {
            slots[0] = new Entry("ph_flame", "Flamethrower",
                    AbilityType.HELD, flameTier);
        }
        // Slot 1 (B) — Solar Radiance (TOGGLE)
        int radianceTier = PhoenixSkillTree.getHighestTier(unlocked, "ph_radiance");
        if (radianceTier >= 1) {
            slots[1] = new Entry("ph_radiance", "Solar Radiance",
                    AbilityType.TOGGLE, radianceTier);
        }
    }
}