package me.mss1r.recruitsrtscommand.client.command;

/** Screen-space camera speed, independent of rendering frequency. */
public final class MapCameraMotion {
    private static final double PIXELS_PER_SECOND = 420.0D;
    private static final long MAX_FRAME_NANOS = 100_000_000L;
    private long lastFrame = Long.MIN_VALUE;

    public double advance(long now) {
        if (lastFrame == Long.MIN_VALUE) {
            lastFrame = now;
            return 0.0D;
        }
        long elapsed = Math.max(0L, Math.min(MAX_FRAME_NANOS, now - lastFrame));
        lastFrame = now;
        return elapsed * (PIXELS_PER_SECOND / 1_000_000_000.0D);
    }

    public void reset() {
        lastFrame = Long.MIN_VALUE;
    }
}
