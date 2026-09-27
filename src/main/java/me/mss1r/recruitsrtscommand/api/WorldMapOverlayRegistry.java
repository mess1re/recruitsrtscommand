package me.mss1r.recruitsrtscommand.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry for client-side tactical-map overlays. */
public final class WorldMapOverlayRegistry {
    private static final List<WorldMapOverlay> OVERLAYS = new CopyOnWriteArrayList<>();

    private WorldMapOverlayRegistry() {
    }

    /** Registers an overlay once, preserving registration order. */
    public static void register(WorldMapOverlay overlay) {
        if (overlay != null && !OVERLAYS.contains(overlay)) {
            OVERLAYS.add(overlay);
        }
    }

    public static List<WorldMapOverlay> overlays() {
        return List.copyOf(OVERLAYS);
    }
}
