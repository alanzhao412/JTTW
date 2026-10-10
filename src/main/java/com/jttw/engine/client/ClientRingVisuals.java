package com.jttw.engine.client;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whose Ring of Fire is burning, as the client knows it, and how far each has faded in: a ring
 * fades in over {@value #FADE_TICKS} ticks when lit and out as long when put out, rather than
 * appearing and vanishing at once.
 */
public final class ClientRingVisuals {
    private ClientRingVisuals() {}

    /** Ticks a ring takes to fade fully in, or out. */
    public static final int FADE_TICKS = 10;

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    /** Each ring's presence, 0 to 1: now and a tick ago, to blend between frames. */
    private static final Map<UUID, float[]> FADE = new ConcurrentHashMap<>();

    public static void setActive(UUID playerId, boolean active) {
        if (active) ACTIVE.add(playerId);
        else ACTIVE.remove(playerId);
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }

    /** Moves every ring a tick further in or out. */
    public static void tickFade() {
        for (UUID id : ACTIVE) FADE.computeIfAbsent(id, k -> new float[2]);
        FADE.entrySet().removeIf(e -> {
            float[] f = e.getValue();
            f[1] = f[0];
            f[0] = Math.max(0, Math.min(1, f[0] + (ACTIVE.contains(e.getKey()) ? 1f : -1f) / FADE_TICKS));
            return f[0] == 0 && f[1] == 0 && !ACTIVE.contains(e.getKey());
        });
    }

    /** How present this player's ring is, 0 (gone) to 1 (fully lit), between ticks. */
    public static float presence(UUID playerId, float partialTick) {
        float[] f = FADE.get(playerId);
        if (f == null) return 0;
        float t = f[1] + (f[0] - f[1]) * partialTick;
        return t * t * (3 - 2 * t); // eased
    }
}
