package me.mss1r.recruitsrtscommand.common.command;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommandLimitsTest {
    @Test
    void rejectsInvalidFireZoneBeforeDispatch() {
        assertTrue(CommandLimits.validFireZone(0, 0, 0, 3));
        assertTrue(CommandLimits.validFireZone(48, 48, 2, 3));
        assertFalse(CommandLimits.validFireZone(-1, 8, 0, 3));
        assertFalse(CommandLimits.validFireZone(8, 49, 0, 3));
        assertFalse(CommandLimits.validFireZone(Integer.MAX_VALUE, 8, 0, 3));
        assertFalse(CommandLimits.validFireZone(8, 8, -1, 3));
        assertFalse(CommandLimits.validFireZone(8, 8, 3, 3));
    }

    @Test
    void snapshotThrottleIsPerPlayerAndClearedOnLogout() {
        SnapshotRequestLimiter limiter = new SnapshotRequestLimiter();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(limiter.acquire(first, 100));
        assertFalse(limiter.acquire(first, 100));
        assertTrue(limiter.acquire(second, 100));
        assertFalse(limiter.acquire(first, 104));
        assertTrue(limiter.acquire(first, 105));
        limiter.remove(first);
        assertTrue(limiter.acquire(first, 105));
        limiter.clear();
        assertTrue(limiter.acquire(second, 100));
    }
}
