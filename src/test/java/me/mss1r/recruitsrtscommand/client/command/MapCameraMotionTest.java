package me.mss1r.recruitsrtscommand.client.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapCameraMotionTest {
    @Test
    void sameDistanceAtDifferentFrameRates() {
        for (int fps : new int[]{30, 60, 144, 240}) {
            MapCameraMotion motion = new MapCameraMotion();
            double distance = motion.advance(0L);
            for (int frame = 1; frame <= fps; frame++) {
                distance += motion.advance(1_000_000_000L * frame / fps);
            }
            assertEquals(420.0D, distance, 0.000001D, "FPS: " + fps);
        }
    }

    @Test
    void longPauseDoesNotJumpAcrossTheMap() {
        MapCameraMotion motion = new MapCameraMotion();
        motion.advance(0L);
        assertEquals(42.0D, motion.advance(30_000_000_000L));
        motion.reset();
        assertEquals(0.0D, motion.advance(60_000_000_000L));
    }
}
