package com.jttw.engine.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientRingVisuals {
    private ClientRingVisuals() {}

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    public static void setActive(UUID playerId, boolean active) {
        if (active) ACTIVE.add(playerId);
        else ACTIVE.remove(playerId);
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }
}