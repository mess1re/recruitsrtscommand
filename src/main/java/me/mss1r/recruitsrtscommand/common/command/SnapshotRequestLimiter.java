package me.mss1r.recruitsrtscommand.common.command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class SnapshotRequestLimiter {
    private final Map<UUID, Long> lastRequests = new HashMap<>();

    boolean acquire(UUID player, long tick) {
        Long last = lastRequests.get(player);
        if (last != null && tick >= last && tick - last < CommandLimits.SNAPSHOT_INTERVAL_TICKS) {
            return false;
        }
        lastRequests.put(player, tick);
        return true;
    }

    void remove(UUID player) {
        lastRequests.remove(player);
    }

    void clear() {
        lastRequests.clear();
    }
}
